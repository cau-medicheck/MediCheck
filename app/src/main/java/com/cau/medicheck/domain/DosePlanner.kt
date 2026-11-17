package com.cau.medicheck.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * "어느 날짜에 회차가 있는가", "어느 날짜들의 복용 기록을 새로 만들어야 하는가"를 계산한다. (docs/DB_DESIGN.md ⑨)
 *
 * 순수 Kotlin 코드라서 DB 없이 단위 테스트를 할 수 있다.
 */
object DosePlanner {

    /**
     * 날짜 [date]에 이 규칙의 회차가 있는가 (DB_DESIGN 9.1의 식을 한 곳에 둔 것).
     *
     * 기록 보충(datesToCreate)과 알림 계산(ReminderPlanner)이 **같은 이 함수**를 쓴다.
     * 그래야 "내일 일정에 보이는 회차 = 내일 실제로 만들어질 기록 = 내일 울릴 알림"이 된다.
     *
     * 아래가 모두 참이면 회차가 있다.
     *   - date ≥ 약의 시작일
     *   - date ≥ 규칙의 적용 시작일 (effectiveFrom이 있을 때)
     *   - date ≤ 약의 종료일 (endDate가 있을 때)
     *   - 일시중지가 없거나, date < min(일시중지 시작일, 오늘)
     *   - date의 요일이 반복 요일에 들어 있음
     *
     * 일시중지를 min(pausedFrom, today)와 비교하는 이유: pausedFrom이 있으면 "지금 중지 중"이라는 뜻이다.
     * 시간대가 바뀌어 pausedFrom이 오늘보다 뒤처럼 보여도, 오늘과 그 뒤에는 회차가 없어야 한다.
     * (약 삭제 여부는 여기서 보지 않는다. 삭제된 약·규칙은 부르는 쪽이 아예 넘기지 않는다.)
     *
     * @param today "오늘". 일시중지 판정에만 쓴다.
     */
    fun occursOn(
        date: LocalDate,
        startDate: LocalDate,
        endDate: LocalDate?,
        repeatDays: Set<DayOfWeek>,
        effectiveFrom: LocalDate?,
        pausedFrom: LocalDate?,
        today: LocalDate
    ): Boolean {
        if (date.isBefore(startDate)) return false
        if (effectiveFrom != null && date.isBefore(effectiveFrom)) return false
        if (endDate != null && date.isAfter(endDate)) return false
        if (pausedFrom != null && !date.isBefore(minOf(pausedFrom, today))) return false
        return date.dayOfWeek in repeatDays
    }

    /**
     * 한 규칙에 대해, 기록을 만들어야 할 날짜 목록을 돌려준다.
     *
     * 범위: max(시작일, 적용 시작일, "마지막으로 만든 날 + 1일") ~ 오늘
     * 그 범위 안에서 occursOn이 참인 날만 고른다 (종료일·일시중지·요일은 occursOn이 거른다).
     *
     * - 미래 날짜는 만들지 않는다 (오늘까지만).
     * - 앱을 며칠 안 열었다면 그 사이의 날짜도 채운다. 그래야 "기한 경과 · 미기록"이 통계에 잡힌다.
     * - 적용 시작일(effectiveFrom)이 있으면 그 전 날짜는 만들지 않는다. 새로 더한 시간 줄이
     *   지난 날짜까지 거슬러 채워지지 않게 하기 위해서다.
     *
     * @param lastCreatedDate 이 규칙에서 마지막으로 기록을 만든 날짜. 처음이면 null
     */
    fun datesToCreate(
        startDate: LocalDate,
        endDate: LocalDate?,
        repeatDays: Set<DayOfWeek>,
        lastCreatedDate: LocalDate?,
        today: LocalDate,
        effectiveFrom: LocalDate? = null,
        pausedFrom: LocalDate? = null
    ): List<LocalDate> {
        var from = startDate
        if (effectiveFrom != null) from = maxOf(from, effectiveFrom)
        if (lastCreatedDate != null) from = maxOf(from, lastCreatedDate.plusDays(1))

        val dates = mutableListOf<LocalDate>()
        var date = from
        while (!date.isAfter(today)) {
            if (occursOn(date, startDate, endDate, repeatDays, effectiveFrom, pausedFrom, today)) {
                dates += date
            }
            date = date.plusDays(1)
        }
        return dates
    }
}
