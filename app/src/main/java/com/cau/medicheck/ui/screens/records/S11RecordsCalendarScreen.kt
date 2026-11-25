package com.cau.medicheck.ui.screens.records

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.theme.TimelineDotMissed
import com.cau.medicheck.ui.theme.TimelineDotSkipped
import com.cau.medicheck.ui.theme.HomeChipMissedText
import com.cau.medicheck.ui.theme.HomeChipSkippedText
import com.cau.medicheck.ui.theme.HomeChipDoneText
import com.cau.medicheck.ui.theme.RateGood
import com.cau.medicheck.ui.components.StatusCountRow
import com.cau.medicheck.ui.components.StatusCount
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.MonthCalendarGrid
import com.cau.medicheck.ui.components.MonthSwitcher
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import java.time.LocalDate

/**
 * S11 기록 캘린더 (v3 recordsCalendar()).
 *
 * 한 달 달력에서 날짜를 고르면 아래 카드에 그날의 완료·건너뜀·미기록 수가 나온다.
 * 점이 찍힌 날은 복용 일정(회차)이 있는 날이다. ‹ ›로 달을 넘겨도 고른 날짜는 그대로다.
 */
@Composable
fun S11RecordsCalendarScreen(
    state: RecordsCalendarUiState,
    onShiftMonth: (Long) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onOpenDay: () -> Unit
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s11)) {
        ScreenIntro()

        SurfaceCard(Modifier.padding(top = 16.dp)) {
            MonthSwitcher(month = state.month, onPrev = { onShiftMonth(-1) }, onNext = { onShiftMonth(1) })
            MonthCalendarGrid(
                month = state.month,
                selected = state.day,
                datesWithDot = state.datesWithDoses,
                onSelect = onSelectDay,
                modifier = Modifier.padding(top = 13.dp)
            )
        }

        // 고른 날짜의 요약: 날짜 + 완료·건너뜀·미복용 칸(일정 탭·통계와 같은 모양) + 넓은 "이날 기록 보기" (2026-11-25)
        SurfaceCard(Modifier.padding(top = 14.dp)) {
            Text(fullDateText(state.day), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            if (!state.isLoading) {
                val c = state.counts
                StatusCountRow(
                    items = listOf(
                        StatusCount(stringResource(R.string.common_count_taken), c.taken, RateGood, HomeChipDoneText),
                        StatusCount(stringResource(R.string.common_count_skipped), c.skipped, TimelineDotSkipped, HomeChipSkippedText),
                        StatusCount(stringResource(R.string.common_count_missed), c.missed, TimelineDotMissed, HomeChipMissedText)
                    ),
                    total = c.taken + c.skipped + c.missed,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            McButton(
                stringResource(R.string.s11_btn_open),
                onOpenDay,
                kind = ButtonKind.LIGHT,
                full = true,
                modifier = Modifier.padding(top = 14.dp)
            )
        }

    }
}
