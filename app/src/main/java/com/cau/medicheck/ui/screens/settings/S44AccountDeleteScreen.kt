package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.ScreenTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.DangerTitle
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.ParagraphText

/**
 * S44 계정·데이터 삭제 (v3 accountDelete()).
 *
 * 지금 로그인한 계정과 그 계정의 데이터를 지우는 화면. 실수로 지우지 않도록 확인 칸에 '삭제'를 입력해야 한다.
 * 이 화면은 입력한 글자만 들고 있고, 검사와 삭제는 AuthViewModel.deleteAccount가 한다
 * (글자가 틀리면 토스트만 뜨고 입력은 그대로 남는다).
 *
 * @param deleting 삭제 처리 중인지 (버튼에 돌아가는 표시, 다시 눌리지 않음)
 * @param onDelete 확인 칸에 입력한 글자를 넘겨 삭제를 요청하는 함수
 */
@Composable
fun S44AccountDeleteScreen(deleting: Boolean, onDelete: (String) -> Unit, onBack: () -> Unit) {
    var confirmText by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        if (!deleting) {
            // 삭제에 성공하면 곧바로 온보딩 화면으로 바뀐다. 키보드가 그 화면 위에 남지 않게 먼저 내린다.
            keyboard?.hide()
            onDelete(confirmText)
        }
    }

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s44)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenTitle(
            title = stringResource(R.string.s44_title),
            subtitle = stringResource(R.string.s44_subtitle),
            modifier = Modifier.padding(vertical = Gap16)
        )

        SurfaceCard {
            Text(
                text = stringResource(R.string.s44_card_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DangerTitle,
                // heading: TalkBack이 이 글자를 "제목"으로 읽는다.
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.s44_card_body),
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = ParagraphText,
                modifier = Modifier.padding(top = 10.dp)
            )

            val label = stringResource(R.string.s44_label)
            FieldLabel(label)
            FormField(
                value = confirmText,
                onValueChange = { confirmText = it },
                placeholder = stringResource(R.string.s44_confirm_word),
                label = label,
                enabled = !deleting,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                // 키보드의 '완료' 키는 키보드만 내린다. 되돌릴 수 없는 삭제는 아래 빨간 버튼을 직접 눌러야만 실행한다.
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() })
            )

            Spacer(Modifier.height(Gap16))
            McButton(
                text = stringResource(R.string.s44_button),
                onClick = submit,
                kind = ButtonKind.RED,
                full = true,
                loading = deleting
            )
        }

        Notice(stringResource(R.string.s44_notice), Modifier.padding(top = Gap16), warn = true)
    }
}
