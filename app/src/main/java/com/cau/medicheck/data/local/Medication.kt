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
 * - paused_from: 일시중지를 시작한 날. null이면 복용 중.
 *   이날부터는 기록·알림을 만들지 않는다. "일시중지 켬"을 누른 그날로만 정해지므로
 *   null이 아니면 "지금 중지 중"이라는 뜻이다. 재개하면 다시 null.
 * - hospital / department / source_memo: 처방 의료기관·진료과·처방 출처 메모(S42).
 *   source_memo는 복용 메모(memo)와 다른 칸이다.
 * - 복용 시간과 요일은 여기 없고 schedules 표에 있다. 약 하나에 시간이 여러 개일 수 있어서다.
 *
 * 새 칸 4개에 Kotlin 기본값(= null)을 둔 이유: 이 칸을 모르는 기존 코드(약 등록 등)가
 * 고치지 않아도 그대로 컴파일되게 하기 위해서다.
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
    val deletedAt: Long? = null,

    @ColumnInfo(name = "paused_from")
    val pausedFrom: String? = null,

    val hospital: String? = null,

    val department: String? = null,

    @ColumnInfo(name = "source_memo")
    val sourceMemo: String? = null,

    /** 아이콘 모양 (domain/MedicationShape 이름 글자, version 7) */
    @ColumnInfo(name = "shape", defaultValue = "TABLET")
    val shape: String = "TABLET",

    /** 아이콘 색 (domain/MedicationColor 이름 글자, version 7) */
    @ColumnInfo(name = "color", defaultValue = "TEAL")
    val color: String = "TEAL"
)
