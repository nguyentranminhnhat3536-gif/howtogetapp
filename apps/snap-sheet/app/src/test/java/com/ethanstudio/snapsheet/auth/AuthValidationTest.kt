package com.ethanstudio.snapsheet.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {
    @Test
    fun acceptsNormalEmailsWithSpaces() {
        assertTrue(AuthValidation.isEmail("name@example.com"))
        assertTrue(AuthValidation.isEmail("  an.nguyen+scan@mail.co.vn "))
    }

    @Test
    fun rejectsBrokenEmails() {
        assertFalse(AuthValidation.isEmail(""))
        assertFalse(AuthValidation.isEmail("name@"))
        assertFalse(AuthValidation.isEmail("name@example"))
        assertFalse(AuthValidation.isEmail("na me@example.com"))
    }

    @Test
    fun signInNeedsEmailThenPassword() {
        assertEquals(AuthError.INVALID_EMAIL, AuthValidation.checkSignIn("bad", "x"))
        assertEquals(AuthError.PASSWORD_REQUIRED, AuthValidation.checkSignIn("a@b.co", ""))
        assertNull(AuthValidation.checkSignIn("a@b.co", "x"))
    }

    @Test
    fun signUpChecksInOrder() {
        assertEquals(AuthError.INVALID_EMAIL, AuthValidation.checkSignUp("bad", "12345678", "12345678", true))
        assertEquals(AuthError.WEAK_PASSWORD, AuthValidation.checkSignUp("a@b.co", "1234567", "1234567", true))
        assertEquals(AuthError.PASSWORD_MISMATCH, AuthValidation.checkSignUp("a@b.co", "12345678", "12345679", true))
        assertEquals(AuthError.TERMS_REQUIRED, AuthValidation.checkSignUp("a@b.co", "12345678", "12345678", false))
        assertNull(AuthValidation.checkSignUp("a@b.co", "12345678", "12345678", true))
    }

    @Test
    fun resetNeedsValidEmail() {
        assertEquals(AuthError.INVALID_EMAIL, AuthValidation.checkEmail("x"))
        assertNull(AuthValidation.checkEmail("x@y.zz"))
    }
}
