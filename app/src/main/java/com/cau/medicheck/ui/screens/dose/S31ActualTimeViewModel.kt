package com.cau.medicheck.ui.screens.dose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.R
import com.cau.medicheck.data.repository.ActualTimeResult
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.components.ToastBus
import com.cau.medicheck.ui.navigation.SelectionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

/**
 * 실제 복용 시각 기록(S31)이 그릴 상태.
 *
 * @param time 시각 칸에 보일 값. 처음에는 저장된 실제 복용 시각(없으면 예정 시각)
 * @param isSaved 저장이 끝났다 → 화면이 스스로 뒤로(회차 상세로) 간다
 */
data class ActualTimeUiState(
    val isLoading: Boolean = true,
    val dose: DoseItem? = null,
    val time: LocalTime = LocalTime.MIDNIGHT,
    val isSaved: Boolean = false
)

/**
 * 실제 복용 시각 기록(S31)의 ViewModel.
 * 시각을 고르는 동안에는 아무것도 저장하지 않는다. "시각 저장 및 복용 완료 기록"을 눌렀을 때 한 번에 저장한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class S31ActualTimeViewModel(
    private val userId: Long,
    private val hasArgs: Boolean,
    private val argKey: DoseKey?,
    private val doseRepository: DoseRepository,
    private val selection: SelectionStore,
    private val toastBus: ToastBus,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val key = MutableStateFlow<DoseKey?>(null)
    private val resolved = MutableStateFlow(false)
    /** 사용자가 고른 시각. 아직 고르지 않았으면 null */
    private val picked = MutableStateFlow<LocalTime?>(null)
    private val saved = MutableStateFlow(false)

    /** (어느 열쇠로 읽었는지, 읽어 온 회차) */
    private val dose = key.flatMapLatest { key ->
        if (key == null) flowOf(null to null)
        else doseRepository.observeDose(userId, key, LocalDate.now(clock)).map { key to it }
    }

    val uiState: StateFlow<ActualTimeUiState> =
        combine(dose, picked, resolved, saved, key) { (loadedKey, dose), picked, resolved, saved, key ->
            ActualTimeUiState(
                // 열쇠는 정해졌는데 아직 DB에서 못 읽어 왔으면 계속 "찾는 중" (빈 상태가 번쩍이지 않게)
                isLoading = !resolved || loadedKey != key,
                dose = dose,
                time = picked ?: dose?.actualTimeOrNull(clock.zone) ?: dose?.time ?: LocalTime.MIDNIGHT,
                isSaved = saved
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActualTimeUiState())

    init {
        viewModelScope.launch {
            doseRepository.refresh(userId)
            key.value = resolveDoseKey(userId, hasArgs, argKey, doseRepository, selection, LocalDate.now(clock))
            resolved.value = true
        }
    }

    fun onTimePicked(time: LocalTime) {
        picked.value = time
    }

    /** 저장. 결과마다 프로토타입의 문구(또는 이 앱이 더한 검증 문구)를 토스트로 띄운다. */
    fun save() {
        val key = key.value
        if (key == null) {
            toastBus.show(R.string.s31_toast_no_dose)
            return
        }
        val time = uiState.value.time
        viewModelScope.launch {
            when (doseRepository.recordActualTime(userId, key, time)) {
                ActualTimeResult.SAVED, ActualTimeResult.UNCHANGED -> {
                    toastBus.show(R.string.s31_toast_saved)
                    saved.value = true
                }
                ActualTimeResult.NOT_FOUND -> toastBus.show(R.string.s31_toast_no_dose)
                ActualTimeResult.FUTURE_DOSE -> toastBus.show(R.string.s10_notice_future)
                ActualTimeResult.TIME_IN_FUTURE -> toastBus.show(R.string.s31_toast_time_future)
            }
        }
    }

    companion object {
        fun factory(userId: Long, hasArgs: Boolean, argKey: DoseKey?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                S31ActualTimeViewModel(
                    userId, hasArgs, argKey, container.doseRepository, container.selection, container.toastBus,
                    clock = container.clock
                )
            }
        }
    }
}
