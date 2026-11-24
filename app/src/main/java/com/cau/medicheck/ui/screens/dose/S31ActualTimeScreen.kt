package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.PickerField
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.TimePickerPopup
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.Mint
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText
import com.cau.medicheck.ui.theme.Teal
import java.time.LocalTime

/**
 * S31 실제 복용 시각 기록 (v3 actualDoseTime()).
 *
 * 예정 시각과 따로, 실제로 먹은 시각을 적고 그 회차를 복용 완료로 기록한다.
 * 시각 칸을 누르면 시각 고르는 창이 열린다. 저장은 아래 버튼을 눌러야 된다.
 */
@Composable
fun S31ActualTimeScreen(
    state: ActualTimeUiState,
    onBack: () -> Unit,
    onTimePicked: (LocalTime) -> Unit,
    onSave: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val dose = state.dose

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s31)) {
        if (state.isLoading) return@McScreenColumn

        // 회차를 찾지 못했을 때
        if (dose == null) {
            BackLink(stringResource(R.string.common_back_previous), onBack)
            ScreenIntro(stringResource(R.string.s31_empty_subtitle), Modifier.padding(vertical = 16.dp))
            SurfaceCard(padding = 0.dp) {
                EmptyState(
                    emoji = Glyph.EMPTY_CIRCLE,
                    title = stringResource(R.string.s31_empty_title),
                    body = stringResource(R.string.s31_empty_body)
                )
            }
            return@McScreenColumn
        }

        BackLink(stringResource(R.string.s31_back), onBack)

        SurfaceCard(Modifier.padding(top = 14.dp)) {
            Text(text = dose.medicationName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(
                text = stringResource(R.string.s31_scheduled_line, dose.date.toString(), timeText(dose.time)),
                fontSize = 12.sp,
                color = ParagraphText,
                modifier = Modifier.padding(top = 7.dp)
            )
            FieldLabel(stringResource(R.string.s31_label))
            PickerField(text = timeText(state.time), onClick = { showTimePicker = true })
            // 빠른 선택: 예정 시각 그대로 / 지금
            val now = LocalTime.now().withSecond(0).withNano(0)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                QuickTime(stringResource(R.string.s31_quick_scheduled, timeText(dose.time)), state.time == dose.time) { onTimePicked(dose.time) }
                QuickTime(stringResource(R.string.s31_quick_now, timeText(now)), state.time == now) { onTimePicked(now) }
            }
            McButton(
                text = stringResource(R.string.s31_btn_save),
                onClick = onSave,
                full = true,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

    }

    if (showTimePicker) {
        TimePickerPopup(
            initialTime = state.time,
            onDismiss = { showTimePicker = false },
            onTimePicked = { time ->
                onTimePicked(time)
                showTimePicker = false
            }
        )
    }
}

/** 빠른 시각 단추: "예정 12:30" / "지금 18:10". 고른 값과 같으면 청록 */
@Composable
private fun QuickTime(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(99.dp)
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) Color.White else Teal,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Teal else Mint)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}
