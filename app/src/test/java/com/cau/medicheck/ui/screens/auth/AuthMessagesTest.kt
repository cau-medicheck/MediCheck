package com.cau.medicheck.ui.screens.auth

import com.cau.medicheck.R
import com.cau.medicheck.domain.AuthError
import org.junit.Assert.assertEquals
import org.junit.Test

/** 로그인 전 화면 토스트 문구 고르기 (구현 명세 01 S01~S03 '규칙·검증') */
class AuthMessagesTest {

    @Test
    fun `오류마다 서로 다른 문구가 있다`() {
        val ids = AuthError.entries.map { it.messageRes() }
        assertEquals(AuthError.entries.size, ids.toSet().size)
    }

    @Test
    fun `빈칸 오류와 형식 오류는 다른 문구다`() {
        assertEquals(R.string.error_email_empty, AuthError.EMAIL_EMPTY.messageRes())
        assertEquals(R.string.error_email_invalid, AuthError.EMAIL_INVALID.messageRes())
        assertEquals(R.string.error_password_empty, AuthError.PASSWORD_EMPTY.messageRes())
        assertEquals(R.string.error_password_too_short, AuthError.PASSWORD_TOO_SHORT.messageRes())
    }

    @Test
    fun `비밀번호 재설정 요청은 빈칸 형식 순서로 검사한다`() {
        assertEquals(R.string.error_email_empty, passwordHelpMessageRes(""))
        assertEquals(R.string.error_email_empty, passwordHelpMessageRes("   "))
        assertEquals(R.string.error_email_invalid, passwordHelpMessageRes("abc"))
    }

    @Test
    fun `형식이 맞으면 가입 여부와 상관없이 같은 안내를 띄운다`() {
        assertEquals(R.string.toast_password_help, passwordHelpMessageRes("a@b.co"))
        assertEquals(R.string.toast_password_help, passwordHelpMessageRes("  Nobody@Test.com "))
    }
}
