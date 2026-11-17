package com.cau.medicheck.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * dose_status_events 표: 복용 기록의 상태가 바뀔 때마다 한 줄씩 **쓰기만 하는** 이력. (DB_DESIGN ⑥-2)
 *
 * 예) "11/16 08:00 비타민 D: 미기록 → 복용 완료 (09:12에 바꿈)"
 * 변경 이력 화면(S30)이 이 표만 읽어 그리고, "마지막 변경 실행취소"가 이 표를 보고 되돌린다.
 *
 * - user_id: 누구의 이력인지. 사용자가 지워지면 함께 지워진다(CASCADE).
 * - dose_record_id: 바뀐 기록. 기록이 지워지면 이 칸만 null이 되고 이력 줄은 남는다(SET NULL).
 * - schedule_id·dose_date·scheduled_time·medication_name: 회차 정보의 복사본.
 *   기록이 지워져도 "어느 회차의 이력인지"를 이력 줄이 스스로 알게 하려고 복사해 둔다.
 *   (schedule_id는 FK가 아니라 값만 복사한 칸이다)
 * - from_status/to_status, from_acted_at/to_acted_at: 바뀌기 전·후 값. 실행취소 때 from 값으로 되돌린다.
 * - is_undo: 실행취소로 생긴 줄이면 true. 실행취소의 실행취소는 하지 않는다.
 */
@Entity(
    tableName = "dose_status_events",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DoseRecord::class,
            parentColumns = ["id"],
            childColumns = ["dose_record_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["user_id"]), Index(value = ["dose_record_id"])]
)
data class DoseStatusEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "dose_record_id")
    val doseRecordId: Long?,

    @ColumnInfo(name = "schedule_id")
    val scheduleId: Long,

    @ColumnInfo(name = "dose_date")
    val doseDate: String,

    @ColumnInfo(name = "scheduled_time")
    val scheduledTime: String,

    @ColumnInfo(name = "medication_name")
    val medicationName: String,

    @ColumnInfo(name = "from_status")
    val fromStatus: String,

    @ColumnInfo(name = "to_status")
    val toStatus: String,

    @ColumnInfo(name = "from_acted_at")
    val fromActedAt: Long?,

    @ColumnInfo(name = "to_acted_at")
    val toActedAt: Long?,

    @ColumnInfo(name = "changed_at")
    val changedAt: Long,

    @ColumnInfo(name = "is_undo", defaultValue = "0")
    val isUndo: Boolean = false
)
