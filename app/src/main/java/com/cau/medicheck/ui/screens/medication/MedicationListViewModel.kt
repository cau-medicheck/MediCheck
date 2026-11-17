package com.cau.medicheck.ui.screens.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.data.repository.MedicationRepository
import com.cau.medicheck.domain.MedicationDetail
import com.cau.medicheck.domain.MedicationStatus
import com.cau.medicheck.domain.filterByName
import com.cau.medicheck.domain.medicationStatus
import com.cau.medicheck.domain.sortForList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

/** 목록의 한 줄: 약과 그 상태(배지) */
data class MedicationRowUi(val med: MedicationDetail, val status: MedicationStatus)

/**
 * 내 약 목록(S06)이 그릴 상태.
 * @param isSample 샘플 계정인가. "이 화면의 데이터는 시연용입니다." 문장은 샘플 계정에서만 보인다.
 */
data class MedicationListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val rows: List<MedicationRowUi> = emptyList(),
    val isSample: Boolean = false
)

/**
 * 내 약 목록(S06)의 ViewModel.
 *
 * 약 목록(DB) + 검색어 + 오늘 날짜, 셋 중 하나라도 바뀌면 목록을 다시 만든다:
 * 이름으로 거르고 → 종료된 약을 뒤로 보내고 → 줄마다 상태(배지)를 붙인다.
 * 검색어는 이 ViewModel이 들고 있어서 상세 화면에 갔다 와도 남는다.
 */
class MedicationListViewModel(
    userId: Long,
    isSample: Boolean,
    medicationRepository: MedicationRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val query = MutableStateFlow("")
    /** "오늘". 화면이 다시 보일 때마다 새로 읽는다(자정이 지나면 '종료' 배지가 바뀔 수 있다). */
    private val today = MutableStateFlow(LocalDate.now(clock))

    val uiState: StateFlow<MedicationListUiState> =
        combine(medicationRepository.observeMedications(userId), query, today) { meds, query, today ->
            MedicationListUiState(
                isLoading = false,
                query = query,
                rows = sortForList(filterByName(meds, query), today)
                    .map { MedicationRowUi(it, medicationStatus(it, today)) },
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

    fun refresh() {
        today.value = LocalDate.now(clock)
    }

    companion object {
        fun factory(userId: Long, isSample: Boolean): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                MedicationListViewModel(userId, isSample, container.medicationRepository)
            }
        }
    }
}
