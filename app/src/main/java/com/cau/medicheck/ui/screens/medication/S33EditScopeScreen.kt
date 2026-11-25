package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.ApplyScope
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.McMedIcon
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.conditionLabel
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.CardBorder
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.Teal

/**
 * S33 일정 변경 적용 범위 (v3 editScope()). 2026-11-25 다시 그림: 묻는 말 하나 + 단추 둘. 단추를 누르면 그 범위로 바로 저장한다.
 *
 * 약을 수정할 때 "언제부터 적용할지"를 고른다. 약 수정 화면(S08)에서 "변경사항 저장"을 누르면 여기로 온다.
 *   오늘부터 : 오늘 아직 시각 전인 회차부터 새 설정
 *   내일부터 : 오늘 회차는 옛 설정 그대로, 내일부터 새 설정
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
    // 범위를 고르고 곧바로 저장한다 (scope는 상태에 바로 들어가므로 saveWithScope가 새 값을 읽는다)
    val choose = { scope: ApplyScope -> if (!state.isSaving) { onScopeChange(scope); onSave() } }

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s33)) {
        BackLink(stringResource(R.string.s33_back), onBack)
        Text(
            stringResource(R.string.s33_question),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp).semantics { heading() }
        )

        // 고친 약 한 줄: 아이콘 + 새 이름 + "매일 13:00 식후로 바뀌어요"
        val shape = RoundedCornerShape(18.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White)
                .border(1.dp, CardBorder, shape)
                .padding(14.dp)
        ) {
            McMedIcon(state.shape, state.color, size = 44.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    state.name.trim().ifEmpty { stringResource(R.string.common_selected_med) },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy
                )
                if (state.times.isNotEmpty()) {
                    val times = state.times.sortedBy { it.time }.joinToString(" · ") { row ->
                        timeText(row.time)
                    }
                    val cond = state.times.map { it.condition }.distinct().singleOrNull()?.takeIf { it != DoseCondition.NONE }
                    val label = if (cond != null) "$times ${conditionLabel(cond)}" else times
                    Text(stringResource(R.string.s33_changes_to, label), fontSize = 12.5.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        // 단추 둘: 누르면 바로 저장
        Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(top = 28.dp)) {
            ScopeButton(stringResource(R.string.s33_scope_today_title), primary = true, busy = state.isSaving && state.scope == ApplyScope.FROM_TODAY) { choose(ApplyScope.FROM_TODAY) }
            ScopeButton(stringResource(R.string.s33_scope_tomorrow_title), primary = false, busy = state.isSaving && state.scope == ApplyScope.FROM_TOMORROW) { choose(ApplyScope.FROM_TOMORROW) }
        }
        Text(
            stringResource(R.string.s33_caption),
            fontSize = 12.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
    }
}

@Composable
private fun ScopeButton(text: String, primary: Boolean, busy: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (primary) Teal else Color.White)
            .then(if (primary) Modifier else Modifier.border(1.dp, Color(0xFFD5E2E4), shape))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        if (busy) CircularProgressIndicator(color = if (primary) Color.White else Teal, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        else Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (primary) Color.White else Teal)
    }
}
