package com.cau.medicheck.ui.screens.login

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.AuthError

/*
 * 로그인 화면과 회원가입 화면이 함께 쓰는 작은 부품들.
 * 같은 모양의 입력칸·버튼을 두 화면에 따로 쓰지 않으려고 여기로 뺐다.
 */

/** 오류 종류(AuthError)에 맞는 문구를 strings.xml에서 고른다. */
@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.USERNAME_INVALID -> R.string.error_username_invalid
    AuthError.PASSWORD_TOO_SHORT -> R.string.error_password_too_short
    AuthError.PASSWORD_MISMATCH -> R.string.error_password_mismatch
    AuthError.DISPLAY_NAME_INVALID -> R.string.error_display_name_invalid
    AuthError.USERNAME_TAKEN -> R.string.error_username_taken
    AuthError.WRONG_CREDENTIALS -> R.string.error_wrong_credentials
    AuthError.EMPTY_FIELDS -> R.string.error_empty_fields
}

/**
 * 입력칸 하나.
 * @param isPassword true면 입력한 글자를 점(•)으로 가린다.
 * @param capitalizeWords true면 단어 첫 글자를 대문자로 올려 준다 (이름 칸에 쓴다).
 * @param isLast 마지막 칸이면 키보드의 버튼이 "완료"가 되고, 누르면 onDone이 불린다.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier,
    @StringRes hintRes: Int? = null,
    isPassword: Boolean = false,
    capitalizeWords: Boolean = false,
    isLast: Boolean = false,
    enabled: Boolean = true,
    onDone: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        supportingText = hintRes?.let { { Text(stringResource(it)) } },
        singleLine = true,
        enabled = enabled,
        visualTransformation =
            if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            // 아이디·비밀번호는 키보드가 첫 글자를 대문자로 바꾸거나 자동 수정하지 않게 한다.
            capitalization =
                if (capitalizeWords) KeyboardCapitalization.Words else KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
            imeAction = if (isLast) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier.fillMaxWidth()
    )
}

/** 오류 문구. 오류가 없으면 아무것도 그리지 않는다. */
@Composable
fun AuthErrorText(error: AuthError?, modifier: Modifier = Modifier) {
    if (error == null) return
    Text(
        text = stringResource(error.messageRes()),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

/** 큰 버튼. 처리 중(isLoading)이면 글자 대신 돌아가는 표시를 보여 주고 눌리지 않는다. */
@Composable
fun AuthSubmitButton(
    @StringRes textRes: Int,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(stringResource(textRes))
        }
    }
}
