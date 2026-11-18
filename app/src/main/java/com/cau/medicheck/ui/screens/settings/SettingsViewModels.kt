package com.cau.medicheck.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.R
import com.cau.medicheck.data.local.A11ySettings
import com.cau.medicheck.data.local.AppPrefs
import com.cau.medicheck.data.repository.SettingsRepository
import com.cau.medicheck.data.system.NetworkMonitor
import com.cau.medicheck.domain.UserSettings
import com.cau.medicheck.ui.components.ToastBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/*
 * 설정 화면들의 ViewModel 모음. (구현 명세 06)
 * ViewModel: 화면이 회전하거나 다시 그려져도 사라지지 않고 상태를 들고 있는 클래스.
 * S19·S20·S22·S39는 들고 있을 값이 없어 ViewModel이 없고, S21은 AuthViewModel을 쓴다.
 */

// =============================================================================================
// S36 개인정보 센터
// =============================================================================================

/**
 * S36 개인정보 센터의 ViewModel. 화면의 상태는 계정별 설정 그 자체(UserSettings)다.
 * 스위치는 누르는 즉시 저장한다(저장 버튼 없음).
 *   약 이름 숨김    → 다음에 뜨는 복약 알림부터 약 이름 대신 일반 문구 (ReminderNotifier가 읽는다)
 *   통계 미리보기 숨김 → 최근 앱 목록의 화면 미리보기를 가린다 (MainActivity가 지켜본다)
 */
class S36PrivacyViewModel(
    private val settingsRepository: SettingsRepository,
    private val toastBus: ToastBus
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.current

    fun setHideMedName(on: Boolean) = save { it.copy(hideMedName = on) }
    fun setHideStatsPreview(on: Boolean) = save { it.copy(hideStatsPreview = on) }

    private fun save(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            // 저장이나 알람 다시 걸기에서 예상 못 한 오류(DB 등)가 나도 앱이 꺼지지 않게 하고, 실패를 알린다.
            // 스위치는 저장된 값(settings)을 그리므로, 저장에 실패하면 원래 위치 그대로 남는다.
            try {
                settingsRepository.update(transform)
            } catch (e: CancellationException) {
                throw e   // 화면이 닫혀 작업이 취소된 것은 오류가 아니다
            } catch (e: Exception) {
                toastBus.show(R.string.error_generic)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MediCheckApplication
                S36PrivacyViewModel(app.container.settingsRepository, app.container.toastBus)
            }
        }
    }
}

// =============================================================================================
// S37 접근성 설정
// =============================================================================================

/**
 * S37 접근성 설정의 ViewModel. 값은 폰 단위 설정(AppPrefs)에 있다 — 계정과 상관없이,
 * 로그인 전 화면에도 같은 글자 크기가 적용되게 하기 위해서다.
 *
 * 저장하면 AppPrefs.a11y(StateFlow)가 새 값을 흘려보내고, 맨 바깥(MediCheckApp)이 그 값을 모든 화면에 건넨다.
 * 그래서 여기서는 "저장"만 하면 앱 전체가 바로 바뀐다.
 */
class S37AccessibilityViewModel(private val appPrefs: AppPrefs) : ViewModel() {

    val settings: StateFlow<A11ySettings> = appPrefs.a11y

    /** 글자 배율(100·125·150·200)을 고른다. 이미 고른 값이면 아무 일도 없다. */
    fun setFontScale(percent: Int) {
        if (percent == settings.value.fontScalePercent) return
        appPrefs.setA11y(settings.value.copy(fontScalePercent = percent))
    }

    fun setHighContrast(on: Boolean) = appPrefs.setA11y(settings.value.copy(highContrast = on))
    fun setReduceMotion(on: Boolean) = appPrefs.setA11y(settings.value.copy(reduceMotion = on))

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MediCheckApplication
                S37AccessibilityViewModel(app.container.appPrefs)
            }
        }
    }
}

// =============================================================================================
// S38 오프라인 상태
// =============================================================================================

/**
 * S38 오프라인 상태의 ViewModel. 폰이 지금 인터넷에 연결돼 있는지만 들고 있다.
 *
 * stateIn(WhileSubscribed): 화면이 이 값을 보고 있는 동안에만 네트워크 상태를 듣는다.
 * 화면을 떠나고 5초가 지나면 듣기를 그만둔다(화면을 돌리는 잠깐 동안은 끊지 않으려고 5초를 둔다).
 */
class S38OfflineViewModel(networkMonitor: NetworkMonitor) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = networkMonitor.isOnlineNow()
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MediCheckApplication
                S38OfflineViewModel(app.container.networkMonitor)
            }
        }
    }
}
