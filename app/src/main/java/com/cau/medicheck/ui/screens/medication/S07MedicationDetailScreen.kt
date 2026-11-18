package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.MedicationDetail
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.LabelValueRow
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.OptionRow
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.ToggleRow
import com.cau.medicheck.ui.components.scheduleDaysText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.Navy

/** 약 상세 화면의 버튼들이 부를 함수 묶음 */
class MedicationDetailActions(
    val onBackToList: () -> Unit,
    val onPausedChange: (Boolean) -> Unit,
    val onEdit: () -> Unit,
    val onDelete: () -> Unit,
    val onOpenSource: () -> Unit,
    val onOpenArchive: () -> Unit
)

/**
 * S07 약 상세 (v3 medDetail()).
 *
 * 약의 정보 표, 일시중지 스위치, 수정·삭제 버튼, 처방 출처 메모·과거 기록 보존으로 가는 줄.
 * 일시중지를 켜면 오늘 아직 시각 전인 회차와 내일 이후 회차가 사라지고, 끄면 오늘 회차부터 다시 생긴다.
 */
@Composable
fun S07MedicationDetailScreen(state: MedicationDetailUiState, actions: MedicationDetailActions) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s07)) {
        val med = state.med
        when {
            state.isLoading -> Unit
            med == null -> MedicationNotFound(actions.onBackToList)
            else -> MedicationDetailBody(med, state, actions)
        }
    }
}

/** 약을 찾지 못했을 때의 안내. 약 수정 화면(S08)도 수정할 약이 없으면 이것을 쓴다. */
@Composable
fun MedicationNotFound(onBackToList: () -> Unit) {
    EmptyState(
        emoji = Glyph.NOT_FOUND,
        title = stringResource(R.string.error_med_not_found),
        body = stringResource(R.string.s07_empty_body),
        actionLabel = stringResource(R.string.s07_empty_action),
        onAction = onBackToList
    )
}

@Composable
private fun MedicationDetailBody(med: MedicationDetail, state: MedicationDetailUiState, actions: MedicationDetailActions) {
    BackLink(stringResource(R.string.s07_back), actions.onBackToList)

    // ----- 큰 아이콘 + 이름 + 상태 배지 -----
    Row(modifier = Modifier.padding(top = 16.dp)) {
        Avatar(Glyph.DOSE, size = 58.dp, fontSize = 30)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = med.name,
                fontSize = 23.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.9).sp,
                color = Navy,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { heading() }
            )
            MedicationStatusPill(state.status, showActive = true)
        }
    }

    // ----- 정보 표 -----
    val source = listOf(med.hospital, med.department).filter { it.isNotBlank() }.joinToString(" · ")
    SurfaceCard(Modifier.padding(top = 22.dp), padding = 0.dp) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            LabelValueRow(stringResource(R.string.s07_label_dosage), med.dosage)
            LabelValueRow(stringResource(R.string.s07_label_times), med.schedules.joinToString(" · ") { timeText(it.time) })
            LabelValueRow(stringResource(R.string.s07_label_days), scheduleDaysText(med.schedules))
            LabelValueRow(stringResource(R.string.s07_label_start), med.startDate.toString())
            LabelValueRow(
                stringResource(R.string.s07_label_end),
                med.endDate?.toString() ?: stringResource(R.string.common_not_set)
            )
            LabelValueRow(
                stringResource(R.string.s07_label_memo),
                med.memo.ifBlank { stringResource(R.string.s07_no_memo) },
                showDivider = source.isNotEmpty()
            )
            // 처방 출처(의료기관 · 진료과)는 S42에서 적어 둔 값이 있을 때만 보인다.
            if (source.isNotEmpty()) {
                LabelValueRow(stringResource(R.string.s07_label_source), source, showDivider = false)
            }
        }
    }

    // ----- 일시중지 스위치 (켜짐 = 일시중지 중) -----
    SurfaceCard(Modifier.padding(top = 16.dp)) {
        ToggleRow(
            title = stringResource(R.string.s07_pause_title),
            description = stringResource(R.string.s07_pause_body),
            checked = med.pausedFrom != null,
            onCheckedChange = actions.onPausedChange,
            enabled = !state.isBusy
        )
    }

    Row(modifier = Modifier.padding(top = 16.dp)) {
        McButton(stringResource(R.string.s07_btn_edit), actions.onEdit, Modifier.weight(1f), full = true)
        Spacer(Modifier.width(10.dp))
        McButton(stringResource(R.string.s07_btn_delete), actions.onDelete, Modifier.weight(1f), ButtonKind.RED, full = true)
    }

    SurfaceCard(Modifier.padding(top = 16.dp), padding = 0.dp) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            OptionRow(
                title = stringResource(R.string.s07_row_source_title),
                description = stringResource(R.string.s07_row_source_desc),
                onClick = actions.onOpenSource
            )
            OptionRow(
                title = stringResource(R.string.s07_row_archive_title),
                description = stringResource(R.string.s07_row_archive_desc),
                onClick = actions.onOpenArchive,
                showDivider = false
            )
        }
    }

    Notice(stringResource(R.string.s07_notice), Modifier.padding(top = 16.dp), warn = true)
}
