package com.cau.medicheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** schedules 표의 창구. 조회는 삭제되지 않은 규칙만 돌려준다. */
@Dao
interface ScheduleDao {

    @Insert
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule)

    /** 약 하나의 사용 중인 규칙들. 시간 순. */
    @Query(
        "SELECT * FROM schedules " +
            "WHERE medication_id = :medicationId AND deleted_at IS NULL " +
            "ORDER BY time"
    )
    suspend fun findActiveByMedication(medicationId: Long): List<Schedule>

    /**
     * 한 사용자의 모든 약에 걸친 사용 중인 규칙들.
     * schedules에는 user_id가 없으므로 medications와 이어 붙여(JOIN) 주인을 가려낸다.
     */
    @Query(
        "SELECT s.* FROM schedules s " +
            "JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId AND m.deleted_at IS NULL AND s.deleted_at IS NULL " +
            "ORDER BY s.time"
    )
    fun observeActiveByUser(userId: Long): Flow<List<Schedule>>

    /**
     * 한 사용자의 사용 중인 규칙 전부를, 알림에 필요한 약 정보와 함께 읽는다.
     * 일시중지 중인 약(paused_from이 있음)은 알림을 울리지 않으므로 여기서 뺀다.
     */
    @Query(
        "SELECT s.id AS scheduleId, m.id AS medicationId, m.name AS medicationName, " +
            "m.dosage AS dosage, m.start_date AS startDate, m.end_date AS endDate, " +
            "s.time AS time, s.repeat_days AS repeatDays, s.effective_from AS effectiveFrom " +
            "FROM schedules s " +
            "JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId AND m.deleted_at IS NULL AND s.deleted_at IS NULL " +
            "AND m.paused_from IS NULL"
    )
    suspend fun findReminderRows(userId: Long): List<ReminderRow>

    @Query("UPDATE schedules SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    /** 약을 삭제할 때 그 약의 규칙을 한꺼번에 soft delete 한다. */
    @Query(
        "UPDATE schedules SET deleted_at = :deletedAt " +
            "WHERE medication_id = :medicationId AND deleted_at IS NULL"
    )
    suspend fun softDeleteByMedication(medicationId: Long, deletedAt: Long)

    /**
     * 이 약의 사용 중인 규칙마다 effective_from = max(지금 값, date) 로 올린다.
     * (일시중지를 풀 때, 끝난 약의 종료일을 늘릴 때 — 비어 있던 기간을 '기한 경과'로 채우지 않게. DB_DESIGN ⑤·⑧-2)
     *
     * 날짜가 "2026-11-16" 모양의 글자라서 글자 비교(<)가 곧 날짜 비교다.
     * 약의 주인이 userId일 때만 바뀐다(다른 사람 약이면 0줄).
     * @return 바뀐 줄 수
     */
    @Query(
        "UPDATE schedules SET effective_from = :date " +
            "WHERE deleted_at IS NULL " +
            "AND (effective_from IS NULL OR effective_from < :date) " +
            "AND medication_id IN (SELECT id FROM medications WHERE id = :medicationId AND user_id = :userId)"
    )
    suspend fun raiseEffectiveFrom(userId: Long, medicationId: Long, date: String): Int

    /**
     * 한 사용자의 규칙 줄을 모두 지운다(물리 삭제). 샘플 계정 다시 만들기에서만 쓴다.
     * 이 규칙들의 기록은 이 함수보다 **먼저** 지워 둔다 (DB_DESIGN ⑧ "명시적 삭제 순서").
     */
    @Query(
        "DELETE FROM schedules " +
            "WHERE medication_id IN (SELECT id FROM medications WHERE user_id = :userId)"
    )
    suspend fun deleteAllForUser(userId: Long)
}
