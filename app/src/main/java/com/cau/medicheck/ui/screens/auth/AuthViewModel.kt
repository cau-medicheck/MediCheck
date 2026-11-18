package com.cau.medicheck.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.R
import com.cau.medicheck.data.local.AppPrefs
import com.cau.medicheck.data.local.User
import com.cau.medicheck.data.repository.AuthRepository
import com.cau.medicheck.data.repository.AuthResult
import com.cau.medicheck.domain.DeleteConfirm
import com.cau.medicheck.ui.components.ToastBus
import com.cau.medicheck.ui.navigation.NavRules
import com.cau.medicheck.ui.navigation.Routes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 어느 버튼을 눌러 처리 중인지. 그 버튼에만 돌아가는 표시를 보여 주려고 구분한다. */
enum class AuthAction { LOGIN, SAMPLE, SIGN_UP }

/**
 * 로그인 전·후 전환과 로그인 전 화면(S00~S03)이 그려야 할 "지금 상태".
 * 오류·안내 문구는 여기 두지 않는다. ViewModel이 토스트(ToastBus)로 바로 띄운다(00 §1.5).
 */
data class AuthUiState(
    /** 앱을 막 켜서, 이전 로그인 기록을 확인하는 중인지 */
    val isCheckingSession: Boolean = true,
    /**
     * 로그인 전 틀(AuthNavHost)의 시작 화면. S00(온보딩) 또는 S01(로그인).
     * 앱을 켤 때·로그아웃·계정 삭제 때만 바뀐다. 온보딩을 마치는 순간에는 바꾸지 않는다
     * (그때 바꾸면 NavHost의 시작 화면이 흔들려 화면 기록이 처음부터 다시 만들어질 수 있다).
     */
    val authStartRoute: String = Routes.S00,
    /** 로그인·가입·샘플 요청을 처리하는 중인지 (버튼과 입력칸을 잠근다) */
    val isLoading: Boolean = false,
    /** 처리 중인 버튼 */
    val pressed: AuthAction? = null,
    /** 로그아웃 처리 중인지 (S19의 로그아웃 버튼을 두 번 누르지 못하게 한다) */
    val isLoggingOut: Boolean = false,
    /** 계정·데이터 삭제 처리 중인지 (S44의 버튼에 돌아가는 표시) */
    val isDeletingAccount: Boolean = false,
    /** 로그인한 사용자. 로그인하지 않았으면 null */
    val currentUser: User? = null
)

/*
 * 상태를 바꾸는 규칙 중 "계산만 하는 부분". ViewModel 밖 순수 함수라 단위 테스트한다.
 */

/** 로그아웃한 뒤의 상태: 사용자 없음, 로그인 전 틀은 S01부터 (온보딩은 이미 봤으므로) */
fun AuthUiState.afterLogout(): AuthUiState =
    copy(currentUser = null, isLoading = false, pressed = null, isLoggingOut = false, authStartRoute = Routes.S01)

/** 계정·데이터를 삭제한 뒤의 상태: 사용자 없음, 처음 쓰는 사람처럼 S00부터 (S44가 부른다, 6단계) */
fun AuthUiState.afterAccountDeleted(): AuthUiState =
    copy(
        currentUser = null, isLoading = false, pressed = null, isLoggingOut = false, isDeletingAccount = false,
        authStartRoute = Routes.S00
    )

/** 앱을 켜서 로그인 기록을 확인한 뒤의 상태 */
fun AuthUiState.afterSessionChecked(user: User?, onboardingSeen: Boolean): AuthUiState =
    copy(
        isCheckingSession = false,
        currentUser = user,
        // 로그인 전 틀의 시작 화면만 고른다. 로그인했으면 이 값은 쓰이지 않지만, 로그아웃 전까지 그대로 둔다.
        authStartRoute = NavRules.startRoute(isLoggedIn = false, onboardingSeen = onboardingSeen)
    )

/**
 * 로그인 전 화면들과 AuthRepository 사이를 잇는다. 앱 맨 바깥(MediCheckApp)이 하나를 만들어
 * "로그인했는지"를 보고 로그인 전 틀과 로그인 뒤 틀 중 하나를 고른다.
 *
 * ViewModel: 화면이 회전하거나 다시 그려져도 사라지지 않고 상태를 들고 있는 클래스.
 * 흐름: 화면 버튼 → 여기 함수 → AuthRepository → 결과를 uiState에 반영 + 토스트 → 화면이 다시 그려짐
 *
 * @param appPrefs 폰 단위 설정(온보딩을 봤는지)
 * @param toastBus 토스트를 띄우는 통로
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val appPrefs: AppPrefs,
    private val toastBus: ToastBus,
    /** 로그인해 둔 사용자의 설정을 읽는 함수 (SettingsRepository.loadForCurrentUser) */
    private val loadSettings: suspend () -> Unit = {}
) : ViewModel() {

    // _uiState는 이 클래스 안에서만 바꿀 수 있고, 화면에는 읽기 전용(uiState)으로 내보낸다.
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** 진행 중인 프로필 저장 작업. 로그아웃할 때 취소하려고 들고 있는다. (Job = 실행 중인 코루틴 작업 하나) */
    private var profileJob: Job? = null

    init {
        // 앱이 켜지면 이전에 로그인해 둔 사용자가 있는지, 온보딩을 본 적 있는지 확인한다.
        // viewModelScope.launch: suspend 함수(DB 작업)를 화면을 멈추지 않고 실행하는 방법.
        viewModelScope.launch {
            // 로그인 뒤 화면을 그리기 전에 그 사람의 설정(시간대 정책 등)을 먼저 읽는다.
            // 그래야 홈이 "오늘"을 처음부터 맞는 시간대로 계산한다.
            loadSettings()
            val user = authRepository.currentUser()
            val seen = appPrefs.onboardingSeen()
            _uiState.update { it.afterSessionChecked(user, seen) }
        }
    }

    /** S01 "로그인" */
    fun login(email: String, password: String) {
        runAuthRequest(AuthAction.LOGIN, successToast = R.string.toast_login_done) {
            authRepository.login(email, password)
        }
    }

    /** S01 "샘플 계정으로 체험하기". 성공 토스트는 없다(v3 demoLogin도 없음). */
    fun loginAsSample() {
        runAuthRequest(AuthAction.SAMPLE, successToast = null) { authRepository.loginAsSample() }
    }

    /** S02 "계정 만들기" */
    fun signUp(displayName: String, email: String, password: String, passwordConfirm: String) {
        runAuthRequest(AuthAction.SIGN_UP, successToast = R.string.toast_signup_done) {
            authRepository.signUp(displayName, email, password, passwordConfirm)
        }
    }

    /** S03 "복구 안내 요청". DB를 보지 않고 형식만 검사한 뒤 안내 토스트를 띄운다. */
    fun requestPasswordHelp(email: String) {
        toastBus.show(passwordHelpMessageRes(email))
    }

    /**
     * S00 "시작하기"·"이미 계정이 있어요". 온보딩을 봤다고 저장만 한다.
     * authStartRoute는 바꾸지 않는다(AuthUiState 설명 참고). 화면 이동은 AuthGraph가 한다.
     */
    fun finishOnboarding() {
        appPrefs.setOnboardingSeen(true)
    }

    /**
     * 프로필(이름·이메일)을 고친다(S21). 성공하면 화면들이 보는 currentUser도 새 값으로 바뀌어
     * 설정 허브의 카드와 홈의 인사말이 바로 바뀐다.
     * @param onDone 저장에 성공했을 때 부를 함수 (화면을 설정 허브로 돌려보낸다)
     */
    fun updateProfile(displayName: String, email: String, onDone: () -> Unit) {
        val userId = _uiState.value.currentUser?.id ?: return
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        profileJob = viewModelScope.launch {
            // 예상 못 한 오류(DB 등)가 나도 앱이 꺼지지 않게 하고, 버튼 잠금을 풀어 준다.
            val result = try {
                authRepository.updateProfile(userId, displayName, email)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                toastBus.show(R.string.s21_toast_failed)
                return@launch
            }
            when (result) {
                is AuthResult.Success -> {
                    // 저장하는 사이에 로그아웃했을 수 있다. 그때 currentUser를 다시 채우면 로그아웃한 계정의
                    // 화면이 되살아나므로, 아직 같은 사람이 로그인해 있을 때만 새 값으로 바꾼다.
                    val stillLoggedIn = _uiState.value.currentUser?.id == userId
                    _uiState.update {
                        if (it.currentUser?.id == userId) it.copy(isLoading = false, currentUser = result.user)
                        else it.copy(isLoading = false)
                    }
                    if (stillLoggedIn) {
                        toastBus.show(R.string.s21_toast_saved)
                        onDone()
                    }
                }
                is AuthResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    toastBus.show(result.error.messageRes())
                }
            }
        }
    }

    /** 로그아웃: 세션을 지우고(알림도 정리됨) S01로. 토스트 "로그아웃했습니다." */
    fun logout() {
        // 버튼을 연달아 눌러도 한 번만 처리한다. 계정 삭제가 진행 중일 때도 받지 않는다
        // (삭제가 끝나기 전에 로그아웃 → 다른 계정 로그인이 끼어들면, 늦게 끝난 삭제가 새 로그인을 지워 버린다).
        val state = _uiState.value
        if (state.isLoggingOut || state.isDeletingAccount || state.currentUser == null) return
        _uiState.update { it.copy(isLoggingOut = true) }
        // 진행 중인 프로필 저장이 있으면 멈춘다. 로그아웃한 뒤에 그 결과가 상태(처리 중 표시 등)를 건드리지 않게 한다.
        profileJob?.cancel()
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: CancellationException) {
                throw e   // 작업이 취소된 것은 오류가 아니므로 그대로 넘긴다
            } catch (e: Exception) {
                // 세션은 logout()의 첫 줄에서 이미 지워졌다. 뒤따르는 알림 정리에서 오류가 나도
                // 앱이 꺼지지 않게 하고, 화면은 아래에서 로그인 전으로 보낸다.
            }
            _uiState.update { it.afterLogout() }
            toastBus.show(R.string.toast_logout_done)
        }
    }

    /**
     * S44 "계정·데이터 삭제". 확인 글자가 맞을 때만 지금 계정의 데이터를 지우고 온보딩(S00)으로 보낸다.
     *
     * 흐름: 확인 글자 검사 → AuthRepository.deleteCurrentAccount(DB → 로그인 기억 → 알림 정리)
     *      → 성공하면 currentUser = null + 시작 화면 S00 → MediCheckApp이 온보딩을 그린다 → 토스트
     * 실패하면 아무것도 바뀌지 않고 토스트만 뜬다.
     *
     * @param confirmText 확인 칸에 입력한 글자
     */
    fun deleteAccount(confirmText: String) {
        val state = _uiState.value
        if (state.isDeletingAccount || state.isLoggingOut || state.currentUser == null) return
        if (!DeleteConfirm.matches(confirmText)) {
            toastBus.show(R.string.s44_toast_mismatch)
            return
        }
        _uiState.update { it.copy(isDeletingAccount = true) }
        profileJob?.cancel()   // 진행 중인 프로필 저장이 지워진 계정의 상태를 건드리지 않게
        viewModelScope.launch {
            val deleted = try {
                authRepository.deleteCurrentAccount()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // DB는 이미 지워졌는데 뒤따르는 알림 정리에서 오류가 난 경우일 수 있다.
                // 로그인 기억이 지워졌으면 삭제는 끝난 것으로 보고 화면을 로그인 전으로 보낸다.
                authRepository.currentUser() == null
            }
            if (deleted) {
                onAccountDeleted()
                toastBus.show(R.string.s44_toast_deleted)
            } else {
                // isLoading도 끈다: 위에서 취소한 프로필 저장이 있었다면 그 "처리 중" 표시가 남지 않게.
                _uiState.update { it.copy(isDeletingAccount = false, isLoading = false) }
                toastBus.show(R.string.s44_toast_failed)
            }
        }
    }

    /**
     * 계정을 지운 뒤의 화면 전환. 온보딩이 다시 보이게 하고(onboarding_seen = false) 시작 화면을 S00으로.
     * 접근성 설정은 폰 단위이고 다른 계정이 남아 있을 수 있어 지우지 않는다(DB_DESIGN ⑧-3).
     * 사용자 없음과 S00을 한 번에 바꾼다 — 따로 바꾸면 잠깐 S01 화면이 그려질 수 있다.
     */
    private fun onAccountDeleted() {
        appPrefs.setOnboardingSeen(false)
        _uiState.update { it.afterAccountDeleted() }
    }

    /**
     * 로그인·샘플·가입이 함께 쓰는 순서: 처리 중 표시 → 요청 → 결과 반영 + 토스트.
     * 이미 처리 중이면 아무것도 하지 않는다(버튼을 연달아 눌러도 한 번만 처리).
     */
    private fun runAuthRequest(action: AuthAction, successToast: Int?, request: suspend () -> AuthResult) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, pressed = action) }

        viewModelScope.launch {
            // 예상 못 한 오류(DB·알람 등)가 나도 앱이 꺼지지 않게 하고, 버튼 잠금을 풀어 준다.
            val result = try {
                request()
            } catch (e: CancellationException) {
                throw e   // 화면이 닫혀 작업이 취소된 것은 오류가 아니므로 그대로 넘긴다
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, pressed = null) }
                toastBus.show(R.string.error_generic)
                return@launch
            }
            when (result) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, pressed = null, currentUser = result.user) }
                    successToast?.let(toastBus::show)
                }
                is AuthResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, pressed = null) }
                    toastBus.show(result.error.messageRes())
                }
            }
        }
    }

    companion object {
        /**
         * AuthViewModel을 만드는 방법을 안드로이드에게 알려 준다.
         * ViewModel은 안드로이드가 대신 만들어 주는데, 우리 ViewModel은 Repository 등이 필요하므로
         * "AppContainer에서 꺼내서 넣어 달라"고 적어 둔 것이다.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MediCheckApplication
                AuthViewModel(
                    app.container.authRepository, app.container.appPrefs, app.container.toastBus,
                    loadSettings = {
                        app.container.settingsRepository.loadForCurrentUser()
                        // 설정을 읽자마자 "처음부터 잠글지"를 정한다. 이 뒤에야 로딩 화면이 걷히므로
                        // 잠금을 켠 계정의 홈 화면이 한순간도 보이지 않는다.
                        app.container.appLock.onColdStart()
                    }
                )
            }
        }
    }
}
