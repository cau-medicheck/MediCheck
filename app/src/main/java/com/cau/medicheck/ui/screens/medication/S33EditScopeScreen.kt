package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.ApplyScope
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.RadioRow
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/**
 * S33 일정 변경 적용 범위 (v3 editScope()).
 *
 * 약을 수정할 때 "언제부터 적용할지"를 고른다. 약 수정 화면(S08)에서 "변경사항 저장"을 누르면 여기로 온다.
 *   오늘 이후 회차에 적용 : 오늘 아직 시각 전인 회차부터 새 설정
 *   내일부터 적용         : 오늘 회차는 옛 설정 그대로, 내일부터 새 설정
 * 어느 쪽이든 지난 날짜의 기록과 이미 응답한 기록은 그대로 남는다.
 * 이 화면은 S08의 ViewModel을 함께 쓴다. 그래서 "이전 화면"으로 돌아가면 입력해 둔 값이 그대로 있다.
 */
@Composable
fun S33EditScopeScreen(
    state: MedicationEditUiState,
    onBack: () -> Unit,
    onScopeChange: (ApplyScope) -> Unit,
    onSave: () -> Unit
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s33)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenIntro(stringResource(R.string.s33_subtitle), Modifier.padding(vertical = 16.dp))

        SurfaceCard {
            // 고친 새 이름을 보여 준다. 기다리는 수정이 없으면 "선택한 약"
            Text(
                text = state.name.trim().ifEmpty { stringResource(R.string.common_selected_med) },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
            Text(
                text = stringResource(R.string.s33_body),
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
            )
            // selectableGroup: TalkBack에게 "이 둘 중 하나를 고르는 묶음"이라고 알려 준다.
            Column(Modifier.selectableGroup()) {
                RadioRow(
                    title = stringResource(R.string.s33_scope_today_title),
                    description = stringResource(R.string.s33_scope_today_desc),
                    selected = state.scope == ApplyScope.FROM_TODAY,
                    onSelect = { onScopeChange(ApplyScope.FROM_TODAY) }
                )
                RadioRow(
                    title = stringResource(R.string.s33_scope_tomorrow_title),
                    description = stringResource(R.string.s33_scope_tomorrow_desc),
                    selected = state.scope == ApplyScope.FROM_TOMORROW,
                    onSelect = { onScopeChange(ApplyScope.FROM_TOMORROW) }
                )
            }
            McButton(
                text = stringResource(R.string.s33_btn_save),
                onClick = onSave,
                full = true,
                loading = state.isSaving,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

    }
}
