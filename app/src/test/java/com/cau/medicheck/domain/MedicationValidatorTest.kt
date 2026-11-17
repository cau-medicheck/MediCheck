package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalTime

class MedicationValidatorTest {

    private val morning = ScheduleDraft(time = LocalTime.of(8, 0), days = RepeatDays.EVERY_DAY)

    /** 문제없는 입력. 테스트마다 한 칸만 바꿔서 그 칸의 규칙을 확인한다. */
    private fun draft(
        name: String = "타이레놀",
        dosage: String = "1정",
        startDate: LocalDate = LocalDate.of(2026, 11, 16),
        endDate: LocalDate? = null,
        memo: String = "",
        schedules: List<ScheduleDraft> = listOf(morning)
    ) = MedicationDraft(name, dosage, startDate, endDate, memo, schedules)

    private fun validate(draft: MedicationDraft) = MedicationValidator.validate(draft)

    @Test
    fun `규칙에 맞는 입력은 통과한다`() {
        assertNull(validate(draft()))
    }

    @Test
    fun `약 이름은 비어 있으면 안 되고 50자를 넘으면 안 된다`() {
        assertEquals(MedicationError.NAME_DOSAGE_TIME_INVALID, validate(draft(name = "")))
        assertEquals(MedicationError.NAME_DOSAGE_TIME_INVALID, validate(draft(name = "   ")))
        assertNull(validate(draft(name = "가".repeat(50))))
        assertEquals(MedicationError.NAME_DOSAGE_TIME_INVALID, validate(draft(name = "가".repeat(51))))
    }

    @Test
    fun `복용량은 비어 있으면 안 되고 30자를 넘으면 안 된다`() {
        assertEquals(MedicationError.NAME_DOSAGE_TIME_INVALID, validate(draft(dosage = " ")))
        assertNull(validate(draft(dosage = "1".repeat(30))))
        assertEquals(MedicationError.NAME_DOSAGE_TIME_INVALID, validate(draft(dosage = "1".repeat(31))))
    }

    @Test
    fun `메모는 비워도 되지만 200자를 넘으면 안 된다`() {
        assertNull(validate(draft(memo = "")))
        assertNull(validate(draft(memo = "가".repeat(200))))
        assertEquals(MedicationError.NAME_DOSAGE_TIME_INVALID, validate(draft(memo = "가".repeat(201))))
    }

    @Test
    fun `종료일은 없어도 되고 시작일과 같아도 되지만 더 빠르면 안 된다`() {
        val start = LocalDate.of(2026, 11, 16)

        assertNull(validate(draft(startDate = start, endDate = null)))
        assertNull(validate(draft(startDate = start, endDate = start)))
        assertNull(validate(draft(startDate = start, endDate = start.plusDays(7))))
        assertEquals(
            MedicationError.DATES_INVALID,
            validate(draft(startDate = start, endDate = start.minusDays(1)))
        )
    }

    @Test
    fun `복용 시간이 하나도 없으면 안 된다`() {
        assertEquals(MedicationError.NAME_DOSAGE_TIME_INVALID, validate(draft(schedules = emptyList())))
    }

    @Test
    fun `요일을 하나도 고르지 않은 복용 시간이 있으면 안 된다`() {
        val noDays = ScheduleDraft(time = LocalTime.of(20, 0), days = emptySet())

        assertEquals(MedicationError.NO_DAYS, validate(draft(schedules = listOf(morning, noDays))))
    }

    @Test
    fun `같은 시각에 요일이 겹치면 안 된다`() {
        val sameTimeOverlap = ScheduleDraft(time = LocalTime.of(8, 0), days = setOf(MONDAY, WEDNESDAY))

        assertEquals(
            MedicationError.DUPLICATE_TIME,
            validate(draft(schedules = listOf(morning, sameTimeOverlap)))
        )
    }

    @Test
    fun `같은 시각이라도 요일이 겹치지 않으면 된다`() {
        val monWed = ScheduleDraft(time = LocalTime.of(8, 0), days = setOf(MONDAY, WEDNESDAY))
        val tueThu = ScheduleDraft(time = LocalTime.of(8, 0), days = setOf(TUESDAY, THURSDAY))

        assertNull(validate(draft(schedules = listOf(monWed, tueThu))))
    }

    @Test
    fun `시각이 다르면 요일이 같아도 된다`() {
        val evening = ScheduleDraft(time = LocalTime.of(20, 0), days = RepeatDays.EVERY_DAY)

        assertNull(validate(draft(schedules = listOf(morning, evening))))
    }

    @Test
    fun `여러 개가 틀리면 프로토타입의 검사 순서에서 가장 앞의 것 하나만 알려 준다`() {
        val noDays = ScheduleDraft(time = LocalTime.of(8, 0), days = emptySet())
        val start = LocalDate.of(2026, 11, 16)

        // ① 이름이 비었고 ② 요일도 없고 ③ 종료일도 앞 → ①
        assertEquals(
            MedicationError.NAME_DOSAGE_TIME_INVALID,
            validate(draft(name = "", schedules = listOf(noDays), startDate = start, endDate = start.minusDays(1)))
        )
        // ② 요일이 없고 ③ 종료일도 앞 → ②
        assertEquals(
            MedicationError.NO_DAYS,
            validate(draft(schedules = listOf(noDays), startDate = start, endDate = start.minusDays(1)))
        )
        // ③ 종료일이 앞이고 ④ 같은 시각이 두 줄 → ③
        assertEquals(
            MedicationError.DATES_INVALID,
            validate(draft(schedules = listOf(morning, morning), startDate = start, endDate = start.minusDays(1)))
        )
        // ④만 틀림
        assertEquals(MedicationError.DUPLICATE_TIME, validate(draft(schedules = listOf(morning, morning))))
    }
}
