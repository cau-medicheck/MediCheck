package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McIcon
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.WarnText

/**
 * S09 삭제 확인 (v3 deleteConfirm()).
 *
 * "삭제하기"를 누르면 약이 목록·앞으로의 일정·알림에서 사라진다. 지난 날짜의 기록과 이미 응답한 기록은
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
        val name = state.med?.name ?: stringResource(R.string.common_selected_med)

        SurfaceCard(Modifier.padding(top = 22.dp), padding = 0.dp) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 26.dp)
            ) {
                McIcon(Glyph.WARNING, size = 40, tint = WarnText)
                Text(
                    text = stringResource(R.string.s09_title),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Navy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .semantics { heading() }
                )
                // "{약 이름}을 삭제하면 …" — 약 이름만 굵게. AnnotatedString = 글자 일부만 모양을 다르게 할 수 있는 글자
                val tail = stringResource(R.string.s09_body_tail)
                val second = stringResource(R.string.s09_body_second)
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name) }
                        append(tail)
                        append("\n")
                        append(second)
                    },
                    fontSize = 12.sp,
                    lineHeight = 21.sp,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
                )
                McButton(
                    text = stringResource(R.string.s09_btn_delete),
                    onClick = onDelete,
                    kind = ButtonKind.RED,
                    full = true,
                    loading = state.isBusy
                )
                McButton(
                    text = stringResource(R.string.s09_btn_keep),
                    onClick = onCancel,
                    kind = ButtonKind.WHITE,
                    full = true,
                    enabled = !state.isBusy,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
