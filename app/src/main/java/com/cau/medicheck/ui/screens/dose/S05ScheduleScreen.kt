package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DayPeriod
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.components.DatePickerPopup
import com.cau.medicheck.ui.components.DayRateCard
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.RecordSheet
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.TimelineGroup
import com.cau.medicheck.ui.components.groupBySameTime
import com.cau.medicheck.ui.components.WeekPager
import com.cau.medicheck.ui.components.dottedDateText
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.lock.LocalAppLocked
import com.cau.medicheck.ui.theme.FieldBorder
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.Teal
import java.time.LocalDate

/** 일정 화면의 버튼들이 부를 함수 묶음 */
class ScheduleActions(
    val onShiftDay: (Long) -> Unit,
    val onSelectDay: (LocalDate) -> Unit,
    val onToday: () -> Unit,
    val onOpenDose: (DoseKey) -> Unit,
    val onQuickTake: (DoseKey) -> Unit,
    val onQuickSkip: (DoseKey) -> Unit,
    val onQuickPending: (DoseKey) -> Unit,
    val onAddMedication: () -> Unit
)

/**
 * S05 일정/캘린더 (v3 schedule()).
 *
 * 날짜를 하루씩 또는 달력으로 옮겨 가며, 그날의 회차를 아침·점심·저녁·밤으로 묶어 본다.
 * 내일 이후 날짜는 지금의 약·규칙으로 계산한 "예정 회차"가 보인다(보기 전용).
 */
@Composable
fun S05ScheduleScreen(state: DayDosesUiState, actions: ScheduleActions) {
    // 날짜 고르는 창이 열려 있는가
    var showDatePicker by remember { mutableStateOf(false) }

    // 지금 기록 창에 띄운 회차. null이면 닫혀 있다.
    var sheetKey by remember { mutableStateOf<DoseKey?>(null) }
    val sheetRow = state.rows.firstOrNull { it.key == sheetKey }
    if (sheetRow != null && !LocalAppLocked.current) {
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

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s05)) {
        // 탭 화면이라 큰 제목은 두지 않는다(하단 탭 이름이 같은 말이다). 화면 이름은 paneTitle로 TalkBack에 알린다.
        // ‹ 11월 18일 수요일 [달력] ›                         [오늘]
        // 가운데 글자는 지금 고른 날짜다. 화살표는 한 주씩 넘기고, 달력 그림을 누르면 달력 창이 열린다.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).offset(x = (-8).dp)) {
                WeekArrow(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.s05_cd_prev_week)) { actions.onShiftDay(-7) }
                Text(
                    text = fullDateText(state.date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = Navy
                )
                // 달력 그림: TalkBack 이름은 "날짜 선택, 2026. 11. 17."
                val dateFieldName = stringResource(R.string.s05_cd_date_field) + ", " + dottedDateText(state.date)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = { showDatePicker = true })
                        .clearAndSetSemantics { contentDescription = dateFieldName; role = Role.Button }
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Teal, modifier = Modifier.size(20.dp))
                }
                WeekArrow(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.s05_cd_next_week)) { actions.onShiftDay(7) }
            }
            val todayShape = RoundedCornerShape(8.dp)
            Text(
                text = stringResource(R.string.s05_btn_today),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Navy,
                modifier = Modifier
                    .clip(todayShape)
                    .background(Color.White)
                    .border(1.dp, FieldBorder, todayShape)
                    .clickable(role = Role.Button, onClick = actions.onToday)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
        // 한 주씩 옆으로 넘기는 날짜 띠
        WeekPager(
            today = state.today,
            selected = state.date,
            onSelect = actions.onSelectDay,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
        )
        HorizontalDivider(thickness = 1.dp, color = Line, modifier = Modifier.padding(bottom = 16.dp))

        if (state.rows.isEmpty() && !state.isLoading) {
            SurfaceCard(Modifier.padding(top = 16.dp), padding = 0.dp) {
                EmptyState(
                    emoji = Glyph.EMPTY_SCHEDULE,
                    title = stringResource(R.string.s05_empty_title),
                    body = stringResource(R.string.s05_empty_body),
                    actionLabel = stringResource(R.string.s05_empty_action),
                    onAction = actions.onAddMedication
                )
            }
        }

        // 그날의 회차를 카드 하나에 시간표로 그린다. 줄을 누르면 기록 창이 올라온다.
        if (state.rows.isNotEmpty()) {
            SurfaceCard(Modifier.padding(top = 16.dp), padding = 0.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = fullDateText(state.date),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Navy,
                        modifier = Modifier.weight(1f).semantics { heading() }
                    )
                    Text(text = stringResource(R.string.s05_total, state.rows.size), fontSize = 12.sp, color = Muted)
                }
                // 그날의 상태별 횟수(기록 탭이 보여 주던 것): "완료 1 · 건너뜀 0 · 미복용 1 · 예정 2"
                Text(
                    text = stringResource(
                        R.string.s05_counts,
                        state.progress.taken, state.progress.skipped, state.progress.missed, state.progress.upcoming
                    ),
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 4.dp)
                )
                // 홈과 같은 모양: 같은 시각의 약은 옅은 상자 하나로 묶는다 (2026-11-23)
                val groups = groupBySameTime(state.rows)
                Column(Modifier.padding(top = 6.dp, bottom = 10.dp)) {
                    groups.forEachIndexed { index, group ->
                        TimelineGroup(
                            rows = group,
                            isFirst = index == 0,
                            isLast = index == groups.lastIndex,
                            onOpen = { row -> sheetKey = row.key }
                        )
                    }
                }
            }
        }

        // 오늘과 지난 날짜: 그날의 진행률 카드(기록 탭이 보여 주던 것)를 일정 아래에 둔다. 앞날은 아직 시작 전이라 보이지 않는다.
        if (state.rows.isNotEmpty() && !state.date.isAfter(state.today)) {
            DayRateCard(state.progress, isToday = state.date == state.today, modifier = Modifier.padding(top = 16.dp))
        }
    }

    if (showDatePicker) {
        DatePickerPopup(
            initialDate = state.date,
            maxDate = null,   // 미래 날짜도 고를 수 있다
            onDismiss = { showDatePicker = false },
            onDatePicked = { date ->
                actions.onSelectDay(date)
                showDatePicker = false
            }
        )
    }
}

/** 시간대 → 구역 제목 문구 */
private fun DayPeriod.labelRes(): Int = when (this) {
    DayPeriod.DAWN -> R.string.s05_period_dawn
    DayPeriod.MORNING -> R.string.s05_period_morning
    DayPeriod.LUNCH -> R.string.s05_period_lunch
    DayPeriod.EVENING -> R.string.s05_period_evening
    DayPeriod.NIGHT -> R.string.s05_period_night
}

/** 지난주·다음 주로 넘기는 작은 화살표 버튼. 한 줄에 날짜 칸까지 넣어야 해서 32dp로 잡았다. */
@Composable
private fun WeekArrow(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description; role = Role.Button }
    ) {
        Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(22.dp))
    }
}
