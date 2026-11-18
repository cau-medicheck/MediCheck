package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DayPeriod
import com.cau.medicheck.domain.DoseCounts
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.PeriodStats
import com.cau.medicheck.domain.periodOf
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.DateChipStrip
import com.cau.medicheck.ui.components.DoseRowUi
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.LabelValueRow
import com.cau.medicheck.ui.components.LinkText
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.NotificationOffCard
import com.cau.medicheck.ui.components.ProgressBar
import com.cau.medicheck.ui.components.ProvideBodyFontScale
import com.cau.medicheck.ui.components.RowSub
import com.cau.medicheck.ui.components.RowTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.lock.LocalAppLocked
import com.cau.medicheck.ui.theme.Amber
import com.cau.medicheck.ui.theme.CardBackground
import com.cau.medicheck.ui.theme.CardBorder
import com.cau.medicheck.ui.theme.Faint
import com.cau.medicheck.ui.theme.HeroDeepEnd
import com.cau.medicheck.ui.theme.HeroDeepStart
import com.cau.medicheck.ui.theme.HomeChipDoneBg
import com.cau.medicheck.ui.theme.HomeChipDoneText
import com.cau.medicheck.ui.theme.HomeChipMissedBg
import com.cau.medicheck.ui.theme.HomeChipMissedText
import com.cau.medicheck.ui.theme.HomeChipWaitBg
import com.cau.medicheck.ui.theme.HomeChipWaitText
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText
import com.cau.medicheck.ui.theme.ProgressTrack
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.theme.TimelineLine
import java.time.LocalDate

/** 홈 화면의 버튼들이 부를 함수 묶음. (화면 함수의 인자가 너무 길어지지 않게 묶었다) */
class HomeActions(
    val onSelectDay: (LocalDate) -> Unit,
    val onOpenSchedule: () -> Unit,
    val onOpenDose: (DoseKey) -> Unit,
    val onQuickTake: (DoseKey) -> Unit,
    val onQuickSkip: (DoseKey) -> Unit,
    val onAddMedication: () -> Unit,
    val onOpenStats: () -> Unit,
    val onOpenNotificationHelp: () -> Unit
)

/**
 * S04 홈 대시보드 (v3 home()).
 *
 * 위에서 아래로: 큰 카드(인사 + 선택한 날짜의 완료율) → (알림이 꺼져 있으면) 안내 카드
 * → 날짜 줄(오늘 앞뒤 3일) → 그날의 회차 목록 → "＋ 새 약 등록" → 이번 주 리포트 카드.
 *
 * @param showNotificationOffCard 폰의 알림이 꺼져 있는가 (판정은 MainGraph가 한다)
 */
@Composable
fun S04HomeScreen(
    displayName: String,
    state: DayDosesUiState,
    /** 최근 7일 통계 (맨 아래 카드) */
    weekStats: PeriodStats,
    showNotificationOffCard: Boolean,
    actions: HomeActions
) {
    // 지금 기록 창에 띄운 회차. null이면 창이 닫혀 있다. 줄이나 카드의 버튼을 누르면 그 회차로 채운다.
    var sheetKey by remember { mutableStateOf<DoseKey?>(null) }
    val sheetRow = state.rows.firstOrNull { it.key == sheetKey }
    if (sheetRow != null && !LocalAppLocked.current) {   // 앱이 잠긴 동안에는 그리지 않는다(잠금 화면 위에 뜨지 않게)
        RecordSheet(
            row = sheetRow,
            date = state.date,
            isFuture = state.date.isAfter(state.today),
            onDismiss = { sheetKey = null },
            onTaken = { actions.onQuickTake(sheetRow.key); sheetKey = null },
            onSkipped = { actions.onQuickSkip(sheetRow.key); sheetKey = null },
            onOpenDetail = { sheetKey = null; actions.onOpenDose(sheetRow.key) }
        )
    }
    val openSheet: (DoseKey) -> Unit = { sheetKey = it }

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s04)) {
        // ----- 날짜 + 큰 제목 + [＋ 약 등록] -----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = fullDateText(state.date), fontSize = 12.sp, color = Muted)
                Text(
                    text = stringResource(if (state.date == state.today) R.string.s04_title_today else R.string.s04_title_other),
                    fontSize = 25.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.9).sp,
                    color = Navy,
                    modifier = Modifier.padding(top = 2.dp).semantics { heading() }
                )
            }
            Spacer(Modifier.width(10.dp))
            McButton(
                text = stringResource(R.string.s04_btn_add_med),
                onClick = actions.onAddMedication,
                kind = ButtonKind.WHITE,
                small = true
            )
        }

        NextDoseCard(state, onOpenDose = openSheet, modifier = Modifier.padding(top = 16.dp))

        if (showNotificationOffCard) {
            NotificationOffCard(onClick = actions.onOpenNotificationHelp, modifier = Modifier.padding(top = 16.dp))
        }

        // ----- 하루 복약 일정 -----
        SurfaceCard(Modifier.padding(top = 16.dp), padding = 0.dp) {
            ScheduleCard(state, actions, onOpenDose = openSheet)
        }

        // ----- 복약 캘린더 (한 주) -----
        SurfaceCard(Modifier.padding(top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
                Text(
                    text = stringResource(R.string.s04_section_calendar),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Navy,
                    modifier = Modifier.weight(1f).semantics { heading() }
                )
                if (state.date != state.today) {
                    McButton(
                        text = stringResource(R.string.s04_btn_today),
                        onClick = { actions.onSelectDay(state.today) },
                        kind = ButtonKind.WHITE,
                        small = true
                    )
                }
            }
            DateChipStrip(today = state.today, selected = state.date, onSelect = actions.onSelectDay)
        }

        WeekRateCard(weekStats, today = state.today, onOpenStats = actions.onOpenStats, modifier = Modifier.padding(top = 16.dp))
    }
}

/**
 * 홈 맨 아래의 "최근 7일 복약률" 카드: 큰 숫자 + 요일별 막대 일곱 개. (새 시안 MediCheck4.html)
 * 카드를 누르면 통계 화면으로 간다. 값은 주간 통계 화면(S13)과 같은 계산(StatsCalculator.recentWeek)에서 온다.
 *
 * 막대: 옅은 바탕 칸 안에 그날의 완료율만큼 아래에서부터 채운다. 오늘은 진한 청록, 나머지는 옅은 청록.
 * 계산할 회차가 없는 날(N/A)은 바탕 칸만 보인다.
 */
@Composable
private fun WeekRateCard(stats: PeriodStats, today: LocalDate, onOpenStats: () -> Unit, modifier: Modifier = Modifier) {
    val rate = stats.total.completionRate
    val weekdays = stringArrayResource(R.array.common_weekdays)   // "일"…"토"
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBackground)
            .border(1.dp, CardBorder, shape)
            .clickable(onClickLabel = stringResource(R.string.s04_report_action), role = Role.Button, onClick = onOpenStats)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.s04_week_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Navy,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Text("›", fontSize = 20.sp, color = Muted, modifier = Modifier.clearAndSetSemantics { })
        }
        Text(
            text = buildAnnotatedString {
                if (rate == null) append(stringResource(R.string.common_na))
                else {
                    append(rate.toString())
                    withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)) { append(" %") }
                }
            },
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            color = Navy,
            modifier = Modifier.padding(top = 10.dp, bottom = 14.dp)
        )
        // 막대 일곱 개. 요일별 숫자는 통계 화면에서 읽을 수 있으므로 여기서는 그림으로만 둔다.
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }
        ) {
            stats.days.forEach { day ->
                val isToday = day.date == today
                val fraction = (day.counts.completionRate ?: 0) / 100f
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .width(26.dp)
                            .height(58.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ProgressTrack)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isToday) Teal else Teal.copy(alpha = 0.32f))
                        )
                    }
                    Text(
                        text = weekdays[day.date.dayOfWeek.value % 7],   // 월=1 … 일=7 → 배열은 일요일이 0번
                        fontSize = 11.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) Teal else Muted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * 홈 맨 위의 카드: 지금 기록을 기다리는 약 하나와 "복용 기록하기" 버튼. (새 시안 MediCheck.html의 홈 맨 위 카드)
 *
 * 고르는 순서: 시간이 지났는데 기록이 없는 회차 중 가장 이른 것 → 없으면 아직 시간이 안 된 회차 중 가장 이른 것.
 * (rows는 시각 순서로 들어온다) 둘 다 없으면 버튼 없이 안내 한 줄만 보인다.
 *
 * 바탕: 짙은 청록 위에 알약 그림(res/drawable-nodpi/home_pills.png)을 오른쪽에 깐다.
 * 그림은 사진이 아니라 빛과 그림자를 계산해 그린 것이다(만든 방법: docs/art/render_pills.py).
 */
@Composable
private fun NextDoseCard(state: DayDosesUiState, onOpenDose: (DoseKey) -> Unit, modifier: Modifier = Modifier) {
    val next = state.rows.firstOrNull { it.display == DoseDisplayStatus.MISSED }
        ?: state.rows.firstOrNull { it.display == DoseDisplayStatus.UPCOMING }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(HeroDeepStart, HeroDeepEnd)))
    ) {
        // 그림은 오른쪽 위에 둔다. 장식이라 TalkBack이 읽지 않는다(contentDescription = null).
        Image(
            painter = painterResource(R.drawable.home_pills),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 16.dp, y = 8.dp).size(196.dp)
        )
        Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            if (next == null) {
                // 회차가 하나도 없는 날과, 모두 기록한 날을 나눠 말한다.
                Text(
                    text = stringResource(if (state.rows.isEmpty()) R.string.s04_next_none else R.string.s04_next_all_done),
                    fontSize = 19.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                RateBlock(state.counts, Modifier.padding(top = 18.dp))
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClockMark()
                Text(
                    text = stringResource(R.string.s04_next_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Text(
                text = next.name,
                fontSize = 27.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
            // "12:30 · 1캡슐 · 점심 식후" — 시각만 굵게
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) { append(timeText(next.time)) }
                    append("  ·  " + next.dosage)
                    if (next.memo.isNotBlank()) append("  ·  " + next.memo)
                },
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            McButton(
                text = stringResource(R.string.s04_next_action),
                onClick = { onOpenDose(next.key) },
                kind = ButtonKind.WHITE
            )
            RateBlock(state.counts, Modifier.padding(top = 20.dp))
        }
    }
}

/**
 * 카드 아래쪽의 복약 완료율: 가는 선 / "복약 완료율" / 큰 숫자 "33%" / 진행 막대.
 * (새 시안 MediCheck4.html의 홈 카드) 완료율의 분모는 예정 시각이 된 회차다 — 계산은 DoseCounts가 한다.
 */
@Composable
private fun RateBlock(counts: DoseCounts, modifier: Modifier = Modifier) {
    val rate = counts.completionRate   // null이면 아직 예정 시각이 된 회차가 없다
    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.18f)))
        Text(
            text = stringResource(R.string.s04_rate_label),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.88f),
            modifier = Modifier.padding(top = 16.dp)
        )
        // "33" 크게 + "%" 작게. TalkBack에는 "33퍼센트"로 한 번에 읽히도록 한 Text에 넣는다.
        Text(
            text = buildAnnotatedString {
                if (rate == null) append(stringResource(R.string.common_na))
                else {
                    append(rate.toString())
                    withStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold)) { append(" %") }
                }
            },
            fontSize = 36.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            color = Color.White,
            modifier = Modifier.padding(top = 2.dp)
        )
        // 진행 막대. 숫자가 위에 글자로 있으므로 TalkBack은 읽지 않는다.
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .width(150.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.22f))
                .clearAndSetSemantics { }
        ) {
            Box(
                Modifier
                    .fillMaxWidth(((rate ?: 0) / 100f).coerceIn(0f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White)
            )
        }
    }
}

/** 작은 시계 그림(동그라미 + 바늘 둘). 장식이라 TalkBack이 읽지 않는다. */
@Composable
private fun ClockMark() {
    Canvas(Modifier.size(14.dp).clearAndSetSemantics { }) {
        val stroke = size.minDimension * 0.11f
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(Color.White, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
        drawLine(Color.White, c, Offset(c.x, size.height * 0.26f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(Color.White, c, Offset(size.width * 0.68f, c.y), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/**
 * "하루 복약 일정" 카드의 안쪽: 제목 + [일정 보기 ›] / 진행 상황 / [전체 | 기록 전] / 시간표.
 * 탭은 이 화면 안에서만 쓰는 보기 설정이라 ViewModel에 두지 않는다(화면을 돌려도 남도록 rememberSaveable).
 */
@Composable
private fun ScheduleCard(state: DayDosesUiState, actions: HomeActions, onOpenDose: (DoseKey) -> Unit) {
    var pendingOnly by rememberSaveable { mutableStateOf(false) }
    val rows = if (pendingOnly) {
        state.rows.filter { it.display == DoseDisplayStatus.MISSED || it.display == DoseDisplayStatus.UPCOMING }
    } else state.rows

    Column(Modifier.padding(top = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp)) {
            Text(
                text = stringResource(R.string.s04_section_schedule),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Navy,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            LinkText(stringResource(R.string.s04_link_schedule), onClick = actions.onOpenSchedule)
        }

        if (state.rows.isEmpty() && !state.isLoading) {
            EmptyState(
                emoji = Glyph.EMPTY_HOME,
                title = stringResource(R.string.s04_empty_title),
                body = stringResource(R.string.s04_empty_body),
                actionLabel = stringResource(R.string.s04_empty_action),
                onAction = actions.onAddMedication
            )
            return@Column
        }

        // 복용 완료 1 / 3회  ▬▬▭▭
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.s04_progress, state.counts.taken, state.rows.size),
                fontSize = 12.sp,
                color = Muted
            )
            Spacer(Modifier.width(10.dp))
            // 막대는 글자와 같은 내용이라 TalkBack이 읽지 않는다.
            ProgressBar(
                fraction = if (state.rows.isEmpty()) 0f else state.counts.taken / state.rows.size.toFloat(),
                modifier = Modifier.width(84.dp).clearAndSetSemantics { }
            )
        }

        // [전체] [기록 전] — 고른 탭 아래에 굵은 밑줄
        Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp).selectableGroup()) {
            UnderlineTab(stringResource(R.string.s04_tab_all), selected = !pendingOnly) { pendingOnly = false }
            UnderlineTab(stringResource(R.string.s04_tab_pending), selected = pendingOnly) { pendingOnly = true }
        }
        HorizontalDivider(thickness = 1.dp, color = Line, modifier = Modifier.padding(horizontal = 18.dp))

        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.s04_pending_none),
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp)
            )
        } else {
            Column(Modifier.padding(top = 6.dp, bottom = 10.dp)) {
                rows.forEachIndexed { index, row ->
                    TimelineRow(
                        row = row,
                        isFirst = index == 0,
                        isLast = index == rows.lastIndex,
                        onOpen = { onOpenDose(row.key) }
                    )
                }
            }
        }
    }
}

/** 밑줄 탭 하나. 고른 탭은 굵은 글자 + 청록 밑줄. */
@Composable
private fun UnderlineTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 8.dp)
            .width(IntrinsicSize.Max)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Navy else Muted,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 11.dp)
        )
        Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected) Teal else Color.Transparent))
    }
}

/**
 * 시간표의 한 줄:  08:00 ●  약 이름                [상태] ›
 *                  아침  │  1정 · 아침 식후
 *
 * 시각 오른쪽의 세로선이 줄과 줄을 이어 하루의 흐름을 보여 준다. 점은 복용 완료면 채운 청록, 아니면 빈 주황 고리.
 * 줄 전체를 누르면 기록 창(RecordSheet)이 아래에서 올라온다.
 */
@Composable
private fun TimelineRow(row: DoseRowUi, isFirst: Boolean, isLast: Boolean, onOpen: () -> Unit) {
    val done = row.display == DoseDisplayStatus.TAKEN
    val periodRes = when (periodOf(row.time)) {
        DayPeriod.MORNING -> R.string.s05_period_morning
        DayPeriod.LUNCH -> R.string.s05_period_lunch
        DayPeriod.EVENING -> R.string.s05_period_evening
        DayPeriod.NIGHT -> R.string.s05_period_night
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.s04_cd_open_dose), role = Role.Button, onClick = onOpen)
            .height(IntrinsicSize.Min)   // 세로선이 줄의 높이만큼 그려지도록
            .padding(horizontal = 18.dp)
    ) {
        // 시각 + 시간대
        Column(Modifier.widthIn(min = 52.dp).padding(vertical = 13.dp)) {
            Text(text = timeText(row.time), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(text = stringResource(periodRes), fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 3.dp))
        }
        // 세로선 + 점
        Canvas(Modifier.padding(horizontal = 9.dp).width(12.dp).fillMaxHeight().clearAndSetSemantics { }) {
            val x = size.width / 2
            val dotY = 22.dp.toPx()
            val r = 4.dp.toPx()
            drawLine(
                color = TimelineLine,
                start = Offset(x, if (isFirst) dotY else 0f),
                end = Offset(x, if (isLast) dotY else size.height),
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(Color.White, radius = r + 1.5.dp.toPx(), center = Offset(x, dotY))   // 선을 가리는 흰 바탕
            if (done) drawCircle(Teal, radius = r, center = Offset(x, dotY))
            else drawCircle(Amber, radius = r - 0.75.dp.toPx(), center = Offset(x, dotY), style = Stroke(1.5.dp.toPx()))
        }
        // 약 이름 + 복용량·메모
        Column(Modifier.weight(1f).padding(vertical = 13.dp)) {
            RowTitle(row.name)
            RowSub(if (row.memo.isBlank()) row.dosage else row.dosage + " · " + row.memo)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight()) {
            Spacer(Modifier.width(8.dp))
            HomeStatusChip(row.display)
            Text(
                text = "›",
                fontSize = 16.sp,
                color = Faint,
                modifier = Modifier.padding(start = 6.dp).clearAndSetSemantics { }
            )
        }
    }
}

/**
 * 홈 시간표의 상태 표시. 다른 화면의 StatusPill보다 글자를 짧게, 색을 진하게 쓴다
 * (홈은 한눈에 훑어보는 화면이라 "기한 경과 · 미기록" 대신 "미기록"만 적는다. 자세한 상태는 회차 상세에 있다).
 */
@Composable
private fun HomeStatusChip(status: DoseDisplayStatus) {
    val (textRes, background, textColor) = when (status) {
        DoseDisplayStatus.TAKEN -> Triple(R.string.status_pill_taken, HomeChipDoneBg, HomeChipDoneText)
        DoseDisplayStatus.SKIPPED -> Triple(R.string.s04_chip_skipped, HomeChipWaitBg, HomeChipWaitText)
        DoseDisplayStatus.MISSED -> Triple(R.string.s04_chip_missed, HomeChipMissedBg, HomeChipMissedText)
        DoseDisplayStatus.UPCOMING -> Triple(R.string.s04_chip_upcoming, HomeChipWaitBg, HomeChipWaitText)
    }
    Text(
        text = stringResource(textRes),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/**
 * 복약 기록 창: 화면 아래에서 올라오는 창에서 "복용 완료"·"건너뜀"을 바로 기록한다. (새 시안 MediCheck5.html)
 *
 * 화면을 옮기지 않고 기록할 수 있게 하려는 것이다. 실제 복용 시각 입력이나 변경 이력은 "회차 상세 보기"로 들어가서 한다.
 * 내일 이후의 회차는 기록할 수 없으므로 버튼 대신 안내 한 줄을 보인다.
 * 이 창은 따로 뜨는 창이라 본문의 글자 배율이 전해지지 않는다 → 안에서 ProvideBodyFontScale을 한 번 더 부른다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordSheet(
    row: DoseRowUi,
    date: LocalDate,
    isFuture: Boolean,
    onDismiss: () -> Unit,
    onTaken: () -> Unit,
    onSkipped: () -> Unit,
    onOpenDetail: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardBackground,
        dragHandle = null
    ) {
        ProvideBodyFontScale {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp)) {
                // 복약 기록                                  [×]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.s04_sheet_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Muted,
                        modifier = Modifier.weight(1f)
                    )
                    val closeLabel = stringResource(R.string.s04_sheet_close)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = closeLabel, onClick = onDismiss)
                            .clearAndSetSemantics { contentDescription = closeLabel; role = Role.Button }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Navy, modifier = Modifier.size(22.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Avatar(Glyph.DOSE)
                    Spacer(Modifier.weight(1f))
                    HomeStatusChip(row.display)
                }
                Text(
                    text = row.name,
                    fontSize = 25.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.9).sp,
                    color = Navy,
                    modifier = Modifier.padding(top = 14.dp).semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.s04_sheet_when, fullDateText(date), timeText(row.time)),
                    fontSize = 13.sp,
                    color = ParagraphText,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                HorizontalDivider(thickness = 1.dp, color = Line)
                LabelValueRow(stringResource(R.string.s04_sheet_dosage), row.dosage, showDivider = row.memo.isNotBlank())
                if (row.memo.isNotBlank()) LabelValueRow(stringResource(R.string.s04_sheet_memo), row.memo, showDivider = false)
                HorizontalDivider(thickness = 1.dp, color = Line)

                if (isFuture) {
                    Text(
                        text = stringResource(R.string.s04_sheet_future),
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                } else {
                    Row(Modifier.padding(top = 18.dp)) {
                        McButton(
                            text = stringResource(R.string.s10_btn_taken),
                            onClick = onTaken,
                            enabled = row.display != DoseDisplayStatus.TAKEN,
                            modifier = Modifier.weight(1.4f)
                        )
                        Spacer(Modifier.width(8.dp))
                        McButton(
                            text = stringResource(R.string.s10_btn_skipped),
                            onClick = onSkipped,
                            kind = ButtonKind.LIGHT,
                            enabled = row.display != DoseDisplayStatus.SKIPPED,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                LinkText(
                    text = stringResource(R.string.s04_sheet_detail),
                    onClick = onOpenDetail,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp)
                )
            }
        }
    }
}
