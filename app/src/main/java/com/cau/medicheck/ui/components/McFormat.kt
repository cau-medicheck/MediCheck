package com.cau.medicheck.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.cau.medicheck.R
import com.cau.medicheck.domain.ScheduleItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * 날짜·시각을 화면 글자로 바꾸는 함수 모음. (구현 명세 00 §5.2)
 * 프로토타입의 표기와 같게 맞춘다.
 */

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "08:00" (24시간제) */
fun timeText(time: LocalTime): String = time.format(TIME_FORMAT)

/** "일"·"월"·…·"토" 중 그 날짜의 요일 한 글자 */
@Composable
fun weekdayText(date: LocalDate): String {
    // common_weekdays는 일요일부터 시작한다. dayOfWeek.value는 월=1 … 일=7이라 7로 나눈 나머지를 쓴다.
    val names = stringArrayResource(R.array.common_weekdays)
    return names[date.dayOfWeek.value % 7]
}

/** "11월 17일 화요일" (v3 formatDate) */
@Composable
fun fullDateText(date: LocalDate): String =
    stringResource(R.string.common_date_full, date.monthValue, date.dayOfMonth, weekdayText(date))

/** "2026. 11. 17." (날짜 고르는 칸의 한국어 표기) */
fun dottedDateText(date: LocalDate): String = "${date.year}. ${date.monthValue}. ${date.dayOfMonth}."

// ---------------------------------------------------------------------------------------------
// 반복 요일 표기 (v3: 7개 모두면 "매일", 아니면 일요일부터 "월·수·금")
// ---------------------------------------------------------------------------------------------

/** 일요일부터 시작하는 요일 순서 (프로토타입의 요일 버튼 순서) */
val SUNDAY_FIRST: List<DayOfWeek> = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
)

/**
 * 요일 묶음을 글자로. 순수 함수라 단위 테스트를 한다(글자는 밖에서 넘겨받는다).
 * @param dayLabels 일요일부터 7개 ("일"…"토")
 */
fun daysText(days: Set<DayOfWeek>, everyDayLabel: String, dayLabels: List<String>): String =
    if (days.size == 7) everyDayLabel
    else SUNDAY_FIRST.filter { it in days }.joinToString("·") { dayLabels[SUNDAY_FIRST.indexOf(it)] }

/**
 * 약의 반복 요일을 글자로.
 *   모든 시간의 요일이 같으면  → "매일" / "월·수·금"
 *   시간마다 요일이 다르면     → "08:00 매일, 20:00 월·수·금"
 */
fun scheduleDaysText(schedules: List<ScheduleItem>, everyDayLabel: String, dayLabels: List<String>): String {
    val distinct = schedules.map { it.days }.distinct()
    return if (distinct.size <= 1) {
        daysText(distinct.firstOrNull().orEmpty(), everyDayLabel, dayLabels)
    } else {
        schedules.joinToString(", ") { timeText(it.time) + " " + daysText(it.days, everyDayLabel, dayLabels) }
    }
}

/** 화면에서 바로 쓰는 판: 글자를 strings.xml에서 꺼내 위 함수에 넘긴다. */
@Composable
fun scheduleDaysText(schedules: List<ScheduleItem>): String = scheduleDaysText(
    schedules,
    stringResource(R.string.common_every_day),
    stringArrayResource(R.array.common_weekdays).toList()
)
