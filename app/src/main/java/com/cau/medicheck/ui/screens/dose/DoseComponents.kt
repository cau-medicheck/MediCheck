package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.ui.screens.medication.formatTime
import java.time.Instant
import java.time.ZoneId

/*
 * 오늘 화면과 기록 화면이 함께 쓰는 부품들: 복용 기록 카드와 그 목록.
 */

/** 복용 기록 카드의 버튼을 눌렀을 때 부를 함수 묶음. 값으로 기록의 id가 넘어간다. */
data class DoseActions(
    val onTake: (recordId: Long) -> Unit = {},
    val onSkip: (recordId: Long) -> Unit = {},
    val onUndo: (recordId: Long) -> Unit = {}
)

/**
 * 하루치 복용 기록 목록.
 * @param header 목록 맨 위에 붙일 내용 (오늘 화면의 요약 문구 등). 없으면 비워 둔다.
 */
@Composable
fun DoseList(
    doses: List<DoseUi>,
    actions: DoseActions,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        if (header != null) item { header() }
        items(doses, key = { it.item.id }) { dose -> DoseCard(dose, actions) }
    }
}

/**
 * 복용 기록 하나를 보여 주는 카드.
 *
 *   예정·깜빡함 → [복용 완료] [건너뛰기] 버튼
 *   복용 완료·건너뜀 → 상태 문구 + [되돌리기] (버튼 줄 없이 한 줄로)
 */
@Composable
fun DoseCard(dose: DoseUi, actions: DoseActions) {
    val item = dose.item
    val isAnswered =
        dose.displayStatus == DoseDisplayStatus.TAKEN || dose.displayStatus == DoseDisplayStatus.SKIPPED

    Card(
        // 이미 응답한 기록은 색을 옅게 해서, 아직 남은 기록이 눈에 먼저 들어오게 한다.
        colors = if (isAnswered) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        } else {
            CardDefaults.cardColors()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = formatTime(item.time), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.medicationName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.dosage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isAnswered) {
                    // 이미 응답한 기록: 상태 문구 아래에 "되돌리기"를 작게 붙여 카드를 한 줄로 끝낸다.
                    Column(horizontalAlignment = Alignment.End) {
                        StatusLabel(dose)
                        TextButton(
                            onClick = { actions.onUndo(item.id) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(stringResource(R.string.action_undo))
                        }
                    }
                } else {
                    StatusLabel(dose)
                }
            }

            // 아직 응답하지 않은 기록에만 버튼 줄을 보여 준다.
            if (!isAnswered) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { actions.onTake(item.id) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_take))
                    }
                    OutlinedButton(onClick = { actions.onSkip(item.id) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_skip))
                    }
                }
            }
        }
    }
}

/** 카드 오른쪽 위의 상태 문구. 상태마다 글자와 색이 다르다. */
@Composable
private fun StatusLabel(dose: DoseUi) {
    val (text, color) = when (dose.displayStatus) {
        DoseDisplayStatus.UPCOMING ->
            stringResource(R.string.status_upcoming) to MaterialTheme.colorScheme.onSurfaceVariant

        DoseDisplayStatus.MISSED ->
            stringResource(R.string.status_missed) to MaterialTheme.colorScheme.error

        DoseDisplayStatus.TAKEN -> {
            // 버튼을 누른 시각을 함께 보여 준다. 예: "08:05 복용"
            val actedAt = dose.item.actedAt
            val label = if (actedAt == null) {
                stringResource(R.string.status_taken)
            } else {
                val time = Instant.ofEpochMilli(actedAt).atZone(ZoneId.systemDefault()).toLocalTime()
                stringResource(R.string.status_taken_at, formatTime(time))
            }
            label to MaterialTheme.colorScheme.primary
        }

        DoseDisplayStatus.SKIPPED ->
            stringResource(R.string.status_skipped) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    StatusText(text, color)
}

@Composable
private fun StatusText(text: String, color: Color) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = color)
}

/** 목록 대신 화면 가운데에 보여 주는 안내 (기록이 없을 때) */
@Composable
fun CenteredMessage(title: String, modifier: Modifier = Modifier, body: String? = null) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            if (body != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CenteredLoading(modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}
