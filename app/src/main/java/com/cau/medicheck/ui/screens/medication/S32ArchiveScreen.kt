package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.OptionRow
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.theme.MetricText
import com.cau.medicheck.ui.theme.Muted
import java.time.LocalDate

/**
 * S32 과거 기록 보존 (v3 archiveScreen()).
 *
 * 오늘보다 앞선 날짜의 복용 기록이 몇 건 남아 있는지와, 최근 10개 날짜를 보여 준다.
 * 약을 고치거나 지워도 이 숫자는 줄지 않는다 — 기록마다 그때의 약 이름·복용량·시각을 찍어 두기 때문이다.
 */
@Composable
fun S32ArchiveScreen(
    state: ArchiveUiState,
    onBack: () -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s32)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenIntro(modifier = Modifier.padding(vertical = 16.dp))

        // "보존된 과거 복용 회차"  96건            [시연 데이터]
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.s32_count_label), fontSize = 12.5.sp, color = Muted)
                    Text(
                        text = stringResource(R.string.s32_count, state.archive.total),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp,
                        color = MetricText
                    )
                }
                // "시연 데이터" 배지는 샘플 계정에서만 보인다.
                if (state.isSample) Pill(stringResource(R.string.s32_badge_sample), PillKind.MINT)
            }
        }

        SectionTitle(stringResource(R.string.s32_section_dates))
        SurfaceCard(padding = 0.dp) {
            val days = state.archive.days
            if (days.isEmpty() && !state.isLoading) {
                EmptyState(
                    emoji = Glyph.TAB_SCHEDULE,
                    title = stringResource(R.string.s32_empty_title),
                    body = stringResource(R.string.s32_empty_body)
                )
            } else {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    days.forEachIndexed { index, (date, count) ->
                        OptionRow(
                            title = fullDateText(date),
                            description = stringResource(R.string.s32_day_count, count),
                            onClick = { onOpenDay(date) },
                            showDivider = index != days.lastIndex
                        )
                    }
                }
            }
        }

    }
}
