package com.cau.medicheck.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.cau.medicheck.ui.theme.MediCheckTheme

/**
 * 회원가입 화면. LoginScreen과 같은 방식으로, 그리기만 하고 실제 가입은 onSignUp에 맡긴다.
 * 입력값 검사(길이, 비밀번호 일치 등)는 여기서 하지 않고 AuthValidator가 한다.
 */
@Composable
fun SignUpScreen(
    state: AuthUiState,
    onSignUp: (username: String, password: String, passwordConfirm: String, displayName: String) -> Unit,
    onBackToLogin: () -> Unit,
    onInputChanged: () -> Unit
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordConfirm by rememberSaveable { mutableStateOf("") }

    val submit = { onSignUp(username, password, passwordConfirm, displayName) }

    Scaffold { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.signup_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            AuthTextField(
                value = displayName,
                onValueChange = { displayName = it; onInputChanged() },
                labelRes = R.string.label_display_name,
                capitalizeWords = true,
                enabled = !state.isLoading
            )
            Spacer(Modifier.height(12.dp))
            AuthTextField(
                value = username,
                onValueChange = { username = it; onInputChanged() },
                labelRes = R.string.label_username,
                hintRes = R.string.hint_username,
                enabled = !state.isLoading
            )
            Spacer(Modifier.height(4.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it; onInputChanged() },
                labelRes = R.string.label_password,
                hintRes = R.string.hint_password,
                isPassword = true,
                enabled = !state.isLoading
            )
            Spacer(Modifier.height(4.dp))
            AuthTextField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it; onInputChanged() },
                labelRes = R.string.label_password_confirm,
                isPassword = true,
                isLast = true,
                enabled = !state.isLoading,
                onDone = submit
            )

            Spacer(Modifier.height(8.dp))
            AuthErrorText(state.error)
            Spacer(Modifier.height(16.dp))

            AuthSubmitButton(
                textRes = R.string.action_sign_up,
                isLoading = state.isLoading,
                onClick = submit
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBackToLogin, enabled = !state.isLoading) {
                Text(stringResource(R.string.action_back_to_login))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    MediCheckTheme(dynamicColor = false) {
        SignUpScreen(
            state = AuthUiState(isCheckingSession = false),
            onSignUp = { _, _, _, _ -> },
            onBackToLogin = {},
            onInputChanged = {}
        )
    }
}
