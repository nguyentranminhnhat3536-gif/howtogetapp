package com.ethanstudio.snapsheet.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Người dùng đang đăng nhập (chỉ những gì app cần hiển thị). */
data class AuthUser(val email: String?, val verified: Boolean, val usesPassword: Boolean, val displayName: String? = null) {
    /** Tên hiện trên hồ sơ: tên Google, nếu không có thì phần trước @ của email. */
    val shownName: String get() = displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore('@').orEmpty()
}

/** Lỗi đã đổi sang loại của app để giao diện hiện câu dễ hiểu. */
class AuthException(val error: AuthError, val code: String? = null) : Exception(code ?: error.name)

/**
 * Đăng ký, đăng nhập và xóa tài khoản qua Firebase Authentication.
 * Mật khẩu chỉ gửi cho Firebase, app không lưu. Tài liệu đã quét KHÔNG gửi đi đâu.
 * Nếu bản build không có cấu hình Firebase thì [isConfigured] = false và mọi thao tác báo NOT_CONFIGURED.
 */
class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context)
        FirebaseAuth.getInstance()
    }.getOrNull()

    val isConfigured: Boolean get() = auth != null

    private val _user = MutableStateFlow(auth?.currentUser?.toAuthUser())
    val user: StateFlow<AuthUser?> = _user.asStateFlow()

    init {
        auth?.addAuthStateListener { _user.value = it.currentUser?.toAuthUser() }
    }

    suspend fun signUp(email: String, password: String): Unit = guarded {
        val result = requireAuth().createUserWithEmailAndPassword(email.trim(), password).awaitTask()
        result.user?.sendEmailVerification()?.awaitTask()
        refreshUser()
    }

    suspend fun signIn(email: String, password: String): Unit = guarded {
        requireAuth().signInWithEmailAndPassword(email.trim(), password).awaitTask()
        refreshUser()
    }

    /** Đăng nhập bằng tài khoản Google trên máy (bảng chọn của Google hiện lên). */
    suspend fun signInWithGoogle(activity: Activity): Unit = guarded {
        val firebase = requireAuth()
        val clientId = webClientId() ?: throw AuthException(AuthError.NOT_CONFIGURED)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthException(AuthError.GOOGLE_FAILED)
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        firebase.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).awaitTask()
        refreshUser()
    }

    /** Gửi email đặt lại mật khẩu. Không báo email có tồn tại hay không (tránh dò tài khoản). */
    suspend fun sendPasswordReset(email: String): Unit = guarded {
        try {
            requireAuth().sendPasswordResetEmail(email.trim()).awaitTask()
        } catch (e: FirebaseAuthInvalidUserException) {
            // Cố ý im lặng: không cho biết email này chưa đăng ký.
        }
    }

    suspend fun resendVerification(): Unit = guarded {
        requireAuth().currentUser?.sendEmailVerification()?.awaitTask()
    }

    /** Đọc lại trạng thái (ví dụ sau khi người dùng bấm link xác nhận trong email). */
    suspend fun refreshUser() {
        val current = auth?.currentUser ?: return
        runCatching { current.reload().awaitTask() }
        _user.value = auth?.currentUser?.toAuthUser()
    }

    suspend fun signOut() {
        auth?.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        _user.value = null
    }

    /**
     * Xóa hẳn tài khoản trên máy chủ. Tài khoản email cần nhập lại mật khẩu ([password]).
     * Tài khoản Google đăng nhập đã lâu sẽ nhận RECENT_LOGIN_REQUIRED: đăng nhập lại rồi xóa.
     */
    suspend fun deleteAccount(password: String?): Unit = guarded {
        val current = requireAuth().currentUser ?: return@guarded
        val email = current.email
        if (password != null && email != null) {
            current.reauthenticate(EmailAuthProvider.getCredential(email, password)).awaitTask()
        }
        current.delete().awaitTask()
        signOut()
    }

    private fun requireAuth(): FirebaseAuth = auth ?: throw AuthException(AuthError.NOT_CONFIGURED)

    /** Mã OAuth "web client" do plugin google-services tạo từ google-services.json (không có thì null). */
    private fun webClientId(): String? {
        @Suppress("DiscouragedApi")
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id == 0) null else context.getString(id)
    }

    /** Chạy một thao tác và đổi mọi lỗi thành [AuthException]. */
    private suspend fun <T> guarded(block: suspend () -> T): T =
        try {
            block()
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("SnapSheetAuth", "Auth failed", e)
            throw AuthException(e.toAuthError(), e.errorCode())
        }
}

private fun FirebaseUser.toAuthUser() = AuthUser(
    email = email,
    verified = isEmailVerified,
    usesPassword = providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID },
    displayName = displayName,
)

/** Đổi lỗi của Firebase và Credential Manager thành [AuthError]. Thứ tự quan trọng: lớp con trước lớp cha. */
internal fun Throwable.toAuthError(): AuthError = when (this) {
    is AuthException -> error
    is GetCredentialCancellationException -> AuthError.CANCELED
    is GetCredentialException -> AuthError.GOOGLE_FAILED
    is FirebaseAuthRecentLoginRequiredException -> AuthError.RECENT_LOGIN_REQUIRED
    is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
    is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
    is FirebaseAuthInvalidUserException -> AuthError.USER_NOT_FOUND
    is FirebaseAuthInvalidCredentialsException -> AuthError.WRONG_CREDENTIALS
    is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
    is FirebaseNetworkException -> AuthError.NETWORK
    is FirebaseAuthException -> when {
        errorCode == "ERROR_OPERATION_NOT_ALLOWED" -> AuthError.PROVIDER_DISABLED
        message.orEmpty().contains("CONFIGURATION_NOT_FOUND") -> AuthError.PROVIDER_DISABLED
        else -> AuthError.UNKNOWN
    }
    else -> if (message.orEmpty().contains("CONFIGURATION_NOT_FOUND")) AuthError.PROVIDER_DISABLED else AuthError.UNKNOWN
}

/** Mã lỗi ngắn để hiện cho người dùng chụp màn hình gửi hỗ trợ (không chứa email hay mật khẩu). */
internal fun Throwable.errorCode(): String = when (this) {
    is FirebaseAuthException -> errorCode
    else -> javaClass.simpleName
}

/** Chờ một Task của Google Play services xong trong coroutine. */
internal suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (!cont.isActive) return@addOnCompleteListener
        if (task.isSuccessful) {
            cont.resume(task.result)
        } else {
            cont.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
        }
    }
}
