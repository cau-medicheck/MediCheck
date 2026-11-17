package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        today: LocalDate = monday16,
        effectiveFrom: LocalDate? = null,
        pausedFrom: LocalDate? = null
    ) = DosePlanner.datesToCreate(startDate, endDate, repeatDays, lastCreatedDate, today, effectiveFrom, pausedFrom)

    private fun occurs(
        date: LocalDate,
        startDate: LocalDate = LocalDate.of(2026, 11, 1),
        endDate: LocalDate? = null,
        repeatDays: Set<DayOfWeek> = RepeatDays.EVERY_DAY,
        effectiveFrom: LocalDate? = null,
        pausedFrom: LocalDate? = null,
        today: LocalDate = monday16
    ) = DosePlanner.occursOn(date, startDate, endDate, repeatDays, effectiveFrom, pausedFrom, today)

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

    // ---------- occursOn: 적용 시작일 · 일시중지 (DB_DESIGN 9.1) ----------

    @Test
    fun `회차는 시작일부터 종료일까지 반복 요일에만 있다`() {
        assertFalse(occurs(day(9), startDate = day(10)))
        assertTrue(occurs(day(10), startDate = day(10)))
        assertTrue(occurs(day(14), endDate = day(14)))
        assertFalse(occurs(day(15), endDate = day(14)))
        assertTrue(occurs(day(16), repeatDays = setOf(MONDAY)))
        assertFalse(occurs(day(17), repeatDays = setOf(MONDAY)))
    }

    @Test
    fun `적용 시작일 전날까지는 회차가 없고 그날부터 있다`() {
        assertFalse(occurs(day(16), effectiveFrom = day(17)))
        assertTrue(occurs(day(17), effectiveFrom = day(17)))
    }

    @Test
    fun `일시중지 시작일부터는 회차가 없고 그 전날까지는 있다`() {
        assertTrue(occurs(day(13), pausedFrom = day(14)))
        assertFalse(occurs(day(14), pausedFrom = day(14)))
        assertFalse(occurs(day(20), pausedFrom = day(14)))
    }

    @Test
    fun `일시중지 시작일이 오늘보다 뒤여도 오늘과 미래에는 회차가 없다`() {
        // 시간대가 바뀌어 paused_from(17일)이 오늘(16일)보다 뒤처럼 보이는 경우
        assertTrue(occurs(day(15), pausedFrom = day(17)))
        assertFalse(occurs(day(16), pausedFrom = day(17)))
        assertFalse(occurs(day(18), pausedFrom = day(17)))
    }

    @Test
    fun `적용 시작일이 있으면 그 전 날짜는 만들지 않는다`() {
        assertEquals(listOf(day(15), day(16)), dates(startDate = day(10), effectiveFrom = day(15)))
        // 마지막으로 만든 날이 더 늦으면 그다음 날부터
        assertEquals(listOf(day(16)), dates(startDate = day(10), effectiveFrom = day(12), lastCreatedDate = day(15)))
    }

    @Test
    fun `적용 시작일이 내일이면 오늘은 만들지 않는다`() {
        assertTrue(dates(startDate = day(10), effectiveFrom = day(17)).isEmpty())
    }

    @Test
    fun `일시중지 중이면 중지 전날까지만 만든다`() {
        assertEquals(listOf(day(12), day(13)), dates(startDate = day(12), pausedFrom = day(14)))
        // 오늘 중지했으면 오늘 치도 만들지 않는다
        assertEquals(listOf(day(15)), dates(startDate = day(15), pausedFrom = day(16)))
    }
}
