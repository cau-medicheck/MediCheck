package com.cau.medicheck.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/*
 * 내 약 목록(S06)의 약 카드와 약 상세(S07)에 붙는 숫자들. (2026-11-24, 시안: 04_화면캡처/내약탭_시안/켜기끄기_시안.html, 약상세_시안/시안.html)
 * 안드로이드와 상관없는 순수 계산이라 단위 테스트로 확인한다(MedicationInsightsTest).
 */

/**
 * 복용 기간 진행: 종료일이 있는 약의 "30일 중 13일째 · D-17".
 * @param day 오늘이 며칠째인가 (시작일이 1일째)
 * @param total 전체 기간(시작일 ~ 종료일, 양 끝 포함)
 * @param daysLeft 종료일까지 남은 날 (종료일 당일이면 0)
 */
data class CourseProgress(val day: Int, val total: Int, val daysLeft: Int)

/**
 * 약 상세의 최근 7일 요일 동그라미 하나: 그날 이 약을 어떻게 했나. (2026-11-24, 시안: 04_화면캡처/약상세_시안/시안.html B)
 * 하루에 회차가 여럿이면: 하나라도 미복용 → MISSED, 아직 시각 전인 회차가 있으면 → UPCOMING,
 * 하나라도 건너뜀 → SKIPPED, 모두 먹었으면 → TAKEN. 그날 회차가 없으면 NONE.
 */
enum class DayMark { TAKEN, MISSED, SKIPPED, UPCOMING, NONE }

data class RecentDay(val date: LocalDate, val mark: DayMark)

/** 목록 위쪽 상태 칩: 전체 · 복용 중 · 비활성화 · 종료 */
enum class MedicationFilter { ALL, ACTIVE, PAUSED, ENDED }

object MedicationInsights {

    /** 약별 복용률을 보는 날 수 (오늘 포함 최근 7일 — 주간 통계와 같은 기간) */
    const val RECENT_DAYS = 7L

    /**
     * 한 약의 최근 7일 복용률(%). 계산 기준은 통계 화면과 같다(StatsCalculator.count — 도래한 회차만, 미복용 여유 30분).
     * 계산할 회차가 없으면 null (화면은 "기록 없음").
     *
     * @param doses 이 약의 회차들 (다른 날짜가 섞여 있어도 최근 7일만 센다)
     */
    fun recentRate(doses: List<DoseItem>, now: LocalDateTime): Int? = recentCounts(doses, now).completionRate

    /** 최근 7일(오늘 포함)의 횟수: 약 상세의 "7번 중 2번 먹음". 계산 기준은 recentRate와 같다. */
    fun recentCounts(doses: List<DoseItem>, now: LocalDateTime): DoseCounts {
        val today = now.toLocalDate()
        val from = today.minusDays(RECENT_DAYS - 1)
        return StatsCalculator.count(doses.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }, now)
    }

    /** 약 상세의 요일 동그라미 7개: 6일 전부터 오늘까지, 하루에 하나. 규칙은 DayMark 설명 참고 */
    fun recentDays(doses: List<DoseItem>, now: LocalDateTime): List<RecentDay> {
        val today = now.toLocalDate()
        val byDate = doses.groupBy { it.date }
        return (RECENT_DAYS - 1 downTo 0).map { back ->
            val date = today.minusDays(back)
            val shown = byDate[date].orEmpty().map { it.displayStatus(now) }
            val mark = when {
                shown.isEmpty() -> DayMark.NONE
                DoseDisplayStatus.MISSED in shown -> DayMark.MISSED
                DoseDisplayStatus.UPCOMING in shown -> DayMark.UPCOMING
                DoseDisplayStatus.SKIPPED in shown -> DayMark.SKIPPED
                else -> DayMark.TAKEN
            }
            RecentDay(date, mark)
        }
    }

    /**
     * 이 약의 다음 복용 회차. 없으면 null (비활성화, 종료, 앞으로 7일 안에 회차가 없을 때).
     *   - 오늘: 아직 기록 전이고 미복용으로 넘어가지 않은 회차 (예정 시각 30분 뒤까지는 "지금 먹을 약"이다)
     *   - 내일부터 7일: 지금 규칙으로 계산한 예정 회차. 미리 건너뛴 회차는 빼고 그다음을 찾는다.
     *
     * @param records 이 약의 오늘 ~ 7일 뒤 기록 (미리 건너뛴 미래 기록이 여기에 들어 있다)
     */
    fun nextDose(med: MedicationDetail, records: List<DoseItem>, now: LocalDateTime): DoseItem? {
        val today = now.toLocalDate()
        val recorded = records.filter { it.medicationId == med.id }.associateBy { it.key }
        recorded.values
            .filter { it.date == today && it.status == DoseStatus.PENDING && !it.isOverdue(now) }
            .minByOrNull { it.time }
            ?.let { return it }
        for (ahead in 1..RECENT_DAYS) {
            val date = today.plusDays(ahead)
            DosePlanner.plannedDoses(date, listOf(med), today)
                .firstOrNull { (recorded[it.key]?.status ?: DoseStatus.PENDING) == DoseStatus.PENDING }
                ?.let { return it }
        }
        return null
    }

    /**
     * 종료일이 있는 약의 기간 진행. 종료일이 없거나, 오늘이 시작일 전이거나 종료일 뒤면 null(보이지 않음).
     */
    fun courseProgress(startDate: LocalDate, endDate: LocalDate?, today: LocalDate): CourseProgress? {
        if (endDate == null || today.isBefore(startDate) || today.isAfter(endDate)) return null
        return CourseProgress(
            day = ChronoUnit.DAYS.between(startDate, today).toInt() + 1,
            total = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1,
            daysLeft = ChronoUnit.DAYS.between(today, endDate).toInt()
        )
    }

    /** 상태 칩으로 거르기. ALL이면 전부 */
    fun matches(status: MedicationStatus, filter: MedicationFilter): Boolean = when (filter) {
        MedicationFilter.ALL -> true
        MedicationFilter.ACTIVE -> status == MedicationStatus.ACTIVE
        MedicationFilter.PAUSED -> status == MedicationStatus.PAUSED
        MedicationFilter.ENDED -> status == MedicationStatus.ENDED
    }
}
