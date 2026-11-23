package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecord
import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.DoseStatusEvent
import com.cau.medicheck.data.local.DoseStatusEventDao
import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.MedicationDao
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.data.local.ScheduleDao
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.data.local.UserDao
import com.cau.medicheck.domain.AuthValidator
import com.cau.medicheck.domain.backup.BackupCheck
import com.cau.medicheck.domain.backup.BackupData
import com.cau.medicheck.domain.backup.BackupDoseRecord
import com.cau.medicheck.domain.backup.BackupMedication
import com.cau.medicheck.domain.backup.BackupSchedule
import com.cau.medicheck.domain.backup.BackupStatusEvent
import com.cau.medicheck.domain.backup.BackupValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/** S28 위쪽에 보여 줄 개수: 사용 중인 약 수, 복용 기록 줄 수(삭제한 약의 기록 포함) */
data class BackupCounts(val medications: Int, val records: Int)

/** 불러온 백업 파일을 복원해도 되는지 살펴본 결과 */
sealed interface RestoreCheck {
    /** 복원할 수 있다. data는 그대로 restore에 넘긴다 */
    data class Ready(val data: BackupData) : RestoreCheck

    /** 형식이 맞지 않는 파일 */
    data object Invalid : RestoreCheck

    /** 다른 계정에서 만든 백업(또는 누구 것인지 적혀 있지 않은 파일) — 복원하지 않는다 */
    data object OtherAccount : RestoreCheck
}

/**
 * 백업 내보내기와 복원의 실제 순서를 담당한다. (S28, DB_DESIGN 부록 B)
 *
 * 내보내기: 지금 계정의 약·규칙·기록·이력을 모두 모아 BackupData로 만든다 (삭제한 약과 그 기록도 포함).
 * 복원:     지금 계정의 약·규칙·기록·이력을 지우고 파일 내용으로 바꾼다. 다른 계정·설정·로그인 기억은 건드리지 않는다.
 *
 * 파일을 읽고 쓰는 일(DocumentFileIo)과 JSON 글자로 바꾸는 일(BackupJson)은 이 클래스 밖에서 한다.
 *
 * @param onChanged 복원한 뒤 부를 함수. 앱에서는 "알람을 다시 걸고, 고른 날짜·회차를 처음 값으로"가 들어온다.
 */
class BackupRepository(
    private val userDao: UserDao,
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao,
    private val doseRecordDao: DoseRecordDao,
    private val statusEventDao: DoseStatusEventDao,
    private val transaction: TransactionRunner,
    private val generator: DoseRecordGenerator,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val onChanged: suspend () -> Unit = {}
) {

    /** 지금 계정의 약 수와 기록 수. 바뀔 때마다 새 값이 흘러나온다. */
    fun observeCounts(userId: Long): Flow<BackupCounts> =
        combine(medicationDao.observeActiveCount(userId), doseRecordDao.observeCountForUser(userId)) { meds, records ->
            BackupCounts(meds, records)
        }

    /**
     * 내보낼 내용을 모은다.
     * 먼저 오늘까지의 빈 날 기록을 채운다 — 앱을 며칠 안 열었어도 그 사이 회차가 파일에 들어가게.
     *
     * 채우기와 표 네 개 읽기를 한 트랜잭션으로 묶는다. 따로 읽으면 그 사이에 약이 추가되거나 상태가 바뀌어
     * "약 목록은 옛것, 기록은 새것"처럼 서로 맞지 않는 파일이 만들어질 수 있다.
     */
    suspend fun export(userId: Long): BackupData = transaction.run {
        generator.generateForUser(userId)
        BackupData(
            version = BackupData.VERSION,
            // 초 단위까지만 적는다. 예: 2026-11-18T10:05:00+09:00
            exportedAt = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS).toString(),
            accountEmail = userDao.findById(userId)?.email,
            medications = medicationDao.findAllForUser(userId).map { it.toBackup() },
            schedules = scheduleDao.findAllForUser(userId).map { it.toBackup() },
            doseRecords = doseRecordDao.findAllEntitiesForUser(userId).map { it.toBackup() },
            statusEvents = statusEventDao.findAllForUser(userId).map { it.toBackup() }   // 오래된 것부터
        )
    }

    /**
     * 불러온 파일을 복원해도 되는지 살펴본다. DB는 건드리지 않는다.
     * 순서: 형식 검사 → 같은 계정의 백업인지 (이메일을 다듬어 비교한다).
     */
    suspend fun inspect(userId: Long, data: BackupData): RestoreCheck {
        val checked = BackupValidator.check(data, LocalDate.now(clock))
        if (checked !is BackupCheck.Ok) return RestoreCheck.Invalid

        val myEmail = userDao.findById(userId)?.email
        val fileEmail = data.accountEmail?.let(AuthValidator::normalizeEmail)
        if (myEmail == null || fileEmail != myEmail) return RestoreCheck.OtherAccount
        return RestoreCheck.Ready(checked.data)
    }

    /**
     * 복원: 지금 계정의 데이터를 파일 내용으로 통째로 바꾼다. [inspect]가 Ready로 돌려준 data만 넘긴다.
     *
     * 한 트랜잭션(전부 되거나 전부 안 되거나)으로 한다. 중간에 오류가 나면 원래 데이터가 그대로 남는다.
     *   ① 지우기: 이력 → 기록 → 규칙 → 약 (가리키는 쪽을 먼저 지운다. DB_DESIGN ⑧ "명시적 삭제 순서")
     *   ② 넣기:   약 → 규칙 → 기록 → 이력. 파일의 id는 쓰지 않고 DB가 새로 매긴 id로 서로를 다시 잇는다
     *            (파일의 id를 그대로 쓰면 이 폰의 다른 계정 줄과 번호가 겹칠 수 있다).
     *   ③ 채우기: 백업한 다음 날부터 오늘까지의 빈 날 기록. 같은 트랜잭션 안에서 한다 —
     *            여기서 오류가 나면 ①②도 없던 일이 되어 "바뀌었는데 실패했다고 안내하는" 일이 없다.
     * 그 뒤 알람을 다시 건다.
     */
    suspend fun restore(userId: Long, data: BackupData) {
        transaction.run {
            statusEventDao.deleteAllForUser(userId)
            doseRecordDao.deleteAllForUser(userId)
            scheduleDao.deleteAllForUser(userId)
            medicationDao.deleteAllForUser(userId)

            // 파일의 id → 새 id. 아래 줄을 넣을 때 이 표로 번호를 바꿔 끼운다.
            val medicationIds = data.medications.associate { it.id to medicationDao.insert(it.toEntity(userId)) }
            val scheduleIds = data.schedules.associate {
                it.id to scheduleDao.insert(it.toEntity(medicationIds.getValue(it.medicationId)))
            }
            val recordIds = data.doseRecords.associate {
                it.id to doseRecordDao.insert(it.toEntity(scheduleIds.getValue(it.scheduleId)))
            }
            // 이력은 파일 순서(오래된 것부터) 그대로 넣는다. 그래야 실행취소가 같은 줄을 고른다.
            statusEventDao.insertAll(
                data.statusEvents.map { event ->
                    event.toEntity(
                        userId = userId,
                        doseRecordId = event.doseRecordId?.let(recordIds::getValue),   // 지워진 기록의 이력은 null 그대로
                        scheduleId = scheduleIds.getValue(event.scheduleId!!)          // 형식 검사가 채워 둔 값
                    )
                }
            )
            generator.generateForUser(userId)
        }
        // 여기까지 왔으면 복원은 끝났다(DB에 반영됨). 뒤따르는 알람 다시 걸기에서 오류가 나도
        // "복원 실패"로 알리면 안 되므로 여기서 막는다. 알람은 앱을 다음에 열 때 다시 걸린다.
        try {
            onChanged()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 일부러 비워 둠
        }
    }

    // ----- 표의 줄 ↔ 파일의 줄 -----

    private fun Medication.toBackup() = BackupMedication(
        id, name, dosage, startDate, endDate, memo, createdAt, deletedAt, pausedFrom, hospital, department, sourceMemo
    )

    private fun Schedule.toBackup() =
        BackupSchedule(id, medicationId, time, repeatDays, effectiveFrom, createdAt, deletedAt, doseCondition)

    private fun DoseRecord.toBackup() = BackupDoseRecord(
        id, scheduleId, date, scheduledTime, medicationName, dosage, memo, status, actedAt, createdAt, doseCondition
    )

    private fun DoseStatusEvent.toBackup() = BackupStatusEvent(
        doseRecordId, scheduleId, doseDate, scheduledTime, medicationName,
        fromStatus, toStatus, fromActedAt, toActedAt, changedAt, isUndo
    )

    private fun BackupMedication.toEntity(userId: Long) = Medication(
        userId = userId, name = name, dosage = dosage, startDate = startDate, endDate = endDate, memo = memo,
        createdAt = createdAt, deletedAt = deletedAt, pausedFrom = pausedFrom,
        hospital = hospital, department = department, sourceMemo = sourceMemo
    )

    private fun BackupSchedule.toEntity(medicationId: Long) = Schedule(
        medicationId = medicationId, time = time, repeatDays = repeatDays,
        createdAt = createdAt, deletedAt = deletedAt, effectiveFrom = effectiveFrom, doseCondition = doseCondition
    )

    private fun BackupDoseRecord.toEntity(scheduleId: Long) = DoseRecord(
        scheduleId = scheduleId, date = date, scheduledTime = scheduledTime,
        medicationName = medicationName, dosage = dosage, memo = memo,
        status = status, actedAt = actedAt, createdAt = createdAt, doseCondition = doseCondition
    )

    private fun BackupStatusEvent.toEntity(userId: Long, doseRecordId: Long?, scheduleId: Long) = DoseStatusEvent(
        userId = userId, doseRecordId = doseRecordId, scheduleId = scheduleId,
        doseDate = doseDate!!, scheduledTime = scheduledTime!!, medicationName = medicationName!!,
        fromStatus = fromStatus, toStatus = toStatus, fromActedAt = fromActedAt, toActedAt = toActedAt,
        changedAt = changedAt, isUndo = isUndo
    )
}
