package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecord
import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.domain.UserSettings
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

    /** 테스트마다 바꿔 끼우는 "지금 설정" */
    private var settings = UserSettings()

    @Before
    fun setUp() {
        sessionStore = FakeSessionStore()
        medicationDao = FakeMedicationDao()
        scheduleDao = FakeScheduleDao(medicationDao)
        doseRecordDao = FakeDoseRecordDao(medicationDao, scheduleDao)
        settings = UserSettings()
        repository = ReminderRepository(sessionStore, scheduleDao, doseRecordDao, clock, currentSettings = { settings })
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
    private suspend fun insertRecord(
        scheduleId: Long,
        status: String,
        time: String = "20:00",
        date: String = "2026-11-16",
        snapshotName: String? = null
    ) {
        val medicationId = scheduleDao.schedules.value.single { it.id == scheduleId }.medicationId
        val name = snapshotName ?: medicationDao.medications.value.single { it.id == medicationId }.name
        doseRecordDao.insertAllIgnore(
            listOf(
                DoseRecord(
                    scheduleId = scheduleId, date = date,
                    scheduledTime = time, medicationName = name, dosage = "1정",
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
        assertTrue(repository.remindersAt(LocalDateTime.of(2026, 11, 16, 12, 0)).isEmpty())
    }

    @Test
    fun `삭제한 약은 알림에 넣지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val scheduleId = insertDaily(me, "타이레놀", "20:00")
        val medicationId = scheduleDao.schedules.value.single { it.id == scheduleId }.medicationId
        scheduleDao.softDeleteByMedication(medicationId, deletedAt = 1)
        medicationDao.softDelete(medicationId, deletedAt = 1)

        assertNull(repository.nextTrigger())
        assertTrue(repository.remindersAt(today20).isEmpty())
    }

    @Test
    fun `그 시각에 먹어야 하는 약을 돌려준다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        insertDaily(me, "아침 약", "08:00")
        insertDaily(me, "저녁 약", "20:00")

        assertEquals(listOf("아침 약"), repository.remindersAt(today8).map { it.dose.medicationName })
        assertEquals(listOf("저녁 약"), repository.remindersAt(today20).map { it.dose.medicationName })
    }

    @Test
    fun `아직 응답하지 않은 기록이 있어도 알린다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val scheduleId = insertDaily(me, "저녁 약", "20:00")
        insertRecord(scheduleId, "PENDING")

        assertEquals(1, repository.remindersAt(today20).size)
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

        assertEquals(listOf("아직 안 먹은 약"), repository.remindersAt(today20).map { it.dose.medicationName })
    }

    @Test
    fun `적용 시작일 전에는 알리지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val scheduleId = insertDaily(me, "저녁 약", "20:00")
        // 오늘 "내일부터 적용"으로 바꾼 규칙
        scheduleDao.update(scheduleDao.schedules.value.single { it.id == scheduleId }.copy(effectiveFrom = "2026-11-17"))

        assertTrue(repository.remindersAt(today20).isEmpty())
        assertEquals(LocalDateTime.of(2026, 11, 17, 20, 0), repository.nextTrigger())
    }

    @Test
    fun `일시중지한 약은 알리지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val scheduleId = insertDaily(me, "저녁 약", "20:00")
        val medicationId = scheduleDao.schedules.value.single { it.id == scheduleId }.medicationId
        assertEquals(1, medicationDao.setPausedFrom(me, medicationId, "2026-11-16"))

        assertNull(repository.nextTrigger())
        assertTrue(repository.remindersAt(today20).isEmpty())
    }

    @Test
    fun `다른 사람의 약은 일시중지할 수 없다`() = runBlocking<Unit> {
        val scheduleId = insertDaily(someoneElse, "남의 약", "20:00")
        val medicationId = scheduleDao.schedules.value.single { it.id == scheduleId }.medicationId

        assertEquals(0, medicationDao.setPausedFrom(me, medicationId, "2026-11-16"))
        assertEquals(0, scheduleDao.raiseEffectiveFrom(me, medicationId, "2026-11-16"))
        assertNull(medicationDao.medications.value.single().pausedFrom)
    }

    // ---------- 5단계: 설정 · 오늘은 기록 기준 · 재알림 · 알림 센터 목록 ----------

    @Test
    fun `복약 시간 알림을 끄면 다음 알림도 지금 알릴 약도 없다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        insertDaily(me, "저녁 약", "20:00")
        settings = UserSettings(reminderEnabled = false, realertEnabled = true)

        assertNull(repository.nextTrigger())
        assertTrue(repository.remindersAt(today20).isEmpty())
    }

    @Test
    fun `오늘은 기록의 예정 시각에 알리고 규칙의 새 시각에는 알리지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        // 규칙은 21:00으로 고쳤지만 오늘 기록은 옛 시각 20:00 그대로다.
        val scheduleId = insertDaily(me, "저녁 약", "21:00")
        insertRecord(scheduleId, "PENDING", time = "20:00")

        assertEquals(today20, repository.nextTrigger())
        assertEquals(1, repository.remindersAt(today20).size)
        assertTrue(repository.remindersAt(LocalDateTime.of(2026, 11, 16, 21, 0)).isEmpty())
    }

    @Test
    fun `지난 시각의 기록이 있는 규칙은 오늘 새 시각에 알리지 않고 내일부터 알린다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        // 10시에 08:00 → 20:00으로 고쳤다. 오늘 기록은 지난 08:00 하나뿐이다.
        val scheduleId = insertDaily(me, "아침 약", "20:00")
        insertRecord(scheduleId, "PENDING", time = "08:00")

        assertTrue(repository.remindersAt(today20).isEmpty())
        assertEquals(LocalDateTime.of(2026, 11, 17, 20, 0), repository.nextTrigger())
    }

    @Test
    fun `알림의 약 이름은 기록에 찍어 둔 이름이다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val scheduleId = insertDaily(me, "새 이름", "20:00")
        insertRecord(scheduleId, "PENDING", snapshotName = "옛 이름")

        assertEquals(listOf("옛 이름"), repository.remindersAt(today20).map { it.dose.medicationName })
    }

    @Test
    fun `재알림이 켜져 있으면 응답하지 않은 회차를 간격 뒤에 다시 알린다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        settings = UserSettings(realertEnabled = true, realertMinutes = 15)
        // 지금은 10:00. 09:50 회차에 응답하지 않았다 → 10:05에 재알림
        val scheduleId = insertDaily(me, "아침 약", "09:50")
        insertRecord(scheduleId, "PENDING", time = "09:50")
        val realertAt = LocalDateTime.of(2026, 11, 16, 10, 5)

        assertEquals(realertAt, repository.nextTrigger())
        val due = repository.remindersAt(realertAt)
        assertEquals(listOf(true), due.map { it.isRealert })
    }

    @Test
    fun `응답한 회차와 재알림이 꺼져 있을 때는 다시 알리지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val taken = insertDaily(me, "먹은 약", "09:50")
        insertRecord(taken, "TAKEN", time = "09:50")
        val pending = insertDaily(me, "안 먹은 약", "09:50")
        insertRecord(pending, "PENDING", time = "09:50")
        val realertAt = LocalDateTime.of(2026, 11, 16, 10, 5)

        // 재알림 꺼짐: 아무것도 없다
        assertTrue(repository.remindersAt(realertAt).isEmpty())

        // 재알림 켜짐: 응답하지 않은 약만
        settings = UserSettings(realertEnabled = true, realertMinutes = 15)
        assertEquals(listOf("안 먹은 약"), repository.remindersAt(realertAt).map { it.dose.medicationName })
    }

    @Test
    fun `다음 알람은 다음 회차와 재알림 중 이른 쪽이다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        settings = UserSettings(realertEnabled = true, realertMinutes = 30)
        val morning = insertDaily(me, "아침 약", "09:50")
        insertRecord(morning, "PENDING", time = "09:50")   // 재알림 10:20
        val soon = insertDaily(me, "곧 먹을 약", "10:10")
        insertRecord(soon, "PENDING", time = "10:10")

        assertEquals(LocalDateTime.of(2026, 11, 16, 10, 10), repository.nextTrigger())
        // 10:10 알람이 울린 뒤에는 10:20 재알림이 다음이다
        assertEquals(
            LocalDateTime.of(2026, 11, 16, 10, 20),
            repository.nextTrigger(notBefore = LocalDateTime.of(2026, 11, 16, 10, 10))
        )
    }

    @Test
    fun `방금 울린 시각은 다시 걸지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        insertDaily(me, "저녁 약", "20:00")

        // 알람이 조금 일찍(지금 10:00) 울렸더라도 20:00을 또 걸지 않고 내일 20:00을 건다
        assertEquals(LocalDateTime.of(2026, 11, 17, 20, 0), repository.nextTrigger(notBefore = today20))
    }

    @Test
    fun `알림 센터 목록은 오늘 남은 회차와 내일 회차다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val morning = insertDaily(me, "아침 약", "08:00")
        insertRecord(morning, "PENDING", time = "08:00")          // 이미 지남 → 오늘 줄 없음
        val evening = insertDaily(me, "저녁 약", "20:00")
        insertRecord(evening, "PENDING")
        val taken = insertDaily(me, "미리 먹은 약", "21:00")
        insertRecord(taken, "TAKEN", time = "21:00")              // 응답함 → 오늘 줄 없음

        val upcoming = repository.upcoming()

        assertTrue(upcoming.hasActiveMedication)
        assertEquals(
            listOf("16 20:00 저녁 약", "17 08:00 아침 약", "17 20:00 저녁 약", "17 21:00 미리 먹은 약"),
            upcoming.items.map { "${it.date.dayOfMonth} ${it.time} ${it.medicationName}" }
        )
    }

    @Test
    fun `알림 센터 목록은 8개까지만이고 약이 없으면 사용 중인 약 없음으로 알려 준다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        assertTrue(repository.upcoming().items.isEmpty())
        assertTrue(!repository.upcoming().hasActiveMedication)

        (11..19).forEach { hour -> insertDaily(me, "약 $hour", "$hour:00") }

        assertEquals(8, repository.upcoming().items.size)
    }

    @Test
    fun `알람이 늦게 울리면 그 사이에 지나간 회차도 함께 알린다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        // 지금은 10:00. 09:50 알람이 늦게 울렸고 그 사이 09:55 회차도 지나갔다.
        val first = insertDaily(me, "첫 약", "09:50")
        insertRecord(first, "PENDING", time = "09:50")
        val second = insertDaily(me, "둘째 약", "09:55")
        insertRecord(second, "PENDING", time = "09:55")
        insertDaily(me, "나중 약", "10:01")
        // 30분보다 더 전에 지나간 회차는 몰아서 알리지 않는다
        val old = insertDaily(me, "오래된 약", "09:00")
        insertRecord(old, "PENDING", time = "09:00")

        val due = repository.remindersAt(LocalDateTime.of(2026, 11, 16, 9, 50))

        assertEquals(listOf("첫 약", "둘째 약"), due.map { it.dose.medicationName })
    }

    @Test
    fun `삭제하거나 일시중지한 약의 남은 기록은 다시 알리지 않는다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        settings = UserSettings(realertEnabled = true, realertMinutes = 15)
        val paused = insertDaily(me, "중지한 약", "09:50")
        insertRecord(paused, "PENDING", time = "09:50")
        val pausedMedId = scheduleDao.schedules.value.single { it.id == paused }.medicationId
        medicationDao.setPausedFrom(me, pausedMedId, "2026-11-16")

        assertNull(repository.nextTrigger())
        assertTrue(repository.remindersAt(LocalDateTime.of(2026, 11, 16, 10, 5)).isEmpty())
    }

    @Test
    fun `계산하기 전에 오늘까지의 기록을 채운다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(me)
        val refreshed = mutableListOf<Long>()
        val withRefresh = ReminderRepository(
            sessionStore, scheduleDao, doseRecordDao, clock,
            refreshRecords = { refreshed += it }, currentSettings = { settings }
        )

        withRefresh.nextTrigger()
        withRefresh.remindersAt(today20)
        withRefresh.upcoming()

        assertEquals(listOf(me, me, me), refreshed)
    }
}
