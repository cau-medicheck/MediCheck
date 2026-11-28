package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 샘플 계정 데이터의 상태 공식 테스트. (DB_DESIGN 부록 D, 2026-11-17 결정 6: 시작일 32일 전)
 * 개수는 v3 fresh()의 공식을 python으로 돌려 센 값과 같다.
 */
class SampleDataTest {

    /** 주어진 날짜 범위(오늘 기준 며칠 전)의 상태별 개수 */
    private fun countsOf(offsets: IntRange): Map<DoseStatus, Int> =
        offsets.flatMap { offset -> SampleData.MEDS.map { SampleData.statusFor(offset, it.no) } }
            .groupingBy { it }.eachCount()

    @Test
    fun `기록은 32일 전부터 오늘까지 33일 약 5개라 165줄이다`() {
        assertEquals(-32, SampleData.START_OFFSET)
        assertEquals(33, SampleData.DAY_OFFSETS.count())
        assertEquals(165, SampleData.DAY_OFFSETS.count() * SampleData.MEDS.size)
    }

    @Test
    fun `전체는 완료 122 건너뜀 20 미기록 23이다`() {
        val counts = countsOf(SampleData.DAY_OFFSETS)
        assertEquals(122, counts[DoseStatus.TAKEN])
        assertEquals(20, counts[DoseStatus.SKIPPED])
        assertEquals(23, counts[DoseStatus.PENDING])
    }

    @Test
    fun `어제까지 32일은 완료 121 건너뜀 20 미기록 19다`() {
        val counts = countsOf(-32..-1)
        assertEquals(121, counts[DoseStatus.TAKEN])
        assertEquals(20, counts[DoseStatus.SKIPPED])
        assertEquals(19, counts[DoseStatus.PENDING])
    }

    @Test
    fun `오늘은 비타민 D만 복용 완료다`() {
        assertEquals(
            listOf(DoseStatus.TAKEN, DoseStatus.PENDING, DoseStatus.PENDING, DoseStatus.PENDING, DoseStatus.PENDING),
            SampleData.MEDS.map { SampleData.statusFor(0, it.no) }
        )
    }

    @Test
    fun `어제 비타민 D는 건너뜀이고 8일 전 비타민 D는 미기록이다`() {
        assertEquals(DoseStatus.SKIPPED, SampleData.statusFor(-1, 1))
        assertEquals(DoseStatus.PENDING, SampleData.statusFor(-8, 1))
        assertEquals(DoseStatus.TAKEN, SampleData.statusFor(-1, 2))
    }

    @Test
    fun `샘플 약은 v3 fresh의 3개에 08시 약 2개를 더한 5개다`() {
        assertEquals(listOf("비타민 D", "오메가3", "마그네슘", "루테인", "유산균"), SampleData.MEDS.map { it.name })
        assertEquals(listOf("08:00", "12:30", "21:00", "08:00", "08:00"), SampleData.MEDS.map { it.time })
        assertEquals("MON,TUE,WED,THU,FRI,SAT,SUN", SampleData.REPEAT_DAYS)
    }

    @Test
    fun `영어 샘플 약은 이름과 복용량만 다르고 나머지는 같다`() {
        val english = SampleData.meds(korean = false)
        assertEquals(listOf("Vitamin D", "Omega-3", "Magnesium", "Lutein", "Probiotics"), english.map { it.name })
        assertEquals(listOf("1 tablet", "1 capsule", "1 tablet", "1 capsule", "2 capsules"), english.map { it.dosage })
        assertEquals(SampleData.MEDS.map { it.time to it.condition }, english.map { it.time to it.condition })
        assertEquals(SampleData.MEDS, SampleData.meds(korean = true))
        assertEquals("User", SampleData.displayName(korean = false))
    }

    @Test
    fun `먹은 시각은 예정보다 10분 일찍부터 76분 늦게 사이이고 오늘 기록에는 없다`() {
        val delays = (-32..-1).flatMap { offset -> SampleData.MEDS.map { SampleData.actedDelayMinutes(offset, it.no)!! } }
        assertTrue(delays.all { it in -10..76 })
        assertTrue(delays.any { it <= 30 } && delays.any { it > 30 })
        assertNull(SampleData.actedDelayMinutes(0, 1))
    }
}
