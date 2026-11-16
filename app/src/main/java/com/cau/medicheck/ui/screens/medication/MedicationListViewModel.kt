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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 약 목록 화면이 그릴 상태. */
data class MedicationListUiState(
    /** 아직 DB에서 첫 목록을 받지 못했는지 */
    val isLoading: Boolean = true,
    val medications: List<MedicationDetail> = emptyList()
)

/**
 * 약 목록 화면의 ViewModel.
 *
 * Repository가 흘려 보내는 약 목록(Flow)을 화면 상태로 바꿔 들고 있는다.
 * 약이 추가·수정·삭제되면 DB → Repository → 여기 → 화면 순으로 저절로 갱신된다.
 *
 * @param userId 로그인한 사용자의 id. 이 사용자의 약만 보여 준다.
 */
class MedicationListViewModel(
    userId: Long,
    medicationRepository: MedicationRepository
) : ViewModel() {

    val uiState: StateFlow<MedicationListUiState> =
        medicationRepository.observeMedications(userId)
            .map { MedicationListUiState(isLoading = false, medications = it) }
            // stateIn: 흐름을 "항상 최신 값을 하나 들고 있는 상태"로 바꾼다.
            // WhileSubscribed(5_000): 화면이 안 보이게 된 뒤 5초가 지나면 DB 구독을 멈춘다.
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MedicationListUiState()
            )

    companion object {
        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MediCheckApplication
                MedicationListViewModel(userId, app.container.medicationRepository)
            }
        }
    }
}
