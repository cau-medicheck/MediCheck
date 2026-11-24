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
import com.cau.medicheck.domain.DayMark
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.DoseCounts
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.MedicationDetail
import com.cau.medicheck.domain.RecentDay
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.ConditionTag
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McMedIcon
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.OptionRow
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.ToggleRow
import com.cau.medicheck.ui.components.conditionLabel
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
    val onOpenSource: () -> Unit,
    val onOpenArchive: () -> Unit
)

/**
 * S07 약 상세 (v3 medDetail()).
 *
 * 이름·요약, 다음 복용 띠, 최근 7일 요일 동그라미, 복용 시간표, 정보(기간·메모·처방 출처),
 * 비활성화 스위치, 수정·삭제 버튼, 지난 기록으로 가는 줄. (2026-11-24 개편, 시안: 04_화면캡처/약상세_시안/시안.html B)
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

    // ----- 큰 아이콘 + 이름 + 상태 배지 + "1 cap · 하루 1번" -----
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
        McMedIcon(med.shape, med.color, size = 58.dp)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = med.name,
                fontSize = 23.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.9).sp,
                color = Navy,
                modifier = Modifier.semantics { heading() }
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                MedicationStatusPill(state.status, showActive = true)
                Text(
                    text = stringResource(R.string.s07_summary, med.dosage, med.schedules.size),
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.padding(start = 7.dp)
                )
            }
        }
    }

    // ----- 다음 복용 띠 (보기 전용) -----
    state.nextDose?.let { NextDoseBand(it) }

    // ----- 최근 7일: 요일 동그라미 -----
    RecentWeekCard(state.recentDays, state.recentCounts)

    // ----- 복용 시간: 시간표처럼 한 줄에 하나 -----
    SectionTitle(stringResource(R.string.s07_section_times))
    SurfaceCard(padding = 0.dp) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            val dayLabels = stringArrayResource(R.array.common_weekdays).toList()
            val everyDay = stringResource(R.string.common_every_day)
            med.schedules.sortedBy { it.time }.forEachIndexed { i, schedule ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(timeText(schedule.time), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
                    ConditionTag(schedule.condition, Modifier.padding(start = 10.dp))
                    Spacer(Modifier.weight(1f))
                    Text(daysText(schedule.days, everyDay, dayLabels), fontSize = 12.sp, color = Muted)
                }
                if (i < med.schedules.lastIndex) HorizontalDivider(color = Line)
            }
        }
    }

    // ----- 정보: 복용 기간 · 메모 · 처방 출처 -----
    SectionTitle(stringResource(R.string.s07_section_info))
    val source = listOf(med.hospital, med.department).filter { it.isNotBlank() }.joinToString(" · ")
    SurfaceCard(padding = 0.dp) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            InfoRow(stringResource(R.string.s07_label_period), periodText(med))
            InfoRow(stringResource(R.string.s07_label_memo), med.memo.ifBlank { stringResource(R.string.s07_no_memo) })
            // 처방 출처(S42): 적어 둔 값이 있으면 그 값, 없으면 "적기 ›". 줄을 누르면 S42로 간다.
            InfoRow(
                label = stringResource(R.string.s07_label_source),
                value = if (source.isNotEmpty()) "$source ›" else stringResource(R.string.s07_source_add),
                onClick = actions.onOpenSource,
                showDivider = false
            )
        }
    }

    // ----- 비활성화 스위치 (켜짐 = 일정·알림에서 빠짐) -----
    SurfaceCard(Modifier.padding(top = 16.dp)) {
        ToggleRow(
            title = stringResource(R.string.s07_pause_title),
            description = stringResource(R.string.s07_pause_body),
            checked = med.pausedFrom != null,
            onCheckedChange = actions.onPausedChange,
            enabled = !state.isBusy
        )
    }

    Row(modifier = Modifier.padding(top = 16.dp)) {
        McButton(stringResource(R.string.s07_btn_edit), actions.onEdit, Modifier.weight(1f), full = true)
        Spacer(Modifier.width(10.dp))
        McButton(stringResource(R.string.s07_btn_delete), actions.onDelete, Modifier.weight(1f), ButtonKind.RED, full = true)
    }

    SurfaceCard(Modifier.padding(top = 16.dp), padding = 0.dp) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            OptionRow(
                title = stringResource(R.string.s07_row_archive_title),
                description = stringResource(R.string.s07_row_archive_desc),
                onClick = actions.onOpenArchive,
                showDivider = false
            )
        }
    }
}

/** 청록 띠: "다음 복용 / 내일 12:30 · 식후" */
@Composable
private fun NextDoseBand(dose: DoseItem) {
    val today = LocalDate.now()
    val day = when (dose.date) {
        today -> stringResource(R.string.s07_day_today)
        today.plusDays(1) -> stringResource(R.string.s07_day_tomorrow)
        else -> fullDateText(dose.date)
    }
    val condition = if (dose.condition == DoseCondition.NONE) "" else " · " + conditionLabel(dose.condition)
    val value = "$day ${timeText(dose.time)}$condition"
    val label = stringResource(R.string.s07_next_label)
    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Teal)
            .padding(horizontal = 15.dp, vertical = 12.dp)
            .clearAndSetSemantics { contentDescription = "$label, $value" }
    ) {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

/** "최근 7일   7번 중 2번 먹음 · 29%" + 요일 동그라미 7개 + 범례 */
@Composable
private fun RecentWeekCard(days: List<RecentDay>, counts: DoseCounts) {
    SurfaceCard(Modifier.padding(top = 12.dp)) {
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
            Text(
                text = if (rate == null) stringResource(R.string.s07_recent_none)
                else stringResource(R.string.s07_recent_summary, counts.due, counts.taken, rate),
                fontSize = 12.sp,
                color = Muted
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            val today = days.lastOrNull()?.date
            days.forEach { day -> DayCircle(day, isToday = day.date == today) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
            DayMark.entries.filter { it != DayMark.NONE }.forEach { Legend(it) }
        }
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
                .size(32.dp)
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

/** 범례 하나: 동그라미를 작게 줄인 것 + 이름 */
@Composable
private fun Legend(mark: DayMark) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(11.dp).markCircle(mark))
        Text(markName(mark), fontSize = 11.sp, color = Muted, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun markName(mark: DayMark): String = stringResource(
    when (mark) {
        DayMark.TAKEN -> R.string.s07_legend_taken
        DayMark.MISSED -> R.string.s07_legend_missed
        DayMark.SKIPPED -> R.string.s07_legend_skipped
        DayMark.UPCOMING -> R.string.s07_legend_upcoming
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
