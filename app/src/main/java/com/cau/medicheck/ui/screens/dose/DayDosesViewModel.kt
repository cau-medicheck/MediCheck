package com.cau.medicheck.ui.screens.dose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.DoseStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** 화면에 그릴 복용 기록 하나 = 기록 + 지금 시각 기준으로 계산한 표시 상태 */
data class DoseUi(
    val item: DoseItem,
    val displayStatus: DoseDisplayStatus
)

/** "하루치 복용 기록" 화면이 그릴 상태. 오늘 화면과 기록 화면이 함께 쓴다. */
data class DayDosesUiState(
    val date: LocalDate,
    /** 보고 있는 날짜가 오늘인지 (기록 화면에서 "다음 날" 버튼을 막는 데 쓴다) */
    val isToday: Boolean = true,
    val isLoading: Boolean = true,
    val doses: List<DoseUi> = emptyList()
) {
    val takenCount: Int get() = doses.count { it.displayStatus == DoseDisplayStatus.TAKEN }
    val totalCount: Int get() = doses.size
}

/**
 * 하루치 복용 기록을 보여 주는 화면의 ViewModel.
 *
 * 오늘 화면과 기록 화면이 같은 ViewModel 클래스를 쓴다.
 *   - 오늘 화면(followToday = true): 날짜가 항상 오늘이다. 자정이 지나면 저절로 다음 날로 넘어간다.
 *   - 기록 화면(followToday = false): 사용자가 날짜를 앞뒤로 옮긴다.
 *
 * "깜빡함"은 저장된 값이 아니라 지금 시각과 비교해 계산하는 값이라서,
 * 시간이 흐르면 화면도 바뀌어야 한다. 그래서 30초마다 지금 시각을 다시 읽어 상태를 새로 계산한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DayDosesViewModel(
    private val userId: Long,
    private val doseRepository: DoseRepository,
    private val followToday: Boolean,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    /** 지금 보고 있는 날짜 */
    private val selectedDate = MutableStateFlow(LocalDate.now(clock))

    /** 30초마다 "지금 시각"을 내보내는 흐름 */
    private val ticker: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now(clock))
            delay(TICK_MILLIS)
        }
    }

    /**
     * 보고 있는 날짜의 기록 흐름.
     * flatMapLatest: 날짜가 바뀌면 이전 날짜의 구독을 끊고 새 날짜의 기록을 구독한다.
     */
    private val dosesOfSelectedDate: Flow<Pair<LocalDate, List<DoseItem>>> =
        selectedDate.flatMapLatest { date ->
            doseRepository.observeDoses(userId, date, date).map { date to it }
        }

    /** 기록 흐름과 시각 흐름 중 하나라도 바뀌면 화면 상태를 새로 만든다. */
    val uiState: StateFlow<DayDosesUiState> =
        combine(dosesOfSelectedDate, ticker) { (date, doses), now ->
            // 오늘 화면인데 날짜가 넘어갔으면(자정이 지남) 새 날짜로 옮기고 그날 기록을 만든다.
            if (followToday && date != now.toLocalDate()) {
                selectedDate.value = now.toLocalDate()
                refresh()
            }
            DayDosesUiState(
                date = date,
                isToday = date == now.toLocalDate(),
                isLoading = false,
                doses = doses.map { DoseUi(it, it.displayStatus(now)) }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DayDosesUiState(date = LocalDate.now(clock))
        )

    init {
        refresh()
    }

    /** 비어 있는 날짜의 기록을 오늘까지 채운다. 화면이 다시 보일 때마다 부른다. */
    fun refresh() {
        viewModelScope.launch { doseRepository.refresh(userId) }
    }

    // ---------- 날짜 옮기기 (기록 화면) ----------

    fun goToPreviousDay() {
        selectedDate.value = selectedDate.value.minusDays(1)
    }

    /** 오늘보다 뒤로는 가지 않는다. (미래의 기록은 아직 만들어지지 않았다) */
    fun goToNextDay() {
        val next = selectedDate.value.plusDays(1)
        if (!next.isAfter(LocalDate.now(clock))) selectedDate.value = next
    }

    fun goToDate(date: LocalDate) {
        if (!date.isAfter(LocalDate.now(clock))) selectedDate.value = date
    }

    // ---------- 상태 바꾸기 ----------

    fun markTaken(recordId: Long) = setStatus(recordId, DoseStatus.TAKEN)

    fun markSkipped(recordId: Long) = setStatus(recordId, DoseStatus.SKIPPED)

    /** 잘못 눌렀을 때 응답 전 상태로 되돌린다. */
    fun undo(recordId: Long) = setStatus(recordId, DoseStatus.PENDING)

    private fun setStatus(recordId: Long, status: DoseStatus) {
        // DB가 바뀌면 기록 흐름이 새 목록을 내보내므로, 화면 상태를 직접 고칠 필요가 없다.
        viewModelScope.launch { doseRepository.setStatus(userId, recordId, status) }
    }

    companion object {
        private const val TICK_MILLIS = 30_000L

        fun factory(userId: Long, followToday: Boolean): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MediCheckApplication
                    DayDosesViewModel(userId, app.container.doseRepository, followToday)
                }
            }
    }
}
