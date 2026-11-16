package com.cau.medicheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** dose_records 표의 창구. */
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
     * 한 사용자의, 날짜 범위 안의 기록들. 약 이름·복용량을 붙여서 돌려준다.
     *
     * 삭제된 약·규칙의 기록도 포함한다 (이력과 통계에는 남아야 하므로 deleted_at 조건이 없다).
     * 날짜가 "2026-11-16" 모양의 글자라서 글자 비교(BETWEEN)가 곧 날짜 비교가 된다.
     */
    @Query(
        "SELECT r.id AS id, r.schedule_id AS scheduleId, r.date AS date, " +
            "r.scheduled_time AS scheduledTime, r.status AS status, r.acted_at AS actedAt, " +
            "m.id AS medicationId, m.name AS medicationName, m.dosage AS dosage " +
            "FROM dose_records r " +
            "JOIN schedules s ON s.id = r.schedule_id " +
            "JOIN medications m ON m.id = s.medication_id " +
            "WHERE m.user_id = :userId AND r.date BETWEEN :fromDate AND :toDate " +
            "ORDER BY r.date, r.scheduled_time, m.name COLLATE NOCASE"
    )
    fun observeRange(userId: Long, fromDate: String, toDate: String): Flow<List<DoseRecordRow>>

    /** 기록 하나를 찾되, 그 기록이 이 사용자의 것일 때만 돌려준다. */
    @Query(
        "SELECT r.* FROM dose_records r " +
            "JOIN schedules s ON s.id = r.schedule_id " +
            "JOIN medications m ON m.id = s.medication_id " +
            "WHERE r.id = :recordId AND m.user_id = :userId"
    )
    suspend fun findOwned(recordId: Long, userId: Long): DoseRecord?

    @Query("UPDATE dose_records SET status = :status, acted_at = :actedAt WHERE id = :recordId")
    suspend fun updateStatus(recordId: Long, status: String, actedAt: Long?)

    /**
     * 한 약의 "아직 응답하지 않았고, 예정 시각도 아직 안 된" 그날의 기록을 지운다.
     *
     * 약이나 복용 시간을 수정·삭제했을 때만 쓴다. 바뀌기 전 규칙으로 만들어 둔 오늘 치 기록이
     * 그대로 남아 있으면 틀린 시각에 "깜빡함"으로 표시되기 때문이다.
     * 지운 뒤에는 새 규칙으로 다시 만든다. (이미 응답했거나 시각이 지난 기록은 건드리지 않는다.)
     */
    @Query(
        "DELETE FROM dose_records " +
            "WHERE date = :date AND status = 'PENDING' AND scheduled_time > :afterTime " +
            "AND schedule_id IN (SELECT id FROM schedules WHERE medication_id = :medicationId)"
    )
    suspend fun deleteUpcomingPending(medicationId: Long, date: String, afterTime: String)
}
