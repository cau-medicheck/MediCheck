package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 5단계 알림 계산 테스트: 기록 줄 + 규칙, 재알림, 알림 센터 목록, 알람 시각 풀기.
 * 기준일: 2026-11-16은 월요일이다.
 */
class ReminderRecordsTest {

    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 11, dayOfMonth)
    private fun at(dayOfMonth: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(day(dayOfMonth), LocalTime.of(hour, minute))

    private fun record(
        scheduleId: Long, dayOfMonth: Int, hour: Int, minute: Int = 0,
        pending: Boolean = true, name: String = "약 $scheduleId"
    ) = ReminderDose(scheduleId, day(dayOfMonth), LocalTime.of(hour, minute), name, "1정", pending)

    private fun rule(scheduleId: Long, hour: Int, name: String = "약 $scheduleId", effectiveFrom: LocalDate? = null) =
        ReminderRule(
            scheduleId = scheduleId, medicationId = scheduleId, medicationName = name, dosage = "1정",
            startDate = day(1), endDate = null, time = LocalTime.of(hour, 0), days = RepeatDays.EVERY_DAY,
            effectiveFrom = effectiveFrom
        )

    // ---------- 다음 알람 ----------

    @Test
    fun `기록이 있는 날은 기록의 시각이 기준이고 기록이 없는 날은 규칙이 기준이다`() {
        // 규칙은 21시로 고쳤지만 오늘(16일) 기록은 20시다.
        val records = listOf(record(1, 16, 20))
        val rules = listOf(rule(1, 21))

        assertEquals(at(16, 20), ReminderPlanner.nextTrigger(records, rules, at(16, 10)))
        // 오늘 20시가 지나면 내일은 규칙의 21시
        assertEquals(at(17, 21), ReminderPlanner.nextTrigger(records, rules, at(16, 20)))
    }

    @Test
    fun `응답한 기록은 다음 알람에서 빠진다`() {
        val records = listOf(record(1, 16, 20, pending = false))

        assertEquals(at(17, 20), ReminderPlanner.nextTrigger(records, listOf(rule(1, 20)), at(16, 10)))
    }

    @Test
    fun `규칙이 없어진 약의 남은 기록도 알린다`() {
        // "내일부터 적용"으로 규칙을 바꿔 옛 규칙이 지워졌어도 오늘 기록은 남아 있다.
        assertEquals(at(16, 20), ReminderPlanner.nextTrigger(listOf(record(1, 16, 20)), emptyList(), at(16, 10)))
    }

    // ---------- 지금 알릴 회차 ----------

    @Test
    fun `그 분의 미기록 회차만 알린다`() {
        val records = listOf(
            record(1, 16, 20),
            record(2, 16, 20, pending = false),
            record(3, 16, 21)
        )

        val due = ReminderPlanner.dueAt(records, emptyList(), at(16, 20).plusSeconds(20))

        assertEquals(listOf(1L), due.map { it.scheduleId })
    }

    @Test
    fun `기록이 없는 회차는 규칙으로 알리고 기록이 있으면 규칙을 쓰지 않는다`() {
        val rules = listOf(rule(1, 20), rule(2, 20))
        // 2번 규칙은 오늘 기록이 08시에 있다 (지난 시각으로 이미 만들어진 회차)
        val records = listOf(record(2, 16, 8))

        val due = ReminderPlanner.dueAt(records, rules, at(16, 20))

        assertEquals(listOf(1L), due.map { it.scheduleId })
    }

    // ---------- 재알림 ----------

    @Test
    fun `재알림 시각은 예정 일시에 간격을 더한 것이다`() {
        val records = listOf(record(1, 16, 9, 50))

        assertEquals(at(16, 10, 5), ReminderPlanner.nextRealert(records, at(16, 10), intervalMinutes = 15))
        // 정확히 예정 시각에 계산해도 (알람이 방금 울린 직후) 재알림이 걸린다
        assertEquals(at(16, 10, 5), ReminderPlanner.nextRealert(records, at(16, 9, 50), intervalMinutes = 15))
    }

    @Test
    fun `재알림은 한 번뿐이고 응답한 회차와 아직 안 된 회차에는 없다`() {
        // 간격이 이미 지난 회차: 다시 걸지 않는다
        assertNull(ReminderPlanner.nextRealert(listOf(record(1, 16, 9)), at(16, 10), 15))
        // 응답한 회차
        assertNull(ReminderPlanner.nextRealert(listOf(record(1, 16, 9, 50, pending = false)), at(16, 10), 15))
        // 아직 시각이 안 된 회차 (회차 알람이 먼저 울려야 한다)
        assertNull(ReminderPlanner.nextRealert(listOf(record(1, 16, 20)), at(16, 10), 15))
    }

    @Test
    fun `자정을 넘기는 재알림은 어제 회차로 계산한다`() {
        val records = listOf(record(1, 15, 23, 50))
        val now = LocalDateTime.of(day(16), LocalTime.of(0, 1))

        assertEquals(at(16, 0, 5), ReminderPlanner.nextRealert(records, now, 15))
        assertEquals(listOf(day(15)), ReminderPlanner.realertsDueAt(records, at(16, 0, 5), 15).map { it.date })
    }

    @Test
    fun `재알림 차례인 회차는 응답 전인 것만 고른다`() {
        val records = listOf(
            record(1, 16, 9, 50),
            record(2, 16, 9, 50, pending = false),
            record(3, 16, 9, 40)
        )

        assertEquals(listOf(1L), ReminderPlanner.realertsDueAt(records, at(16, 10, 5), 15).map { it.scheduleId })
    }

    // ---------- 알림 센터 목록 ----------

    @Test
    fun `목록은 오늘 남은 것과 내일 것을 날짜 시각 이름 순으로 보인다`() {
        val records = listOf(
            record(1, 16, 8, name = "아침 약"),               // 지남
            record(2, 16, 20, name = "저녁 약"),
            record(3, 16, 20, name = "가나다"),
            record(4, 16, 21, pending = false, name = "먹은 약")
        )
        val rules = listOf(rule(1, 8, "아침 약"), rule(2, 20, "저녁 약"))

        val list = ReminderPlanner.upcoming(records, rules, at(16, 10), limit = 8)

        assertEquals(
            listOf("16 20:00 가나다", "16 20:00 저녁 약", "17 08:00 아침 약", "17 20:00 저녁 약"),
            list.map { "${it.date.dayOfMonth} ${it.time} ${it.medicationName}" }
        )
    }

    @Test
    fun `정확히 지금 분인 회차와 적용 시작 전인 내일 회차는 목록에 없다`() {
        val records = listOf(record(1, 16, 10))
        // 2번 규칙은 18일부터 적용된다 → 내일(17일) 줄이 없다
        val rules = listOf(rule(1, 10), rule(2, 9, effectiveFrom = day(18)))

        val list = ReminderPlanner.upcoming(records, rules, at(16, 10), limit = 8)

        assertEquals(listOf("17 10:00"), list.map { "${it.date.dayOfMonth} ${it.time}" })
    }

    @Test
    fun `목록은 정한 개수까지만 보인다`() {
        val rules = (1L..12L).map { rule(it, 11 + it.toInt() % 12) }

        assertEquals(8, ReminderPlanner.upcoming(emptyList(), rules, at(16, 10), limit = 8).size)
    }

    // ---------- 알람이 울렸을 때 "몇 시 회차였나" ----------

    @Test
    fun `알람에 실린 예정 일시 글자를 그대로 쓴다`() {
        // 미국 뉴욕은 2026-03-08 02:00에 시계를 03:00으로 당긴다. 02:30은 없는 시각이라 03:30에 울린다.
        val newYork = ZoneId.of("America/New_York")
        val scheduled = LocalDateTime.of(2026, 3, 8, 2, 30)
        val firedAtMillis = scheduled.atZone(newYork).toInstant().toEpochMilli()

        // 글자가 있으면 02:30 회차를 찾는다
        assertEquals(scheduled, ReminderTrigger.resolve("2026-03-08T02:30", firedAtMillis, newYork))
        // 글자가 없으면(옛 알람) 밀리초를 시각으로 바꾼다 → 03:30
        assertEquals(LocalDateTime.of(2026, 3, 8, 3, 30), ReminderTrigger.resolve(null, firedAtMillis, newYork))
        // 글자가 깨져 있어도 밀리초로 대신한다
        assertEquals(LocalDateTime.of(2026, 3, 8, 3, 30), ReminderTrigger.resolve("??", firedAtMillis, newYork))
    }

    @Test
    fun `알림 계산은 시간대와 상관없이 같은 결과를 낸다`() {
        // ReminderPlanner는 날짜·시각(LocalDateTime)만 다루므로 시간대가 끼어들 곳이 없다.
        val records = listOf(record(1, 16, 20))
        assertTrue(ReminderPlanner.dueAt(records, emptyList(), at(16, 20)).isNotEmpty())
    }
}
