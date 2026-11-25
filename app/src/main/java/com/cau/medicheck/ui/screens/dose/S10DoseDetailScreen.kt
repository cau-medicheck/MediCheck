package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.domain.MedicationColor
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.MedShapeArt
import com.cau.medicheck.ui.components.StatusPill
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.conditionLabel
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.components.toColor
import com.cau.medicheck.ui.theme.CardBackground
import com.cau.medicheck.ui.theme.CardBorder
import com.cau.medicheck.ui.theme.FieldBorder
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText
import com.cau.medicheck.ui.theme.Teal

/** 회차 상세 화면의 버튼들이 부를 함수 묶음 */
class DoseDetailActions(
    val onBack: () -> Unit,
    val onMark: (DoseStatus) -> Unit,
    val onUndo: () -> Unit,
    val onOpenActualTime: () -> Unit,
    val onOpenHistory: () -> Unit,
    val onOpenSchedule: () -> Unit
)

/**
 * S10 회차 상세/상태 (v3 doseDetail()).
 *
 * 회차 하나의 상태를 복용 완료 / 건너뜀 / 미기록으로 바꾸고, 마지막 변경을 되돌릴 수 있다.
 * 내일 이후 회차는 보기 전용이다: 주황 안내가 보이고 상태 버튼과 "실제 복용 시각 기록"이 눌리지 않는다.
 */
@Composable
fun S10DoseDetailScreen(state: DoseDetailUiState, actions: DoseDetailActions) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s10)) {
        val dose = state.dose
        when {
            state.isLoading -> Unit   // 회차를 찾는 잠깐 동안은 아무것도 그리지 않는다
            // 어느 회차인지 없이 열렸고 대신 보여 줄 회차도 없을 때
            dose == null -> EmptyState(
                emoji = Glyph.PILL,
                title = stringResource(R.string.s10_empty_title),
                body = stringResource(R.string.s10_empty_body),
                actionLabel = stringResource(R.string.s10_empty_action),
                onAction = actions.onOpenSchedule
            )
            else -> DoseDetailBody(dose, state, actions)
        }
    }
}

@Composable
private fun DoseDetailBody(dose: DoseItem, state: DoseDetailUiState, actions: DoseDetailActions) {
    BackLink(stringResource(R.string.s10_back), actions.onBack)

    // ----- 머리 카드: 약 색으로 옅게 칠한 바탕 + 아이콘 + 이름·상태 + "1 cap · 11월 24일 화요일 · 12:30 식후" (약 상세와 같은 모양) -----
    val medColor = (state.color ?: MedicationColor.TEAL).toColor()
    val cardShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(cardShape)
            .background(CardBackground)
            .border(1.dp, CardBorder, cardShape)
            .background(Brush.linearGradient(listOf(medColor.copy(alpha = 0.16f), medColor.copy(alpha = 0.05f))))
    ) {
        val shape = state.shape
        val color = state.color
        if (shape != null && color != null) {
            MedShapeArt(shape, color, Modifier.align(Alignment.TopEnd).offset(x = 14.dp, y = (-14).dp).size(104.dp).alpha(0.18f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(18.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(3.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                if (shape != null && color != null) MedShapeArt(shape, color, Modifier.size(34.dp))
                else Avatar(Glyph.DOSE, size = 56.dp, fontSize = 26)
            }
            Column(Modifier.padding(start = 13.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dose.medicationName,
                        fontSize = 22.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                        color = Navy,
                        modifier = Modifier.weight(1f, fill = false).semantics { heading() }
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusPill(state.display)
                }
                val condition = if (dose.condition == DoseCondition.NONE) "" else " " + conditionLabel(dose.condition)
                Text(
                    text = stringResource(R.string.s10_subtitle, dose.dosage, fullDateText(dose.date), timeText(dose.time)) + condition,
                    fontSize = 12.5.sp,
                    color = ParagraphText,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }

    // ----- 상태 단추 3개를 한 줄로. 지금 상태만 청록. 앞날 회차는 복용 완료만 막는다(미리 건너뛰기는 된다) -----
    Text(
        text = stringResource(R.string.s10_section_state),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Navy,
        modifier = Modifier.padding(start = 2.dp, top = 20.dp, bottom = 10.dp).semantics { heading() }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
        StateTile("✓", R.string.s10_tile_taken, DoseStatus.TAKEN, dose.status, enabled = !state.isFuture, onMark = actions.onMark, modifier = Modifier.weight(1f))
        StateTile("–", R.string.s10_tile_skipped, DoseStatus.SKIPPED, dose.status, enabled = true, onMark = actions.onMark, modifier = Modifier.weight(1f))
        StateTile("○", R.string.s10_tile_pending, DoseStatus.PENDING, dose.status, enabled = true, onMark = actions.onMark, modifier = Modifier.weight(1f))
    }

    // ----- 작은 줄: 실제 복용 시각 · 변경 이력 · 메모 -----
    SurfaceCard(Modifier.padding(top = 14.dp), padding = 0.dp) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            DetailRow(
                label = stringResource(R.string.s10_row_actual),
                value = state.actualTime?.let { timeText(it) + " ›" } ?: stringResource(R.string.s10_actual_add),
                onClick = if (state.isFuture) null else actions.onOpenActualTime
            )
            HorizontalDivider(color = Line)
            DetailRow(stringResource(R.string.s10_row_history), stringResource(R.string.s10_history_open), actions.onOpenHistory)
            HorizontalDivider(color = Line)
            DetailRow(stringResource(R.string.s10_row_memo), dose.memo.ifBlank { stringResource(R.string.s07_no_memo) }, null)
        }
    }

    // 마지막 변경 되돌리기 (글자 단추). 되돌릴 변경이 있을 때만 (2026-11-25). 맨 아래라 숨겨도 위가 움직이지 않는다
    if (state.canUndo) Text(
        text = stringResource(R.string.s10_undo_text),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Muted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = actions.onUndo)
            .padding(vertical = 14.dp)
    )
}

/** 상태 단추 하나: 위 기호, 아래 이름. 지금 상태면 청록 바탕 */
@Composable
private fun StateTile(
    symbol: String,
    labelRes: Int,
    target: DoseStatus,
    current: DoseStatus,
    enabled: Boolean,
    onMark: (DoseStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val on = target == current
    val shape = RoundedCornerShape(14.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(if (on) Teal else Color.White)
            .border(1.5.dp, if (on) Teal else FieldBorder, shape)
            .selectable(selected = on, enabled = enabled, role = Role.RadioButton, onClick = { onMark(target) })
            .padding(vertical = 12.dp)
    ) {
        Text(symbol, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else Navy)
        Text(stringResource(labelRes), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else Navy, modifier = Modifier.padding(top = 2.dp))
    }
}

/** 작은 줄: 왼쪽 이름, 오른쪽 값. onClick이 있으면 줄 전체를 누를 수 있고 값이 청록 */
@Composable
private fun DetailRow(label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 14.dp)
    ) {
        Text(label, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(end = 12.dp))
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (onClick != null) Teal else Navy,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
