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
 * - effective_from: 이 규칙(지금 값)이 회차를 만들기 시작하는 날. null이면 약의 시작일부터.
 *   약을 고칠 때 "오늘부터/내일부터 적용"을 지키고, 새로 더한 시간이 지난 날짜까지
 *   거슬러 채워지지 않게 하려고 둔 칸이다.
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
    val deletedAt: Long? = null,

    @ColumnInfo(name = "effective_from")
    val effectiveFrom: String? = null
)

/**
 * 알림을 예약하는 데 필요한 것만 모은 조회 결과: 규칙 한 줄 + 그 약의 이름·복용량·기간.
 * 표가 아니라 schedules와 medications를 이어 붙여 읽은 결과의 모양이다.
 *
 * 일시중지 칸(pausedFrom)은 두지 않는다. 조회문(WHERE)에서 중지 중인 약을 이미 뺐기 때문이다.
 */
data class ReminderRow(
    val scheduleId: Long,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val startDate: String,
    val endDate: String?,
    val time: String,
    val repeatDays: String,
    val effectiveFrom: String?
)
