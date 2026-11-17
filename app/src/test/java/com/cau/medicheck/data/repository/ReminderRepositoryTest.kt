package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecord
import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.Schedule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * ReminderRepository 단위 테스트.
 * 시계는 2026-11-16(월) 10:00에 멈춰 둔다.
 */
class ReminderRepositoryTest {

    private val me = 1L
    private val someoneElse = 2L
    private val clock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC)
    private val today8 = LocalDateTime.of(2026, 11, 16, 8, 0)
    private val today20 = LocalDateTime.of(2026, 11, 16, 20, 0)

    private lateinit var sessionStore: FakeSessionStore
    private lateinit var medicationDao: FakeMedicationDao
    private lateinit var scheduleDao: FakeScheduleDao
    private lateinit var doseRecordDao: FakeDoseRecordDao
    private lateinit var repository: ReminderRepository

    @Before
    fun setUp() {
        sessionStore = FakeSessionStore()
        medicationDao = FakeMedicationDao()
        scheduleDao = FakeScheduleDao(medicationDao)
        doseRecordDao = FakeDoseRecordDao(medicationDao, scheduleDao)
        repository = ReminderRepository(sessionStore, scheduleDao, doseRecordDao, clock)
    }

    /** 매일 먹는 약 하나를 표에 넣고 규칙의 id를 돌려준다. */
    private suspend fun insertDaily(userId: Long, name: String, time: String): Long {
        val medicationId = medicationDao.insert(
            Medication(
                userId = userId, name = name, dosage = "1정",
                startDate = "2026-11-01", endDate = null, memo = null, createdAt = 0
            )
        )
        return scheduleDao.insert(
            Schedule(
                medicationId = medicationId, time = time,
                repeatDays = "MON,TUE,WED,THU,FRI,SAT,SUN", createdAt = 0
            )
        )
    }

    /** 기록의 약 이름 스냅샷은 그 약의 실제 이름으로 넣는다(알림 문구가 스냅샷을 쓰므로). */
    private suspend fun insertRecord(scheduleId: Long, status: String) {
        val medicationId = scheduleDao.schedules.value.single { it.id == scheduleId }.medicationId
        val name = medicationDao.medications.value.single { it.id == medicationId }.name
        doseRecordDao.insertAllIgnore(
            listOf(
                DoseRecord(
                    scheduleId = scheduleId, date = "2026-11-16",
                    scheduledTime = "20:00", medicationName = name, dosage = "1정",
                    status = status, createdAt = 0
                )
            )
        )
    }

    @Test
    fun `로그인한 사람이 없으면 다음 알림이 없다`() = runBlocking<Unit> {
        insertDaily(me, "타이레놀", "20:00")

        assertNull(repository.nextTrigger())
    }

    @Test
    fun `로그인한 사람의 약 중 가장 이른 다음 시각을 돌려준다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        insertDaily(me, "아침 약", "08:00")   // 오늘은 이미 지남 → 내일 08:00
        insertDaily(me, "저녁 약", "20:00")   // 오늘 20:00

        assertEquals(today20, repository.nextTrigger())
    }

    @Test
    fun `다른 사람의 약은 알림에 넣지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        insertDaily(someoneElse, "남의 약", "12:00")
        insertDaily(me, "내 약", "20:00")

        assertEquals(today20, repository.nextTrigger())
        assertTrue(repository.dueReminders(LocalDateTime.of(2026, 11, 16, 12, 0)).isEmpty())
    }

    @Test
    fun `삭제한 약은 알림에 넣지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val scheduleId = insertDaily(me, "타이레놀", "20:00")
        val medicationId = scheduleDao.schedules.value.single { it.id == scheduleId }.medicationId
        scheduleDao.softDeleteByMedication(medicationId, deletedAt = 1)
        medicationDao.softDelete(medicationId, deletedAt = 1)

        assertNull(repository.nextTrigger())
        assertTrue(repository.dueReminders(today20).isEmpty())
    }

    @Test
    fun `그 시각에 먹어야 하는 약을 돌려준다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        insertDaily(me, "아침 약", "08:00")
        insertDaily(me, "저녁 약", "20:00")

        assertEquals(listOf("아침 약"), repository.dueReminders(today8).map { it.medicationName })
        assertEquals(listOf("저녁 약"), repository.dueReminders(today20).map { it.medicationName })
    }

    @Test
    fun `아직 응답하지 않은 기록이 있어도 알린다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val scheduleId = insertDaily(me, "저녁 약", "20:00")
        insertRecord(scheduleId, "PENDING")

        assertEquals(1, repository.dueReminders(today20).size)
    }

    @Test
    fun `이미 복용 완료나 건너뜀으로 기록한 약은 알리지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val taken = insertDaily(me, "미리 먹은 약", "20:00")
        val skipped = insertDaily(me, "건너뛴 약", "20:00")
        val pending = insertDaily(me, "아직 안 먹은 약", "20:00")
        insertRecord(taken, "TAKEN")
        insertRecord(skipped, "SKIPPED")
        insertRecord(pending, "PENDING")

        assertEquals(listOf("아직 안 먹은 약"), repository.dueReminders(today20).map { it.medicationName })
    }

    @Test
    fun `적용 시작일 전에는 알리지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val scheduleId = insertDaily(me, "저녁 약", "20:00")
        // 오늘 "내일부터 적용"으로 바꾼 규칙
        scheduleDao.update(scheduleDao.schedules.value.single { it.id == scheduleId }.copy(effectiveFrom = "2026-11-17"))

        assertTrue(repository.dueReminders(today20).isEmpty())
        assertEquals(LocalDateTime.of(2026, 11, 17, 20, 0), repository.nextTrigger())
    }

    @Test
    fun `일시중지한 약은 알리지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val scheduleId = insertDaily(me, "저녁 약", "20:00")
        val medicationId = scheduleDao.schedules.value.single { it.id == scheduleId }.medicationId
        assertEquals(1, medicationDao.setPausedFrom(me, medicationId, "2026-11-16"))

        assertNull(repository.nextTrigger())
        assertTrue(repository.dueReminders(today20).isEmpty())
    }

    @Test
    fun `다른 사람의 약은 일시중지할 수 없다`() = runBlocking<Unit> {
        val scheduleId = insertDaily(someoneElse, "남의 약", "20:00")
        val medicationId = scheduleDao.schedules.value.single { it.id == scheduleId }.medicationId

        assertEquals(0, medicationDao.setPausedFrom(me, medicationId, "2026-11-16"))
        assertEquals(0, scheduleDao.raiseEffectiveFrom(me, medicationId, "2026-11-16"))
        assertNull(medicationDao.medications.value.single().pausedFrom)
    }
}
