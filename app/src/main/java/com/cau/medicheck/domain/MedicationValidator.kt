package com.cau.medicheck.domain

/**
 * 약 등록·수정 입력값이 규칙에 맞는지 검사한다. 순수 Kotlin 코드라서 단위 테스트를 할 수 있다.
 */
object MedicationValidator {

    const val NAME_MAX = 50
    const val DOSAGE_MAX = 30
    const val MEMO_MAX = 200

    /**
     * @return 문제가 없으면 null, 있으면 가장 먼저 발견한 문제 하나
     */
    fun validate(draft: MedicationDraft): MedicationError? {
        val name = draft.name.trim()
        val dosage = draft.dosage.trim()
        val endDate = draft.endDate

        return when {
            name.isEmpty() || name.length > NAME_MAX -> MedicationError.NAME_INVALID
            dosage.isEmpty() || dosage.length > DOSAGE_MAX -> MedicationError.DOSAGE_INVALID
            draft.memo.trim().length > MEMO_MAX -> MedicationError.MEMO_TOO_LONG
            endDate != null && endDate.isBefore(draft.startDate) -> MedicationError.END_BEFORE_START
            draft.schedules.isEmpty() -> MedicationError.NO_SCHEDULE
            draft.schedules.any { it.days.isEmpty() } -> MedicationError.SCHEDULE_NO_DAYS
            hasOverlap(draft.schedules) -> MedicationError.SCHEDULE_DUPLICATE
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
