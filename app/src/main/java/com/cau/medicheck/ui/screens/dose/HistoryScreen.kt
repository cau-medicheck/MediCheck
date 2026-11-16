package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.DatePickerPopup
import com.cau.medicheck.ui.screens.medication.currentLocale
import com.cau.medicheck.ui.screens.medication.formatDate
import java.time.LocalDate

/**
 * 기록 화면: 날짜를 골라 그날의 복용 기록을 본다.
 *
 * 지난 날의 기록도 여기서 고칠 수 있다 (예: 어제 먹었는데 누르는 것을 잊은 경우).
 * 날짜는 ← → 버튼으로 하루씩 옮기거나, 가운데 날짜를 눌러 달력에서 고른다. 오늘보다 뒤로는 갈 수 없다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: DayDosesUiState,
    actions: DoseActions,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    onLogout: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.action_logout)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ----- 날짜를 옮기는 줄:  ←  2026. 11. 16.  → -----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onPreviousDay) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_previous_day)
                    )
                }
                TextButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDate(state.date, currentLocale()),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                // 오늘을 보고 있으면 "다음 날" 버튼을 잠근다.
                IconButton(onClick = onNextDay, enabled = !state.isToday) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.action_next_day)
                    )
                }
            }

            // ----- 그날의 기록 -----
            when {
                state.isLoading -> CenteredLoading()

                state.doses.isEmpty() ->
                    CenteredMessage(title = stringResource(R.string.history_empty))

                else -> DoseList(
                    doses = state.doses,
                    actions = actions,
                    header = {
                        Text(
                            text = stringResource(R.string.today_summary, state.takenCount, state.totalCount),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerPopup(
            initialDate = state.date,
            maxDate = LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onDatePicked = { date ->
                onDatePicked(date)
                showDatePicker = false
            }
        )
    }
}
