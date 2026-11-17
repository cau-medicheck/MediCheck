package com.cau.medicheck.ui.screens.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.R
import com.cau.medicheck.data.repository.ReminderRepository
import com.cau.medicheck.data.repository.SettingsRepository
import com.cau.medicheck.domain.ReminderChannels
import com.cau.medicheck.domain.ReminderDose
import com.cau.medicheck.domain.TimeZoneMode
import com.cau.medicheck.domain.UserSettings
import com.cau.medicheck.notification.NotificationAccess
import com.cau.medicheck.ui.components.ToastBus
import com.cau.medicheck.ui.screens.dose.tickerFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// =============================================================================================
// S16 알림 센터
// =============================================================================================

/**
 * 알림 센터 화면이 그릴 값.
 * @param items 앞으로 실제로 울릴 알림 (오늘 남은 것 + 내일, 최대 8개)
 * @param hasActiveMedication 사용 중인 약이 하나라도 있는가 (빈 목록의 안내 글을 고르는 데 쓴다)
 */
data class NotificationCenterUiState(
    val isLoading: Boolean = true,
    val items: List<ReminderDose> = emptyList(),
    val hasActiveMedication: Boolean = false,
    val reminderEnabled: Boolean = true,
    val hideMedName: Boolean = true
)

/**
 * S16 알림 센터의 ViewModel.
 * 목록은 알람 예약과 같은 계산(ReminderRepository.upcoming)으로 만든다. 그래서 맨 위 줄이 곧 다음에 울릴 알람이다.
 * 시각이 지나면 줄이 빠져야 하므로 30초마다, 그리고 화면이 다시 보일 때마다 다시 계산한다.
 */
class S16NotificationCenterViewModel(
    private val reminderRepository: ReminderRepository,
    settingsRepository: SettingsRepository,
    clock: Clock
) : ViewModel() {

    /** 이 숫자를 바꾸면 목록을 다시 계산한다 (화면이 다시 보일 때) */
    private val refreshCount = MutableStateFlow(0)

    val uiState: StateFlow<NotificationCenterUiState> =
        combine(tickerFlow(clock), refreshCount, settingsRepository.current) { _, _, settings -> settings }
            // mapLatest: 계산 중에 새 값이 오면 하던 계산을 버리고 새로 한다.
            .mapLatest { settings ->
                val upcoming = reminderRepository.upcoming()
                NotificationCenterUiState(
                    isLoading = false,
                    items = upcoming.items,
                    hasActiveMedication = upcoming.hasActiveMedication,
                    reminderEnabled = settings.reminderEnabled,
                    hideMedName = settings.hideMedName
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationCenterUiState())

    fun refresh() {
        refreshCount.update { it + 1 }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                S16NotificationCenterViewModel(
                    container.reminderRepository, container.settingsRepository, container.clock
                )
            }
        }
    }
}

// =============================================================================================
// S17 알림 설정
// =============================================================================================

/**
 * S17 알림 설정의 ViewModel. 화면의 상태는 설정 그 자체(UserSettings)다.
 * 스위치·간격은 바꾸는 즉시 저장한다(저장 버튼 없음). 저장하면 SettingsRepository가 알람을 다시 건다.
 *
 * @param sendTest 테스트 알림을 띄우는 함수. 폰에서 알림이 꺼져 있어 못 띄웠으면 false를 돌려준다.
 */
class S17NotificationSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val toastBus: ToastBus,
    private val sendTest: (UserSettings) -> Boolean
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.current

    fun setReminderEnabled(on: Boolean) = save { it.copy(reminderEnabled = on) }
    fun setSoundEnabled(on: Boolean) = save { it.copy(soundEnabled = on) }
    fun setVibrateEnabled(on: Boolean) = save { it.copy(vibrateEnabled = on) }
    fun setRealertEnabled(on: Boolean) = save { it.copy(realertEnabled = on) }

    /** 재알림 간격을 고른다. 같은 값을 다시 고르면 아무 일도 없다. */
    fun setRealertMinutes(minutes: Int) {
        if (minutes == settings.value.realertMinutes) return
        viewModelScope.launch {
            settingsRepository.update { it.copy(realertMinutes = minutes) }
            toastBus.show(R.string.s17_realert_changed)
        }
    }

    /** "테스트 알림 보내기". '복약 시간 알림'이 꺼져 있어도 보낸다 (폰이 알림을 어떻게 보여 주는지 시험하는 버튼이다). */
    fun sendTestNotification() {
        val sent = sendTest(settings.value)
        toastBus.show(if (sent) R.string.s17_test_sent else R.string.s17_test_blocked)
    }

    private fun save(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MediCheckApplication
                val container = app.container
                S17NotificationSettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    toastBus = container.toastBus,
                    sendTest = { settings ->
                        val enabled = NotificationAccess.notificationsEnabled(app, ReminderChannels.idFor(settings))
                        if (enabled) container.reminderNotifier.showTest(settings)
                        enabled
                    }
                )
            }
        }
    }
}

// =============================================================================================
// S35 시간대 변경 정책
// =============================================================================================

/**
 * @param saved 저장되어 있는 정책
 * @param selected 화면에서 고른 정책 ("정책 저장"을 눌러야 저장된다)
 * @param homeZoneId 등록한 시간대 (예: "Asia/Seoul")
 * @param homeZoneOffset 그 시간대가 UTC보다 얼마나 빠른지 (예: "+09:00")
 */
data class TimezonePolicyUiState(
    val saved: TimeZoneMode = TimeZoneMode.LOCAL,
    val selected: TimeZoneMode = TimeZoneMode.LOCAL,
    val homeZoneId: String = "",
    val homeZoneOffset: String = "",
    val isSaving: Boolean = false
)

/** S35 시간대 변경 정책의 ViewModel. */
class S35TimezonePolicyViewModel(
    private val settingsRepository: SettingsRepository,
    private val toastBus: ToastBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState(settingsRepository.current.value))
    val uiState: StateFlow<TimezonePolicyUiState> = _uiState

    fun select(mode: TimeZoneMode) {
        _uiState.update { it.copy(selected = mode) }
    }

    /**
     * "정책 저장": 저장하면 앱 시계의 기준 시간대가 바로 바뀌고 알람도 새 기준으로 다시 걸린다.
     * 같은 값을 다시 저장해도 토스트는 뜬다 (v3와 같음).
     */
    fun save() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val mode = _uiState.value.selected
            settingsRepository.update { it.copy(timeZoneMode = mode) }
            _uiState.update { it.copy(saved = mode, isSaving = false) }
            toastBus.show(R.string.s35_saved)
        }
    }

    private fun initialState(settings: UserSettings): TimezonePolicyUiState {
        val zoneId = settings.homeZoneId ?: ZoneId.systemDefault().id
        return TimezonePolicyUiState(
            saved = settings.timeZoneMode,
            selected = settings.timeZoneMode,
            homeZoneId = zoneId,
            homeZoneOffset = offsetText(zoneId)
        )
    }

    companion object {
        /**
         * 시간대 id → "+09:00" 모양. UTC는 "+00:00"으로 나온다.
         * (ZoneOffset을 그대로 글자로 바꾸면 UTC가 "Z"가 되어 "(UTCZ)"처럼 보인다)
         */
        fun offsetText(zoneId: String): String =
            runCatching { ZonedDateTime.now(ZoneId.of(zoneId)).format(DateTimeFormatter.ofPattern("xxx")) }
                .getOrDefault("")

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                S35TimezonePolicyViewModel(container.settingsRepository, container.toastBus)
            }
        }
    }
}
