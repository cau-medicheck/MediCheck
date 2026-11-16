package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.MedicationDao
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.data.local.ScheduleDao
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.domain.MedicationDraft
import com.cau.medicheck.domain.MedicationError
import com.cau.medicheck.domain.RepeatDays
import com.cau.medicheck.domain.ScheduleDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
import java.time.LocalDate
import java.time.LocalTime

/**
 * MedicationRepository 단위 테스트.
 * 진짜 DB 대신 메모리에만 저장하는 가짜 DAO를 끼워서, 등록·수정·삭제의 순서와 결과를 확인한다.
 */
class MedicationRepositoryTest {

    private val me = 1L
    private val someoneElse = 2L

    private lateinit var medicationDao: FakeMedicationDao
    private lateinit var scheduleDao: FakeScheduleDao
    private lateinit var repository: MedicationRepository

    private val morning = ScheduleDraft(time = LocalTime.of(8, 0), days = RepeatDays.EVERY_DAY)
    private val evening = ScheduleDraft(time = LocalTime.of(20, 0), days = setOf(MONDAY, WEDNESDAY, FRIDAY))

    private fun draft(
        name: String = "타이레놀",
        schedules: List<ScheduleDraft> = listOf(morning, evening),
        endDate: LocalDate? = null,
        memo: String = "식후 30분"
    ) = MedicationDraft(
        name = name,
        dosage = "1정",
        startDate = LocalDate.of(2026, 11, 16),
        endDate = endDate,
        memo = memo,
        schedules = schedules
    )

    @Before
    fun setUp() {
        medicationDao = FakeMedicationDao()
        scheduleDao = FakeScheduleDao(medicationDao)
        repository = MedicationRepository(
            medicationDao = medicationDao,
            scheduleDao = scheduleDao,
            transaction = FakeTransactionRunner(),
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
}

/** 메모리의 목록에만 저장하는 가짜 MedicationDao. */
private class FakeMedicationDao : MedicationDao {
    val medications = MutableStateFlow<List<Medication>>(emptyList())

    override suspend fun insert(medication: Medication): Long {
        val newId = medications.value.size + 1L
        medications.update { it + medication.copy(id = newId) }
        return newId
    }

    override suspend fun update(medication: Medication) {
        medications.update { list -> list.map { if (it.id == medication.id) medication else it } }
    }

    override suspend fun findById(id: Long): Medication? = medications.value.find { it.id == id }

    override fun observeActive(userId: Long): Flow<List<Medication>> =
        medications.map { list -> list.filter { it.userId == userId && it.deletedAt == null } }

    override suspend fun softDelete(id: Long, deletedAt: Long) {
        medications.update { list -> list.map { if (it.id == id) it.copy(deletedAt = deletedAt) else it } }
    }
}

/** 메모리의 목록에만 저장하는 가짜 ScheduleDao. 주인을 가려내려고 약 목록을 함께 본다. */
private class FakeScheduleDao(private val medicationDao: FakeMedicationDao) : ScheduleDao {
    val schedules = MutableStateFlow<List<Schedule>>(emptyList())

    override suspend fun insert(schedule: Schedule): Long {
        val newId = schedules.value.size + 1L
        schedules.update { it + schedule.copy(id = newId) }
        return newId
    }

    override suspend fun update(schedule: Schedule) {
        schedules.update { list -> list.map { if (it.id == schedule.id) schedule else it } }
    }

    override suspend fun findActiveByMedication(medicationId: Long): List<Schedule> =
        schedules.value
            .filter { it.medicationId == medicationId && it.deletedAt == null }
            .sortedBy { it.time }

    override fun observeActiveByUser(userId: Long): Flow<List<Schedule>> =
        schedules.map { list ->
            val myMedicationIds = medicationDao.medications.value
                .filter { it.userId == userId && it.deletedAt == null }
                .map { it.id }
            list.filter { it.medicationId in myMedicationIds && it.deletedAt == null }.sortedBy { it.time }
        }

    override suspend fun softDelete(id: Long, deletedAt: Long) {
        schedules.update { list -> list.map { if (it.id == id) it.copy(deletedAt = deletedAt) else it } }
    }

    override suspend fun softDeleteByMedication(medicationId: Long, deletedAt: Long) {
        schedules.update { list ->
            list.map {
                if (it.medicationId == medicationId && it.deletedAt == null) it.copy(deletedAt = deletedAt) else it
            }
        }
    }
}

/** 트랜잭션 없이 그냥 순서대로 실행만 하는 가짜. */
private class FakeTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
