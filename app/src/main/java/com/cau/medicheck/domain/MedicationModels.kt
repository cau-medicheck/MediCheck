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
    val days: Set<DayOfWeek>,
    /** 이 규칙이 회차를 만들기 시작하는 날. 없으면 약의 시작일부터 */
    val effectiveFrom: LocalDate? = null,
    /** 복용 조건 (식전·식후 등) */
    val condition: DoseCondition = DoseCondition.NONE
)

/** 약 하나와 그 약의 복용 시간 규칙들. 목록 화면과 수정 화면이 읽는 모양. */
data class MedicationDetail(
    val id: Long,
    val name: String,
    val dosage: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val memo: String,
    val schedules: List<ScheduleItem>,
    /** 일시중지를 시작한 날. 중지 중이 아니면 null */
    val pausedFrom: LocalDate? = null,
    /** 처방 출처 메모(S42): 의료기관 · 진료과 · 추가 메모. 없으면 "" */
    val hospital: String = "",
    val department: String = "",
    val sourceMemo: String = "",
    /** 아이콘 모양·색 (내 약 목록·약 상세) */
    val shape: MedicationShape = MedicationShape.TABLET,
    val color: MedicationColor = MedicationColor.TEAL
)

/**
 * 저장하려는 복용 시간 규칙 하나.
 * @param id 이미 저장돼 있던 규칙이면 그 id, 새로 추가한 규칙이면 null
 */
data class ScheduleDraft(
    val id: Long? = null,
    val time: LocalTime,
    val days: Set<DayOfWeek>,
    val condition: DoseCondition = DoseCondition.NONE
)

/** 등록·수정 화면에서 "저장"을 눌렀을 때 넘어오는 입력값 묶음. */
data class MedicationDraft(
    val name: String,
    val dosage: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val memo: String,
    val schedules: List<ScheduleDraft>,
    /** 등록할 때만: false면 "쉬는 중"으로 등록한다(일정에 넣지 않고 등록만). 수정할 때는 쓰지 않는다 */
    val startActive: Boolean = true,
    /** 아이콘 모양·색 */
    val shape: MedicationShape = MedicationShape.TABLET,
    val color: MedicationColor = MedicationColor.TEAL
)

/**
 * 약 저장·삭제가 실패하는 이유의 목록. 화면이 이 값에 맞는 문구를 고른다.
 * 묶음은 프로토타입의 안내 문구 네 가지에 맞췄다 (검사 순서도 이 순서다).
 */
enum class MedicationError {
    NAME_DOSAGE_TIME_INVALID,  // ① 이름·복용량이 비었거나 너무 김, 복용 시간이 하나도 없음
    NO_DAYS,                   // ② 반복 요일이 하나도 없음
    DATES_INVALID,             // ③ 종료일이 시작일보다 앞
    DUPLICATE_TIME,            // ④ 같은 요일에 같은 시각이 두 번
    NOT_FOUND                  // 약이 없거나 내 약이 아님
}

/**
 * 약을 수정할 때 "언제부터 적용할지" (S33 일정 변경 적용 범위).
 *   FROM_TODAY    : 오늘 이후 회차에 적용. 오늘 아직 시각 전인 회차도 새 설정으로 바뀐다.
 *   FROM_TOMORROW : 내일부터 적용. 오늘 회차는 옛 설정 그대로 둔다.
 * 어느 쪽이든 지난 날짜의 기록과 이미 응답한 기록은 바뀌지 않는다.
 */
enum class ApplyScope { FROM_TODAY, FROM_TOMORROW }
