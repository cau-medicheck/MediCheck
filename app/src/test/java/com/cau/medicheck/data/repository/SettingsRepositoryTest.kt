package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.UserSettingsEntity
import com.cau.medicheck.domain.TimeZoneMode
import com.cau.medicheck.domain.UserSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

/** SettingsRepository 단위 테스트: 계정별 설정 읽기·저장 */
class SettingsRepositoryTest {

    private val me = 1L
    private val other = 2L

    private lateinit var sessionStore: FakeSessionStore
    private lateinit var dao: FakeUserSettingsDao
    private lateinit var repository: SettingsRepository
    private var changedCount = 0

    @Before
    fun setUp() {
        sessionStore = FakeSessionStore()
        dao = FakeUserSettingsDao()
        changedCount = 0
        repository = SettingsRepository(
            sessionStore, dao,
            systemZone = { ZoneId.of("Asia/Seoul") },
            onChanged = { changedCount++ }
        )
    }

    @Test
    fun `로그인한 사람이 없으면 기본값이다`() = runBlocking<Unit> {
        repository.loadForCurrentUser()

        assertEquals(UserSettings(), repository.current.value)
    }

    @Test
    fun `설정 줄이 없는 계정은 기본값으로 줄을 만든다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)

        repository.loadForCurrentUser()

        assertEquals(UserSettings(homeZoneId = "Asia/Seoul"), repository.current.value)
        assertEquals("Asia/Seoul", dao.find(me)?.homeZoneId)
    }

    @Test
    fun `고치면 저장되고 지금 설정이 바뀌고 알람을 다시 걸라고 알린다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        repository.loadForCurrentUser()

        repository.update { it.copy(soundEnabled = false, realertEnabled = true, realertMinutes = 30) }

        assertEquals(false, repository.current.value.soundEnabled)
        assertEquals(30, repository.current.value.realertMinutes)
        assertEquals(false, dao.find(me)?.soundEnabled)
        assertEquals(true, dao.find(me)?.realertEnabled)
        assertEquals(1, changedCount)
    }

    @Test
    fun `설정은 계정마다 따로다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        repository.loadForCurrentUser()
        repository.update { it.copy(reminderEnabled = false, timeZoneMode = TimeZoneMode.HOME) }

        // 다른 계정으로 로그인하면 그 계정은 기본값
        sessionStore.saveLoggedInUserId(other)
        repository.loadForCurrentUser()
        assertTrue(repository.current.value.reminderEnabled)
        assertEquals(TimeZoneMode.LOCAL, repository.current.value.timeZoneMode)

        // 다시 돌아오면 저장한 값
        sessionStore.saveLoggedInUserId(me)
        repository.loadForCurrentUser()
        assertEquals(false, repository.current.value.reminderEnabled)
        assertEquals(TimeZoneMode.HOME, repository.current.value.timeZoneMode)
    }

    @Test
    fun `고를 수 없는 재알림 간격은 15분으로 읽고 15분으로 저장한다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        dao.insert(UserSettingsEntity(userId = me, homeZoneId = "Asia/Seoul", realertMinutes = 7))

        repository.loadForCurrentUser()
        assertEquals(15, repository.current.value.realertMinutes)

        repository.update { it.copy(realertMinutes = 45) }
        assertEquals(15, dao.find(me)?.realertMinutes)
    }

    @Test
    fun `화면에 없는 칸은 고쳐도 그대로 남는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        dao.insert(UserSettingsEntity(userId = me, homeZoneId = "America/New_York", appLockEnabled = true))
        repository.loadForCurrentUser()

        repository.update { it.copy(vibrateEnabled = false) }

        val saved = dao.find(me)!!
        assertEquals(true, saved.appLockEnabled)
        assertEquals("America/New_York", saved.homeZoneId)
        assertEquals(false, saved.vibrateEnabled)
    }

    @Test
    fun `로그아웃한 상태에서는 고쳐도 아무 일도 없다`() = runBlocking<Unit> {
        repository.update { it.copy(soundEnabled = false) }

        assertEquals(UserSettings(), repository.current.value)
        assertTrue(dao.rows.value.isEmpty())
        assertEquals(0, changedCount)
    }
}
