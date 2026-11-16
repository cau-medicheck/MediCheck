package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

/**
 * "어느 날짜의 복용 기록을 만들어야 하는가" 계산 테스트. (docs/DB_DESIGN.md ⑨)
 * 기준일: 2026-11-16은 월요일이다.
 */
class DosePlannerTest {

    private val monday16 = LocalDate.of(2026, 11, 16)

    private fun dates(
        startDate: LocalDate,
        endDate: LocalDate? = null,
        repeatDays: Set<DayOfWeek> = RepeatDays.EVERY_DAY,
        lastCreatedDate: LocalDate? = null,
        today: LocalDate = monday16
    ) = DosePlanner.datesToCreate(startDate, endDate, repeatDays, lastCreatedDate, today)

    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 11, dayOfMonth)

    @Test
    fun `처음에는 시작일부터 오늘까지 만든다`() {
        assertEquals(listOf(day(14), day(15), day(16)), dates(startDate = day(14)))
    }

    @Test
    fun `오늘 시작한 약은 오늘 하루만 만든다`() {
        assertEquals(listOf(day(16)), dates(startDate = day(16)))
    }

    @Test
    fun `반복 요일에 해당하는 날만 만든다`() {
        // 11/9(월) ~ 11/16(월) 사이의 월·수·금
        assertEquals(
            listOf(day(9), day(11), day(13), day(16)),
            dates(startDate = day(9), repeatDays = setOf(MONDAY, WEDNESDAY, FRIDAY))
        )
    }

    @Test
    fun `이미 만든 날 다음 날부터 이어서 만든다`() {
        assertEquals(listOf(day(15), day(16)), dates(startDate = day(1), lastCreatedDate = day(14)))
    }

    @Test
    fun `며칠 동안 앱을 안 열었어도 비어 있는 날짜를 전부 채운다`() {
        // 11/11까지 만들고 12~15일은 앱을 안 열었다가 16일에 열었다.
        assertEquals(
            listOf(day(12), day(13), day(14), day(15), day(16)),
            dates(startDate = day(1), lastCreatedDate = day(11))
        )
    }

    @Test
    fun `오늘 치까지 이미 만들었으면 더 만들지 않는다`() {
        assertTrue(dates(startDate = day(1), lastCreatedDate = day(16)).isEmpty())
    }

    @Test
    fun `미래 날짜는 만들지 않는다`() {
        assertTrue(dates(startDate = day(17)).isEmpty())
        assertEquals(day(16), dates(startDate = day(10)).last())
    }

    @Test
    fun `종료일이 지났으면 종료일까지만 만든다`() {
        assertEquals(listOf(day(12), day(13), day(14)), dates(startDate = day(12), endDate = day(14)))
    }

    @Test
    fun `종료일이 아직 안 됐으면 오늘까지만 만든다`() {
        assertEquals(listOf(day(15), day(16)), dates(startDate = day(15), endDate = day(30)))
    }

    @Test
    fun `종료일까지 다 만든 뒤에는 더 만들지 않는다`() {
        assertTrue(dates(startDate = day(1), endDate = day(14), lastCreatedDate = day(14)).isEmpty())
    }

    @Test
    fun `시작일을 뒤로 미뤘으면 새 시작일부터 만든다`() {
        // 예전에 11/5까지 만들었지만, 시작일을 11/15로 바꾼 경우
        assertEquals(listOf(day(15), day(16)), dates(startDate = day(15), lastCreatedDate = day(5)))
    }
}
