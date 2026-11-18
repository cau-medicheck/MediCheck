package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecord
import com.cau.medicheck.data.local.DoseStatusEvent
import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.data.local.User
import com.cau.medicheck.domain.backup.FIXTURE_EMAIL
import com.cau.medicheck.domain.backup.validBackup
import com.cau.medicheck.domain.backup.validRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * BackupRepository 단위 테스트: 내보내기·검사·복원. 시계는 2026-11-17(화) 10:00(UTC)에 멈춰 둔다.
 * 가짜 DAO는 CASCADE를 흉내 내지 않으므로, 복원 뒤 옛 줄이 남지 않는지로 "명시적 삭제 순서"가 확인된다.
 */
class BackupRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC)

    private lateinit var userDao: FakeUserDao
    private lateinit var medicationDao: FakeMedicationDao
    private lateinit var scheduleDao: FakeScheduleDao
    private lateinit var doseRecordDao: FakeDoseRecordDao
    private lateinit var eventDao: FakeDoseStatusEventDao
    private lateinit var repository: BackupRepository
    private var changedCount = 0
    private var me = 0L
    private var other = 0L

    @Before
    fun setUp() = runBlocking<Unit> {
        userDao = FakeUserDao()
        medicationDao = FakeMedicationDao()
        scheduleDao = FakeScheduleDao(medicationDao)
        doseRecordDao = FakeDoseRecordDao(medicationDao, scheduleDao)
        eventDao = FakeDoseStatusEventDao()
        changedCount = 0
        repository = BackupRepository(
            userDao, medicationDao, scheduleDao, doseRecordDao, eventDao, FakeTransactionRunner(),
            generator = DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, eventDao, clock),
            clock = clock,
            onChanged = { changedCount++ }
        )
        // 다른 사람을 먼저 넣어 둔다 → 내 줄의 id가 파일의 id(1)와 다르게 매겨진다.
        other = userDao.insert(User(email = "other@mail.com", passwordHash = "x", displayName = "다른 사람", createdAt = 0))
        addMedicationWithRecord(other, "남의 약", date = "2026-11-17")
        me = userDao.insert(User(email = FIXTURE_EMAIL, passwordHash = "x", displayName = "전수환", createdAt = 0))
    }

    /** 약 하나 + 매일 08:00 규칙 + 기록 한 줄을 넣는다. @return 약 id */
    private suspend fun addMedicationWithRecord(
        userId: Long, name: String, date: String, deletedAt: Long? = null
    ): Long {
        val medId = medicationDao.insert(
            Medication(
                userId = userId, name = name, dosage = "1정", startDate = "2026-11-17", endDate = null,
                memo = null, createdAt = 1, deletedAt = deletedAt
            )
        )
        val scheduleId = scheduleDao.insert(
            Schedule(
                medicationId = medId, time = "08:00", repeatDays = "MON,TUE,WED,THU,FRI,SAT,SUN",
                createdAt = 1, deletedAt = deletedAt
            )
        )
        doseRecordDao.insert(
            DoseRecord(
                scheduleId = scheduleId, date = date, scheduledTime = "08:00",
                medicationName = name, dosage = "1정", status = "PENDING", createdAt = 1
            )
        )
        return medId
    }

    private fun myMedications() = medicationDao.medications.value.filter { it.userId == me }
    private fun medicationsOf(userId: Long) = medicationDao.medications.value.filter { it.userId == userId }

    // ---------- 내보내기 ----------

    @Test
    fun `내보내기에는 내 것만 들어가고 삭제한 약과 그 기록도 들어간다`() = runBlocking<Unit> {
        addMedicationWithRecord(me, "쓰는 약", date = "2026-11-17")
        addMedicationWithRecord(me, "지운 약", date = "2026-11-16", deletedAt = 99)

        val data = repository.export(me)

        assertEquals(FIXTURE_EMAIL, data.accountEmail)
        assertEquals(1, data.version)
        assertEquals(listOf("쓰는 약", "지운 약"), data.medications.map { it.name })
        assertEquals(2, data.schedules.size)
        assertEquals(setOf("쓰는 약", "지운 약"), data.doseRecords.map { it.medicationName }.toSet())
        assertEquals("2026-11-17T10:00Z", data.exportedAt)
    }

    @Test
    fun `내보내기 전에 오늘까지의 빈 날 기록을 채운다`() = runBlocking<Unit> {
        // 약만 있고 기록이 없는 상태 (앱을 열지 않은 날)
        val medId = medicationDao.insert(
            Medication(
                userId = me, name = "새 약", dosage = "1정", startDate = "2026-11-17", endDate = null,
                memo = null, createdAt = 1
            )
        )
        scheduleDao.insert(Schedule(medicationId = medId, time = "08:00", repeatDays = "TUE", createdAt = 1))

        val data = repository.export(me)

        assertEquals(listOf("2026-11-17"), data.doseRecords.map { it.date })
    }

    @Test
    fun `개수는 사용 중인 약 수와 삭제한 약의 기록까지 센 기록 수다`() = runBlocking<Unit> {
        addMedicationWithRecord(me, "쓰는 약", date = "2026-11-17")
        addMedicationWithRecord(me, "지운 약", date = "2026-11-16", deletedAt = 99)

        assertEquals(BackupCounts(medications = 1, records = 2), repository.observeCounts(me).first())
    }

    // ---------- 검사 ----------

    @Test
    fun `같은 계정의 올바른 백업은 복원할 수 있다고 본다`() = runBlocking<Unit> {
        assertTrue(repository.inspect(me, validBackup) is RestoreCheck.Ready)
        // 이메일은 대소문자·앞뒤 공백을 다듬어 비교한다
        assertTrue(repository.inspect(me, validBackup.copy(accountEmail = " Suhwan@CAU.ac.kr ")) is RestoreCheck.Ready)
    }

    @Test
    fun `다른 계정의 백업이나 주인이 적혀 있지 않은 백업은 복원하지 않는다`() = runBlocking<Unit> {
        assertEquals(RestoreCheck.OtherAccount, repository.inspect(other, validBackup))
        assertEquals(RestoreCheck.OtherAccount, repository.inspect(me, validBackup.copy(accountEmail = null)))
    }

    @Test
    fun `형식이 틀린 백업은 검사에서 걸리고 DB는 그대로다`() = runBlocking<Unit> {
        addMedicationWithRecord(me, "쓰는 약", date = "2026-11-17")
        val bad = validBackup.copy(doseRecords = listOf(validRecord.copy(status = "DONE")))

        assertEquals(RestoreCheck.Invalid, repository.inspect(me, bad))
        assertEquals(listOf("쓰는 약"), myMedications().map { it.name })
    }

    // ---------- 복원 ----------

    @Test
    fun `복원은 내 데이터만 파일 내용으로 바꾸고 다른 계정은 그대로 둔다`() = runBlocking<Unit> {
        addMedicationWithRecord(me, "옛 약", date = "2026-11-17")
        eventDao.insert(
            DoseStatusEvent(
                userId = me, doseRecordId = null, scheduleId = 1, doseDate = "2026-11-17", scheduledTime = "08:00",
                medicationName = "옛 약", fromStatus = "PENDING", toStatus = "TAKEN",
                fromActedAt = null, toActedAt = 1, changedAt = 1
            )
        )
        val ready = repository.inspect(me, validBackup) as RestoreCheck.Ready

        repository.restore(me, ready.data)

        assertEquals(listOf("비타민 D"), myMedications().map { it.name })
        assertEquals(listOf("남의 약"), medicationsOf(other).map { it.name })
        assertEquals(1, doseRecordDao.findAllEntitiesForUser(other).size)
        // 옛 이력은 지워지고 파일의 이력 한 줄만 남는다
        assertEquals(listOf("비타민 D"), eventDao.events.value.filter { it.userId == me }.map { it.medicationName })
        assertEquals(1, changedCount)
    }

    @Test
    fun `복원하면 번호가 새로 매겨지고 약 규칙 기록 이력이 서로 맞게 이어진다`() = runBlocking<Unit> {
        val ready = repository.inspect(me, validBackup) as RestoreCheck.Ready

        repository.restore(me, ready.data)

        val medication = myMedications().single()
        val schedule = scheduleDao.findAllForUser(me).single()
        val record = doseRecordDao.findAllEntitiesForUser(me).single { it.date == "2026-11-16" }
        val event = eventDao.events.value.single { it.userId == me }
        // 파일의 id는 모두 1이지만, 이 폰에는 다른 사람의 줄이 먼저 있어 새 id는 1이 아니다.
        assertTrue(medication.id != 1L && schedule.id != 1L && record.id != 1L)
        assertEquals(medication.id, schedule.medicationId)
        assertEquals(schedule.id, record.scheduleId)
        assertEquals(record.id, event.doseRecordId)
        assertEquals(schedule.id, event.scheduleId)
        // 값은 파일 그대로
        assertEquals("중앙대병원", medication.hospital)
        assertEquals("TAKEN", record.status)
        assertEquals(1_794_783_780_000, record.actedAt)
    }

    @Test
    fun `복원 뒤 백업 다음 날부터 오늘까지의 빈 날을 채운다`() = runBlocking<Unit> {
        // 백업의 마지막 기록은 11/16, 오늘은 11/17 → 11/17 회차가 미기록으로 생긴다
        val ready = repository.inspect(me, validBackup) as RestoreCheck.Ready

        repository.restore(me, ready.data)

        val records = doseRecordDao.findAllEntitiesForUser(me).sortedBy { it.date }
        assertEquals(listOf("2026-11-16", "2026-11-17"), records.map { it.date })
        assertEquals("PENDING", records.last().status)
    }

    @Test
    fun `복원 뒤 알람 다시 걸기에서 오류가 나도 복원은 성공으로 끝난다`() = runBlocking<Unit> {
        val failingAfter = BackupRepository(
            userDao, medicationDao, scheduleDao, doseRecordDao, eventDao, FakeTransactionRunner(),
            generator = DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, eventDao, clock),
            clock = clock,
            onChanged = { error("알람을 걸지 못함") }
        )
        val ready = failingAfter.inspect(me, validBackup) as RestoreCheck.Ready

        failingAfter.restore(me, ready.data)   // 오류가 밖으로 나오지 않는다

        assertEquals(listOf("비타민 D"), myMedications().map { it.name })
    }

    @Test
    fun `기록이 지워진 이력은 연결 없이 복원한다`() = runBlocking<Unit> {
        val detached = validBackup.copy(
            statusEvents = validBackup.statusEvents.map { it.copy(doseRecordId = null) }
        )
        val ready = repository.inspect(me, detached) as RestoreCheck.Ready

        repository.restore(me, ready.data)

        assertNull(eventDao.events.value.single { it.userId == me }.doseRecordId)
    }

    @Test
    fun `내보낸 것을 그대로 복원하면 내용이 같다`() = runBlocking<Unit> {
        addMedicationWithRecord(me, "쓰는 약", date = "2026-11-17")
        addMedicationWithRecord(me, "지운 약", date = "2026-11-16", deletedAt = 99)
        val before = repository.export(me)

        repository.restore(me, (repository.inspect(me, before) as RestoreCheck.Ready).data)
        val after = repository.export(me)

        // id는 새로 매겨지므로 id를 뺀 내용으로 비교한다
        assertEquals(before.medications.map { it.copy(id = 0) }, after.medications.map { it.copy(id = 0) })
        assertEquals(
            before.doseRecords.map { it.copy(id = 0, scheduleId = 0) },
            after.doseRecords.map { it.copy(id = 0, scheduleId = 0) }
        )
    }
}
