package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 내 약 카드의 숫자: 최근 7일 복용률, 기간 진행, 상태 칩 거르기 */
class MedicationInsightsTest {

    private val today = LocalDate.of(2026, 11, 24)
    private val now = LocalDateTime.of(today, LocalTime.of(12, 0))

    private fun dose(daysAgo: Long, status: DoseStatus, time: LocalTime = LocalTime.of(8, 0), scheduleId: Long = 1) = DoseItem(
        recordId = daysAgo, scheduleId = scheduleId, medicationId = 1, medicationName = "비타민", dosage = "1정", memo = "",
        date = today.minusDays(daysAgo), time = time, status = status, actedAt = null
    )

    /** 매일 08:00·20:00에 먹는 약 */
    private val med = MedicationDetail(
        id = 1, name = "비타민", dosage = "1정", startDate = today.minusDays(30), endDate = null, memo = "",
        schedules = listOf(
            ScheduleItem(1, LocalTime.of(8, 0), DayOfWeek.entries.toSet()),
            ScheduleItem(2, LocalTime.of(20, 0), DayOfWeek.entries.toSet())
        )
    )

    @Test
    fun `최근 7일 복용률은 도래한 회차 중 복용 완료 비율이다`() {
        val doses = listOf(
            dose(0, DoseStatus.TAKEN), dose(1, DoseStatus.TAKEN), dose(2, DoseStatus.SKIPPED), dose(3, DoseStatus.PENDING)
        )
        assertEquals(50, MedicationInsights.recentRate(doses, now))   // 4회 중 2회
    }

    @Test
    fun `7일보다 오래된 회차와 아직 시각 전인 회차는 세지 않는다`() {
        val doses = listOf(
            dose(7, DoseStatus.SKIPPED),                              // 8일 전 → 빠짐
            dose(6, DoseStatus.TAKEN),                                // 7일 전(오늘 포함 7일의 첫날) → 셈
            dose(0, DoseStatus.PENDING, time = LocalTime.of(21, 0))   // 오늘 21:00 → 아직 시각 전
        )
        assertEquals(100, MedicationInsights.recentRate(doses, now))
    }

    @Test
    fun `계산할 회차가 없으면 null이다`() {
        assertNull(MedicationInsights.recentRate(emptyList(), now))
        assertNull(MedicationInsights.recentRate(listOf(dose(0, DoseStatus.PENDING, time = LocalTime.of(20, 0))), now))
    }

    @Test
    fun `종료일이 있으면 며칠째와 남은 날을 센다`() {
        // 11/12 ~ 12/11 (30일), 오늘 11/24는 13일째, 17일 남음
        val p = MedicationInsights.courseProgress(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 12, 11), today)
        assertEquals(CourseProgress(day = 13, total = 30, daysLeft = 17), p)
    }

    @Test
    fun `시작일과 종료일 당일도 기간 안이다`() {
        assertEquals(1, MedicationInsights.courseProgress(today, today.plusDays(9), today)?.day)
        assertEquals(0, MedicationInsights.courseProgress(today.minusDays(9), today, today)?.daysLeft)
    }

    @Test
    fun `종료일이 없거나 기간 밖이면 보이지 않는다`() {
        assertNull(MedicationInsights.courseProgress(today, null, today))
        assertNull(MedicationInsights.courseProgress(today.plusDays(1), today.plusDays(5), today))   // 아직 시작 전
        assertNull(MedicationInsights.courseProgress(today.minusDays(5), today.minusDays(1), today)) // 이미 끝남
    }

    @Test
    fun `상태 칩으로 거른다`() {
        assertTrue(MedicationInsights.matches(MedicationStatus.PAUSED, MedicationFilter.ALL))
        assertTrue(MedicationInsights.matches(MedicationStatus.PAUSED, MedicationFilter.PAUSED))
        assertFalse(MedicationInsights.matches(MedicationStatus.ACTIVE, MedicationFilter.PAUSED))
        assertTrue(MedicationInsights.matches(MedicationStatus.ENDED, MedicationFilter.ENDED))
    }

    @Test
    fun `요일 동그라미는 6일 전부터 오늘까지 7개다`() {
        val days = MedicationInsights.recentDays(emptyList(), now)
        assertEquals(7, days.size)
        assertEquals(today.minusDays(6), days.first().date)
        assertEquals(today, days.last().date)
        assertTrue(days.all { it.mark == DayMark.NONE })
    }

    @Test
    fun `하루에 여러 회차면 미복용 - 예정 - 건너뜀 - 먹음 순으로 정한다`() {
        val doses = listOf(
            dose(3, DoseStatus.TAKEN), dose(3, DoseStatus.PENDING, scheduleId = 2),                 // 지난날 기록 전 → 미복용
            dose(2, DoseStatus.TAKEN), dose(2, DoseStatus.SKIPPED, scheduleId = 2),                 // 건너뜀
            dose(1, DoseStatus.TAKEN), dose(1, DoseStatus.TAKEN, scheduleId = 2),                   // 다 먹음
            dose(0, DoseStatus.TAKEN), dose(0, DoseStatus.PENDING, LocalTime.of(20, 0), 2)          // 오늘 밤 회차 남음 → 예정
        )
        val marks = MedicationInsights.recentDays(doses, now).associate { it.date to it.mark }
        assertEquals(DayMark.MISSED, marks[today.minusDays(3)])
        assertEquals(DayMark.SKIPPED, marks[today.minusDays(2)])
        assertEquals(DayMark.TAKEN, marks[today.minusDays(1)])
        assertEquals(DayMark.UPCOMING, marks[today])
        assertEquals(DayMark.NONE, marks[today.minusDays(4)])
    }

    @Test
    fun `최근 7일 횟수는 시각이 된 회차만 센다`() {
        val doses = listOf(dose(1, DoseStatus.TAKEN), dose(1, DoseStatus.SKIPPED, scheduleId = 2), dose(0, DoseStatus.PENDING, LocalTime.of(20, 0), 2))
        val counts = MedicationInsights.recentCounts(doses, now)
        assertEquals(1, counts.taken)
        assertEquals(2, counts.due)
    }

    @Test
    fun `다음 복용은 오늘 아직 안 먹은 회차가 먼저다`() {
        val records = listOf(dose(0, DoseStatus.TAKEN), dose(0, DoseStatus.PENDING, LocalTime.of(20, 0), 2))
        val next = MedicationInsights.nextDose(med, records, now)
        assertEquals(today, next?.date)
        assertEquals(LocalTime.of(20, 0), next?.time)
    }

    @Test
    fun `오늘 회차가 끝났으면 내일 첫 회차다`() {
        val records = listOf(dose(0, DoseStatus.TAKEN), dose(0, DoseStatus.TAKEN, LocalTime.of(20, 0), 2))
        val next = MedicationInsights.nextDose(med, records, now)
        assertEquals(today.plusDays(1), next?.date)
        assertEquals(LocalTime.of(8, 0), next?.time)
    }

    @Test
    fun `미리 건너뛴 회차는 빼고 그다음을 찾는다`() {
        val tomorrowSkipped = dose(-1, DoseStatus.SKIPPED)   // 내일 08:00 미리 건너뜀
        val next = MedicationInsights.nextDose(med, listOf(tomorrowSkipped), LocalDateTime.of(today, LocalTime.of(21, 0)))
        assertEquals(today.plusDays(1), next?.date)
        assertEquals(LocalTime.of(20, 0), next?.time)
    }

    @Test
    fun `비활성화한 약은 다음 복용이 없다`() {
        assertNull(MedicationInsights.nextDose(med.copy(pausedFrom = today), emptyList(), now))
    }
}
