package com.cau.medicheck.ui.screens.records

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.DayRateCard
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.TimelineRow
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.screens.dose.DayDosesUiState

/**
 * S12 날짜별 기록 (v3 recordsDetail()).
 *
 * 고른 날짜의 완료율과 회차 목록. 상태를 바꾸려면 "상세 ›"로 회차 상세(S10)에 간다
 * (이 화면의 줄에는 "바로 복용 완료" 버튼이 없다 — 그 버튼은 홈·일정에만 있다).
 * 수치는 도래한 회차만 센다. 그래서 "총 N회"가 목록의 줄 수보다 적을 수 있다(아직 시각 전인 회차는 빠진다).
 *
 * 상태는 홈·일정과 같은 DayDosesViewModel이 만든다(같은 "선택 날짜의 회차 목록"이므로).
 */
@Composable
fun S12RecordsDetailScreen(
    state: DayDosesUiState,
    onBackToCalendar: () -> Unit,
    onOpenDose: (DoseKey) -> Unit
) {

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s12)) {
        BackLink(stringResource(R.string.s12_back), onBackToCalendar)
        ScreenIntro(fullDateText(state.date), Modifier.padding(vertical = 16.dp))

        // 기록을 읽어 오는 잠깐 동안은 "N/A · 총 0회"가 번쩍이지 않게 아래를 그리지 않는다.
        if (state.isLoading) return@McScreenColumn

        DayRateCard(state.progress, isToday = state.date == state.today)

        SectionTitle(stringResource(R.string.s12_section_list))
        SurfaceCard(padding = 0.dp) {
            if (state.rows.isEmpty()) {
                EmptyState(
                    emoji = Glyph.EMPTY_HOME,
                    title = stringResource(R.string.s12_empty_title),
                    body = stringResource(R.string.s12_empty_body)
                )
            } else {
                // 지난 기록을 보는 화면이라 줄을 누르면 기록 창이 아니라 회차 상세로 간다.
                Column(Modifier.padding(vertical = 8.dp)) {
                    state.rows.forEachIndexed { index, row ->
                        TimelineRow(
                            row = row,
                            isFirst = index == 0,
                            isLast = index == state.rows.lastIndex,
                            onOpen = { onOpenDose(row.key) }
                        )
                    }
                }
            }
        }
    }
}
