package com.cau.medicheck.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

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
 * 주차: "11월 4주차". 한 주 = 월~일이고, 그 주의 목요일이 있는 달의 몇 번째 주인지로 센다.
 * (달력에서 흔히 쓰는 방식. 달의 경계에 걸친 주는 목요일이 있는 달에 속한다)
 */
data class WeekOfMonth(val month: YearMonth, val index: Int)

/**
 * 패턴 탭의 시간대 한 줄: 최근 7일 동안 그 시간대 회차의 횟수.
 * @param hasSchedule 그 시간대에 회차가 하나라도 있었나 (없으면 화면에 "일정 없음")
 */
data class SlotRate(val period: DayPeriod, val counts: DoseCounts, val hasSchedule: Boolean)

/** 연속 기록: 하루 약을 모두 먹은 날이 며칠 이어졌나. current = 지금까지, best = 가장 길었던 기록 */
data class Streaks(val current: Int, val best: Int)

/**
 * 복용 기록 목록으로 통계를 계산한다. 순수 Kotlin 코드라서 단위 테스트를 할 수 있다. (DB_DESIGN ⑩)
 *
 * 기간:
 *   최근 7일(홈 카드) = 오늘을 포함한 최근 7일
 *   주간(S13) = 월~일 한 주
 *   월간(S14) = 그 달의 1일 ~ 말일, 그 달의 주차들
 *   패턴(S15) = 시간대별은 최근 7일, 연속 기록은 최근 1년
 * 어느 기간이든 "도래한 회차"(예정 일시 ≤ 지금)만 센다. 그래서 미래 날짜는 저절로 빠진다.
 */
object StatsCalculator {

    /** 주간 통계가 보는 날 수 */
    const val WEEK_DAYS = 7L
    /** 패턴 탭의 시간대별이 보는 날 수 (오늘 포함) */
    const val SLOT_DAYS = 7L
    /** 연속 기록을 찾는 날 수 (오늘 포함) */
    const val STREAK_DAYS = 365L

    /**
     * 회차들을 상태별로 센다.
     *
     * 아직 도래하지 않은 회차(미래 날짜, 오늘인데 예정 시각 전)는 상태와 상관없이 "예정"으로만 센다.
     * 미리 복용 완료를 눌러 둔 회차도 그 시각이 될 때까지는 완료율에 들어가지 않는다.
     * 도래한 회차만 복용 완료 / 건너뜀 / 미복용으로 나눠 센다.
     * 단 미기록은 여유 시간(예정 시각 + 30분)까지는 아직 "예정"으로 센다 — 화면의 미복용 표시와 맞추기 위해서.
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
                dose.isOverdue(now) -> missed++
                else -> upcoming++   // 미기록이지만 아직 여유 시간 안
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

    /** 그 날짜가 든 주의 월요일 */
    fun weekStartOf(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

    /** 한 주(월~일): monday ~ monday+6일 */
    fun week(doses: List<DoseItem>, monday: LocalDate, now: LocalDateTime): PeriodStats =
        periodStats(doses, monday, monday.plusDays(6), now)

    /** 그 주가 몇 월 몇 주차인가 (목요일이 있는 달 기준) */
    fun weekOfMonth(monday: LocalDate): WeekOfMonth {
        val thursday = monday.plusDays(3)
        val month = YearMonth.from(thursday)
        val firstThursday = firstThursdayOf(month)
        return WeekOfMonth(month, (ChronoUnit.DAYS.between(firstThursday, thursday) / 7).toInt() + 1)
    }

    /** 그 달에 속한 주들의 월요일 (1주차부터). 목요일이 그 달에 있는 주만 */
    fun weeksOfMonth(month: YearMonth): List<LocalDate> =
        generateSequence(weekStartOf(firstThursdayOf(month))) { it.plusWeeks(1) }
            .takeWhile { YearMonth.from(it.plusDays(3)) == month }
            .toList()

    private fun firstThursdayOf(month: YearMonth): LocalDate {
        val first = month.atDay(1)
        // 목요일(4)까지 며칠 남았나. 1일이 금~일이면 다음 주 목요일
        return first.plusDays(((4 - first.dayOfWeek.value + 7) % 7).toLong())
    }

    /**
     * 패턴 탭의 시간대별: 최근 7일(오늘−6일 ~ 오늘)의 회차를 예정 시각의 시간대로 나눠 센다.
     * 새벽은 그 시간대에 회차가 있을 때만 넣는다(대부분의 사람에게는 없는 줄이라).
     */
    fun slotRates(doses: List<DoseItem>, today: LocalDate, now: LocalDateTime): List<SlotRate> {
        val from = today.minusDays(SLOT_DAYS - 1)
        val recent = doses.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
        return DayPeriod.entries.map { period ->
            val inPeriod = recent.filter { periodOf(it.time) == period }
            SlotRate(period, count(inPeriod, now), hasSchedule = inPeriod.isNotEmpty())
        }.filter { it.period != DayPeriod.DAWN || it.hasSchedule }
    }

    /**
     * 연속 기록: 그날의 회차를 모두 "복용 완료"한 날이 며칠 이어졌나.
     *   - 회차가 없는 날은 건너뛴다(이어짐을 끊지도, 늘리지도 않는다)
     *   - 오늘은 아직 남은 회차가 있고 빠뜨린 것(건너뜀·미복용)이 없으면 끊지 않는다(하루가 진행 중이라서)
     *   - 건너뜀도 "다 먹은 날"이 아니다
     */
    fun streaks(doses: List<DoseItem>, today: LocalDate, now: LocalDateTime): Streaks {
        val from = today.minusDays(STREAK_DAYS - 1)
        val byDate = doses.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }.groupBy { it.date }
        var run = 0
        var best = 0
        byDate.keys.sorted().forEach { date ->
            val c = count(byDate.getValue(date), now)
            when {
                c.upcoming == 0 && c.due > 0 && c.taken == c.due -> { run++; best = maxOf(best, run) }
                date == today && c.skipped == 0 && c.missed == 0 -> Unit   // 오늘은 아직 진행 중
                else -> run = 0
            }
        }
        return Streaks(current = run, best = best)
    }
}
