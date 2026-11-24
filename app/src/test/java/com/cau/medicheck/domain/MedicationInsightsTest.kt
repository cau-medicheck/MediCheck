package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 내 약 카드의 숫자: 최근 7일 복용률, 기간 진행, 상태 칩 거르기 */
class MedicationInsightsTest {

    private val today = LocalDate.of(2026, 11, 24)
    private val now = LocalDateTime.of(today, LocalTime.of(12, 0))

    private fun dose(daysAgo: Long, status: DoseStatus, time: LocalTime = LocalTime.of(8, 0)) = DoseItem(
        recordId = daysAgo, scheduleId = 1, medicationId = 1, medicationName = "비타민", dosage = "1정", memo = "",
        date = today.minusDays(daysAgo), time = time, status = status, actedAt = null
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
}
