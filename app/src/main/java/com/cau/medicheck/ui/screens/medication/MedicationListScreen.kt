package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.MedicationDetail
import com.cau.medicheck.domain.RepeatDays
import com.cau.medicheck.domain.ScheduleItem
import com.cau.medicheck.ui.theme.MediCheckTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * 약 목록 화면 (아래쪽 탭의 "내 약").
 *
 * 그리기만 한다. 약 목록은 state로 받고, 버튼을 누르면 밖에서 받은 함수를 부른다.
 *   - onAddClick: "약 추가" 버튼
 *   - onMedicationClick: 약 카드를 눌렀을 때 (그 약의 id를 넘긴다)
 *   - onLogout: 위쪽의 로그아웃 버튼
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationListScreen(
    state: MedicationListUiState,
    onAddClick: () -> Unit,
    onMedicationClick: (medicationId: Long) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.medications_title)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.action_logout)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_medication)) }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> CenteredBox(Modifier.padding(innerPadding)) {
                CircularProgressIndicator()
            }

            state.medications.isEmpty() -> CenteredBox(Modifier.padding(innerPadding)) {
                EmptyMessage()
            }

            else -> LazyColumn(
                // LazyColumn: 화면에 보이는 줄만 그리는 세로 목록. 약이 많아도 느려지지 않는다.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // key: 줄마다 약의 id를 이름표로 붙여, 목록이 바뀔 때 어느 줄이 어느 약인지 알게 한다.
                items(state.medications, key = { it.id }) { medication ->
                    MedicationCard(medication, onClick = { onMedicationClick(medication.id) })
                }
            }
        }
    }
}

/** 약 하나를 보여 주는 카드: 이름, 복용량, 복용 시간들, 기간, 메모 */
@Composable
private fun MedicationCard(medication: MedicationDetail, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = medication.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = medication.dosage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            medication.schedules.forEach { schedule ->
                Text(
                    text = stringResource(
                        R.string.schedule_line,
                        formatTime(schedule.time),
                        daysSummary(schedule.days)
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = periodSummary(medication.startDate, medication.endDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (medication.memo.isNotEmpty()) {
                Text(
                    text = medication.memo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 등록된 약이 없을 때 보여 주는 안내 */
@Composable
private fun EmptyMessage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = stringResource(R.string.medications_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.medications_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CenteredBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) { content() }
}

// 아래는 Android Studio 미리 보기용 코드다. 앱 실행에는 쓰이지 않는다.

@Preview(showBackground = true)
@Composable
private fun MedicationListScreenPreview() {
    val sample = listOf(
        MedicationDetail(
            id = 1, name = "Tylenol", dosage = "1 tablet",
            startDate = LocalDate.of(2026, 11, 16), endDate = null, memo = "After meals",
            schedules = listOf(
                ScheduleItem(1, LocalTime.of(8, 0), RepeatDays.EVERY_DAY),
                ScheduleItem(2, LocalTime.of(20, 0), setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
            )
        )
    )
    MediCheckTheme(dynamicColor = false) {
        MedicationListScreen(
            state = MedicationListUiState(isLoading = false, medications = sample),
            onAddClick = {}, onMedicationClick = {}, onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MedicationListEmptyPreview() {
    MediCheckTheme(dynamicColor = false) {
        MedicationListScreen(
            state = MedicationListUiState(isLoading = false),
            onAddClick = {}, onMedicationClick = {}, onLogout = {}
        )
    }
}
