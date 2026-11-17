package com.cau.medicheck.domain

import java.time.LocalDate

/** 약의 상태. 목록(S06)과 상세(S07)의 배지를 정한다. */
enum class MedicationStatus {
    ACTIVE,   // 복용 중
    PAUSED,   // 일시중지
    ENDED     // 종료 (종료일이 지남)
}

/**
 * 약의 상태를 정한다. 위에서부터 먼저 맞는 것 하나만.
 *   종료일이 있고 종료일 < 오늘 → 종료   (종료일 당일은 아직 복용 중이다 — 마지막 날)
 *   일시중지 시작일이 있음       → 일시중지
 *   그 밖                       → 복용 중 (시작일이 미래인 약도)
 */
fun medicationStatus(med: MedicationDetail, today: LocalDate): MedicationStatus = when {
    med.endDate != null && med.endDate.isBefore(today) -> MedicationStatus.ENDED
    med.pausedFrom != null -> MedicationStatus.PAUSED
    else -> MedicationStatus.ACTIVE
}

/**
 * 목록(S06)의 순서: 끝나지 않은 약이 먼저, 종료된 약은 뒤로. 각 묶음 안은 등록한 순서(id가 작은 것부터).
 * 일시중지 약은 자리를 옮기지 않는다.
 */
fun sortForList(meds: List<MedicationDetail>, today: LocalDate): List<MedicationDetail> =
    meds.sortedWith(
        compareBy<MedicationDetail> { medicationStatus(it, today) == MedicationStatus.ENDED }.thenBy { it.id }
    )

/** 이름에 검색어가 들어 있는 약만 남긴다. 대소문자와 검색어 앞뒤 공백은 무시한다. 검색어가 비면 전부. */
fun filterByName(meds: List<MedicationDetail>, query: String): List<MedicationDetail> {
    val needle = query.trim().lowercase()
    return if (needle.isEmpty()) meds else meds.filter { it.name.lowercase().contains(needle) }
}
