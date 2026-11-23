package com.cau.medicheck.data.repository

import com.cau.medicheck.domain.ApplyScope
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.MedicationDraft
import com.cau.medicheck.domain.MedicationError
import com.cau.medicheck.domain.RepeatDays
import com.cau.medicheck.domain.ScheduleDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * MedicationRepository 단위 테스트.
 * 진짜 DB 대신 메모리에만 저장하는 가짜 DAO(Fakes.kt)를 끼워서, 등록·수정·삭제의 순서와 결과를 확인한다.
 *
 * 시계는 2026-11-16(월) 10:00에 멈춰 둔다. "오늘"과 "지금"이 테스트마다 달라지지 않게 하기 위해서다.
 */
class MedicationRepositoryTest {

    private val me = 1L
    private val someoneElse = 2L

    private val today = LocalDate.of(2026, 11, 16)
    private val clock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC)

    private lateinit var medicationDao: FakeMedicationDao
    private lateinit var scheduleDao: FakeScheduleDao
    private lateinit var doseRecordDao: FakeDoseRecordDao
    private lateinit var eventDao: FakeDoseStatusEventDao
    private lateinit var repository: MedicationRepository

    private val morning = ScheduleDraft(time = LocalTime.of(8, 0), days = RepeatDays.EVERY_DAY)
    private val evening = ScheduleDraft(time = LocalTime.of(20, 0), days = setOf(MONDAY, WEDNESDAY, FRIDAY))

    private fun draft(
        name: String = "타이레놀",
        schedules: List<ScheduleDraft> = listOf(morning, evening),
        startDate: LocalDate = today,
        endDate: LocalDate? = null,
        memo: String = "식후 30분"
    ) = MedicationDraft(
        name = name,
        dosage = "1정",
        startDate = startDate,
        endDate = endDate,
        memo = memo,
        schedules = schedules
    )

    @Before
    fun setUp() {
        medicationDao = FakeMedicationDao()
        scheduleDao = FakeScheduleDao(medicationDao)
        doseRecordDao = FakeDoseRecordDao(medicationDao, scheduleDao)
        eventDao = FakeDoseStatusEventDao()
        repository = MedicationRepository(
            medicationDao = medicationDao,
            scheduleDao = scheduleDao,
            transaction = FakeTransactionRunner(),
            doseRecordGenerator = DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, eventDao, clock),
            now = { 5_000L },
            clock = clock
        )
    }

    /** 약을 하나 등록하고 그 id를 돌려준다. */
    private suspend fun addTylenol(userId: Long = me): Long =
        (repository.addMedication(userId, draft()) as MedicationResult.Success).medicationId

    @Test
    fun `시각을 바꾸면 그 줄의 미리 건너뛴 기록은 지운다`() = runBlocking<Unit> {
        val id = addTylenol()
        val (s08, s20) = scheduleDao.schedules.value.sortedBy { it.time }
        val generator = DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, eventDao, clock)
        generator.createFutureRecord(me, s08.id, today.plusDays(2))
        generator.createFutureRecord(me, s20.id, today.plusDays(2))   // 11/18 수요일 = 20:00 줄의 회차일

        repository.updateMedication(
            me, id, draft(schedules = listOf(morning.copy(id = s08.id, time = LocalTime.of(9, 0)), evening.copy(id = s20.id)))
        )

        val future = doseRecordDao.records.value.filter { it.date > today.toString() }
        assertEquals(listOf(s20.id), future.map { it.scheduleId })   // 시각이 바뀐 08:00 줄의 것만 지워짐
    }

    @Test
    fun `약을 지우면 미리 건너뛴 기록도 지운다`() = runBlocking<Unit> {
        val id = addTylenol()
        val s08 = scheduleDao.schedules.value.minBy { it.time }
        DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, eventDao, clock).createFutureRecord(me, s08.id, today.plusDays(1))

        repository.deleteMedication(me, id)

        assertTrue(doseRecordDao.records.value.none { it.date > today.toString() })
    }

    // ---------- 등록 ----------

    @Test
    fun `등록하면 복용 조건이 규칙 줄과 기록 스냅샷에 저장된다`() = runBlocking<Unit> {
        repository.addMedication(
            me,
            draft(schedules = listOf(morning.copy(condition = DoseCondition.AFTER_MEAL), evening.copy(condition = DoseCondition.BEDTIME)))
        )

        assertEquals(listOf("AFTER_MEAL", "BEDTIME"), scheduleDao.schedules.value.map { it.doseCondition })
        val today08 = doseRecordDao.records.value.single { it.date == today.toString() && it.scheduledTime == "08:00" }
        assertEquals("AFTER_MEAL", today08.doseCondition)
    }

    @Test
    fun `조건만 바꾸면 적용 시작일은 그대로이고 오늘 남은 기록의 조건만 바뀐다`() = runBlocking<Unit> {
        val id = addTylenol()
        val (s08, s20) = scheduleDao.schedules.value.sortedBy { it.time }

        repository.updateMedication(
            me, id,
            draft(schedules = listOf(morning.copy(id = s08.id, condition = DoseCondition.AFTER_MEAL), evening.copy(id = s20.id, condition = DoseCondition.BEDTIME)))
        )

        val saved = scheduleDao.schedules.value.sortedBy { it.time }
        assertEquals(listOf("AFTER_MEAL", "BEDTIME"), saved.map { it.doseCondition })
        assertEquals(listOf<String?>(null, null), saved.map { it.effectiveFrom })   // 회차가 생기는 날은 그대로
        val todays = doseRecordDao.records.value.filter { it.date == today.toString() }.associateBy { it.scheduledTime }
        assertEquals("NONE", todays.getValue("08:00").doseCondition)      // 이미 시각이 지난 기록은 그대로
        assertEquals("BEDTIME", todays.getValue("20:00").doseCondition)   // 오늘 남은 기록은 새 조건
    }

    @Test
    fun `내일부터 적용으로 조건을 바꾸면 오늘 기록은 그대로다`() = runBlocking<Unit> {
        val id = addTylenol()
        val (s08, s20) = scheduleDao.schedules.value.sortedBy { it.time }

        repository.updateMedication(
            me, id,
            draft(schedules = listOf(morning.copy(id = s08.id), evening.copy(id = s20.id, condition = DoseCondition.BEDTIME))),
            scope = ApplyScope.FROM_TOMORROW
        )

        assertEquals("BEDTIME", scheduleDao.schedules.value.single { it.id == s20.id }.doseCondition)
        val today20 = doseRecordDao.records.value.single { it.date == today.toString() && it.scheduledTime == "20:00" }
        assertEquals("NONE", today20.doseCondition)
    }


    @Test
    fun `약을 등록하면 약 한 줄과 복용 시간 줄들이 저장된다`() = runBlocking<Unit> {
        val id = addTylenol()

        val saved = medicationDao.medications.value.single()
        assertEquals(id, saved.id)
        assertEquals(me, saved.userId)
        assertEquals("타이레놀", saved.name)
        assertEquals("2026-11-16", saved.startDate)
        assertNull(saved.endDate)
        assertEquals(5_000L, saved.createdAt)

        val schedules = scheduleDao.schedules.value
        assertEquals(listOf("08:00", "20:00"), schedules.map { it.time })
        assertEquals("MON,TUE,WED,THU,FRI,SAT,SUN", schedules[0].repeatDays)
        assertEquals("MON,WED,FRI", schedules[1].repeatDays)
        assertTrue(schedules.all { it.medicationId == id })
    }

    @Test
    fun `이름과 복용량의 앞뒤 공백은 지우고 빈 메모는 null로 저장한다`() = runBlocking<Unit> {
        repository.addMedication(me, draft(name = "  타이레놀  ", memo = "   "))

        val saved = medicationDao.medications.value.single()
        assertEquals("타이레놀", saved.name)
        assertNull(saved.memo)
    }

    @Test
    fun `규칙에 맞지 않는 입력은 저장하지 않는다`() = runBlocking<Unit> {
        val result = repository.addMedication(me, draft(schedules = emptyList()))

        assertEquals(MedicationResult.Failure(MedicationError.NAME_DOSAGE_TIME_INVALID), result)
        assertTrue(medicationDao.medications.value.isEmpty())
    }

    // ---------- 조회 ----------

    @Test
    fun `약을 읽으면 날짜와 시간과 요일이 타입으로 바뀌어 나온다`() = runBlocking<Unit> {
        val id = addTylenol()

        val detail = repository.getMedication(me, id)

        assertNotNull(detail)
        assertEquals(LocalDate.of(2026, 11, 16), detail!!.startDate)
        assertEquals("식후 30분", detail.memo)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), detail.schedules.map { it.time })
        assertEquals(setOf(MONDAY, WEDNESDAY, FRIDAY), detail.schedules[1].days)
    }

    @Test
    fun `목록에는 내 약만 나온다`() = runBlocking<Unit> {
        addTylenol(userId = me)
        repository.addMedication(someoneElse, draft(name = "남의 약"))

        val mine = repository.observeMedications(me).first()

        assertEquals(listOf("타이레놀"), mine.map { it.name })
        assertEquals(2, mine.single().schedules.size)
    }

    @Test
    fun `다른 사람의 약은 읽을 수 없다`() = runBlocking<Unit> {
        val id = addTylenol(userId = someoneElse)

        assertNull(repository.getMedication(me, id))
    }

    // ---------- 수정 ----------

    @Test
    fun `약 정보를 수정하면 같은 줄의 값이 바뀐다`() = runBlocking<Unit> {
        val id = addTylenol()
        val saved = repository.getMedication(me, id)!!

        val result = repository.updateMedication(
            me, id,
            draft(
                name = "타이레놀 500",
                endDate = LocalDate.of(2026, 12, 1),
                schedules = saved.schedules.map { ScheduleDraft(it.id, it.time, it.days) }
            )
        )

        assertEquals(MedicationResult.Success(id), result)
        val updated = medicationDao.medications.value.single()
        assertEquals("타이레놀 500", updated.name)
        assertEquals("2026-12-01", updated.endDate)
        assertEquals(5_000L, updated.createdAt)
    }

    @Test
    fun `복용 시간은 남긴 것은 수정하고 새것은 추가하고 뺀 것은 soft delete 한다`() = runBlocking<Unit> {
        val id = addTylenol()
        val (savedMorning, savedEvening) = repository.getMedication(me, id)!!.schedules

        repository.updateMedication(
            me, id,
            draft(
                schedules = listOf(
                    // 아침 규칙은 남기되 시각을 09:00으로 바꾼다
                    ScheduleDraft(savedMorning.id, LocalTime.of(9, 0), savedMorning.days),
                    // 저녁 규칙은 뺀다 (목록에 넣지 않음)
                    // 점심 규칙을 새로 추가한다 (id 없음)
                    ScheduleDraft(null, LocalTime.of(13, 0), setOf(MONDAY))
                )
            )
        )

        val all = scheduleDao.schedules.value
        assertEquals("09:00", all.single { it.id == savedMorning.id }.time)
        assertEquals(5_000L, all.single { it.id == savedEvening.id }.deletedAt)
        assertEquals("MON", all.single { it.time == "13:00" }.repeatDays)

        val active = repository.getMedication(me, id)!!.schedules
        assertEquals(listOf(LocalTime.of(9, 0), LocalTime.of(13, 0)), active.map { it.time })
    }

    @Test
    fun `다른 사람의 약은 수정할 수 없다`() = runBlocking<Unit> {
        val id = addTylenol(userId = someoneElse)

        val result = repository.updateMedication(me, id, draft(name = "바꿔치기"))

        assertEquals(MedicationResult.Failure(MedicationError.NOT_FOUND), result)
        assertEquals("타이레놀", medicationDao.medications.value.single().name)
    }

    // ---------- 삭제 ----------

    @Test
    fun `약을 삭제하면 줄은 남고 삭제 시각만 기록된다`() = runBlocking<Unit> {
        val id = addTylenol()

        val result = repository.deleteMedication(me, id)

        assertEquals(MedicationResult.Success(id), result)
        assertEquals(5_000L, medicationDao.medications.value.single().deletedAt)
        assertTrue(scheduleDao.schedules.value.all { it.deletedAt == 5_000L })
    }

    @Test
    fun `삭제한 약은 목록에도 나오지 않고 읽을 수도 없다`() = runBlocking<Unit> {
        val id = addTylenol()

        repository.deleteMedication(me, id)

        assertTrue(repository.observeMedications(me).first().isEmpty())
        assertNull(repository.getMedication(me, id))
    }

    @Test
    fun `다른 사람의 약은 삭제할 수 없다`() = runBlocking<Unit> {
        val id = addTylenol(userId = someoneElse)

        val result = repository.deleteMedication(me, id)

        assertEquals(MedicationResult.Failure(MedicationError.NOT_FOUND), result)
        assertNull(medicationDao.medications.value.single().deletedAt)
    }

    // ---------- 복용 기록과의 약속 (기록 생성·보충) ----------

    /** 오늘(월요일) 치 기록을 "시각 → 상태" 목록으로 본다. */
    private fun todayRecords() = doseRecordDao.records.value
        .filter { it.date == today.toString() }
        .sortedBy { it.scheduledTime }
        .map { it.scheduledTime to it.status }

    @Test
    fun `약을 등록하면 오늘 치 복용 기록이 바로 만들어진다`() = runBlocking<Unit> {
        addTylenol()   // 매일 08:00 + 월·수·금 20:00, 오늘은 월요일

        assertEquals(listOf("08:00" to "PENDING", "20:00" to "PENDING"), todayRecords())
    }

    @Test
    fun `수정하기 직전에 지난 날짜의 기록을 바뀌기 전 시각으로 먼저 채운다`() = runBlocking<Unit> {
        // 사흘 전(금요일)부터 먹기 시작한 약. 등록할 때 금·토·일·월 기록이 만들어진다.
        repository.addMedication(me, draft(startDate = today.minusDays(3), schedules = listOf(morning)))
        // 앱을 안 열어서 기록이 없던 상황을 만든다.
        doseRecordDao.records.value = emptyList()
        val id = medicationDao.medications.value.single().id
        val saved = repository.getMedication(me, id)!!.schedules.single()

        // 08:00 → 09:00으로 바꾼다. (지금은 10:00이라 둘 다 이미 지난 시각)
        repository.updateMedication(
            me, id,
            draft(
                startDate = today.minusDays(3),
                schedules = listOf(ScheduleDraft(saved.id, LocalTime.of(9, 0), saved.days))
            )
        )

        // 지난 날짜와 오늘의 기록은 바뀌기 전 시각(08:00)으로 남아 있어야 한다.
        val records = doseRecordDao.records.value.sortedBy { it.date }
        assertEquals(
            listOf("2026-11-13", "2026-11-14", "2026-11-15", "2026-11-16"),
            records.map { it.date }
        )
        assertTrue(records.all { it.scheduledTime == "08:00" })
    }

    @Test
    fun `시각을 바꾸면 아직 안 된 오늘 치 기록은 새 시각으로 다시 만들어진다`() = runBlocking<Unit> {
        val id = addTylenol()
        val (savedMorning, savedEvening) = repository.getMedication(me, id)!!.schedules

        // 저녁 20:00 → 21:00. 지금은 10:00이라 저녁 기록은 아직 시각 전이다.
        repository.updateMedication(
            me, id,
            draft(
                schedules = listOf(
                    ScheduleDraft(savedMorning.id, savedMorning.time, savedMorning.days),
                    ScheduleDraft(savedEvening.id, LocalTime.of(21, 0), savedEvening.days)
                )
            )
        )

        // 이미 지난 아침 기록(08:00)은 그대로, 저녁 기록은 21:00으로.
        assertEquals(listOf("08:00" to "PENDING", "21:00" to "PENDING"), todayRecords())
    }

    @Test
    fun `이미 응답한 오늘 치 기록은 시각을 바꿔도 그대로 남는다`() = runBlocking<Unit> {
        val id = addTylenol()
        val (savedMorning, savedEvening) = repository.getMedication(me, id)!!.schedules
        val eveningRecord = doseRecordDao.records.value.single { it.scheduledTime == "20:00" }
        doseRecordDao.updateStatus(me, eveningRecord.id, "TAKEN", 9_000L)   // 저녁 약을 미리 먹었다고 기록

        repository.updateMedication(
            me, id,
            draft(
                schedules = listOf(
                    ScheduleDraft(savedMorning.id, savedMorning.time, savedMorning.days),
                    ScheduleDraft(savedEvening.id, LocalTime.of(21, 0), savedEvening.days)
                )
            )
        )

        assertEquals(listOf("08:00" to "PENDING", "20:00" to "TAKEN"), todayRecords())
    }

    @Test
    fun `약을 삭제하면 아직 안 된 오늘 치 기록만 지워지고 나머지 기록은 남는다`() = runBlocking<Unit> {
        val id = addTylenol()

        repository.deleteMedication(me, id)

        // 08:00은 이미 지난 시각이라 남고(깜빡함으로 집계), 20:00은 아직이라 지워진다.
        assertEquals(listOf("08:00" to "PENDING"), todayRecords())
    }

    // ---------- 스냅샷 · 적용 시작일 · 오늘 치 다시 맞추기 ----------

    /** 저장된 규칙 그대로 폼에 넘긴 것처럼 ScheduleDraft 목록을 만든다. */
    private suspend fun savedDrafts(id: Long) =
        repository.getMedication(me, id)!!.schedules.map { ScheduleDraft(it.id, it.time, it.days) }

    @Test
    fun `약 이름을 바꿔도 지난 기록은 옛 이름이고 오늘 남은 기록은 같은 줄이 새 이름으로 바뀐다`() = runBlocking<Unit> {
        repository.addMedication(me, draft(startDate = today.minusDays(1)))
        val id = medicationDao.medications.value.single().id
        val eveningBefore = doseRecordDao.records.value.single { it.date == "2026-11-16" && it.scheduledTime == "20:00" }

        repository.updateMedication(me, id, draft(name = "타이레놀 500", startDate = today.minusDays(1), schedules = savedDrafts(id)))

        val records = doseRecordDao.records.value
        // 어제 기록과 오늘 이미 지난 08:00 기록은 옛 이름 그대로
        assertTrue(records.filter { it.date == "2026-11-15" }.all { it.medicationName == "타이레놀" })
        assertEquals("타이레놀", records.single { it.date == "2026-11-16" && it.scheduledTime == "08:00" }.medicationName)
        // 오늘 아직 시각 전인 20:00 기록은 같은 id 그대로 새 이름
        val eveningAfter = records.single { it.date == "2026-11-16" && it.scheduledTime == "20:00" }
        assertEquals(eveningBefore.id, eveningAfter.id)
        assertEquals("타이레놀 500", eveningAfter.medicationName)
    }

    @Test
    fun `시간 줄을 새로 더해도 지난 날짜의 기록은 생기지 않는다`() = runBlocking<Unit> {
        repository.addMedication(me, draft(startDate = today.minusDays(3), schedules = listOf(morning)))
        val id = medicationDao.medications.value.single().id

        repository.updateMedication(
            me, id,
            draft(startDate = today.minusDays(3), schedules = savedDrafts(id) + ScheduleDraft(null, LocalTime.of(21, 0), RepeatDays.EVERY_DAY))
        )

        val newSchedule = scheduleDao.schedules.value.single { it.time == "21:00" }
        assertEquals("2026-11-16", newSchedule.effectiveFrom)
        assertEquals(listOf("2026-11-16"), doseRecordDao.records.value.filter { it.scheduleId == newSchedule.id }.map { it.date })
    }

    @Test
    fun `이미 지난 시각으로 바꾼 줄은 내일부터 적용되어 오늘 기한 경과 회차가 생기지 않는다`() = runBlocking<Unit> {
        // 지금은 10:00. 저녁 20:00 줄을 09:00으로 바꾼다.
        val id = addTylenol()
        val (savedMorning, savedEvening) = repository.getMedication(me, id)!!.schedules

        repository.updateMedication(
            me, id,
            draft(schedules = listOf(
                ScheduleDraft(savedMorning.id, savedMorning.time, savedMorning.days),
                ScheduleDraft(savedEvening.id, LocalTime.of(9, 0), savedEvening.days)
            ))
        )

        assertEquals("2026-11-17", scheduleDao.schedules.value.single { it.id == savedEvening.id }.effectiveFrom)
        // 오늘 이 줄의 기록은 없다 (20:00 미기록은 지워지고, 09:00은 내일부터)
        assertEquals(listOf("08:00" to "PENDING"), todayRecords())
    }

    @Test
    fun `손대지 않은 줄에는 적용 시작일을 적지 않는다`() = runBlocking<Unit> {
        val id = addTylenol()

        repository.updateMedication(me, id, draft(memo = "식전", schedules = savedDrafts(id)))

        assertTrue(scheduleDao.schedules.value.all { it.effectiveFrom == null })
        assertEquals("식전", doseRecordDao.records.value.single { it.scheduledTime == "20:00" }.memo)
    }

    @Test
    fun `오늘 남은 기록을 지울 때 이력은 남고 기록과의 연결만 끊긴다`() = runBlocking<Unit> {
        val id = addTylenol()
        val evening = doseRecordDao.records.value.single { it.scheduledTime == "20:00" }
        // 미리 복용 완료 → 실행취소로 다시 미기록이 된 기록 (이력 2줄)
        listOf("PENDING" to "TAKEN", "TAKEN" to "PENDING").forEach { (from, to) ->
            eventDao.insert(
                com.cau.medicheck.data.local.DoseStatusEvent(
                    userId = me, doseRecordId = evening.id, scheduleId = evening.scheduleId,
                    doseDate = evening.date, scheduledTime = evening.scheduledTime,
                    medicationName = evening.medicationName, fromStatus = from, toStatus = to,
                    fromActedAt = null, toActedAt = null, changedAt = 1L
                )
            )
        }

        repository.deleteMedication(me, id)

        assertTrue(doseRecordDao.records.value.none { it.id == evening.id })
        assertEquals(2, eventDao.events.value.size)
        assertTrue(eventDao.events.value.all { it.doseRecordId == null && it.scheduleId == evening.scheduleId })
    }
    @Test
    fun `시각이 지나 내일부터가 된 줄은 같은 날 이름만 고쳐 다시 저장해도 내일부터 그대로다`() = runBlocking<Unit> {
        // 10:00에 저녁 20:00 줄을 09:00으로 바꾼다 → 이미 지난 시각이라 내일부터.
        val id = addTylenol()
        val (savedMorning, savedEvening) = repository.getMedication(me, id)!!.schedules
        repository.updateMedication(
            me, id,
            draft(schedules = listOf(
                ScheduleDraft(savedMorning.id, savedMorning.time, savedMorning.days),
                ScheduleDraft(savedEvening.id, LocalTime.of(9, 0), savedEvening.days)
            ))
        )

        // 같은 날 이름만 바꿔 다시 저장한다. 그 줄은 이제 "그대로인 줄"이다.
        repository.updateMedication(me, id, draft(name = "타이레놀 500", schedules = savedDrafts(id)))

        assertEquals("2026-11-17", scheduleDao.schedules.value.single { it.id == savedEvening.id }.effectiveFrom)
        // 오늘 그 줄의 기록이 새로 생기지 않는다 (생기면 만들자마자 '기한 경과')
        assertTrue(doseRecordDao.records.value.none { it.scheduleId == savedEvening.id && it.date == today.toString() })
    }

    @Test
    fun `내일부터로 적어 둔 줄이라도 시각이 아직이면 다시 저장할 때 오늘부터로 내린다`() = runBlocking<Unit> {
        val id = addTylenol()
        val (_, savedEvening) = repository.getMedication(me, id)!!.schedules
        // "내일부터 적용"(3단계 S33)으로 저장해 둔 상황을 직접 만든다. 오늘 20:00 기록은 없앤다.
        scheduleDao.update(scheduleDao.schedules.value.single { it.id == savedEvening.id }.copy(effectiveFrom = "2026-11-17"))
        doseRecordDao.records.value = doseRecordDao.records.value.filterNot { it.scheduleId == savedEvening.id }

        repository.updateMedication(me, id, draft(schedules = savedDrafts(id)))

        assertEquals("2026-11-16", scheduleDao.schedules.value.single { it.id == savedEvening.id }.effectiveFrom)
        assertEquals(listOf("08:00" to "PENDING", "20:00" to "PENDING"), todayRecords())
    }

    @Test
    fun `끝난 약의 종료일을 늘려도 끝나 있던 날의 기록은 생기지 않는다`() = runBlocking<Unit> {
        // 11/01에 시작해 11/10에 끝난 약. 오늘은 11/16(월) 10:00.
        repository.addMedication(
            me, draft(startDate = LocalDate.of(2026, 11, 1), endDate = LocalDate.of(2026, 11, 10))
        )
        val id = medicationDao.medications.value.single().id
        assertEquals("2026-11-10", doseRecordDao.records.value.maxOf { it.date })

        // 종료일을 없앤다(다시 먹기 시작)
        repository.updateMedication(me, id, draft(startDate = LocalDate.of(2026, 11, 1), endDate = null, schedules = savedDrafts(id)))

        // 11/11~11/15는 비어 있어야 한다
        assertTrue(doseRecordDao.records.value.none { it.date in "2026-11-11".."2026-11-15" })
        // 줄마다 오늘 시각 규칙: 08:00은 이미 지나서 내일부터, 20:00은 아직이라 오늘부터
        val schedules = scheduleDao.schedules.value.associateBy { it.time }
        assertEquals("2026-11-17", schedules.getValue("08:00").effectiveFrom)
        assertEquals("2026-11-16", schedules.getValue("20:00").effectiveFrom)
        assertEquals(listOf("20:00" to "PENDING"), todayRecords())
    }

    @Test
    fun `종료일을 앞으로 당기면 새 종료일 뒤의 미기록만 지워지고 응답한 기록은 남는다`() = runBlocking<Unit> {
        // 11/13부터 매일 08:00. 기록: 11/13 ~ 11/16
        repository.addMedication(me, draft(startDate = today.minusDays(3), schedules = listOf(morning)))
        val id = medicationDao.medications.value.single().id
        val taken = doseRecordDao.records.value.single { it.date == "2026-11-15" }
        doseRecordDao.updateStatus(me, taken.id, "TAKEN", 1L)

        // 종료일을 11/14로 당긴다
        repository.updateMedication(
            me, id, draft(startDate = today.minusDays(3), endDate = LocalDate.of(2026, 11, 14), schedules = savedDrafts(id))
        )

        assertEquals(
            listOf("2026-11-13" to "PENDING", "2026-11-14" to "PENDING", "2026-11-15" to "TAKEN"),
            doseRecordDao.records.value.sortedBy { it.date }.map { it.date to it.status }
        )
    }

    @Test
    fun `다시 맞추기로 지운 회차는 실행취소할 것이 없다`() = runBlocking<Unit> {
        val id = addTylenol()
        val (savedMorning, savedEvening) = repository.getMedication(me, id)!!.schedules
        val doseRepository = DoseRepository(
            doseRecordDao, eventDao,
            DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, eventDao, clock),
            FakeTransactionRunner(), { kotlinx.coroutines.flow.flowOf(emptyList()) }, clock
        )
        val eveningKey = com.cau.medicheck.domain.DoseKey(savedEvening.id, today)
        // 저녁 회차를 복용 완료로 했다가 다시 미기록으로 (실행취소할 거리가 있는 상태)
        doseRepository.changeStatus(me, eveningKey, com.cau.medicheck.domain.DoseStatus.TAKEN)
        doseRepository.changeStatus(me, eveningKey, com.cau.medicheck.domain.DoseStatus.PENDING)

        // 저녁 20:00 → 09:00 (이미 지난 시각 → 내일부터, 오늘 20:00 미기록은 지워짐)
        repository.updateMedication(
            me, id,
            draft(schedules = listOf(
                ScheduleDraft(savedMorning.id, savedMorning.time, savedMorning.days),
                ScheduleDraft(savedEvening.id, LocalTime.of(9, 0), savedEvening.days)
            ))
        )

        assertEquals(UndoResult.NOTHING, doseRepository.undoLast(me, eveningKey))
        assertTrue(eventDao.events.value.all { it.doseRecordId == null })
    }

    // ---------- 적용 범위 (S33): 오늘 이후 / 내일부터 ----------

    @Test
    fun `내일부터 적용하면 오늘 기록은 옛 시각과 옛 이름 그대로이고 바뀐 줄은 내일부터다`() = runBlocking<Unit> {
        val id = addTylenol()
        val (savedMorning, savedEvening) = repository.getMedication(me, id)!!.schedules

        // 지금은 10:00. 저녁 20:00 → 21:00, 이름도 바꿔 "내일부터 적용"
        repository.updateMedication(
            me, id,
            draft(
                name = "타이레놀 500",
                schedules = listOf(
                    ScheduleDraft(savedMorning.id, savedMorning.time, savedMorning.days),
                    ScheduleDraft(savedEvening.id, LocalTime.of(21, 0), savedEvening.days)
                )
            ),
            ApplyScope.FROM_TOMORROW
        )

        val evening = doseRecordDao.records.value.single { it.scheduleId == savedEvening.id }
        assertEquals("20:00", evening.scheduledTime)
        assertEquals("타이레놀", evening.medicationName)
        assertEquals("2026-11-17", scheduleDao.schedules.value.single { it.id == savedEvening.id }.effectiveFrom)
        // 손대지 않은 아침 줄에는 적용 시작일을 적지 않는다
        assertNull(scheduleDao.schedules.value.single { it.id == savedMorning.id }.effectiveFrom)
    }

    @Test
    fun `오늘 이후로 적용하면 아직 시각 전인 오늘 기록이 새 시각과 새 이름으로 바뀐다`() = runBlocking<Unit> {
        val id = addTylenol()
        val (savedMorning, savedEvening) = repository.getMedication(me, id)!!.schedules

        repository.updateMedication(
            me, id,
            draft(
                name = "타이레놀 500",
                schedules = listOf(
                    ScheduleDraft(savedMorning.id, savedMorning.time, savedMorning.days),
                    ScheduleDraft(savedEvening.id, LocalTime.of(21, 0), savedEvening.days)
                )
            ),
            ApplyScope.FROM_TODAY
        )

        val records = doseRecordDao.records.value
        // 저녁(아직 시각 전)은 새 시각·새 이름, 아침(이미 지남)은 옛 이름 그대로
        assertEquals("21:00" to "타이레놀 500", records.single { it.scheduleId == savedEvening.id }.let { it.scheduledTime to it.medicationName })
        assertEquals("타이레놀", records.single { it.scheduleId == savedMorning.id }.medicationName)
    }

    @Test
    fun `내일부터 적용으로 더한 새 시간은 오늘 기록을 만들지 않는다`() = runBlocking<Unit> {
        val id = addTylenol()
        val saved = savedDrafts(id)

        repository.updateMedication(
            me, id,
            draft(schedules = saved + ScheduleDraft(time = LocalTime.of(22, 0), days = RepeatDays.EVERY_DAY)),
            ApplyScope.FROM_TOMORROW
        )

        assertTrue(doseRecordDao.records.value.none { it.scheduledTime == "22:00" })
        assertEquals("2026-11-17", scheduleDao.schedules.value.single { it.time == "22:00" }.effectiveFrom)
    }

    @Test
    fun `미래였던 시작일을 지난 날짜로 당겨도 지난 날짜와 지난 시각의 기록은 생기지 않는다`() = runBlocking<Unit> {
        // 다음 주에 시작하는 약: 아직 기록이 없다
        val id = (repository.addMedication(me, draft(startDate = today.plusDays(7))) as MedicationResult.Success).medicationId
        assertTrue(doseRecordDao.records.value.isEmpty())

        // 시작일을 사흘 전으로 당겨 "오늘 이후 회차에 적용" (지금은 10:00)
        repository.updateMedication(me, id, draft(startDate = today.minusDays(3), schedules = savedDrafts(id)))

        // 지난 날짜도, 이미 지난 아침 08:00도 생기지 않고, 아직인 저녁 20:00만 생긴다
        assertEquals(listOf(today.toString() to "20:00"), doseRecordDao.records.value.map { it.date to it.scheduledTime })
    }

    @Test
    fun `미래였던 시작일을 당기면서 내일부터 적용하면 오늘 기록은 생기지 않는다`() = runBlocking<Unit> {
        val id = (repository.addMedication(me, draft(startDate = today.plusDays(7))) as MedicationResult.Success).medicationId

        repository.updateMedication(
            me, id, draft(startDate = today.minusDays(3), schedules = savedDrafts(id)), ApplyScope.FROM_TOMORROW
        )

        assertTrue(doseRecordDao.records.value.isEmpty())
        assertTrue(scheduleDao.schedules.value.all { it.effectiveFrom == "2026-11-17" })
    }

    // ---------- 일시중지 · 재개 (S07) ----------

    @Test
    fun `일시중지하면 오늘 시각 전인 미기록만 지워지고 지난 것은 남는다`() = runBlocking<Unit> {
        val id = addTylenol()

        val result = repository.setPaused(me, id, paused = true)

        assertEquals(MedicationResult.Success(id), result)
        assertEquals(today.toString(), medicationDao.medications.value.single().pausedFrom)
        // 아침 08:00(이미 지남)은 남고 저녁 20:00(아직)은 지워진다
        assertEquals(listOf("08:00"), doseRecordDao.records.value.map { it.scheduledTime })
    }

    @Test
    fun `일시중지한 동안에는 날짜가 지나도 기록이 생기지 않고 재개하면 그날부터 다시 생긴다`() = runBlocking<Unit> {
        val id = addTylenol()
        repository.setPaused(me, id, paused = true)

        // 이틀 뒤(11/18 수요일 10:00)의 시계로 본다
        val later = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC)
        val laterGenerator = DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, eventDao, later)
        val laterRepository = MedicationRepository(
            medicationDao = medicationDao, scheduleDao = scheduleDao, transaction = FakeTransactionRunner(),
            doseRecordGenerator = laterGenerator, now = { 6_000L }, clock = later
        )
        laterGenerator.generateForUser(me)
        assertTrue(doseRecordDao.records.value.none { it.date > today.toString() })

        laterRepository.setPaused(me, id, paused = false)

        assertNull(medicationDao.medications.value.single().pausedFrom)
        assertTrue(scheduleDao.schedules.value.all { it.effectiveFrom == "2026-11-18" })
        // 재개한 날(18일) 치만 생기고, 중지했던 17일은 비어 있다
        val after = doseRecordDao.records.value.filter { it.date > today.toString() }
        assertEquals(setOf("2026-11-18"), after.map { it.date }.toSet())
        assertEquals(2, after.size)
    }

    @Test
    fun `이미 그 상태인 일시중지는 아무것도 바꾸지 않고 남의 약은 중지할 수 없다`() = runBlocking<Unit> {
        val id = addTylenol()
        val before = doseRecordDao.records.value

        assertEquals(MedicationResult.Success(id), repository.setPaused(me, id, paused = false))
        assertEquals(before, doseRecordDao.records.value)
        assertEquals(
            MedicationResult.Failure(MedicationError.NOT_FOUND),
            repository.setPaused(someoneElse, id, paused = true)
        )
        assertNull(medicationDao.medications.value.single().pausedFrom)
    }

    // ---------- 처방 출처 (S42) · 같은 이름 찾기 (S40) ----------

    @Test
    fun `처방 출처는 앞뒤 공백을 지우고 빈 값은 null로 저장하며 기록과 규칙은 그대로다`() = runBlocking<Unit> {
        val id = addTylenol()
        val recordsBefore = doseRecordDao.records.value
        val schedulesBefore = scheduleDao.schedules.value

        repository.updateSource(me, id, hospital = "  ABC Clinic ", department = "   ", sourceMemo = "3개월마다")

        val saved = medicationDao.medications.value.single()
        assertEquals("ABC Clinic", saved.hospital)
        assertNull(saved.department)
        assertEquals("3개월마다", saved.sourceMemo)
        assertEquals(recordsBefore, doseRecordDao.records.value)
        assertEquals(schedulesBefore, scheduleDao.schedules.value)
        // 읽을 때는 빈 값이 ""로 나온다
        assertEquals("ABC Clinic" to "", repository.getMedication(me, id)!!.let { it.hospital to it.department })
    }

    @Test
    fun `남의 약에는 처방 출처를 저장할 수 없다`() = runBlocking<Unit> {
        val id = addTylenol()

        val result = repository.updateSource(someoneElse, id, "병원", "내과", "")

        assertEquals(MedicationResult.Failure(MedicationError.NOT_FOUND), result)
        assertNull(medicationDao.medications.value.single().hospital)
    }

    @Test
    fun `같은 이름 찾기는 공백과 대소문자를 무시하고 삭제한 약과 남의 약은 뺀다`() = runBlocking<Unit> {
        val vitamin = (repository.addMedication(me, draft(name = "Vitamin D")) as MedicationResult.Success).medicationId
        val paused = (repository.addMedication(me, draft(name = "vitamin d")) as MedicationResult.Success).medicationId
        val deleted = (repository.addMedication(me, draft(name = "VITAMIN D")) as MedicationResult.Success).medicationId
        repository.addMedication(me, draft(name = "Vitamin C"))
        repository.addMedication(someoneElse, draft(name = "Vitamin D"))
        repository.setPaused(me, paused, paused = true)
        repository.deleteMedication(me, deleted)

        val same = repository.findSameName(me, "  vitamin D ")

        // 일시중지한 약은 포함, 삭제한 약·남의 약·다른 이름은 제외. 등록한 순서로 나온다.
        assertEquals(listOf(vitamin, paused), same.map { it.id })
    }
}
