package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/** 통계 "기타" 탭: 빠뜨림(시간대·요일·약·결론), 최근 14일 칸, 제시간에 먹었나. 기준 "지금" = 2026-11-24(화) 12:00 */
class PatternInsightsTest {

    private val today = LocalDate.of(2026, 11, 24)
    private val now = LocalDateTime.of(today, LocalTime.of(12, 0))
    private val zone = ZoneOffset.UTC
    private var id = 1L

    private fun dose(daysAgo: Long, hour: Int, status: DoseStatus, med: Long = 1, name: String = "약$med", actedMinute: Long? = null): DoseItem {
        val date = today.minusDays(daysAgo)
        val time = LocalTime.of(hour, 0)
        val acted = actedMinute?.let { LocalDateTime.of(date, time).plusMinutes(it).toInstant(zone).toEpochMilli() }
        return DoseItem(
            recordId = id++, scheduleId = med * 10 + hour, medicationId = med, medicationName = name, dosage = "1정", memo = "",
            date = date, time = time, status = status, actedAt = acted
        )
    }

    @Test
    fun `빠뜨림은 건너뜀과 미복용이고 시각 전이나 여유 안은 세지 않는다`() {
        assertEquals(MissStats(1, 1), PatternInsights.missOf(dose(1, 8, DoseStatus.SKIPPED), now))
        assertEquals(MissStats(1, 1), PatternInsights.missOf(dose(1, 8, DoseStatus.PENDING), now))
        assertEquals(MissStats(0, 1), PatternInsights.missOf(dose(1, 8, DoseStatus.TAKEN), now))
        assertEquals(MissStats(), PatternInsights.missOf(dose(0, 20, DoseStatus.PENDING), now))   // 오늘 20시는 아직
    }

    @Test
    fun `4주보다 오래된 회차는 세지 않는다`() {
        val doses = listOf(dose(27, 8, DoseStatus.SKIPPED), dose(28, 8, DoseStatus.SKIPPED))
        assertEquals(MissStats(1, 1), PatternInsights.slots(doses, today, now).first { it.period == DayPeriod.MORNING }.stats)
    }

    @Test
    fun `시간대별 빠뜨린 비율과 대표 시각`() {
        val doses = listOf(dose(1, 21, DoseStatus.PENDING), dose(2, 21, DoseStatus.PENDING), dose(3, 21, DoseStatus.TAKEN))
        val night = PatternInsights.slots(doses, today, now).first { it.period == DayPeriod.NIGHT }
        assertEquals(67, night.stats.rate)
        assertEquals(LocalTime.of(21, 0), night.time)
        assertEquals(false, PatternInsights.slots(doses, today, now).first { it.period == DayPeriod.EVENING }.hasSchedule)
    }

    @Test
    fun `요일별 빠뜨린 횟수는 월요일부터`() {
        // 11/22(일), 11/21(토) 빠뜨림
        val doses = listOf(dose(2, 8, DoseStatus.SKIPPED), dose(3, 8, DoseStatus.SKIPPED), dose(1, 8, DoseStatus.TAKEN))
        val week = PatternInsights.byWeekday(doses, today, now)
        assertEquals(DayOfWeek.MONDAY, week.first().first)
        assertEquals(1, week.first { it.first == DayOfWeek.SATURDAY }.second)
        assertEquals(1, week.first { it.first == DayOfWeek.SUNDAY }.second)
        assertEquals(0, week.first { it.first == DayOfWeek.MONDAY }.second)
    }

    @Test
    fun `자주 빠뜨린 약은 많이 빠뜨린 순이고 빠뜨린 적 없는 약은 빠진다`() {
        val doses = listOf(
            dose(1, 21, DoseStatus.PENDING, med = 2, name = "마그네슘"), dose(2, 21, DoseStatus.PENDING, med = 2, name = "마그네슘"),
            dose(1, 12, DoseStatus.SKIPPED, med = 3, name = "오메가"),
            dose(1, 8, DoseStatus.TAKEN, med = 4, name = "비타민")
        )
        val meds = PatternInsights.meds(doses, today, now)
        assertEquals(listOf("마그네슘", "오메가"), meds.map { it.name })
        assertEquals(LocalTime.of(21, 0), meds.first().time)
    }

    @Test
    fun `결론 카드는 가장 많이 빠뜨린 약과 시각이고 주말에 더 많은지 본다`() {
        val doses = listOf(
            dose(2, 21, DoseStatus.PENDING, med = 2, name = "마그네슘"),    // 일
            dose(3, 21, DoseStatus.PENDING, med = 2, name = "마그네슘"),    // 토
            dose(1, 21, DoseStatus.TAKEN, med = 2, name = "마그네슘"),      // 월
            dose(1, 12, DoseStatus.SKIPPED, med = 3, name = "오메가")
        )
        val worst = PatternInsights.worst(doses, today, now)!!
        assertEquals("마그네슘", worst.name)
        assertEquals(MissStats(2, 3), worst.stats)
        assertTrue(worst.weekendMore)
        assertNull(PatternInsights.worst(listOf(dose(1, 8, DoseStatus.TAKEN)), today, now))
    }

    @Test
    fun `최근 14일 칸은 다 먹음 일부 못 먹음 없음 진행 중으로 나뉜다`() {
        val doses = listOf(
            dose(3, 8, DoseStatus.TAKEN), dose(3, 21, DoseStatus.TAKEN),      // 다 먹음
            dose(2, 8, DoseStatus.TAKEN), dose(2, 21, DoseStatus.SKIPPED),    // 일부
            dose(1, 8, DoseStatus.PENDING),                                   // 못 먹음
            dose(0, 8, DoseStatus.TAKEN), dose(0, 21, DoseStatus.PENDING)     // 오늘 진행 중
        )
        val marks = PatternInsights.habitDays(doses, today, now).toMap()
        assertEquals(14, marks.size)
        assertEquals(HabitMark.ALL, marks[today.minusDays(3)])
        assertEquals(HabitMark.SOME, marks[today.minusDays(2)])
        assertEquals(HabitMark.NONE, marks[today.minusDays(1)])
        assertEquals(HabitMark.OPEN, marks[today])
        assertEquals(HabitMark.EMPTY, marks[today.minusDays(5)])
    }

    @Test
    fun `제시간은 예정 30분 안이고 일찍 먹은 것도 포함한다`() {
        val doses = listOf(
            dose(1, 8, DoseStatus.TAKEN, actedMinute = -10),
            dose(2, 8, DoseStatus.TAKEN, actedMinute = 30),
            dose(3, 8, DoseStatus.TAKEN, actedMinute = 45),
            dose(4, 8, DoseStatus.TAKEN, actedMinute = 90),
            dose(5, 8, DoseStatus.SKIPPED)
        )
        assertEquals(Punctuality(onTime = 2, late = 1, veryLate = 1), PatternInsights.punctuality(doses, today, zone))
        assertEquals(50, PatternInsights.punctuality(doses, today, zone).onTimeRate)
    }

    @Test
    fun `시간대별 평균 늦은 분은 먹은 회차만으로 낸다`() {
        val doses = listOf(
            dose(1, 8, DoseStatus.TAKEN, actedMinute = 10), dose(2, 8, DoseStatus.TAKEN, actedMinute = 20),
            dose(1, 21, DoseStatus.TAKEN, actedMinute = -6)
        )
        val avg = PatternInsights.averageDelay(doses, today, zone)
        assertEquals(15, avg[DayPeriod.MORNING])
        assertEquals(-6, avg[DayPeriod.NIGHT])
        assertNull(avg[DayPeriod.LUNCH])
    }

    @Test
    fun `6시간 넘게 차이 나는 기록은 나중에 몰아서 누른 것으로 보고 뺀다`() {
        val doses = listOf(dose(1, 8, DoseStatus.TAKEN, actedMinute = 20), dose(2, 8, DoseStatus.TAKEN, actedMinute = 60 * 30))
        assertEquals(Punctuality(onTime = 1), PatternInsights.punctuality(doses, today, zone))
        assertEquals(20, PatternInsights.averageDelay(doses, today, zone)[DayPeriod.MORNING])
    }
}
