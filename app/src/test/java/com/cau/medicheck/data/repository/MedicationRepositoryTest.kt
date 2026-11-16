package com.cau.medicheck.data.repository

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
        repository = MedicationRepository(
            medicationDao = medicationDao,
            scheduleDao = scheduleDao,
            transaction = FakeTransactionRunner(),
            doseRecordGenerator = DoseRecordGenerator(medicationDao, scheduleDao, doseRecordDao, clock),
            now = { 5_000L }
        )
    }

    /** 약을 하나 등록하고 그 id를 돌려준다. */
    private suspend fun addTylenol(userId: Long = me): Long =
        (repository.addMedication(userId, draft()) as MedicationResult.Success).medicationId

    // ---------- 등록 ----------

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

        assertEquals(MedicationResult.Failure(MedicationError.NO_SCHEDULE), result)
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
        doseRecordDao.updateStatus(eveningRecord.id, "TAKEN", 9_000L)   // 저녁 약을 미리 먹었다고 기록

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
}
