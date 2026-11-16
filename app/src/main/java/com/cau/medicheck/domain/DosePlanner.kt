package com.cau.medicheck.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * "어느 날짜들의 복용 기록을 새로 만들어야 하는지"를 계산한다. (docs/DB_DESIGN.md ⑨)
 *
 * 순수 Kotlin 코드라서 DB 없이 단위 테스트를 할 수 있다.
 */
object DosePlanner {

    /**
     * 한 규칙에 대해, 기록을 만들어야 할 날짜 목록을 돌려준다.
     *
     * 범위: (시작일과 "마지막으로 만든 날 + 1일" 중 늦은 날) ~ (오늘과 종료일 중 이른 날)
     * 그 범위 안에서 요일이 반복 요일에 들어 있는 날만 고른다.
     *
     * - 미래 날짜는 만들지 않는다 (오늘까지만).
     * - 앱을 며칠 안 열었다면 그 사이의 날짜도 채운다. 그래야 "깜빡함"이 통계에 잡힌다.
     *
     * @param lastCreatedDate 이 규칙에서 마지막으로 기록을 만든 날짜. 처음이면 null
     */
    fun datesToCreate(
        startDate: LocalDate,
        endDate: LocalDate?,
        repeatDays: Set<DayOfWeek>,
        lastCreatedDate: LocalDate?,
        today: LocalDate
    ): List<LocalDate> {
        val from = if (lastCreatedDate == null) startDate else maxOf(startDate, lastCreatedDate.plusDays(1))
        val to = if (endDate == null) today else minOf(today, endDate)

        val dates = mutableListOf<LocalDate>()
        var date = from
        while (!date.isAfter(to)) {
            if (date.dayOfWeek in repeatDays) dates += date
            date = date.plusDays(1)
        }
        return dates
    }
}
