package com.cau.medicheck.ui.screens.stats

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.domain.DayPeriod
import com.cau.medicheck.domain.PeriodStats
import com.cau.medicheck.domain.SkipPattern
import com.cau.medicheck.domain.StatsCalculator
import com.cau.medicheck.ui.navigation.SelectionStore
import com.cau.medicheck.ui.screens.dose.ClockViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.YearMonth

/*
 * 통계 화면 세 개의 ViewModel. 셋 다 같은 모양이다:
 *   기간의 기록을 구독하고(DB가 바뀌면 저절로 새 목록이 온다) + 지금 시각(30초마다) → StatsCalculator로 계산.
 * 계산식은 여기 없고 domain/StatsCalculator.kt에 있다(단위 테스트 대상).
 */

// ---------- S13 주간 통계: 오늘을 포함한 최근 7일 ----------

/** @param stats 날짜별 7개(맨 뒤가 오늘)와 합계 */
data class WeekStatsUiState(val isLoading: Boolean = true, val stats: PeriodStats = PeriodStats(emptyList()))

@OptIn(ExperimentalCoroutinesApi::class)
class WeekStatsViewModel(
    userId: Long,
    doseRepository: DoseRepository,
    clock: Clock = Clock.systemDefaultZone()
) : ClockViewModel(userId, doseRepository, clock) {

    val uiState: StateFlow<WeekStatsUiState> =
        // 자정이 지나 오늘이 바뀌면 기간이 하루 밀리므로 범위를 다시 구독한다.
        today.flatMapLatest { today ->
            doseRepository.observeDoses(userId, today.minusDays(StatsCalculator.WEEK_DAYS - 1), today)
                .map { doses -> today to doses }
        }.combine(now) { (today, doses), now ->
            WeekStatsUiState(isLoading = false, stats = StatsCalculator.recentWeek(doses, today, now))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekStatsUiState())

    companion object {
        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                WeekStatsViewModel(userId, container.doseRepository)
            }
        }
    }
}

// ---------- S14 월간 통계: 보는 달의 1일 ~ 말일 ----------

/** @param month 보는 달 (기록 캘린더와 같은 값) */
data class MonthStatsUiState(
    val month: YearMonth,
    val isLoading: Boolean = true,
    val stats: PeriodStats = PeriodStats(emptyList())
)

@OptIn(ExperimentalCoroutinesApi::class)
class MonthStatsViewModel(
    userId: Long,
    doseRepository: DoseRepository,
    private val selection: SelectionStore,
    clock: Clock = Clock.systemDefaultZone()
) : ClockViewModel(userId, doseRepository, clock) {

    val uiState: StateFlow<MonthStatsUiState> =
        selection.selectedMonth.flatMapLatest { month ->
            doseRepository.observeDoses(userId, month.atDay(1), month.atEndOfMonth()).map { doses -> month to doses }
        }.combine(now) { (month, doses), now ->
            MonthStatsUiState(month = month, isLoading = false, stats = StatsCalculator.month(doses, month, now))
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MonthStatsUiState(selection.selectedMonth.value)
        )

    /** ‹ ›: 보는 달을 넘긴다. 기록 캘린더의 달도 같이 바뀐다(같은 값을 쓴다). */
    fun shiftMonth(delta: Long) = selection.shiftMonth(delta)

    companion object {
        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                MonthStatsViewModel(userId, container.doseRepository, container.selection)
            }
        }
    }
}

// ---------- S15 건너뜀 패턴: 오늘을 포함한 최근 30일 ----------

data class SkipPatternUiState(
    val isLoading: Boolean = true,
    val pattern: SkipPattern = SkipPattern(DayPeriod.entries.associateWith { 0 }, 0)
)

@OptIn(ExperimentalCoroutinesApi::class)
class SkipPatternViewModel(
    userId: Long,
    doseRepository: DoseRepository,
    clock: Clock = Clock.systemDefaultZone()
) : ClockViewModel(userId, doseRepository, clock) {

    val uiState: StateFlow<SkipPatternUiState> =
        today.flatMapLatest { today ->
            doseRepository.observeDoses(userId, today.minusDays(StatsCalculator.PATTERN_DAYS - 1), today)
                .map { doses -> today to doses }
        }.combine(now) { (today, doses), now ->
            SkipPatternUiState(isLoading = false, pattern = StatsCalculator.skipPattern(doses, today, now))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SkipPatternUiState())

    companion object {
        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                SkipPatternViewModel(userId, container.doseRepository)
            }
        }
    }
}
