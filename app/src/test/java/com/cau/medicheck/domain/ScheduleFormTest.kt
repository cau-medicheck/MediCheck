package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalTime

/** 폼의 시간 줄 → 저장할 규칙으로 바꾸기 (통일 모드 / 혼합 요일 모드) 테스트 */
class ScheduleFormTest {

    private val monWedFri = setOf(MONDAY, WEDNESDAY, FRIDAY)
    private val eight = LocalTime.of(8, 0)
    private val twenty = LocalTime.of(20, 0)

    @Test
    fun `줄마다 고른 복용 조건이 그대로 저장할 규칙에 들어간다`() {
        val rows = listOf(FormTimeRow(1, eight, DoseCondition.BEFORE_MEAL), FormTimeRow(null, twenty))

        val drafts = toScheduleDrafts(rows, monWedFri, originalDays = null)

        assertEquals(listOf(DoseCondition.BEFORE_MEAL, DoseCondition.NONE), drafts.map { it.condition })
    }

    @Test
    fun `통일 모드에서는 모든 줄에 화면에서 고른 요일이 들어간다`() {
        val rows = listOf(FormTimeRow(1, eight), FormTimeRow(null, twenty))

        val drafts = toScheduleDrafts(rows, monWedFri, originalDays = null)

        assertEquals(listOf(monWedFri, monWedFri), drafts.map { it.days })
        assertEquals(listOf(1L, null), drafts.map { it.id })
        assertEquals(listOf(eight, twenty), drafts.map { it.time })
    }

    @Test
    fun `혼합 모드에서는 원래 줄은 자기 요일을 지키고 새 줄만 합집합을 받는다`() {
        val original = mapOf(1L to RepeatDays.EVERY_DAY, 2L to monWedFri)
        val rows = listOf(FormTimeRow(1, eight), FormTimeRow(2, LocalTime.of(21, 0)), FormTimeRow(null, twenty))

        val drafts = toScheduleDrafts(rows, RepeatDays.EVERY_DAY, original)

        assertEquals(RepeatDays.EVERY_DAY, drafts[0].days)
        assertEquals(monWedFri, drafts[1].days)            // 시각을 바꿔도 요일은 그대로
        assertEquals(RepeatDays.EVERY_DAY, drafts[2].days) // 새 줄 = 버튼에 보이는 합집합
    }

    @Test
    fun `혼합 모드에서 원래 줄끼리는 시각이 같아도 요일이 안 겹치면 검사를 통과한다`() {
        val original = mapOf(1L to setOf(MONDAY, WEDNESDAY), 2L to setOf(TUESDAY, THURSDAY))
        val rows = listOf(FormTimeRow(1, eight), FormTimeRow(2, eight))
        val draft = MedicationDraft(
            name = "타이레놀", dosage = "1정", startDate = LocalDate.of(2026, 11, 16), endDate = null, memo = "",
            schedules = toScheduleDrafts(rows, setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY), original)
        )

        assertNull(MedicationValidator.validate(draft))
    }

    @Test
    fun `요일이 서로 다른 규칙이 있을 때만 혼합이다`() {
        fun item(id: Long, days: Set<java.time.DayOfWeek>) = ScheduleItem(id, eight, days)

        assertFalse(hasMixedDays(listOf(item(1, monWedFri), item(2, monWedFri))))
        assertFalse(hasMixedDays(listOf(item(1, monWedFri))))
        assertFalse(hasMixedDays(emptyList()))
        assertTrue(hasMixedDays(listOf(item(1, RepeatDays.EVERY_DAY), item(2, monWedFri))))
    }
}
