package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.MedicationColor
import com.cau.medicheck.domain.MedicationShape
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.MedShapeArt
import com.cau.medicheck.ui.components.toColor
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/** 삭제 단추의 빨강 (시안 S09 B) */
private val DeleteRed = Color(0xFFC8504B)

/**
 * S09 삭제 확인 (v3 deleteConfirm()). 2026-11-25 시안 B(04_화면캡처/남은화면_시안/S09_시안.html)로 다시 그림:
 * 약 상세와 같은 약 색으로 칠한 큰 약 카드 한 장 + "이 약을 삭제할까요?" + 빨간 삭제 단추 + 취소 글자.
 *
 * "삭제"를 누르면 약이 목록·앞으로의 일정·알림에서 사라진다. 지난 날짜의 기록과 이미 응답한 기록은
 * 그때의 이름·복용량 그대로 남는다(기록마다 찍어 둔 스냅샷).
 */
@Composable
fun S09DeleteConfirmScreen(
    state: MedicationDetailUiState,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s09)) {
        if (state.isLoading) return@McScreenColumn

        BackLink(stringResource(R.string.s09_back), onCancel)

        // 약을 찾지 못했으면 이름 자리에 "선택한 약"을 쓴다 (v3)
        val med = state.med
        val name = med?.name ?: stringResource(R.string.common_selected_med)
        val color = med?.color ?: MedicationColor.TEAL
        val medColor = color.toColor()

        // ----- 큰 약 카드: 약 그림 + 이름 + 언제 먹는지 -----
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(medColor.copy(alpha = 0.16f), medColor.copy(alpha = 0.05f))))
                .padding(vertical = 30.dp, horizontal = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(66.dp)
                    .shadow(3.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
            ) {
                MedShapeArt(med?.shape ?: MedicationShape.TABLET, color, Modifier.size(40.dp))
            }
            Text(
                text = name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp)
            )
            if (med != null) {
                Text(whenText(med), fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 3.dp))
            }
        }

        Text(
            text = stringResource(R.string.s09_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Navy,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 26.dp)
                .semantics { heading() }
        )

        // 빨간 삭제 단추 ("Omega-3 삭제"). 처리 중이면 돌아가는 표시
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(DeleteRed)
                .clickable(enabled = !state.isBusy, role = Role.Button, onClick = onDelete)
        ) {
            if (state.isBusy) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text(stringResource(R.string.s09_btn_delete, name), fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
        Text(
            text = stringResource(R.string.action_cancel),
            fontSize = 14.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !state.isBusy, role = Role.Button, onClick = onCancel)
                .padding(vertical = 14.dp)
        )
    }
}
