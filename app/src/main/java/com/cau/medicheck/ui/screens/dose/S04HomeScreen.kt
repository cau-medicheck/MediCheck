package com.cau.medicheck.ui.screens.dose

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.PeriodStats
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.LinkText
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.NotificationOffCard
import com.cau.medicheck.ui.components.ProgressBar
import com.cau.medicheck.ui.components.RecordSheet
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.TimelineGroup
import com.cau.medicheck.ui.components.groupBySameTime
import com.cau.medicheck.ui.lock.LocalAppLocked
import com.cau.medicheck.ui.theme.CardBackground
import com.cau.medicheck.ui.theme.CardBorder
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ProgressTrack
import com.cau.medicheck.ui.theme.Teal
import java.time.LocalDate

/** 홈 화면의 버튼들이 부를 함수 묶음. (화면 함수의 인자가 너무 길어지지 않게 묶었다) */
class HomeActions(
    val onOpenSchedule: () -> Unit,
    val onOpenDose: (DoseKey) -> Unit,
    val onQuickTake: (DoseKey) -> Unit,
    val onQuickSkip: (DoseKey) -> Unit,
    val onQuickPending: (DoseKey) -> Unit,
    /** 맨 위 카드: 약 이름·동그라미 / "N개 모두 복용 완료" / 다시 눌러 해제 */
    val slotCard: SlotCardActions,
    val onAddMedication: () -> Unit,
    val onOpenStats: () -> Unit,
    val onOpenNotificationHelp: () -> Unit
)

/**
 * S04 홈 대시보드 (v3 home()).
 *
 * 위에서 아래로: 맨 위 카드(날짜 + 지금 먹을 약 묶음, 옆으로 넘겨 다른 시각) → (알림이 꺼져 있으면) 안내 카드
 * → 하루 복약 일정(같은 시각은 한 상자) → 최근 7일 복약률.
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
            onOpenDetail = { sheetKey = null; actions.onOpenDose(sheetRow.key) },
            onPending = { actions.onQuickPending(sheetRow.key); sheetKey = null }
        )
    }
    val openSheet: (DoseKey) -> Unit = { sheetKey = it }

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s04)) {
        // ----- 맨 위 카드: 날짜 + 지금 먹을 약 묶음 -----
        // 홈은 언제나 오늘의 복약만 보여 준다(날짜를 옮겨 보는 것은 일정 탭에서 한다). 큰 제목은 두지 않고 날짜는 카드 안에 적는다.
        SlotHeroCard(state, actions.slotCard)

        if (showNotificationOffCard) {
            NotificationOffCard(onClick = actions.onOpenNotificationHelp, modifier = Modifier.padding(top = 16.dp))
        }

        // ----- 하루 복약 일정 -----
        SurfaceCard(Modifier.padding(top = 16.dp), padding = 0.dp) {
            ScheduleCard(state, actions, onOpenDose = openSheet)
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
                fontWeight = FontWeight.Bold,
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
            fontWeight = FontWeight.Bold,
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
                fontWeight = FontWeight.Bold,
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
                text = stringResource(R.string.s04_progress, state.progress.taken, state.progress.total),
                fontSize = 12.sp,
                color = Muted
            )
            Spacer(Modifier.width(10.dp))
            // 막대는 글자와 같은 내용이라 TalkBack이 읽지 않는다.
            ProgressBar(
                fraction = (state.progress.percent ?: 0) / 100f,
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
            // 같은 시각의 약들은 한 묶음으로 (rows는 이미 시각 순서라 이웃한 것끼리만 묶으면 된다)
            val groups = groupBySameTime(rows)
            Column(Modifier.padding(top = 6.dp, bottom = 10.dp)) {
                groups.forEachIndexed { index, group ->
                    TimelineGroup(
                        rows = group,
                        isFirst = index == 0,
                        isLast = index == groups.lastIndex,
                        onOpen = { row -> onOpenDose(row.key) }
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
