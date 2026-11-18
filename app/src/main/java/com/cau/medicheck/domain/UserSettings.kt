package com.cau.medicheck.domain

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** 알림 기준 시간 정책 (S35). LOCAL = 폰의 지금 시간대, HOME = 가입할 때 등록한 시간대 */
enum class TimeZoneMode { LOCAL, HOME }

/** 앱 잠금을 풀 때 본인 확인 방법 (S41). BIOMETRIC = 기기 생체인증, DEVICE_CREDENTIAL = 기기 PIN·패턴·비밀번호 */
enum class AppLockMethod { BIOMETRIC, DEVICE_CREDENTIAL }

/**
 * 계정별 설정. (user_settings 표의 화면·계산용 모양)
 * 기본값은 v3 프로토타입의 처음 상태와 같다: 알림·소리·진동 켜짐, 재알림 꺼짐, 15분, 약 이름 숨김 켜짐,
 * 통계 미리보기 숨김·앱 잠금 꺼짐.
 *
 * @param homeZoneId 가입할 때 폰의 시간대 (예: "Asia/Seoul"). 로그인 전에는 null
 * @param hideStatsPreview '통계 미리보기 숨김'(S36): 최근 앱 목록에 앱 화면 미리보기가 보이지 않게 한다
 * @param appLockEnabled '앱 잠금'(S41)
 */
data class UserSettings(
    val reminderEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val realertEnabled: Boolean = false,
    val realertMinutes: Int = DEFAULT_REALERT_MINUTES,
    val hideMedName: Boolean = true,
    val timeZoneMode: TimeZoneMode = TimeZoneMode.LOCAL,
    val homeZoneId: String? = null,
    val hideStatsPreview: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockMethod: AppLockMethod = AppLockMethod.BIOMETRIC
) {
    /**
     * 최근 앱 목록(앱 전환 화면)에 이 앱의 화면 미리보기를 가려야 하는지.
     * 통계 미리보기 숨김을 켰거나, 앱 잠금을 켠 경우(잠긴 앱의 화면이 미리보기로 보이면 안 되므로).
     */
    val hidesRecentsPreview: Boolean get() = hideStatsPreview || appLockEnabled

    /**
     * 앱이 "지금·오늘"을 계산할 시간대.
     * HOME이면 등록한 시간대, 그 밖에는(또는 등록한 시간대를 읽을 수 없으면) 폰의 지금 시간대.
     */
    fun zone(systemZone: ZoneId): ZoneId {
        if (timeZoneMode != TimeZoneMode.HOME || homeZoneId == null) return systemZone
        return runCatching { ZoneId.of(homeZoneId) }.getOrDefault(systemZone)
    }

    companion object {
        const val DEFAULT_REALERT_MINUTES = 15

        /** 재알림 간격으로 고를 수 있는 값(분) */
        val REALERT_OPTIONS = listOf(10, 15, 30, 60)

        /** 고를 수 없는 값이 저장돼 있으면 15분으로 본다. */
        fun normalizeRealertMinutes(minutes: Int): Int =
            if (minutes in REALERT_OPTIONS) minutes else DEFAULT_REALERT_MINUTES
    }
}

/**
 * 앱 시계. "지금 몇 시인가"와 "어느 시간대로 볼 것인가"를 한곳에서 정한다. (S35)
 *
 * Clock은 자바 표준 클래스로, LocalDate.now(clock)처럼 넘기면 그 시계 기준의 오늘을 돌려준다.
 * 보통의 Clock.systemDefaultZone()은 만든 순간의 시간대로 굳어 버려서, 앱이 켜져 있는 동안
 * 폰의 시간대가 바뀌면 옛 시간대로 계산한다. 이 시계는 부를 때마다 시간대를 다시 물어본다.
 *
 * @param zoneProvider 지금 써야 할 시간대를 돌려주는 함수 (설정이 LOCAL이면 폰 시간대, HOME이면 등록한 시간대)
 */
class AppClock(
    private val zoneProvider: () -> ZoneId,
    private val base: Clock = Clock.systemUTC()
) : Clock() {
    override fun getZone(): ZoneId = zoneProvider()
    override fun withZone(zone: ZoneId): Clock = base.withZone(zone)
    override fun instant(): Instant = base.instant()
}

/**
 * 소리·진동 설정에 맞는 알림 채널 고르기.
 *
 * 채널(channel)은 안드로이드가 알림을 종류별로 묶는 단위다. 한 번 만든 채널의 소리·진동은 앱이 바꿀 수 없어서,
 * 조합 4가지마다 채널을 하나씩 미리 만들어 두고 알림을 띄울 때 맞는 채널을 고른다.
 */
object ReminderChannels {
    const val SOUND_VIBRATE = "reminder_sound_vibrate"
    const val SOUND = "reminder_sound"
    const val VIBRATE = "reminder_vibrate"
    const val SILENT = "reminder_silent"

    /** 1단계~4단계에서 쓰던 채널. 앱이 켜질 때 지운다. */
    const val LEGACY = "medication_reminders"

    fun idFor(sound: Boolean, vibrate: Boolean): String = when {
        sound && vibrate -> SOUND_VIBRATE
        sound -> SOUND
        vibrate -> VIBRATE
        else -> SILENT
    }

    fun idFor(settings: UserSettings): String = idFor(settings.soundEnabled, settings.vibrateEnabled)
}
