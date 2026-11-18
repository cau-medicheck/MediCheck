package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.OptionRow
import com.cau.medicheck.ui.components.ProgressBar
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.StatusPill
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText

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

    // ----- 회차 카드: 아이콘, 약 이름, "1정 · 11월 16일 월요일 · 08:00", 배지, 진행 막대 -----
    SurfaceCard(Modifier.padding(top = 16.dp), padding = 0.dp) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 25.dp)
        ) {
            Avatar(Glyph.DOSE, size = 72.dp, fontSize = 33)
            Text(
                text = dose.medicationName,
                fontSize = 23.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.9).sp,
                color = Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .semantics { heading() }
            )
            Text(
                text = stringResource(R.string.s10_subtitle, dose.dosage, fullDateText(dose.date), timeText(dose.time)),
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = ParagraphText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            StatusPill(state.display)
            if (state.actualTime != null) {
                Text(
                    text = stringResource(R.string.s10_actual_time, timeText(state.actualTime)),
                    fontSize = 11.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            // 진행 막대는 배지와 같은 정보라 TalkBack이 읽지 않게 한다.
            ProgressBar(
                fraction = when (dose.status) {
                    DoseStatus.TAKEN -> 1f
                    DoseStatus.SKIPPED -> 0.2f
                    DoseStatus.PENDING -> 0f
                },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clearAndSetSemantics { }
            )
        }
    }

    // ----- 상태 버튼 3개. 지금 상태인 버튼만 청록이고 뒤에 "· 현재 상태"가 붙는다 -----
    SectionTitle(stringResource(R.string.s10_section_state))
    SurfaceCard {
        StateButton(R.string.s10_btn_taken, DoseStatus.TAKEN, dose.status, !state.isFuture, actions.onMark)
        StateButton(R.string.s10_btn_skipped, DoseStatus.SKIPPED, dose.status, !state.isFuture, actions.onMark)
        StateButton(R.string.s10_btn_pending, DoseStatus.PENDING, dose.status, !state.isFuture, actions.onMark)
        if (state.isFuture) {
            Notice(stringResource(R.string.s10_notice_future), Modifier.padding(bottom = 9.dp), warn = true)
        }
        Notice(stringResource(R.string.s10_notice_pending))
    }

    // ----- 다른 화면으로 가는 줄 -----
    SurfaceCard(Modifier.padding(top = 16.dp), padding = 0.dp) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            OptionRow(
                title = stringResource(R.string.s10_row_actual_title),
                description = stringResource(R.string.s10_row_actual_desc),
                onClick = actions.onOpenActualTime,
                enabled = !state.isFuture
            )
            OptionRow(
                title = stringResource(R.string.s10_row_history_title),
                description = stringResource(R.string.s10_row_history_desc),
                onClick = actions.onOpenHistory,
                showDivider = false
            )
        }
    }

    McButton(
        text = stringResource(R.string.s10_btn_undo),
        onClick = actions.onUndo,
        kind = ButtonKind.WHITE,
        full = true,
        modifier = Modifier.padding(top = 16.dp)
    )

    // 복용 메모 (기록을 만들 때 찍어 둔 메모)
    Notice(
        text = dose.memo.ifBlank { stringResource(R.string.s10_no_memo) },
        modifier = Modifier.padding(top = 16.dp)
    )
}

/** 상태 버튼 하나. 지금 상태면 청록 + "· 현재 상태", 아니면 흰 버튼. */
@Composable
private fun StateButton(
    labelRes: Int,
    target: DoseStatus,
    current: DoseStatus,
    enabled: Boolean,
    onMark: (DoseStatus) -> Unit
) {
    val isCurrent = target == current
    val label = stringResource(labelRes)
    McButton(
        text = if (isCurrent) label + " " + stringResource(R.string.s10_current_suffix) else label,
        onClick = { onMark(target) },
        kind = if (isCurrent) ButtonKind.PRIMARY else ButtonKind.WHITE,
        full = true,
        enabled = enabled,
        modifier = Modifier.padding(bottom = 9.dp)
    )
}
