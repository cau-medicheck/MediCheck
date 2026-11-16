package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.RepeatDays
import com.cau.medicheck.ui.components.DatePickerPopup
import com.cau.medicheck.ui.theme.MediCheckTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * 약 등록·수정 화면에서 일어나는 모든 동작의 묶음.
 * 함수가 많아서 하나씩 따로 넘기지 않고 이 묶음 하나로 넘긴다.
 * 실제 내용은 MainNavHost에서 MedicationEditViewModel의 함수들로 채운다.
 */
data class MedicationEditActions(
    val onNameChange: (String) -> Unit = {},
    val onDosageChange: (String) -> Unit = {},
    val onMemoChange: (String) -> Unit = {},
    val onStartDateChange: (LocalDate) -> Unit = {},
    val onEndDateChange: (LocalDate?) -> Unit = {},
    val onAddSchedule: () -> Unit = {},
    val onRemoveSchedule: (key: Int) -> Unit = {},
    val onScheduleTimeChange: (key: Int, time: LocalTime) -> Unit = { _, _ -> },
    val onScheduleDayToggle: (key: Int, day: DayOfWeek) -> Unit = { _, _ -> },
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onBack: () -> Unit = {}
)

/** 날짜를 고르는 창이 "시작일"용인지 "종료일"용인지 */
private enum class DateTarget { START, END }

/**
 * 약 등록·수정 화면. 등록과 수정이 같은 화면을 쓰고, state.isNew로 제목과 삭제 버튼만 달라진다.
 *
 * 입력칸의 값은 전부 state(ViewModel이 들고 있음)에서 오고,
 * 이 화면이 직접 기억하는 것은 "어떤 창(날짜·시간·삭제 확인)이 열려 있는지"뿐이다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationEditScreen(state: MedicationEditUiState, actions: MedicationEditActions) {
    // 지금 열려 있는 창. null이면 닫혀 있다.
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }
    var timePickerKey by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.medication_new_title else R.string.medication_edit_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (!state.isNew && !state.isLoading) {
                        IconButton(onClick = { showDeleteConfirm = true }, enabled = !state.isSaving) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ----- 약 정보 -----
            OutlinedTextField(
                value = state.name,
                onValueChange = actions.onNameChange,
                label = { Text(stringResource(R.string.label_medication_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.dosage,
                onValueChange = actions.onDosageChange,
                label = { Text(stringResource(R.string.label_dosage)) },
                supportingText = { Text(stringResource(R.string.hint_dosage)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.memo,
                onValueChange = actions.onMemoChange,
                label = { Text(stringResource(R.string.label_memo)) },
                supportingText = { Text(stringResource(R.string.hint_memo)) },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            // ----- 복용 기간 -----
            SectionTitle(R.string.section_period)
            val locale = currentLocale()
            DateRow(
                label = stringResource(R.string.label_start_date),
                value = formatDate(state.startDate, locale),
                onClick = { dateTarget = DateTarget.START }
            )
            Spacer(Modifier.height(8.dp))
            DateRow(
                label = stringResource(R.string.label_end_date),
                value = state.endDate?.let { formatDate(it, locale) }
                    ?: stringResource(R.string.no_end_date),
                onClick = { dateTarget = DateTarget.END },
                // 종료일이 정해져 있을 때만 "지우기" 버튼을 보여 준다.
                onClear = if (state.endDate != null) ({ actions.onEndDateChange(null) }) else null
            )

            // ----- 복용 시간 -----
            SectionTitle(R.string.section_times)
            state.schedules.forEach { row ->
                ScheduleRowCard(
                    row = row,
                    canRemove = state.schedules.size > 1,   // 마지막 한 줄은 지울 수 없다
                    onTimeClick = { timePickerKey = row.key },
                    onDayToggle = { day -> actions.onScheduleDayToggle(row.key, day) },
                    onRemove = { actions.onRemoveSchedule(row.key) }
                )
                Spacer(Modifier.height(8.dp))
            }
            TextButton(onClick = actions.onAddSchedule) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_add_time))
            }

            // ----- 오류 문구와 저장 버튼 -----
            Spacer(Modifier.height(8.dp))
            state.error?.let { error ->
                Text(
                    text = stringResource(error.messageRes()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = actions.onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ----- 아래는 버튼을 눌렀을 때 화면 위에 뜨는 창들 -----

    dateTarget?.let { target ->
        DatePickerPopup(
            initialDate = if (target == DateTarget.START) state.startDate else state.endDate ?: state.startDate,
            onDismiss = { dateTarget = null },
            onDatePicked = { date ->
                if (target == DateTarget.START) actions.onStartDateChange(date) else actions.onEndDateChange(date)
                dateTarget = null
            }
        )
    }

    timePickerKey?.let { key ->
        val row = state.schedules.find { it.key == key }
        if (row != null) {
            TimePickerPopup(
                initialTime = row.time,
                onDismiss = { timePickerKey = null },
                onTimePicked = { time ->
                    actions.onScheduleTimeChange(key, time)
                    timePickerKey = null
                }
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_medication_title)) },
            text = { Text(stringResource(R.string.delete_medication_body, state.name)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; actions.onDelete() }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

/** "시작일  2026. 11. 16." 처럼 이름과 값을 보여 주고, 누르면 날짜를 고르는 창이 열리는 줄 */
@Composable
private fun DateRow(label: String, value: String, onClick: () -> Unit, onClear: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(value)
        }
        if (onClear != null) {
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear_end_date))
            }
        }
    }
}

/** 복용 시간 한 줄: 시각 버튼 + 지우기 버튼 + 요일 버튼 7개 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleRowCard(
    row: ScheduleRow,
    canRemove: Boolean,
    onTimeClick: () -> Unit,
    onDayToggle: (DayOfWeek) -> Unit,
    onRemove: () -> Unit
) {
    val locale = currentLocale()
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onTimeClick) {
                    Text(formatTime(row.time), style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = daysSummary(row.days),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_remove_time))
                    }
                }
            }
            // FlowRow: 한 줄에 다 안 들어가면 다음 줄로 넘겨 주는 가로 배치
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RepeatDays.ALL.forEach { day ->
                    FilterChip(
                        selected = day in row.days,
                        onClick = { onDayToggle(day) },
                        label = { Text(shortDayName(day, locale)) }
                    )
                }
            }
        }
    }
}

/** 시각을 고르는 창 (24시간제) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerPopup(
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

// 아래는 Android Studio 미리 보기용 코드다. 앱 실행에는 쓰이지 않는다.

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun MedicationEditScreenPreview() {
    MediCheckTheme(dynamicColor = false) {
        MedicationEditScreen(
            state = MedicationEditUiState(
                isNew = false,
                name = "Tylenol",
                dosage = "1 tablet",
                memo = "After meals",
                startDate = LocalDate.of(2026, 11, 16),
                schedules = listOf(
                    ScheduleRow(0, 1, LocalTime.of(8, 0), RepeatDays.EVERY_DAY),
                    ScheduleRow(1, 2, LocalTime.of(20, 0), setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                )
            ),
            actions = MedicationEditActions()
        )
    }
}
