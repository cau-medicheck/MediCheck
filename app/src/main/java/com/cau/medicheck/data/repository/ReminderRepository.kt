package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.ReminderRow
import com.cau.medicheck.data.local.ScheduleDao
import com.cau.medicheck.data.local.SessionStore
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.domain.ReminderPlanner
import com.cau.medicheck.domain.ReminderRule
import com.cau.medicheck.domain.RepeatDays
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 알림에 필요한 정보를 DB에서 읽어 주는 곳.
 *
 * 알림은 화면이 꺼져 있을 때도 동작해야 해서, "누구의 약인지"를 화면에서 넘겨받을 수 없다.
 * 그래서 SessionStore에 기억해 둔 로그인 사용자를 직접 읽는다. 로그인한 사람이 없으면 알림도 없다.
 */
class ReminderRepository(
    private val sessionStore: SessionStore,
    private val scheduleDao: ScheduleDao,
    private val doseRecordDao: DoseRecordDao,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    /** 지금 시각 이후로 가장 먼저 오는 복용 일시. 알림을 예약할 시각이다. 없으면 null. */
    suspend fun nextTrigger(): LocalDateTime? =
        ReminderPlanner.nextTrigger(loadRules(), LocalDateTime.now(clock))

    /**
     * 주어진 일시에 알려야 할 약들.
     * 그 시각에 걸린 규칙 중, 사용자가 이미 복용 완료·건너뜀으로 기록한 것은 뺀다.
     * (약을 미리 먹고 기록해 둔 사람에게 또 알리지 않기 위해서다.)
     */
    suspend fun dueReminders(at: LocalDateTime): List<ReminderRule> {
        val userId = sessionStore.getLoggedInUserId() ?: return emptyList()
        return ReminderPlanner.dueAt(loadRules(userId), at).mapNotNull { rule ->
            // 기록은 "이 사용자의 것"일 때만 찾는다(계정 격리). 기록이 없으면 아직 응답 전으로 본다.
            val record = doseRecordDao.findOwnedByKey(userId, rule.scheduleId, at.toLocalDate().toString())
                ?: return@mapNotNull rule
            when {
                record.status != DoseStatus.PENDING.name -> null
                // 오늘은 기록 줄이 기준이다(DB_DESIGN 9.1). 기록의 예정 시각이 규칙의 시각과 다르면
                // 그 회차는 옛 시각으로 이미 받은 것이다. 예: 12시에 07:00 줄(기한 경과)을 20:00으로 고치면
                // 오늘은 07:00 기록 하나뿐이고 20:00 회차는 내일부터다 → 오늘 20:00에는 알리지 않는다.
                record.scheduledTime != rule.time.format(TIME_FORMAT) -> null
                // 알림 문구는 기록의 스냅샷(그 회차를 만들 때의 이름·복용량)을 쓴다(DB_DESIGN ⑥).
                else -> rule.copy(medicationName = record.medicationName, dosage = record.dosage)
            }
        }
    }

    private companion object {
        /** "08:05" 모양. 기록의 scheduled_time과 같은 꼴 */
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    /** 로그인한 사용자의 사용 중인 규칙 전부. 로그인한 사람이 없으면 빈 목록. */
    private suspend fun loadRules(): List<ReminderRule> {
        val userId = sessionStore.getLoggedInUserId() ?: return emptyList()
        return loadRules(userId)
    }

    /** 일시중지 중인 약은 DB 조회(findReminderRows)에서 이미 빠져 있다. */
    private suspend fun loadRules(userId: Long): List<ReminderRule> =
        scheduleDao.findReminderRows(userId).map { it.toRule() }

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
}
