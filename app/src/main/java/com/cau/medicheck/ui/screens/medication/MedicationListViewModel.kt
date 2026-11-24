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
import com.cau.medicheck.domain.CourseProgress
import com.cau.medicheck.domain.MedicationDetail
import com.cau.medicheck.domain.MedicationFilter
import com.cau.medicheck.domain.MedicationInsights
import com.cau.medicheck.domain.MedicationStatus
import com.cau.medicheck.domain.filterByName
import com.cau.medicheck.domain.medicationStatus
import com.cau.medicheck.domain.sortForList
import com.cau.medicheck.ui.components.ToastAction
import com.cau.medicheck.ui.components.ToastBus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime

/**
 * 목록의 한 줄: 약, 그 상태(복용 중·쉬는 중·종료), 최근 7일 복용률, 기간 진행.
 * @param recentRate 최근 7일 복용률(%). 계산할 회차가 없으면 null
 * @param course 종료일이 있는 약의 "N일 중 M일째". 없으면 null
 */
data class MedicationRowUi(
    val med: MedicationDetail,
    val status: MedicationStatus,
    val recentRate: Int? = null,
    val course: CourseProgress? = null
)

/**
 * 내 약 목록(S06)이 그릴 상태.
 * @param counts 상태 칩에 적을 개수 (검색어로 거른 뒤, 상태로 거르기 전)
 */
data class MedicationListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val filter: MedicationFilter = MedicationFilter.ALL,
    val counts: Map<MedicationFilter, Int> = emptyMap(),
    val rows: List<MedicationRowUi> = emptyList(),
    val isSample: Boolean = false
)

/**
 * 내 약 목록(S06)의 ViewModel.
 *
 * 약 목록(DB) + 최근 7일 기록(DB) + 검색어 + 상태 칩 + 오늘, 하나라도 바뀌면 목록을 다시 만든다:
 * 이름으로 거르고 → 종료된 약을 뒤로 보내고 → 줄마다 상태·복용률·기간을 붙이고 → 상태 칩으로 거른다.
 * 검색어·상태 칩은 이 ViewModel이 들고 있어서 상세 화면에 갔다 와도 남는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicationListViewModel(
    private val userId: Long,
    isSample: Boolean,
    private val medicationRepository: MedicationRepository,
    doseRepository: DoseRepository,
    private val toastBus: ToastBus,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(MedicationFilter.ALL)
    /** "지금". 화면이 다시 보일 때마다 새로 읽는다(자정이 지나면 '종료'와 복용률이 바뀔 수 있다). */
    private val now = MutableStateFlow(LocalDateTime.now(clock))

    /** 최근 7일의 기록. 날짜가 바뀌면 새 7일로 다시 구독한다 */
    private val recentDoses = now.flatMapLatest { now ->
        val today = now.toLocalDate()
        doseRepository.observeDoses(userId, today.minusDays(MedicationInsights.RECENT_DAYS - 1), today)
    }

    val uiState: StateFlow<MedicationListUiState> =
        combine(medicationRepository.observeMedications(userId), recentDoses, query, filter, now) { meds, doses, query, filter, now ->
            val today = now.toLocalDate()
            val dosesByMed = doses.groupBy { it.medicationId }
            val all = sortForList(filterByName(meds, query), today).map { med ->
                MedicationRowUi(
                    med = med,
                    status = medicationStatus(med, today),
                    recentRate = MedicationInsights.recentRate(dosesByMed[med.id].orEmpty(), now),
                    course = MedicationInsights.courseProgress(med.startDate, med.endDate, today)
                )
            }
            MedicationListUiState(
                isLoading = false,
                query = query,
                filter = filter,
                counts = MedicationFilter.entries.associateWith { f -> all.count { MedicationInsights.matches(it.status, f) } },
                rows = all.filter { MedicationInsights.matches(it.status, filter) },
                isSample = isSample
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MedicationListUiState(isSample = isSample)
        )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: MedicationFilter) {
        filter.value = value
    }

    /**
     * 약 카드의 스위치: 켜면 복용 중(오늘부터 일정에 들어감), 끄면 쉬는 중(일정·알림에서 빠짐).
     * 약 상세의 "쉬는 중"(예전 이름: 일시중지)과 같은 기능이다(MedicationRepository.setPaused).
     */
    fun setActive(medId: Long, name: String, active: Boolean) {
        viewModelScope.launch {
            val result = medicationRepository.setPaused(userId, medId, paused = !active)
            if (result is MedicationResult.Failure) {
                toastBus.show(R.string.error_med_not_found)
                return@launch
            }
            toastBus.show(
                if (active) R.string.s06_toast_activated else R.string.s06_toast_resting,
                ToastAction(R.string.s04_toast_undo) { undoActive(medId, active) },
                name
            )
        }
    }

    /** 스위치의 실행취소: 바로 전 상태로 되돌린다 */
    private fun undoActive(medId: Long, wasActivated: Boolean) {
        viewModelScope.launch { medicationRepository.setPaused(userId, medId, paused = wasActivated) }
    }

    fun refresh() {
        now.value = LocalDateTime.now(clock)
    }

    companion object {
        fun factory(userId: Long, isSample: Boolean): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                MedicationListViewModel(
                    userId, isSample, container.medicationRepository, container.doseRepository, container.toastBus, container.clock
                )
            }
        }
    }
}
