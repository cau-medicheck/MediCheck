package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
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
    fun `기록은 32일 전부터 오늘까지 33일 약 3개라 99줄이다`() {
        assertEquals(-32, SampleData.START_OFFSET)
        assertEquals(33, SampleData.DAY_OFFSETS.count())
        assertEquals(99, SampleData.DAY_OFFSETS.count() * SampleData.MEDS.size)
    }

    @Test
    fun `전체는 완료 73 건너뜀 12 미기록 14다`() {
        val counts = countsOf(SampleData.DAY_OFFSETS)
        assertEquals(73, counts[DoseStatus.TAKEN])
        assertEquals(12, counts[DoseStatus.SKIPPED])
        assertEquals(14, counts[DoseStatus.PENDING])
    }

    @Test
    fun `어제까지 32일은 완료 72 건너뜀 12 미기록 12다`() {
        val counts = countsOf(-32..-1)
        assertEquals(72, counts[DoseStatus.TAKEN])
        assertEquals(12, counts[DoseStatus.SKIPPED])
        assertEquals(12, counts[DoseStatus.PENDING])
    }

    @Test
    fun `오늘은 비타민 D만 복용 완료다`() {
        assertEquals(
            listOf(DoseStatus.TAKEN, DoseStatus.PENDING, DoseStatus.PENDING),
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
    fun `샘플 약은 v3 fresh와 같은 3개다`() {
        assertEquals(listOf("비타민 D", "오메가3", "마그네슘"), SampleData.MEDS.map { it.name })
        assertEquals(listOf("08:00", "12:30", "21:00"), SampleData.MEDS.map { it.time })
        assertEquals("MON,TUE,WED,THU,FRI,SAT,SUN", SampleData.REPEAT_DAYS)
    }
}
