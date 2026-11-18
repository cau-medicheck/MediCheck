package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.MedicationStatus
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.ListRow
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.RowSub
import com.cau.medicheck.ui.components.RowTitle
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.scheduleDaysText
import com.cau.medicheck.ui.components.timeText

/**
 * S06 내 약 목록 (v3 medsList()).
 *
 * 검색칸에 글자를 치면 이름에 그 글자가 든 약만 남는다. 종료된 약은 "종료" 배지를 달고 목록 아래쪽에,
 * 일시중지한 약은 "일시중지" 배지를 달고 제자리에 있다.
 */
@Composable
fun S06MedicationListScreen(
    state: MedicationListUiState,
    onQueryChange: (String) -> Unit,
    onAdd: () -> Unit,
    onOpen: (medId: Long) -> Unit
) {
    val focusManager = LocalFocusManager.current

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s06)) {
        ScreenIntro(stringResource(R.string.s06_subtitle))

        // [약 이름으로 검색........] [＋ 추가]
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
            FormField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.s06_search_hint),
                label = stringResource(R.string.s06_search_hint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                // 키보드의 검색 키를 누르면 키보드를 닫는다 (목록은 글자를 칠 때마다 이미 걸러져 있다).
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            McButton(stringResource(R.string.s06_btn_add), onAdd, small = true)
        }

        SurfaceCard(Modifier.padding(top = 16.dp), padding = 0.dp) {
            if (state.rows.isEmpty() && !state.isLoading) {
                // 약이 하나도 없을 때와 검색 결과가 없을 때 같은 안내를 쓴다 (v3)
                EmptyState(
                    emoji = Glyph.PILL,
                    title = stringResource(R.string.s06_empty_title),
                    body = stringResource(R.string.s06_empty_body),
                    actionLabel = stringResource(R.string.s06_empty_action),
                    onAction = onAdd
                )
            } else {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    state.rows.forEachIndexed { index, row ->
                        MedicationRow(row, onOpen, showDivider = index != state.rows.lastIndex)
                    }
                }
            }
        }

        // 둘째 문장("시연용입니다")은 샘플 계정일 때만 붙인다. 내 계정의 데이터는 시연용이 아니기 때문이다.
        val notice = stringResource(R.string.s06_notice) +
            if (state.isSample) " " + stringResource(R.string.s06_notice_sample) else ""
        Notice(notice, Modifier.padding(top = 16.dp))
    }
}

/** 약 한 줄: [✚] 이름 (배지) / "1정 · 08:00 · 20:00" / "매일"   [상세 ›] */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicationRow(row: MedicationRowUi, onOpen: (Long) -> Unit, showDivider: Boolean) {
    val med = row.med
    val detailName = stringResource(R.string.s06_cd_detail, med.name)
    ListRow(
        leading = { Avatar(Glyph.DOSE) },
        trailing = {
            McButton(
                text = stringResource(R.string.common_detail),
                onClick = { onOpen(med.id) },
                kind = ButtonKind.LIGHT,
                small = true,
                modifier = Modifier.semantics { contentDescription = detailName }
            )
        },
        showDivider = showDivider
    ) {
        // 이름이 길어 배지가 옆에 안 들어가면 배지가 아래 줄로 내려간다.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RowTitle(med.name)
            MedicationStatusPill(row.status, showActive = false)
        }
        RowSub((listOf(med.dosage) + med.schedules.map { timeText(it.time) }).joinToString(" · "))
        RowSub(scheduleDaysText(med.schedules))
    }
}

/**
 * 약 상태 배지: 일시중지(주황) / 종료(회색) / 복용 중(민트).
 * @param showActive false면 "복용 중"일 때 아무것도 그리지 않는다 (목록에서는 배지 없음이 곧 복용 중)
 */
@Composable
fun MedicationStatusPill(status: MedicationStatus, showActive: Boolean) {
    when (status) {
        MedicationStatus.ENDED -> Pill(stringResource(R.string.med_status_ended), PillKind.GRAY)
        MedicationStatus.PAUSED -> Pill(stringResource(R.string.med_status_paused), PillKind.ORANGE)
        MedicationStatus.ACTIVE -> if (showActive) Pill(stringResource(R.string.med_status_active), PillKind.MINT)
    }
}
