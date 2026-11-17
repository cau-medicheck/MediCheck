package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** 약 상태(복용 중·일시중지·종료), 목록 순서, 이름 검색 테스트. 기준일: 2026-11-16 */
class MedicationStatusTest {

    private val today = LocalDate.of(2026, 11, 16)

    private fun med(
        id: Long,
        name: String = "약$id",
        endDate: LocalDate? = null,
        pausedFrom: LocalDate? = null,
        startDate: LocalDate = today.minusDays(10)
    ) = MedicationDetail(
        id = id, name = name, dosage = "1정", startDate = startDate, endDate = endDate, memo = "",
        schedules = listOf(ScheduleItem(id, LocalTime.of(8, 0), RepeatDays.EVERY_DAY)),
        pausedFrom = pausedFrom
    )

    @Test
    fun `종료일 당일은 아직 복용 중이고 다음 날부터 종료다`() {
        assertEquals(MedicationStatus.ACTIVE, medicationStatus(med(1, endDate = today), today))
        assertEquals(MedicationStatus.ENDED, medicationStatus(med(1, endDate = today.minusDays(1)), today))
        assertEquals(MedicationStatus.ACTIVE, medicationStatus(med(1), today))
    }

    @Test
    fun `일시중지 시작일이 있으면 일시중지이고 종료가 일시중지보다 우선이다`() {
        assertEquals(MedicationStatus.PAUSED, medicationStatus(med(1, pausedFrom = today), today))
        assertEquals(
            MedicationStatus.ENDED,
            medicationStatus(med(1, endDate = today.minusDays(1), pausedFrom = today.minusDays(3)), today)
        )
    }

    @Test
    fun `시작일이 미래인 약도 복용 중이다`() {
        assertEquals(MedicationStatus.ACTIVE, medicationStatus(med(1, startDate = today.plusDays(5)), today))
    }

    @Test
    fun `목록은 종료된 약을 뒤로 보내고 같은 묶음 안은 등록한 순서다`() {
        val meds = listOf(
            med(3),
            med(1, endDate = today.minusDays(1)),     // 종료
            med(2, pausedFrom = today),               // 일시중지는 자리를 옮기지 않는다
            med(4, endDate = today.minusDays(5))      // 종료
        )

        assertEquals(listOf(2L, 3L, 1L, 4L), sortForList(meds, today).map { it.id })
    }

    @Test
    fun `이름 검색은 대소문자와 검색어 앞뒤 공백을 무시한다`() {
        val meds = listOf(med(1, "비타민 D"), med(2, "오메가3"), med(3, "Vitamin C"))

        assertEquals(listOf(1L), filterByName(meds, "d").map { it.id })
        assertEquals(listOf(2L), filterByName(meds, " 3 ").map { it.id })
        assertEquals(listOf(3L), filterByName(meds, "VITA").map { it.id })
        assertEquals(3, filterByName(meds, "   ").size)
        assertEquals(0, filterByName(meds, "xyz").size)
    }
}
