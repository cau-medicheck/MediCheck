package com.cau.medicheck.domain

/**
 * 약 등록·수정 입력값이 규칙에 맞는지 검사한다. 순수 Kotlin 코드라서 단위 테스트를 할 수 있다.
 */
object MedicationValidator {

    const val NAME_MAX = 50
    const val DOSAGE_MAX = 30
    const val MEMO_MAX = 200

    /**
     * 프로토타입과 같은 순서로 검사한다. 여러 개가 틀려도 가장 앞의 것 하나만 돌려준다.
     *   ① 이름·복용량·복용 시간  ② 반복 요일  ③ 시작일·종료일  ④ 같은 시각 중복
     * 이름·복용량·메모는 앞뒤 공백을 뺀 값으로 본다. 종료일이 시작일과 같은 것은 괜찮다.
     *
     * @return 문제가 없으면 null, 있으면 가장 먼저 발견한 문제 하나
     */
    fun validate(draft: MedicationDraft): MedicationError? {
        val name = draft.name.trim()
        val dosage = draft.dosage.trim()
        val endDate = draft.endDate

        return when {
            name.isEmpty() || name.length > NAME_MAX -> MedicationError.NAME_DOSAGE_TIME_INVALID
            dosage.isEmpty() || dosage.length > DOSAGE_MAX -> MedicationError.NAME_DOSAGE_TIME_INVALID
            // 메모가 너무 긴 경우는 화면에서 입력 자체를 막으므로 여기까지 오지 않는다(백업 복원 등 대비).
            draft.memo.trim().length > MEMO_MAX -> MedicationError.NAME_DOSAGE_TIME_INVALID
            draft.schedules.isEmpty() -> MedicationError.NAME_DOSAGE_TIME_INVALID
            draft.schedules.any { it.days.isEmpty() } -> MedicationError.NO_DAYS
            endDate != null && endDate.isBefore(draft.startDate) -> MedicationError.DATES_INVALID
            hasOverlap(draft.schedules) -> MedicationError.DUPLICATE_TIME
            else -> null
        }
    }

    /**
     * 같은 시각이면서 요일이 하나라도 겹치는 규칙이 있는지 본다.
     * 예) "08:00 매일"과 "08:00 월·수"는 월·수 08:00이 두 번이 되므로 겹친다.
     *     "08:00 월·수"와 "08:00 화·목"은 겹치지 않는다.
     */
    private fun hasOverlap(schedules: List<ScheduleDraft>): Boolean {
        for (i in schedules.indices) {
            for (j in i + 1 until schedules.size) {
                val a = schedules[i]
                val b = schedules[j]
                if (a.time == b.time && a.days.any { it in b.days }) return true
            }
        }
        return false
    }
}
