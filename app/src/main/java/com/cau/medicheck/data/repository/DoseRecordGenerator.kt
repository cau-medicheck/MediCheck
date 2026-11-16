package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecord
import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.MedicationDao
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
 * @param clock 지금 날짜·시각을 알려 주는 시계. 테스트에서는 멈춘 시계로 바꿔 끼운다.
 */
class DoseRecordGenerator(
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao,
    private val doseRecordDao: DoseRecordDao,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    /** 한 사용자의 사용 중인 모든 약에 대해, 비어 있는 날짜의 기록을 오늘까지 채운다. */
    suspend fun generateForUser(userId: Long) {
        medicationDao.findActive(userId).forEach { generateForMedication(it) }
    }

    /**
     * 약 하나의 사용 중인 규칙마다, 비어 있는 날짜의 기록을 오늘까지 채운다.
     * 이미 있는 (규칙, 날짜)는 DB가 알아서 건너뛰므로 여러 번 불러도 중복이 생기지 않는다.
     */
    suspend fun generateForMedication(medication: Medication) {
        if (medication.deletedAt != null) return

        val today = LocalDate.now(clock)
        val createdAt = clock.millis()
        val startDate = LocalDate.parse(medication.startDate)
        val endDate = medication.endDate?.let(LocalDate::parse)

        scheduleDao.findActiveByMedication(medication.id).forEach { schedule ->
            val dates = DosePlanner.datesToCreate(
                startDate = startDate,
                endDate = endDate,
                repeatDays = RepeatDays.decode(schedule.repeatDays),
                lastCreatedDate = doseRecordDao.lastDate(schedule.id)?.let(LocalDate::parse),
                today = today
            )
            if (dates.isNotEmpty()) {
                doseRecordDao.insertAllIgnore(
                    dates.map { date ->
                        DoseRecord(
                            scheduleId = schedule.id,
                            date = date.toString(),
                            scheduledTime = schedule.time,   // 규칙의 "지금" 시각을 복사해 둔다
                            status = DoseStatus.PENDING.name,
                            createdAt = createdAt
                        )
                    }
                )
            }
        }
    }

    /**
     * 약이나 복용 시간이 바뀐 뒤에 부른다.
     * 바뀌기 전 규칙으로 만들어 둔 오늘 치 기록 중 "아직 응답도 안 했고 시각도 안 된 것"을 지운다.
     * (그대로 두면 옛 시각에 깜빡한 것으로 표시된다. 지운 뒤 generateForMedication이 새 규칙으로 다시 만든다.)
     */
    suspend fun discardUpcomingToday(medicationId: Long) {
        doseRecordDao.deleteUpcomingPending(
            medicationId = medicationId,
            date = LocalDate.now(clock).toString(),
            afterTime = LocalTime.now(clock).format(TIME_FORMAT)
        )
    }

    private companion object {
        /** "08:05"처럼 표의 scheduled_time과 같은 모양. 글자끼리 비교해도 시각 순서가 맞는다. */
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
