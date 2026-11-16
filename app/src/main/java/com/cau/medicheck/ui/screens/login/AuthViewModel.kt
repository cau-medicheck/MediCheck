package com.cau.medicheck.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.data.local.User
import com.cau.medicheck.data.repository.AuthRepository
import com.cau.medicheck.data.repository.AuthResult
import com.cau.medicheck.domain.AuthError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 화면이 그려야 할 "지금 상태".
 * 화면은 이 값만 보고 그리고, 값이 바뀌면 자동으로 다시 그린다.
 */
data class AuthUiState(
    /** 앱을 막 켜서, 이전 로그인 기록을 확인하는 중인지 */
    val isCheckingSession: Boolean = true,
    /** 로그인·가입 요청을 처리하는 중인지 (버튼을 잠그고 돌아가는 표시를 보여 준다) */
    val isLoading: Boolean = false,
    /** 로그인한 사용자. 로그인하지 않았으면 null */
    val currentUser: User? = null,
    /** 마지막 요청이 실패한 이유. 없으면 null */
    val error: AuthError? = null
)

/**
 * 로그인·회원가입 화면과 AuthRepository 사이를 잇는다.
 *
 * ViewModel: 화면이 회전하거나 다시 그려져도 사라지지 않고 상태를 들고 있는 클래스.
 * 흐름: 화면에서 버튼 클릭 → 여기 함수 호출 → AuthRepository 호출 → 결과를 uiState에 반영 → 화면이 다시 그려짐
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // _uiState는 이 클래스 안에서만 바꿀 수 있고, 화면에는 읽기 전용(uiState)으로 내보낸다.
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // 앱이 켜지면 이전에 로그인해 둔 사용자가 있는지 확인한다.
        // viewModelScope.launch: suspend 함수(DB 작업)를 화면을 멈추지 않고 실행하는 방법.
        viewModelScope.launch {
            val user = authRepository.currentUser()
            _uiState.update { it.copy(isCheckingSession = false, currentUser = user) }
        }
    }

    fun login(username: String, password: String) {
        runAuthRequest { authRepository.login(username, password) }
    }

    fun signUp(username: String, password: String, passwordConfirm: String, displayName: String) {
        runAuthRequest { authRepository.signUp(username, password, passwordConfirm, displayName) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(currentUser = null, error = null) }
        }
    }

    /** 화면을 옮기거나 입력을 고칠 때 이전 오류 문구를 지운다. */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** 로그인과 가입이 공통으로 쓰는 순서: 로딩 표시 → 요청 → 결과 반영 */
    private fun runAuthRequest(request: suspend () -> AuthResult) {
        if (_uiState.value.isLoading) return   // 버튼을 연달아 눌러도 한 번만 처리
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            when (val result = request()) {
                is AuthResult.Success ->
                    _uiState.update { it.copy(isLoading = false, currentUser = result.user) }
                is AuthResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    companion object {
        /**
         * AuthViewModel을 만드는 방법을 안드로이드에게 알려 준다.
         * ViewModel은 안드로이드가 대신 만들어 주는데, 우리 ViewModel은 AuthRepository가
         * 필요하므로 "AppContainer에서 꺼내서 넣어 달라"고 적어 둔 것이다.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MediCheckApplication
                AuthViewModel(app.container.authRepository)
            }
        }
    }
}
