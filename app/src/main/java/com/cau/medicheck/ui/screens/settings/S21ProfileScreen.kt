package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.AuthValidator
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Gap22
import com.cau.medicheck.ui.theme.HeroDeepEnd
import com.cau.medicheck.ui.theme.HeroDeepStart
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

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
        ScreenIntro(modifier = Modifier.padding(top = Gap16))
        // 가운데 청록 동그라미 + 이름·이메일. 입력하는 대로 바로 바뀐다 (2026-11-25 시안 B)
        ProfileBadge(nameInput.trim(), emailInput.trim(), Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, bottom = 6.dp))

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

        // 아래 단추 둘: 취소(흰) · 저장(청록). 시안 비율 1 : 1.12
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(top = Gap22)) {
            McButton(stringResource(R.string.action_cancel), onBack, Modifier.weight(1f), ButtonKind.WHITE, full = true, enabled = !saving)
            McButton(stringResource(R.string.s21_save), submit, Modifier.weight(1.12f), full = true, loading = saving)
        }
    }
}

/** 청록 동그라미(이름 첫 글자) + 아래 "이름님" · 이메일. 이름이 비어 있으면 동그라미는 비워 둔다 */
@Composable
private fun ProfileBadge(name: String, email: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.clearAndSetSemantics { }) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(HeroDeepStart, HeroDeepEnd)))
        ) {
            Text(name.take(1), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
        if (name.isNotEmpty()) {
            Text(stringResource(R.string.s19_name, name), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Navy, modifier = Modifier.padding(top = 9.dp))
        }
        Text(email, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
    }
}
