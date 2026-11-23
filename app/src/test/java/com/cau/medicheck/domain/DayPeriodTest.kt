package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** 시간대 묶음(새벽·아침·점심·저녁·밤)의 경계 테스트. 일정 화면(S05)이 이 묶음으로 회차를 나눈다. */
class DayPeriodTest {

    private fun period(hour: Int, minute: Int) = periodOf(LocalTime.of(hour, minute))

    @Test
    fun `0시부터 4시 59분까지는 새벽이다`() {
        assertEquals(DayPeriod.DAWN, period(0, 0))
        assertEquals(DayPeriod.DAWN, period(2, 0))
        assertEquals(DayPeriod.DAWN, period(4, 59))
    }

    @Test
    fun `5시부터 10시 59분까지는 아침이다`() {
        assertEquals(DayPeriod.MORNING, period(5, 0))
        assertEquals(DayPeriod.MORNING, period(10, 59))
    }

    @Test
    fun `11시부터 15시 59분까지는 점심이다`() {
        assertEquals(DayPeriod.LUNCH, period(11, 0))
        assertEquals(DayPeriod.LUNCH, period(15, 59))
    }

    @Test
    fun `16시부터 20시 59분까지는 저녁이다`() {
        assertEquals(DayPeriod.EVENING, period(16, 0))
        assertEquals(DayPeriod.EVENING, period(20, 59))
    }

    @Test
    fun `21시부터는 밤이다`() {
        assertEquals(DayPeriod.NIGHT, period(21, 0))
        assertEquals(DayPeriod.NIGHT, period(23, 59))
    }
}
