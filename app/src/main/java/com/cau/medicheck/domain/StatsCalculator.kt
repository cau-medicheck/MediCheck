package com.cau.medicheck.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/**
 * 복용 기록의 개수 묶음. 하루치에도, 일주일치에도 쓴다.
 *
 * @param upcoming 아직 예정 시각이 안 된 기록의 수. 완료율 계산에는 넣지 않는다.
 */
data class DoseCounts(
    val taken: Int = 0,
    val skipped: Int = 0,
    val missed: Int = 0,
    val upcoming: Int = 0
) {
    /** "먹었어야 할 횟수" = 완료율의 분모. 아직 시간이 안 된 것은 빠진다. */
    val due: Int get() = taken + skipped + missed

    /**
     * 완료율(%) = 복용 완료 ÷ (복용 완료 + 건너뜀 + 깜빡함) × 100. (docs/DB_DESIGN.md ⑩)
     * 먹었어야 할 횟수가 0이면 계산할 수 없으므로 null ("기록 없음"으로 표시한다).
     * 소수점은 반올림한다.
     */
    val completionRate: Int? get() = if (due == 0) null else Math.round(taken * 100f / due)

    operator fun plus(other: DoseCounts) = DoseCounts(
        taken = taken + other.taken,
        skipped = skipped + other.skipped,
        missed = missed + other.missed,
        upcoming = upcoming + other.upcoming
    )
}

/** 하루치 통계 */
data class DayStats(val date: LocalDate, val counts: DoseCounts)

/**
 * 일주일치 통계.
 * @param days 월요일부터 일요일까지 7개. 기록이 없는 날도 빈 값으로 들어 있다.
 */
data class WeekStats(val weekStart: LocalDate, val days: List<DayStats>) {
    val weekEnd: LocalDate get() = weekStart.plusDays(6)

    /** 일주일 전체의 합계 */
    val total: DoseCounts get() = days.fold(DoseCounts()) { sum, day -> sum + day.counts }
}

/**
 * 복용 기록 목록으로 통계를 계산한다. 순수 Kotlin 코드라서 단위 테스트를 할 수 있다.
 */
object StatsCalculator {

    /** 주어진 날짜가 속한 주의 월요일. 한 주는 월요일에 시작해 일요일에 끝난다. */
    fun weekStartOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * 기록들을 상태별로 센다.
     * "깜빡함"과 "예정"은 저장된 값이 아니라 지금 시각(now)과 비교해서 정해진다.
     */
    fun count(doses: List<DoseItem>, now: LocalDateTime): DoseCounts {
        var taken = 0
        var skipped = 0
        var missed = 0
        var upcoming = 0
        doses.forEach { dose ->
            when (dose.displayStatus(now)) {
                DoseDisplayStatus.TAKEN -> taken++
                DoseDisplayStatus.SKIPPED -> skipped++
                DoseDisplayStatus.MISSED -> missed++
                DoseDisplayStatus.UPCOMING -> upcoming++
            }
        }
        return DoseCounts(taken, skipped, missed, upcoming)
    }

    /**
     * weekStart(월요일)부터 7일간의 통계를 만든다.
     * doses에 그 주 밖의 기록이 섞여 있어도 무시한다.
     */
    fun weekStats(doses: List<DoseItem>, weekStart: LocalDate, now: LocalDateTime): WeekStats {
        val dosesByDate = doses.groupBy { it.date }
        val days = (0L..6L).map { offset ->
            val date = weekStart.plusDays(offset)
            DayStats(date, count(dosesByDate[date].orEmpty(), now))
        }
        return WeekStats(weekStart, days)
    }
}
