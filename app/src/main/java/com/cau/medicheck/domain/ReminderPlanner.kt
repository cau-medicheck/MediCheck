package com.cau.medicheck.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 알림을 보내는 데 필요한 정보만 모은 "규칙 하나".
 * 복용 시간 규칙(schedules)에 그 약의 이름·복용량·기간을 붙인 것이다.
 *
 * 일시중지 중인 약은 DB 조회에서 이미 빠져 들어오므로 일시중지 칸은 없다.
 * @param effectiveFrom 이 규칙의 적용 시작일. 이날 전에는 알리지 않는다. null이면 약의 시작일부터.
 */
data class ReminderRule(
    val scheduleId: Long,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val time: LocalTime,
    val days: Set<DayOfWeek>,
    val effectiveFrom: LocalDate? = null
) {
    /**
     * 날짜 [date]에 이 규칙의 회차가 있는가. 기록 보충과 같은 식(DosePlanner.occursOn)을 쓴다.
     * 일시중지 칸은 없으므로 pausedFrom = null, 그래서 today 값은 결과에 영향이 없다.
     */
    fun occursOn(date: LocalDate): Boolean =
        DosePlanner.occursOn(date, startDate, endDate, days, effectiveFrom, pausedFrom = null, today = date)
}

/**
 * "다음 알림은 언제인가", "지금 알려야 할 약은 무엇인가"를 계산한다.
 * 순수 Kotlin 코드라서 폰 없이 단위 테스트를 할 수 있다.
 */
object ReminderPlanner {

    /**
     * 한 규칙에서, `after`보다 뒤에 오는 가장 가까운 복용 일시를 돌려준다.
     * 복용 기간이 끝났거나 요일이 하나도 없으면 null.
     *
     * 반복 요일은 일주일 단위라서, 앞으로 8일만 살펴보면 다음 날짜를 반드시 찾는다.
     */
    fun nextOccurrence(rule: ReminderRule, after: LocalDateTime): LocalDateTime? {
        // 시작일·적용 시작일 전에는 회차가 없으므로 그중 가장 늦은 날부터 살펴본다.
        var firstDate = maxOf(after.toLocalDate(), rule.startDate)
        if (rule.effectiveFrom != null) firstDate = maxOf(firstDate, rule.effectiveFrom)
        for (offset in 0L..7L) {
            val date = firstDate.plusDays(offset)
            if (rule.endDate != null && date.isAfter(rule.endDate)) return null
            if (!rule.occursOn(date)) continue

            val candidate = LocalDateTime.of(date, rule.time)
            if (candidate.isAfter(after)) return candidate
        }
        return null
    }

    /** 여러 규칙 중에서 가장 먼저 오는 다음 복용 일시. 알림을 예약할 시각이 된다. */
    fun nextTrigger(rules: List<ReminderRule>, after: LocalDateTime): LocalDateTime? =
        rules.mapNotNull { nextOccurrence(it, after) }.minOrNull()

    /**
     * 주어진 일시(분 단위)에 복용해야 하는 규칙들. 알림이 울렸을 때 "어떤 약을 알릴지" 고르는 데 쓴다.
     * 같은 시각에 여러 약이 걸려 있으면 여러 개가 나온다.
     */
    fun dueAt(rules: List<ReminderRule>, at: LocalDateTime): List<ReminderRule> {
        val minute = at.truncatedTo(ChronoUnit.MINUTES)
        val date = minute.toLocalDate()
        return rules.filter { rule -> rule.time == minute.toLocalTime() && rule.occursOn(date) }
    }
}
