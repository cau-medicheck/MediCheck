package com.cau.medicheck.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.domain.StatsCalculator
import com.cau.medicheck.domain.WeekStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** 통계 화면이 그릴 상태. */
data class StatsUiState(
    val isLoading: Boolean = true,
    /** 보고 있는 주의 통계. 아직 읽기 전이면 null */
    val week: WeekStats? = null,
    /** 보고 있는 주가 이번 주인지 ("다음 주" 버튼을 막는 데 쓴다) */
    val isCurrentWeek: Boolean = true,
    /** 오늘 날짜 (그래프에서 오늘 막대를 강조하는 데 쓴다) */
    val today: LocalDate
)

/**
 * 주간 통계 화면의 ViewModel.
 *
 * 보고 있는 주(월요일 날짜)를 기억하고, 그 주의 복용 기록을 받아 StatsCalculator로 통계를 만든다.
 * 기록이 바뀌면(복용 완료를 누르는 등) 통계도 저절로 다시 계산된다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val userId: Long,
    private val doseRepository: DoseRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    /** 지금 보고 있는 주의 월요일 */
    private val weekStart = MutableStateFlow(StatsCalculator.weekStartOf(LocalDate.now(clock)))

    val uiState: StateFlow<StatsUiState> =
        weekStart.flatMapLatest { start ->
            // 그 주의 월요일 ~ 일요일 기록을 구독한다.
            doseRepository.observeDoses(userId, start, start.plusDays(6)).map { doses ->
                val now = LocalDateTime.now(clock)
                StatsUiState(
                    isLoading = false,
                    week = StatsCalculator.weekStats(doses, start, now),
                    isCurrentWeek = start == StatsCalculator.weekStartOf(now.toLocalDate()),
                    today = now.toLocalDate()
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(today = LocalDate.now(clock))
        )

    init {
        refresh()
    }

    /** 비어 있는 날짜의 기록을 오늘까지 채운다. (며칠 만에 앱을 열어도 깜빡한 날이 통계에 잡히게) */
    fun refresh() {
        viewModelScope.launch { doseRepository.refresh(userId) }
    }

    fun goToPreviousWeek() {
        weekStart.value = weekStart.value.minusWeeks(1)
    }

    /** 이번 주보다 뒤로는 가지 않는다. */
    fun goToNextWeek() {
        val next = weekStart.value.plusWeeks(1)
        if (!next.isAfter(StatsCalculator.weekStartOf(LocalDate.now(clock)))) weekStart.value = next
    }

    companion object {
        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MediCheckApplication
                StatsViewModel(userId, app.container.doseRepository)
            }
        }
    }
}
