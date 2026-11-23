package com.cau.medicheck.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * dose_records 표: "2026-11-16 08:00 비타민 D 1정 → 복용 완료" 같은 실제 회차 하나가 한 줄. (docs/DB_DESIGN.md ⑥)
 *
 * schedules가 "매일 08:00"이라는 규칙이라면, 이 표는 그 규칙이 날짜마다 만들어 낸 실제 회차다.
 *
 * - schedule_id: 어느 규칙에서 나온 회차인지.
 * - date: 복용 예정 날짜 ("2026-11-16").
 * - 스냅샷(snapshot = 그때의 값을 복사해 찍어 둔 사본) 4칸:
 *     scheduled_time(예정 시각), medication_name(약 이름), dosage(복용량), memo(약 메모).
 *   기록을 만들 때 한 번 채우고 그 뒤로는 바꾸지 않는다. 그래서 나중에 약 이름을 고치거나
 *   약을 지워도 지난 회차는 "그때의 이름·복용량·시각"으로 보인다.
 *   (예외 하나: 오늘·아직 응답 없음·예정 시각 전인 기록은 약을 고칠 때 새 값으로 다시 맞춘다 — DB_DESIGN ⑧)
 * - status: "PENDING"(미기록) / "TAKEN"(복용 완료) / "SKIPPED"(건너뜀).
 *           "기한 경과"는 저장하지 않는다. PENDING인데 시각이 지났으면 볼 때마다 계산한다.
 * - acted_at: 복용 완료면 실제 복용 시각(처음엔 누른 시각), 건너뜀이면 누른 시각, 미기록이면 null.
 *
 * indices의 unique = true: 같은 규칙·같은 날짜의 줄은 하나만 있을 수 있다.
 * 그래서 앱을 여러 번 열어 기록 생성이 반복되어도 중복으로 생기지 않는다.
 * 이 두 값(schedule_id, date)이 "회차의 열쇠"(domain의 DoseKey)다.
 *
 * medicationName·dosage에 Kotlin 기본값을 두지 않은 이유: 빈 이름으로 기록이 만들어지면
 * 화면에 빈 약 이름이 보이므로, 기록을 만드는 모든 곳에서 값을 빠뜨리면 컴파일 오류가 나게 했다.
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

    @ColumnInfo(name = "medication_name")
    val medicationName: String,

    val dosage: String,

    val memo: String? = null,

    val status: String,

    @ColumnInfo(name = "acted_at")
    val actedAt: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /** 복용 조건 스냅샷: 기록을 만들 때 규칙 줄의 조건을 복사해 둔다(나중에 조건을 바꿔도 지난 기록은 그대로) */
    @ColumnInfo(name = "dose_condition", defaultValue = "NONE")
    val doseCondition: String = "NONE"
)

/**
 * 복용 기록 한 줄을 화면이 읽기 좋게 모은 조회 결과.
 *
 * 표가 아니라 "세 표(dose_records, schedules, medications)를 이어 붙여 읽은 결과"의 모양이다.
 * 약 이름·복용량·메모는 기록의 스냅샷 칸에서 읽는다. medications는 "내 기록인지" 확인과
 * 약 번호(medicationId)를 얻는 데만 쓴다.
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
    val dosage: String,
    val memo: String?,
    val doseCondition: String = "NONE"
)

/** "어느 날짜에 기록이 몇 개" 한 줄. (S32의 날짜 목록 조회 결과) */
data class DateCount(val date: String, val count: Int)
