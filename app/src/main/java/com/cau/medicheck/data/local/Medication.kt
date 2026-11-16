package com.cau.medicheck.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * medications 표: 약 하나가 한 줄. (docs/DB_DESIGN.md ④)
 *
 * - user_id: 이 약의 주인. users 표의 id를 가리킨다(ForeignKey = 다른 표의 줄을 가리키는 칸).
 *   onDelete = CASCADE: 사용자가 지워지면 그 사용자의 약도 함께 지워진다.
 * - 날짜는 "2026-11-16" 같은 글자로 저장한다.
 * - deleted_at: 약을 "삭제"해도 줄을 지우지 않고 여기에 삭제 시각만 적는다(soft delete).
 *   과거 복용 기록과 통계를 남기기 위해서다. null이면 사용 중인 약.
 * - 복용 시간과 요일은 여기 없고 schedules 표에 있다. 약 하나에 시간이 여러 개일 수 있어서다.
 */
@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"])]
)
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    val name: String,

    val dosage: String,

    @ColumnInfo(name = "start_date")
    val startDate: String,

    @ColumnInfo(name = "end_date")
    val endDate: String?,

    val memo: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)
