package com.cau.medicheck.ui.screens.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.R
import com.cau.medicheck.data.repository.MedicationRepository
import com.cau.medicheck.data.repository.MedicationResult
import com.cau.medicheck.domain.ApplyScope
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.FormTimeRow
import com.cau.medicheck.domain.MedicationDetail
import com.cau.medicheck.domain.MedicationDraft
import com.cau.medicheck.domain.MedicationError
import com.cau.medicheck.domain.MedicationValidator
import com.cau.medicheck.domain.RepeatDays
import com.cau.medicheck.domain.hasMixedDays
import com.cau.medicheck.domain.toScheduleDrafts
import com.cau.medicheck.ui.components.ToastBus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * 폼의 복용 시간 한 줄.
 * @param key 화면에서 줄을 구분하는 번호 (줄을 지우고 더해도 겹치지 않는다)
 * @param id 이미 저장돼 있던 규칙이면 그 id. 수정할 때 "같은 줄"을 찾는 데 쓴다. 새 줄이면 null
 * @param condition 이 시간의 복용 조건 (식전·식후 등, 기본: 상관없음)
 */
data class TimeRow(val key: Int, val id: Long?, val time: LocalTime, val condition: DoseCondition = DoseCondition.NONE)

/**
 * 같은 이름의 약이 있어서 확인을 기다리는 등록 (S40이 읽는다).
 * @param name 입력한 이름 (앞뒤 공백을 뺀 것)
 * @param sameNameMeds 이름이 같은 내 약들
 */
data class PendingNew(val name: String, val sameNameMeds: List<MedicationDetail>)

/**
 * 약 등록/수정(S08)·적용 범위(S33)·중복 확인(S40)이 함께 그리는 상태.
 *
 * @param isNew 등록이면 true, 수정이면 false
 * @param notFound 수정할 약을 찾지 못했다 (삭제됐거나 내 약이 아님)
 * @param days 요일 버튼에 켜져 있는 요일
 * @param isMixedDays 혼합 요일 모드 — 시간마다 요일이 다르게 저장된 약을 열었고 아직 요일 버튼을 안 눌렀다
 * @param scope S33에서 고른 적용 범위
 * @param hasPendingEdit 검사를 통과한 수정이 적용 범위 선택을 기다리는 중인가
 * @param pendingNew 같은 이름 확인을 기다리는 등록. 없으면 null
 */
data class MedicationEditUiState(
    val isNew: Boolean,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val name: String = "",
    val dosage: String = "",
    val memo: String = "",
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val times: List<TimeRow> = emptyList(),
    val days: Set<DayOfWeek> = RepeatDays.EVERY_DAY,
    val isMixedDays: Boolean = false,
    val isSaving: Boolean = false,
    val scope: ApplyScope = ApplyScope.FROM_TODAY,
    val hasPendingEdit: Boolean = false,
    val pendingNew: PendingNew? = null
)

/** 한 번만 일어나는 일(화면 이동). 화면 쪽(MedicationGraph)이 받아서 실제로 이동한다. */
sealed interface MedicationEditEvent {
    /** 같은 이름의 약이 있다 → 중복 확인(S40)으로 */
    data object GoToDuplicate : MedicationEditEvent
    /** 수정 검사를 통과했다 → 적용 범위(S33)로 */
    data class GoToScope(val medId: Long) : MedicationEditEvent
    /** 새 약을 저장했다 → 그 약의 상세(S07)로 */
    data class Registered(val medId: Long) : MedicationEditEvent
    /** 수정을 저장했다 → 그 약의 상세(S07)로 */
    data class Updated(val medId: Long) : MedicationEditEvent
    /** 저장하려는데 약이 사라졌다 → 목록(S06)으로 */
    data object GoToList : MedicationEditEvent
    /** 기다리는 등록·수정이 없다 → 폼(S08)으로 돌아간다 */
    data object BackToForm : MedicationEditEvent
}

/**
 * 약 등록/수정(S08)의 ViewModel. 적용 범위(S33)와 중복 확인(S40)도 이 ViewModel을 함께 쓴다.
 *
 * 등록과 수정이 같은 화면을 쓴다. medicationId가 null이면 등록, 있으면 수정이다.
 *
 *   등록: submit() → 검사 통과 → 같은 이름이 없으면 바로 저장 / 있으면 S40으로 → confirmDuplicate()로 저장
 *   수정: submit() → 검사 통과 → S33으로 → saveWithScope()로 저장
 *
 * S33·S40에서 "이전 화면"으로 돌아오면 입력해 둔 값이 그대로 있다(같은 ViewModel이므로).
 * 검사에 걸리면 저장하지 않고 그 이유를 토스트로 띄운다.
 */
class MedicationEditViewModel(
    private val userId: Long,
    private val medicationId: Long?,
    private val medicationRepository: MedicationRepository,
    private val toastBus: ToastBus,
    today: LocalDate = LocalDate.now()
) : ViewModel() {

    /** 새 복용 시간 줄에 매길 다음 번호 */
    private var nextKey = 0

    /** 혼합 요일 모드일 때만 값이 있다: 규칙 id → 그 규칙의 원래 요일. 요일 버튼을 누르면 null이 된다. */
    private var originalDays: Map<Long, Set<DayOfWeek>>? = null

    private val _uiState = MutableStateFlow(
        if (medicationId == null) {
            // 새 약의 처음 값(v3): 시간 "08:00" 한 줄, 요일 7개 모두, 시작일 오늘
            MedicationEditUiState(isNew = true, startDate = today, times = listOf(newRow(DEFAULT_TIME)))
        } else {
            MedicationEditUiState(isNew = false, isLoading = true, startDate = today)
        }
    )
    val uiState: StateFlow<MedicationEditUiState> = _uiState.asStateFlow()

    /** Channel: 넣은 것을 한 번 꺼내면 사라지는 통로. "이동하라"는 알림이 화면 회전 등으로 두 번 실행되지 않는다. */
    private val _events = Channel<MedicationEditEvent>(Channel.BUFFERED)
    val events: Flow<MedicationEditEvent> = _events.receiveAsFlow()

    init {
        if (medicationId != null) loadMedication(medicationId)
    }

    /** 수정할 약을 DB에서 읽어 입력칸에 채운다. */
    private fun loadMedication(id: Long) {
        viewModelScope.launch {
            val medication = medicationRepository.getMedication(userId, id)
            if (medication == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            val mixed = hasMixedDays(medication.schedules)
            originalDays = if (mixed) medication.schedules.associate { it.id to it.days } else null
            _uiState.update {
                it.copy(
                    isLoading = false,
                    name = medication.name,
                    dosage = medication.dosage,
                    memo = medication.memo,
                    startDate = medication.startDate,
                    endDate = medication.endDate,
                    times = medication.schedules.map { s -> TimeRow(key = nextKey++, id = s.id, time = s.time, condition = s.condition) },
                    // 혼합 모드면 버튼에는 모든 시간 요일의 합집합을 켜 둔다.
                    days = medication.schedules.flatMap { s -> s.days }.toSet(),
                    isMixedDays = mixed
                )
            }
        }
    }

    // ---------- 입력칸이 바뀔 때 (길이 한도를 넘는 입력은 받지 않는다) ----------

    fun onNameChange(value: String) {
        if (value.length <= MedicationValidator.NAME_MAX) _uiState.update { it.copy(name = value) }
    }

    fun onDosageChange(value: String) {
        if (value.length <= MedicationValidator.DOSAGE_MAX) _uiState.update { it.copy(dosage = value) }
    }

    fun onMemoChange(value: String) {
        if (value.length <= MedicationValidator.MEMO_MAX) _uiState.update { it.copy(memo = value) }
    }

    fun onStartDateChange(date: LocalDate) = _uiState.update { it.copy(startDate = date) }

    /** null을 넘기면 "종료일 없음"이 된다. */
    fun onEndDateChange(date: LocalDate?) = _uiState.update { it.copy(endDate = date) }

    // ---------- 복용 시간 줄 · 요일 ----------

    /** "＋ 시간 추가하기": 언제나 20:00 줄을 맨 아래에 더한다(v3). 이미 20:00이 있으면 제출할 때 중복으로 걸린다. */
    fun addTime() = _uiState.update { it.copy(times = it.times + newRow(ADDED_TIME)) }

    /** 줄 삭제. 마지막 한 줄은 지우지 않는다. */
    fun removeTime(key: Int) = _uiState.update { state ->
        if (state.times.size <= 1) state else state.copy(times = state.times.filterNot { it.key == key })
    }

    fun onTimeChange(key: Int, time: LocalTime) = _uiState.update { state ->
        state.copy(times = state.times.map { if (it.key == key) it.copy(time = time) else it })
    }

    /** 복용 조건 버튼: 그 줄의 조건을 바꾼다 (한 줄에 하나만 고른다) */
    fun onConditionChange(key: Int, condition: DoseCondition) = _uiState.update { state ->
        state.copy(times = state.times.map { if (it.key == key) it.copy(condition = condition) else it })
    }

    /** 요일 버튼: 켜져 있으면 끄고 꺼져 있으면 켠다. 한 번이라도 누르면 혼합 요일 모드가 끝난다. */
    fun onDayToggle(day: DayOfWeek) {
        originalDays = null
        _uiState.update { state ->
            state.copy(days = if (day in state.days) state.days - day else state.days + day, isMixedDays = false)
        }
    }

    private fun newRow(time: LocalTime) = TimeRow(key = nextKey++, id = null, time = time)

    // ---------- 제출 ----------

    /** 지금 입력값을 저장할 모양으로 */
    private fun currentDraft(): MedicationDraft {
        val state = _uiState.value
        return MedicationDraft(
            name = state.name,
            dosage = state.dosage,
            startDate = state.startDate,
            endDate = state.endDate,
            memo = state.memo,
            schedules = toScheduleDrafts(state.times.map { FormTimeRow(it.id, it.time, it.condition) }, state.days, originalDays)
        )
    }

    /**
     * "약 등록 완료" / "변경사항 저장".
     * 검사 ①~④에 걸리면 그 문구를 토스트로 띄우고 끝낸다. 통과하면:
     *   등록 → 같은 이름의 약이 있으면 S40으로, 없으면 바로 저장하고 상세로
     *   수정 → 아직 저장하지 않고 S33(적용 범위)으로
     */
    fun submit() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading || state.notFound) return

        val draft = currentDraft()
        // 요일이 하나도 없는지는 혼합 모드에서도 버튼에 보이는 요일로 본다.
        val error = MedicationValidator.validate(draft)
            ?: if (state.days.isEmpty()) MedicationError.NO_DAYS else null
        if (error != null) {
            toastBus.show(error.messageRes())
            return
        }

        if (medicationId != null) {
            _uiState.update { it.copy(hasPendingEdit = true, scope = ApplyScope.FROM_TODAY) }
            _events.trySend(MedicationEditEvent.GoToScope(medicationId))
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val same = medicationRepository.findSameName(userId, draft.name)
            if (same.isNotEmpty()) {
                _uiState.update { it.copy(isSaving = false, pendingNew = PendingNew(draft.name.trim(), same)) }
                _events.send(MedicationEditEvent.GoToDuplicate)
            } else {
                register(draft, R.string.s08_toast_saved)
            }
        }
    }

    /** 새 약을 저장하고 그 약의 상세로 가라고 알린다. */
    private suspend fun register(draft: MedicationDraft, toastRes: Int) {
        when (val result = medicationRepository.addMedication(userId, draft)) {
            is MedicationResult.Success -> {
                toastBus.show(toastRes)
                // isSaving은 켠 채로 둔다: 화면이 바뀌기 전에 버튼이 한 번 더 눌려도 다시 저장되지 않게.
                _uiState.update { it.copy(pendingNew = null) }
                _events.send(MedicationEditEvent.Registered(result.medicationId))
            }
            is MedicationResult.Failure -> {
                _uiState.update { it.copy(isSaving = false) }
                toastBus.show(result.error.messageRes())
            }
        }
    }

    // ---------- S40 중복 확인 ----------

    /** "그대로 등록". 기다리는 등록이 없으면 저장하지 않고 폼으로 돌아간다. */
    fun confirmDuplicate() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.pendingNew == null) {
            _events.trySend(MedicationEditEvent.BackToForm)
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch { register(currentDraft(), R.string.s40_toast_registered) }
    }

    /** "다시 확인": 기다리는 등록을 지운다. 입력값은 그대로 남는다. */
    fun cancelDuplicate() = _uiState.update { it.copy(pendingNew = null) }

    // ---------- S33 적용 범위 ----------

    fun onScopeChange(scope: ApplyScope) = _uiState.update { it.copy(scope = scope) }

    /** "적용 범위 확인 후 저장". 기다리는 수정이 없으면 저장하지 않고 상세로 간다. */
    fun saveWithScope() {
        val state = _uiState.value
        val id = medicationId
        if (state.isSaving) return
        if (id == null || !state.hasPendingEdit) {
            _events.trySend(if (id != null) MedicationEditEvent.Updated(id) else MedicationEditEvent.GoToList)
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = medicationRepository.updateMedication(userId, id, currentDraft(), state.scope)) {
                is MedicationResult.Success -> {
                    toastBus.show(R.string.s33_toast_saved)
                    // isSaving은 켠 채로 둔다 (화면이 바뀌기 전의 두 번 눌림 방지)
                    _uiState.update { it.copy(hasPendingEdit = false) }
                    _events.send(MedicationEditEvent.Updated(id))
                }
                is MedicationResult.Failure -> {
                    _uiState.update { it.copy(isSaving = false) }
                    toastBus.show(result.error.messageRes())
                    // 저장하는 사이 약이 사라졌으면 더 고칠 것이 없으므로 목록으로 보낸다.
                    if (result.error == MedicationError.NOT_FOUND) _events.send(MedicationEditEvent.GoToList)
                }
            }
        }
    }

    companion object {
        /** 새 약의 첫 시간 줄 (v3 blankDraft) */
        private val DEFAULT_TIME: LocalTime = LocalTime.of(8, 0)
        /** "＋ 시간 추가하기"로 더하는 줄의 시각 (v3 addTime) */
        private val ADDED_TIME: LocalTime = LocalTime.of(20, 0)

        fun factory(userId: Long, medicationId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                MedicationEditViewModel(
                    userId, medicationId, container.medicationRepository, container.toastBus,
                    today = LocalDate.now(container.clock)
                )
            }
        }
    }
}
