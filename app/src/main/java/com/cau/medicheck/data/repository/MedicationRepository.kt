package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.MedicationDao
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.data.local.ScheduleDao
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.domain.ApplyScope
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.MedicationDetail
import com.cau.medicheck.domain.MedicationDraft
import com.cau.medicheck.domain.MedicationError
import com.cau.medicheck.domain.MedicationValidator
import com.cau.medicheck.domain.RepeatDays
import com.cau.medicheck.domain.ScheduleItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
 *   - 수정·삭제 직후: "오늘 치 다시 맞추기" — 아직 시각이 안 된 오늘 치 미기록을 새 값으로 고치거나 지우고,
 *     새 규칙으로 다시 만든다 (DoseRecordGenerator.reconcileUpcomingToday).
 *   - 수정 때 시각·요일이 바뀐 줄과 새로 더한 줄에는 적용 시작일(effective_from)을 적는다.
 *     그래서 지난 날짜가 새 설정으로 거슬러 채워지지 않는다.
 *     (언제부터 적용할지는 S33에서 고른다: 오늘 이후 / 내일부터 — ApplyScope)
 *
 * @param clock "오늘"과 "지금 시각"을 알려 주는 시계. 테스트에서는 멈춘 시계를 넣는다.
 * @param onChanged 약이 등록·수정·삭제된 뒤에 부를 함수. 앱에서는 "알림을 다시 예약하라"가 들어온다.
 *                  (이 클래스가 알림을 직접 알지 않아도 되게 함수로 받는다. 테스트에서는 비워 둔다.)
 */
class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao,
    private val transaction: TransactionRunner,
    private val doseRecordGenerator: DoseRecordGenerator,
    private val now: () -> Long = System::currentTimeMillis,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val onChanged: suspend () -> Unit = {}
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
            createdAt = createdAt,
            // "바로 복용 시작"을 끄고 등록하면 쉬는 중으로 만든다. 시작일(이 지났으면 그날)부터 멈춰 두어
            // 기록이 하나도 생기지 않게 한다 → 켜기 전에 '미복용'이 쌓이지 않는다. 켜면 그날부터 일정에 들어간다.
            pausedFrom = if (draft.startActive) null else minOf(draft.startDate, LocalDate.now(clock)).toString()
        )
        val newId = transaction.run {
            val medicationId = medicationDao.insert(newMedication)
            draft.schedules.forEach { schedule ->
                scheduleDao.insert(
                    Schedule(
                        medicationId = medicationId,
                        time = schedule.time.format(TIME_FORMAT),
                        repeatDays = RepeatDays.encode(schedule.days),
                        createdAt = createdAt,
                        doseCondition = schedule.condition.name
                    )
                )
            }
            // 등록하자마자 오늘 목록에 보이도록 오늘까지의 복용 기록을 만든다.
            doseRecordGenerator.generateForMedication(newMedication.copy(id = medicationId))
            medicationId
        }
        onChanged()
        return MedicationResult.Success(newId)
    }

    /**
     * 약을 수정한다.
     *
     * 복용 시간은 "지금 저장된 것"과 "화면에서 넘어온 것"을 규칙 id로 짝지어 네 가지로 나눈다.
     * (시각 글자를 비교해 짝짓지 않는다 — DB_DESIGN ⑤ "줄 맞추기 규칙")
     *   - id가 있고 시각이나 요일이 달라짐 → 그 줄을 고치고 effective_from = 적용일
     *   - id가 있고 값이 같음             → 손대지 않음 (단 effective_from이 내일 이후이고 그 줄의 시각이
 *                                      아직 오지 않았으면 오늘로 내림)
     *   - id가 없음                      → 새 줄 추가, effective_from = 적용일
     *   - 저장돼 있는데 넘어온 것에 없음    → soft delete
     *
     * 적용일("오늘 시각 규칙"): 그 줄의 새 시각이 지금보다 뒤면 오늘, 이미 지났으면 내일.
     * 저장하자마자 '기한 경과'가 되는 회차가 생기지 않게 하기 위해서다.
     *
     * [scope]가 FROM_TOMORROW("내일부터 적용")이면 적용일은 언제나 내일이고, 오늘 치 기록은 건드리지 않는다.
     *
     * 종료일: 끝난 약의 종료일을 늘리면 규칙 줄의 적용 시작일을 적용일까지 올리고(끝나 있던 날을 채우지 않게),
     * 종료일을 앞으로 당기면 새 종료일 뒤의 미기록을 지운다 (DB_DESIGN ⑤·⑧).
     */
    suspend fun updateMedication(
        userId: Long,
        medicationId: Long,
        draft: MedicationDraft,
        scope: ApplyScope = ApplyScope.FROM_TODAY
    ): MedicationResult {
        MedicationValidator.validate(draft)?.let { return MedicationResult.Failure(it) }
        val found = findOwned(userId, medicationId)
            ?: return MedicationResult.Failure(MedicationError.NOT_FOUND)

        val changedAt = now()
        val today = LocalDate.now(clock)
        val nowTime = LocalTime.now(clock).format(TIME_FORMAT)
        // 줄 하나의 적용일. "내일부터 적용"이면 언제나 내일, "오늘 이후"면 오늘 시각 규칙(아직이면 오늘, 지났으면 내일).
        fun applyDateOf(time: String): String =
            if (scope == ApplyScope.FROM_TOMORROW) today.plusDays(1).toString()
            else applyDate(time, today, nowTime).toString()
        transaction.run {
            // 트랜잭션 안에서 약을 다시 읽는다. 밖에서 읽은 값으로 줄 전체를 덮어쓰면, 그 사이에 저장된
            // 일시중지·처방 출처가 옛 값으로 되돌아갈 수 있다.
            val existing = findOwned(userId, medicationId) ?: found
            val updated = existing.copy(
                name = draft.name.trim(),
                dosage = draft.dosage.trim(),
                startDate = draft.startDate.toString(),
                endDate = draft.endDate?.toString(),
                memo = draft.memo.trim().ifEmpty { null }
            )
            // 바꾸기 전에, 지금까지의 규칙으로 오늘까지의 기록을 먼저 채워 둔다.
            doseRecordGenerator.generateForMedication(existing)

            medicationDao.update(updated)

            val saved = scheduleDao.findActiveByMedication(medicationId).associateBy { it.id }
            val keptIds = mutableSetOf<Long>()
            val timeChangedIds = mutableSetOf<Long>()   // 시각이나 요일이 바뀐 줄 (미리 건너뜀을 지울 대상)

            draft.schedules.forEach { schedule ->
                val savedSchedule = schedule.id?.let { saved[it] }
                val newTime = schedule.time.format(TIME_FORMAT)
                val newDays = RepeatDays.encode(schedule.days)
                val newCondition = schedule.condition.name
                if (savedSchedule != null) {
                    keptIds += savedSchedule.id
                    val changed = savedSchedule.time != newTime || savedSchedule.repeatDays != newDays
                    if (changed) {
                        timeChangedIds += savedSchedule.id
                        scheduleDao.update(
                            savedSchedule.copy(
                                time = newTime,
                                repeatDays = newDays,
                                effectiveFrom = applyDateOf(newTime),
                                doseCondition = newCondition
                            )
                        )
                    } else if (savedSchedule.doseCondition != newCondition) {
                        // 복용 조건만 바뀜: 회차가 생기는 날은 그대로라 적용 시작일을 건드리지 않는다.
                        // 오늘 남은 기록의 조건은 아래 reconcileUpcomingToday가 맞춘다("오늘 이후"일 때).
                        scheduleDao.update(savedSchedule.copy(doseCondition = newCondition))
                    } else if (scope == ApplyScope.FROM_TODAY &&
                        savedSchedule.effectiveFrom != null && savedSchedule.effectiveFrom > today.toString()
                    ) {
                        // 같은 날 앞서 "내일부터"로 바꿔 둔 줄을 "오늘부터"로 다시 저장한 경우.
                        // 무조건 오늘로 내리면 안 된다: 시각이 이미 지난 줄을 오늘로 내리면 오늘 기록이 새로 생기자마자
                        // '기한 경과'가 된다(오늘 시각 규칙을 거스름). 그래서 바뀐 줄과 같은 규칙으로 적용일을 다시 구해,
                        // 그 날이 지금 값보다 앞일 때만 내린다. (시각이 지난 줄은 내일 그대로)
                        val target = applyDate(savedSchedule.time, today, nowTime).toString()
                        if (target < savedSchedule.effectiveFrom) {
                            scheduleDao.update(savedSchedule.copy(effectiveFrom = target))
                        }
                    }
                } else {
                    scheduleDao.insert(
                        Schedule(
                            medicationId = medicationId,
                            time = newTime,
                            repeatDays = newDays,
                            createdAt = changedAt,
                            effectiveFrom = applyDateOf(newTime),
                            doseCondition = newCondition
                        )
                    )
                }
            }

            val removedIds = saved.keys.filter { it !in keptIds }
            removedIds.forEach { scheduleDao.softDelete(it, changedAt) }

            // 미리 건너뛴 앞날의 기록 정리: 시각·요일이 바뀌었거나 빠진 줄의 것은 그날 회차가 맞지 않으므로 지운다.
            val staleIds = timeChangedIds + removedIds
            if (staleIds.isNotEmpty()) doseRecordGenerator.deleteFutureRecords(userId, medicationId, staleIds)

            // 끝난 약의 종료일을 늘린 경우 (DB_DESIGN ⑤ "종료일을 늘릴 때").
            // 그냥 두면 생성기가 "마지막 기록일 + 1일"부터 오늘까지 채워서, 약이 끝나 있던 날들이
            // 모두 '기한 경과 · 미기록'이 된다. 그래서 남은 규칙 줄마다 적용 시작일을 적용일까지 올린다.
            // 미래였던 시작일을 오늘이나 그 전으로 당긴 경우도 같다: 그냥 두면 새 시작일부터 오늘까지가
            // 한꺼번에 채워져 '기한 경과'가 되고, "내일부터 적용"을 골랐는데도 오늘 기록이 생긴다.
            val startPulledIn = existing.startDate > today.toString() && updated.startDate <= today.toString()
            if (extendsEndedMedication(existing, updated, today) || startPulledIn) {
                scheduleDao.findActiveByMedication(medicationId).forEach { schedule ->
                    // 줄마다 "오늘 시각 규칙"을 쓴다: 시각이 아직이면 오늘부터, 지났으면 내일부터.
                    val target = applyDateOf(schedule.time)
                    if (schedule.effectiveFrom == null || schedule.effectiveFrom < target) {
                        scheduleDao.update(schedule.copy(effectiveFrom = target))
                    }
                }
            }

            // 종료일을 앞으로 당긴 경우 (DB_DESIGN ⑧ "종료일 당김 정리").
            // 새 종료일 뒤의 미기록은 더 이상 먹을 일이 없는데, 남겨 두면 '기한 경과'로 보이고 통계 분모에 들어간다.
            // 응답한 기록(복용 완료·건너뜀)은 남긴다. 지우기 전에 이력의 연결을 먼저 끊는다(이력 줄은 남음).
            val newEnd = updated.endDate
            if (newEnd != null && (existing.endDate == null || newEnd < existing.endDate)) {
                doseRecordGenerator.deletePendingAfterEnd(userId, medicationId, LocalDate.parse(newEnd))
                // 새 종료일 뒤에 미리 건너뛴 기록도 지운다(그날은 더 이상 회차가 아니다)
                doseRecordGenerator.deleteFutureRecords(userId, medicationId, after = LocalDate.parse(newEnd))
            }

            // "오늘 이후 회차에 적용": 아직 시각이 안 된 오늘 치 기록은 옛 값으로 만든 것이므로 새 값으로
            // 고치거나 지우고, 지운 자리는 새 규칙으로 다시 만든다.
            // "내일부터 적용": 오늘 치는 옛 설정 그대로 둔다(바뀐 줄의 적용 시작일이 내일이라 새로 생기지도 않는다).
            if (scope == ApplyScope.FROM_TODAY) doseRecordGenerator.reconcileUpcomingToday(userId, updated)
            doseRecordGenerator.generateForMedication(updated)
        }
        onChanged()
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
            doseRecordGenerator.reconcileUpcomingToday(userId, existing.copy(deletedAt = deletedAt))
            // 미리 건너뛴 앞날의 기록도 지운다(그날 회차가 없어졌다)
            doseRecordGenerator.deleteFutureRecords(userId, medicationId)
        }
        onChanged()
        return MedicationResult.Success(medicationId)
    }

    /**
     * 일시중지를 켜거나(paused = true) 끈다(재개). (S07의 스위치, DB_DESIGN ⑧-2)
     *
     * 켤 때: ① 지금까지의 규칙으로 오늘까지 기록을 채우고 ② 중지 시작일 = 오늘 ③ 오늘 아직 시각 전인 미기록을 지운다.
     *        이미 응답한 기록과 시각이 지난 오늘 기록, 지난 날짜 기록은 남는다. 중지한 동안에는 기록이 생기지 않는다.
     * 끌 때: ① 중지 시작일을 지우고 ② 규칙마다 적용 시작일을 오늘로 올린 뒤(중지했던 날짜를 다시 채우지 않게)
     *        ③ 오늘 치 기록을 다시 만든다.
     * 이미 그 상태면 아무것도 하지 않는다(두 번 눌림 대비).
     */
    suspend fun setPaused(userId: Long, medicationId: Long, paused: Boolean): MedicationResult {
        val existing = findOwned(userId, medicationId)
            ?: return MedicationResult.Failure(MedicationError.NOT_FOUND)
        if ((existing.pausedFrom != null) == paused) return MedicationResult.Success(medicationId)

        val today = LocalDate.now(clock).toString()
        transaction.run {
            if (paused) {
                doseRecordGenerator.generateForMedication(existing)
                medicationDao.setPausedFrom(userId, medicationId, today)
                doseRecordGenerator.reconcileUpcomingToday(userId, existing.copy(pausedFrom = today))
                // 미리 건너뛴 앞날의 기록도 지운다(멈춘 동안은 회차가 없다)
                doseRecordGenerator.deleteFutureRecords(userId, medicationId)
            } else {
                medicationDao.setPausedFrom(userId, medicationId, null)
                // 다시 켤 때 규칙 줄마다 "오늘 시각 규칙": 시각이 아직이면 오늘부터, 이미 지났으면 내일부터.
                // 그냥 오늘부터로 하면 10시에 켰을 때 08:00 회차가 오늘 기록으로 생겨 곧바로 '미복용'이 된다 (2026-11-24).
                val nowTime = LocalTime.now(clock).format(TIME_FORMAT)
                scheduleDao.findActiveByMedication(medicationId).forEach { schedule ->
                    val target = applyDate(schedule.time, LocalDate.now(clock), nowTime).toString()
                    if (schedule.effectiveFrom == null || schedule.effectiveFrom < target) {
                        scheduleDao.update(schedule.copy(effectiveFrom = target))
                    }
                }
                // 생성기는 넘겨받은 약의 값으로 계산하므로, 중지가 풀린 값을 넘겨야 오늘 치가 생긴다.
                doseRecordGenerator.generateForMedication(existing.copy(pausedFrom = null))
            }
        }
        onChanged()   // 알림을 다시 계산한다 (중지한 약은 알림 대상에서 빠진다)
        return MedicationResult.Success(medicationId)
    }

    /**
     * 처방 출처 메모를 저장한다. (S42) 앞뒤 공백을 빼고, 빈 값은 "없음"(NULL)으로 저장한다.
     * 일정과 상관없는 정보라 복용 기록이나 알림은 건드리지 않는다.
     */
    suspend fun updateSource(
        userId: Long,
        medicationId: Long,
        hospital: String,
        department: String,
        sourceMemo: String
    ): MedicationResult {
        fun clean(text: String) = text.trim().ifEmpty { null }
        val changed = medicationDao.updateSource(userId, medicationId, clean(hospital), clean(department), clean(sourceMemo))
        return if (changed > 0) MedicationResult.Success(medicationId)
        else MedicationResult.Failure(MedicationError.NOT_FOUND)
    }

    /**
     * 같은 이름의 내 약들. 앞뒤 공백과 대소문자는 무시한다. (S08 등록 → S40 중복 확인)
     * 일시중지·종료된 약도 포함하고, 삭제한 약과 다른 사람의 약은 빠진다. 등록한 순서로 돌려준다.
     */
    suspend fun findSameName(userId: Long, name: String): List<MedicationDetail> {
        val needle = name.trim().lowercase()
        return medicationDao.findActive(userId)
            .filter { it.name.trim().lowercase() == needle }
            .sortedBy { it.id }
            .map { it.toDetail(scheduleDao.findActiveByMedication(it.id)) }
    }

    /**
     * 바뀐 줄·새 줄이 회차를 만들기 시작하는 날 (DB_DESIGN ⑤ "오늘 시각 규칙").
     * 새 시각이 지금보다 뒤면 오늘부터, 이미 지났으면 내일부터.
     * ("08:05" 모양의 글자라 글자 비교가 곧 시각 비교다)
     */
    private fun applyDate(newTime: String, today: LocalDate, nowTime: String): LocalDate =
        if (newTime > nowTime) today else today.plusDays(1)

    /**
     * 이미 끝난 약(옛 종료일 < 오늘)의 종료일을 뒤로 늘렸는가(또는 종료일을 없앴는가).
     * ("2026-11-16" 모양의 글자라 글자 비교가 곧 날짜 비교다)
     */
    private fun extendsEndedMedication(existing: Medication, updated: Medication, today: LocalDate): Boolean {
        val oldEnd = existing.endDate ?: return false
        if (oldEnd >= today.toString()) return false
        val newEnd = updated.endDate
        return newEnd == null || newEnd > oldEnd
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
            .map {
                ScheduleItem(
                    it.id, LocalTime.parse(it.time), RepeatDays.decode(it.repeatDays),
                    effectiveFrom = it.effectiveFrom?.let(LocalDate::parse),
                    condition = DoseCondition.parse(it.doseCondition)
                )
            }
            .sortedBy { it.time },
        pausedFrom = pausedFrom?.let(LocalDate::parse),
        hospital = hospital.orEmpty(),
        department = department.orEmpty(),
        sourceMemo = sourceMemo.orEmpty()
    )

    private companion object {
        /** "08:05" 모양. 표의 time 칸과 같은 꼴로만 만든다 (LocalTime.toString은 초가 붙을 수 있어서 쓰지 않음) */
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
