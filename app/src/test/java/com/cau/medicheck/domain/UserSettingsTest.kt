package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** 설정값 · 알림 채널 고르기 · 앱 시계 테스트 */
class UserSettingsTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun `소리와 진동 조합마다 채널이 하나씩 있다`() {
        assertEquals("reminder_sound_vibrate", ReminderChannels.idFor(sound = true, vibrate = true))
        assertEquals("reminder_sound", ReminderChannels.idFor(sound = true, vibrate = false))
        assertEquals("reminder_vibrate", ReminderChannels.idFor(sound = false, vibrate = true))
        assertEquals("reminder_silent", ReminderChannels.idFor(sound = false, vibrate = false))
    }

    @Test
    fun `기본 설정은 알림 소리 진동 켜짐 재알림 꺼짐 15분 이름 숨김 켜짐이다`() {
        val defaults = UserSettings()

        assertEquals(listOf(true, true, true, false, true),
            listOf(defaults.reminderEnabled, defaults.soundEnabled, defaults.vibrateEnabled, defaults.realertEnabled, defaults.hideMedName))
        assertEquals(15, defaults.realertMinutes)
        assertEquals(TimeZoneMode.LOCAL, defaults.timeZoneMode)
    }

    @Test
    fun `고를 수 없는 재알림 간격은 15분으로 본다`() {
        assertEquals(listOf(10, 15, 30, 60), UserSettings.REALERT_OPTIONS.map(UserSettings::normalizeRealertMinutes))
        assertEquals(15, UserSettings.normalizeRealertMinutes(0))
        assertEquals(15, UserSettings.normalizeRealertMinutes(45))
    }

    @Test
    fun `기기 현지 시간 기준이면 폰의 시간대를 등록한 시간대 기준이면 등록한 시간대를 쓴다`() {
        val local = UserSettings(timeZoneMode = TimeZoneMode.LOCAL, homeZoneId = "Asia/Seoul")
        val home = UserSettings(timeZoneMode = TimeZoneMode.HOME, homeZoneId = "Asia/Seoul")

        assertEquals(newYork, local.zone(systemZone = newYork))
        assertEquals(seoul, home.zone(systemZone = newYork))
    }

    @Test
    fun `등록한 시간대를 읽을 수 없으면 폰의 시간대를 쓴다`() {
        assertEquals(newYork, UserSettings(timeZoneMode = TimeZoneMode.HOME, homeZoneId = null).zone(newYork))
        assertEquals(newYork, UserSettings(timeZoneMode = TimeZoneMode.HOME, homeZoneId = "없는/시간대").zone(newYork))
    }

    @Test
    fun `앱 시계는 같은 순간이라도 시간대에 따라 오늘이 달라진다`() {
        // 서울 2026-11-17 00:30 = 뉴욕 2026-11-16 10:30
        val moment = Clock.fixed(Instant.parse("2026-11-16T15:30:00Z"), ZoneOffset.UTC)
        var zone: ZoneId = seoul
        val clock = AppClock({ zone }, moment)

        assertEquals(LocalDate.of(2026, 11, 17), LocalDate.now(clock))
        assertEquals(LocalDateTime.of(2026, 11, 17, 0, 30), LocalDateTime.now(clock))

        // 시계를 새로 만들지 않아도, 시간대가 바뀌면 바로 그 시간대로 계산한다
        zone = newYork
        assertEquals(LocalDate.of(2026, 11, 16), LocalDate.now(clock))
        assertEquals(newYork, clock.zone)
    }
}
