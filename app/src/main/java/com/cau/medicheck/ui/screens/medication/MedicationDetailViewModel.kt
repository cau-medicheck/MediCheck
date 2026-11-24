package com.cau.medicheck.ui.screens.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.R
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.data.repository.MedicationRepository
import com.cau.medicheck.data.repository.MedicationResult
import com.cau.medicheck.domain.DoseCounts
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.MedicationDetail
import com.cau.medicheck.domain.MedicationInsights
import com.cau.medicheck.domain.MedicationStatus
import com.cau.medicheck.domain.RecentDay
import com.cau.medicheck.domain.medicationStatus
import com.cau.medicheck.domain.sortForList
import com.cau.medicheck.ui.components.ToastBus
import com.cau.medicheck.ui.screens.dose.tickerFlow
import com.cau.medicheck.ui.navigation.SelectionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 약 상세(S07)와 삭제 확인(S09)이 그릴 상태.
 *
 * @param med 보여 줄 약. 찾는 중이거나 없으면 null
 * @param isBusy 일시중지·삭제를 처리하는 중 (스위치·버튼을 잠시 막는다)
 * @param isDeleted 삭제가 끝났다 → 화면이 스스로 목록(S06)으로 간다
 */
data class MedicationDetailUiState(
    val isLoading: Boolean = true,
    val med: MedicationDetail? = null,
    val status: MedicationStatus = MedicationStatus.ACTIVE,
    val isBusy: Boolean = false,
    val isDeleted: Boolean = false,
    /** 최근 7일 요일 동그라미 (6일 전 → 오늘) */
    val recentDays: List<RecentDay> = emptyList(),
    /** 최근 7일 횟수: "7번 중 2번 먹음" */
    val recentCounts: DoseCounts = DoseCounts(),
    /** 다음 복용 회차. 없으면 null (비활성화·종료 등) */
    val nextDose: DoseItem? = null
)

/**
 * 약 상세(S07)와 삭제 확인(S09)의 ViewModel. (두 화면이 같은 클래스를 쓰되, 화면마다 따로 하나씩 만들어진다)
 *
 * 어느 약인가: 주소에 약 번호가 있으면 그 약. 없으면 마지막으로 본 약 → 그것도 없으면 목록의 첫 약.
 * 약은 Flow로 지켜본다. 그래서 일시중지를 바꾸거나 처방 출처를 저장하고 돌아오면 바로 반영된다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicationDetailViewModel(
    private val userId: Long,
    private val argMedId: Long?,
    private val medicationRepository: MedicationRepository,
    private val doseRepository: DoseRepository,
    private val selection: SelectionStore,
    private val toastBus: ToastBus,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val medId = MutableStateFlow<Long?>(null)
    private val resolved = MutableStateFlow(false)
    private val busy = MutableStateFlow(false)
    private val deleted = MutableStateFlow(false)
    private val today = MutableStateFlow(LocalDate.now(clock))
    /** "지금". 미복용 여부와 다음 복용이 시각에 따라 바뀌므로 30초마다 새 값이 들어온다. */
    private val now = MutableStateFlow(LocalDateTime.now(clock))

    /** 삭제를 시작하기 직전에 화면에 있던 약. 삭제 중·삭제 뒤에도 이 값을 그대로 보여 준다. */
    private var frozen: MedicationDetail? = null
    private val deleting = MutableStateFlow(false)

    /** (어느 번호로 읽었는지, 읽어 온 약) */
    private val med = medId.flatMapLatest { id ->
        if (id == null) flowOf(null to null)
        else medicationRepository.observeMedications(userId).map { meds -> id to meds.find { it.id == id } }
    }

    /** 이 약의 6일 전 ~ 7일 뒤 기록 (최근 7일 동그라미와, 미리 건너뛴 미래 회차를 빼고 다음 복용을 찾는 데 쓴다) */
    private val records = combine(medId, now.map { it.toLocalDate() }.distinctUntilChanged()) { id, day -> id to day }
        .flatMapLatest { (id, day) ->
            if (id == null) flowOf(emptyList())
            else doseRepository.observeDoses(userId, day.minusDays(MedicationInsights.RECENT_DAYS - 1), day.plusDays(MedicationInsights.RECENT_DAYS))
                .map { doses -> doses.filter { it.medicationId == id } }
        }

    val uiState: StateFlow<MedicationDetailUiState> =
        combine(
            combine(med, medId) { (loadedId, med), id -> Triple(loadedId, med, id) },
            combine(resolved, busy, deleted, deleting) { r, b, d, del -> listOf(r, b, d, del) },
            today
        ) { (loadedId, med, id), (resolved, busy, deleted, deleting), today ->
            // 삭제가 저장되는 순간 목록에서 약이 빠져 "못 찾음"이 흘러온다. 그대로 그리면 화면을 옮기기 전에
            // 이름이 잠깐 "선택한 약"으로 바뀌어 깜빡이므로, 삭제 중에는 얼려 둔 약을 계속 보여 준다.
            val shown = if (deleting) frozen else med
            MedicationDetailUiState(
                isLoading = !resolved || loadedId != id,
                med = shown,
                status = shown?.let { medicationStatus(it, today) } ?: MedicationStatus.ACTIVE,
                isBusy = busy,
                isDeleted = deleted
            )
        }.combine(combine(records, now) { r, n -> r to n }) { state, (records, now) ->
            val med = state.med ?: return@combine state
            state.copy(
                recentDays = MedicationInsights.recentDays(records, now),
                recentCounts = MedicationInsights.recentCounts(records, now),
                nextDose = MedicationInsights.nextDose(med, records, now)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicationDetailUiState())

    init {
        viewModelScope.launch { tickerFlow(clock).collect { now.value = it } }
        // 오늘까지의 기록을 먼저 채운다 (앱을 며칠 안 열었으면 그 사이 날짜의 기록이 아직 없다)
        viewModelScope.launch { doseRepository.refresh(userId) }
        viewModelScope.launch {
            val meds = medicationRepository.observeMedications(userId).first()
            val id = argMedId
                ?: selection.lastMedId.value?.takeIf { last -> meds.any { it.id == last } }
                ?: sortForList(meds, LocalDate.now(clock)).firstOrNull()?.id
            medId.value = id
            if (id != null && meds.any { it.id == id }) selection.setLastMedId(id)
            resolved.value = true
        }
    }

    /** 화면이 다시 보일 때 "오늘"을 새로 읽는다 (자정이 지나면 '종료' 배지가 바뀔 수 있다). */
    fun refresh() {
        today.value = LocalDate.now(clock)
        now.value = LocalDateTime.now(clock)
    }

    /** 일시중지 스위치. 켜면 중지, 끄면 재개. 처리하는 동안은 스위치를 막는다. */
    fun setPaused(paused: Boolean) {
        val id = medId.value ?: return
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                val result = medicationRepository.setPaused(userId, id, paused)
                if (result is MedicationResult.Failure) toastBus.show(R.string.error_med_not_found)
            } finally {
                busy.value = false   // 오류가 나도 스위치가 잠긴 채로 남지 않게
            }
        }
    }

    /**
     * "삭제하기". 지운 뒤 목록으로 가도록 isDeleted를 켠다.
     * 대상 약이 없으면(화면에 "선택한 약"으로 보일 때) 아무것도 지우지 않고 토스트 없이 목록으로 간다.
     */
    fun delete() {
        if (busy.value) return
        val target = uiState.value.med
        if (target == null) {
            deleted.value = true
            return
        }
        frozen = target
        deleting.value = true
        busy.value = true
        viewModelScope.launch {
            if (medicationRepository.deleteMedication(userId, target.id) is MedicationResult.Success) {
                toastBus.show(R.string.s09_toast_deleted)
            }
            // busy는 켠 채로 둔다: 목록으로 옮겨 가기 전에 "삭제하기"가 한 번 더 눌려도 다시 실행되지 않게.
            deleted.value = true
        }
    }

    companion object {
        fun factory(userId: Long, medId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                MedicationDetailViewModel(
                    userId, medId, container.medicationRepository, container.doseRepository, container.selection, container.toastBus,
                    clock = container.clock
                )
            }
        }
    }
}
