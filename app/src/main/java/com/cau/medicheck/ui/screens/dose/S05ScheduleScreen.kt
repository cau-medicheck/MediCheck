package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.DatePickerPopup
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.RecordSheet
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.TimelineRow
import com.cau.medicheck.ui.components.WeekPager
import com.cau.medicheck.ui.components.dottedDateText
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.components.mondayOf
import com.cau.medicheck.ui.lock.LocalAppLocked
import com.cau.medicheck.ui.theme.FieldBorder
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import java.time.LocalDate

/** 일정 화면의 버튼들이 부를 함수 묶음 */
class ScheduleActions(
    val onShiftDay: (Long) -> Unit,
    val onSelectDay: (LocalDate) -> Unit,
    val onToday: () -> Unit,
    val onOpenDose: (DoseKey) -> Unit,
    val onQuickTake: (DoseKey) -> Unit,
    val onQuickSkip: (DoseKey) -> Unit,
    val onAddMedication: () -> Unit
)

/**
 * S05 일정/캘린더 (v3 schedule()).
 *
 * 날짜를 하루씩 또는 달력으로 옮겨 가며, 그날의 회차를 아침·점심·저녁·밤으로 묶어 본다.
 * 내일 이후 날짜는 지금의 약·규칙으로 계산한 "예정 회차"가 보인다(보기 전용).
 */
@OptIn(ExperimentalLayoutApi::class)
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
            onOpenDetail = { sheetKey = null; actions.onOpenDose(sheetRow.key) }
        )
    }

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s05)) {
        // 복약 일정                                   [＋ 약 등록]
        // 2026년 11월
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScreenIntro(
                stringResource(R.string.common_month_title, state.date.year, state.date.monthValue),
                Modifier.weight(1f)
            )
            McButton(stringResource(R.string.s04_btn_add_med), actions.onAddMedication, kind = ButtonKind.WHITE, small = true)
        }

        HorizontalDivider(thickness = 1.dp, color = Line, modifier = Modifier.padding(top = 14.dp))
        // ‹ 11월 16일 – 11월 22일 ›                 [2026. 11. 18. ▦] [오늘]
        // 글자를 키워 한 줄에 안 들어가면 오른쪽 묶음이 다음 줄로 내려간다(FlowRow).
        val monday = mondayOf(state.date)
        FlowRow(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = (-8).dp)) {
                WeekArrow(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.s05_cd_prev_week)) { actions.onShiftDay(-7) }
                Text(
                    text = stringResource(
                        R.string.s05_week_range,
                        monday.monthValue, monday.dayOfMonth, monday.plusDays(6).monthValue, monday.plusDays(6).dayOfMonth
                    ),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = Navy
                )
                WeekArrow(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.s05_cd_next_week)) { actions.onShiftDay(7) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 날짜 칸: 고른 날짜를 보여 주고, 누르면 달력 창이 열린다.
                // TalkBack 이름: "날짜 선택, 2026. 11. 17." (이름만 넣으면 고른 날짜를 읽지 않는다)
                val dateFieldName = stringResource(R.string.s05_cd_date_field) + ", " + dottedDateText(state.date)
                val fieldShape = RoundedCornerShape(8.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(fieldShape)
                        .background(Color.White)
                        .border(1.dp, FieldBorder, fieldShape)
                        .clickable(role = Role.Button, onClick = { showDatePicker = true })
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .clearAndSetSemantics { contentDescription = dateFieldName; role = Role.Button }
                ) {
                    Text(text = dottedDateText(state.date), fontSize = 12.sp, color = Navy)
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = Navy,
                        modifier = Modifier.padding(start = 6.dp).size(15.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                // [오늘]: 날짜 칸과 같은 높이의 작은 버튼
                Text(
                    text = stringResource(R.string.s05_btn_today),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    modifier = Modifier
                        .clip(fieldShape)
                        .background(Color.White)
                        .border(1.dp, FieldBorder, fieldShape)
                        .clickable(role = Role.Button, onClick = actions.onToday)
                        .padding(horizontal = 9.dp, vertical = 8.dp)
                )
            }
        }
        // 한 주씩 옆으로 넘기는 날짜 띠
        WeekPager(
            today = state.today,
            selected = state.date,
            onSelect = actions.onSelectDay,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
        )
        HorizontalDivider(thickness = 1.dp, color = Line, modifier = Modifier.padding(bottom = 16.dp))

        // 요약: "1 / 2회 완료"  "건너뜀 0 · 미기록 1"   [50%]  — 도래한 회차만 센 숫자다.
        val counts = state.counts
        val rate = counts.completionRate
        SurfaceCard(Modifier.padding(top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.s05_summary_count, counts.taken, counts.due),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text(
                        text = stringResource(R.string.s05_summary_detail, counts.skipped, counts.missed),
                        fontSize = 11.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Pill(
                    text = if (rate == null) stringResource(R.string.common_na)
                    else stringResource(R.string.common_percent, rate),
                    kind = PillKind.BLUE
                )
            }
        }

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
                Column(Modifier.padding(bottom = 10.dp)) {
                    state.rows.forEachIndexed { index, row ->
                        TimelineRow(
                            row = row,
                            isFirst = index == 0,
                            isLast = index == state.rows.lastIndex,
                            onOpen = { sheetKey = row.key }
                        )
                    }
                }
            }
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
