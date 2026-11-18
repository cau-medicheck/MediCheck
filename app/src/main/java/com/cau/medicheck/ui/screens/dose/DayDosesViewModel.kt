package com.cau.medicheck.ui.screens.dose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.R
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.data.repository.StatusChangeResult
import com.cau.medicheck.data.repository.UndoResult
import com.cau.medicheck.domain.DayPeriod
import com.cau.medicheck.domain.DoseCounts
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.domain.StatsCalculator
import com.cau.medicheck.domain.periodOf
import com.cau.medicheck.ui.components.DoseRowUi
import com.cau.medicheck.ui.components.ToastAction
import com.cau.medicheck.ui.components.ToastBus
import com.cau.medicheck.ui.navigation.SelectionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * "선택한 날짜의 회차 목록" 화면이 그릴 상태. 홈(S04)과 일정(S05)이 함께 쓴다.
 *
 * @param rows 그날의 회차 줄 (예정 시각 → 약 이름 순)
 * @param counts 도래한 회차만 센 횟수. 완료율은 counts.completionRate (null이면 "N/A")
 */
data class DayDosesUiState(
    val today: LocalDate,
    val date: LocalDate,
    val isLoading: Boolean = true,
    val rows: List<DoseRowUi> = emptyList(),
    val counts: DoseCounts = DoseCounts()
) {
    /** 일정 화면용: 아침 → 점심 → 저녁 → 밤 순서로 묶은 것. 회차가 없는 묶음은 빠진다. */
    val rowsByPeriod: List<Pair<DayPeriod, List<DoseRowUi>>>
        get() = DayPeriod.entries
            .map { period -> period to rows.filter { periodOf(it.time) == period } }
            .filter { it.second.isNotEmpty() }
}

/**
 * 홈(S04)과 일정(S05)의 ViewModel.
 *
 * 선택 날짜는 이 ViewModel이 아니라 SelectionStore에 있다. 그래서 홈에서 고른 날짜가 일정 화면에도 그대로 보인다.
 *
 * 화면 상태는 세 가지가 바뀔 때마다 새로 만들어진다.
 *   ① 선택 날짜  ② 그날의 회차(DB가 바뀌면 저절로 새 목록이 온다)  ③ 지금 시각(30초마다)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DayDosesViewModel(
    private val userId: Long,
    private val doseRepository: DoseRepository,
    private val selection: SelectionStore,
    private val toastBus: ToastBus,
    private val appScope: CoroutineScope,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    /**
     * "지금". 30초마다 시계가 새 값을 넣고, 화면이 다시 보일 때(refresh)도 바로 새 값을 넣는다.
     * 앱을 밤새 뒤에 두었다가 아침에 열면 30초 시계는 아직 어젯밤에 멈춰 있을 수 있기 때문이다.
     */
    private val now = MutableStateFlow(LocalDateTime.now(clock))

    /** "오늘". 자정이 지날 때만 값이 바뀐다. */
    private val today = now.map { it.toLocalDate() }.distinctUntilChanged()

    /**
     * flatMapLatest: 날짜(또는 오늘)가 바뀌면 앞 날짜의 구독을 끊고 새 날짜의 회차를 구독한다.
     * combine: 회차 목록과 지금 시각 중 하나라도 바뀌면 화면 상태를 새로 만든다.
     */
    val uiState: StateFlow<DayDosesUiState> =
        combine(selection.selectedDay, today) { date, today -> date to today }
            .flatMapLatest { (date, today) ->
                doseRepository.observeDay(userId, date, today).map { doses -> Triple(date, today, doses) }
            }
            .combine(now) { (date, today, doses), now ->
                DayDosesUiState(
                    today = today,
                    date = date,
                    isLoading = false,
                    rows = doses.map { it.toRowUi(now, today) },
                    counts = StatsCalculator.count(doses, now)
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DayDosesUiState(today = LocalDate.now(clock), date = selection.selectedDay.value)
            )

    init {
        viewModelScope.launch { tickerFlow(clock).collect { now.value = it } }
        refresh()
        // 자정이 지나면: 오늘을 보고 있었으면 새 오늘로 따라가고, 새 날의 기록을 만든다.
        viewModelScope.launch {
            today.drop(1).collect { newToday ->
                selection.followToday(newToday)
                doseRepository.refresh(userId)
            }
        }
    }

    /** 비어 있는 날짜의 기록을 오늘까지 채운다. 화면이 다시 보일 때마다 부른다. */
    fun refresh() {
        now.value = LocalDateTime.now(clock)
        viewModelScope.launch { doseRepository.refresh(userId) }
    }

    // ---------- 날짜 옮기기 (미래도 볼 수 있다 — 예정 회차를 보기만) ----------

    fun selectDay(date: LocalDate) = selection.selectDay(date)

    fun shiftDay(days: Long) = selection.selectDay(selection.selectedDay.value.plusDays(days))

    fun goToToday() = selection.selectDay(LocalDate.now(clock))

    // ---------- 줄에서 바로 복용 완료 ----------

    /**
     * 회차 줄의 "✓ 복용 완료". 회차 상세의 같은 버튼과 같은 길(changeStatus)로 기록한다.
     * 성공하면 토스트에 "실행취소" 버튼을 5초 동안 붙인다.
     */
    fun quickTake(key: DoseKey) {
        viewModelScope.launch {
            when (doseRepository.changeStatus(userId, key, DoseStatus.TAKEN)) {
                StatusChangeResult.CHANGED ->
                    toastBus.show(R.string.common_toast_marked_taken, ToastAction(R.string.s04_toast_undo) { undo(key) })
                StatusChangeResult.UNCHANGED -> toastBus.show(R.string.common_toast_marked_taken)
                StatusChangeResult.FUTURE,
                StatusChangeResult.NOT_FOUND -> toastBus.show(R.string.common_toast_dose_not_found)
            }
        }
    }

    /** 홈의 기록 창에서 "건너뜀"을 눌렀을 때. quickTake와 같은 흐름이고 상태만 다르다. */
    fun quickSkip(key: DoseKey) {
        viewModelScope.launch {
            when (doseRepository.changeStatus(userId, key, DoseStatus.SKIPPED)) {
                StatusChangeResult.CHANGED ->
                    toastBus.show(R.string.common_toast_marked_skipped, ToastAction(R.string.s04_toast_undo) { undo(key) })
                StatusChangeResult.UNCHANGED -> toastBus.show(R.string.common_toast_marked_skipped)
                StatusChangeResult.FUTURE,
                StatusChangeResult.NOT_FOUND -> toastBus.show(R.string.common_toast_dose_not_found)
            }
        }
    }

    /**
     * 토스트의 "실행취소". viewModelScope가 아니라 앱 전체의 작업 공간(appScope)에서 돈다.
     * 버튼을 누르기 전에 다른 탭으로 가서 이 ViewModel이 없어졌더라도 끝까지 실행되게 하려는 것이다.
     */
    private fun undo(key: DoseKey) {
        appScope.launch {
            val done = doseRepository.undoLast(userId, key) == UndoResult.UNDONE
            toastBus.show(if (done) R.string.s10_toast_undo_done else R.string.s10_toast_undo_none)
        }
    }

    companion object {
        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                DayDosesViewModel(
                    userId, container.doseRepository, container.selection, container.toastBus, container.appScope,
                    clock = container.clock
                )
            }
        }
    }
}
