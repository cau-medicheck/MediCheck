package com.cau.medicheck.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/*
 * 통계 "기타" 탭의 숫자들. (2026-11-24, 시안: 04_화면캡처/통계_시안/패턴컨셉_시안.html A·B)
 * 기간은 최근 4주(오늘 포함 28일). 안드로이드와 상관없는 순수 계산이라 단위 테스트로 확인한다(PatternInsightsTest).
 *
 * "빠뜨림" = 먹어야 했는데 안 먹은 것 = 건너뜀 + 미복용(예정 시각 30분 뒤까지 기록 없음).
 * 아직 시각 전이거나 여유 시간 안인 회차는 세지 않는다(통계 화면 전체와 같은 기준).
 */

/** 빠뜨린 횟수 / 먹어야 했던 횟수 */
data class MissStats(val missed: Int = 0, val due: Int = 0) {
    /** 빠뜨린 비율(%). 먹어야 했던 것이 없으면 null */
    val rate: Int? get() = if (due == 0) null else Math.round(missed * 100f / due)
    operator fun plus(o: MissStats) = MissStats(missed + o.missed, due + o.due)
}

/** 시간대 한 줄: 그 시간대의 빠뜨림. hasSchedule = 그 시간대에 회차가 하나라도 있었나 */
data class SlotMiss(val period: DayPeriod, val stats: MissStats, val hasSchedule: Boolean, val time: LocalTime?, val medCount: Int)

/**
 * 가장 자주 빠뜨린 회차(약 하나의 한 시각): 결론 카드에 쓴다.
 * @param weekendMore 주말(토·일)에 평일보다 더 자주 빠뜨렸나
 */
data class WorstDose(
    val medicationId: Long,
    val name: String,
    val time: LocalTime,
    val condition: DoseCondition,
    val stats: MissStats,
    val weekendMore: Boolean
)

/** 약 하나의 빠뜨림 (자주 빠뜨린 약 순위) */
data class MedMiss(val medicationId: Long, val name: String, val time: LocalTime, val condition: DoseCondition, val stats: MissStats)

/** 하루의 모양: 최근 14일 칸 */
enum class HabitMark {
    ALL,      // 그날 회차를 모두 먹음
    SOME,     // 일부만 먹음
    NONE,     // 하나도 못 먹음
    EMPTY,    // 회차가 없던 날
    OPEN      // 오늘인데 아직 끝나지 않음(빠뜨린 것 없음)
}

/**
 * 제시간에 먹었나: 먹은 회차의 "복용 완료를 누른 시각 − 예정 시각".
 * @param onTime 예정 30분 안(일찍 먹은 것 포함)
 * @param late 30분 ~ 1시간 늦게
 * @param veryLate 1시간 넘게 늦게
 */
data class Punctuality(val onTime: Int = 0, val late: Int = 0, val veryLate: Int = 0) {
    val total: Int get() = onTime + late + veryLate
    val onTimeRate: Int? get() = if (total == 0) null else Math.round(onTime * 100f / total)
}

object PatternInsights {

    /** 보는 날 수 (오늘 포함 4주) */
    const val DAYS = 28L
    /** 연속 기록 아래 칸 수 */
    const val HABIT_DAYS = 14L
    /** "제시간" 경계(분) */
    const val ON_TIME_MINUTES = 30L
    /**
     * 예정 시각과 이보다 더 차이 나는 기록은 "나중에 몰아서 누른 것"으로 보고 제시간·평균 계산에서 뺀다(분).
     * 복용 완료를 누른 시각을 먹은 시각으로 보는데, 며칠 지난 회차를 뒤늦게 누르면 수십 시간 차이가 나기 때문이다.
     */
    const val MAX_DELAY_MINUTES = 360L

    /** 최근 4주 안의 회차만 */
    fun recent(doses: List<DoseItem>, today: LocalDate): List<DoseItem> {
        val from = today.minusDays(DAYS - 1)
        return doses.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
    }

    /** 회차 하나의 빠뜨림: 먹어야 했던 것만 1, 빠뜨렸으면 missed 1 */
    fun missOf(dose: DoseItem, now: LocalDateTime): MissStats = when {
        !dose.isDue(now) -> MissStats()
        dose.status == DoseStatus.TAKEN -> MissStats(0, 1)
        dose.status == DoseStatus.SKIPPED -> MissStats(1, 1)
        dose.isOverdue(now) -> MissStats(1, 1)
        else -> MissStats()   // 미기록이지만 아직 여유 시간 안
    }

    private fun sum(doses: List<DoseItem>, now: LocalDateTime) = doses.fold(MissStats()) { s, d -> s + missOf(d, now) }

    /** 시간대별 빠뜨린 비율. 새벽은 회차가 있을 때만. time = 그 시간대에서 가장 흔한 예정 시각 */
    fun slots(doses: List<DoseItem>, today: LocalDate, now: LocalDateTime): List<SlotMiss> {
        val recent = recent(doses, today)
        return DayPeriod.entries.map { period ->
            val inPeriod = recent.filter { periodOf(it.time) == period }
            val time = inPeriod.groupingBy { it.time }.eachCount().maxByOrNull { it.value }?.key
            SlotMiss(period, sum(inPeriod, now), inPeriod.isNotEmpty(), time, inPeriod.map { it.medicationId }.distinct().size)
        }.filter { it.period != DayPeriod.DAWN || it.hasSchedule }
    }

    /** 요일별 빠뜨린 횟수 (월 → 일) */
    fun byWeekday(doses: List<DoseItem>, today: LocalDate, now: LocalDateTime): List<Pair<DayOfWeek, Int>> {
        val recent = recent(doses, today)
        return DayOfWeek.entries.map { day -> day to recent.filter { it.date.dayOfWeek == day }.sumOf { missOf(it, now).missed } }
    }

    /** 약별 빠뜨림, 많이 빠뜨린 순 (빠뜨린 적 없는 약은 뺀다). 시각은 그 약에서 가장 많이 빠뜨린 시각 */
    fun meds(doses: List<DoseItem>, today: LocalDate, now: LocalDateTime): List<MedMiss> =
        recent(doses, today).groupBy { it.medicationId }.mapNotNull { (id, list) ->
            val stats = sum(list, now)
            if (stats.missed == 0) return@mapNotNull null
            val worst = list.groupBy { it.time }.maxByOrNull { (_, l) -> sum(l, now).missed }!!.value.first()
            MedMiss(id, worst.medicationName, worst.time, worst.condition, stats)
        }.sortedWith(compareByDescending<MedMiss> { it.stats.missed }.thenBy { it.name })

    /** 결론 카드: 가장 자주 빠뜨린 (약, 시각). 빠뜨린 적이 없으면 null */
    fun worst(doses: List<DoseItem>, today: LocalDate, now: LocalDateTime): WorstDose? {
        val recent = recent(doses, today)
        val best = recent.groupBy { it.medicationId to it.time }
            .map { (key, list) -> Triple(key, list, sum(list, now)) }
            .filter { it.third.missed > 0 }
            .maxWithOrNull(compareBy<Triple<Pair<Long, LocalTime>, List<DoseItem>, MissStats>> { it.third.missed }.thenBy { it.third.rate ?: 0 })
            ?: return null
        val list = best.second
        val weekend = sum(list.filter { it.date.dayOfWeek.value >= 6 }, now)
        val weekday = sum(list.filter { it.date.dayOfWeek.value < 6 }, now)
        val weekendMore = weekend.due > 0 && weekday.due > 0 && (weekend.rate ?: 0) > (weekday.rate ?: 0)
        val first = list.first()
        return WorstDose(first.medicationId, first.medicationName, first.time, first.condition, best.third, weekendMore)
    }

    /** 최근 14일 칸 (13일 전 → 오늘) */
    fun habitDays(doses: List<DoseItem>, today: LocalDate, now: LocalDateTime): List<Pair<LocalDate, HabitMark>> {
        val byDate = doses.groupBy { it.date }
        return (HABIT_DAYS - 1 downTo 0).map { back ->
            val date = today.minusDays(back)
            val c = StatsCalculator.count(byDate[date].orEmpty(), now)
            val mark = when {
                c.due == 0 && c.upcoming == 0 -> HabitMark.EMPTY
                c.upcoming == 0 && c.taken == c.due -> HabitMark.ALL
                date == today && c.skipped == 0 && c.missed == 0 -> HabitMark.OPEN
                c.taken == 0 -> HabitMark.NONE
                else -> HabitMark.SOME
            }
            date to mark
        }
    }

    /** 먹은 회차의 늦은 정도(분). 일찍 먹었으면 음수. 누른 시각이 없거나 6시간 넘게 차이 나면(나중에 기록) null */
    fun delayMinutes(dose: DoseItem, zone: ZoneId): Long? {
        if (dose.status != DoseStatus.TAKEN) return null
        val acted = dose.actedAt ?: return null
        val actual = LocalDateTime.ofInstant(Instant.ofEpochMilli(acted), zone)
        val minutes = ChronoUnit.MINUTES.between(LocalDateTime.of(dose.date, dose.time), actual)
        return minutes.takeIf { kotlin.math.abs(it) <= MAX_DELAY_MINUTES }
    }

    /** 제시간에 먹었나 (최근 4주, 먹은 회차만) */
    fun punctuality(doses: List<DoseItem>, today: LocalDate, zone: ZoneId): Punctuality {
        var on = 0; var late = 0; var veryLate = 0
        recent(doses, today).mapNotNull { delayMinutes(it, zone) }.forEach { m ->
            when {
                m <= ON_TIME_MINUTES -> on++
                m <= 60 -> late++
                else -> veryLate++
            }
        }
        return Punctuality(on, late, veryLate)
    }

    /** 시간대별 평균 늦은 분(일찍이면 음수). 먹은 기록이 없는 시간대는 빠진다 */
    fun averageDelay(doses: List<DoseItem>, today: LocalDate, zone: ZoneId): Map<DayPeriod, Int> =
        recent(doses, today).mapNotNull { d -> delayMinutes(d, zone)?.let { periodOf(d.time) to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> Math.round(list.average()).toInt() }
}
