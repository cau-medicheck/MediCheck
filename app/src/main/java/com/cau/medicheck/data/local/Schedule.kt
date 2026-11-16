package com.cau.medicheck.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * schedules 표: "매일 08:00", "월·수·금 20:00" 같은 반복 규칙 하나가 한 줄. (docs/DB_DESIGN.md ⑤)
 *
 * - medication_id: 어느 약의 규칙인지.
 * - time: "08:00" 같은 24시간제 글자.
 * - repeat_days: "MON,WED,FRI"처럼 요일을 쉼표로 이은 글자. 매일이면 7개 전부.
 *   글자 ↔ 요일 묶음 변환은 domain/RepeatDays.kt가 한다.
 * - deleted_at: medications와 같은 soft delete.
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medication_id"])]
)
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "medication_id")
    val medicationId: Long,

    val time: String,

    @ColumnInfo(name = "repeat_days")
    val repeatDays: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)
