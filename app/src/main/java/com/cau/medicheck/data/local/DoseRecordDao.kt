package com.cau.medicheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * dose_records 표의 창구.
 *
 * 계정 격리: 쓰기 함수와 "번호로 찾는" 함수는 userId를 받아, 조회문 안에서
 * "이 기록의 약 주인 = userId"인지 확인한다. 다른 사람의 번호가 들어와도 0줄 변경·null이다.
 * (예외: insertAllIgnore·lastDate는 DoseRecordGenerator가 방금 주인을 확인한 규칙으로만 부른다)
 */
@Dao
interface DoseRecordDao {

    /**
     * 기록 여러 줄을 한꺼번에 넣는다.
     * IGNORE: 이미 같은 (규칙, 날짜)의 줄이 있으면 오류를 내지 않고 그 줄만 건너뛴다.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(records: List<DoseRecord>)

    /** 한 규칙에서 마지막으로 기록을 만든 날짜. 기록이 하나도 없으면 null. */
    @Query("SELECT MAX(date) FROM dose_records WHERE schedule_id = :scheduleId")
    suspend fun lastDate(scheduleId: Long): String?

    /**
     * 한 사용자의, 날짜 범위 안의 기록들.
     *
     * 약 이름·복용량·메모는 기록의 스냅샷 칸(r.medication_name 등)에서 읽는다.
     * 그래서 약 이름을 바꿔도 지난 기록은 그때 이름으로 나온다.
     * 삭제된 약·규칙의 기록도 포함한다 (이력과 통계에는 남아야 하므로 deleted_at 조건이 없다).
     * 날짜가 "2026-11-16" 모양의 글자라서 글자 비교(BETWEEN)가 곧 날짜 비교가 된다.
     */
    @Query(
        "SELECT r.id AS id, r.schedule_id AS scheduleId, r.date AS date, " +
            "r.scheduled_time AS scheduledTime, r.status AS status, r.acted_at AS actedAt, " +
            "m.id AS medicationId, r.medication_name AS medicationName, " +
            "r.dosage AS dosage, r.memo AS memo " +
            "FROM dose_records r " +
            "JOIN schedules s ON s.id = r.schedule_id " +
            "JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId AND r.date BETWEEN :fromDate AND :toDate " +
            "ORDER BY r.date, r.scheduled_time, r.medication_name COLLATE NOCASE"
    )
    fun observeRange(userId: Long, fromDate: String, toDate: String): Flow<List<DoseRecordRow>>

    /**
     * 회차 하나(규칙 번호 + 날짜)의 기록을 계속 흘려 보낸다. 내 기록이 아니거나 없으면 null. (S10·S31)
     * 고르는 칸은 observeRange와 같다.
     */
    @Query(
        "SELECT r.id AS id, r.schedule_id AS scheduleId, r.date AS date, " +
            "r.scheduled_time AS scheduledTime, r.status AS status, r.acted_at AS actedAt, " +
            "m.id AS medicationId, r.medication_name AS medicationName, " +
            "r.dosage AS dosage, r.memo AS memo " +
            "FROM dose_records r " +
            "JOIN schedules s ON s.id = r.schedule_id " +
            "JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId AND r.schedule_id = :scheduleId AND r.date = :date"
    )
    fun observeOwnedRowByKey(userId: Long, scheduleId: Long, date: String): Flow<DoseRecordRow?>

    /** 기록 하나를 번호로 찾되, 그 기록이 이 사용자의 것일 때만 돌려준다. */
    @Query(
        "SELECT r.* FROM dose_records r " +
            "JOIN schedules s ON s.id = r.schedule_id " +
            "JOIN medications m ON m.id = s.medication_id " +
            "WHERE r.id = :recordId AND m.user_id = :userId"
    )
    suspend fun findOwned(userId: Long, recordId: Long): DoseRecord?

    /**
     * 회차 열쇠(규칙 번호, 날짜)로 이 사용자의 기록을 찾는다. 없으면 null.
     * 삭제된 약의 기록도 찾는다 (지난 기록의 상태를 고칠 수 있게 — 결정 5).
     */
    @Query(
        "SELECT r.* FROM dose_records r " +
            "JOIN schedules s ON s.id = r.schedule_id " +
            "JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId AND r.schedule_id = :scheduleId AND r.date = :date"
    )
    suspend fun findOwnedByKey(userId: Long, scheduleId: Long, date: String): DoseRecord?

    /**
     * 기록의 상태와 acted_at을 바꾼다. 이 사용자의 기록일 때만 바뀐다.
     * 상태 변경 이력은 DoseRepository가 같은 트랜잭션에서 따로 쓴다.
     * @return 바뀐 줄 수 (0 또는 1)
     */
    @Query(
        "UPDATE dose_records SET status = :status, acted_at = :actedAt " +
            "WHERE id = :recordId AND schedule_id IN (" +
            "SELECT s.id FROM schedules s JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId)"
    )
    suspend fun updateStatus(userId: Long, recordId: Long, status: String, actedAt: Long?): Int

    /**
     * 한 약의 "오늘 남은 미기록" = 그 날짜이면서 PENDING이고 예정 시각이 afterTime보다 뒤인 기록들.
     * 약을 수정·삭제·일시중지한 직후 "오늘 치 다시 맞추기"(DB_DESIGN ⑧)에서 고치거나 지울 대상을 고른다.
     * 삭제된 규칙의 기록도 포함한다(빠진 시간 줄의 오늘 기록도 지워야 하므로).
     */
    @Query(
        "SELECT r.* FROM dose_records r " +
            "JOIN schedules s ON s.id = r.schedule_id " +
            "JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId AND m.id = :medicationId " +
            "AND r.date = :date AND r.status = 'PENDING' AND r.scheduled_time > :afterTime"
    )
    suspend fun findUpcomingPending(
        userId: Long,
        medicationId: Long,
        date: String,
        afterTime: String
    ): List<DoseRecord>

    /**
     * "오늘 남은 미기록" 한 줄의 스냅샷을 새 값으로 맞춘다 (DB_DESIGN ⑧ "고친다" 갈래).
     * 줄을 지우고 새로 만들지 않고 고치는 이유: id가 그대로라 상태 변경 이력과의 연결이 끊기지 않는다.
     * 안전을 위해 PENDING인 줄만 바꾼다.
     */
    @Query(
        "UPDATE dose_records SET scheduled_time = :time, medication_name = :name, " +
            "dosage = :dosage, memo = :memo " +
            "WHERE id = :recordId AND status = 'PENDING' AND schedule_id IN (" +
            "SELECT s.id FROM schedules s JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId)"
    )
    suspend fun updateUpcomingSnapshot(
        userId: Long,
        recordId: Long,
        time: String,
        name: String,
        dosage: String,
        memo: String?
    ): Int

    /**
     * 한 약의, 날짜가 endDate보다 뒤이고 아직 미기록(PENDING)인 기록 번호들.
     * 종료일을 앞으로 당겼을 때 지울 대상을 고른다(DB_DESIGN ⑧ "종료일 당김 정리").
     * 삭제된 규칙의 기록도 포함한다. 이 사용자의 약일 때만 나온다.
     */
    @Query(
        "SELECT r.id FROM dose_records r " +
            "JOIN schedules s ON s.id = r.schedule_id " +
            "JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId AND m.id = :medicationId " +
            "AND r.date > :endDate AND r.status = 'PENDING'"
    )
    suspend fun findPendingAfter(userId: Long, medicationId: Long, endDate: String): List<Long>

    /**
     * 기록 여러 줄을 번호로 지운다 (DB_DESIGN ⑧ "지운다" 갈래). 이 사용자의 기록만 지워진다.
     * 부르기 전에 DoseStatusEventDao.detachRecords로 이력의 연결을 먼저 끊어 둔다.
     */
    @Query(
        "DELETE FROM dose_records WHERE id IN (:recordIds) AND schedule_id IN (" +
            "SELECT s.id FROM schedules s JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId)"
    )
    suspend fun deleteByIds(userId: Long, recordIds: List<Long>)

    /**
     * 한 사용자의 기록을 모두 지운다(물리 삭제). 샘플 계정 다시 만들기에서만 쓴다.
     * 이력은 먼저 지우고, 규칙·약은 이 뒤에 지운다 (DB_DESIGN ⑧ "명시적 삭제 순서").
     */
    @Query(
        "DELETE FROM dose_records WHERE schedule_id IN (" +
            "SELECT s.id FROM schedules s JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId)"
    )
    suspend fun deleteAllForUser(userId: Long)
}
