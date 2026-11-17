package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecord
import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.DoseStatusEventDao
import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.MedicationDao
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.data.local.ScheduleDao
import com.cau.medicheck.domain.DosePlanner
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.domain.RepeatDays
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 복용 기록(dose_records)을 미리 만들어 두는 일을 맡는다. (docs/DB_DESIGN.md ⑨)
 *
 * 기록은 사용자가 버튼을 누를 때 생기는 것이 아니라, 규칙(schedules)을 보고 시스템이 먼저 만든다.
 * "기록 보충"은 이 클래스 한 곳에만 있고, 필요한 곳에서 이것을 부른다.
 *   - 앱을 열 때 (DoseRepository.refresh)
 *   - 약·복용 시간을 수정·삭제하기 직전과 직후 (MedicationRepository)
 *
 * 기록을 만들 때 그 순간의 약 이름·복용량·메모·예정 시각을 기록 줄에 복사한다(스냅샷).
 * 나중에 약을 고쳐도 이미 만든 기록은 그때 값을 유지한다.
 *
 * @param clock 지금 날짜·시각을 알려 주는 시계. 테스트에서는 멈춘 시계로 바꿔 끼운다.
 */
class DoseRecordGenerator(
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao,
    private val doseRecordDao: DoseRecordDao,
    private val statusEventDao: DoseStatusEventDao,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    /** 한 사용자의 사용 중인 모든 약에 대해, 비어 있는 날짜의 기록을 오늘까지 채운다. */
    suspend fun generateForUser(userId: Long) {
        medicationDao.findActive(userId).forEach { generateForMedication(it) }
    }

    /**
     * 약 하나의 사용 중인 규칙마다, 비어 있는 날짜의 기록을 오늘까지 채운다.
     * 이미 있는 (규칙, 날짜)는 DB가 알아서 건너뛰므로 여러 번 불러도 중복이 생기지 않는다.
     *
     * @param medication 기록에 복사할 "지금" 약 값. 약을 고치기 직전에 부를 때는 고치기 전 값을 넘긴다.
     */
    suspend fun generateForMedication(medication: Medication) {
        if (medication.deletedAt != null) return

        val today = LocalDate.now(clock)
        val createdAt = clock.millis()
        val startDate = LocalDate.parse(medication.startDate)
        val endDate = medication.endDate?.let(LocalDate::parse)
        val pausedFrom = medication.pausedFrom?.let(LocalDate::parse)

        scheduleDao.findActiveByMedication(medication.id).forEach { schedule ->
            val dates = DosePlanner.datesToCreate(
                startDate = startDate,
                endDate = endDate,
                repeatDays = RepeatDays.decode(schedule.repeatDays),
                lastCreatedDate = doseRecordDao.lastDate(schedule.id)?.let(LocalDate::parse),
                today = today,
                effectiveFrom = schedule.effectiveFrom?.let(LocalDate::parse),
                pausedFrom = pausedFrom
            )
            if (dates.isNotEmpty()) {
                doseRecordDao.insertAllIgnore(
                    dates.map { date -> newRecord(medication, schedule, date, createdAt) }
                )
            }
        }
    }

    /** 새 기록 한 줄. 상태는 미기록(PENDING), 스냅샷 칸은 지금 약·규칙 값으로 채운다. */
    private fun newRecord(medication: Medication, schedule: Schedule, date: LocalDate, createdAt: Long) =
        DoseRecord(
            scheduleId = schedule.id,
            date = date.toString(),
            scheduledTime = schedule.time,          // 규칙의 "지금" 시각을 복사해 둔다
            medicationName = medication.name,       // 약의 "지금" 이름·복용량·메모를 복사해 둔다
            dosage = medication.dosage,
            memo = medication.memo,
            status = DoseStatus.PENDING.name,
            createdAt = createdAt
        )

    /**
     * 오늘 치 다시 맞추기 (DB_DESIGN ⑧).
     * 약·규칙을 수정·삭제·일시중지한 **직후**에 부른다. [medication]은 바뀐 뒤의 약 값이다.
     *
     * 대상은 "오늘 남은 미기록" = 오늘 날짜이면서 아직 응답하지 않았고 예정 시각이 지금보다 뒤인 기록뿐이다.
     * 바뀌기 전 값으로 만든 기록이라 그대로 두면 틀린 시각·이름으로 보이기 때문이다.
     * 이미 응답했거나 시각이 지난 기록, 지난 날짜의 기록은 건드리지 않는다.
     *
     * 기록마다 두 갈래로 나눈다.
     *   (1) 고친다: 그 규칙이 남아 있고 새 설정으로도 오늘이 회차일이면, 같은 줄의 스냅샷을 새 값으로 바꾼다.
     *       id가 그대로라 상태 변경 이력과의 연결이 유지된다.
     *   (2) 지운다: 약이 삭제·일시중지됐거나, 그 규칙 줄이 빠졌거나, 새 설정으로 오늘이 회차일이 아니면.
     *       지우기 전에 그 기록을 가리키는 이력의 연결을 먼저 끊는다(이력 줄은 남는다).
     * 지운 뒤에는 generateForMedication이 새 설정으로 오늘 기록을 다시 만든다(부르는 쪽이 이어서 부름).
     */
    suspend fun reconcileUpcomingToday(userId: Long, medication: Medication) {
        val today = LocalDate.now(clock)
        val upcoming = doseRecordDao.findUpcomingPending(
            userId = userId,
            medicationId = medication.id,
            date = today.toString(),
            afterTime = LocalTime.now(clock).format(TIME_FORMAT)
        )
        if (upcoming.isEmpty()) return

        val medicationGone = medication.deletedAt != null || medication.pausedFrom != null
        val activeSchedules =
            if (medicationGone) emptyMap()
            else scheduleDao.findActiveByMedication(medication.id).associateBy { it.id }

        val toDelete = mutableListOf<Long>()
        upcoming.forEach { record ->
            val schedule = activeSchedules[record.scheduleId]
            if (schedule != null && occursToday(medication, schedule, today)) {
                doseRecordDao.updateUpcomingSnapshot(
                    userId = userId,
                    recordId = record.id,
                    time = schedule.time,
                    name = medication.name,
                    dosage = medication.dosage,
                    memo = medication.memo
                )
            } else {
                toDelete += record.id
            }
        }

        if (toDelete.isNotEmpty()) {
            // FK의 SET NULL에 기대지 않고 직접 연결을 끊은 뒤 지운다 (DB_DESIGN ⑦ "FK 동작에 기대지 않는다")
            statusEventDao.detachRecords(userId, toDelete)
            doseRecordDao.deleteByIds(userId, toDelete)
        }
    }

    /**
     * 종료일 당김 정리 (DB_DESIGN ⑧). 종료일을 앞으로 당긴 직후에 부른다.
     * 새 종료일 [endDate] 뒤 날짜의 미기록(PENDING)을 지운다. 응답한 기록(복용 완료·건너뜀)은 남긴다.
     * 남겨 두면 '종료'된 약의 미기록이 '기한 경과'로 보이고 완료율 분모에 들어가기 때문이다.
     * "오늘 치 다시 맞추기"의 지우는 갈래와 같은 순서: 이력의 연결을 먼저 끊고(이력 줄은 남음) 기록을 지운다.
     */
    suspend fun deletePendingAfterEnd(userId: Long, medicationId: Long, endDate: LocalDate) {
        val ids = doseRecordDao.findPendingAfter(userId, medicationId, endDate.toString())
        if (ids.isEmpty()) return
        statusEventDao.detachRecords(userId, ids)
        doseRecordDao.deleteByIds(userId, ids)
    }

    /** 바뀐 뒤의 약·규칙 값으로 오늘이 회차일인가 */
    private fun occursToday(medication: Medication, schedule: Schedule, today: LocalDate): Boolean =
        DosePlanner.occursOn(
            date = today,
            startDate = LocalDate.parse(medication.startDate),
            endDate = medication.endDate?.let(LocalDate::parse),
            repeatDays = RepeatDays.decode(schedule.repeatDays),
            effectiveFrom = schedule.effectiveFrom?.let(LocalDate::parse),
            pausedFrom = medication.pausedFrom?.let(LocalDate::parse),
            today = today
        )

    private companion object {
        /** "08:05"처럼 표의 scheduled_time과 같은 모양. 글자끼리 비교해도 시각 순서가 맞는다. */
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
