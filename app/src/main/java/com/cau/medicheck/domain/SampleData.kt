package com.cau.medicheck.domain

/**
 * "샘플 계정으로 체험하기"가 만드는 예시 데이터. v3 프로토타입의 fresh()를 옮긴 것이다. (DB_DESIGN 부록 D)
 * 안드로이드 기능을 쓰지 않는 순수 Kotlin이라 단위 테스트로 개수를 확인할 수 있다.
 *
 * 만드는 것: 약 3개(각각 매일 한 번), 약마다 32일 전부터 오늘까지 하루 1줄의 기록 = 33일 × 3 = 99줄.
 *   상태: TAKEN 73 · SKIPPED 12 · PENDING 14 (어제까지 96줄 = 72 · 12 · 12, 오늘 3줄 = 1 · 0 · 2)
 *
 * 2026-11-17 결정 6: 약 시작일을 32일 전으로 해서 기록(지난 32일치)과 맞춘다.
 * 01 명세의 처음 안(시작일 38일 전, 117줄)과 다르다. 그 안에서는 38~33일 전의 6일이
 * 기록 공식 밖이라 전부 '기한 경과'가 되었는데, 시작일을 당겨 그 구간이 없어졌다.
 * 끝난 약(종료일이 지난 약)은 더하지 않는다.
 */
object SampleData {
    const val EMAIL = "demo@medicheck.app"
    const val DISPLAY_NAME = "사용자"
    const val DISPLAY_NAME_EN = "User"

    /** 약의 시작일 = 오늘 - 32일. 기록도 이날부터 오늘까지 만든다. */
    const val START_OFFSET = -32

    /** 매일 = 7개 요일 전부 */
    val REPEAT_DAYS: String = RepeatDays.encode(RepeatDays.EVERY_DAY)

    /**
     * 샘플 약 하나. no는 v3의 약 번호(1·2·3)로, 상태 공식에 쓰인다.
     * 처음에는 메모에 "아침 식후"를 적었으나, 복용 조건이 생긴 뒤(2026-11-23)로는 조건으로 옮겼다(메모는 비움).
     */
    data class Med(
        val no: Int, val name: String, val dosage: String, val time: String, val condition: DoseCondition,
        val shape: MedicationShape = MedicationShape.TABLET, val color: MedicationColor = MedicationColor.TEAL
    )

    val MEDS = listOf(
        Med(1, "비타민 D", "1정", "08:00", DoseCondition.AFTER_MEAL, MedicationShape.TABLET, MedicationColor.ORANGE),
        Med(2, "오메가3", "1캡슐", "12:30", DoseCondition.AFTER_MEAL, MedicationShape.CAPSULE, MedicationColor.BLUE),
        Med(3, "마그네슘", "1정", "21:00", DoseCondition.BEDTIME, MedicationShape.TABLET, MedicationColor.PURPLE),
        // 4·5번은 v3에 없던 약(2026-11-29 추가): 같은 시각(08:00)에 약이 여러 개인 모습과 통계 순위를 보여 주려고 넣었다.
        Med(4, "루테인", "1캡슐", "08:00", DoseCondition.BEFORE_MEAL, MedicationShape.CAPSULE, MedicationColor.PINK),
        Med(5, "유산균", "2캡슐", "08:00", DoseCondition.EMPTY_STOMACH, MedicationShape.CAPSULE, MedicationColor.TEAL)
    )

    /** 같은 약 3개의 영어 이름·복용량. 번호·시각·조건·모양은 MEDS와 같다. */
    val MEDS_EN = listOf(
        MEDS[0].copy(name = "Vitamin D", dosage = "1 tablet"),
        MEDS[1].copy(name = "Omega-3", dosage = "1 capsule"),
        MEDS[2].copy(name = "Magnesium", dosage = "1 tablet"),
        MEDS[3].copy(name = "Lutein", dosage = "1 capsule"),
        MEDS[4].copy(name = "Probiotics", dosage = "2 capsules")
    )

    /** 기기 언어에 맞는 샘플 약 목록 */
    fun meds(korean: Boolean): List<Med> = if (korean) MEDS else MEDS_EN

    /** 기기 언어에 맞는 샘플 계정 이름 */
    fun displayName(korean: Boolean): String = if (korean) DISPLAY_NAME else DISPLAY_NAME_EN

    /** 기록을 만드는 날의 범위: 오늘 기준 며칠 전인지 (-32 ~ 0) */
    val DAY_OFFSETS: IntRange = START_OFFSET..0

    /**
     * 그날 그 약의 기록 상태 (v3 fresh()의 공식 그대로).
     * @param dayOffset 오늘이 0, 어제가 -1
     * @param medNo 약 번호 1~5
     *
     * 음수 나머지: Kotlin의 %는 부호가 앞 숫자를 따르지만(-9 % 9 == 0), "0인지"만 보므로 v3(JavaScript)와 같은 결과다.
     */
    fun statusFor(dayOffset: Int, medNo: Int): DoseStatus = when {
        dayOffset == 0 -> if (medNo == 1) DoseStatus.TAKEN else DoseStatus.PENDING
        (dayOffset + medNo) % 9 == 0 -> DoseStatus.SKIPPED
        (dayOffset + medNo) % 7 == 0 -> DoseStatus.PENDING
        else -> DoseStatus.TAKEN
    }

    /**
     * 복용 완료 기록의 "실제로 먹은 시각"이 예정 시각보다 몇 분 늦었는지 (-10 ~ 76분, 음수면 일찍).
     * 20번에 14번쯤은 제시간(30분 안), 5번은 30분~1시간 늦게, 1번은 1시간 넘게 늦게. 약마다 몇 분씩 다르게 민다(시간대별 평균이 똑같아 보이지 않게).
     * 통계의 "제시간에 먹었나"가 비어 보이지 않게 넣는 값이다. 무작위가 아니라 날짜와 약 번호로 정해져서 언제 만들어도 같다.
     * 오늘(0) 기록에는 넣지 않는다(null): 아직 오지 않은 시각이 "먹은 시각"이 될 수 있어서.
     */
    fun actedDelayMinutes(dayOffset: Int, medNo: Int): Int? =
        if (dayOffset == 0) null else when (val step = (-dayOffset * 7 + medNo * 11) % 20) {
            in 0..13 -> step * 2 - 6
            in 14..18 -> 30 + (step - 13) * 5
            else -> 70
        } + (medNo * 5) % 12 - 4
}
