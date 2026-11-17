package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 주간 통계 계산 테스트. (docs/DB_DESIGN.md ⑩)
 * 기준: 2026-11-16은 월요일, "지금"은 수요일(18일) 12:00.
 */
class StatsCalculatorTest {

    private val monday = LocalDate.of(2026, 11, 16)
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

    // ---------- 한 주의 범위 ----------

    @Test
    fun `한 주는 월요일에 시작한다`() {
        assertEquals(monday, StatsCalculator.weekStartOf(LocalDate.of(2026, 11, 16)))   // 월요일 자신
        assertEquals(monday, StatsCalculator.weekStartOf(LocalDate.of(2026, 11, 18)))   // 수요일
        assertEquals(monday, StatsCalculator.weekStartOf(LocalDate.of(2026, 11, 22)))   // 일요일
        assertEquals(monday.plusDays(7), StatsCalculator.weekStartOf(LocalDate.of(2026, 11, 23)))
    }

    // ---------- 상태별로 세기 ----------

    @Test
    fun `PENDING은 시각이 지났으면 깜빡함으로 아직이면 예정으로 센다`() {
        val doses = listOf(
            dose(18, 8, DoseStatus.PENDING),    // 오늘 08:00, 지금은 12:00 → 깜빡함
            dose(18, 20, DoseStatus.PENDING),   // 오늘 20:00 → 예정
            dose(17, 20, DoseStatus.PENDING)    // 어제 → 깜빡함
        )

        assertEquals(DoseCounts(missed = 2, upcoming = 1), StatsCalculator.count(doses, now))
    }

    @Test
    fun `복용 완료와 건너뜀은 그대로 센다`() {
        val doses = listOf(
            dose(16, 8, DoseStatus.TAKEN),
            dose(17, 8, DoseStatus.TAKEN),
            dose(18, 8, DoseStatus.SKIPPED)
        )

        assertEquals(DoseCounts(taken = 2, skipped = 1), StatsCalculator.count(doses, now))
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
    fun `예정 시각 정각의 회차는 도래한 것으로 센다`() {
        val doses = listOf(dose(18, 12, DoseStatus.PENDING))   // 지금이 12:00

        assertEquals(DoseCounts(missed = 1), StatsCalculator.count(doses, now))
    }

    // ---------- 일주일 통계 ----------

    @Test
    fun `일주일 통계는 월요일부터 일요일까지 7일이다`() {
        val stats = StatsCalculator.weekStats(emptyList(), monday, now)

        assertEquals(7, stats.days.size)
        assertEquals(monday, stats.days.first().date)
        assertEquals(LocalDate.of(2026, 11, 22), stats.days.last().date)
        assertEquals(LocalDate.of(2026, 11, 22), stats.weekEnd)
    }

    @Test
    fun `기록은 날짜별로 나뉘어 세어진다`() {
        val doses = listOf(
            dose(16, 8, DoseStatus.TAKEN), dose(16, 20, DoseStatus.TAKEN),      // 월: 2/2
            dose(17, 8, DoseStatus.TAKEN), dose(17, 20, DoseStatus.PENDING),    // 화: 1/2 (깜빡함 1)
            dose(18, 8, DoseStatus.SKIPPED), dose(18, 20, DoseStatus.PENDING)   // 수: 0/1 (건너뜀 1, 예정 1)
        )

        val stats = StatsCalculator.weekStats(doses, monday, now)

        assertEquals(DoseCounts(taken = 2), stats.days[0].counts)
        assertEquals(DoseCounts(taken = 1, missed = 1), stats.days[1].counts)
        assertEquals(DoseCounts(skipped = 1, upcoming = 1), stats.days[2].counts)
        assertEquals(100, stats.days[0].counts.completionRate)
        assertEquals(50, stats.days[1].counts.completionRate)
        assertEquals(0, stats.days[2].counts.completionRate)
    }

    @Test
    fun `일주일 합계는 7일을 더한 값이다`() {
        val doses = listOf(
            dose(16, 8, DoseStatus.TAKEN), dose(16, 20, DoseStatus.TAKEN),
            dose(17, 8, DoseStatus.TAKEN), dose(17, 20, DoseStatus.PENDING),
            dose(18, 8, DoseStatus.SKIPPED), dose(18, 20, DoseStatus.PENDING)
        )

        val total = StatsCalculator.weekStats(doses, monday, now).total

        // 복용 3, 깜빡함 1, 건너뜀 1, 예정 1 → 3 ÷ 5 = 60%
        assertEquals(DoseCounts(taken = 3, skipped = 1, missed = 1, upcoming = 1), total)
        assertEquals(60, total.completionRate)
    }

    @Test
    fun `기록이 없는 날은 빈 값이고 완료율이 없다`() {
        val stats = StatsCalculator.weekStats(listOf(dose(16, 8, DoseStatus.TAKEN)), monday, now)

        assertEquals(DoseCounts(), stats.days[3].counts)
        assertNull(stats.days[3].counts.completionRate)
    }

    @Test
    fun `그 주 밖의 기록은 세지 않는다`() {
        val doses = listOf(
            dose(15, 8, DoseStatus.TAKEN),    // 지난주 일요일
            dose(16, 8, DoseStatus.TAKEN),    // 이번 주 월요일
            dose(23, 8, DoseStatus.TAKEN)     // 다음 주 월요일
        )

        assertEquals(DoseCounts(taken = 1), StatsCalculator.weekStats(doses, monday, now).total)
    }
}
