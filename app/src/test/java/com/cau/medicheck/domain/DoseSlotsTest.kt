package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * 홈 맨 위 카드가 처음 보여 줄 시각 묶음 고르기(DoseSlots.pick) 테스트.
 * 오늘의 묶음: 08:00 · 12:30 · 21:00 (08:00과 12:30의 한가운데는 10:15, 12:30과 21:00의 한가운데는 16:45)
 */
class DoseSlotsTest {

    private val times = listOf(LocalTime.of(8, 0), LocalTime.of(12, 30), LocalTime.of(21, 0))

    private fun pick(now: String, vararg pending: Boolean) =
        DoseSlots.pick(times, pending.toList(), LocalTime.parse(now))

    @Test
    fun `가장 가까운 시각을 고른다`() {
        assertEquals(0, pick("07:30", true, true, true))
        assertEquals(0, pick("08:10", true, true, true))
        assertEquals(1, pick("11:50", true, true, true))
        assertEquals(2, pick("22:00", true, true, true))
    }

    @Test
    fun `한가운데를 넘으면 다음 시각이다`() {
        assertEquals(0, pick("10:14", true, true, true))
        assertEquals(1, pick("10:15", true, true, true))   // 딱 한가운데면 다음 시각
        assertEquals(1, pick("16:44", true, true, true))
        assertEquals(2, pick("16:45", true, true, true))
    }

    @Test
    fun `가까운 묶음을 다 기록했으면 다음 시각으로 넘어간다`() {
        assertEquals(1, pick("08:10", false, true, true))
        assertEquals(2, pick("08:10", false, false, true))
    }

    @Test
    fun `뒤에 남은 것이 없으면 앞쪽의 기록 안 한 묶음 중 가장 늦은 것`() {
        assertEquals(1, pick("21:30", true, true, false))
        assertEquals(0, pick("21:30", true, false, false))
    }

    @Test
    fun `모두 기록했으면 -1`() {
        assertEquals(-1, pick("12:00", false, false, false))
    }

    @Test
    fun `묶음이 하나뿐이면 그것을 고른다`() {
        assertEquals(0, DoseSlots.pick(listOf(LocalTime.of(9, 0)), listOf(true), LocalTime.of(23, 0)))
    }

    @Test
    fun `묶음이 없으면 -1`() {
        assertEquals(-1, DoseSlots.pick(emptyList(), emptyList(), LocalTime.NOON))
    }
}
