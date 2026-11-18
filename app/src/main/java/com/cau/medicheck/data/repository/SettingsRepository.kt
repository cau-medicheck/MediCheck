package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.SessionStore
import com.cau.medicheck.data.local.UserSettingsDao
import com.cau.medicheck.data.local.UserSettingsEntity
import com.cau.medicheck.domain.AppLockMethod
import com.cau.medicheck.domain.TimeZoneMode
import com.cau.medicheck.domain.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId

/**
 * 지금 로그인한 사용자의 설정(user_settings 표)을 읽고 고치는 곳.
 *
 * 설정을 메모리에 들고 있는 이유: 앱 시계(AppClock)와 알림 계산이 "지금 설정"을 아주 자주 물어보는데,
 * 그때마다 DB를 읽을 수는 없다. 그래서 [current]에 최신 값을 올려 두고, 고칠 때마다 함께 바꾼다.
 *
 * @param systemZone 폰의 지금 시간대. 설정 줄이 없는 계정에 기본 줄을 만들 때 home_zone_id로 쓴다.
 * @param onChanged 설정을 고친 뒤 부를 함수. 앱에서는 "알람을 다시 걸어라"가 들어온다.
 */
class SettingsRepository(
    private val sessionStore: SessionStore,
    private val userSettingsDao: UserSettingsDao,
    private val systemZone: () -> ZoneId = ZoneId::systemDefault,
    private val onChanged: suspend () -> Unit = {}
) {
    private val _current = MutableStateFlow(UserSettings())

    /** 지금 로그인한 사용자의 설정. 로그인한 사람이 없으면 기본값. */
    val current: StateFlow<UserSettings> = _current.asStateFlow()

    /** 읽기·고치기가 겹쳐서 옛 값으로 덮어쓰지 않도록 한 번에 하나씩만 실행한다. */
    private val mutex = Mutex()

    /**
     * 로그인한 사용자의 설정을 DB에서 읽어 [current]에 올린다. 설정 줄이 없으면 기본값으로 만든다.
     * 앱이 켜질 때, 로그인·로그아웃할 때, 알림 수신기가 깨어났을 때 부른다.
     */
    suspend fun loadForCurrentUser() = mutex.withLock {
        val userId = sessionStore.getLoggedInUserId()
        _current.value = if (userId == null) UserSettings() else loadEntity(userId).toDomain()
    }

    /**
     * 설정을 고쳐 저장한다: DB에 저장 → [current] 갱신 → [onChanged] 호출.
     * @param transform 지금 설정을 받아 고친 설정을 돌려주는 함수 (예: { it.copy(soundEnabled = false) })
     */
    suspend fun update(transform: (UserSettings) -> UserSettings) {
        mutex.withLock {
            val userId = sessionStore.getLoggedInUserId() ?: return
            // 메모리의 값이 아니라 DB의 줄을 다시 읽어 그 위에 고친다 (home_zone_id처럼 고치지 않는 칸을 잃지 않게).
            val entity = loadEntity(userId)
            val changed = transform(entity.toDomain())
            val saved = entity.copy(
                reminderEnabled = changed.reminderEnabled,
                soundEnabled = changed.soundEnabled,
                vibrateEnabled = changed.vibrateEnabled,
                realertEnabled = changed.realertEnabled,
                realertMinutes = UserSettings.normalizeRealertMinutes(changed.realertMinutes),
                hideMedName = changed.hideMedName,
                timeZoneMode = changed.timeZoneMode.name,
                hideStatsPreview = changed.hideStatsPreview,
                appLockEnabled = changed.appLockEnabled,
                appLockMethod = changed.appLockMethod.name
            )
            userSettingsDao.upsert(saved)
            _current.value = saved.toDomain()
        }
        onChanged()
    }

    private suspend fun loadEntity(userId: Long): UserSettingsEntity =
        userSettingsDao.find(userId)
            ?: UserSettingsEntity.defaultsFor(userId, systemZone().id).also { userSettingsDao.upsert(it) }

    /** 표의 줄(글자·숫자) → 계산에 쓰는 모양 */
    private fun UserSettingsEntity.toDomain() = UserSettings(
        reminderEnabled = reminderEnabled,
        soundEnabled = soundEnabled,
        vibrateEnabled = vibrateEnabled,
        realertEnabled = realertEnabled,
        realertMinutes = UserSettings.normalizeRealertMinutes(realertMinutes),
        hideMedName = hideMedName,
        timeZoneMode = if (timeZoneMode == UserSettingsEntity.TIME_ZONE_HOME) TimeZoneMode.HOME else TimeZoneMode.LOCAL,
        homeZoneId = homeZoneId,
        hideStatsPreview = hideStatsPreview,
        appLockEnabled = appLockEnabled,
        // 모르는 글자가 저장돼 있으면 생체인증으로 읽는다.
        appLockMethod = if (appLockMethod == UserSettingsEntity.APP_LOCK_DEVICE_CREDENTIAL) {
            AppLockMethod.DEVICE_CREDENTIAL
        } else {
            AppLockMethod.BIOMETRIC
        }
    )
}
