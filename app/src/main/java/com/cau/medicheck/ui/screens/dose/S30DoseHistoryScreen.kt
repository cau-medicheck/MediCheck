package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.ListRow
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.RowSub
import com.cau.medicheck.ui.components.RowTitle
import com.cau.medicheck.ui.components.ScreenTitle
import com.cau.medicheck.ui.components.SurfaceCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "2026. 11. 17. 오후 3:04:05" (v3 toLocaleString('ko-KR')과 같은 모양) */
private val CHANGED_AT_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy. M. d. a h:mm:ss", Locale.KOREAN)

/**
 * S30 복용 변경 이력 (v3 auditTrail()).
 *
 * 상태를 바꾼 기록을 최신순으로 보여 준다: "2026-11-17 · 미기록 → 복용 완료" / "변경 시각 …".
 * 회차 상세의 버튼, 홈·일정의 "✓ 복용 완료", 실제 복용 시각 저장, 실행취소가 모두 여기에 남는다.
 */
@Composable
fun S30DoseHistoryScreen(
    state: DoseHistoryUiState,
    onBack: () -> Unit,
    onOpenDose: () -> Unit
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s30)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenTitle(
            title = stringResource(R.string.s30_title),
            subtitle = stringResource(R.string.s30_subtitle),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        SurfaceCard(padding = 0.dp) {
            if (state.items.isEmpty() && !state.isLoading) {
                EmptyState(
                    emoji = Glyph.HISTORY,
                    title = stringResource(R.string.s30_empty_title),
                    body = stringResource(R.string.s30_empty_body)
                )
            } else {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    state.items.forEachIndexed { index, item ->
                        ListRow(
                            leading = { Avatar(Glyph.HISTORY) },
                            showDivider = index != state.items.lastIndex
                        ) {
                            RowTitle(item.medicationName.ifBlank { stringResource(R.string.s30_name_fallback) })
                            RowSub(
                                stringResource(
                                    R.string.s30_line,
                                    item.date.toString(),
                                    stringResource(item.from.wordRes()),
                                    stringResource(item.to.wordRes())
                                )
                            )
                            val changedAt = Instant.ofEpochMilli(item.changedAt)
                                .atZone(ZoneId.systemDefault())
                                .format(CHANGED_AT_FORMAT)
                            RowSub(stringResource(R.string.s30_changed_at, changedAt))
                        }
                    }
                }
            }
        }

        McButton(
            text = stringResource(R.string.s30_btn_open_dose),
            onClick = onOpenDose,
            kind = ButtonKind.LIGHT,
            full = true,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/** 저장 상태 → 이력에 쓰는 낱말 ("복용 완료" / "건너뜀" / "미기록") */
private fun DoseStatus.wordRes(): Int = when (this) {
    DoseStatus.TAKEN -> R.string.common_status_word_taken
    DoseStatus.SKIPPED -> R.string.common_status_word_skipped
    DoseStatus.PENDING -> R.string.common_status_word_pending
}
