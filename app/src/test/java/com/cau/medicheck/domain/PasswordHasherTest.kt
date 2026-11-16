package com.cau.medicheck.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PasswordHasher 단위 테스트.
 * 폰 없이 컴퓨터에서 바로 실행된다: ./gradlew testDebugUnitTest
 */
class PasswordHasherTest {

    @Test
    fun `올바른 비밀번호는 통과한다`() {
        val stored = PasswordHasher.hash("correct horse battery")

        assertTrue(PasswordHasher.verify("correct horse battery", stored))
    }

    @Test
    fun `틀린 비밀번호는 통과하지 못한다`() {
        val stored = PasswordHasher.hash("correct horse battery")

        assertFalse(PasswordHasher.verify("correct horse batterx", stored))
        assertFalse(PasswordHasher.verify("", stored))
    }

    @Test
    fun `저장되는 글자에 원래 비밀번호가 들어 있지 않다`() {
        val stored = PasswordHasher.hash("my-secret-password")

        assertFalse(stored.contains("my-secret-password"))
        assertTrue(stored.startsWith("pbkdf2$"))
    }

    @Test
    fun `같은 비밀번호라도 해시는 매번 다르다`() {
        // 사용자마다 salt가 달라서, 비밀번호가 같아도 저장되는 값은 다르다.
        val first = PasswordHasher.hash("same-password")
        val second = PasswordHasher.hash("same-password")

        assertNotEquals(first, second)
        assertTrue(PasswordHasher.verify("same-password", first))
        assertTrue(PasswordHasher.verify("same-password", second))
    }

    @Test
    fun `형식이 깨진 저장값은 오류 없이 false를 돌려준다`() {
        assertFalse(PasswordHasher.verify("anything", ""))
        assertFalse(PasswordHasher.verify("anything", "plain-text-password"))
        assertFalse(PasswordHasher.verify("anything", "pbkdf2\$abc\$AAAA\$AAAA"))
        assertFalse(PasswordHasher.verify("anything", "pbkdf2\$1000\$***\$***"))
    }

    @Test
    fun `한글과 특수문자가 섞인 비밀번호도 된다`() {
        val stored = PasswordHasher.hash("비밀번호!@# 123")

        assertTrue(PasswordHasher.verify("비밀번호!@# 123", stored))
        assertFalse(PasswordHasher.verify("비밀번호!@# 124", stored))
    }
}
