package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseStatusEvent
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.domain.SampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * SampleAccountSeeder 단위 테스트. 시계는 2026-11-16(월) 10:00(UTC)에 멈춰 둔다.
 * 가짜 DAO는 CASCADE를 흉내 내지 않으므로, 옛 샘플이 0줄이 되는지로 "명시적 삭제 순서"가 빠짐없는지 확인된다.
 */
class SampleAccountSeederTest {

    private val today = LocalDate.of(2026, 11, 16)
    private val todayClock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC)
    private val tomorrowClock = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC)

    private lateinit var userDao: FakeUserDao
    private lateinit var medicationDao: FakeMedicationDao
    private lateinit var scheduleDao: FakeScheduleDao
    private lateinit var doseRecordDao: FakeDoseRecordDao
    private lateinit var eventDao: FakeDoseStatusEventDao
    private lateinit var settingsDao: FakeUserSettingsDao

    @Before
    fun setUp() {
        userDao = FakeUserDao()
        medicationDao = FakeMedicationDao()
        scheduleDao = FakeScheduleDao(medicationDao)
        doseRecordDao = FakeDoseRecordDao(medicationDao, scheduleDao)
        eventDao = FakeDoseStatusEventDao()
        settingsDao = FakeUserSettingsDao()
    }

    /** 해시 계산은 테스트 스레드에서 바로 하게(Dispatchers.Unconfined) 한다. */
    private fun seeder(clock: Clock) = SampleAccountSeeder(
        userDao, medicationDao, scheduleDao, doseRecordDao, eventDao, settingsDao,
        FakeTransactionRunner(), clock, Dispatchers.Unconfined
    )

    @Test
    fun `처음 들어가면 샘플 계정과 약 3개 기록 99줄 설정 줄을 만든다`() = runBlocking<Unit> {
        val user = seeder(todayClock).prepare()

        assertEquals(SampleData.EMAIL, user.email)
        assertEquals("사용자", user.displayName)
        assertTrue(user.isSample)
        assertTrue(user.passwordHash.startsWith("pbkdf2$"))
        assertEquals(3, medicationDao.medications.value.size)
        assertEquals(3, scheduleDao.schedules.value.size)
        assertEquals(99, doseRecordDao.records.value.size)
        assertEquals(user.id, settingsDao.rows.value.single().userId)
        assertEquals("Z", settingsDao.rows.value.single().homeZoneId)
        assertTrue(eventDao.events.value.isEmpty())
    }

    @Test
    fun `약의 시작일과 첫 기록은 오늘보다 32일 전이다`() = runBlocking<Unit> {
        seeder(todayClock).prepare()

        assertTrue(medicationDao.medications.value.all { it.startDate == "2026-10-15" })
        assertTrue(medicationDao.medications.value.all { it.endDate == null && it.pausedFrom == null })
        assertTrue(scheduleDao.schedules.value.all { it.effectiveFrom == null })
        assertEquals("2026-10-15", doseRecordDao.records.value.minOf { it.date })
        assertEquals("2026-11-16", doseRecordDao.records.value.maxOf { it.date })
    }

    @Test
    fun `기록의 스냅샷 칸은 약 값으로 채우고 상태 수는 공식과 같다`() = runBlocking<Unit> {
        seeder(todayClock).prepare()

        val records = doseRecordDao.records.value
        val vitamin = records.filter { it.medicationName == "비타민 D" }
        assertEquals(33, vitamin.size)
        assertTrue(vitamin.all { it.dosage == "1정" && it.memo == "아침 식후" && it.scheduledTime == "08:00" })
        assertTrue(records.all { it.actedAt == null })
        val counts = records.groupingBy { it.status }.eachCount()
        assertEquals(mapOf("TAKEN" to 73, "SKIPPED" to 12, "PENDING" to 14), counts)
    }

    @Test
    fun `샘플이 오늘까지 기록을 만들어 두어 보충이 중복을 만들지 않는다`() = runBlocking<Unit> {
        val user = seeder(todayClock).prepare()
        val generator = DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, eventDao, todayClock)

        generator.generateForUser(user.id)

        assertEquals(99, doseRecordDao.records.value.size)
    }

    @Test
    fun `같은 날 다시 들어가면 같은 계정을 그대로 쓴다`() = runBlocking<Unit> {
        val first = seeder(todayClock).prepare()
        medicationDao.update(medicationDao.medications.value.first().copy(name = "바꾼 이름"))

        val second = seeder(todayClock).prepare()

        assertEquals(first.id, second.id)
        assertEquals(1, userDao.users.size)
        assertEquals("바꾼 이름", medicationDao.medications.value.first().name)
    }

    @Test
    fun `다음 날 들어가면 옛 샘플의 모든 데이터를 지우고 새로 만든다`() = runBlocking<Unit> {
        val old = seeder(todayClock).prepare()
        // 옛 샘플에 상태 변경 이력도 하나 만들어 둔다
        val doseRepository = DoseRepository(
            doseRecordDao, eventDao,
            DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, eventDao, todayClock),
            FakeTransactionRunner(), todayClock
        )
        doseRepository.changeStatus(old.id, DoseKey(1, today), DoseStatus.SKIPPED)
        assertEquals(1, eventDao.events.value.size)

        val fresh = seeder(tomorrowClock).prepare()

        assertNotEquals(old.id, fresh.id)
        assertNull(userDao.findById(old.id))
        assertEquals(1, userDao.users.size)
        // 옛 샘플의 이력·기록·규칙·약·설정이 0줄 (CASCADE에 기대지 않음)
        assertTrue(eventDao.events.value.none { it.userId == old.id })
        assertTrue(settingsDao.rows.value.none { it.userId == old.id })
        assertTrue(medicationDao.medications.value.none { it.userId == old.id })
        assertEquals(3, medicationDao.medications.value.size)
        assertEquals(3, scheduleDao.schedules.value.size)
        assertEquals(99, doseRecordDao.records.value.size)
        assertEquals("2026-11-17", doseRecordDao.records.value.maxOf { it.date })
    }

    @Test
    fun `다른 계정의 데이터는 샘플을 다시 만들어도 남는다`() = runBlocking<Unit> {
        val other = userDao.insert(
            com.cau.medicheck.data.local.User(email = "a@b.co", passwordHash = "x", displayName = "남", createdAt = 0)
        )
        eventDao.insert(
            DoseStatusEvent(
                userId = other, doseRecordId = null, scheduleId = 99, doseDate = "2026-11-01",
                scheduledTime = "08:00", medicationName = "남의 약", fromStatus = "PENDING",
                toStatus = "TAKEN", fromActedAt = null, toActedAt = 1, changedAt = 1
            )
        )
        seeder(todayClock).prepare()

        seeder(tomorrowClock).prepare()

        assertTrue(userDao.users.any { it.id == other })
        assertEquals(1, eventDao.events.value.count { it.userId == other })
    }
}
