package com.cau.medicheck.ui.screens.medication

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.cau.medicheck.R
import com.cau.medicheck.domain.MedicationError
import com.cau.medicheck.domain.RepeatDays
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/*
 * 날짜·시간·요일을 화면에 보여 줄 글자로 바꾸는 함수들.
 * 목록 화면과 수정 화면이 함께 쓴다.
 */

private val WEEKDAYS = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
)
private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 지금 앱이 쓰는 언어. 폰(또는 앱) 언어를 바꾸면 이 값도 바뀐다. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** 08:00 같은 24시간제 글자 */
fun formatTime(time: LocalTime): String = time.format(timeFormatter)

/** 언어에 맞는 날짜 글자. 영어: "Nov 16, 2026", 한국어: "2026. 11. 16." */
fun formatDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

/** 요일 하나의 짧은 이름. 영어: "Mon", 한국어: "월" */
fun shortDayName(day: DayOfWeek, locale: Locale): String =
    day.getDisplayName(TextStyle.SHORT, locale)

/** 요일 묶음을 한 줄로. "매일", "평일", "주말", 그 밖에는 "월, 수, 금" */
@Composable
fun daysSummary(days: Set<DayOfWeek>): String {
    val locale = currentLocale()
    return when (days) {
        RepeatDays.EVERY_DAY -> stringResource(R.string.days_every_day)
        WEEKDAYS -> stringResource(R.string.days_weekdays)
        WEEKEND -> stringResource(R.string.days_weekends)
        else -> RepeatDays.ALL.filter { it in days }.joinToString(", ") { shortDayName(it, locale) }
    }
}

/** 복용 기간을 한 줄로. 종료일이 없으면 "11월 16일부터", 있으면 "시작일 ~ 종료일" */
@Composable
fun periodSummary(startDate: LocalDate, endDate: LocalDate?): String {
    val locale = currentLocale()
    return if (endDate == null) {
        stringResource(R.string.period_from, formatDate(startDate, locale))
    } else {
        stringResource(R.string.period_range, formatDate(startDate, locale), formatDate(endDate, locale))
    }
}

/** 오류 종류(MedicationError)에 맞는 문구를 strings.xml에서 고른다. */
@StringRes
fun MedicationError.messageRes(): Int = when (this) {
    MedicationError.NAME_INVALID -> R.string.error_med_name_invalid
    MedicationError.DOSAGE_INVALID -> R.string.error_med_dosage_invalid
    MedicationError.MEMO_TOO_LONG -> R.string.error_med_memo_too_long
    MedicationError.END_BEFORE_START -> R.string.error_med_end_before_start
    MedicationError.NO_SCHEDULE -> R.string.error_med_no_schedule
    MedicationError.SCHEDULE_NO_DAYS -> R.string.error_med_schedule_no_days
    MedicationError.SCHEDULE_DUPLICATE -> R.string.error_med_schedule_duplicate
    MedicationError.NOT_FOUND -> R.string.error_med_not_found
}
