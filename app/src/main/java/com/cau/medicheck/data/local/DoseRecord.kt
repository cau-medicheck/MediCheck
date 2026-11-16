package com.cau.medicheck.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * dose_records 표: "2026-11-16 08:00 타이레놀 → 복용 완료" 같은 실제 건 하나가 한 줄. (docs/DB_DESIGN.md ⑥)
 *
 * schedules가 "매일 08:00"이라는 규칙이라면, 이 표는 그 규칙이 날짜마다 만들어 낸 실제 건이다.
 *
 * - schedule_id: 어느 규칙에서 나온 건인지.
 * - date: 복용 예정 날짜 ("2026-11-16").
 * - scheduled_time: 이 줄을 만들 당시의 예정 시각 ("08:00"). 나중에 규칙의 시간이 바뀌어도 그대로 남는다.
 * - status: "PENDING"(아직 응답 없음) / "TAKEN"(복용 완료) / "SKIPPED"(일부러 건너뜀).
 *           "깜빡함(MISSED)"은 저장하지 않는다. PENDING인데 시각이 지났으면 깜빡한 것으로 계산한다.
 * - acted_at: 사용자가 버튼을 누른 실제 시각. 아직 누르지 않았으면 null.
 *
 * indices의 unique = true: 같은 규칙·같은 날짜의 줄은 하나만 있을 수 있다.
 * 그래서 앱을 여러 번 열어 기록 생성이 반복되어도 중복으로 생기지 않는다.
 */
@Entity(
    tableName = "dose_records",
    foreignKeys = [
        ForeignKey(
            entity = Schedule::class,
            parentColumns = ["id"],
            childColumns = ["schedule_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["schedule_id", "date"], unique = true)]
)
data class DoseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "schedule_id")
    val scheduleId: Long,

    val date: String,

    @ColumnInfo(name = "scheduled_time")
    val scheduledTime: String,

    val status: String,

    @ColumnInfo(name = "acted_at")
    val actedAt: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

/**
 * 복용 기록 한 줄에 약 이름·복용량을 붙인 조회 결과.
 *
 * 표가 아니라 "세 표(dose_records, schedules, medications)를 이어 붙여 읽은 결과"의 모양이다.
 * DoseRecordDao의 조회문에서 `AS 이름`으로 붙인 이름과 여기 필드 이름이 같아야 Room이 채워 준다.
 */
data class DoseRecordRow(
    val id: Long,
    val scheduleId: Long,
    val date: String,
    val scheduledTime: String,
    val status: String,
    val actedAt: Long?,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String
)
