package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DayPeriod
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.DatePickerPopup
import com.cau.medicheck.ui.components.DoseRow
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.PickerField
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.dottedDateText
import com.cau.medicheck.ui.components.fullDateText
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

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s05)) {
        ScreenIntro(stringResource(R.string.s05_subtitle))

        // ‹ 전날   11월 17일 화요일   다음날 ›
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            McButton(stringResource(R.string.s05_btn_prev), { actions.onShiftDay(-1) }, kind = ButtonKind.WHITE, small = true)
            Text(
                text = fullDateText(state.date),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
            McButton(stringResource(R.string.s05_btn_next), { actions.onShiftDay(1) }, kind = ButtonKind.WHITE, small = true)
        }

        // [2026. 11. 17.]                오늘로
        // TalkBack 이름: "날짜 선택, 2026. 11. 17." (이름만 넣으면 고른 날짜를 읽지 않는다)
        val dateFieldName = stringResource(R.string.s05_cd_date_field) + ", " + dottedDateText(state.date)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            PickerField(
                text = dottedDateText(state.date),
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .widthIn(max = 180.dp)
                    .semantics { contentDescription = dateFieldName }
            )
            Spacer(Modifier.width(8.dp))
            McButton(stringResource(R.string.s05_btn_today), actions.onToday, kind = ButtonKind.LIGHT, small = true)
        }

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

        // 시간대 묶음: 아침 (1) / 점심 (1) / … 회차가 있는 묶음만 그린다.
        state.rowsByPeriod.forEach { (period, rows) ->
            SectionTitle(
                text = stringResource(period.labelRes()),
                suffix = stringResource(R.string.s05_period_count, rows.size)
            )
            SurfaceCard(padding = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    rows.forEachIndexed { index, row ->
                        DoseRow(
                            row = row,
                            onDetail = { actions.onOpenDose(row.key) },
                            onQuickTake = { actions.onQuickTake(row.key) },
                            showDivider = index != rows.lastIndex
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
