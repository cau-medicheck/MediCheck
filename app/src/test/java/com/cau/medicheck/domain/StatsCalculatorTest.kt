package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/**
 * 통계 계산 테스트: 완료율, 최근 7일, 월간, 건너뜀 패턴. (docs/DB_DESIGN.md ⑩)
 * 기준: "지금"은 2026-11-18(수) 12:00.
 */
class StatsCalculatorTest {

    private val now = LocalDateTime.of(2026, 11, 18, 12, 0)
    private var nextId = 1L

    private fun dose(dayOfMonth: Int, hour: Int, status: DoseStatus) = DoseItem(
        recordId = nextId++, scheduleId = 1, medicationId = 1, medicationName = "타이레놀", dosage = "1정", memo = "",
        date = LocalDate.of(2026, 11, dayOfMonth), time = LocalTime.of(hour, 0),
        status = status, actedAt = null
    )

    // ---------- 완료율 ----------

    @Test
    fun `완료율은 복용 완료를 먹었어야 할 횟수로 나눈 값이다`() {
        // 복용 3, 건너뜀 1, 깜빡함 1 → 3 ÷ 5 = 60%
        assertEquals(60, DoseCounts(taken = 3, skipped = 1, missed = 1).completionRate)
    }

    @Test
    fun `아직 시간이 안 된 복약은 완료율에 넣지 않는다`() {
        // 예정 10건이 있어도 분모는 복용 1 + 깜빡함 1 = 2 → 50%
        assertEquals(50, DoseCounts(taken = 1, missed = 1, upcoming = 10).completionRate)
    }

    @Test
    fun `건너뜀도 먹었어야 할 횟수에 들어간다`() {
        assertEquals(50, DoseCounts(taken = 1, skipped = 1).completionRate)
    }

    @Test
    fun `먹었어야 할 횟수가 0이면 완료율은 없다`() {
        assertNull(DoseCounts().completionRate)
        assertNull(DoseCounts(upcoming = 5).completionRate)
    }

    @Test
    fun `전부 먹었으면 100이고 하나도 안 먹었으면 0이다`() {
        assertEquals(100, DoseCounts(taken = 4).completionRate)
        assertEquals(0, DoseCounts(missed = 2, skipped = 1).completionRate)
    }

    @Test
    fun `완료율의 소수점은 반올림한다`() {
        assertEquals(67, DoseCounts(taken = 2, missed = 1).completionRate)   // 66.67 → 67
        assertEquals(33, DoseCounts(taken = 1, missed = 2).completionRate)   // 33.33 → 33
    }

    // ---------- 상태별로 세기 ----------

    @Test
    fun `PENDING은 시각이 지났으면 기한 경과로 아직이면 예정으로 센다`() {
        val doses = listOf(
            dose(18, 8, DoseStatus.PENDING),    // 오늘 08:00, 지금은 12:00 → 기한 경과
            dose(18, 20, DoseStatus.PENDING),   // 오늘 20:00 → 예정
            dose(17, 20, DoseStatus.PENDING)    // 어제 → 기한 경과
        )

        assertEquals(DoseCounts(missed = 2, upcoming = 1), StatsCalculator.count(doses, now))
    }

    @Test
    fun `아직 시각 전인 회차는 미리 복용 완료해도 예정으로만 센다`() {
        val doses = listOf(
            dose(18, 8, DoseStatus.TAKEN),      // 오늘 08:00 → 도래함, 복용 완료
            dose(18, 20, DoseStatus.TAKEN),     // 오늘 20:00을 미리 복용 완료 → 20:00까지는 완료율에 안 들어감
            dose(19, 8, DoseStatus.SKIPPED)     // 내일 → 예정
        )

        val counts = StatsCalculator.count(doses, now)

        assertEquals(DoseCounts(taken = 1, upcoming = 2), counts)
        assertEquals(100, counts.completionRate)
    }

    @Test
    fun `예정 시각과 같은 분이면 도래한 것으로 센다`() {
        val doses = listOf(dose(18, 12, DoseStatus.PENDING))   // 지금이 12:00

        assertEquals(DoseCounts(missed = 1), StatsCalculator.count(doses, now))
    }

    // ---------- 최근 7일 (주간 통계 S13) ----------

    private val today = LocalDate.of(2026, 11, 18)

    /** 날짜·시각을 자유롭게 정하는 회차 */
    private fun doseOn(date: LocalDate, hour: Int, minute: Int, status: DoseStatus) = DoseItem(
        recordId = nextId++, scheduleId = 1, medicationId = 1, medicationName = "타이레놀", dosage = "1정", memo = "",
        date = date, time = LocalTime.of(hour, minute), status = status, actedAt = null
    )

    @Test
    fun `최근 7일은 오늘을 포함해 6일 전부터다`() {
        val stats = StatsCalculator.recentWeek(emptyList(), today, now)

        assertEquals(7, stats.days.size)
        assertEquals(LocalDate.of(2026, 11, 12), stats.days.first().date)
        assertEquals(today, stats.days.last().date)
    }

    @Test
    fun `기록은 날짜별로 나뉘어 세어진다`() {
        val doses = listOf(
            dose(16, 8, DoseStatus.TAKEN), dose(16, 20, DoseStatus.TAKEN),      // 16일: 2/2
            dose(17, 8, DoseStatus.TAKEN), dose(17, 20, DoseStatus.PENDING),    // 17일: 1/2 (기한 경과 1)
            dose(18, 8, DoseStatus.SKIPPED), dose(18, 20, DoseStatus.PENDING)   // 18일(오늘): 0/1 (건너뜀 1, 예정 1)
        )

        val days = StatsCalculator.recentWeek(doses, today, now).days

        // days[4] = 16일, days[5] = 17일, days[6] = 18일(오늘)
        assertEquals(DoseCounts(taken = 2), days[4].counts)
        assertEquals(DoseCounts(taken = 1, missed = 1), days[5].counts)
        assertEquals(DoseCounts(skipped = 1, upcoming = 1), days[6].counts)
        assertEquals(100, days[4].counts.completionRate)
        assertEquals(50, days[5].counts.completionRate)
        assertEquals(0, days[6].counts.completionRate)
    }

    @Test
    fun `최근 7일 합계는 7일을 더한 값이다`() {
        val doses = listOf(
            dose(16, 8, DoseStatus.TAKEN), dose(16, 20, DoseStatus.TAKEN),
            dose(17, 8, DoseStatus.TAKEN), dose(17, 20, DoseStatus.PENDING),
            dose(18, 8, DoseStatus.SKIPPED), dose(18, 20, DoseStatus.PENDING)
        )

        val total = StatsCalculator.recentWeek(doses, today, now).total

        // 복용 3, 기한 경과 1, 건너뜀 1, 예정 1 → 3 ÷ 5 = 60%
        assertEquals(DoseCounts(taken = 3, skipped = 1, missed = 1, upcoming = 1), total)
        assertEquals(60, total.completionRate)
    }

    @Test
    fun `기록이 없는 날은 빈 값이고 완료율이 없다`() {
        val days = StatsCalculator.recentWeek(listOf(dose(16, 8, DoseStatus.TAKEN)), today, now).days

        assertEquals(DoseCounts(), days[3].counts)   // 15일
        assertNull(days[3].counts.completionRate)
    }

    @Test
    fun `최근 7일 밖의 기록은 세지 않는다`() {
        val doses = listOf(
            dose(11, 8, DoseStatus.TAKEN),    // 7일 전 → 범위 밖
            dose(12, 8, DoseStatus.TAKEN),    // 6일 전 → 범위의 첫날
            dose(19, 8, DoseStatus.TAKEN)     // 내일 → 범위 밖
        )

        assertEquals(DoseCounts(taken = 1), StatsCalculator.recentWeek(doses, today, now).total)
    }

    // ---------- 월간 (S14) ----------

    @Test
    fun `월간은 1일부터 말일까지이고 미래 날짜는 분모에 없다`() {
        val november = YearMonth.of(2026, 11)
        val doses = listOf(
            dose(1, 8, DoseStatus.TAKEN),
            dose(17, 8, DoseStatus.SKIPPED),
            dose(18, 8, DoseStatus.PENDING),    // 오늘 08:00 → 기한 경과
            dose(18, 20, DoseStatus.PENDING),   // 오늘 20:00 → 아직
            dose(30, 8, DoseStatus.PENDING)     // 미래 → 분모에 없음
        )

        val stats = StatsCalculator.month(doses, november, now)

        assertEquals(30, stats.days.size)
        assertEquals(LocalDate.of(2026, 11, 1), stats.days.first().date)
        assertEquals(LocalDate.of(2026, 11, 30), stats.days.last().date)
        assertEquals(DoseCounts(taken = 1, skipped = 1, missed = 1, upcoming = 2), stats.total)
        assertEquals(33, stats.total.completionRate)   // 1 ÷ 3
    }

    @Test
    fun `달마다 날짜 수가 다르다`() {
        assertEquals(28, StatsCalculator.month(emptyList(), YearMonth.of(2026, 2), now).days.size)
        assertEquals(29, StatsCalculator.month(emptyList(), YearMonth.of(2028, 2), now).days.size)   // 윤년
        assertEquals(31, StatsCalculator.month(emptyList(), YearMonth.of(2026, 12), now).days.size)
    }

    @Test
    fun `미래 달의 월간 완료율은 없다`() {
        val december = YearMonth.of(2026, 12)
        val doses = listOf(doseOn(LocalDate.of(2026, 12, 5), 8, 0, DoseStatus.PENDING))

        assertNull(StatsCalculator.month(doses, december, now).total.completionRate)
    }

    // ---------- 건너뜀 패턴 (S15) ----------

    @Test
    fun `패턴은 오늘에서 29일 전까지 세고 30일 전은 세지 않는다`() {
        val doses = listOf(
            doseOn(today.minusDays(29), 8, 0, DoseStatus.SKIPPED),
            doseOn(today.minusDays(30), 8, 0, DoseStatus.SKIPPED),
            doseOn(today, 8, 0, DoseStatus.SKIPPED)
        )

        assertEquals(2, StatsCalculator.skipPattern(doses, today, now).totalSkipped)
    }

    @Test
    fun `건너뜀은 예정 시각의 시간대로 나눈다`() {
        val yesterday = today.minusDays(1)
        val doses = listOf(
            doseOn(yesterday, 0, 0, DoseStatus.SKIPPED),                                                  // 새벽
            doseOn(yesterday, 5, 0, DoseStatus.SKIPPED), doseOn(yesterday, 10, 59, DoseStatus.SKIPPED),   // 아침
            doseOn(yesterday, 11, 0, DoseStatus.SKIPPED), doseOn(yesterday, 15, 59, DoseStatus.SKIPPED),  // 점심
            doseOn(yesterday, 16, 0, DoseStatus.SKIPPED), doseOn(yesterday, 20, 59, DoseStatus.SKIPPED),  // 저녁
            doseOn(yesterday, 21, 0, DoseStatus.SKIPPED)                                                  // 밤
        )

        val pattern = StatsCalculator.skipPattern(doses, today, now)

        assertEquals(
            mapOf(DayPeriod.DAWN to 1, DayPeriod.MORNING to 2, DayPeriod.LUNCH to 2, DayPeriod.EVENING to 2, DayPeriod.NIGHT to 1),
            pattern.skippedByPeriod
        )
    }

    @Test
    fun `기한 경과 미기록은 건너뜀과 따로 세고 시각 전인 것은 어디에도 넣지 않는다`() {
        val doses = listOf(
            doseOn(today.minusDays(1), 8, 0, DoseStatus.PENDING),   // 어제 미기록 → 기한 경과
            doseOn(today, 8, 0, DoseStatus.PENDING),                // 오늘 08:00 미기록 → 기한 경과 (지금 12:00)
            doseOn(today, 20, 0, DoseStatus.PENDING),               // 오늘 20:00 → 아직
            doseOn(today, 20, 0, DoseStatus.SKIPPED),               // 미리 건너뜀으로 눌러도 20:00 전에는 세지 않는다
            doseOn(today.minusDays(1), 8, 0, DoseStatus.SKIPPED),
            doseOn(today.minusDays(1), 8, 0, DoseStatus.TAKEN)
        )

        val pattern = StatsCalculator.skipPattern(doses, today, now)

        assertEquals(1, pattern.totalSkipped)
        assertEquals(2, pattern.overdueUnrecorded)
    }

    @Test
    fun `기록이 없으면 네 시간대 모두 0이다`() {
        val pattern = StatsCalculator.skipPattern(emptyList(), today, now)

        assertEquals(DayPeriod.entries.associateWith { 0 }, pattern.skippedByPeriod)
        assertEquals(0, pattern.overdueUnrecorded)
    }
}
