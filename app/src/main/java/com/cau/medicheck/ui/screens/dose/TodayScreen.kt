package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.ui.screens.medication.currentLocale
import com.cau.medicheck.ui.screens.medication.formatDate
import com.cau.medicheck.ui.theme.MediCheckTheme
import java.time.LocalDate
import java.time.LocalTime

/**
 * 오늘 화면: 오늘 먹어야 할 약을 시각 순으로 보여 주고, 복용 완료·건너뛰기를 기록한다.
 * 로그인하면 가장 먼저 보이는 화면이다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    displayName: String,
    state: DayDosesUiState,
    actions: DoseActions,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.today_title))
                        Text(
                            text = formatDate(state.date, currentLocale()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
        val contentModifier = Modifier.padding(innerPadding)
        when {
            state.isLoading -> CenteredLoading(contentModifier)

            state.doses.isEmpty() -> CenteredMessage(
                title = stringResource(R.string.today_empty_title),
                body = stringResource(R.string.today_empty_body),
                modifier = contentModifier
            )

            else -> DoseList(
                doses = state.doses,
                actions = actions,
                modifier = contentModifier,
                header = {
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = stringResource(R.string.home_greeting, displayName),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.today_summary, state.takenCount, state.totalCount),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            )
        }
    }
}

// 아래는 Android Studio 미리 보기용 코드다. 앱 실행에는 쓰이지 않는다.

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    val today = LocalDate.of(2026, 11, 16)
    fun dose(id: Long, hour: Int, name: String, status: DoseStatus, display: DoseDisplayStatus) = DoseUi(
        item = DoseItem(id, id, name, "1 tablet", today, LocalTime.of(hour, 0), status, null),
        displayStatus = display
    )
    MediCheckTheme(dynamicColor = false) {
        TodayScreen(
            displayName = "Suhwan",
            state = DayDosesUiState(
                date = today,
                isLoading = false,
                doses = listOf(
                    dose(1, 8, "Tylenol", DoseStatus.TAKEN, DoseDisplayStatus.TAKEN),
                    dose(2, 13, "Vitamin C", DoseStatus.PENDING, DoseDisplayStatus.MISSED),
                    dose(3, 20, "Tylenol", DoseStatus.PENDING, DoseDisplayStatus.UPCOMING)
                )
            ),
            actions = DoseActions(),
            onLogout = {}
        )
    }
}
