package com.cau.medicheck.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cau.medicheck.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 날짜를 고르는 창. 약 등록·수정 화면과 기록 화면이 함께 쓴다.
 *
 * @param maxDate 고를 수 있는 마지막 날짜. null이면 제한 없음 (기록 화면에서는 오늘까지만 고르게 한다)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerPopup(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    maxDate: LocalDate? = null
) {
    // 날짜 고르는 부품은 날짜를 "1970년부터 흐른 밀리초(UTC 기준)"로 주고받는다.
    // 그래서 LocalDate와 밀리초 사이를 바꿔 준다.
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                maxDate == null || utcTimeMillis <= maxDate.toUtcMillis()

            override fun isSelectableYear(year: Int): Boolean =
                maxDate == null || year <= maxDate.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { onDatePicked(it.toLocalDate()) } ?: onDismiss()
                }
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
