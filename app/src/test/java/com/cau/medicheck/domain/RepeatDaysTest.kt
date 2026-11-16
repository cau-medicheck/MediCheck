package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY

class RepeatDaysTest {

    @Test
    fun `요일 묶음을 쉼표로 이은 글자로 바꾼다`() {
        assertEquals("MON,WED,FRI", RepeatDays.encode(setOf(MONDAY, WEDNESDAY, FRIDAY)))
    }

    @Test
    fun `어떤 순서로 넣어도 월요일부터 순서대로 적는다`() {
        assertEquals("MON,FRI,SUN", RepeatDays.encode(setOf(SUNDAY, FRIDAY, MONDAY)))
    }

    @Test
    fun `매일은 일곱 요일 전부다`() {
        assertEquals("MON,TUE,WED,THU,FRI,SAT,SUN", RepeatDays.encode(RepeatDays.EVERY_DAY))
    }

    @Test
    fun `글자를 요일 묶음으로 되돌린다`() {
        assertEquals(setOf(MONDAY, WEDNESDAY, FRIDAY), RepeatDays.decode("MON,WED,FRI"))
    }

    @Test
    fun `바꿨다가 되돌리면 원래 묶음과 같다`() {
        val days = setOf(MONDAY, SUNDAY)

        assertEquals(days, RepeatDays.decode(RepeatDays.encode(days)))
        assertEquals(RepeatDays.EVERY_DAY, RepeatDays.decode(RepeatDays.encode(RepeatDays.EVERY_DAY)))
    }

    @Test
    fun `공백과 소문자와 모르는 글자는 너그럽게 처리한다`() {
        assertEquals(setOf(MONDAY, FRIDAY), RepeatDays.decode(" mon , XYZ,FRI "))
        assertEquals(emptySet<java.time.DayOfWeek>(), RepeatDays.decode(""))
    }
}
