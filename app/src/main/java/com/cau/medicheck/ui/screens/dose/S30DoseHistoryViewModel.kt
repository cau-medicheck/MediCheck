package com.cau.medicheck.ui.screens.dose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.data.repository.DoseHistoryItem
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.navigation.SelectionStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 복용 변경 이력(S30)이 그릴 상태. items는 최신 변경이 맨 위다. */
data class DoseHistoryUiState(
    val isLoading: Boolean = true,
    val items: List<DoseHistoryItem> = emptyList()
)

/** 복용 변경 이력(S30)의 ViewModel. 읽기만 한다. */
class S30DoseHistoryViewModel(
    private val userId: Long,
    private val doseRepository: DoseRepository,
    private val selection: SelectionStore
) : ViewModel() {

    val uiState: StateFlow<DoseHistoryUiState> =
        doseRepository.observeHistory(userId)
            .map { DoseHistoryUiState(isLoading = false, items = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DoseHistoryUiState())

    /** "복용 회차 열기"가 열 회차: 선택 날짜의 첫 회차. 그날 회차가 없으면 null (화면은 일정으로 간다) */
    suspend fun firstDoseOfSelectedDay(): DoseKey? =
        doseRepository.firstDoseOf(userId, selection.selectedDay.value)

    companion object {
        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                S30DoseHistoryViewModel(userId, container.doseRepository, container.selection)
            }
        }
    }
}
