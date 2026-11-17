package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.DoseRecordRow
import com.cau.medicheck.data.local.ReminderRow
import com.cau.medicheck.data.local.ScheduleDao
import com.cau.medicheck.data.local.SessionStore
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.domain.DueReminder
import com.cau.medicheck.domain.ReminderDose
import com.cau.medicheck.domain.ReminderPlanner
import com.cau.medicheck.domain.ReminderRule
import com.cau.medicheck.domain.RepeatDays
import com.cau.medicheck.domain.UserSettings
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 알림 센터(S16)에 보여 줄 것: 앞으로 울릴 알림 목록과, 사용 중인 약이 하나라도 있는지 */
data class UpcomingReminders(val items: List<ReminderDose>, val hasActiveMedication: Boolean)

/**
 * 알림에 필요한 정보를 DB에서 읽어 주는 곳.
 *
 * 알림은 화면이 꺼져 있을 때도 동작해야 해서, "누구의 약인지"를 화면에서 넘겨받을 수 없다.
 * 그래서 SessionStore에 기억해 둔 로그인 사용자를 직접 읽는다. 로그인한 사람이 없으면 알림도 없다.
 *
 * 회차의 출처 (DB_DESIGN 9.1): 기록이 있는 날(어제·오늘)은 복용 기록 줄, 기록이 없는 날(내일 이후)은 규칙.
 * 그래서 계산하기 전에 항상 오늘까지의 기록을 먼저 채운다([refreshRecords]).
 *
 * @param refreshRecords 한 사용자의 기록을 오늘까지 채우는 함수 (DoseRepository.refresh)
 * @param currentSettings 지금 설정을 돌려주는 함수 (SettingsRepository.current)
 */
class ReminderRepository(
    private val sessionStore: SessionStore,
    private val scheduleDao: ScheduleDao,
    private val doseRecordDao: DoseRecordDao,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val refreshRecords: suspend (userId: Long) -> Unit = {},
    private val currentSettings: () -> UserSettings = { UserSettings() }
) {

    /**
     * 다음에 알람을 걸 일시: 다음 회차 시각과 다음 재알림 시각 중 이른 것. 없으면 null.
     * '복약 시간 알림'이 꺼져 있으면 항상 null이다.
     *
     * @param notBefore 방금 울린 알람의 예정 일시. 알람이 조금 일찍 울려도 같은 분을 다시 걸지 않게,
     *                  지금 시각과 이 값 중 늦은 쪽보다 뒤에서 찾는다.
     */
    suspend fun nextTrigger(notBefore: LocalDateTime? = null): LocalDateTime? {
        val settings = currentSettings()
        if (!settings.reminderEnabled) return null
        val source = loadSource() ?: return null

        val now = LocalDateTime.now(clock)
        val after = if (notBefore != null && notBefore.isAfter(now)) notBefore else now
        val nextDose = ReminderPlanner.nextTrigger(source.records, source.rules, after)
        val nextRealert =
            if (settings.realertEnabled) ReminderPlanner.nextRealert(source.records, after, settings.realertMinutes)
            else null
        return listOfNotNull(nextDose, nextRealert).minOrNull()
    }

    /**
     * 주어진 일시에 띄울 알림들: 그 시각의 회차 + (재알림이 켜져 있으면) 그 시각이 재알림 차례인 회차.
     * 이미 복용 완료·건너뜀으로 기록한 회차는 뺀다. '복약 시간 알림'이 꺼져 있으면 빈 목록.
     */
    suspend fun remindersAt(at: LocalDateTime): List<DueReminder> {
        val settings = currentSettings()
        if (!settings.reminderEnabled) return emptyList()
        val source = loadSource() ?: return emptyList()

        // 알람이 늦게 울렸으면(절전 상태 등) 그 사이에 시각이 지나간 회차도 함께 알린다.
        // 다음 알람은 "지금보다 뒤"에서 찾으므로, 여기서 알리지 않으면 그 회차는 알림 없이 지나가 버린다.
        // (폰이 꺼져 있던 동안의 알림을 몰아서 보내지는 않도록, 늦은 것으로 보는 범위는 30분까지만이다.)
        val now = LocalDateTime.now(clock)
        val until = minOf(maxOf(now, at), at.plusMinutes(LATE_WINDOW_MINUTES))
        val doses = ReminderPlanner.dueBetween(source.records, source.rules, at, until)
            .map { DueReminder(it, isRealert = false) }
        val realerts =
            if (settings.realertEnabled) {
                ReminderPlanner.realertsDueBetween(source.records, at, until, settings.realertMinutes)
                    // 같은 회차를 "회차 알림"과 "다시 알림"으로 두 번 띄우지 않는다.
                    .filter { realert -> doses.none { it.dose.key == realert.key } }
                    .map { DueReminder(it, isRealert = true) }
            } else emptyList()
        return doses + realerts
    }

    /** 앞으로 실제로 울릴 알림 목록 (S16). 알람 예약과 같은 계산을 쓴다. */
    suspend fun upcoming(limit: Int = UPCOMING_LIMIT): UpcomingReminders {
        val source = loadSource() ?: return UpcomingReminders(emptyList(), hasActiveMedication = false)
        val now = LocalDateTime.now(clock)
        return UpcomingReminders(
            items = ReminderPlanner.upcoming(source.records, source.rules, now, limit),
            // 사용 중 = 삭제·일시중지가 아니고(조회에서 이미 빠짐) 종료일이 지나지 않은 약
            hasActiveMedication = source.rules.any { it.endDate == null || !it.endDate.isBefore(now.toLocalDate()) }
        )
    }

    /** 계산의 재료: 어제~내일의 기록 줄과 사용 중인 규칙 */
    private class Source(val records: List<ReminderDose>, val rules: List<ReminderRule>)

    /** 로그인한 사용자의 재료를 읽는다. 로그인한 사람이 없으면 null. */
    private suspend fun loadSource(): Source? {
        val userId = sessionStore.getLoggedInUserId() ?: return null
        // 오늘 앱을 한 번도 열지 않았어도 오늘 기록이 있게 한다. 실패해도 규칙으로 계산할 수 있으므로 계속한다.
        runCatching { refreshRecords(userId) }

        val today = LocalDate.now(clock)
        // 어제: 자정을 넘기는 재알림(23:50 + 15분)용. 내일: 폰의 날짜가 뒤로 가 이미 기록이 있는 경우용.
        // 일시중지 중인 약과 삭제한 약은 DB 조회(findReminderRows)에서 이미 빠져 있다.
        val rules = scheduleDao.findReminderRows(userId).map { it.toRule() }
        // 기록은 삭제·일시중지한 약의 것도 남아 있으므로, 사용 중인 약의 기록만 알림 대상으로 삼는다.
        // 규칙이 아니라 "약"으로 거른다 — '내일부터 적용'으로 지운 옛 시간 줄의 오늘 기록은 오늘 울려야 한다.
        val activeMedicationIds = rules.map { it.medicationId }.toSet()
        val records = doseRecordDao
            .observeRange(userId, today.minusDays(1).toString(), today.plusDays(1).toString())
            .first()
            .filter { it.medicationId in activeMedicationIds }
            .map { it.toDose() }
        return Source(records, rules)
    }

    /** 기록 줄 → 알림 계산의 입력. 이름·복용량·시각은 기록에 찍어 둔 값(스냅샷)이다. */
    private fun DoseRecordRow.toDose() = ReminderDose(
        scheduleId = scheduleId,
        date = LocalDate.parse(date),
        time = LocalTime.parse(scheduledTime),
        medicationName = medicationName,
        dosage = dosage,
        pending = status == DoseStatus.PENDING.name
    )

    /** 표에서 읽은 줄(글자) → 계산에 쓰는 모양(날짜·시각·요일 타입) */
    private fun ReminderRow.toRule() = ReminderRule(
        scheduleId = scheduleId,
        medicationId = medicationId,
        medicationName = medicationName,
        dosage = dosage,
        startDate = LocalDate.parse(startDate),
        endDate = endDate?.let(LocalDate::parse),
        time = LocalTime.parse(time),
        days = RepeatDays.decode(repeatDays),
        effectiveFrom = effectiveFrom?.let(LocalDate::parse)
    )

    companion object {
        /** 알림 센터에 보여 줄 최대 줄 수 (v3와 같음) */
        const val UPCOMING_LIMIT = 8

        /** 알람이 늦게 울렸을 때, 그 사이에 지나간 회차를 함께 알리는 범위(분) */
        const val LATE_WINDOW_MINUTES = 30L
    }
}
