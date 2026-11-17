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
 * 알림 계산의 입력 한 줄 = "어느 날 몇 시에 어떤 약".
 *
 * 오늘(과 어제)의 회차는 복용 기록(dose_records) 줄에서 만든다. 이름·복용량·시각은 기록에 찍어 둔 값(스냅샷)이다.
 * 기록이 아직 없는 날(내일 이후)의 회차는 규칙에서 계산해 만든다.
 *
 * @param pending 아직 응답 전(PENDING)인가. 복용 완료·건너뜀으로 기록한 회차는 알리지 않는다.
 */
data class ReminderDose(
    val scheduleId: Long,
    val date: LocalDate,
    val time: LocalTime,
    val medicationName: String,
    val dosage: String,
    val pending: Boolean = true
) {
    /** 예정 일시 */
    val at: LocalDateTime get() = LocalDateTime.of(date, time)
    val key: DoseKey get() = DoseKey(scheduleId, date)
}

/** 지금 띄울 알림 하나. [isRealert]가 true면 "다시 알림"이다. */
data class DueReminder(val dose: ReminderDose, val isRealert: Boolean)

/**
 * "다음 알림은 언제인가", "지금 알려야 할 약은 무엇인가"를 계산한다.
 *
 * 회차의 출처는 두 가지다 (DB_DESIGN 9.1).
 *   - 기록이 있는 회차 → 기록 줄 그대로 (예정 시각·이름은 기록의 스냅샷, 응답했으면 대상에서 뺀다)
 *   - 기록이 없는 회차 → 규칙에서 계산 (내일 이후가 여기에 해당한다)
 * 그래서 "내일부터 적용"으로 시각을 바꿔도 오늘은 기록의 옛 시각에 울린다.
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

    // ---------- 기록 줄 + 규칙 (5단계) ----------

    /** 규칙 하나가 날짜 [date]에 만드는 회차. 그날 회차가 없으면 null. */
    private fun ReminderRule.doseOn(date: LocalDate): ReminderDose? =
        if (occursOn(date)) ReminderDose(scheduleId, date, time, medicationName, dosage) else null

    /**
     * 규칙에서, [after]보다 뒤이고 기록이 아직 없는 가장 가까운 회차의 일시.
     * 기록이 이미 있는 회차는 기록 쪽에서 다루므로 건너뛴다.
     */
    private fun nextUnrecorded(rule: ReminderRule, after: LocalDateTime, recorded: Set<DoseKey>): LocalDateTime? {
        var from = after
        // 기록은 많아야 어제·오늘·내일 3일 치라서 4번이면 기록 없는 날에 닿는다.
        repeat(4) {
            val next = nextOccurrence(rule, from) ?: return null
            if (DoseKey(rule.scheduleId, next.toLocalDate()) !in recorded) return next
            from = next
        }
        return null
    }

    /**
     * 다음 회차 알림 시각: [after]보다 뒤에 오는 가장 이른 것.
     * @param recordDoses 어제~내일의 기록 줄
     * @param rules 사용 중인 규칙 (기록이 없는 날의 회차를 계산한다)
     */
    fun nextTrigger(recordDoses: List<ReminderDose>, rules: List<ReminderRule>, after: LocalDateTime): LocalDateTime? {
        val recorded = recordDoses.map { it.key }.toSet()
        val fromRecords = recordDoses.filter { it.pending && it.at.isAfter(after) }.map { it.at }
        val fromRules = rules.mapNotNull { nextUnrecorded(it, after, recorded) }
        return (fromRecords + fromRules).minOrNull()
    }

    /** 일시 [at](분 단위)에 알릴 회차들. 응답한 회차는 뺀다. */
    fun dueAt(recordDoses: List<ReminderDose>, rules: List<ReminderRule>, at: LocalDateTime): List<ReminderDose> =
        dueBetween(recordDoses, rules, at, at)

    /**
     * [from]분부터 [to]분까지(양 끝 포함)에 예정된, 아직 응답 전인 회차들.
     * 알람이 제때 울리면 from과 to가 같은 분이다. 늦게 울렸을 때는 그 사이의 회차도 함께 고른다.
     */
    fun dueBetween(
        recordDoses: List<ReminderDose>,
        rules: List<ReminderRule>,
        from: LocalDateTime,
        to: LocalDateTime
    ): List<ReminderDose> {
        val first = from.truncatedTo(ChronoUnit.MINUTES)
        val last = maxOf(first, to.truncatedTo(ChronoUnit.MINUTES))
        val recorded = recordDoses.map { it.key }.toSet()
        val fromRecords = recordDoses.filter { it.pending && !it.at.isBefore(first) && !it.at.isAfter(last) }
        // 기록이 없는 회차는 규칙에서: 범위에 걸친 날짜(보통 하루)마다 그날 회차를 만들어 범위 안의 것만 고른다.
        val days = generateSequence(first.toLocalDate()) { it.plusDays(1) }.takeWhile { !it.isAfter(last.toLocalDate()) }
        val fromRules = days.flatMap { date ->
            rules.asSequence()
                .filter { DoseKey(it.scheduleId, date) !in recorded }
                .mapNotNull { it.doseOn(date) }
                .filter { !it.at.isBefore(first) && !it.at.isAfter(last) }
        }.toList()
        return (fromRecords + fromRules).sortedBy { it.at }
    }

    /**
     * 다음 재알림 시각. 예정 일시가 지났지만 아직 재알림 간격이 다 지나지 않았고 응답 전인 회차 중에서
     * (예정 일시 + 간격)이 가장 이른 것. 재알림은 회차마다 한 번이다.
     */
    fun nextRealert(recordDoses: List<ReminderDose>, now: LocalDateTime, intervalMinutes: Int): LocalDateTime? =
        recordDoses
            .filter { it.pending && !it.at.isAfter(now) }
            .map { it.at.plusMinutes(intervalMinutes.toLong()) }
            .filter { it.isAfter(now) }
            .minOrNull()

    /** 일시 [at](분 단위)에 "다시 알림"을 보낼 회차들: 예정 일시 + 간격이 [at]이고 아직 응답 전인 것. */
    fun realertsDueAt(recordDoses: List<ReminderDose>, at: LocalDateTime, intervalMinutes: Int): List<ReminderDose> =
        realertsDueBetween(recordDoses, at, at, intervalMinutes)

    /** [from]분부터 [to]분까지(양 끝 포함)에 재알림 차례가 온 회차들. (알람이 늦게 울렸을 때용) */
    fun realertsDueBetween(
        recordDoses: List<ReminderDose>,
        from: LocalDateTime,
        to: LocalDateTime,
        intervalMinutes: Int
    ): List<ReminderDose> {
        val first = from.truncatedTo(ChronoUnit.MINUTES)
        val last = maxOf(first, to.truncatedTo(ChronoUnit.MINUTES))
        return recordDoses.filter {
            val realertAt = it.at.plusMinutes(intervalMinutes.toLong())
            it.pending && !realertAt.isBefore(first) && !realertAt.isAfter(last)
        }
    }

    /**
     * 앞으로 실제로 울릴 알림 목록 (S16): 오늘 남은 것 + 내일, 날짜 → 시각 → 약 이름 순으로 [limit]개.
     * 정확히 지금 분인 회차는 이미 울린 것으로 보고 뺀다.
     */
    fun upcoming(
        recordDoses: List<ReminderDose>,
        rules: List<ReminderRule>,
        now: LocalDateTime,
        limit: Int
    ): List<ReminderDose> {
        val days = listOf(now.toLocalDate(), now.toLocalDate().plusDays(1))
        val recorded = recordDoses.map { it.key }.toSet()
        val fromRecords = recordDoses.filter { it.pending && it.date in days }
        val fromRules = days.flatMap { date ->
            rules.filter { DoseKey(it.scheduleId, date) !in recorded }.mapNotNull { it.doseOn(date) }
        }
        return (fromRecords + fromRules)
            .filter { it.at.isAfter(now) }
            .sortedWith(compareBy<ReminderDose> { it.date }.thenBy { it.time }.thenBy { it.medicationName })
            .take(limit)
    }
}

/**
 * 알람이 울렸을 때 "몇 시 회차의 알람이었는지"를 알아낸다.
 *
 * 알람을 걸 때 예정 일시를 글자("2026-03-08T02:30")로 함께 실어 보낸다. 밀리초 숫자를 다시 시각으로 바꾸면,
 * 일광절약시간(여름에 시계를 1시간 당기는 제도)이 시작되는 날의 02:30처럼 "없는 시각"이 03:30으로 밀려
 * 02:30 회차를 찾지 못하기 때문이다. 글자가 없을 때만(옛 버전이 건 알람) 밀리초를 쓴다.
 */
object ReminderTrigger {
    fun resolve(localText: String?, triggerAtMillis: Long, zone: java.time.ZoneId): LocalDateTime =
        localText?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            ?: java.time.Instant.ofEpochMilli(triggerAtMillis).atZone(zone).toLocalDateTime()
}
