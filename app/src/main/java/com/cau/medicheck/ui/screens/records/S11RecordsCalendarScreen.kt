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
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.MonthCalendarGrid
import com.cau.medicheck.ui.components.MonthSwitcher
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.ScreenTitle
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
        ScreenTitle(stringResource(R.string.s11_title), stringResource(R.string.s11_subtitle))

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

        // 고른 날짜의 요약: "11월 16일 월요일" / "완료 1 · 건너뜀 0 · 미기록 1"   [기록 보기 ›]
        SurfaceCard(Modifier.padding(top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(fullDateText(state.day), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text(
                        // 읽어 오는 잠깐 동안은 "완료 0 · …"이 번쩍이지 않게 비워 둔다.
                        text = if (state.isLoading) "" else stringResource(
                            R.string.s11_summary, state.counts.taken, state.counts.skipped, state.counts.missed
                        ),
                        fontSize = 11.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                McButton(stringResource(R.string.s11_btn_open), onOpenDay, kind = ButtonKind.LIGHT, small = true)
            }
        }

        Notice(stringResource(R.string.s11_notice), Modifier.padding(top = 16.dp))
    }
}
