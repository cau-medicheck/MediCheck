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
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.domain.StatsCalculator
import com.cau.medicheck.domain.periodOf
import com.cau.medicheck.ui.components.DoseRowUi
import com.cau.medicheck.ui.components.ToastAction
import com.cau.medicheck.ui.components.ToastBus
import com.cau.medicheck.ui.navigation.SelectionStore
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * "선택한 날짜의 회차 목록" 화면이 그릴 상태. 홈(S04)과 일정(S05)이 함께 쓴다.
 *
 * @param rows 그날의 회차 줄 (예정 시각 → 약 이름 순)
 * @param counts 도래한 회차만 센 횟수. 완료율은 counts.completionRate (null이면 "N/A")
 */
/**
 * 하루의 진행 상황: 그날의 회차를 "지금 기록된 상태"로 센다. 예정 시각이 됐는지는 보지 않는다.
 *
 * 홈 카드·일정의 진행률은 이 값을 쓴다. 그래서 08:00 약을 07:30에 미리 "복용 완료"로 기록해도 바로 올라간다.
 * (통계 화면의 완료율은 그와 달리 예정 시각이 된 회차만 센다 — DoseCounts.completionRate, S20 기준)
 *
 * @param missed 기록하지 않았고 예정 시각이 지난 회차 (미복용)
 * @param upcoming 기록하지 않았고 아직 예정 시각 전인 회차 (예정)
 * @param percent 복용 완료 ÷ 그날 전체 회차 × 100 (반올림). 회차가 없으면 null
 */
data class DayProgress(val taken: Int, val skipped: Int, val missed: Int, val upcoming: Int) {
    /** 아직 기록하지 않은 회차 (미복용 + 예정) */
    val pending: Int get() = missed + upcoming
    val total: Int get() = taken + skipped + pending
    val percent: Int? get() = if (total == 0) null else Math.round(taken * 100f / total)

    companion object {
        fun of(rows: List<DoseRowUi>) = DayProgress(
            taken = rows.count { it.display == DoseDisplayStatus.TAKEN },
            skipped = rows.count { it.display == DoseDisplayStatus.SKIPPED },
            missed = rows.count { it.display == DoseDisplayStatus.MISSED },
            upcoming = rows.count { it.display == DoseDisplayStatus.UPCOMING }
        )
    }
}

data class DayDosesUiState(
    val today: LocalDate,
    val date: LocalDate,
    val isLoading: Boolean = true,
    val rows: List<DoseRowUi> = emptyList(),
    val counts: DoseCounts = DoseCounts(),
    /** 지금 시각 (30초마다 바뀐다). 홈 카드가 "40분 뒤"와 처음 보일 시각을 계산하는 데 쓴다 */
    val now: LocalDateTime = LocalDateTime.of(today, LocalTime.MIN),
    /** 홈 맨 위 카드의 "N개 모두 복용 완료"로 기록한 회차. 다시 누르면 이 회차들만 해제한다 */
    val takenByButton: Set<DoseKey> = emptySet(),
    /** 내일 첫 복약 시각과 그 시각의 약 개수 (홈에서만 채운다. 없으면 null) */
    val tomorrowFirst: Pair<LocalTime, Int>? = null
) {
    /** 하루 진행 상황 (기록한 상태 기준, 시각과 무관). 홈 카드·일정이 쓴다 */
    val progress: DayProgress get() = DayProgress.of(rows)

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
    private val clock: Clock = Clock.systemDefaultZone(),
    /** true면 고른 날짜와 상관없이 언제나 오늘을 본다 (홈). 자정이 지나면 새 오늘로 넘어간다 */
    private val pinToToday: Boolean = false
) : ViewModel() {

    /**
     * "지금". 30초마다 시계가 새 값을 넣고, 화면이 다시 보일 때(refresh)도 바로 새 값을 넣는다.
     * 앱을 밤새 뒤에 두었다가 아침에 열면 30초 시계는 아직 어젯밤에 멈춰 있을 수 있기 때문이다.
     */
    private val now = MutableStateFlow(LocalDateTime.now(clock))

    /** "오늘". 자정이 지날 때만 값이 바뀐다. */
    private val today = now.map { it.toLocalDate() }.distinctUntilChanged()

    /**
     * 홈 맨 위 카드의 "N개 모두 복용 완료"로 기록한 회차들.
     * 버튼을 다시 누르면 이 회차들만 기록 전으로 돌린다(그 전에 따로 기록한 약은 그대로 — 2026-11-23 결정).
     * 앱을 켜 둔 동안만 기억한다(ViewModel이 사라지면 잊는다).
     */
    private val takenByButton = MutableStateFlow<Set<DoseKey>>(emptySet())

    /** 내일 첫 복약(시각, 개수). 홈에서만 구독한다. 오늘 약을 모두 기록했을 때 카드에 보인다 */
    private val tomorrowFirst: Flow<Pair<LocalTime, Int>?> =
        if (!pinToToday) flowOf(null)
        else today.flatMapLatest { today ->
            doseRepository.observeDay(userId, today.plusDays(1), today).map { doses ->
                val first = doses.minOfOrNull { it.time } ?: return@map null
                first to doses.count { it.time == first }
            }
        }

    /**
     * flatMapLatest: 날짜(또는 오늘)가 바뀌면 앞 날짜의 구독을 끊고 새 날짜의 회차를 구독한다.
     * combine: 회차 목록과 지금 시각 중 하나라도 바뀌면 화면 상태를 새로 만든다.
     */
    val uiState: StateFlow<DayDosesUiState> =
        (if (pinToToday) today.map { it to it } else combine(selection.selectedDay, today) { date, today -> date to today })
            .flatMapLatest { (date, today) ->
                doseRepository.observeDay(userId, date, today).map { doses -> Triple(date, today, doses) }
            }
            .combine(now) { (date, today, doses), now ->
                DayDosesUiState(
                    today = today,
                    date = date,
                    isLoading = false,
                    rows = doses.map { it.toRowUi(now, today) },
                    counts = StatsCalculator.count(doses, now),
                    now = now
                )
            }
            .combine(takenByButton) { state, byButton -> state.copy(takenByButton = byButton) }
            .combine(tomorrowFirst) { state, first -> state.copy(tomorrowFirst = first) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DayDosesUiState(
                    today = LocalDate.now(clock),
                    date = if (pinToToday) LocalDate.now(clock) else selection.selectedDay.value
                )
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
        forgetButton(listOf(key))
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
        forgetButton(listOf(key))
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

    /** 기록 창의 "기록 전으로". 기록한 상태를 지우고 미기록으로 돌린다. */
    fun quickPending(key: DoseKey) {
        forgetButton(listOf(key))
        viewModelScope.launch {
            when (doseRepository.changeStatus(userId, key, DoseStatus.PENDING)) {
                StatusChangeResult.CHANGED ->
                    toastBus.show(R.string.common_toast_marked_pending, ToastAction(R.string.s04_toast_undo) { undo(key) })
                StatusChangeResult.UNCHANGED -> toastBus.show(R.string.common_toast_marked_pending)
                StatusChangeResult.FUTURE,
                StatusChangeResult.NOT_FOUND -> toastBus.show(R.string.common_toast_dose_not_found)
            }
        }
    }

    // ---------- 홈 맨 위 카드 (시각 묶음) ----------

    /**
     * 카드의 약 이름·동그라미를 눌렀을 때: 복용 완료면 기록 전으로, 아니면 복용 완료로.
     * 하나씩 바꾼 약은 "버튼으로 기록한 약"에서 뺀다(버튼을 다시 눌러도 이 약은 해제되지 않게).
     */
    fun toggleTaken(row: DoseRowUi) {
        if (row.display == DoseDisplayStatus.TAKEN) quickPending(row.key) else quickTake(row.key)
    }

    /**
     * "N개 모두 복용 완료": 묶음에서 아직 기록 안 한 회차를 한 번에 복용 완료로. 바뀐 회차를 기억해 둔다.
     * 실행취소는 이번에 바꾼 회차만 한 번에 되돌린다.
     */
    fun takeAll(keys: List<DoseKey>) {
        viewModelScope.launch {
            val changed = doseRepository.changeStatuses(userId, keys, DoseStatus.TAKEN)
            if (changed.isEmpty()) return@launch
            takenByButton.value = takenByButton.value + changed
            toastBus.show(
                R.string.s04_toast_taken_n,
                ToastAction(R.string.s04_toast_undo) { undoAll(changed, rememberAsButton = false) },
                changed.size
            )
        }
    }

    /** "모두 복용함 · 다시 누르면 해제": 버튼으로 기록한 회차만 기록 전으로 돌린다. */
    fun releaseAll(keys: List<DoseKey>) {
        val mine = keys.filter { it in takenByButton.value }
        if (mine.isEmpty()) return
        forgetButton(mine)
        viewModelScope.launch {
            val changed = doseRepository.changeStatuses(userId, mine, DoseStatus.PENDING)
            if (changed.isEmpty()) return@launch
            toastBus.show(
                R.string.s04_toast_released_n,
                ToastAction(R.string.s04_toast_undo) { undoAll(changed, rememberAsButton = true) },
                changed.size
            )
        }
    }

    private fun forgetButton(keys: List<DoseKey>) {
        takenByButton.value = takenByButton.value - keys.toSet()
    }

    /**
     * 한 번에 바꾼 것의 "실행취소". undo와 같은 이유로 appScope에서 돈다.
     * @param rememberAsButton true면 되돌린 뒤 다시 "버튼으로 기록한 약"으로 기억한다(해제를 취소한 경우)
     */
    private fun undoAll(keys: List<DoseKey>, rememberAsButton: Boolean) {
        if (rememberAsButton) takenByButton.value = takenByButton.value + keys else forgetButton(keys)
        appScope.launch {
            val undone = doseRepository.undoLastAll(userId, keys)
            toastBus.show(if (undone > 0) R.string.s10_toast_undo_done else R.string.s10_toast_undo_none)
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
        /** @param forHome true면 홈: 고른 날짜와 상관없이 언제나 오늘을 본다(일정에서 날짜를 옮겨도 그대로). */
        fun factory(userId: Long, forHome: Boolean = false): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                DayDosesViewModel(
                    userId, container.doseRepository, container.selection, container.toastBus, container.appScope,
                    clock = container.clock,
                    pinToToday = forHome
                )
            }
        }
    }
}
