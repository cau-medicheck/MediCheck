package com.cau.medicheck.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DayPeriod
import com.cau.medicheck.ui.components.BarChart
import com.cau.medicheck.ui.components.BarColumnUi
import com.cau.medicheck.ui.components.CountTriple
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.MetricNumber
import com.cau.medicheck.ui.components.MonthSwitcher
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.ProgressBar
import com.cau.medicheck.ui.components.ScreenTitle
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SegmentedTabs
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.solidBar
import com.cau.medicheck.ui.theme.ChartBarMuted
import com.cau.medicheck.ui.theme.ChartBottom
import com.cau.medicheck.ui.theme.ChartTop
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText
import com.cau.medicheck.ui.theme.ProgressFill

/*
 * 통계 화면 세 개: S13 주간 · S14 월간 · S15 건너뜀 패턴.
 * 세 화면 위쪽의 탭("주간 | 월간 | 누락 분석")으로 서로 오간다. 계산은 ViewModel(→ StatsCalculator)이 하고
 * 여기서는 받은 값을 그리기만 한다.
 */

/** 통계 화면 맨 위: 큰 제목 + 탭 세 개. index = 지금 화면의 탭 번호(0 주간, 1 월간, 2 누락 분석) */
@Composable
private fun ColumnScope.StatsHeader(titleRes: Int, subtitleRes: Int, index: Int, onTab: (Int) -> Unit) {
    ScreenTitle(stringResource(titleRes), stringResource(subtitleRes))
    SegmentedTabs(
        labels = listOf(
            stringResource(R.string.stats_tab_week),
            stringResource(R.string.stats_tab_month),
            stringResource(R.string.stats_tab_pattern)
        ),
        selectedIndex = index,
        onSelect = onTab,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}

/** 완료율 글자: "80%" 또는 계산할 것이 없으면 "N/A" */
@Composable
private fun rateText(rate: Int?): String =
    if (rate == null) stringResource(R.string.common_na) else stringResource(R.string.common_percent, rate)

// =============================================================================================
// S13 주간 통계
// =============================================================================================

/**
 * S13 주간 통계 (v3 weekStats()).
 * 오늘을 포함한 최근 7일의 완료율과 요일별 막대. 막대는 왼쪽이 6일 전, 오른쪽 끝이 오늘이다.
 */
@Composable
fun S13WeekStatsScreen(state: WeekStatsUiState, onTab: (Int) -> Unit) {
    val total = state.stats.total
    val weekdays = stringArrayResource(R.array.common_weekdays)        // "일"…"토"
    val weekdaysFull = stringArrayResource(R.array.common_weekdays_full)

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s13)) {
        StatsHeader(R.string.s13_title, R.string.s13_subtitle, index = 0, onTab = onTab)
        // 기록을 읽어 오는 잠깐 동안은 "N/A · 0"이 번쩍이지 않게 아래를 그리지 않는다.
        if (state.isLoading) return@McScreenColumn

        SurfaceCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.s13_rate_label), fontSize = 11.sp, color = Muted)
                MetricNumber(rateText(total.completionRate), fontSize = 46, modifier = Modifier.padding(vertical = 6.dp))
                ProgressBar(
                    fraction = (total.completionRate ?: 0) / 100f,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .clearAndSetSemantics { }
                )
                CountTriple(
                    items = listOf(
                        stringResource(R.string.common_count_taken) to total.taken,
                        stringResource(R.string.common_count_skipped) to total.skipped,
                        stringResource(R.string.common_count_missed) to total.missed
                    ),
                    valueSize = 19
                )
            }
        }

        SectionTitle(stringResource(R.string.s13_section_daily))
        SurfaceCard {
            // 위(옅은 청록)에서 아래(진한 청록)로 흐르는 막대 색
            val gradient = Brush.verticalGradient(listOf(ChartTop, ChartBottom))
            val columns = state.stats.days.map { day ->
                val index = day.date.dayOfWeek.value % 7   // 일요일 = 0
                val rate = rateText(day.counts.completionRate)
                BarColumnUi(
                    percent = day.counts.completionRate,
                    topLabel = rate,
                    bottomLabel = weekdays[index],
                    description = stringResource(R.string.s13_cd_bar, weekdaysFull[index], rate),
                    brush = gradient
                )
            }
            BarChart(columns, maxBarHeight = 110.dp, gap = 6.dp)
        }

        Notice(stringResource(R.string.s13_notice), Modifier.padding(top = 16.dp))
    }
}

// =============================================================================================
// S14 월간 통계
// =============================================================================================

/**
 * S14 월간 통계 (v3 monthStats()).
 * 보는 달의 완료율과 일자별 막대. 완료율이 100%인 날만 진한 색, 그 밖(0~99%·계산할 것 없음)은 옅은 색이다.
 * 이번 달이면 오늘 뒤의 날짜는 아직 도래한 회차가 없어 가장 낮은 막대로 보인다.
 */
@Composable
fun S14MonthStatsScreen(state: MonthStatsUiState, onTab: (Int) -> Unit, onShiftMonth: (Long) -> Unit) {
    val total = state.stats.total

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s14)) {
        StatsHeader(R.string.screen_title_s14, R.string.s14_subtitle, index = 1, onTab = onTab)
        // 기록을 읽어 오는 잠깐 동안은 "N/A · 0"이 번쩍이지 않게 아래를 그리지 않는다.
        if (state.isLoading) return@McScreenColumn

        SurfaceCard {
            MonthSwitcher(month = state.month, onPrev = { onShiftMonth(-1) }, onNext = { onShiftMonth(1) })
            Text(
                stringResource(R.string.s14_rate_label),
                fontSize = 11.sp,
                color = Muted,
                modifier = Modifier.padding(top = 16.dp)
            )
            MetricNumber(rateText(total.completionRate), fontSize = 42)
            ProgressBar(
                fraction = (total.completionRate ?: 0) / 100f,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clearAndSetSemantics { }
            )
            Text(
                text = stringResource(R.string.s14_counts, total.taken, total.skipped, total.missed),
                fontSize = 12.sp,
                lineHeight = 21.sp,
                color = ParagraphText,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        SectionTitle(stringResource(R.string.s14_section_daily))
        SurfaceCard {
            val columns = state.stats.days.map { day ->
                val rate = day.counts.completionRate
                BarColumnUi(
                    percent = rate,
                    topLabel = null,
                    bottomLabel = null,
                    // 예: "2026-11-03: 100%", 계산할 것이 없는 날은 "2026-11-20: N/A"
                    description = stringResource(R.string.s14_cd_bar, day.date.toString(), rateText(rate)),
                    brush = solidBar(if (rate == 100) ProgressFill else ChartBarMuted)
                )
            }
            BarChart(columns, maxBarHeight = 115.dp, gap = 2.dp)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.s14_first_day), fontSize = 11.sp, color = Muted)
                Text(stringResource(R.string.s14_last_day, state.month.lengthOfMonth()), fontSize = 11.sp, color = Muted)
            }
        }
    }
}

// =============================================================================================
// S15 건너뜀 패턴
// =============================================================================================

/**
 * S15 건너뜀 패턴 (v3 missed()).
 * 최근 30일 동안 "건너뜀"으로 기록한 횟수를 시간대(아침·점심·저녁·밤)별로 보여 준다.
 * 응답하지 않은 회차(기한 경과 · 미기록)는 건너뜀에 넣지 않고 아래 안내에 따로 적는다.
 */
@Composable
fun S15SkipPatternScreen(state: SkipPatternUiState, onTab: (Int) -> Unit) {
    val pattern = state.pattern
    // 막대 길이의 기준: 네 시간대 중 가장 많은 횟수 (0으로 나누지 않게 최소 1)
    val max = maxOf(1, pattern.skippedByPeriod.values.maxOrNull() ?: 0)

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s15)) {
        StatsHeader(R.string.s15_title, R.string.s15_subtitle, index = 2, onTab = onTab)
        // 기록을 읽어 오는 잠깐 동안은 "N/A · 0"이 번쩍이지 않게 아래를 그리지 않는다.
        if (state.isLoading) return@McScreenColumn

        SurfaceCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.s15_total_label), fontSize = 11.sp, color = Muted)
                MetricNumber(
                    text = pattern.totalSkipped.toString(),
                    fontSize = 45,
                    suffix = stringResource(R.string.s15_unit)
                )
            }
        }

        SectionTitle(stringResource(R.string.s15_section_periods))
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                DayPeriod.entries.forEach { period ->
                    val count = pattern.skippedByPeriod[period] ?: 0
                    Column(Modifier.padding(vertical = 12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(period.labelRes()),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy
                            )
                            Text(stringResource(R.string.s15_count, count), fontSize = 11.sp, color = Muted)
                        }
                        ProgressBar(
                            fraction = count.toFloat() / max,
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .clearAndSetSemantics { }
                        )
                    }
                }
            }
        }

        Notice(
            stringResource(R.string.s15_notice, pattern.overdueUnrecorded, pattern.totalSkipped),
            Modifier.padding(top = 16.dp),
            warn = true
        )    }
}

/** 시간대 → 이름 문구 (일정 화면 S05의 구역 제목과 같은 글자) */
private fun DayPeriod.labelRes(): Int = when (this) {
    DayPeriod.MORNING -> R.string.s05_period_morning
    DayPeriod.LUNCH -> R.string.s05_period_lunch
    DayPeriod.EVENING -> R.string.s05_period_evening
    DayPeriod.NIGHT -> R.string.s05_period_night
}
