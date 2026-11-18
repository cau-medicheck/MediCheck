package com.cau.medicheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * dose_status_events 표의 창구.
 * 이력 줄은 고치지 않는다. 넣기·읽기와, 연결 끊기(detach)·계정 단위 지우기만 있다.
 */
@Dao
interface DoseStatusEventDao {

    @Insert
    suspend fun insert(event: DoseStatusEvent): Long

    /**
     * 한 기록의 가장 최근 이력 한 줄. 없으면 null. (실행취소)
     * 같은 시각이면 id가 큰 쪽(나중에 넣은 줄)이 최근이다.
     */
    @Query(
        "SELECT * FROM dose_status_events " +
            "WHERE user_id = :userId AND dose_record_id = :recordId " +
            "ORDER BY changed_at DESC, id DESC LIMIT 1"
    )
    suspend fun latestFor(userId: Long, recordId: Long): DoseStatusEvent?

    /** 한 사용자의 이력 전체, 최신순. (변경 이력 화면 S30 — 2단계에서 씀) */
    @Query(
        "SELECT * FROM dose_status_events WHERE user_id = :userId " +
            "ORDER BY changed_at DESC, id DESC"
    )
    fun observeForUser(userId: Long): Flow<List<DoseStatusEvent>>

    /**
     * 한 사용자의 이력 전체, **오래된 것부터**. (백업 S28 — 6단계에서 씀)
     * 파일에 이 순서로 쓰고 복원 때 같은 순서로 넣어야, 같은 시각인 줄의 앞뒤가 유지된다.
     */
    @Query(
        "SELECT * FROM dose_status_events WHERE user_id = :userId " +
            "ORDER BY changed_at ASC, id ASC"
    )
    suspend fun findAllForUser(userId: Long): List<DoseStatusEvent>

    /**
     * 지울 기록들을 가리키는 이력의 연결을 끊는다(dose_record_id = null).
     * 기록을 지우기 **직전에** 부른다. FK의 SET NULL이 켜져 있는지에 기대지 않고 직접 한다(DB_DESIGN ⑦).
     * 이력 줄은 회차 정보를 복사해 들고 있으므로 연결이 끊겨도 어느 회차의 이력인지 안다.
     */
    @Query(
        "UPDATE dose_status_events SET dose_record_id = NULL " +
            "WHERE user_id = :userId AND dose_record_id IN (:recordIds)"
    )
    suspend fun detachRecords(userId: Long, recordIds: List<Long>)

    /** 한 사용자의 이력을 모두 지운다. (샘플 계정 다시 만들기 — 명시적 삭제 순서의 첫 단계) */
    @Query("DELETE FROM dose_status_events WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: Long)

    /** 여러 줄을 목록 순서대로 넣는다. (S28 복원 — 파일의 순서가 새 id의 순서가 된다) */
    @Insert
    suspend fun insertAll(events: List<DoseStatusEvent>)
}
