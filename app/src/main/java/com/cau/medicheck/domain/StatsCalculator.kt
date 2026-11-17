package com.cau.medicheck.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * 복용 기록의 개수 묶음. 하루치에도, 여러 날 치에도 쓴다.
 * 화면의 이름표: taken = "완료", skipped = "건너뜀", missed = "미기록"(기한 경과 · 미기록).
 *
 * @param upcoming 아직 예정 시각이 안 된 회차의 수(미리 복용 완료를 눌러 둔 것 포함). 완료율 계산에는 넣지 않는다.
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
     * 도래한 회차가 0이면 계산할 수 없으므로 null (화면에는 "N/A"로 표시한다).
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
 * 어떤 기간의 통계.
 * @param days 기간의 첫날부터 마지막 날까지 하루에 하나. 기록이 없는 날도 빈 값으로 들어 있다.
 */
data class PeriodStats(val days: List<DayStats>) {
    /** 기간 전체의 합계. 완료율은 이 합계로 계산한다(날짜별 완료율의 평균이 아니다). */
    val total: DoseCounts get() = days.fold(DoseCounts()) { sum, day -> sum + day.counts }
}

/**
 * 건너뜀 패턴(S15).
 * @param skippedByPeriod 시간대(아침·점심·저녁·밤)마다 건너뛴 횟수. 네 시간대가 모두 들어 있다.
 * @param overdueUnrecorded 같은 기간의 "기한 경과 · 미기록" 횟수. 건너뜀과 따로 센다.
 */
data class SkipPattern(val skippedByPeriod: Map<DayPeriod, Int>, val overdueUnrecorded: Int) {
    val totalSkipped: Int get() = skippedByPeriod.values.sum()
}

/**
 * 복용 기록 목록으로 통계를 계산한다. 순수 Kotlin 코드라서 단위 테스트를 할 수 있다. (DB_DESIGN ⑩)
 *
 * 기간:
 *   주간(S13) = 오늘을 포함한 최근 7일 (월~일이 아니다)
 *   월간(S14) = 그 달의 1일 ~ 말일
 *   패턴(S15) = 오늘을 포함한 최근 30일
 * 어느 기간이든 "도래한 회차"(예정 일시 ≤ 지금)만 센다. 그래서 미래 날짜는 저절로 빠진다.
 */
object StatsCalculator {

    /** 주간 통계가 보는 날 수 */
    const val WEEK_DAYS = 7L
    /** 건너뜀 패턴이 보는 날 수 */
    const val PATTERN_DAYS = 30L

    /**
     * 회차들을 상태별로 센다.
     *
     * 아직 도래하지 않은 회차(미래 날짜, 오늘인데 예정 시각 전)는 상태와 상관없이 "예정"으로만 센다.
     * 미리 복용 완료를 눌러 둔 회차도 그 시각이 될 때까지는 완료율에 들어가지 않는다.
     * 도래한 회차만 복용 완료 / 건너뜀 / 기한 경과 미기록으로 나눠 센다.
     */
    fun count(doses: List<DoseItem>, now: LocalDateTime): DoseCounts {
        var taken = 0
        var skipped = 0
        var missed = 0
        var upcoming = 0
        doses.forEach { dose ->
            when {
                !dose.isDue(now) -> upcoming++
                dose.status == DoseStatus.TAKEN -> taken++
                dose.status == DoseStatus.SKIPPED -> skipped++
                else -> missed++
            }
        }
        return DoseCounts(taken, skipped, missed, upcoming)
    }

    /**
     * from부터 to까지(양 끝 포함) 날짜마다의 통계를 만든다.
     * doses에 그 기간 밖의 기록이 섞여 있어도 무시한다.
     */
    fun periodStats(doses: List<DoseItem>, from: LocalDate, to: LocalDate, now: LocalDateTime): PeriodStats {
        val dosesByDate = doses.groupBy { it.date }
        val days = generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .map { date -> DayStats(date, count(dosesByDate[date].orEmpty(), now)) }
            .toList()
        return PeriodStats(days)
    }

    /** 최근 7일: 오늘−6일 ~ 오늘. 맨 뒤가 오늘이다. */
    fun recentWeek(doses: List<DoseItem>, today: LocalDate, now: LocalDateTime): PeriodStats =
        periodStats(doses, today.minusDays(WEEK_DAYS - 1), today, now)

    /** 한 달: 1일 ~ 말일. 이번 달이면 오늘 뒤의 날짜는 도래한 회차가 없어 빈 값이 된다. */
    fun month(doses: List<DoseItem>, month: YearMonth, now: LocalDateTime): PeriodStats =
        periodStats(doses, month.atDay(1), month.atEndOfMonth(), now)

    /**
     * 건너뜀 패턴: 최근 30일(오늘−29일 ~ 오늘)의 도래한 회차 중
     *   - 건너뜀(SKIPPED)을 예정 시각의 시간대별로 세고
     *   - 응답 없는 것(기한 경과 · 미기록)은 따로 센다. 미기록을 건너뜀으로 바꾸지 않는다.
     * 시간대 경계는 일정 화면(S05)과 같은 periodOf를 쓴다.
     */
    fun skipPattern(doses: List<DoseItem>, today: LocalDate, now: LocalDateTime): SkipPattern {
        val from = today.minusDays(PATTERN_DAYS - 1)
        val due = doses.filter { !it.date.isBefore(from) && !it.date.isAfter(today) && it.isDue(now) }
        val skipped = due.filter { it.status == DoseStatus.SKIPPED }
        return SkipPattern(
            skippedByPeriod = DayPeriod.entries.associateWith { period ->
                skipped.count { periodOf(it.time) == period }
            },
            overdueUnrecorded = due.count { it.status == DoseStatus.PENDING }
        )
    }
}
