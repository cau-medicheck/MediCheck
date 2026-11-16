package com.cau.medicheck.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/*
 * 화면과 로직이 쓰는 "약" 모양.
 *
 * DB 표(Medication, Schedule)는 날짜·시간·요일을 글자로 들고 있어서 그대로 쓰기 불편하다.
 * 그래서 Repository가 표에서 읽은 것을 아래 모양으로 바꿔서 내보낸다.
 *   LocalDate = 날짜(2026-11-16), LocalTime = 시각(08:00). 둘 다 Java 기본 타입.
 */

/** 복용 시간 규칙 하나: 몇 시에, 무슨 요일에. */
data class ScheduleItem(
    val id: Long,
    val time: LocalTime,
    val days: Set<DayOfWeek>
)

/** 약 하나와 그 약의 복용 시간 규칙들. 목록 화면과 수정 화면이 읽는 모양. */
data class MedicationDetail(
    val id: Long,
    val name: String,
    val dosage: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val memo: String,
    val schedules: List<ScheduleItem>
)

/**
 * 저장하려는 복용 시간 규칙 하나.
 * @param id 이미 저장돼 있던 규칙이면 그 id, 새로 추가한 규칙이면 null
 */
data class ScheduleDraft(
    val id: Long? = null,
    val time: LocalTime,
    val days: Set<DayOfWeek>
)

/** 등록·수정 화면에서 "저장"을 눌렀을 때 넘어오는 입력값 묶음. */
data class MedicationDraft(
    val name: String,
    val dosage: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val memo: String,
    val schedules: List<ScheduleDraft>
)

/** 약 저장·삭제가 실패하는 이유의 목록. 화면이 이 값에 맞는 문구를 고른다. */
enum class MedicationError {
    NAME_INVALID,            // 이름이 비었거나 너무 김
    DOSAGE_INVALID,          // 복용량이 비었거나 너무 김
    MEMO_TOO_LONG,           // 메모가 너무 김
    END_BEFORE_START,        // 종료일이 시작일보다 앞
    NO_SCHEDULE,             // 복용 시간이 하나도 없음
    SCHEDULE_NO_DAYS,        // 요일을 하나도 고르지 않은 복용 시간이 있음
    SCHEDULE_DUPLICATE,      // 같은 시간·같은 요일이 겹치는 복용 시간이 있음
    NOT_FOUND                // 약이 없거나 내 약이 아님
}
