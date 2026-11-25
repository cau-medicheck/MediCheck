package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.theme.HomeChipWaitBg
import com.cau.medicheck.domain.MedicationStatus
import com.cau.medicheck.domain.DayMark
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.DoseCounts
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.MedicationDetail
import com.cau.medicheck.domain.RecentDay
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.McSwitch
import com.cau.medicheck.ui.components.MedShapeArt
import com.cau.medicheck.ui.components.conditionLabel
import com.cau.medicheck.ui.components.daysText
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.components.timeWithCondition
import com.cau.medicheck.ui.components.toColor
import com.cau.medicheck.ui.components.weekdayText
import com.cau.medicheck.ui.theme.CardBackground
import com.cau.medicheck.ui.theme.CardBorder
import com.cau.medicheck.ui.theme.DangerTitle
import com.cau.medicheck.ui.theme.HeroDeepEnd
import com.cau.medicheck.ui.theme.HeroDeepStart
import com.cau.medicheck.ui.theme.HomeChipDoneBg
import com.cau.medicheck.ui.theme.HomeChipDoneText
import com.cau.medicheck.ui.theme.HomeChipMissedBg
import com.cau.medicheck.ui.theme.HomeChipMissedText
import com.cau.medicheck.ui.theme.HomeChipSkippedBg
import com.cau.medicheck.ui.theme.HomeChipSkippedText
import com.cau.medicheck.ui.theme.HomeChipWaitText
import com.cau.medicheck.ui.theme.MemoIconBg
import com.cau.medicheck.ui.theme.MemoIconTint
import com.cau.medicheck.ui.theme.Mint
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ProgressTrack
import com.cau.medicheck.ui.theme.RateFair
import com.cau.medicheck.ui.theme.RateGood
import com.cau.medicheck.ui.theme.RateLow
import com.cau.medicheck.ui.theme.SourceIconBg
import com.cau.medicheck.ui.theme.SourceIconTint
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
 * 이름 카드(윗부분은 약 색 머리, 아래는 최근 7일 + 복용률 원 그래프) → 다음 복용 청록 띠 →
 * 부수 정보 작은 칸 3개(복용 기간·메모·처방 출처) → 비활성화 한 줄 → "약 수정" 버튼과 "이 약 삭제" 글자.
 * (2026-11-24, 시안: 04_화면캡처/약상세_시안/시안4.html C2)
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

    // ----- 이름 카드: 윗부분은 약 색으로 옅게 칠한 머리, 아래는 최근 7일 -----
    val medColor = med.color.toColor()
    val cardShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(cardShape)
            .background(CardBackground)
            .border(1.dp, CardBorder, cardShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(medColor.copy(alpha = 0.16f), medColor.copy(alpha = 0.05f))))
        ) {
            // 오른쪽 위의 큰 흐린 그림 (장식)
            MedShapeArt(
                med.shape, med.color,
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 14.dp, y = (-14).dp)
                    .size(110.dp)
                    .alpha(0.2f)
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(18.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(58.dp)
                        .shadow(3.dp, RoundedCornerShape(17.dp))
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color.White)
                ) {
                    MedShapeArt(med.shape, med.color, Modifier.size(36.dp))
                }
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
        }
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp)) {
            RecentWeek(state.recentDays, state.recentCounts)
        }
    }

    // ----- 다음 복용: 진한 청록 띠 (보기 전용) -----
    // 다음 복용이 없어도(비활성화·종료) 띠를 같은 크기의 회색으로 남긴다. 띠가 사라지면 비활성화를 켜고 끌 때
    // 아래 칸들이 위아래로 움직이기 때문이다.
    NextDoseBand(state.nextDose, state.status)

    // ----- 부수 정보: 작은 칸 3개 (복용 기간 · 메모 · 처방 출처) -----
    val source = listOf(med.hospital, med.department).filter { it.isNotBlank() }.joinToString(" · ")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
        InfoTile(Icons.Filled.CalendarMonth, Teal, Mint, stringResource(R.string.s07_label_period), periodShort(med), Modifier.weight(1f))
        InfoTile(
            Icons.Filled.Description, MemoIconTint, MemoIconBg, stringResource(R.string.s07_label_memo),
            med.memo.ifBlank { stringResource(R.string.s07_no_memo) }, Modifier.weight(1f)
        )
        // 처방 출처(S42): 누르면 적는 화면으로. 적어 둔 값이 없으면 "적기 ›"
        InfoTile(
            Icons.Filled.LocalHospital, SourceIconTint, SourceIconBg, stringResource(R.string.s07_label_source),
            if (source.isNotEmpty()) source else stringResource(R.string.s07_source_add), Modifier.weight(1f),
            onClick = actions.onOpenSource
        )
    }

    // ----- 비활성화: 작은 한 줄. 줄 어디를 눌러도 켜고 끈다 -----
    val paused = med.pausedFrom != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .toggleable(value = paused, enabled = !state.isBusy, role = Role.Switch, onValueChange = actions.onPausedChange)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Icon(Icons.Filled.Pause, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(R.string.s07_pause_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        McSwitch(checked = paused, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { })
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
internal fun whenText(med: MedicationDetail): String {
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

/**
 * 다음 복용 띠: [시계] 다음 복용 / 내일 12:30 · 식후 (진한 청록).
 * 다음 복용이 없으면 같은 크기의 회색 띠: "없음 · 비활성화 중" / "없음 · 복용 기간 끝" / "없음 · 7일 안에 일정 없음"
 */
@Composable
private fun NextDoseBand(dose: DoseItem?, status: MedicationStatus) {
    val value = if (dose == null) {
        stringResource(
            when (status) {
                MedicationStatus.PAUSED -> R.string.s07_next_none_paused
                MedicationStatus.ENDED -> R.string.s07_next_none_ended
                MedicationStatus.ACTIVE -> R.string.s07_next_none
            }
        )
    } else {
        val today = LocalDate.now()
        val day = when (dose.date) {
            today -> stringResource(R.string.s07_day_today)
            today.plusDays(1) -> stringResource(R.string.s07_day_tomorrow)
            else -> fullDateText(dose.date)
        }
        val condition = if (dose.condition == DoseCondition.NONE) "" else " · " + conditionLabel(dose.condition)
        "$day ${timeText(dose.time)}$condition"
    }
    val active = dose != null
    val fg = if (active) Color.White else HomeChipWaitText
    val label = stringResource(R.string.s07_next_label)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (active) Brush.linearGradient(listOf(HeroDeepStart, HeroDeepEnd))
                else Brush.linearGradient(listOf(HomeChipWaitBg, HomeChipWaitBg))
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clearAndSetSemantics { contentDescription = "$label, $value" }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) Color.White.copy(alpha = 0.18f) else Color.White)
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = fg, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, fontSize = 11.sp, color = if (active) Color.White.copy(alpha = 0.85f) else HomeChipWaitText)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 최근 7일: 제목 · "7번 중 2번 먹었어요" · 오른쪽 원 그래프, 아래 요일 동그라미 7개 */
@Composable
private fun RecentWeek(days: List<RecentDay>, counts: DoseCounts) {
    val rate = counts.completionRate
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.s07_recent_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Navy,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = if (rate == null) stringResource(R.string.s07_recent_none)
                else stringResource(R.string.s07_recent_sentence, counts.due, counts.taken),
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (rate != null) RateRing(rate)
    }
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        val today = days.lastOrNull()?.date
        days.forEach { day -> DayCircle(day, isToday = day.date == today) }
    }
}

/** 복용률 원 그래프 (가운데 "29%"). 80% 이상 초록 · 50% 이상 주황 · 그 아래 빨강 — 내 약 카드의 막대와 같은 기준 */
@Composable
private fun RateRing(rate: Int) {
    val (arc, text) = when {
        rate >= 80 -> RateGood to HomeChipDoneText
        rate >= 50 -> RateFair to HomeChipMissedText
        else -> RateLow to DangerTitle
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
        Canvas(Modifier.size(48.dp)) {
            val stroke = 5.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(ProgressTrack, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(arc, -90f, 360f * rate / 100f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(stringResource(R.string.s07_recent_rate, rate), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = text)
    }
}

/** 부수 정보 작은 칸: [색 아이콘] / 이름 / 값 (길면 …). onClick이 있으면 칸을 누를 수 있고 값이 청록색 */
@Composable
private fun InfoTile(
    icon: ImageVector,
    tint: Color,
    iconBg: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(CardBackground)
            .border(1.dp, CardBorder, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 11.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint)   // 진한 색 네모 + 흰 아이콘 (2026-11-25 아이콘 3번)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
        Text(label, fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 6.dp))
        Text(
            text = if (onClick != null && !value.endsWith("›")) "$value ›" else value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (onClick != null) Teal else Navy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** 작은 칸에 들어가는 복용 기간: "39일째" / "12월 1일 시작" / "끝남" */
@Composable
private fun periodShort(med: MedicationDetail): String {
    val today = LocalDate.now()
    val end = med.endDate
    return when {
        today.isBefore(med.startDate) -> stringResource(R.string.s07_period_short_future, med.startDate.monthValue, med.startDate.dayOfMonth)
        end != null && today.isAfter(end) -> stringResource(R.string.s07_period_short_ended)
        else -> stringResource(R.string.s07_period_short_day, ChronoUnit.DAYS.between(med.startDate, today).toInt() + 1)
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


