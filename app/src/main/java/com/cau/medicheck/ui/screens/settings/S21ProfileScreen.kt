package com.cau.medicheck.ui.screens.settings

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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.AuthValidator
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Gap22

/**
 * S21 프로필 관리 (v3 profile()).
 *
 * 표시 이름과 이메일(로그인할 때 쓰는 아이디)을 고친다.
 * 입력 중인 글자는 이 화면이 들고 있고(rememberSaveable = 화면을 돌려도 남는 기억),
 * 검사와 저장은 AuthViewModel.updateProfile → AuthRepository.updateProfile이 한다.
 * 저장에 실패하면 토스트만 뜨고 입력은 그대로 남는다.
 *
 * @param displayName 지금 표시 이름 (입력칸의 처음 값)
 * @param email 지금 이메일 (입력칸의 처음 값)
 * @param saving 저장 처리 중인지 (버튼에 돌아가는 표시, 다시 눌리지 않음)
 * @param onSave (이름, 이메일)을 넘겨 저장을 요청하는 함수
 */
@Composable
fun S21ProfileScreen(
    displayName: String,
    email: String,
    saving: Boolean,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var nameInput by rememberSaveable { mutableStateOf(displayName) }
    var emailInput by rememberSaveable { mutableStateOf(email) }
    val focusManager = LocalFocusManager.current
    val submit = { if (!saving) onSave(nameInput, emailInput) }

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s21)) {
        BackLink(stringResource(R.string.s20_back), onBack)
        ScreenIntro(stringResource(R.string.s21_subtitle), Modifier.padding(top = Gap16))
        Spacer(Modifier.height(Gap22))
        Avatar(Glyph.PROFILE, Modifier.align(Alignment.CenterHorizontally), size = 70.dp, fontSize = 34)

        val nameLabel = stringResource(R.string.s21_label_name)
        FieldLabel(nameLabel)
        FormField(
            value = nameInput,
            // v3 maxlength="30": 30자를 넘는 글자는 받아들이지 않는다 (붙여 넣어도 앞 30자만)
            onValueChange = { nameInput = it.take(AuthValidator.DISPLAY_NAME_MAX) },
            label = nameLabel,
            enabled = !saving,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        val emailLabel = stringResource(R.string.auth_label_email)
        FieldLabel(emailLabel)
        FormField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = emailLabel,
            enabled = !saving,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            // 키보드의 '완료' 키로도 저장한다.
            keyboardActions = KeyboardActions(onDone = { submit() })
        )

        Spacer(Modifier.height(Gap22))
        McButton(text = stringResource(R.string.s21_save), onClick = submit, full = true, loading = saving)
        Notice(stringResource(R.string.s21_notice), Modifier.padding(top = Gap16))
    }
}
