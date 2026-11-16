package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {

    /** 문제없는 입력. 테스트마다 한 칸만 바꿔서 그 칸의 규칙을 확인한다. */
    private fun validate(
        username: String = "suhwan_01",
        password: String = "password1",
        passwordConfirm: String = password,
        displayName: String = "전수환"
    ) = AuthValidator.validateSignUp(username, password, passwordConfirm, displayName)

    @Test
    fun `규칙에 맞는 입력은 통과한다`() {
        assertNull(validate())
    }

    @Test
    fun `아이디는 4자 이상 20자 이하여야 한다`() {
        assertEquals(AuthError.USERNAME_INVALID, validate(username = "abc"))
        assertNull(validate(username = "abcd"))
        assertNull(validate(username = "a".repeat(20)))
        assertEquals(AuthError.USERNAME_INVALID, validate(username = "a".repeat(21)))
    }

    @Test
    fun `아이디에는 영문 소문자와 숫자와 밑줄만 쓸 수 있다`() {
        assertEquals(AuthError.USERNAME_INVALID, validate(username = "su hwan"))
        assertEquals(AuthError.USERNAME_INVALID, validate(username = "수환수환"))
        assertEquals(AuthError.USERNAME_INVALID, validate(username = "su-hwan"))
        assertEquals(AuthError.USERNAME_INVALID, validate(username = ""))
    }

    @Test
    fun `아이디의 대문자와 앞뒤 공백은 다듬어서 받아들인다`() {
        assertNull(validate(username = "  SuHwan  "))
        assertEquals("suhwan", AuthValidator.normalizeUsername("  SuHwan  "))
    }

    @Test
    fun `비밀번호는 8자 이상이어야 한다`() {
        assertEquals(AuthError.PASSWORD_TOO_SHORT, validate(password = "1234567"))
        assertNull(validate(password = "12345678"))
    }

    @Test
    fun `비밀번호와 비밀번호 확인이 다르면 실패한다`() {
        assertEquals(
            AuthError.PASSWORD_MISMATCH,
            validate(password = "password1", passwordConfirm = "password2")
        )
    }

    @Test
    fun `이름은 비어 있으면 안 되고 20자를 넘으면 안 된다`() {
        assertEquals(AuthError.DISPLAY_NAME_INVALID, validate(displayName = ""))
        assertEquals(AuthError.DISPLAY_NAME_INVALID, validate(displayName = "   "))
        assertNull(validate(displayName = "가".repeat(20)))
        assertEquals(AuthError.DISPLAY_NAME_INVALID, validate(displayName = "가".repeat(21)))
    }

    @Test
    fun `로그인은 빈 칸만 검사한다`() {
        assertEquals(AuthError.EMPTY_FIELDS, AuthValidator.validateLogin("", "password1"))
        assertEquals(AuthError.EMPTY_FIELDS, AuthValidator.validateLogin("   ", "password1"))
        assertEquals(AuthError.EMPTY_FIELDS, AuthValidator.validateLogin("suhwan", ""))
        assertNull(AuthValidator.validateLogin("suhwan", "x"))
    }
}
