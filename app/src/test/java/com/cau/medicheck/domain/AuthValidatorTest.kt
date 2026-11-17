package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    /** 문제없는 입력. 테스트마다 한 칸만 바꿔서 그 칸의 규칙을 확인한다. */
    private fun validate(
        displayName: String = "전수환",
        email: String = "suhwan@cau.ac.kr",
        password: String = "pass12",
        passwordConfirm: String = password
    ) = AuthValidator.validateSignUp(displayName, email, password, passwordConfirm)

    @Test
    fun `규칙에 맞는 입력은 통과한다`() {
        assertNull(validate())
    }

    @Test
    fun `이메일은 골뱅이와 점이 있는 주소 모양이어야 한다`() {
        assertTrue(AuthValidator.isValidEmail("a@b.co"))
        assertTrue(AuthValidator.isValidEmail("first.last+tag@mail.example.com"))
        assertFalse(AuthValidator.isValidEmail(""))
        assertFalse(AuthValidator.isValidEmail("suhwan"))
        assertFalse(AuthValidator.isValidEmail("suhwan@cau"))
        assertFalse(AuthValidator.isValidEmail("@cau.ac.kr"))
        assertFalse(AuthValidator.isValidEmail("su hwan@cau.ac.kr"))
        assertFalse(AuthValidator.isValidEmail("su@hwan@cau.ac.kr"))
        assertEquals(AuthError.EMAIL_INVALID, validate(email = "not-an-email"))
    }

    @Test
    fun `이메일의 대문자와 앞뒤 공백은 다듬어서 받아들인다`() {
        assertNull(validate(email = "  SuHwan@CAU.ac.kr  "))
        assertEquals("suhwan@cau.ac.kr", AuthValidator.normalizeEmail("  SuHwan@CAU.ac.kr  "))
    }

    @Test
    fun `비밀번호는 6자 이상이어야 한다`() {
        assertEquals(AuthError.PASSWORD_TOO_SHORT, validate(password = "12345"))
        assertNull(validate(password = "123456"))
    }

    @Test
    fun `비밀번호와 비밀번호 확인이 다르면 실패한다`() {
        assertEquals(
            AuthError.PASSWORD_MISMATCH,
            validate(password = "password1", passwordConfirm = "password2")
        )
    }

    @Test
    fun `이름은 비어 있으면 안 되고 30자를 넘으면 안 된다`() {
        assertEquals(AuthError.DISPLAY_NAME_INVALID, validate(displayName = ""))
        assertEquals(AuthError.DISPLAY_NAME_INVALID, validate(displayName = "   "))
        assertNull(validate(displayName = "가".repeat(30)))
        assertEquals(AuthError.DISPLAY_NAME_INVALID, validate(displayName = "가".repeat(31)))
    }

    @Test
    fun `로그인은 이메일 빈칸 형식 비밀번호 빈칸 길이 순서로 검사한다`() {
        assertEquals(AuthError.EMAIL_EMPTY, AuthValidator.validateLogin("", ""))
        assertEquals(AuthError.EMAIL_EMPTY, AuthValidator.validateLogin("   ", "password1"))
        assertEquals(AuthError.EMAIL_INVALID, AuthValidator.validateLogin("suhwan", ""))
        assertEquals(AuthError.PASSWORD_EMPTY, AuthValidator.validateLogin("a@b.co", ""))
        assertEquals(AuthError.PASSWORD_TOO_SHORT, AuthValidator.validateLogin("a@b.co", "12345"))
        assertNull(AuthValidator.validateLogin(" A@B.co ", "123456"))
    }

    @Test
    fun `가입에서도 빈칸은 형식보다 먼저 알린다`() {
        assertEquals(AuthError.EMAIL_EMPTY, validate(email = "  "))
        assertEquals(AuthError.PASSWORD_EMPTY, validate(password = ""))
        // 이름 → 이메일 → 비밀번호 → 일치 순서
        assertEquals(AuthError.DISPLAY_NAME_INVALID, validate(displayName = "", email = ""))
        assertEquals(AuthError.EMAIL_INVALID, validate(email = "x", password = "1"))
        assertEquals(AuthError.PASSWORD_TOO_SHORT, validate(password = "1", passwordConfirm = "2"))
    }

    @Test
    fun `비밀번호는 다듬지 않고 공백도 한 글자로 센다`() {
        assertNull(validate(password = "     a"))
        assertEquals(AuthError.PASSWORD_TOO_SHORT, validate(password = "  a  "))
    }

    @Test
    fun `프로필 수정은 이름과 이메일을 검사한다`() {
        assertNull(AuthValidator.validateProfile("전수환", "a@b.co"))
        assertEquals(AuthError.DISPLAY_NAME_INVALID, AuthValidator.validateProfile("", "a@b.co"))
        assertEquals(AuthError.EMAIL_INVALID, AuthValidator.validateProfile("전수환", "ab.co"))
        assertEquals(AuthError.EMAIL_EMPTY, AuthValidator.validateProfile("전수환", " "))
    }
}
