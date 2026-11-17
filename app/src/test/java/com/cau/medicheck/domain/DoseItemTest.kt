package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * 화면에 보여 줄 상태 계산 테스트. (docs/DB_DESIGN.md ⑩)
 * "기한 경과 · 미기록(MISSED)"은 저장하지 않고, PENDING인 기록의 예정 일시와 지금을 비교해서 정한다.
 */
class DoseItemTest {

    private val date = LocalDate.of(2026, 11, 16)

    /** 2026-11-16 08:00 예정인 기록 */
    private fun dose(status: DoseStatus, actedAt: Long? = null) = DoseItem(
        recordId = 1, scheduleId = 1, medicationId = 1, medicationName = "타이레놀", dosage = "1정", memo = "",
        date = date, time = LocalTime.of(8, 0), status = status, actedAt = actedAt
    )

    private fun at(hour: Int, minute: Int = 0, onDate: LocalDate = date) =
        LocalDateTime.of(onDate, LocalTime.of(hour, minute))

    @Test
    fun `예정 시각 전의 PENDING은 예정이다`() {
        assertEquals(DoseDisplayStatus.UPCOMING, dose(DoseStatus.PENDING).displayStatus(at(7, 59)))
    }

    @Test
    fun `예정 시각 정각부터 PENDING은 기한 경과다`() {
        assertEquals(DoseDisplayStatus.MISSED, dose(DoseStatus.PENDING).displayStatus(at(8, 0)))
        // 초가 붙어 있어도 분 단위로 본다 (08:00:30도 08:00과 같다)
        assertEquals(DoseDisplayStatus.MISSED, dose(DoseStatus.PENDING).displayStatus(at(8, 0).plusSeconds(30)))
        assertEquals(DoseDisplayStatus.UPCOMING, dose(DoseStatus.PENDING).displayStatus(at(7, 59).plusSeconds(59)))
    }

    @Test
    fun `예정 시각이 지난 PENDING은 기한 경과다`() {
        assertEquals(DoseDisplayStatus.MISSED, dose(DoseStatus.PENDING).displayStatus(at(8, 1)))
    }

    @Test
    fun `도래 여부는 상태와 상관없이 예정 일시만 본다`() {
        assertFalse(dose(DoseStatus.TAKEN).isDue(at(7, 59)))
        assertTrue(dose(DoseStatus.TAKEN).isDue(at(8, 0)))
        assertTrue(dose(DoseStatus.PENDING).isDue(at(0, 0, onDate = date.plusDays(1))))
    }

    @Test
    fun `실제 복용 시각은 복용 완료이고 같은 날짜일 때만 보여 준다`() {
        val zone = ZoneOffset.UTC
        val sameDay = LocalDateTime.of(date, LocalTime.of(7, 50, 40)).toInstant(zone).toEpochMilli()
        val nextDay = LocalDateTime.of(date.plusDays(1), LocalTime.of(9, 0)).toInstant(zone).toEpochMilli()

        assertEquals(LocalTime.of(7, 50), dose(DoseStatus.TAKEN, sameDay).actualTimeOrNull(zone))
        // 다음 날 누른 복용 완료는 "실제 복용 시각"이 아니다
        assertNull(dose(DoseStatus.TAKEN, nextDay).actualTimeOrNull(zone))
        // 건너뜀·미기록은 보여 주지 않는다
        assertNull(dose(DoseStatus.SKIPPED, sameDay).actualTimeOrNull(zone))
        assertNull(dose(DoseStatus.PENDING).actualTimeOrNull(zone))
    }

    @Test
    fun `다음 날이 되면 전날의 PENDING은 시각과 상관없이 기한 경과다`() {
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
