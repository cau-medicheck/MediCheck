package com.cau.medicheck.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.cau.medicheck.R
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
