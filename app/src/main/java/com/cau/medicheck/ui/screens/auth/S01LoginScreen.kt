package com.cau.medicheck.ui.screens.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.LinkText
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.MediCheckTheme

/**
 * S01 로그인. (v3 login())
 *
 * 이 화면은 그리기만 한다. 로그인·샘플 계정·이동은 밖에서 받은 함수들이 한다.
 * 입력 오류는 화면에 글씨로 쓰지 않고 ViewModel이 토스트로 알린다(v3와 같은 방식).
 *
 * @param state 처리 중인지, 어느 버튼을 눌렀는지
 * @param onLogin "로그인" 또는 비밀번호 칸의 키보드 '완료'
 * @param onSample "샘플 계정으로 체험하기"
 * @param onGoToSignUp "회원가입" 탭
 * @param onForgot "비밀번호를 잊으셨나요?"
 */
@Composable
fun S01LoginScreen(
    state: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onSample: () -> Unit,
    onGoToSignUp: () -> Unit,
    onForgot: () -> Unit
) {
    // 입력칸의 글자. rememberSaveable: 화면을 돌려도, S02에 갔다 와도 적은 글자가 남는다.
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }   // 비밀번호는 회전 시 저장하지 않는다(보안)
    val focusManager = LocalFocusManager.current
    val busy = state.isLoading
    // 처리 중(비밀번호 해시 계산 중)에는 다른 화면으로 가는 글자 버튼·탭이 눌려도 무시한다.
    // (LinkText·SegmentedTabs에는 enabled 인자가 없어서 여기서 막는다)
    val submit = { if (!busy) onLogin(email, password) }

    AuthFrame(
        paneTitle = stringResource(R.string.screen_title_s01),
        tagline = stringResource(R.string.s01_tagline),
        footer = {
            AuthFooterLink(
                question = stringResource(R.string.s01_no_account),
                action = stringResource(R.string.auth_tab_signup),
                onClick = { if (!busy) onGoToSignUp() }
            )
        }
    ) {
        AuthCardTitle(stringResource(R.string.auth_tab_login))
        val emailLabel = stringResource(R.string.auth_label_email)
        FieldLabel(emailLabel)
        FormField(
            value = email,
            onValueChange = { email = it },
            placeholder = stringResource(R.string.auth_hint_email),
            label = emailLabel,
            enabled = !busy,
            // 이메일 키보드, 첫 글자 자동 대문자·자동 수정 끔, 키보드 버튼 '다음' → 비밀번호 칸으로
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
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
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            visualTransformation = PasswordVisualTransformation()   // 글자를 점(•)으로 가린다
        )

        // 오른쪽 정렬 링크 (v3: text-align:right; margin-top:10px)
        Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.CenterEnd) {
            LinkText(
                text = stringResource(R.string.s01_forgot),
                onClick = { if (!busy) onForgot() }
            )
        }
        Spacer(Modifier.height(Gap16))

        McButton(
            text = stringResource(R.string.s01_submit),
            onClick = submit,
            kind = ButtonKind.PRIMARY,
            full = true,
            enabled = !busy,
            loading = state.pressed == AuthAction.LOGIN
        )
        Spacer(Modifier.height(Gap16))
        McButton(
            text = stringResource(R.string.s01_sample),
            onClick = { if (!busy) onSample() },
            kind = ButtonKind.LIGHT,
            full = true,
            enabled = !busy,
            loading = state.pressed == AuthAction.SAMPLE
        )
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun S01LoginScreenPreview() {
    MediCheckTheme {
        S01LoginScreen(
            state = AuthUiState(isCheckingSession = false),
            onLogin = { _, _ -> }, onSample = {}, onGoToSignUp = {}, onForgot = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 860, name = "로그인 처리 중")
@Composable
private fun S01LoginScreenLoadingPreview() {
    MediCheckTheme {
        S01LoginScreen(
            state = AuthUiState(isCheckingSession = false, isLoading = true, pressed = AuthAction.LOGIN),
            onLogin = { _, _ -> }, onSample = {}, onGoToSignUp = {}, onForgot = {}
        )
    }
}
