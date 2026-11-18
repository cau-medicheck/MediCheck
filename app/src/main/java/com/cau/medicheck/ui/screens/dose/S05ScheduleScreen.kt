package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DayPeriod
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.DatePickerPopup
import com.cau.medicheck.ui.components.DateScroller
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
import com.cau.medicheck.ui.components.dottedDateText
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.lock.LocalAppLocked
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
        ScreenIntro(stringResource(R.string.s05_subtitle))

        // 2026년 11월                    [날짜 선택] [오늘]
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text(
                text = stringResource(R.string.common_month_title, state.date.year, state.date.monthValue),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Navy,
                modifier = Modifier.weight(1f)
            )
            // TalkBack 이름: "날짜 선택, 2026. 11. 17." (이름만 넣으면 고른 날짜를 읽지 않는다)
            val dateFieldName = stringResource(R.string.s05_cd_date_field) + ", " + dottedDateText(state.date)
            McButton(
                text = stringResource(R.string.s05_cd_date_field),
                onClick = { showDatePicker = true },
                kind = ButtonKind.WHITE,
                small = true,
                modifier = Modifier.semantics { contentDescription = dateFieldName }
            )
            Spacer(Modifier.width(8.dp))
            McButton(stringResource(R.string.s05_btn_today), actions.onToday, kind = ButtonKind.LIGHT, small = true)
        }

        // 옆으로 넘기며 고르는 날짜 띠
        DateScroller(
            today = state.today,
            selected = state.date,
            onSelect = actions.onSelectDay,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
        )

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
