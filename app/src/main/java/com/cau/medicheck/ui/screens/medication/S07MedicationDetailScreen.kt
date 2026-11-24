package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.theme.DangerTitle
import com.cau.medicheck.ui.components.timeWithCondition
import com.cau.medicheck.ui.components.McSwitch
import androidx.compose.foundation.selection.toggleable
import com.cau.medicheck.domain.DayMark
import com.cau.medicheck.domain.DoseCounts
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.MedicationDetail
import com.cau.medicheck.domain.RecentDay
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McMedIcon
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.daysText
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.components.weekdayText
import com.cau.medicheck.ui.theme.HomeChipDoneBg
import com.cau.medicheck.ui.theme.HomeChipDoneText
import com.cau.medicheck.ui.theme.HomeChipMissedBg
import com.cau.medicheck.ui.theme.HomeChipMissedText
import com.cau.medicheck.ui.theme.HomeChipSkippedBg
import com.cau.medicheck.ui.theme.HomeChipSkippedText
import com.cau.medicheck.ui.theme.HomeChipWaitText
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ProgressTrack
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.theme.TimelineDotWait
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 약 상세 화면의 버튼들이 부를 함수 묶음 */
class MedicationDetailActions(
    val onBackToList: () -> Unit,
    val onPausedChange: (Boolean) -> Unit,
    val onEdit: () -> Unit,
    val onDelete: () -> Unit,
    val onOpenSource: () -> Unit
)

/**
 * S07 약 상세 (v3 medDetail()).
 *
 * 카드 두 장: 위 카드(이름·상태·복용 시각 / 다음 복용 / 최근 7일 요일 동그라미),
 * 아래 목록(복용 기간·메모·처방 출처·비활성화). 그 아래 "약 수정" 버튼과 "이 약 삭제" 글자.
 * (2026-11-24, 시안: 04_화면캡처/약상세_시안/시안2.html A — 덩어리가 많아 어지럽다는 의견으로 줄였다)
 * 일시중지를 켜면 오늘 아직 시각 전인 회차와 내일 이후 회차가 사라지고, 끄면 오늘 회차부터 다시 생긴다.
 */
@Composable
fun S07MedicationDetailScreen(state: MedicationDetailUiState, actions: MedicationDetailActions) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s07)) {
        val med = state.med
        when {
            state.isLoading -> Unit
            med == null -> MedicationNotFound(actions.onBackToList)
            else -> MedicationDetailBody(med, state, actions)
        }
    }
}

/** 약을 찾지 못했을 때의 안내. 약 수정 화면(S08)도 수정할 약이 없으면 이것을 쓴다. */
@Composable
fun MedicationNotFound(onBackToList: () -> Unit) {
    EmptyState(
        emoji = Glyph.NOT_FOUND,
        title = stringResource(R.string.error_med_not_found),
        body = stringResource(R.string.s07_empty_body),
        actionLabel = stringResource(R.string.s07_empty_action),
        onAction = onBackToList
    )
}

@Composable
private fun MedicationDetailBody(med: MedicationDetail, state: MedicationDetailUiState, actions: MedicationDetailActions) {
    BackLink(stringResource(R.string.s07_back), actions.onBackToList)

    // ----- 위 카드: 이름·상태·복용 시각 / 다음 복용 / 최근 7일 -----
    SurfaceCard(Modifier.padding(top = 16.dp), padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            McMedIcon(med.shape, med.color, size = 56.dp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = med.name,
                        fontSize = 23.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.9).sp,
                        color = Navy,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .semantics { heading() }
                    )
                    Spacer(Modifier.width(8.dp))
                    MedicationStatusPill(state.status, showActive = true)
                }
                Text(
                    text = whenText(med) + " · " + med.dosage,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // 다음 복용 (비활성화·종료면 없다)
        state.nextDose?.let { dose ->
            HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 16.dp))
            NextDoseLine(dose)
        }

        HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 16.dp))
        RecentWeek(state.recentDays, state.recentCounts)
    }

    // ----- 아래 목록: 복용 기간 · 메모 · 처방 출처 · 비활성화 -----
    val source = listOf(med.hospital, med.department).filter { it.isNotBlank() }.joinToString(" · ")
    SurfaceCard(Modifier.padding(top = 12.dp), padding = 0.dp) {
        Column(Modifier.padding(horizontal = 18.dp)) {
            InfoRow(stringResource(R.string.s07_label_period), periodText(med))
            InfoRow(stringResource(R.string.s07_label_memo), med.memo.ifBlank { stringResource(R.string.s07_no_memo) })
            // 처방 출처(S42): 적어 둔 값이 있으면 그 값, 없으면 "적기 ›". 줄을 누르면 S42로 간다.
            InfoRow(
                label = stringResource(R.string.s07_label_source),
                value = if (source.isNotEmpty()) "$source ›" else stringResource(R.string.s07_source_add),
                onClick = actions.onOpenSource
            )
            // 비활성화 (켜짐 = 오늘 이후 일정·알림에서 빠짐). 줄 어디를 눌러도 켜고 끈다.
            val pauseLabel = stringResource(R.string.s07_pause_title)
            val paused = med.pausedFrom != null
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = paused, enabled = !state.isBusy, role = Role.Switch, onValueChange = actions.onPausedChange)
                    .padding(vertical = 12.dp)
            ) {
                Text(pauseLabel, fontSize = 12.sp, color = Muted, modifier = Modifier.weight(1f))
                McSwitch(checked = paused, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { })
            }
        }
    }

    McButton(stringResource(R.string.s07_btn_edit), actions.onEdit, Modifier.padding(top = 14.dp), full = true)
    Text(
        text = stringResource(R.string.s07_btn_delete),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = DangerTitle,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = actions.onDelete)
            .padding(vertical = 14.dp)
    )
}

/** "매일 12:30 식후" / "매일 08:00 식전 · 20:00" / 시간마다 요일이 다르면 "월·수·금 08:00, 매일 20:00" */
@Composable
private fun whenText(med: MedicationDetail): String {
    val dayLabels = stringArrayResource(R.array.common_weekdays).toList()
    val everyDay = stringResource(R.string.common_every_day)
    val schedules = med.schedules.sortedBy { it.time }
    val times = schedules.map { timeWithCondition(it) }
    return if (schedules.map { it.days }.distinct().size <= 1) {
        daysText(schedules.firstOrNull()?.days.orEmpty(), everyDay, dayLabels) + " " + times.joinToString(" · ")
    } else {
        schedules.indices.joinToString(", ") { daysText(schedules[it].days, everyDay, dayLabels) + " " + times[it] }
    }
}

/** "다음 복용  내일 12:30" */
@Composable
private fun NextDoseLine(dose: DoseItem) {
    val today = LocalDate.now()
    val day = when (dose.date) {
        today -> stringResource(R.string.s07_day_today)
        today.plusDays(1) -> stringResource(R.string.s07_day_tomorrow)
        else -> fullDateText(dose.date)
    }
    val label = stringResource(R.string.s07_next_label)
    val value = "$day ${timeText(dose.time)}"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$label, $value" }
    ) {
        Text(label, fontSize = 13.sp, color = Muted)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Navy, modifier = Modifier.padding(start = 8.dp))
    }
}

/** "최근 7일   7번 중 2번 · 29%" + 요일 동그라미 7개 (범례 없음 — ✓ ! – ○ 기호로 구분) */
@Composable
private fun RecentWeek(days: List<RecentDay>, counts: DoseCounts) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.s07_recent_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Navy,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        val rate = counts.completionRate
        if (rate == null) {
            Text(stringResource(R.string.s07_recent_none), fontSize = 12.sp, color = Muted)
        } else {
            Text(stringResource(R.string.s07_recent_count, counts.due, counts.taken), fontSize = 12.sp, color = Muted)
            Text(
                text = stringResource(R.string.s07_recent_rate, rate),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = when {
                    rate >= 80 -> HomeChipDoneText
                    rate >= 50 -> HomeChipMissedText
                    else -> DangerTitle
                }
            )
        }
    }
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        val today = days.lastOrNull()?.date
        days.forEach { day -> DayCircle(day, isToday = day.date == today) }
    }
}

/** 동그라미의 바탕색 · 글자색 · 기호. 범례도 같은 값을 써서 색이 어긋나지 않게 한다 */
private data class MarkLook(val bg: Color, val fg: Color, val symbol: String)

private fun DayMark.look(): MarkLook = when (this) {
    DayMark.TAKEN -> MarkLook(HomeChipDoneBg, HomeChipDoneText, "✓")
    DayMark.MISSED -> MarkLook(HomeChipMissedBg, HomeChipMissedText, "!")
    DayMark.SKIPPED -> MarkLook(HomeChipSkippedBg, HomeChipSkippedText, "–")
    DayMark.UPCOMING -> MarkLook(Color.White, HomeChipWaitText, "○")
    DayMark.NONE -> MarkLook(ProgressTrack, Muted, "")
}

/** 동그라미 모양: 바탕색, 예정이면 흰 바탕에 회색 테두리 */
private fun Modifier.markCircle(mark: DayMark): Modifier = this
    .clip(CircleShape)
    .background(mark.look().bg)
    .then(if (mark == DayMark.UPCOMING) Modifier.border(1.5.dp, TimelineDotWait, CircleShape) else Modifier)

/** 요일 동그라미 하나: 먹음 ✓ · 미복용 ! · 건너뜀 – · 예정 ○ · 회차 없음(빈 동그라미) */
@Composable
private fun DayCircle(day: RecentDay, isToday: Boolean) {
    val (_, fg, symbol) = day.mark.look()
    val dayName = if (isToday) stringResource(R.string.s07_day_today) else weekdayText(day.date)
    val description = fullDateText(day.date) + ", " + markName(day.mark)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .markCircle(day.mark)
        ) {
            Text(symbol, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = fg)
        }
        Text(
            text = dayName,
            fontSize = 11.sp,
            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (isToday) Navy else Muted,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}


@Composable
private fun markName(mark: DayMark): String = stringResource(
    when (mark) {
        DayMark.TAKEN -> R.string.s07_mark_taken
        DayMark.MISSED -> R.string.s07_mark_missed
        DayMark.SKIPPED -> R.string.s07_mark_skipped
        DayMark.UPCOMING -> R.string.s07_mark_upcoming
        DayMark.NONE -> R.string.s07_mark_none
    }
)

/** 정보 칸의 한 줄: 왼쪽 이름, 오른쪽 값. onClick이 있으면 줄 전체를 누를 수 있고 값이 청록색이다. */
@Composable
private fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null, showDivider: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 13.dp)
    ) {
        Text(label, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(end = 12.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (onClick != null) Teal else Navy,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
    if (showDivider) HorizontalDivider(color = Line)
}

/** 복용 기간: "10월 17일부터 · 39일째" / "11월 12일 ~ 12월 11일 · 13일째" / 시작 전·끝남 */
@Composable
private fun periodText(med: MedicationDetail): String {
    val today = LocalDate.now()
    val start = med.startDate
    val end = med.endDate
    val dayN = ChronoUnit.DAYS.between(start, today).toInt() + 1
    return when {
        today.isBefore(start) -> stringResource(R.string.s07_period_future, start.monthValue, start.dayOfMonth)
        end == null -> stringResource(R.string.s07_period_open, start.monthValue, start.dayOfMonth, dayN)
        today.isAfter(end) -> stringResource(R.string.s07_period_ended, start.monthValue, start.dayOfMonth, end.monthValue, end.dayOfMonth)
        else -> stringResource(R.string.s07_period_range, start.monthValue, start.dayOfMonth, end.monthValue, end.dayOfMonth, dayN)
    }
}
