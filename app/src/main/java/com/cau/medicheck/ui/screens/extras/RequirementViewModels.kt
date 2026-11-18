package com.cau.medicheck.ui.screens.extras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.data.repository.RequirementRepository
import com.cau.medicheck.domain.Moscow
import com.cau.medicheck.domain.Requirement
import com.cau.medicheck.domain.Requirements
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/*
 * 107개 기능 목록을 보여 주는 두 화면의 ViewModel. 둘 다 자산을 읽어 상태에 올려 두기만 한다.
 * 기능 목록은 사용자와 상관없어서 userId를 받지 않는다.
 */

/**
 * 기능별 요구사항 맵(S45)이 그릴 상태.
 * 검색어와 고른 우선순위는 여기 없다 — 입력칸 값은 화면이 바로 들고 있는다(S45ReqMapScreen 설명 참고).
 * @param counts 우선순위별 개수. 검색과 상관없이 전체 기준
 */
data class ReqMapUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val all: List<Requirement> = emptyList(),
    val counts: Map<Moscow, Int> = emptyMap()
)

class S45ReqMapViewModel(repository: RequirementRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ReqMapUiState())
    val uiState: StateFlow<ReqMapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = try {
                val all = repository.load()
                ReqMapUiState(isLoading = false, all = all, counts = Requirements.countByMoscow(all))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 자산을 읽지 못해도 앱이 꺼지지 않게 하고, 화면에 안내 한 줄을 보여 준다.
                ReqMapUiState(isLoading = false, loadFailed = true)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MediCheckApplication
                S45ReqMapViewModel(app.container.requirementRepository)
            }
        }
    }
}

/**
 * 안전·개발 범위(S46)가 그릴 상태.
 * @param wont 이번 릴리스에서 제외한(Won’t) 기능들, 파일 순서 그대로
 */
data class SafetyScopeUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val wont: List<Requirement> = emptyList()
)

class S46SafetyScopeViewModel(repository: RequirementRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SafetyScopeUiState())
    val uiState: StateFlow<SafetyScopeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = try {
                SafetyScopeUiState(isLoading = false, wont = repository.load().filter { it.moscow == Moscow.WONT })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SafetyScopeUiState(isLoading = false, loadFailed = true)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MediCheckApplication
                S46SafetyScopeViewModel(app.container.requirementRepository)
            }
        }
    }
}
