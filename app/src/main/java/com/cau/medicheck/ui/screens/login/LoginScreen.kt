package com.cau.medicheck.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.AuthError
import com.cau.medicheck.ui.theme.MediCheckTheme

/**
 * 로그인 화면.
 *
 * 이 화면은 "그리기"만 한다. 실제 로그인은 밖에서 받은 onLogin을 부를 뿐이다.
 *   - state: 지금 상태 (처리 중인지, 오류가 있는지)
 *   - onLogin: 로그인 버튼을 눌렀을 때 부를 함수
 *   - onGoToSignUp: "계정 만들기"를 눌렀을 때 부를 함수
 *   - onInputChanged: 입력을 고쳤을 때 부를 함수 (이전 오류 문구를 지우는 데 쓴다)
 */
@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (username: String, password: String) -> Unit,
    onGoToSignUp: () -> Unit,
    onInputChanged: () -> Unit
) {
    // 입력칸에 적힌 글자. rememberSaveable: 화면을 돌려도 적은 글자가 남는다.
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val submit = { onLogin(username, password) }

    Scaffold { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()                              // 키보드가 올라오면 그만큼 위로 민다
                .verticalScroll(rememberScrollState())     // 화면이 작으면 스크롤된다
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            AuthTextField(
                value = username,
                onValueChange = { username = it; onInputChanged() },
                labelRes = R.string.label_username,
                enabled = !state.isLoading
            )
            Spacer(Modifier.height(12.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it; onInputChanged() },
                labelRes = R.string.label_password,
                isPassword = true,
                isLast = true,
                enabled = !state.isLoading,
                onDone = submit
            )

            Spacer(Modifier.height(8.dp))
            AuthErrorText(state.error)
            Spacer(Modifier.height(16.dp))

            AuthSubmitButton(
                textRes = R.string.action_login,
                isLoading = state.isLoading,
                onClick = submit
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onGoToSignUp, enabled = !state.isLoading) {
                Text(stringResource(R.string.action_go_to_sign_up))
            }
        }
    }
}

// 아래는 Android Studio에서 화면을 미리 보기 위한 코드다. 앱 실행에는 쓰이지 않는다.

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MediCheckTheme(dynamicColor = false) {
        LoginScreen(
            state = AuthUiState(isCheckingSession = false),
            onLogin = { _, _ -> },
            onGoToSignUp = {},
            onInputChanged = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenErrorPreview() {
    MediCheckTheme(dynamicColor = false) {
        LoginScreen(
            state = AuthUiState(isCheckingSession = false, error = AuthError.WRONG_CREDENTIALS),
            onLogin = { _, _ -> },
            onGoToSignUp = {},
            onInputChanged = {}
        )
    }
}
