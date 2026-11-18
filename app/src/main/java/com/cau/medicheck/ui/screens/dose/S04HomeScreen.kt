package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DayPeriod
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.periodOf
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.DateChipStrip
import com.cau.medicheck.ui.components.DoseRowUi
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.LinkText
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.NotificationOffCard
import com.cau.medicheck.ui.components.ProgressBar
import com.cau.medicheck.ui.components.RowSub
import com.cau.medicheck.ui.components.RowTitle
import com.cau.medicheck.ui.components.StatusPill
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.Amber
import com.cau.medicheck.ui.theme.Faint
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Mint
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.NoticeBorder
import com.cau.medicheck.ui.theme.NoticeText
import com.cau.medicheck.ui.theme.ParagraphText
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.theme.TimelineLine
import java.time.LocalDate

/** 홈 화면의 버튼들이 부를 함수 묶음. (화면 함수의 인자가 너무 길어지지 않게 묶었다) */
class HomeActions(
    val onSelectDay: (LocalDate) -> Unit,
    val onOpenSchedule: () -> Unit,
    val onOpenDose: (DoseKey) -> Unit,
    val onQuickTake: (DoseKey) -> Unit,
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
    showNotificationOffCard: Boolean,
    actions: HomeActions
) {
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

        NextDoseCard(state, onOpenDose = actions.onOpenDose, modifier = Modifier.padding(top = 16.dp))

        if (showNotificationOffCard) {
            NotificationOffCard(onClick = actions.onOpenNotificationHelp, modifier = Modifier.padding(top = 16.dp))
        }

        // ----- 하루 복약 일정 -----
        SurfaceCard(Modifier.padding(top = 16.dp), padding = 0.dp) {
            ScheduleCard(state, actions)
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

        SurfaceCard(Modifier.padding(top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.s04_report_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text(
                        text = stringResource(R.string.s04_report_body),
                        fontSize = 11.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                McButton(
                    text = stringResource(R.string.s04_report_action),
                    onClick = actions.onOpenStats,
                    kind = ButtonKind.LIGHT,
                    small = true
                )
            }
        }
    }
}

/**
 * 홈 맨 위의 카드: 지금 기록을 기다리는 약 하나와 "복용 기록하기" 버튼. (새 시안 MediCheck.html의 홈 맨 위 카드)
 *
 * 고르는 순서: 시간이 지났는데 기록이 없는 회차 중 가장 이른 것 → 없으면 아직 시간이 안 된 회차 중 가장 이른 것.
 * (rows는 시각 순서로 들어온다) 둘 다 없으면 버튼 없이 안내 한 줄만 보인다.
 */
@Composable
private fun NextDoseCard(state: DayDosesUiState, onOpenDose: (DoseKey) -> Unit, modifier: Modifier = Modifier) {
    val next = state.rows.firstOrNull { it.display == DoseDisplayStatus.MISSED }
        ?: state.rows.firstOrNull { it.display == DoseDisplayStatus.UPCOMING }
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Mint)
            .border(1.dp, NoticeBorder, shape)
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        if (next == null) {
            // 회차가 하나도 없는 날과, 모두 기록한 날을 나눠 말한다.
            Text(
                text = stringResource(if (state.rows.isEmpty()) R.string.s04_next_none else R.string.s04_next_all_done),
                fontSize = 19.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClockMark()
            Text(
                text = stringResource(R.string.s04_next_label),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = NoticeText,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Text(
            text = next.name,
            fontSize = 27.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            color = Navy,
            modifier = Modifier.padding(top = 8.dp)
        )
        // "12:30 · 1캡슐 · 점심 식후" — 시각만 굵게
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Navy)) { append(timeText(next.time)) }
                append("  ·  " + next.dosage)
                if (next.memo.isNotBlank()) append("  ·  " + next.memo)
            },
            fontSize = 13.sp,
            color = ParagraphText,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        McButton(text = stringResource(R.string.s04_next_action), onClick = { onOpenDose(next.key) })
    }
}

/** 작은 시계 그림(동그라미 + 바늘 둘). 장식이라 TalkBack이 읽지 않는다. */
@Composable
private fun ClockMark() {
    Canvas(Modifier.size(14.dp).clearAndSetSemantics { }) {
        val stroke = size.minDimension * 0.11f
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(NoticeText, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
        drawLine(NoticeText, c, Offset(c.x, size.height * 0.26f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(NoticeText, c, Offset(size.width * 0.68f, c.y), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/**
 * "하루 복약 일정" 카드의 안쪽: 제목 + [일정 보기 ›] / 진행 상황 / [전체 | 기록 전] / 시간표.
 * 탭은 이 화면 안에서만 쓰는 보기 설정이라 ViewModel에 두지 않는다(화면을 돌려도 남도록 rememberSaveable).
 */
@Composable
private fun ScheduleCard(state: DayDosesUiState, actions: HomeActions) {
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
                        onOpen = { actions.onOpenDose(row.key) }
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
 * 줄 전체를 누르면 회차 상세로 간다(기록은 거기서 한다).
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
            StatusPill(row.display)
            Text(
                text = "›",
                fontSize = 16.sp,
                color = Faint,
                modifier = Modifier.padding(start = 6.dp).clearAndSetSemantics { }
            )
        }
    }
}
