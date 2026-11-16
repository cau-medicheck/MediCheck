package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 화면에 보여 줄 상태 계산 테스트. (docs/DB_DESIGN.md ⑩)
 * "깜빡함(MISSED)"은 저장하지 않고, PENDING인 기록의 예정 일시와 지금을 비교해서 정한다.
 */
class DoseItemTest {

    private val date = LocalDate.of(2026, 11, 16)

    /** 2026-11-16 08:00 예정인 기록 */
    private fun dose(status: DoseStatus) = DoseItem(
        id = 1, medicationId = 1, medicationName = "타이레놀", dosage = "1정",
        date = date, time = LocalTime.of(8, 0), status = status, actedAt = null
    )

    private fun at(hour: Int, minute: Int = 0, onDate: LocalDate = date) =
        LocalDateTime.of(onDate, LocalTime.of(hour, minute))

    @Test
    fun `예정 시각 전의 PENDING은 예정이다`() {
        assertEquals(DoseDisplayStatus.UPCOMING, dose(DoseStatus.PENDING).displayStatus(at(7, 59)))
    }

    @Test
    fun `예정 시각 정각의 PENDING은 아직 예정이다`() {
        assertEquals(DoseDisplayStatus.UPCOMING, dose(DoseStatus.PENDING).displayStatus(at(8, 0)))
    }

    @Test
    fun `예정 시각이 지난 PENDING은 깜빡함이다`() {
        assertEquals(DoseDisplayStatus.MISSED, dose(DoseStatus.PENDING).displayStatus(at(8, 1)))
    }

    @Test
    fun `다음 날이 되면 전날의 PENDING은 시각과 상관없이 깜빡함이다`() {
        val nextDayEarlyMorning = at(6, 0, onDate = date.plusDays(1))

        assertEquals(DoseDisplayStatus.MISSED, dose(DoseStatus.PENDING).displayStatus(nextDayEarlyMorning))
    }

    @Test
    fun `복용 완료와 건너뜀은 시각과 상관없이 그대로다`() {
        assertEquals(DoseDisplayStatus.TAKEN, dose(DoseStatus.TAKEN).displayStatus(at(7, 0)))
        assertEquals(DoseDisplayStatus.TAKEN, dose(DoseStatus.TAKEN).displayStatus(at(23, 0)))
        assertEquals(DoseDisplayStatus.SKIPPED, dose(DoseStatus.SKIPPED).displayStatus(at(7, 0)))
        assertEquals(DoseDisplayStatus.SKIPPED, dose(DoseStatus.SKIPPED).displayStatus(at(23, 0)))
    }
}
