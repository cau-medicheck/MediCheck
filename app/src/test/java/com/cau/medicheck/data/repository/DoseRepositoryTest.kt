package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.domain.DoseStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * DoseRecordGenerator(기록 생성·보충)와 DoseRepository(조회·상태 변경) 단위 테스트.
 *
 * 시계는 2026-11-16(월) 10:00에 멈춰 둔다.
 */
class DoseRepositoryTest {

    private val me = 1L
    private val someoneElse = 2L
    private val today = LocalDate.of(2026, 11, 16)
    private val clock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC)

    private lateinit var medicationDao: FakeMedicationDao
    private lateinit var scheduleDao: FakeScheduleDao
    private lateinit var doseRecordDao: FakeDoseRecordDao
    private lateinit var generator: DoseRecordGenerator
    private lateinit var repository: DoseRepository

    @Before
    fun setUp() {
        medicationDao = FakeMedicationDao()
        scheduleDao = FakeScheduleDao(medicationDao)
        doseRecordDao = FakeDoseRecordDao(medicationDao, scheduleDao)
        generator = DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, clock)
        repository = DoseRepository(doseRecordDao, generator, FakeTransactionRunner(), clock)
    }

    /** 약 하나와 규칙 하나를 표에 직접 넣는다. (기록은 만들지 않는다) */
    private suspend fun insertMedication(
        userId: Long = me,
        name: String = "타이레놀",
        startDate: LocalDate = today,
        endDate: LocalDate? = null,
        time: String = "08:00",
        repeatDays: String = "MON,TUE,WED,THU,FRI,SAT,SUN"
    ): Long {
        val medicationId = medicationDao.insert(
            Medication(
                userId = userId, name = name, dosage = "1정",
                startDate = startDate.toString(), endDate = endDate?.toString(),
                memo = null, createdAt = 0
            )
        )
        scheduleDao.insert(
            Schedule(medicationId = medicationId, time = time, repeatDays = repeatDays, createdAt = 0)
        )
        return medicationId
    }

    private suspend fun todayDoses(userId: Long = me) = repository.observeDoses(userId, today, today).first()

    // ---------- 기록 생성 · 보충 ----------

    @Test
    fun `refresh는 시작일부터 오늘까지의 기록을 PENDING으로 만든다`() = runBlocking<Unit> {
        insertMedication(startDate = today.minusDays(2))

        repository.refresh(me)

        val records = doseRecordDao.records.value
        assertEquals(listOf("2026-11-14", "2026-11-15", "2026-11-16"), records.map { it.date })
        assertTrue(records.all { it.status == "PENDING" && it.scheduledTime == "08:00" && it.actedAt == null })
    }

    @Test
    fun `refresh를 여러 번 불러도 기록이 중복으로 생기지 않는다`() = runBlocking<Unit> {
        insertMedication(startDate = today.minusDays(2))

        repository.refresh(me)
        repository.refresh(me)
        repository.refresh(me)

        assertEquals(3, doseRecordDao.records.value.size)
    }

    @Test
    fun `반복 요일이 아닌 날의 기록은 만들지 않는다`() = runBlocking<Unit> {
        // 오늘은 월요일. 화·목에만 먹는 약은 오늘 기록이 없다.
        insertMedication(repeatDays = "TUE,THU")

        repository.refresh(me)

        assertTrue(todayDoses().isEmpty())
    }

    @Test
    fun `종료일이 지난 약은 종료일까지만 기록을 만든다`() = runBlocking<Unit> {
        insertMedication(startDate = today.minusDays(4), endDate = today.minusDays(2))

        repository.refresh(me)

        assertEquals(
            listOf("2026-11-12", "2026-11-13", "2026-11-14"),
            doseRecordDao.records.value.map { it.date }
        )
    }

    @Test
    fun `삭제된 약의 기록은 새로 만들지 않는다`() = runBlocking<Unit> {
        val medicationId = insertMedication()
        medicationDao.softDelete(medicationId, deletedAt = 1)

        repository.refresh(me)

        assertTrue(doseRecordDao.records.value.isEmpty())
    }

    // ---------- 조회 ----------

    @Test
    fun `기록은 약 이름과 복용량이 붙어서 시각 순으로 나온다`() = runBlocking<Unit> {
        insertMedication(name = "비타민", time = "20:00")
        insertMedication(name = "타이레놀", time = "08:00")
        repository.refresh(me)

        val doses = todayDoses()

        assertEquals(listOf("타이레놀", "비타민"), doses.map { it.medicationName })
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), doses.map { it.time })
        assertEquals("1정", doses.first().dosage)
        assertEquals(today, doses.first().date)
        assertEquals(DoseStatus.PENDING, doses.first().status)
    }

    @Test
    fun `다른 사람의 기록은 보이지 않는다`() = runBlocking<Unit> {
        insertMedication(userId = someoneElse)
        repository.refresh(someoneElse)

        assertTrue(todayDoses(userId = me).isEmpty())
        assertEquals(1, todayDoses(userId = someoneElse).size)
    }

    @Test
    fun `약을 삭제해도 이미 있는 기록은 이력에 남는다`() = runBlocking<Unit> {
        val medicationId = insertMedication()
        repository.refresh(me)

        scheduleDao.softDeleteByMedication(medicationId, deletedAt = 1)
        medicationDao.softDelete(medicationId, deletedAt = 1)

        assertEquals(listOf("타이레놀"), todayDoses().map { it.medicationName })
    }

    // ---------- 상태 변경 ----------

    @Test
    fun `복용 완료로 바꾸면 상태와 누른 시각이 기록된다`() = runBlocking<Unit> {
        insertMedication()
        repository.refresh(me)
        val recordId = todayDoses().single().id

        val changed = repository.setStatus(me, recordId, DoseStatus.TAKEN)

        assertTrue(changed)
        val dose = todayDoses().single()
        assertEquals(DoseStatus.TAKEN, dose.status)
        assertEquals(clock.millis(), dose.actedAt)
    }

    @Test
    fun `복용 완료와 건너뜀 사이를 바꿀 수 있다`() = runBlocking<Unit> {
        insertMedication()
        repository.refresh(me)
        val recordId = todayDoses().single().id

        repository.setStatus(me, recordId, DoseStatus.TAKEN)
        repository.setStatus(me, recordId, DoseStatus.SKIPPED)

        assertEquals(DoseStatus.SKIPPED, todayDoses().single().status)
    }

    @Test
    fun `되돌리면 응답 전 상태가 되고 누른 시각이 지워진다`() = runBlocking<Unit> {
        insertMedication()
        repository.refresh(me)
        val recordId = todayDoses().single().id
        repository.setStatus(me, recordId, DoseStatus.TAKEN)

        repository.setStatus(me, recordId, DoseStatus.PENDING)

        val dose = todayDoses().single()
        assertEquals(DoseStatus.PENDING, dose.status)
        assertNull(dose.actedAt)
    }

    @Test
    fun `지난 날의 기록도 나중에 복용 완료로 바꿀 수 있다`() = runBlocking<Unit> {
        insertMedication(startDate = today.minusDays(1))
        repository.refresh(me)
        val yesterday = today.minusDays(1)
        val recordId = repository.observeDoses(me, yesterday, yesterday).first().single().id

        repository.setStatus(me, recordId, DoseStatus.TAKEN)

        assertEquals(DoseStatus.TAKEN, repository.observeDoses(me, yesterday, yesterday).first().single().status)
    }

    @Test
    fun `다른 사람의 기록은 바꿀 수 없다`() = runBlocking<Unit> {
        insertMedication(userId = someoneElse)
        repository.refresh(someoneElse)
        val recordId = todayDoses(userId = someoneElse).single().id

        val changed = repository.setStatus(me, recordId, DoseStatus.TAKEN)

        assertFalse(changed)
        assertEquals(DoseStatus.PENDING, todayDoses(userId = someoneElse).single().status)
    }

    @Test
    fun `없는 기록을 바꾸려 하면 false를 돌려준다`() = runBlocking<Unit> {
        assertFalse(repository.setStatus(me, recordId = 999, status = DoseStatus.TAKEN))
    }
}
