package com.cau.medicheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * medications 표의 창구.
 *
 * 조회는 기본적으로 "삭제되지 않은 약"(deleted_at IS NULL)만 돌려준다.
 */
@Dao
interface MedicationDao {

    @Insert
    suspend fun insert(medication: Medication): Long

    /** id가 같은 줄을 통째로 바꾼다. */
    @Update
    suspend fun update(medication: Medication)

    /** 삭제된 약도 찾는다. (수정·삭제 전에 주인을 확인하는 데 쓴다) */
    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun findById(id: Long): Medication?

    /**
     * 한 사용자의 사용 중인 약 목록.
     *
     * Flow: 한 번 돌려주고 끝나는 게 아니라, 표가 바뀔 때마다 새 목록을 계속 흘려 보낸다.
     * 그래서 약을 추가·수정·삭제하면 목록 화면이 저절로 갱신된다.
     * (Flow를 돌려주는 함수는 suspend를 붙이지 않는다.)
     */
    @Query(
        "SELECT * FROM medications " +
            "WHERE user_id = :userId AND deleted_at IS NULL " +
            "ORDER BY name COLLATE NOCASE"
    )
    fun observeActive(userId: Long): Flow<List<Medication>>

    /** 한 사용자의 사용 중인 약 목록을 한 번만 읽는다. (복용 기록을 만들 때 쓴다) */
    @Query("SELECT * FROM medications WHERE user_id = :userId AND deleted_at IS NULL")
    suspend fun findActive(userId: Long): List<Medication>

    /** soft delete: 줄을 지우지 않고 삭제 시각만 적는다. */
    @Query("UPDATE medications SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
