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
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.ui.components.ToastBus
import com.cau.medicheck.ui.navigation.SelectionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 회차 상세(S10)가 그릴 상태.
 *
 * @param dose 보여 줄 회차. 아직 찾는 중이거나 없으면 null
 * @param display 지금 시각 기준 표시 상태 (배지)
 * @param isFuture 내일 이후 회차인가. 그러면 보기 전용이다(상태 버튼이 눌리지 않는다)
 * @param actualTime "실제 복용 HH:mm"으로 보여 줄 시각. 보여 주지 않으면 null
 * @param shouldClose 주소로 받은 회차를 찾지 못했다 → 화면이 스스로 뒤로 간다
 */
data class DoseDetailUiState(
    val isLoading: Boolean = true,
    val dose: DoseItem? = null,
    val display: DoseDisplayStatus = DoseDisplayStatus.UPCOMING,
    val isFuture: Boolean = false,
    val actualTime: LocalTime? = null,
    val shouldClose: Boolean = false
)

/** DB에서 읽어 온 결과: 어느 열쇠로 읽었고(key), 무엇이 나왔고(dose), 그때의 오늘(today) */
private data class Loaded(val key: DoseKey?, val dose: DoseItem?, val today: LocalDate)

/**
 * 회차 상세(S10)의 ViewModel.
 *
 * 열릴 때: 오늘까지의 기록을 먼저 채우고(refresh) → 보여 줄 회차를 정한 뒤(resolveDoseKey) → 그 회차를 구독한다.
 * 상태를 바꾸는 버튼은 DoseRepository의 함수만 부르고, 결과에 맞는 토스트를 띄운다.
 * DB가 바뀌면 구독 중인 흐름이 새 값을 주므로 화면 상태를 직접 고치지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class S10DoseDetailViewModel(
    private val userId: Long,
    private val hasArgs: Boolean,
    private val argKey: DoseKey?,
    private val doseRepository: DoseRepository,
    private val selection: SelectionStore,
    private val toastBus: ToastBus,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    /** "지금". 30초마다, 그리고 화면이 다시 보일 때(refresh) 새 값이 들어온다. */
    private val now = MutableStateFlow(LocalDateTime.now(clock))
    private val today = now.map { it.toLocalDate() }.distinctUntilChanged()

    /** 보여 줄 회차의 열쇠. 정해지기 전에는 null */
    private val key = MutableStateFlow<DoseKey?>(null)
    /** 어느 회차를 보여 줄지 정하는 일이 끝났는가 */
    private val resolved = MutableStateFlow(false)
    private val shouldClose = MutableStateFlow(false)

    /** 자정이 지나 오늘이 바뀌면 새 오늘 기준으로 다시 구독한다 (내일 회차가 오늘 회차가 되어 버튼이 켜진다). */
    private val dose = combine(key, today) { key, today -> key to today }
        .flatMapLatest { (key, today) ->
            if (key == null) flowOf(Loaded(null, null, today))
            else doseRepository.observeDose(userId, key, today).map { Loaded(key, it, today) }
        }

    val uiState: StateFlow<DoseDetailUiState> =
        combine(dose, now, resolved, shouldClose, key) { (loadedKey, dose, today), now, resolved, close, key ->
            DoseDetailUiState(
                // 열쇠는 정해졌는데 그 회차를 DB에서 아직 못 읽어 왔으면 계속 "찾는 중"이다.
                // (이 조건이 없으면 읽어 오는 잠깐 동안 "회차가 없어요"가 번쩍인다)
                isLoading = !resolved || loadedKey != key,
                dose = dose,
                display = dose?.displayStatus(now) ?: DoseDisplayStatus.UPCOMING,
                isFuture = dose?.date?.isAfter(today) == true,
                actualTime = dose?.actualTimeOrNull(clock.zone),
                shouldClose = close
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DoseDetailUiState())

    init {
        viewModelScope.launch { tickerFlow(clock).collect { now.value = it } }
        viewModelScope.launch {
            doseRepository.refresh(userId)
            val found = resolveDoseKey(userId, hasArgs, argKey, doseRepository, selection, LocalDate.now(clock))
            if (found != null) {
                key.value = found
                selection.setLastDose(found)
            } else if (hasArgs) {
                // 주소로 받은 회차가 없다(삭제된 약, 다른 계정, 틀린 값). 안내하고 화면을 닫는다.
                toastBus.show(R.string.common_toast_dose_not_found)
                shouldClose.value = true
            }
            resolved.value = true
        }
        // 자정이 지나면 새 날의 기록을 만든다.
        viewModelScope.launch { today.drop(1).collect { doseRepository.refresh(userId) } }
    }

    /** 화면이 다시 보일 때: 지금 시각을 바로 다시 읽고 오늘까지의 기록을 채운다. */
    fun refresh() {
        now.value = LocalDateTime.now(clock)
        viewModelScope.launch { doseRepository.refresh(userId) }
    }

    /** 상태 버튼 3개. 지금 상태와 같은 버튼을 눌러도(UNCHANGED) 프로토타입처럼 같은 토스트를 띄운다. */
    fun mark(status: DoseStatus) {
        val key = key.value ?: return
        viewModelScope.launch {
            when (doseRepository.changeStatus(userId, key, status)) {
                StatusChangeResult.CHANGED, StatusChangeResult.UNCHANGED -> toastBus.show(
                    when (status) {
                        DoseStatus.TAKEN -> R.string.common_toast_marked_taken
                        DoseStatus.SKIPPED -> R.string.common_toast_marked_skipped
                        DoseStatus.PENDING -> R.string.common_toast_marked_pending
                    }
                )
                StatusChangeResult.FUTURE -> toastBus.show(R.string.s10_notice_future)
                StatusChangeResult.NOT_FOUND -> toastBus.show(R.string.common_toast_dose_not_found)
            }
        }
    }

    /** "마지막 변경 실행취소": 이 회차의 마지막 변경 하나를 (실제 복용 시각까지) 되돌린다. */
    fun undo() {
        val key = key.value
        viewModelScope.launch {
            val done = key != null && doseRepository.undoLast(userId, key) == UndoResult.UNDONE
            toastBus.show(if (done) R.string.s10_toast_undo_done else R.string.s10_toast_undo_none)
        }
    }

    companion object {
        fun factory(userId: Long, hasArgs: Boolean, argKey: DoseKey?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                S10DoseDetailViewModel(
                    userId, hasArgs, argKey, container.doseRepository, container.selection, container.toastBus
                )
            }
        }
    }
}
