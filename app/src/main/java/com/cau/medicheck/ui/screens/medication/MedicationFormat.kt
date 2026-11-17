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
 * 예전 통계 화면(임시 S13)이 쓰는 날짜·요일 글자 함수와, 약 저장 오류의 문구 고르기.
 * 새 화면의 날짜·요일 글자는 ui/components/McFormat.kt에 있다. (4단계에서 통계를 새로 만들면 위쪽 함수는 지운다)
 */

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

/** 오류 종류(MedicationError)에 맞는 토스트 문구를 strings.xml에서 고른다. (문구는 프로토타입 그대로) */
@StringRes
fun MedicationError.messageRes(): Int = when (this) {
    MedicationError.NAME_DOSAGE_TIME_INVALID -> R.string.s08_toast_check_basic
    MedicationError.NO_DAYS -> R.string.s08_toast_check_days
    MedicationError.DATES_INVALID -> R.string.s08_toast_check_dates
    MedicationError.DUPLICATE_TIME -> R.string.s08_toast_duplicate_time
    MedicationError.NOT_FOUND -> R.string.error_med_not_found
}
