package com.cau.medicheck.domain

/*
 * 약의 겉모습: 내 약 목록·약 상세에 보이는 아이콘의 모양과 색. 약을 등록할 때 고른다. (2026-11-24)
 * 약 이름만으로는 한눈에 구분하기 어려워, 실제 약과 비슷한 모양·색을 붙여 알아보기 쉽게 한다.
 * DB에는 이름 글자("CAPSULE", "BLUE")로 저장한다(medications.shape, medications.color).
 * 실제 화면 색 값은 ui 쪽(McMedIcon)이 정한다.
 */

/** 약 모양 */
enum class MedicationShape {
    TABLET,   // 알약 (기본값)
    CAPSULE,  // 캡슐
    LIQUID,   // 물약
    POWDER;   // 가루

    companion object {
        /** DB·백업의 글자 → 값. 모르는 글자나 null(옛 데이터)이면 알약 */
        fun parse(text: String?): MedicationShape = entries.firstOrNull { it.name == text } ?: TABLET
    }
}

/** 약 색 */
enum class MedicationColor {
    TEAL,     // 청록 (기본값)
    ORANGE,
    BLUE,
    PURPLE,
    PINK,
    GRAY;

    companion object {
        /** DB·백업의 글자 → 값. 모르는 글자나 null(옛 데이터)이면 청록 */
        fun parse(text: String?): MedicationColor = entries.firstOrNull { it.name == text } ?: TEAL
    }
}
