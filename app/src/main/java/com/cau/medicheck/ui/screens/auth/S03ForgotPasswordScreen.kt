package com.cau.medicheck.ui.screens.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.AuthLogo
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McAppFrame
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.ScreenTitle
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Gap22
import com.cau.medicheck.ui.theme.MediCheckTheme

/**
 * S03 비밀번호 재설정 안내. (v3 forgot())
 *
 * 계정이 이 기기 안에만 있어서 복구 메일을 보낼 수 없다. 그래서 이 화면은 이메일 형식만 확인하고
 * "다른 이메일로 새 계정을 만들어 달라"는 안내를 토스트로 보인다. 비밀번호를 바꾸는 기능은 없다
 * (본인 확인 수단이 없으므로, 확인 없이 바꾸게 하면 폰을 집은 누구나 남의 계정을 열 수 있다).
 *
 * @param onRequest "복구 안내 요청" 또는 키보드 '완료'
 * @param onBack "‹ 로그인 화면"
 */
@Composable
fun S03ForgotPasswordScreen(
    onRequest: (email: String) -> Unit,
    onBack: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }

    McAppFrame(screenId = Routes.S03, loggedIn = false) {
        McScreenColumn(paneTitle = stringResource(R.string.screen_title_s03)) {
            BackLink(label = stringResource(R.string.s03_back), onClick = onBack)
            Spacer(Modifier.height(Gap22))
            AuthLogo(
                symbol = Glyph.MAIL,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 24.dp, bottom = 16.dp)
            )
            // 제목은 왼쪽 정렬 (v3 header())
            ScreenTitle(
                title = stringResource(R.string.screen_title_s03),   // v3: 머리말 제목과 같은 글자
                subtitle = stringResource(R.string.s03_subtitle)
            )
            Spacer(Modifier.height(Gap22))

            val emailLabel = stringResource(R.string.s03_label_email)
            FieldLabel(emailLabel)
            FormField(
                value = email,
                onValueChange = { email = it },
                placeholder = stringResource(R.string.auth_hint_email),
                label = emailLabel,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onRequest(email) })
            )

            Spacer(Modifier.height(Gap22))
            McButton(
                text = stringResource(R.string.s03_submit),
                onClick = { onRequest(email) },
                kind = ButtonKind.PRIMARY,
                full = true
            )
            Spacer(Modifier.height(Gap16))
            Notice(stringResource(R.string.s03_notice), warn = true)
        }
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun S03ForgotPasswordScreenPreview() {
    MediCheckTheme { S03ForgotPasswordScreen(onRequest = {}, onBack = {}) }
}
