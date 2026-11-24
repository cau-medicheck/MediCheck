package com.cau.medicheck.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/*
 * 내 약 목록(S06)의 약 카드에 붙는 숫자들. (2026-11-24, 시안: 04_화면캡처/내약탭_시안/켜기끄기_시안.html)
 * 안드로이드와 상관없는 순수 계산이라 단위 테스트로 확인한다(MedicationInsightsTest).
 */

/**
 * 복용 기간 진행: 종료일이 있는 약의 "30일 중 13일째 · D-17".
 * @param day 오늘이 며칠째인가 (시작일이 1일째)
 * @param total 전체 기간(시작일 ~ 종료일, 양 끝 포함)
 * @param daysLeft 종료일까지 남은 날 (종료일 당일이면 0)
 */
data class CourseProgress(val day: Int, val total: Int, val daysLeft: Int)

/** 목록 위쪽 상태 칩: 전체 · 복용 중 · 쉬는 중 · 종료 */
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
    fun recentRate(doses: List<DoseItem>, now: LocalDateTime): Int? {
        val today = now.toLocalDate()
        val from = today.minusDays(RECENT_DAYS - 1)
        val recent = doses.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
        return StatsCalculator.count(recent, now).completionRate
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
