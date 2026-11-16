package com.cau.medicheck.ui.screens.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.data.repository.MedicationRepository
import com.cau.medicheck.data.repository.MedicationResult
import com.cau.medicheck.domain.MedicationDraft
import com.cau.medicheck.domain.MedicationError
import com.cau.medicheck.domain.RepeatDays
import com.cau.medicheck.domain.ScheduleDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * 화면에 보이는 복용 시간 한 줄.
 * @param key 화면에서 줄을 구분하는 번호. 아직 저장 전인 줄은 DB id가 없어서 따로 매긴다.
 * @param id 이미 저장된 규칙이면 DB의 id, 새로 추가한 줄이면 null
 */
data class ScheduleRow(
    val key: Int,
    val id: Long?,
    val time: LocalTime,
    val days: Set<DayOfWeek>
)

/** 약 등록·수정 화면이 그릴 상태 = 입력칸에 적힌 값 전부 + 진행 상황. */
data class MedicationEditUiState(
    /** true면 새 약 등록, false면 기존 약 수정 */
    val isNew: Boolean,
    /** 수정할 약을 DB에서 읽어 오는 중인지 */
    val isLoading: Boolean = false,
    val name: String = "",
    val dosage: String = "",
    val memo: String = "",
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val schedules: List<ScheduleRow> = emptyList(),
    val error: MedicationError? = null,
    /** 저장·삭제를 처리하는 중인지 */
    val isSaving: Boolean = false,
    /** 저장·삭제가 끝나서 이전 화면으로 돌아가야 하는지 */
    val isFinished: Boolean = false
)

/**
 * 약 등록·수정 화면의 ViewModel.
 *
 * 등록과 수정이 같은 화면을 쓴다. medicationId가 null이면 등록, 있으면 수정이다.
 * 입력칸을 고칠 때마다 여기 함수가 불려 uiState가 바뀌고, 화면이 그 값으로 다시 그려진다.
 */
class MedicationEditViewModel(
    private val userId: Long,
    private val medicationId: Long?,
    private val medicationRepository: MedicationRepository,
    today: LocalDate = LocalDate.now()
) : ViewModel() {

    /** 새 복용 시간 줄에 매길 다음 번호 */
    private var nextKey = 0

    private val _uiState = MutableStateFlow(
        if (medicationId == null) {
            // 새 약: 시작일은 오늘, 복용 시간은 "매일 08:00" 한 줄로 시작한다.
            MedicationEditUiState(
                isNew = true,
                startDate = today,
                schedules = listOf(newRow(DEFAULT_TIMES.first()))
            )
        } else {
            MedicationEditUiState(isNew = false, isLoading = true, startDate = today)
        }
    )
    val uiState: StateFlow<MedicationEditUiState> = _uiState.asStateFlow()

    init {
        if (medicationId != null) loadMedication(medicationId)
    }

    /** 수정할 약을 DB에서 읽어 입력칸에 채운다. */
    private fun loadMedication(id: Long) {
        viewModelScope.launch {
            val medication = medicationRepository.getMedication(userId, id)
            if (medication == null) {
                _uiState.update { it.copy(isLoading = false, error = MedicationError.NOT_FOUND) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    name = medication.name,
                    dosage = medication.dosage,
                    memo = medication.memo,
                    startDate = medication.startDate,
                    endDate = medication.endDate,
                    schedules = medication.schedules.map { s ->
                        ScheduleRow(key = nextKey++, id = s.id, time = s.time, days = s.days)
                    }
                )
            }
        }
    }

    // ---------- 입력칸이 바뀔 때 ----------

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }

    fun onDosageChange(value: String) = _uiState.update { it.copy(dosage = value, error = null) }

    fun onMemoChange(value: String) = _uiState.update { it.copy(memo = value, error = null) }

    fun onStartDateChange(date: LocalDate) =
        _uiState.update { it.copy(startDate = date, error = null) }

    /** null을 넘기면 "종료일 없음"이 된다. */
    fun onEndDateChange(date: LocalDate?) =
        _uiState.update { it.copy(endDate = date, error = null) }

    // ---------- 복용 시간 줄 ----------

    fun addSchedule() = _uiState.update { state ->
        val usedTimes = state.schedules.map { it.time }.toSet()
        // 아직 안 쓴 기본 시간(08:00 → 13:00 → 20:00)을 고르고, 다 썼으면 마지막 시간 + 1시간.
        val time = DEFAULT_TIMES.firstOrNull { it !in usedTimes }
            ?: state.schedules.last().time.plusHours(1)
        state.copy(schedules = state.schedules + newRow(time), error = null)
    }

    fun removeSchedule(key: Int) = _uiState.update { state ->
        state.copy(schedules = state.schedules.filterNot { it.key == key }, error = null)
    }

    fun onScheduleTimeChange(key: Int, time: LocalTime) = updateRow(key) { it.copy(time = time) }

    /** 요일 버튼을 누를 때: 켜져 있으면 끄고, 꺼져 있으면 켠다. */
    fun onScheduleDayToggle(key: Int, day: DayOfWeek) = updateRow(key) { row ->
        row.copy(days = if (day in row.days) row.days - day else row.days + day)
    }

    private fun updateRow(key: Int, change: (ScheduleRow) -> ScheduleRow) =
        _uiState.update { state ->
            state.copy(
                schedules = state.schedules.map { if (it.key == key) change(it) else it },
                error = null
            )
        }

    private fun newRow(time: LocalTime) =
        ScheduleRow(key = nextKey++, id = null, time = time, days = RepeatDays.EVERY_DAY)

    // ---------- 저장 · 삭제 ----------

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return

        val draft = MedicationDraft(
            name = state.name,
            dosage = state.dosage,
            startDate = state.startDate,
            endDate = state.endDate,
            memo = state.memo,
            schedules = state.schedules.map { ScheduleDraft(it.id, it.time, it.days) }
        )

        runRequest {
            if (medicationId == null) {
                medicationRepository.addMedication(userId, draft)
            } else {
                medicationRepository.updateMedication(userId, medicationId, draft)
            }
        }
    }

    fun delete() {
        val id = medicationId ?: return
        if (_uiState.value.isSaving) return
        runRequest { medicationRepository.deleteMedication(userId, id) }
    }

    /** 저장과 삭제가 공통으로 쓰는 순서: 처리 중 표시 → 요청 → 성공이면 화면 닫기, 실패면 오류 표시 */
    private fun runRequest(request: suspend () -> MedicationResult) {
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = request()) {
                is MedicationResult.Success ->
                    _uiState.update { it.copy(isSaving = false, isFinished = true) }
                is MedicationResult.Failure ->
                    _uiState.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }

    companion object {
        /** 복용 시간을 추가할 때 차례로 제안하는 기본 시각: 아침, 점심, 저녁 */
        private val DEFAULT_TIMES = listOf(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(20, 0))

        fun factory(userId: Long, medicationId: Long?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MediCheckApplication
                    MedicationEditViewModel(userId, medicationId, app.container.medicationRepository)
                }
            }
    }
}
