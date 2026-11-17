package com.cau.medicheck.ui.screens.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.R
import com.cau.medicheck.data.repository.DoseArchive
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.data.repository.MedicationRepository
import com.cau.medicheck.data.repository.MedicationResult
import com.cau.medicheck.ui.components.ToastBus
import com.cau.medicheck.ui.navigation.SelectionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

// =============================================================================================
// S42 처방 출처·진료과
// =============================================================================================

/**
 * 처방 출처 메모(S42)가 그릴 상태.
 * @param medName 약 이름. 약을 찾지 못했으면 null (화면은 "선택한 약"으로 보인다)
 * @param isDone 저장이 끝났다 → 이전 화면(약 상세)으로
 * @param goToList 약을 찾지 못해 저장하지 못했다 → 목록으로
 */
data class PrescriptionSourceUiState(
    val isLoading: Boolean = true,
    val medName: String? = null,
    val hospital: String = "",
    val department: String = "",
    val memo: String = "",
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
    val goToList: Boolean = false
)

/** 처방 출처 메모(S42)의 ViewModel. 약을 처음 한 번 읽어 칸을 채우고, "메모 저장"을 누르면 세 값을 저장한다. */
class PrescriptionSourceViewModel(
    private val userId: Long,
    private val medId: Long?,
    private val medicationRepository: MedicationRepository,
    private val toastBus: ToastBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrescriptionSourceUiState())
    val uiState: StateFlow<PrescriptionSourceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val med = medId?.let { medicationRepository.getMedication(userId, it) }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    medName = med?.name,
                    hospital = med?.hospital.orEmpty(),
                    department = med?.department.orEmpty(),
                    memo = med?.sourceMemo.orEmpty()
                )
            }
        }
    }

    fun onHospitalChange(value: String) {
        if (value.length <= HOSPITAL_MAX) _uiState.update { it.copy(hospital = value) }
    }

    fun onDepartmentChange(value: String) {
        if (value.length <= DEPARTMENT_MAX) _uiState.update { it.copy(department = value) }
    }

    fun onMemoChange(value: String) {
        if (value.length <= MEMO_MAX) _uiState.update { it.copy(memo = value) }
    }

    /** "메모 저장". 검사는 없다 — 셋 다 비워도 저장된다(= 지우기). */
    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = medId?.let {
                medicationRepository.updateSource(userId, it, state.hospital, state.department, state.memo)
            }
            if (result is MedicationResult.Success) {
                toastBus.show(R.string.s42_toast_saved)
                _uiState.update { it.copy(isSaving = false, isDone = true) }
            } else {
                toastBus.show(R.string.error_med_not_found)
                _uiState.update { it.copy(isSaving = false, goToList = true) }
            }
        }
    }

    companion object {
        const val HOSPITAL_MAX = 50
        const val DEPARTMENT_MAX = 30
        const val MEMO_MAX = 200

        fun factory(userId: Long, medId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                PrescriptionSourceViewModel(userId, medId, container.medicationRepository, container.toastBus)
            }
        }
    }
}

// =============================================================================================
// S32 과거 기록 보존
// =============================================================================================

/**
 * 과거 기록 보존(S32)이 그릴 상태.
 * @param isSample 샘플 계정인가. "시연 데이터" 배지는 샘플 계정에서만 보인다.
 */
data class ArchiveUiState(
    val isLoading: Boolean = true,
    val archive: DoseArchive = DoseArchive(0, emptyList()),
    val isSample: Boolean = false
)

/** 과거 기록 보존(S32)의 ViewModel. 오늘보다 앞선 날짜의 기록 수와 최근 10개 날짜를 읽는다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveViewModel(
    userId: Long,
    isSample: Boolean,
    doseRepository: DoseRepository,
    private val selection: SelectionStore,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    /** "오늘". 화면이 다시 보일 때 새로 읽는다 (자정이 지나면 어제 기록이 "과거"가 된다). */
    private val today = MutableStateFlow(LocalDate.now(clock))

    val uiState: StateFlow<ArchiveUiState> =
        today.flatMapLatest { doseRepository.observeArchive(userId, it) }
            .map { ArchiveUiState(isLoading = false, archive = it, isSample = isSample) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchiveUiState(isSample = isSample))

    fun refresh() {
        today.value = LocalDate.now(clock)
    }

    /** 날짜 줄을 눌렀을 때: 그 날짜를 선택 날짜로 한다(홈·일정·기록 화면도 그 날짜를 보게 된다). */
    fun selectDay(date: LocalDate) = selection.selectDay(date)

    companion object {
        fun factory(userId: Long, isSample: Boolean): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                ArchiveViewModel(userId, isSample, container.doseRepository, container.selection, container.clock)
            }
        }
    }
}
