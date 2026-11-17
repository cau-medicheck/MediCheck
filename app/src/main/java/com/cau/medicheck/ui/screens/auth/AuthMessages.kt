package com.cau.medicheck.ui.screens.auth

import androidx.annotation.StringRes
import com.cau.medicheck.R
import com.cau.medicheck.domain.AuthError
import com.cau.medicheck.domain.AuthValidator

/*
 * 로그인 전 화면(S01~S03)이 띄우는 토스트 문구를 고르는 규칙.
 * 화면이나 ViewModel 안에 섞어 두지 않고 순수 함수로 떼어 내서, 폰 없이 단위 테스트한다.
 */

/** 오류 종류(AuthError)에 맞는 문구 번호를 strings.xml에서 고른다. */
@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.EMAIL_EMPTY -> R.string.error_email_empty
    AuthError.EMAIL_INVALID -> R.string.error_email_invalid
    AuthError.PASSWORD_EMPTY -> R.string.error_password_empty
    AuthError.PASSWORD_TOO_SHORT -> R.string.error_password_too_short
    AuthError.PASSWORD_MISMATCH -> R.string.error_password_mismatch
    AuthError.DISPLAY_NAME_INVALID -> R.string.error_display_name_invalid
    AuthError.EMAIL_TAKEN -> R.string.error_email_taken
    AuthError.WRONG_CREDENTIALS -> R.string.error_wrong_credentials
    AuthError.SAMPLE_FAILED -> R.string.error_sample_failed
}

/**
 * S03 "복구 안내 요청"을 눌렀을 때의 토스트 문구.
 * 빈칸 → 형식 순서로 검사하고, 통과하면 가입 여부와 상관없이 같은 안내를 보인다.
 * (어떤 이메일이 가입돼 있는지 남에게 알려 주지 않기 위해 DB를 찾아보지 않는다.)
 */
@StringRes
fun passwordHelpMessageRes(email: String): Int = when {
    email.isBlank() -> R.string.error_email_empty
    !AuthValidator.isValidEmail(email) -> R.string.error_email_invalid
    else -> R.string.toast_password_help
}
