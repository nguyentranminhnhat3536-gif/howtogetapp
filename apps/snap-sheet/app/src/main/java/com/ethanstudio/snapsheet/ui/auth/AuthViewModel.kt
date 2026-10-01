package com.ethanstudio.snapsheet.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.snapsheet.auth.AuthError
import com.ethanstudio.snapsheet.auth.AuthException
import com.ethanstudio.snapsheet.auth.AuthRepository
import com.ethanstudio.snapsheet.auth.AuthUser
import com.ethanstudio.snapsheet.auth.AuthValidation
import com.ethanstudio.snapsheet.data.SessionStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val busy: Boolean = false,
    val error: AuthError? = null,
    /** Email vừa đăng ký, hiện trên màn "Check your inbox". */
    val pendingEmail: String = "",
)

sealed interface AuthEvent {
    /** Đăng nhập hoặc đăng ký xong, hoặc chọn dùng không cần tài khoản: vào màn chính. */
    data object EnterApp : AuthEvent
    data object VerificationSent : AuthEvent
    data object ResetSent : AuthEvent
    data object ResentVerification : AuthEvent
    data object SignedOut : AuthEvent
    data object Deleted : AuthEvent
}

/** Trạng thái chung của các màn đăng nhập, đăng ký và phần tài khoản trong tab Account. */
class AuthViewModel(
    private val auth: AuthRepository,
    private val session: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val user: StateFlow<AuthUser?> = auth.user
    val onboarded = session.onboarded

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun clearError() = _state.update { it.copy(error = null) }

    fun signIn(email: String, password: String) {
        val invalid = AuthValidation.checkSignIn(email, password)
        if (invalid != null) return fail(invalid)
        launch {
            auth.signIn(email, password)
            session.setOnboarded()
            _events.send(AuthEvent.EnterApp)
        }
    }

    fun signUp(email: String, password: String, confirm: String, agreed: Boolean) {
        val invalid = AuthValidation.checkSignUp(email, password, confirm, agreed)
        if (invalid != null) return fail(invalid)
        launch {
            auth.signUp(email, password)
            session.setOnboarded()
            _state.update { it.copy(pendingEmail = email.trim()) }
            _events.send(AuthEvent.VerificationSent)
        }
    }

    fun signInWithGoogle(activity: Activity) {
        launch {
            auth.signInWithGoogle(activity)
            session.setOnboarded()
            _events.send(AuthEvent.EnterApp)
        }
    }

    fun sendPasswordReset(email: String) {
        val invalid = AuthValidation.checkEmail(email)
        if (invalid != null) return fail(invalid)
        launch {
            auth.sendPasswordReset(email)
            _events.send(AuthEvent.ResetSent)
        }
    }

    fun resendVerification() = launch {
        auth.resendVerification()
        _events.send(AuthEvent.ResentVerification)
    }

    fun refreshUser() {
        viewModelScope.launch { auth.refreshUser() }
    }

    /** "Continue without an account": app dùng đủ tính năng không cần tài khoản. */
    fun continueWithoutAccount() {
        viewModelScope.launch {
            session.setOnboarded()
            _events.send(AuthEvent.EnterApp)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            _events.send(AuthEvent.SignedOut)
        }
    }

    fun deleteAccount(password: String?) = launch {
        auth.deleteAccount(password?.takeIf { it.isNotEmpty() })
        _events.send(AuthEvent.Deleted)
    }

    private fun fail(error: AuthError) = _state.update { it.copy(error = error) }

    /** Chạy một thao tác mạng: bật trạng thái bận, đổi lỗi thành câu cho người dùng. */
    private fun launch(block: suspend () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                block()
            } catch (e: AuthException) {
                if (e.error != AuthError.CANCELED) fail(e.error)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
