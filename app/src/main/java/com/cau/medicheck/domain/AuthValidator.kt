package com.cau.medicheck.domain

/**
 * 회원가입·로그인이 실패하는 이유의 목록.
 * 화면에 보여 줄 문장은 여기 두지 않고, 화면 쪽에서 이 값에 맞는 문장을 고른다.
 */
enum class AuthError {
    USERNAME_INVALID,       // 아이디 형식이 맞지 않음
    PASSWORD_TOO_SHORT,     // 비밀번호가 너무 짧음
    PASSWORD_MISMATCH,      // 비밀번호와 비밀번호 확인이 다름
    DISPLAY_NAME_INVALID,   // 이름이 비었거나 너무 김
    USERNAME_TAKEN,         // 이미 있는 아이디
    WRONG_CREDENTIALS,      // 아이디가 없거나 비밀번호가 틀림
    EMPTY_FIELDS            // 로그인 칸이 비어 있음
}

/**
 * 입력값이 규칙에 맞는지 검사한다. 순수 Kotlin 코드라서 단위 테스트를 할 수 있다.
 */
object AuthValidator {

    const val USERNAME_MIN = 4
    const val USERNAME_MAX = 20
    const val PASSWORD_MIN = 8
    const val DISPLAY_NAME_MAX = 20

    /** 아이디: 영문 소문자·숫자·밑줄만, 4~20자 */
    private val usernamePattern = Regex("^[a-z0-9_]{$USERNAME_MIN,$USERNAME_MAX}$")

    /**
     * 아이디를 저장·비교하기 전에 다듬는다.
     * 앞뒤 공백을 없애고 소문자로 바꿔서 "Suhwan"과 "suhwan"을 같은 아이디로 취급한다.
     */
    fun normalizeUsername(username: String): String = username.trim().lowercase()

    /**
     * 회원가입 입력값을 검사한다.
     * @return 문제가 없으면 null, 있으면 가장 먼저 발견한 문제 하나
     */
    fun validateSignUp(
        username: String,
        password: String,
        passwordConfirm: String,
        displayName: String
    ): AuthError? {
        val name = displayName.trim()
        return when {
            name.isEmpty() || name.length > DISPLAY_NAME_MAX -> AuthError.DISPLAY_NAME_INVALID
            !usernamePattern.matches(normalizeUsername(username)) -> AuthError.USERNAME_INVALID
            password.length < PASSWORD_MIN -> AuthError.PASSWORD_TOO_SHORT
            password != passwordConfirm -> AuthError.PASSWORD_MISMATCH
            else -> null
        }
    }

    /**
     * 로그인 입력값을 검사한다. 칸이 비어 있는지만 본다.
     * (형식이 틀린 아이디는 어차피 DB에 없으므로 "아이디 또는 비밀번호가 틀림"으로 처리된다.)
     */
    fun validateLogin(username: String, password: String): AuthError? =
        if (username.isBlank() || password.isEmpty()) AuthError.EMPTY_FIELDS else null
}
