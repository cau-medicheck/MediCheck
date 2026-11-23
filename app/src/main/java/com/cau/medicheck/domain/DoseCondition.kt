package com.cau.medicheck.domain

/**
 * 복용 조건: 약을 "언제에 맞춰" 먹는지. 복용 시간(규칙 줄)마다 하나씩 고른다. (2026-11-23 결정)
 *
 * 시간대(새벽·아침·점심·저녁·밤)는 시각을 보고 앱이 정하고(DayPeriod), 복용 조건은 사용자가 고른다.
 * 둘을 나눈 이유: 같은 08:00에 "식전" 약과 "식후" 약이 함께 있어도 화면에서 서로 부딪히지 않게 하려는 것이다.
 *
 * DB에는 이름 글자("AFTER_MEAL")로 저장한다(schedules.dose_condition, dose_records.dose_condition).
 * 화면 글자는 strings.xml에 있다(ui 쪽에서 고른다).
 */
enum class DoseCondition {
    NONE,           // 상관없음 (기본값, 화면에 꼬리표를 붙이지 않는다)
    BEFORE_MEAL,    // 식전
    AFTER_MEAL,     // 식후
    EMPTY_STOMACH,  // 공복
    BEDTIME;        // 취침 전

    companion object {
        /** DB·백업의 글자 → 값. 모르는 글자나 null(옛 데이터)이면 NONE */
        fun parse(text: String?): DoseCondition = entries.firstOrNull { it.name == text } ?: NONE
    }
}
