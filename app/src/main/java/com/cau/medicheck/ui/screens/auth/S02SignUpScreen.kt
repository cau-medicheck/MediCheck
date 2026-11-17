package com.cau.medicheck.ui.screens.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.AuthValidator
import com.cau.medicheck.ui.components.AuthLogo
import com.cau.medicheck.ui.components.AuthSub
import com.cau.medicheck.ui.components.AuthTitle
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McAppFrame
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.SegmentedTabs
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Gap22
import com.cau.medicheck.ui.theme.MediCheckTheme

/**
 * S02 회원가입. (v3 signup())
 *
 * 그리기만 한다. 입력 검사·저장은 onSignUp을 받은 ViewModel → AuthRepository가 하고,
 * 오류는 토스트로 알린다. 실패해도 입력값은 지우지 않는다.
 *
 * @param onSignUp "계정 만들기" 또는 비밀번호 확인 칸의 키보드 '완료'.
 *                 인자 순서는 AuthRepository.signUp과 같다(이름, 이메일, 비밀번호, 확인).
 * @param onGoToLogin "로그인" 탭
 */
@Composable
fun S02SignUpScreen(
    state: AuthUiState,
    onSignUp: (displayName: String, email: String, password: String, passwordConfirm: String) -> Unit,
    onGoToLogin: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }   // 비밀번호는 회전 시 저장하지 않는다(보안)
    var passwordConfirm by remember { mutableStateOf("") }   // 비밀번호는 회전 시 저장하지 않는다(보안)
    val focusManager = LocalFocusManager.current
    val busy = state.isLoading
    val submit = { if (!busy) onSignUp(name, email, password, passwordConfirm) }
    // 키보드 '다음' → 아래 칸으로
    val next = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })

    McAppFrame(screenId = Routes.S02, loggedIn = false) {
        McScreenColumn(paneTitle = stringResource(R.string.screen_title_s02)) {
            AuthLogo(
                symbol = Glyph.LOGO,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 24.dp, bottom = 16.dp)
            )
            AuthTitle(stringResource(R.string.auth_tab_signup))
            AuthSub(stringResource(R.string.s02_subtitle), Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(Gap22))

            SegmentedTabs(
                labels = listOf(stringResource(R.string.auth_tab_login), stringResource(R.string.auth_tab_signup)),
                selectedIndex = 1,
                onSelect = { index -> if (index == 0 && !busy) onGoToLogin() }
            )

            val nameLabel = stringResource(R.string.auth_label_name)
            FieldLabel(nameLabel)
            FormField(
                value = name,
                // v3 maxlength="30": 30자를 넘는 글자는 받아들이지 않는다
                onValueChange = { name = it.take(AuthValidator.DISPLAY_NAME_MAX) },
                placeholder = stringResource(R.string.auth_hint_name),
                label = nameLabel,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = next
            )

            val emailLabel = stringResource(R.string.auth_label_email)
            FieldLabel(emailLabel)
            FormField(
                value = email,
                onValueChange = { email = it },
                placeholder = stringResource(R.string.auth_hint_email),
                label = emailLabel,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = next
            )

            val passwordLabel = stringResource(R.string.auth_label_password)
            FieldLabel(passwordLabel)
            FormField(
                value = password,
                onValueChange = { password = it },
                placeholder = stringResource(R.string.auth_hint_password),
                label = passwordLabel,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = next,
                visualTransformation = PasswordVisualTransformation()
            )

            val confirmLabel = stringResource(R.string.auth_label_password_confirm)
            FieldLabel(confirmLabel)
            FormField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it },
                placeholder = stringResource(R.string.auth_hint_password_confirm),
                label = confirmLabel,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(Modifier.height(Gap22))
            McButton(
                text = stringResource(R.string.s02_submit),
                onClick = submit,
                kind = ButtonKind.PRIMARY,
                full = true,
                enabled = !busy,
                loading = state.pressed == AuthAction.SIGN_UP
            )
            Spacer(Modifier.height(Gap16))
            Notice(stringResource(R.string.s02_notice))
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun S02SignUpScreenPreview() {
    MediCheckTheme {
        S02SignUpScreen(
            state = AuthUiState(isCheckingSession = false),
            onSignUp = { _, _, _, _ -> },
            onGoToLogin = {}
        )
    }
}
