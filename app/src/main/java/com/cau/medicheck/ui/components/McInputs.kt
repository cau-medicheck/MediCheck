package com.cau.medicheck.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cau.medicheck.R
import java.time.LocalTime

/**
 * 시각을 고르는 창 (24시간제). 약 등록 화면과 실제 복용 시각 기록(S31)이 함께 쓴다.
 *
 * @param initialTime 창이 열릴 때 맞춰져 있을 시각
 * @param onTimePicked "확인"을 눌렀을 때 고른 시각을 넘겨받는 함수
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerPopup(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onTimePicked: (LocalTime) -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onTimePicked(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        text = { TimePicker(state = pickerState) }
    )
}
