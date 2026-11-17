package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.domain.DoseKey
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
    private lateinit var eventDao: FakeDoseStatusEventDao
    private lateinit var generator: DoseRecordGenerator
    private lateinit var repository: DoseRepository

    @Before
    fun setUp() {
        medicationDao = FakeMedicationDao()
        scheduleDao = FakeScheduleDao(medicationDao)
        doseRecordDao = FakeDoseRecordDao(medicationDao, scheduleDao)
        eventDao = FakeDoseStatusEventDao()
        generator = DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, eventDao, clock)
        repository = DoseRepository(doseRecordDao, eventDao, generator, FakeTransactionRunner(), clock)
    }

    /** 약 하나와 규칙 하나를 표에 직접 넣는다. (기록은 만들지 않는다) */
    private suspend fun insertMedication(
        userId: Long = me,
        name: String = "타이레놀",
        startDate: LocalDate = today,
        endDate: LocalDate? = null,
        time: String = "08:00",
        repeatDays: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
        effectiveFrom: LocalDate? = null,
        pausedFrom: LocalDate? = null
    ): Long {
        val medicationId = medicationDao.insert(
            Medication(
                userId = userId, name = name, dosage = "1정",
                startDate = startDate.toString(), endDate = endDate?.toString(),
                memo = "식후", createdAt = 0, pausedFrom = pausedFrom?.toString()
            )
        )
        scheduleDao.insert(
            Schedule(
                medicationId = medicationId, time = time, repeatDays = repeatDays, createdAt = 0,
                effectiveFrom = effectiveFrom?.toString()
            )
        )
        return medicationId
    }

    /** 첫 번째 규칙(id 1)의 오늘 회차 열쇠 */
    private val todayKey get() = DoseKey(scheduleId = 1, date = today)

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
    fun `기록을 만들 때 약 이름 복용량 메모 예정 시각을 기록에 복사해 둔다`() = runBlocking<Unit> {
        insertMedication(name = "비타민", time = "20:00")

        repository.refresh(me)

        val record = doseRecordDao.records.value.single()
        assertEquals("비타민", record.medicationName)
        assertEquals("1정", record.dosage)
        assertEquals("식후", record.memo)
        assertEquals("20:00", record.scheduledTime)
    }

    @Test
    fun `약 이름을 나중에 바꿔도 이미 만든 기록은 그때 이름으로 나온다`() = runBlocking<Unit> {
        val medicationId = insertMedication(name = "타이레놀")
        repository.refresh(me)

        val medication = medicationDao.findById(medicationId)!!
        medicationDao.update(medication.copy(name = "타이레놀 500", dosage = "2정"))

        val dose = todayDoses().single()
        assertEquals("타이레놀", dose.medicationName)
        assertEquals("1정", dose.dosage)
        assertEquals("식후", dose.memo)
        assertEquals(1L, dose.scheduleId)
    }

    @Test
    fun `적용 시작일 전의 날짜는 기록을 만들지 않는다`() = runBlocking<Unit> {
        insertMedication(startDate = today.minusDays(5), effectiveFrom = today.minusDays(1))

        repository.refresh(me)

        assertEquals(listOf("2026-11-15", "2026-11-16"), doseRecordDao.records.value.map { it.date })
    }

    @Test
    fun `일시중지한 날부터는 기록을 만들지 않는다`() = runBlocking<Unit> {
        insertMedication(startDate = today.minusDays(3), pausedFrom = today.minusDays(1))

        repository.refresh(me)

        assertEquals(listOf("2026-11-13", "2026-11-14"), doseRecordDao.records.value.map { it.date })
    }

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

    // ---------- 상태 변경 이력 · 회차 열쇠 · 실행취소 ----------

    @Test
    fun `상태를 바꾸면 이력이 한 줄 남는다`() = runBlocking<Unit> {
        insertMedication()
        repository.refresh(me)

        val result = repository.changeStatus(me, todayKey, DoseStatus.TAKEN)

        assertEquals(StatusChangeResult.CHANGED, result)
        val event = eventDao.events.value.single()
        val record = doseRecordDao.records.value.single()
        assertEquals(me, event.userId)
        assertEquals(record.id, event.doseRecordId)
        assertEquals(1L, event.scheduleId)
        assertEquals("2026-11-16", event.doseDate)
        assertEquals("08:00", event.scheduledTime)
        assertEquals("타이레놀", event.medicationName)
        assertEquals("PENDING", event.fromStatus)
        assertEquals("TAKEN", event.toStatus)
        assertNull(event.fromActedAt)
        assertEquals(clock.millis(), event.toActedAt)
        assertFalse(event.isUndo)
    }

    @Test
    fun `같은 상태로 바꾸면 아무것도 쓰지 않는다`() = runBlocking<Unit> {
        insertMedication()
        repository.refresh(me)
        repository.changeStatus(me, todayKey, DoseStatus.TAKEN)

        val result = repository.changeStatus(me, todayKey, DoseStatus.TAKEN)

        assertEquals(StatusChangeResult.UNCHANGED, result)
        assertEquals(1, eventDao.events.value.size)
    }

    @Test
    fun `기록 번호로 바꿔도 이력이 남는다`() = runBlocking<Unit> {
        insertMedication()
        repository.refresh(me)
        val recordId = todayDoses().single().id

        repository.setStatus(me, recordId, DoseStatus.SKIPPED)
        repository.setStatus(me, recordId, DoseStatus.PENDING)

        assertEquals(listOf("PENDING" to "SKIPPED", "SKIPPED" to "PENDING"),
            eventDao.events.value.map { it.fromStatus to it.toStatus })
    }

    @Test
    fun `기록 줄이 없는 미래 회차는 바꿀 수 없다`() = runBlocking<Unit> {
        insertMedication()
        repository.refresh(me)

        val result = repository.changeStatus(me, DoseKey(1, today.plusDays(1)), DoseStatus.TAKEN)

        assertEquals(StatusChangeResult.FUTURE, result)
        assertTrue(eventDao.events.value.isEmpty())
    }

    @Test
    fun `다른 사람의 회차를 열쇠로 바꾸려 하면 아무것도 바뀌지 않는다`() = runBlocking<Unit> {
        insertMedication(userId = someoneElse)
        repository.refresh(someoneElse)

        val result = repository.changeStatus(me, todayKey, DoseStatus.TAKEN)

        assertEquals(StatusChangeResult.NOT_FOUND, result)
        assertEquals("PENDING", doseRecordDao.records.value.single().status)
        assertTrue(eventDao.events.value.isEmpty())
        // DAO도 주인이 아니면 0줄만 바꾼다
        assertEquals(0, doseRecordDao.updateStatus(me, doseRecordDao.records.value.single().id, "TAKEN", 1L))
    }

    @Test
    fun `기록이 아직 없는 오늘 회차는 먼저 보충한 뒤 바꾼다`() = runBlocking<Unit> {
        insertMedication()   // refresh를 부르지 않아 기록이 없다

        val result = repository.changeStatus(me, todayKey, DoseStatus.TAKEN)

        assertEquals(StatusChangeResult.CHANGED, result)
        assertEquals("TAKEN", doseRecordDao.records.value.single().status)
    }

    @Test
    fun `실행취소하면 바로 전 상태와 시각으로 돌아가고 실행취소 이력이 남는다`() = runBlocking<Unit> {
        insertMedication()
        repository.refresh(me)
        repository.changeStatus(me, todayKey, DoseStatus.TAKEN)

        val result = repository.undoLast(me, todayKey)

        assertEquals(UndoResult.UNDONE, result)
        val record = doseRecordDao.records.value.single()
        assertEquals("PENDING", record.status)
        assertNull(record.actedAt)
        val undo = eventDao.events.value.last()
        assertTrue(undo.isUndo)
        assertEquals("TAKEN" to "PENDING", undo.fromStatus to undo.toStatus)
    }

    @Test
    fun `실행취소의 실행취소는 하지 않는다`() = runBlocking<Unit> {
        insertMedication()
        repository.refresh(me)
        repository.changeStatus(me, todayKey, DoseStatus.TAKEN)
        repository.undoLast(me, todayKey)

        assertEquals(UndoResult.NOTHING, repository.undoLast(me, todayKey))
        assertEquals("PENDING", doseRecordDao.records.value.single().status)
    }

    @Test
    fun `이력 없이 정해진 상태는 실행취소하지 않는다`() = runBlocking<Unit> {
        insertMedication()
        repository.refresh(me)
        // 샘플 데이터처럼 이력 없이 상태가 정해진 기록
        doseRecordDao.updateStatus(me, doseRecordDao.records.value.single().id, "SKIPPED", null)

        assertEquals(UndoResult.NOTHING, repository.undoLast(me, todayKey))
        assertEquals("SKIPPED", doseRecordDao.records.value.single().status)
    }

    @Test
    fun `실행취소는 그 회차만 되돌린다`() = runBlocking<Unit> {
        insertMedication(time = "08:00")
        insertMedication(time = "20:00")
        repository.refresh(me)
        repository.changeStatus(me, DoseKey(1, today), DoseStatus.TAKEN)
        repository.changeStatus(me, DoseKey(2, today), DoseStatus.SKIPPED)

        repository.undoLast(me, DoseKey(1, today))

        assertEquals(listOf("PENDING", "SKIPPED"), doseRecordDao.records.value.sortedBy { it.scheduleId }.map { it.status })
    }

    @Test
    fun `삭제한 약의 지난 기록도 상태를 고칠 수 있다`() = runBlocking<Unit> {
        val medicationId = insertMedication(startDate = today.minusDays(1))
        repository.refresh(me)
        scheduleDao.softDeleteByMedication(medicationId, deletedAt = 1)
        medicationDao.softDelete(medicationId, deletedAt = 1)

        val result = repository.changeStatus(me, DoseKey(1, today.minusDays(1)), DoseStatus.TAKEN)

        assertEquals(StatusChangeResult.CHANGED, result)
    }
}
