package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.MedicationDao
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.data.local.ScheduleDao
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.domain.MedicationDetail
import com.cau.medicheck.domain.MedicationDraft
import com.cau.medicheck.domain.MedicationError
import com.cau.medicheck.domain.MedicationValidator
import com.cau.medicheck.domain.RepeatDays
import com.cau.medicheck.domain.ScheduleItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalTime

/** 약 저장·삭제의 결과. 성공이면 약의 id, 실패면 이유가 들어 있다. */
sealed interface MedicationResult {
    data class Success(val medicationId: Long) : MedicationResult
    data class Failure(val error: MedicationError) : MedicationResult
}

/**
 * 약과 복용 시간 규칙의 등록·조회·수정·삭제를 담당한다.
 *
 * 화면(ViewModel)은 표 두 개(medications, schedules)를 따로 다루지 않고
 * "약 하나 = 약 정보 + 복용 시간들"(MedicationDetail)로만 주고받는다.
 * 표의 글자("2026-11-16", "08:00", "MON,WED")와 코드의 타입 사이의 변환도 여기서 한다.
 *
 * 모든 함수가 userId를 받는다. 다른 사람의 약을 읽거나 고칠 수 없게 주인을 확인하기 위해서다.
 *
 * 복용 기록과의 약속 (docs/DB_DESIGN.md ⑨):
 *   - 등록 직후: 오늘 치 기록을 바로 만든다 (등록하자마자 오늘 목록에 보이게).
 *   - 수정·삭제 직전: 바뀌기 전 규칙으로 오늘까지의 기록을 먼저 채운다 (과거 기록이 사라지지 않게).
 *   - 수정·삭제 직후: 아직 시각이 안 된 오늘 치 기록은 지우고, 새 규칙으로 다시 만든다.
 */
class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao,
    private val transaction: TransactionRunner,
    private val doseRecordGenerator: DoseRecordGenerator,
    private val now: () -> Long = System::currentTimeMillis
) {

    /**
     * 한 사용자의 사용 중인 약 목록을 계속 흘려 보낸다. (약이 바뀌면 새 목록이 다시 나온다)
     *
     * combine: 두 흐름(약 목록, 규칙 목록) 중 하나라도 바뀌면 둘을 합쳐 새 결과를 만든다.
     */
    fun observeMedications(userId: Long): Flow<List<MedicationDetail>> =
        combine(
            medicationDao.observeActive(userId),
            scheduleDao.observeActiveByUser(userId)
        ) { medications, schedules ->
            // 규칙들을 약 id별로 묶어 두고, 약마다 자기 규칙을 붙인다.
            val schedulesByMedication = schedules.groupBy { it.medicationId }
            medications.map { it.toDetail(schedulesByMedication[it.id].orEmpty()) }
        }

    /** 약 하나를 읽는다. 없거나, 삭제됐거나, 내 약이 아니면 null. */
    suspend fun getMedication(userId: Long, medicationId: Long): MedicationDetail? {
        val medication = findOwned(userId, medicationId) ?: return null
        return medication.toDetail(scheduleDao.findActiveByMedication(medicationId))
    }

    /** 약을 새로 등록한다. 약 한 줄과 복용 시간 여러 줄을 한 묶음(트랜잭션)으로 저장한다. */
    suspend fun addMedication(userId: Long, draft: MedicationDraft): MedicationResult {
        MedicationValidator.validate(draft)?.let { return MedicationResult.Failure(it) }

        val createdAt = now()
        val newMedication = Medication(
            userId = userId,
            name = draft.name.trim(),
            dosage = draft.dosage.trim(),
            startDate = draft.startDate.toString(),
            endDate = draft.endDate?.toString(),
            memo = draft.memo.trim().ifEmpty { null },
            createdAt = createdAt
        )
        val newId = transaction.run {
            val medicationId = medicationDao.insert(newMedication)
            draft.schedules.forEach { schedule ->
                scheduleDao.insert(
                    Schedule(
                        medicationId = medicationId,
                        time = schedule.time.toString(),
                        repeatDays = RepeatDays.encode(schedule.days),
                        createdAt = createdAt
                    )
                )
            }
            // 등록하자마자 오늘 목록에 보이도록 오늘까지의 복용 기록을 만든다.
            doseRecordGenerator.generateForMedication(newMedication.copy(id = medicationId))
            medicationId
        }
        return MedicationResult.Success(newId)
    }

    /**
     * 약을 수정한다.
     *
     * 복용 시간은 "지금 저장된 것"과 "화면에서 넘어온 것"을 비교해서 세 가지로 나눈다.
     *   - 넘어온 것에 id가 있고 저장된 것에도 있음 → 그 줄을 수정
     *   - 넘어온 것에 id가 없음                  → 새 줄 추가
     *   - 저장돼 있는데 넘어온 것에 없음           → soft delete
     */
    suspend fun updateMedication(
        userId: Long,
        medicationId: Long,
        draft: MedicationDraft
    ): MedicationResult {
        MedicationValidator.validate(draft)?.let { return MedicationResult.Failure(it) }
        val existing = findOwned(userId, medicationId)
            ?: return MedicationResult.Failure(MedicationError.NOT_FOUND)

        val changedAt = now()
        val updated = existing.copy(
            name = draft.name.trim(),
            dosage = draft.dosage.trim(),
            startDate = draft.startDate.toString(),
            endDate = draft.endDate?.toString(),
            memo = draft.memo.trim().ifEmpty { null }
        )
        transaction.run {
            // 바꾸기 전에, 지금까지의 규칙으로 오늘까지의 기록을 먼저 채워 둔다.
            doseRecordGenerator.generateForMedication(existing)

            medicationDao.update(updated)

            val saved = scheduleDao.findActiveByMedication(medicationId).associateBy { it.id }
            val keptIds = mutableSetOf<Long>()

            draft.schedules.forEach { schedule ->
                val savedSchedule = schedule.id?.let { saved[it] }
                if (savedSchedule != null) {
                    keptIds += savedSchedule.id
                    scheduleDao.update(
                        savedSchedule.copy(
                            time = schedule.time.toString(),
                            repeatDays = RepeatDays.encode(schedule.days)
                        )
                    )
                } else {
                    scheduleDao.insert(
                        Schedule(
                            medicationId = medicationId,
                            time = schedule.time.toString(),
                            repeatDays = RepeatDays.encode(schedule.days),
                            createdAt = changedAt
                        )
                    )
                }
            }

            saved.keys.filter { it !in keptIds }.forEach { scheduleDao.softDelete(it, changedAt) }

            // 아직 시각이 안 된 오늘 치 기록은 옛 규칙의 것이므로 지우고, 새 규칙으로 다시 만든다.
            doseRecordGenerator.discardUpcomingToday(medicationId)
            doseRecordGenerator.generateForMedication(updated)
        }
        return MedicationResult.Success(medicationId)
    }

    /** 약을 삭제한다(soft delete). 그 약의 복용 시간들도 함께 soft delete 한다. */
    suspend fun deleteMedication(userId: Long, medicationId: Long): MedicationResult {
        val existing = findOwned(userId, medicationId)
            ?: return MedicationResult.Failure(MedicationError.NOT_FOUND)

        val deletedAt = now()
        transaction.run {
            // 지우기 전에, 지금까지의 규칙으로 오늘까지의 기록을 먼저 채워 둔다.
            doseRecordGenerator.generateForMedication(existing)

            scheduleDao.softDeleteByMedication(medicationId, deletedAt)
            medicationDao.softDelete(medicationId, deletedAt)

            // 삭제한 약의, 아직 시각이 안 된 오늘 치 기록은 더 이상 먹을 일이 없으므로 지운다.
            doseRecordGenerator.discardUpcomingToday(medicationId)
        }
        return MedicationResult.Success(medicationId)
    }

    /** id로 약을 찾되, 내 약이고 삭제되지 않은 경우에만 돌려준다. */
    private suspend fun findOwned(userId: Long, medicationId: Long): Medication? =
        medicationDao.findById(medicationId)
            ?.takeIf { it.userId == userId && it.deletedAt == null }

    /** 표의 줄(글자) → 화면이 쓰는 모양(날짜·시간·요일 타입) */
    private fun Medication.toDetail(schedules: List<Schedule>) = MedicationDetail(
        id = id,
        name = name,
        dosage = dosage,
        startDate = LocalDate.parse(startDate),
        endDate = endDate?.let(LocalDate::parse),
        memo = memo.orEmpty(),
        schedules = schedules
            .map { ScheduleItem(it.id, LocalTime.parse(it.time), RepeatDays.decode(it.repeatDays)) }
            .sortedBy { it.time }
    )
}
