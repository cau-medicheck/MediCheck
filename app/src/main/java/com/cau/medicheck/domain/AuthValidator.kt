package com.cau.medicheck.domain

/**
 * 회원가입·로그인·프로필 수정이 실패하는 이유의 목록.
 * 화면에 보여 줄 문장은 여기 두지 않고, 화면 쪽에서 이 값에 맞는 문장을 고른다.
 */
enum class AuthError {
    EMAIL_EMPTY,            // 이메일 칸이 비어 있음
    EMAIL_INVALID,          // 이메일 형식이 맞지 않음
    PASSWORD_EMPTY,         // 비밀번호 칸이 비어 있음
    PASSWORD_TOO_SHORT,     // 비밀번호가 너무 짧음
    PASSWORD_MISMATCH,      // 비밀번호와 비밀번호 확인이 다름
    DISPLAY_NAME_INVALID,   // 이름이 비었거나 너무 김
    EMAIL_TAKEN,            // 이미 가입된 이메일 (샘플 계정 이메일 포함)
    WRONG_CREDENTIALS,      // 이메일이 없거나 비밀번호가 틀림
    SAMPLE_FAILED           // 샘플 계정을 준비하다 DB 오류가 남
}

/**
 * 입력값이 규칙에 맞는지 검사한다. 순수 Kotlin 코드라서 단위 테스트를 할 수 있다.
 *
 * 검사 순서는 "빈칸 → 형식 → 길이"다. 빈칸을 형식보다 먼저 보는 이유: 사용자가 칸을 비웠을 때
 * "형식이 틀렸다"가 아니라 "입력해 달라"는 안내를 받게 하기 위해서다 (F006 누락값 안내).
 */
object AuthValidator {

    const val PASSWORD_MIN = 6
    const val DISPLAY_NAME_MAX = 30

    /**
     * 이메일 모양: "무언가@무언가.무언가" (공백과 @가 중간에 더 있으면 안 됨).
     * 실제로 존재하는 주소인지는 확인하지 않는다. 계정은 이 기기 안에만 저장되기 때문이다.
     */
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /**
     * 이메일을 저장·비교하기 전에 다듬는다.
     * 앞뒤 공백을 없애고 소문자로 바꿔서 "Me@Mail.com"과 "me@mail.com"을 같은 계정으로 취급한다.
     */
    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun isValidEmail(email: String): Boolean = emailPattern.matches(normalizeEmail(email))

    fun isValidDisplayName(name: String): Boolean = name.trim().let { it.isNotEmpty() && it.length <= DISPLAY_NAME_MAX }

    /** 이메일 칸 검사: 빈칸 → 형식. 문제없으면 null */
    private fun checkEmail(email: String): AuthError? = when {
        email.isBlank() -> AuthError.EMAIL_EMPTY
        !isValidEmail(email) -> AuthError.EMAIL_INVALID
        else -> null
    }

    /** 비밀번호 칸 검사: 빈칸 → 길이. 비밀번호는 다듬지 않는다(공백도 한 글자). */
    private fun checkPassword(password: String): AuthError? = when {
        password.isEmpty() -> AuthError.PASSWORD_EMPTY
        password.length < PASSWORD_MIN -> AuthError.PASSWORD_TOO_SHORT
        else -> null
    }

    /**
     * 회원가입 입력값을 검사한다. 순서: 이름 → 이메일 → 비밀번호 → 비밀번호 확인 일치
     * @return 문제가 없으면 null, 있으면 가장 먼저 발견한 문제 하나
     */
    fun validateSignUp(
        displayName: String,
        email: String,
        password: String,
        passwordConfirm: String
    ): AuthError? {
        if (!isValidDisplayName(displayName)) return AuthError.DISPLAY_NAME_INVALID
        checkEmail(email)?.let { return it }
        checkPassword(password)?.let { return it }
        if (password != passwordConfirm) return AuthError.PASSWORD_MISMATCH
        return null
    }

    /** 프로필 수정 입력값을 검사한다. 순서: 이름 → 이메일 */
    fun validateProfile(displayName: String, email: String): AuthError? {
        if (!isValidDisplayName(displayName)) return AuthError.DISPLAY_NAME_INVALID
        return checkEmail(email)
    }

    /**
     * 로그인 입력값을 검사한다. 순서: 이메일 빈칸 → 이메일 형식 → 비밀번호 빈칸 → 비밀번호 길이.
     * 여기를 통과해야 DB를 찾아본다.
     */
    fun validateLogin(email: String, password: String): AuthError? =
        checkEmail(email) ?: checkPassword(password)
}
