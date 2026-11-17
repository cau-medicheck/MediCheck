package com.cau.medicheck.ui.navigation

import com.cau.medicheck.domain.DoseKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * 선택 날짜 저장소 테스트.
 * 시계는 2026-11-16(월) 10:00에 멈춰 둔다.
 */
class SelectionStoreTest {

    private val today = LocalDate.of(2026, 11, 16)
    private val clock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC)
    private val store = SelectionStore(clock)

    @Test
    fun `처음에는 오늘과 이번 달이다`() {
        assertEquals(today, store.selectedDay.value)
        assertEquals(YearMonth.of(2026, 11), store.selectedMonth.value)
        assertNull(store.lastDose.value)
    }

    @Test
    fun `날짜를 고르면 보고 있는 달도 그 날짜의 달이 된다`() {
        store.selectDay(LocalDate.of(2026, 12, 3))

        assertEquals(LocalDate.of(2026, 12, 3), store.selectedDay.value)
        assertEquals(YearMonth.of(2026, 12), store.selectedMonth.value)
    }

    @Test
    fun `달만 옮기면 선택 날짜는 그대로다`() {
        store.shiftMonth(-1)

        assertEquals(YearMonth.of(2026, 10), store.selectedMonth.value)
        assertEquals(today, store.selectedDay.value)
    }

    @Test
    fun `자정이 지나면 오늘을 보고 있던 선택은 새 오늘로 따라간다`() {
        store.followToday(today.plusDays(1))

        assertEquals(today.plusDays(1), store.selectedDay.value)
    }

    @Test
    fun `자정이 지나도 다른 날짜를 보고 있었으면 그대로 둔다`() {
        store.selectDay(today.minusDays(2))

        store.followToday(today.plusDays(1))

        assertEquals(today.minusDays(2), store.selectedDay.value)
    }

    @Test
    fun `어제를 본 뒤 자정이 두 번 지나도 엉뚱하게 따라가지 않는다`() {
        store.followToday(today.plusDays(1))            // 선택 = 17일
        store.selectDay(today)                          // 16일(어제)을 고름

        store.followToday(today.plusDays(2))            // 18일이 됨

        assertEquals(today, store.selectedDay.value)
    }

    @Test
    fun `reset은 선택 날짜와 마지막 회차를 처음 값으로 돌린다`() {
        store.selectDay(LocalDate.of(2026, 12, 3))
        store.setLastDose(DoseKey(1, today))

        store.reset()

        assertEquals(today, store.selectedDay.value)
        assertEquals(YearMonth.of(2026, 11), store.selectedMonth.value)
        assertNull(store.lastDose.value)
    }
}
