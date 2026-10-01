package com.ethanstudio.snapsheet.auth

/** Các lỗi đăng nhập mà người dùng có thể gặp. Mỗi lỗi có một câu giải thích riêng (xem AuthMessages). */
enum class AuthError {
    INVALID_EMAIL,
    PASSWORD_REQUIRED,
    WEAK_PASSWORD,
    PASSWORD_MISMATCH,
    TERMS_REQUIRED,
    WRONG_CREDENTIALS,
    EMAIL_IN_USE,
    USER_NOT_FOUND,
    TOO_MANY_REQUESTS,
    NETWORK,
    RECENT_LOGIN_REQUIRED,
    NOT_CONFIGURED,
    PROVIDER_DISABLED,
    GOOGLE_FAILED,
    CANCELED,
    UNKNOWN,
}

/** Kiểm tra dữ liệu nhập trước khi gửi lên máy chủ. Trả về lỗi đầu tiên, hoặc null nếu hợp lệ. */
object AuthValidation {
    const val MIN_PASSWORD = 8

    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

    fun isEmail(input: String): Boolean = EMAIL.matches(input.trim())

    fun checkEmail(email: String): AuthError? = if (isEmail(email)) null else AuthError.INVALID_EMAIL

    fun checkSignIn(email: String, password: String): AuthError? = when {
        !isEmail(email) -> AuthError.INVALID_EMAIL
        password.isEmpty() -> AuthError.PASSWORD_REQUIRED
        else -> null
    }

    fun checkSignUp(email: String, password: String, confirm: String, agreed: Boolean): AuthError? = when {
        !isEmail(email) -> AuthError.INVALID_EMAIL
        password.length < MIN_PASSWORD -> AuthError.WEAK_PASSWORD
        password != confirm -> AuthError.PASSWORD_MISMATCH
        !agreed -> AuthError.TERMS_REQUIRED
        else -> null
    }
}
