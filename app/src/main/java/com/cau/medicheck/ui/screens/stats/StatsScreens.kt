package com.cau.medicheck.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DayPeriod
import com.cau.medicheck.domain.DayStats
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.DoseCounts
import com.cau.medicheck.domain.HabitMark
import com.cau.medicheck.domain.Punctuality
import com.cau.medicheck.domain.SlotMiss
import com.cau.medicheck.domain.Streaks
import com.cau.medicheck.domain.WorstDose
import com.cau.medicheck.domain.periodOf
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.GlyphText
import com.cau.medicheck.ui.components.McMedIcon
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.StatusCount
import com.cau.medicheck.ui.components.StatusCountRow
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.conditionLabel
import com.cau.medicheck.ui.components.monthTitleText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.DangerTitle
import com.cau.medicheck.ui.theme.HeroDeepEnd
import com.cau.medicheck.ui.theme.HeroDeepStart
import com.cau.medicheck.ui.theme.HomeChipDoneBg
import com.cau.medicheck.ui.theme.HomeChipDoneText
import com.cau.medicheck.ui.theme.HomeChipMissedText
import com.cau.medicheck.ui.theme.HomeChipSkippedText
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ProgressTrack
import com.cau.medicheck.ui.theme.RateFair
import com.cau.medicheck.ui.theme.RateGood
import com.cau.medicheck.ui.theme.RateLow
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.theme.TimelineDotMissed
import com.cau.medicheck.ui.theme.TimelineDotSkipped
import java.time.LocalDate
import java.time.YearMonth

/*
 * 통계 화면 세 개: S13 주간 · S14 월간 · S15 기타(빠뜨림 진단·복용 습관). (2026-11-24 개편, 시안: 04_화면캡처/통계_시안/시안5.html, 시안4.html 패턴)
 * 위쪽 탭("주간 | 월간 | 패턴")으로 서로 오간다. 계산은 ViewModel(→ StatsCalculator)이 하고 여기서는 그리기만 한다.
 * 설명 문장은 두지 않는다(계산 기준은 설정의 "복약률 계산 기준"에만 있다).
 */

/** 통계 화면 맨 위: 밑줄 글자 탭 "주간 · 월간 · 패턴". index = 지금 화면의 탭 번호(0 주간, 1 월간, 2 패턴) */
@Composable
private fun StatsHeader(index: Int, onTab: (Int) -> Unit) {
    val labels = listOf(
        stringResource(R.string.stats_tab_week),
        stringResource(R.string.stats_tab_month),
        stringResource(R.string.stats_tab_pattern)
    )
    Column(Modifier.padding(top = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.selectableGroup()) {
            labels.forEachIndexed { i, label ->
                val on = i == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .selectable(selected = on, role = Role.Tab, onClick = { onTab(i) })
                        .padding(top = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (on) Navy else TabOffText,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    // 고른 탭만 청록 밑줄
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .width(if (on) 34.dp else 0.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (on) Teal else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(color = Line)
    }
}

/** 고르지 않은 탭 글자 (흰 바탕에서 읽을 수 있는 진하기) */
private val TabOffText = Color(0xFF5E7480)

// ---------------------------------------------------------------------------------------------
// 색: 80% 이상 초록 · 50% 이상 주황 · 그 아래 빨강 (내 약 카드·약 상세와 같은 기준)
// ---------------------------------------------------------------------------------------------

/** 막대·원 그래프의 색 */
private fun rateFill(rate: Int): Color = when {
    rate >= 80 -> RateGood
    rate >= 50 -> RateFair
    else -> RateLow
}

/** 숫자 글자의 색 (흰 바탕에서 읽기 좋게 막대 색보다 진하다) */
private fun rateText(rate: Int): Color = when {
    rate >= 80 -> HomeChipDoneText
    rate >= 50 -> HomeChipMissedText
    else -> DangerTitle
}

/** 달력 칸 색 (진할수록 많이 먹은 날) */
private val HeatFull = Color(0xFF17806D)    // 80% 이상 — 글자는 흰색
private val HeatGood = Color(0xFF9FDCCF)    // 50% 이상
private val HeatSome = Color(0xFFFFD3A6)    // 조금
private val HeatNone = Color(0xFFF4B4B4)    // 0%
private val HeatEmpty = Color(0xFFF4F7F8)   // 회차가 없거나 아직 안 온 날
private val HeatEmptyText = Color(0xFF6E828B)

private fun heatColor(rate: Int?): Color = when {
    rate == null -> HeatEmpty
    rate >= 80 -> HeatFull
    rate >= 50 -> HeatGood
    rate > 0 -> HeatSome
    else -> HeatNone
}

// ---------------------------------------------------------------------------------------------
// 공통 부품
// ---------------------------------------------------------------------------------------------

/**
 * 기간 이동 알약: 화면 맨 아래(하단 탭 바로 위)에 떠 있는 가로로 긴 흰 알약 [‹  11월 4주차 / 11.23 ~ 11.29  ›].
 * 스크롤해도 그 자리에 있다. 그림자로 떠 보이게 한다.
 */
@Composable
private fun FloatingPeriodBar(title: String, range: String, prevLabel: String, nextLabel: String, onPrev: () -> Unit, onNext: () -> Unit, nextEnabled: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(99.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = Navy.copy(alpha = 0.25f), spotColor = Navy.copy(alpha = 0.25f))
            .clip(shape)
            .background(Color.White)
            .padding(6.dp)
    ) {
        RoundArrow(Glyph.PREV, prevLabel, onPrev, enabled = true)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Navy, modifier = Modifier.semantics { heading() })
            Text(range, fontSize = 12.sp, color = Muted)
        }
        RoundArrow(Glyph.NEXT, nextLabel, onNext, enabled = nextEnabled)
    }
}

/** 알약 양 끝의 동그란 화살표 단추 (48dp 누름 영역) */
@Composable
private fun RoundArrow(symbol: String, label: String, onClick: () -> Unit, enabled: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(ProgressTrack)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                if (!enabled) disabled()
            }
    ) {
        GlyphText(symbol, fontSize = 18, color = Navy)
    }
}

/** 통계 화면 틀: 스크롤 내용 위에 아래쪽 기간 알약을 겹쳐 띄운다(알약이 없으면 내용만) */
@Composable
private fun StatsFrame(paneTitle: String, bar: (@Composable (Modifier) -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        McScreenColumn(paneTitle = paneTitle) {
            content()
            // 마지막 카드가 떠 있는 알약에 가리지 않게 아래 여백
            if (bar != null) Spacer(Modifier.height(84.dp))
        }
        bar?.invoke(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

/** "11.23 ~ 11.29" */
@Composable
private fun rangeText(from: LocalDate, to: LocalDate): String =
    stringResource(R.string.stats_range, from.monthValue, from.dayOfMonth, to.monthValue, to.dayOfMonth)

/**
 * 요약 카드: [원 그래프 %]  5 / 9회  [지난주 ▲ 4%p]
 *                          완료 · 건너뜀 · 미복용 칸
 */
@Composable
private fun SummaryCard(total: DoseCounts, prevRate: Int?, @androidx.annotation.StringRes deltaRes: Int) {
    val rate = total.completionRate
    SurfaceCard(Modifier.padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RateRing(rate, size = 92.dp)
            Column(Modifier.padding(start = 18.dp)) {
                Text(
                    text = stringResource(R.string.stats_count, total.taken, total.due),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Navy
                )
                if (rate != null && prevRate != null) DeltaChip(rate - prevRate, deltaRes, Modifier.padding(top = 8.dp))
            }
        }
        // 완료 · 건너뜀 · 미복용: 선으로 나눈 세 칸 (색 점 + 이름 / 횟수 / 비율)
        HorizontalDivider(color = Line, modifier = Modifier.padding(top = 18.dp))
        StatusCountRow(
            items = listOf(
                StatusCount(stringResource(R.string.common_count_taken), total.taken, RateGood, HomeChipDoneText),
                StatusCount(stringResource(R.string.common_count_skipped), total.skipped, TimelineDotSkipped, HomeChipSkippedText),
                StatusCount(stringResource(R.string.common_count_missed), total.missed, TimelineDotMissed, HomeChipMissedText)
            ),
            total = total.due,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}


/** 원 그래프: 가운데 "56%", 계산할 것이 없으면 "–" */
@Composable
private fun RateRing(rate: Int?, size: Dp) {
    val label = if (rate == null) stringResource(R.string.common_na) else stringResource(R.string.common_percent, rate)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = label }
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(ProgressTrack, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (rate != null && rate > 0) {
                drawArc(rateFill(rate), -90f, 360f * rate / 100f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = if (rate == null) Muted else rateText(rate))
    }
}

/** "지난주 ▲ 4%p" / "지난주 ▼ 18%p" / "지난주와 같음" */
@Composable
private fun DeltaChip(delta: Int, @androidx.annotation.StringRes deltaRes: Int, modifier: Modifier = Modifier) {
    val (bg, fg, mark) = when {
        delta > 0 -> Triple(HomeChipDoneBg, HomeChipDoneText, "▲")
        delta < 0 -> Triple(Color(0xFFFDEDED), DangerTitle, "▼")
        else -> Triple(ProgressTrack, Muted, "=")
    }
    Text(
        text = stringResource(deltaRes, mark, kotlin.math.abs(delta)),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** 카드 제목 (+ 오른쪽 작은 글자) */
@Composable
private fun CardTitle(title: String, side: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Navy, modifier = Modifier.weight(1f).semantics { heading() })
        if (side != null) Text(side, fontSize = 12.sp, color = Muted)
    }
}

/** 가로 막대 한 줄의 막대 부분 */
@Composable
private fun RateBar(rate: Int?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(10.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(ProgressTrack)
    ) {
        if (rate != null && rate > 0) {
            Box(
                Modifier
                    .fillMaxWidth(rate / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(rateFill(rate))
            )
        }
    }
}

// =============================================================================================
// S13 주간: 월~일 한 주
// =============================================================================================

@Composable
fun S13WeekStatsScreen(state: WeeklyStatsUiState, onTab: (Int) -> Unit, onShiftWeek: (Long) -> Unit) {
    val week = state.week
    val title = stringResource(R.string.stats_week_title, week.month.monthValue, week.index)
    val range = rangeText(state.monday, state.monday.plusDays(6))
    val prev = stringResource(R.string.stats_cd_prev_week)
    val next = stringResource(R.string.stats_cd_next_week)
    StatsFrame(
        paneTitle = stringResource(R.string.screen_title_s13),
        bar = { m -> FloatingPeriodBar(title, range, prev, next, { onShiftWeek(-1) }, { onShiftWeek(1) }, !state.isThisWeek, m) }
    ) {
        StatsHeader(index = 0, onTab = onTab)
        // 기록을 읽어 오는 잠깐 동안은 "– · 0"이 번쩍이지 않게 아래를 그리지 않는다.
        if (state.isLoading) return@StatsFrame

        SummaryCard(state.stats.total, state.prevRate, R.string.stats_delta_week)

        SurfaceCard(Modifier.padding(top = 12.dp)) {
            CardTitle(stringResource(R.string.stats_section_weekday))
            WeekdayBars(state.stats.days, state.today)
        }
    }
}

/** 요일별 세로 막대 7개(월~일). 아직 안 온 날은 점선 칸, 회차가 없던 날은 빈 칸 */
@Composable
private fun WeekdayBars(days: List<DayStats>, today: LocalDate) {
    val names = stringArrayResource(R.array.common_weekdays)   // "일"…"토"
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            val rate = day.counts.completionRate
            val future = day.date.isAfter(today)
            val name = names[day.date.dayOfWeek.value % 7]
            val pct = if (rate == null) "" else stringResource(R.string.common_percent, rate)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(36.dp)
                    .clearAndSetSemantics { contentDescription = "$name $pct" }
            ) {
                Text(pct, fontSize = 11.sp, color = Muted, modifier = Modifier.height(18.dp))
                val shape = RoundedCornerShape(9.dp)
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .width(24.dp)
                        .height(104.dp)
                        .clip(shape)
                        .then(
                            if (future) Modifier.background(Color.White).border(1.5.dp, Line, shape)
                            else Modifier.background(ProgressTrack)
                        )
                ) {
                    if (rate != null && rate > 0) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(rate / 100f).clip(shape).background(rateFill(rate)))
                    }
                }
                val isToday = day.date == today
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (isToday) Navy else Muted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// =============================================================================================
// S14 월간: 원 그래프 + 날짜별 달력 + 주차별 가로 막대
// =============================================================================================

@Composable
fun S14MonthStatsScreen(state: MonthStatsUiState, onTab: (Int) -> Unit, onShiftMonth: (Long) -> Unit) {
    val month = state.month
    val title = monthTitleText(month)
    val range = rangeText(month.atDay(1), month.atEndOfMonth())
    val prev = stringResource(R.string.common_cd_prev_month)
    val next = stringResource(R.string.common_cd_next_month)
    StatsFrame(
        paneTitle = stringResource(R.string.screen_title_s14),
        bar = { m -> FloatingPeriodBar(title, range, prev, next, { onShiftMonth(-1) }, { onShiftMonth(1) }, month.isBefore(YearMonth.from(state.today)), m) }
    ) {
        StatsHeader(index = 1, onTab = onTab)
        if (state.isLoading) return@StatsFrame

        SummaryCard(state.stats.total, state.prevRate, R.string.stats_delta_month)

        SurfaceCard(Modifier.padding(top = 12.dp)) {
            CardTitle(stringResource(R.string.stats_section_calendar))
            HeatCalendar(month, state.stats.days, state.today)
        }

        if (state.weeks.isNotEmpty()) {
            SurfaceCard(Modifier.padding(top = 12.dp), padding = 0.dp) {
                Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 6.dp)) {
                    Box(Modifier.padding(horizontal = 8.dp)) { CardTitle(stringResource(R.string.stats_section_weeks)) }
                    state.weeks.forEachIndexed { i, row ->
                        WeekRateRow(row)
                        if (i < state.weeks.lastIndex) HorizontalDivider(color = Line)
                    }
                }
            }
        }
    }
}

/** 날짜별 달력: 칸 색 = 그날 복용률. 일요일부터. 아래에 "0% ━━ 100%" 색 띠 */
@Composable
private fun HeatCalendar(month: YearMonth, days: List<DayStats>, today: LocalDate) {
    val weekdays = stringArrayResource(R.array.common_weekdays)
    val byDate = days.associateBy { it.date }
    val blanks = month.atDay(1).dayOfWeek.value % 7
    val cells: List<LocalDate?> = List(blanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            weekdays.forEach { name ->
                Text(name, fontSize = 11.sp, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).clearAndSetSemantics { })
            }
        }
        cells.chunked(7).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { date ->
                    if (date == null) Spacer(Modifier.weight(1f))
                    else HeatCell(date, byDate[date]?.counts?.completionRate?.takeIf { !date.isAfter(today) }, date == today, Modifier.weight(1f))
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics { }) {
            Text(stringResource(R.string.common_percent, 0), fontSize = 11.sp, color = Muted)
            Box(
                Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Brush.horizontalGradient(listOf(HeatNone, HeatSome, HeatGood, HeatFull)))
            )
            Text(stringResource(R.string.common_percent, 100), fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun HeatCell(date: LocalDate, rate: Int?, isToday: Boolean, modifier: Modifier) {
    val label = if (rate == null) stringResource(R.string.stats_cd_day_none, date.dayOfMonth)
    else stringResource(R.string.stats_cd_day, date.dayOfMonth, rate)
    val shape = RoundedCornerShape(8.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1.15f)
            .clip(shape)
            .background(heatColor(rate))
            .then(if (isToday) Modifier.border(2.dp, Navy, shape) else Modifier)
            .clearAndSetSemantics { contentDescription = label }
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 12.sp,
            fontWeight = if (rate == null) FontWeight.Medium else FontWeight.Bold,
            color = when {
                rate == null -> HeatEmptyText
                rate >= 80 -> Color.White
                else -> Navy
            }
        )
    }
}

/** 주차별 한 줄: "1주차 / 11.2 ~ 11.8   ━━━━━━   80%". 이번 주는 옅은 바탕 */
@Composable
private fun WeekRateRow(row: WeekRow) {
    val name = stringResource(R.string.stats_week_index, row.index)
    val range = rangeText(row.monday, row.monday.plusDays(6))
    val pct = if (row.rate == null) stringResource(R.string.common_na) else stringResource(R.string.common_percent, row.rate)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (row.isThisWeek) Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F8F9)) else Modifier)
            .padding(horizontal = 8.dp, vertical = 12.dp)   // 모든 줄이 같은 여백이라야 막대 길이가 맞는다
            .clearAndSetSemantics { contentDescription = "$name $range $pct" }
    ) {
        Column(Modifier.width(96.dp)) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(range, fontSize = 11.sp, color = Muted)
        }
        RateBar(row.rate, Modifier.weight(1f))
        Text(
            text = pct,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (row.rate == null) Muted else rateText(row.rate),
            textAlign = TextAlign.End,
            modifier = Modifier.width(54.dp)
        )
    }
}

// =============================================================================================
// S15 기타: 빠뜨림 진단(최근 4주) + 복용 습관
//   결론 카드 → 시간대별 빠뜨린 비율 → 요일별 빠뜨린 횟수 → 자주 빠뜨린 약 → 연속 기록 → 제시간에 먹었나 → 시간대별 평균
// =============================================================================================

/** 빠뜨린 비율의 색: 낮을수록 좋다 (20% 이하 초록 · 50% 이하 주황 · 그 위 빨강) */
private fun missFill(rate: Int): Color = when {
    rate <= 20 -> RateGood
    rate <= 50 -> RateFair
    else -> RateLow
}

private fun missText(rate: Int): Color = when {
    rate <= 20 -> HomeChipDoneText
    rate <= 50 -> HomeChipMissedText
    else -> DangerTitle
}

@Composable
fun S15PatternScreen(state: PatternUiState, onTab: (Int) -> Unit, onEditMedication: (Long) -> Unit) {
    StatsFrame(paneTitle = stringResource(R.string.screen_title_s15), bar = null) {
        StatsHeader(index = 2, onTab = onTab)
        if (state.isLoading) return@StatsFrame

        WorstCard(state.worst, onEditMedication)

        // ----- 시간대별 빠뜨린 비율 -----
        val worstPeriod = state.slots.filter { it.hasSchedule && (it.stats.rate ?: 0) > 0 }.maxByOrNull { it.stats.rate ?: 0 }?.period
        SurfaceCard(Modifier.padding(top = 12.dp), padding = 0.dp) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp)) {
                CardTitle(stringResource(R.string.etc_section_slots), stringResource(R.string.etc_recent))
                state.slots.forEachIndexed { i, slot ->
                    SlotMissRow(slot, highlight = slot.period == worstPeriod)
                    if (i < state.slots.lastIndex) HorizontalDivider(color = Line)
                }
            }
        }

        // ----- 요일별 빠뜨린 횟수 -----
        SurfaceCard(Modifier.padding(top = 12.dp)) {
            CardTitle(stringResource(R.string.etc_section_weekday), stringResource(R.string.etc_recent))
            WeekdayMissBars(state.weekdays)
        }

        // ----- 자주 빠뜨린 약 -----
        if (state.meds.isNotEmpty()) {
            SurfaceCard(Modifier.padding(top = 12.dp), padding = 0.dp) {
                Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp)) {
                    CardTitle(stringResource(R.string.etc_section_meds), stringResource(R.string.etc_recent))
                    state.meds.forEachIndexed { i, m ->
                        MedMissRow(i + 1, m)
                        if (i < state.meds.lastIndex) HorizontalDivider(color = Line)
                    }
                }
            }
        }

        StreakCard(state.streaks, state.habitDays)
        PunctualityCard(state.punctuality)
        if (state.averageDelay.isNotEmpty()) DelayCard(state.averageDelay)
    }
}

/** 맨 위 결론 카드(짙은 청록): "밤 9시 Magnesium을 / 28번 중 25번 못 먹었어요" + [알림 시간 바꾸기 ›]. 빠뜨린 적이 없으면 칭찬 한 줄 */
@Composable
private fun WorstCard(worst: WorstDose?, onEditMedication: (Long) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(HeroDeepStart, HeroDeepEnd)))
            .padding(18.dp)
    ) {
        Text(stringResource(R.string.etc_worst_kicker), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f))
        if (worst == null) {
            Text(
                text = stringResource(R.string.etc_worst_none),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(top = 6.dp)
            )
            return@Column
        }
        val when_ = stringResource(R.string.etc_worst_when, stringResource(worst.time.let { periodOf(it) }.look().labelRes), timeText(worst.time))
        Text(
            text = stringResource(R.string.etc_worst_title, when_, worst.name),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = stringResource(R.string.etc_worst_count, worst.stats.due, worst.stats.missed),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        if (worst.weekendMore) {
            Text(
                text = stringResource(R.string.etc_weekend_more),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        Text(
            text = stringResource(R.string.etc_change_time),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = HeroDeepStart,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable(role = Role.Button, onClick = { onEditMedication(worst.medicationId) })
                .padding(vertical = 12.dp)
        )
    }
}

/** 시간대 → 이름·아이콘·아이콘 색·아이콘 바탕 */
private data class SlotLook(val labelRes: Int, val icon: ImageVector, val tint: Color, val bg: Color, val iconSize: Dp = 20.dp)

private fun DayPeriod.look(): SlotLook = when (this) {
    DayPeriod.DAWN -> SlotLook(R.string.s05_period_dawn, Icons.Outlined.Nightlight, Color(0xFF6B7BB0), Color(0xFFEEF0F8))
    DayPeriod.MORNING -> SlotLook(R.string.s05_period_morning, Icons.Outlined.WbSunny, Color(0xFFC96F12), Color(0xFFFFF3E2))
    DayPeriod.LUNCH -> SlotLook(R.string.s05_period_lunch, Icons.Filled.Circle, Color(0xFFD9A21B), Color(0xFFFFF8DC), iconSize = 12.dp)
    DayPeriod.EVENING -> SlotLook(R.string.s05_period_evening, Icons.Outlined.WbTwilight, Color(0xFFC2703D), Color(0xFFFCEFE6))
    DayPeriod.NIGHT -> SlotLook(R.string.s05_period_night, Icons.Filled.Bedtime, Color(0xFF4B5E9C), Color(0xFFE9EDF8))
}

@Composable
private fun SlotIcon(period: DayPeriod, empty: Boolean = false) {
    val look = period.look()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (empty) Color(0xFFF1F4F6) else look.bg)
    ) {
        Icon(look.icon, contentDescription = null, tint = if (empty) Color(0xFFA9B6BC) else look.tint, modifier = Modifier.size(look.iconSize))
    }
}

/** 시간대 한 줄: [아이콘] 아침 / 08:00 · 3개   ━━━   21% / 18/84. 가장 나쁜 줄은 옅은 빨강 테두리 */
@Composable
private fun SlotMissRow(slot: SlotMiss, highlight: Boolean) {
    val name = stringResource(slot.period.look().labelRes)
    val rate = slot.stats.rate
    val empty = !slot.hasSchedule
    val value = when {
        empty -> stringResource(R.string.s15_no_schedule)
        rate == null -> stringResource(R.string.common_na)
        else -> stringResource(R.string.common_percent, rate)
    }
    val sub = if (slot.time != null) stringResource(R.string.etc_slot_sub, timeText(slot.time), slot.medCount) else ""
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (highlight) Modifier.clip(shape).background(Color(0xFFFFF8F8)).border(1.5.dp, Color(0xFFF4B4B4), shape) else Modifier)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = "$name $sub $value" }
    ) {
        SlotIcon(slot.period, empty)
        Column(Modifier.padding(start = 12.dp).width(84.dp)) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (empty) HeatEmptyText else Navy)
            if (sub.isNotEmpty()) Text(sub, fontSize = 11.sp, color = Muted)
        }
        MissBar(if (empty) null else rate, Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(64.dp)) {
            Text(
                text = value,
                fontSize = if (empty) 12.sp else 15.sp,
                fontWeight = if (empty) FontWeight.Medium else FontWeight.ExtraBold,
                color = if (empty || rate == null) HeatEmptyText else missText(rate)
            )
            if (!empty) Text(stringResource(R.string.etc_fraction, slot.stats.missed, slot.stats.due), fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun MissBar(rate: Int?, modifier: Modifier = Modifier) {
    Box(modifier.height(10.dp).clip(RoundedCornerShape(99.dp)).background(ProgressTrack)) {
        if (rate != null && rate > 0) {
            Box(Modifier.fillMaxWidth(rate / 100f).fillMaxHeight().clip(RoundedCornerShape(99.dp)).background(missFill(rate)))
        }
    }
}

/** 요일별 빠뜨린 횟수: 세로 막대 7개(월~일). 가장 많은 요일은 빨강 */
@Composable
private fun WeekdayMissBars(days: List<Pair<java.time.DayOfWeek, Int>>) {
    val names = stringArrayResource(R.array.common_weekdays)
    val max = days.maxOfOrNull { it.second } ?: 0
    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        days.forEach { (day, count) ->
            val name = names[day.value % 7]
            val top = max > 0 && count == max
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(36.dp).clearAndSetSemantics { contentDescription = "$name $count" }
            ) {
                Text(count.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (top) DangerTitle else Navy)
                val h = if (max == 0) 4.dp else (6 + 64f * count / max).dp
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(22.dp)
                        .height(h)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (top) RateLow else Color(0xFFF3C1A0))
                )
                Text(
                    name,
                    fontSize = 12.sp,
                    fontWeight = if (top) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (top) DangerTitle else Muted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/** 자주 빠뜨린 약 한 줄: 1  [약 아이콘] Magnesium / 21:00 취침 전      25/28번 */
@Composable
private fun MedMissRow(rank: Int, m: MedMissUi) {
    val cond = if (m.miss.condition == DoseCondition.NONE) "" else " " + conditionLabel(m.miss.condition)
    val sub = timeText(m.miss.time) + cond
    val count = stringResource(R.string.etc_med_count, m.miss.stats.missed, m.miss.stats.due)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = "$rank ${m.miss.name} $sub $count" }
    ) {
        Text(rank.toString(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Muted, modifier = Modifier.width(22.dp))
        McMedIcon(m.shape, m.color, size = 38.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(m.miss.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(sub, fontSize = 11.sp, color = Muted)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(m.miss.stats.missed.toString(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DangerTitle)
            Text(stringResource(R.string.etc_med_of, m.miss.stats.due), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Muted, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

/** 연속 기록: [불꽃] 3일째 / 최고 7일 + 최근 14일 칸 */
@Composable
private fun StreakCard(streaks: Streaks, days: List<Pair<LocalDate, HabitMark>>) {
    SurfaceCard(Modifier.padding(top = 12.dp)) {
        CardTitle(stringResource(R.string.s15_section_streak))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFFB547), Color(0xFFF0743E))))
            ) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            StreakValue(stringResource(R.string.s15_streak_current), streaks.current, Modifier.weight(1f).padding(start = 16.dp))
            Box(Modifier.width(1.dp).height(44.dp).background(Line))
            StreakValue(stringResource(R.string.s15_streak_best), streaks.best, Modifier.weight(1f).padding(start = 18.dp))
        }
        if (days.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                days.forEach { (date, mark) ->
                    val label = stringResource(habitLabel(mark))
                    Box(
                        Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(habitColor(mark))
                            .clearAndSetSemantics { contentDescription = "${date.monthValue}.${date.dayOfMonth} $label" }
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                val first = days.first().first
                Text("${first.monthValue}.${first.dayOfMonth}", fontSize = 11.sp, color = Muted, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.s07_day_today), fontSize = 11.sp, color = Muted)
            }
        }
    }
}

private fun habitColor(mark: HabitMark): Color = when (mark) {
    HabitMark.ALL -> RateGood
    HabitMark.SOME -> HeatSome
    HabitMark.NONE -> HeatNone
    HabitMark.EMPTY -> HeatEmpty
    HabitMark.OPEN -> Color(0xFFDCE7EA)
}

private fun habitLabel(mark: HabitMark): Int = when (mark) {
    HabitMark.ALL -> R.string.etc_habit_all
    HabitMark.SOME -> R.string.etc_habit_some
    HabitMark.NONE -> R.string.etc_habit_none
    HabitMark.EMPTY -> R.string.etc_habit_empty
    HabitMark.OPEN -> R.string.etc_habit_open
}

@Composable
private fun StreakValue(label: String, days: Int, modifier: Modifier) {
    val unit = stringResource(R.string.s15_days_unit)
    Column(modifier.clearAndSetSemantics { contentDescription = "$label $days$unit" }) {
        Text(label, fontSize = 12.sp, color = Muted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(days.toString(), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
            Text(unit, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.padding(start = 2.dp, bottom = 5.dp))
        }
    }
}

/** 제시간에 먹었나: 원 그래프(예정 30분 안 비율) + 세 줄 */
@Composable
private fun PunctualityCard(p: Punctuality) {
    SurfaceCard(Modifier.padding(top = 12.dp)) {
        CardTitle(stringResource(R.string.etc_section_on_time), stringResource(R.string.etc_recent))
        if (p.total == 0) {
            Text(stringResource(R.string.etc_no_taken), fontSize = 13.sp, color = Muted)
            return@SurfaceCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RateRing(p.onTimeRate, size = 86.dp)
            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PunctualLine(stringResource(R.string.etc_on_time), p.onTime, strong = true)
                PunctualLine(stringResource(R.string.etc_late), p.late, strong = false)
                PunctualLine(stringResource(R.string.etc_very_late), p.veryLate, strong = false)
            }
        }
    }
}

@Composable
private fun PunctualLine(label: String, count: Int, strong: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal, color = if (strong) Navy else Muted)
        Text(
            stringResource(R.string.etc_times, count),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (strong) Navy else Muted,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

/** 시간대별 평균: 예정보다 몇 분 늦게/일찍. 가운데 세로선 = 예정 시각, 점 = 평균 (±60분이 양 끝) */
@Composable
private fun DelayCard(avg: Map<DayPeriod, Int>) {
    SurfaceCard(Modifier.padding(top = 12.dp)) {
        CardTitle(stringResource(R.string.etc_section_delay), stringResource(R.string.etc_recent))
        DayPeriod.entries.filter { it in avg }.forEachIndexed { i, period ->
            val minutes = avg.getValue(period)
            val name = stringResource(period.look().labelRes)
            val value = when {
                minutes == 0 -> stringResource(R.string.etc_delay_exact)
                minutes > 0 -> stringResource(R.string.etc_delay_late, durationText(minutes))
                else -> stringResource(R.string.etc_delay_early, durationText(-minutes))
            }
            val color = when {
                minutes <= ON_TIME -> HomeChipDoneText
                minutes <= 60 -> HomeChipMissedText
                else -> DangerTitle
            }
            if (i > 0) HorizontalDivider(color = Line)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp)
                    .clearAndSetSemantics { contentDescription = "$name $value" }
            ) {
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Navy, modifier = Modifier.width(48.dp))
                Canvas(Modifier.weight(1f).height(20.dp)) {
                    val y = size.height / 2
                    drawLine(Line, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                    val mid = size.width / 2
                    drawLine(Color(0xFF9AABB2), Offset(mid, 2.dp.toPx()), Offset(mid, size.height - 2.dp.toPx()), strokeWidth = 2.dp.toPx())
                    val x = mid + (minutes.coerceIn(-60, 60) / 60f) * (size.width / 2 - 8.dp.toPx())
                    drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(x, y))
                    drawCircle(when {
                        minutes <= ON_TIME -> RateGood
                        minutes <= 60 -> RateFair
                        else -> RateLow
                    }, radius = 5.5.dp.toPx(), center = Offset(x, y))
                }
                Text(value, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = color, textAlign = TextAlign.End, modifier = Modifier.width(104.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 48.dp, end = 104.dp)) {
            Text(stringResource(R.string.etc_scale_early), fontSize = 11.sp, color = Muted, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.etc_scale_on), fontSize = 11.sp, color = Muted)
            Text(stringResource(R.string.etc_scale_late), fontSize = 11.sp, color = Muted, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}

/** "8분" / "1시간 30분" / "2시간" */
@Composable
private fun durationText(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.etc_minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.etc_hours, minutes / 60)
    else -> stringResource(R.string.etc_hours_minutes, minutes / 60, minutes % 60)
}

/** "제시간" 경계(분) — PatternInsights.ON_TIME_MINUTES와 같다 */
private const val ON_TIME = 30
