package com.cau.medicheck.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 비밀번호를 그대로 저장하지 않고 "해시"로 바꿔서 저장하기 위한 도구.
 *
 * 해시: 원래 값을 알아낼 수 없게 뒤섞은 결과. 같은 입력이면 항상 같은 결과가 나온다.
 *   - 가입할 때: 비밀번호 → 해시 → DB에 저장
 *   - 로그인할 때: 입력한 비밀번호를 같은 방법으로 해시해서, 저장된 해시와 같은지 비교
 * 이렇게 하면 DB 파일이 유출되어도 원래 비밀번호는 알 수 없다.
 *
 * 방식: PBKDF2-HMAC-SHA256
 *   - salt(소금): 사용자마다 다른 무작위 값. 같은 비밀번호라도 해시가 달라지게 한다.
 *   - 반복 횟수: 일부러 계산을 느리게 만들어, 비밀번호를 하나씩 대입해 보는 공격을 어렵게 한다.
 *
 * 저장 형식 (password_hash 칸에 들어가는 글자):
 *   pbkdf2$반복횟수$salt$해시      (salt와 해시는 Base64 글자로 바꾼 것)
 *
 * 안드로이드 기능을 쓰지 않는 순수 Kotlin 코드라서 단위 테스트를 할 수 있다.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2"
    private const val ITERATIONS = 100_000
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256

    /** 비밀번호를 해시해서, DB에 저장할 글자로 돌려준다. 부를 때마다 salt가 달라진다. */
    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(password, salt, ITERATIONS)
        return listOf(PREFIX, ITERATIONS.toString(), encode(salt), encode(hash)).joinToString("$")
    }

    /** 입력한 비밀번호가 저장된 해시와 맞는지 확인한다. */
    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split("$")
        if (parts.size != 4 || parts[0] != PREFIX) return false

        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = decode(parts[2]) ?: return false
        val expected = decode(parts[3]) ?: return false

        val actual = pbkdf2(password, salt, iterations)
        // 두 해시를 비교한다. isEqual은 비교에 걸리는 시간이 일정해서 더 안전하다.
        return MessageDigest.isEqual(expected, actual)
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(text: String): ByteArray? =
        try {
            Base64.getDecoder().decode(text)
        } catch (e: IllegalArgumentException) {
            null
        }
}
