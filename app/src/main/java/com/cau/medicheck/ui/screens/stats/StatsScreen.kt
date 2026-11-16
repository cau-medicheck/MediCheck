package com.cau.medicheck.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DayStats
import com.cau.medicheck.domain.DoseCounts
import com.cau.medicheck.domain.WeekStats
import com.cau.medicheck.ui.screens.medication.currentLocale
import com.cau.medicheck.ui.screens.medication.formatDate
import com.cau.medicheck.ui.screens.medication.shortDayName
import com.cau.medicheck.ui.theme.MediCheckTheme
import java.time.LocalDate

/**
 * 주간 통계 화면.
 *
 *   ←  11월 16일 ~ 11월 22일  →        보는 주를 옮긴다 (이번 주보다 뒤로는 못 감)
 *   복약 완료율 60%                     복용 완료 ÷ (복용 완료 + 건너뜀 + 깜빡함)
 *   복용 완료 3 · 깜빡함 1 · 건너뜀 1     세 가지를 따로 보여 준다
 *   요일별 막대 그래프                   막대 높이 = 그날의 완료율
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.action_logout)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val week = state.week
        if (state.isLoading || week == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            WeekNavigator(week, state.isCurrentWeek, onPreviousWeek, onNextWeek)
            Spacer(Modifier.height(12.dp))
            RateCard(week.total)
            Spacer(Modifier.height(12.dp))
            CountsRow(week.total)
            Spacer(Modifier.height(12.dp))
            DailyChartCard(week.days, state.today)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.stats_rate_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 보는 주를 옮기는 줄:  ←  시작일 ~ 종료일  → */
@Composable
private fun WeekNavigator(
    week: WeekStats,
    isCurrentWeek: Boolean,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val locale = currentLocale()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onPreviousWeek) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_previous_week)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            if (isCurrentWeek) {
                Text(
                    text = stringResource(R.string.stats_this_week),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(
                    R.string.period_range,
                    formatDate(week.weekStart, locale),
                    formatDate(week.weekEnd, locale)
                ),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
        // 이번 주를 보고 있으면 "다음 주" 버튼을 잠근다.
        IconButton(onClick = onNextWeek, enabled = !isCurrentWeek) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.action_next_week)
            )
        }
    }
}

/** 가장 크게 보여 주는 숫자: 이번 주 완료율 */
@Composable
private fun RateCard(total: DoseCounts) {
    val rate = total.completionRate
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_rate_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                // 먹었어야 할 복약이 하나도 없으면 완료율을 계산할 수 없다.
                text = if (rate == null) {
                    stringResource(R.string.stats_no_records)
                } else {
                    stringResource(R.string.stats_rate_value, rate)
                },
                style = if (rate == null) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.displayLarge
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (rate != null) {
                Text(
                    text = stringResource(R.string.stats_rate_detail, total.taken, total.due),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 복용 완료 · 깜빡함 · 건너뜀 횟수를 나란히 */
@Composable
private fun CountsRow(total: DoseCounts) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        CountCard(
            label = stringResource(R.string.status_taken),
            count = total.taken,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        CountCard(
            label = stringResource(R.string.status_missed),
            count = total.missed,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        CountCard(
            label = stringResource(R.string.status_skipped),
            count = total.skipped,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CountCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium, color = color)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 요일별 막대 그래프 카드 */
@Composable
private fun DailyChartCard(days: List<DayStats>, today: LocalDate) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.stats_daily_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    DayBar(day = day, isToday = day.date == today, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 하루치 막대 하나.
 *
 * 그래프 라이브러리 없이 상자 두 개로 그린다.
 *   바깥 상자: 옅은 색의 빈 통 (높이 고정)
 *   안쪽 상자: 완료율만큼 아래에서부터 채운 진한 색
 * 그 아래에 "복용/먹었어야 할 횟수"와 요일 이름을 적는다.
 */
@Composable
private fun DayBar(day: DayStats, isToday: Boolean, modifier: Modifier = Modifier) {
    val counts = day.counts
    val rate = counts.completionRate
    val locale = currentLocale()

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .height(BAR_HEIGHT)
                .width(BAR_WIDTH)
                .clip(RoundedCornerShape(6.dp))
                // 카드보다 밝은 색으로 빈 통을 그려서, 기록이 없는 날도 자리가 보이게 한다.
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (rate != null && rate > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(rate / 100f)   // 완료율만큼만 채운다
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            // 먹었어야 할 복약이 없던 날은 "-"
            text = if (counts.due == 0) "-" else stringResource(R.string.stats_day_count, counts.taken, counts.due),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = shortDayName(day.date.dayOfWeek, locale),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private val BAR_HEIGHT = 120.dp
private val BAR_WIDTH = 24.dp

// 아래는 Android Studio 미리 보기용 코드다. 앱 실행에는 쓰이지 않는다.

@Preview(showBackground = true)
@Composable
private fun StatsScreenPreview() {
    val monday = LocalDate.of(2026, 11, 16)
    val sample = WeekStats(
        weekStart = monday,
        days = listOf(
            DoseCounts(taken = 2),
            DoseCounts(taken = 1, missed = 1),
            DoseCounts(skipped = 1, upcoming = 1),
            DoseCounts(), DoseCounts(), DoseCounts(), DoseCounts()
        ).mapIndexed { index, counts -> DayStats(monday.plusDays(index.toLong()), counts) }
    )
    MediCheckTheme(dynamicColor = false) {
        StatsScreen(
            state = StatsUiState(isLoading = false, week = sample, today = monday.plusDays(2)),
            onPreviousWeek = {}, onNextWeek = {}, onLogout = {}
        )
    }
}
