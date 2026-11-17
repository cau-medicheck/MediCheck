package com.cau.medicheck.ui.components

import com.cau.medicheck.domain.RepeatDays
import com.cau.medicheck.domain.ScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalTime

/** 반복 요일 표기 테스트 (프로토타입: 7개 모두면 "매일", 아니면 일요일부터 "·"로 잇기) */
class McFormatTest {

    private val labels = listOf("일", "월", "화", "수", "목", "금", "토")

    private fun days(vararg day: java.time.DayOfWeek) = daysText(day.toSet(), "매일", labels)

    @Test
    fun `일곱 요일 모두면 매일이다`() {
        assertEquals("매일", daysText(RepeatDays.EVERY_DAY, "매일", labels))
    }

    @Test
    fun `일부 요일은 일요일부터 순서대로 가운뎃점으로 잇는다`() {
        assertEquals("월·수·금", days(FRIDAY, MONDAY, WEDNESDAY))
        assertEquals("일·토", days(SATURDAY, SUNDAY))
        assertEquals("수", days(WEDNESDAY))
    }

    @Test
    fun `모든 시간의 요일이 같으면 요일만 다르면 시간마다 적는다`() {
        val same = listOf(
            ScheduleItem(1, LocalTime.of(8, 0), setOf(MONDAY, WEDNESDAY, FRIDAY)),
            ScheduleItem(2, LocalTime.of(20, 0), setOf(MONDAY, WEDNESDAY, FRIDAY))
        )
        val mixed = listOf(
            ScheduleItem(1, LocalTime.of(8, 0), RepeatDays.EVERY_DAY),
            ScheduleItem(2, LocalTime.of(20, 0), setOf(MONDAY, WEDNESDAY, FRIDAY))
        )

        assertEquals("월·수·금", scheduleDaysText(same, "매일", labels))
        assertEquals("08:00 매일, 20:00 월·수·금", scheduleDaysText(mixed, "매일", labels))
    }
}
