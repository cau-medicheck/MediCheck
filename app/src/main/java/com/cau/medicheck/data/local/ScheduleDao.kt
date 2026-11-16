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

    @Query("UPDATE schedules SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    /** 약을 삭제할 때 그 약의 규칙을 한꺼번에 soft delete 한다. */
    @Query(
        "UPDATE schedules SET deleted_at = :deletedAt " +
            "WHERE medication_id = :medicationId AND deleted_at IS NULL"
    )
    suspend fun softDeleteByMedication(medicationId: Long, deletedAt: Long)
}
