package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.data.repository.DoseHistoryItem
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.theme.HomeChipDoneBg
import com.cau.medicheck.ui.theme.HomeChipDoneText
import com.cau.medicheck.ui.theme.HomeChipSkippedBg
import com.cau.medicheck.ui.theme.HomeChipSkippedText
import com.cau.medicheck.ui.theme.HomeChipWaitBg
import com.cau.medicheck.ui.theme.HomeChipWaitText
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "2026. 11. 17. 오후 3:04:05" (v3 toLocaleString('ko-KR')과 같은 모양) */

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
    onOpenDose: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault()
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s30)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        if (state.items.isEmpty() && !state.isLoading) {
            SurfaceCard(Modifier.padding(top = 16.dp), padding = 0.dp) {
                EmptyState(
                    emoji = Glyph.HISTORY,
                    title = stringResource(R.string.s30_empty_title),
                    body = stringResource(R.string.s30_empty_body)
                )
            }
        } else {
            // 회차 날짜별로 묶는다: "11월 24일 화요일" 아래에 그날 회차를 바꾼 기록들
            state.items.groupBy { it.date }.forEach { (date, items) ->
                Text(
                    text = fullDateText(date),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    modifier = Modifier.padding(start = 2.dp, top = 18.dp, bottom = 8.dp).semantics { heading() }
                )
                SurfaceCard(padding = 0.dp) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        items.forEachIndexed { index, item ->
                            HistoryRow(item, zone)
                            if (index != items.lastIndex) HorizontalDivider(color = Line)
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

/** 이력 한 줄: 약 이름 / [기록 전] → [복용 완료]            오후 11:47 */
@Composable
private fun HistoryRow(item: DoseHistoryItem, zone: ZoneId) {
    val name = item.medicationName.ifBlank { stringResource(R.string.s30_name_fallback) }
    val from = stringResource(item.from.wordRes())
    val to = stringResource(item.to.wordRes())
    val time = Instant.ofEpochMilli(item.changedAt).atZone(zone).format(CHANGED_TIME_FORMAT)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clearAndSetSemantics { contentDescription = "$name $from → $to $time" }
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                StatusWordChip(item.from)
                Text("→", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(horizontal = 6.dp))
                StatusWordChip(item.to)
            }
        }
        Text(time, fontSize = 12.sp, color = Muted)
    }
}

/** 상태 낱말 꼬리표: 복용 완료(초록) · 건너뜀(파랑) · 기록 전(회색) */
@Composable
private fun StatusWordChip(status: DoseStatus) {
    val (bg, fg) = when (status) {
        DoseStatus.TAKEN -> HomeChipDoneBg to HomeChipDoneText
        DoseStatus.SKIPPED -> HomeChipSkippedBg to HomeChipSkippedText
        DoseStatus.PENDING -> HomeChipWaitBg to HomeChipWaitText
    }
    Text(
        text = stringResource(status.wordRes()),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

// 쓸 때마다 만든다: 앱이 켜진 채로 기기 언어가 바뀌어도 오전/오후 글자가 그 언어로 나오게
private val CHANGED_TIME_FORMAT: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("a h:mm", Locale.getDefault())

/** 저장 상태 → 이력에 쓰는 낱말 ("복용 완료" / "건너뜀" / "미기록") */
private fun DoseStatus.wordRes(): Int = when (this) {
    DoseStatus.TAKEN -> R.string.common_status_word_taken
    DoseStatus.SKIPPED -> R.string.common_status_word_skipped
    DoseStatus.PENDING -> R.string.common_status_word_pending
}
