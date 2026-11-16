package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * "다음 알림은 언제인가", "지금 알릴 약은 무엇인가" 계산 테스트.
 * 기준일: 2026-11-16은 월요일이다.
 */
class ReminderPlannerTest {

    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 11, dayOfMonth)

    private fun at(dayOfMonth: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(day(dayOfMonth), LocalTime.of(hour, minute))

    private fun rule(
        scheduleId: Long = 1,
        hour: Int = 8,
        minute: Int = 0,
        days: Set<DayOfWeek> = RepeatDays.EVERY_DAY,
        startDate: LocalDate = day(1),
        endDate: LocalDate? = null,
        name: String = "타이레놀"
    ) = ReminderRule(
        scheduleId = scheduleId, medicationId = scheduleId, medicationName = name, dosage = "1정",
        startDate = startDate, endDate = endDate, time = LocalTime.of(hour, minute), days = days
    )

    // ---------- 다음 복용 일시 ----------

    @Test
    fun `오늘 시각이 아직 안 됐으면 오늘 그 시각이다`() {
        assertEquals(at(16, 8), ReminderPlanner.nextOccurrence(rule(hour = 8), after = at(16, 7, 30)))
    }

    @Test
    fun `오늘 시각이 이미 지났으면 내일 그 시각이다`() {
        assertEquals(at(17, 8), ReminderPlanner.nextOccurrence(rule(hour = 8), after = at(16, 9)))
    }

    @Test
    fun `정확히 그 시각이면 지금이 아니라 다음 번을 돌려준다`() {
        // 알람이 08:00에 울린 직후 "다음"을 물으면 내일이어야 한다. (같은 알람이 또 걸리지 않게)
        assertEquals(at(17, 8), ReminderPlanner.nextOccurrence(rule(hour = 8), after = at(16, 8)))
    }

    @Test
    fun `반복 요일이 아닌 날은 건너뛴다`() {
        val monWedFri = rule(hour = 20, days = setOf(MONDAY, WEDNESDAY, FRIDAY))

        // 월요일 21시 → 다음은 수요일(18일) 20시
        assertEquals(at(18, 20), ReminderPlanner.nextOccurrence(monWedFri, after = at(16, 21)))
    }

    @Test
    fun `일주일에 하루뿐인 규칙은 다음 주 같은 요일이다`() {
        val mondayOnly = rule(hour = 8, days = setOf(MONDAY))

        assertEquals(at(23, 8), ReminderPlanner.nextOccurrence(mondayOnly, after = at(16, 9)))
    }

    @Test
    fun `시작일이 아직 안 됐으면 시작일부터다`() {
        assertEquals(
            at(20, 8),
            ReminderPlanner.nextOccurrence(rule(hour = 8, startDate = day(20)), after = at(16, 9))
        )
    }

    @Test
    fun `종료일이 지났으면 다음 복용은 없다`() {
        assertNull(ReminderPlanner.nextOccurrence(rule(hour = 8, endDate = day(15)), after = at(16, 7)))
    }

    @Test
    fun `종료일 당일의 시각이 지났으면 다음 복용은 없다`() {
        assertNull(ReminderPlanner.nextOccurrence(rule(hour = 8, endDate = day(16)), after = at(16, 9)))
    }

    @Test
    fun `요일을 하나도 고르지 않은 규칙은 다음 복용이 없다`() {
        assertNull(ReminderPlanner.nextOccurrence(rule(days = emptySet()), after = at(16, 7)))
    }

    // ---------- 여러 규칙 중 가장 이른 것 ----------

    @Test
    fun `여러 규칙 중 가장 먼저 오는 시각을 고른다`() {
        val rules = listOf(rule(scheduleId = 1, hour = 20), rule(scheduleId = 2, hour = 13), rule(scheduleId = 3, hour = 8))

        // 지금 10시 → 8시는 지났으니 13시가 가장 이르다
        assertEquals(at(16, 13), ReminderPlanner.nextTrigger(rules, after = at(16, 10)))
    }

    @Test
    fun `규칙이 없으면 다음 알림도 없다`() {
        assertNull(ReminderPlanner.nextTrigger(emptyList(), after = at(16, 10)))
    }

    // ---------- 지금 알릴 약 ----------

    @Test
    fun `같은 시각에 걸린 약은 모두 알린다`() {
        val rules = listOf(
            rule(scheduleId = 1, hour = 8, name = "타이레놀"),
            rule(scheduleId = 2, hour = 8, name = "비타민"),
            rule(scheduleId = 3, hour = 20, name = "저녁 약")
        )

        val due = ReminderPlanner.dueAt(rules, at(16, 8))

        assertEquals(listOf("타이레놀", "비타민"), due.map { it.medicationName })
    }

    @Test
    fun `초 단위가 조금 어긋나도 같은 분이면 알린다`() {
        val fewSecondsLate = at(16, 8).plusSeconds(12)

        assertEquals(1, ReminderPlanner.dueAt(listOf(rule(hour = 8)), fewSecondsLate).size)
    }

    @Test
    fun `요일이나 기간에 해당하지 않는 약은 알리지 않는다`() {
        val wednesdayOnly = rule(scheduleId = 1, hour = 8, days = setOf(WEDNESDAY))
        val notStartedYet = rule(scheduleId = 2, hour = 8, startDate = day(20))
        val alreadyEnded = rule(scheduleId = 3, hour = 8, endDate = day(10))

        val due = ReminderPlanner.dueAt(listOf(wednesdayOnly, notStartedYet, alreadyEnded), at(16, 8))

        assertTrue(due.isEmpty())
    }
}
