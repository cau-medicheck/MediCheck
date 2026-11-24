package com.cau.medicheck.ui.screens.stats

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.domain.PeriodStats
import com.cau.medicheck.domain.SlotRate
import com.cau.medicheck.domain.Streaks
import com.cau.medicheck.domain.WeekOfMonth
import com.cau.medicheck.domain.StatsCalculator
import com.cau.medicheck.ui.navigation.SelectionStore
import com.cau.medicheck.ui.screens.dose.ClockViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/*
 * 통계 화면들의 ViewModel. 모두 같은 모양이다:
 *   기간의 기록을 구독하고(DB가 바뀌면 저절로 새 목록이 온다) + 지금 시각(30초마다) → StatsCalculator로 계산.
 * 계산식은 여기 없고 domain/StatsCalculator.kt에 있다(단위 테스트 대상).
 */

// ---------- 홈의 "최근 7일 복약률" 카드: 오늘을 포함한 최근 7일 ----------

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
                WeekStatsViewModel(userId, container.doseRepository, container.clock)
            }
        }
    }
}

// ---------- S13 주간 통계: 월~일 한 주. ‹ › 로 다른 주 ----------

/**
 * @param monday 보는 주의 월요일
 * @param stats 월~일 7개와 합계
 * @param prevRate 지난주 완료율 (계산할 것이 없으면 null → "지난주 대비"를 그리지 않는다)
 * @param isThisWeek 이번 주면 › 를 막는다(앞날 주는 볼 것이 없다)
 */
data class WeeklyStatsUiState(
    val monday: LocalDate,
    val today: LocalDate,
    val isLoading: Boolean = true,
    val stats: PeriodStats = PeriodStats(emptyList()),
    val prevRate: Int? = null
) {
    val week: WeekOfMonth get() = StatsCalculator.weekOfMonth(monday)
    val isThisWeek: Boolean get() = monday == StatsCalculator.weekStartOf(today)
}

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyStatsViewModel(
    userId: Long,
    doseRepository: DoseRepository,
    clock: Clock = Clock.systemDefaultZone()
) : ClockViewModel(userId, doseRepository, clock) {

    private val monday = MutableStateFlow(StatsCalculator.weekStartOf(LocalDate.now(clock)))

    val uiState: StateFlow<WeeklyStatsUiState> =
        // 지난주와 비교하므로 지난주 월요일 ~ 이번 주 일요일을 한 번에 읽는다
        monday.flatMapLatest { start ->
            doseRepository.observeDoses(userId, start.minusWeeks(1), start.plusDays(6)).map { doses -> start to doses }
        }.combine(now) { (start, doses), now ->
            WeeklyStatsUiState(
                monday = start,
                today = now.toLocalDate(),
                isLoading = false,
                stats = StatsCalculator.week(doses, start, now),
                prevRate = StatsCalculator.week(doses, start.minusWeeks(1), now).total.completionRate
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            WeeklyStatsUiState(monday.value, LocalDate.now(clock))
        )

    /** ‹ ›: 한 주 앞뒤로. 이번 주보다 뒤로는 가지 않는다 */
    fun shiftWeek(delta: Long) {
        val next = monday.value.plusWeeks(delta)
        if (next.isAfter(StatsCalculator.weekStartOf(LocalDate.now(clock)))) return
        monday.value = next
    }

    companion object {
        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                WeeklyStatsViewModel(userId, container.doseRepository, container.clock)
            }
        }
    }
}

// ---------- S14 월간 통계: 보는 달의 1일 ~ 말일 ----------

/** 월간의 주차 한 줄: "1주차 · 11.2 ~ 11.8 · 80%" */
data class WeekRow(val monday: LocalDate, val index: Int, val rate: Int?, val isThisWeek: Boolean)

/**
 * @param month 보는 달 (기록 캘린더와 같은 값)
 * @param prevRate 지난달 완료율 (없으면 "지난달 대비"를 그리지 않는다)
 * @param weeks 그 달의 주차들 (목요일이 그 달에 있는 주)
 */
data class MonthStatsUiState(
    val month: YearMonth,
    val isLoading: Boolean = true,
    val stats: PeriodStats = PeriodStats(emptyList()),
    val prevRate: Int? = null,
    val weeks: List<WeekRow> = emptyList(),
    val today: LocalDate = LocalDate.now()
)

@OptIn(ExperimentalCoroutinesApi::class)
class MonthStatsViewModel(
    userId: Long,
    doseRepository: DoseRepository,
    private val selection: SelectionStore,
    clock: Clock = Clock.systemDefaultZone()
) : ClockViewModel(userId, doseRepository, clock) {

    val uiState: StateFlow<MonthStatsUiState> =
        // 지난달(대비)과, 주차가 앞뒤 달에 걸치는 날까지 함께 읽는다
        selection.selectedMonth.flatMapLatest { month ->
            val from = month.minusMonths(1).atDay(1)
            val to = month.atEndOfMonth().plusDays(6)
            doseRepository.observeDoses(userId, from, to).map { doses -> month to doses }
        }.combine(now) { (month, doses), now ->
            val thisMonday = StatsCalculator.weekStartOf(now.toLocalDate())
            MonthStatsUiState(
                month = month,
                isLoading = false,
                stats = StatsCalculator.month(doses, month, now),
                prevRate = StatsCalculator.month(doses, month.minusMonths(1), now).total.completionRate,
                weeks = StatsCalculator.weeksOfMonth(month).mapIndexed { i, monday ->
                    WeekRow(monday, i + 1, StatsCalculator.week(doses, monday, now).total.completionRate, monday == thisMonday)
                },
                today = now.toLocalDate()
            )
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
                MonthStatsViewModel(userId, container.doseRepository, container.selection, container.clock)
            }
        }
    }
}

// ---------- S15 패턴: 시간대별(최근 7일) + 연속 기록 ----------

data class PatternUiState(
    val isLoading: Boolean = true,
    val slots: List<SlotRate> = emptyList(),
    val streaks: Streaks = Streaks(0, 0)
)

@OptIn(ExperimentalCoroutinesApi::class)
class PatternViewModel(
    userId: Long,
    doseRepository: DoseRepository,
    clock: Clock = Clock.systemDefaultZone()
) : ClockViewModel(userId, doseRepository, clock) {

    val uiState: StateFlow<PatternUiState> =
        today.flatMapLatest { today ->
            doseRepository.observeDoses(userId, today.minusDays(StatsCalculator.STREAK_DAYS - 1), today)
                .map { doses -> today to doses }
        }.combine(now) { (today, doses), now ->
            PatternUiState(
                isLoading = false,
                slots = StatsCalculator.slotRates(doses, today, now),
                streaks = StatsCalculator.streaks(doses, today, now)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatternUiState())

    companion object {
        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                PatternViewModel(userId, container.doseRepository, container.clock)
            }
        }
    }
}
