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
 * (예외: 주인을 확인하는 findById — 삭제된 약도 찾아야 "내 약인지"를 판단할 수 있다)
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
            "ORDER BY id"   // 등록한 순서 (프로토타입의 목록 순서)
    )
    fun observeActive(userId: Long): Flow<List<Medication>>

    /** 한 사용자의 사용 중인 약 목록을 한 번만 읽는다. (복용 기록을 만들 때 쓴다) */
    @Query("SELECT * FROM medications WHERE user_id = :userId AND deleted_at IS NULL")
    suspend fun findActive(userId: Long): List<Medication>

    /** soft delete: 줄을 지우지 않고 삭제 시각만 적는다. */
    @Query("UPDATE medications SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    /**
     * 일시중지 시작일을 적거나(date = 오늘) 지운다(date = null, 재개). (DB_DESIGN ⑧-2)
     *
     * WHERE에 user_id를 함께 거는 이유: 다른 사람의 약 번호가 들어와도 0줄만 바뀌게 하기 위해서다(계정 격리).
     * @return 바뀐 줄 수 (내 약이 아니면 0)
     */
    @Query(
        "UPDATE medications SET paused_from = :date " +
            "WHERE id = :medicationId AND user_id = :userId AND deleted_at IS NULL"
    )
    suspend fun setPausedFrom(userId: Long, medicationId: Long, date: String?): Int

    /**
     * 처방 출처 메모(의료기관·진료과·추가 메모)를 고친다. (S42)
     * 일정과 상관없는 정보라 이 세 칸만 바꾼다.
     * @return 바뀐 줄 수 (내 약이 아니거나 삭제된 약이면 0)
     */
    @Query(
        "UPDATE medications SET hospital = :hospital, department = :department, source_memo = :sourceMemo " +
            "WHERE id = :medicationId AND user_id = :userId AND deleted_at IS NULL"
    )
    suspend fun updateSource(
        userId: Long,
        medicationId: Long,
        hospital: String?,
        department: String?,
        sourceMemo: String?
    ): Int

    /**
     * 한 사용자의 약 줄을 모두 지운다(물리 삭제). 샘플 계정 다시 만들기에서만 쓴다.
     * 이 약들에 딸린 규칙·기록은 이 함수보다 **먼저** 지워 둔다 (DB_DESIGN ⑧ "명시적 삭제 순서").
     */
    @Query("DELETE FROM medications WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: Long)

    /** 한 사용자의 사용 중인 약 수. 바뀔 때마다 새 값이 흘러나온다. (S28 "약 n개") */
    @Query("SELECT COUNT(*) FROM medications WHERE user_id = :userId AND deleted_at IS NULL")
    fun observeActiveCount(userId: Long): Flow<Int>

    /** 한 사용자의 약 전부, 삭제한 약도 포함. (S28 백업 — 과거 기록이 가리키는 약까지 파일에 넣어야 한다) */
    @Query("SELECT * FROM medications WHERE user_id = :userId ORDER BY id")
    suspend fun findAllForUser(userId: Long): List<Medication>
}
