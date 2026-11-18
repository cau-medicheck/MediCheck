package com.cau.medicheck.ui.screens.medication

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.LocalToastBus
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.PickerField
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.DashedBg
import com.cau.medicheck.ui.theme.DashedBorder
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/**
 * S29 사진으로 약 등록 — 시안 (v3 ocrConcept()).
 *
 * 이번 릴리스에서 만들지 않는 기능(약 봉투 사진에서 글자를 읽어 자동 입력)의 모양만 보여 주는 화면이다.
 * 사진을 고를 수는 있지만 고른 사진은 저장하지도, 읽지도, 다른 화면에 넘기지도 않는다.
 * ViewModel이 없다 — "사진을 골랐는지"만 이 화면이 떠 있는 동안 기억한다.
 */
@Composable
fun S29PhotoRegisterScreen(onBack: () -> Unit) {
    val toastBus = LocalToastBus.current
    var picked by remember { mutableStateOf(false) }

    // 안드로이드가 제공하는 "사진 고르기" 창을 띄우는 도구. 권한을 요청할 필요가 없다.
    // 사용자가 고르면 그 사진의 주소(Uri)가 돌아오고, 취소하면 null이 돌아온다.
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            picked = true
            toastBus.show(R.string.s29_toast_picked)
        }
    }

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s29)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenIntro(stringResource(R.string.s29_subtitle), Modifier.padding(vertical = 16.dp))

        SurfaceCard {
            UploadBox()

            PickerField(
                text = stringResource(if (picked) R.string.s29_file_picked else R.string.s29_file_none),
                trailing = stringResource(R.string.s29_file_action),
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.padding(top = 16.dp)
            )

            FieldLabel(stringResource(R.string.s29_label_example))
            val first = stringResource(R.string.s29_example_fields)
            val second = stringResource(R.string.s29_example_warning)
            Notice(
                buildAnnotatedString {
                    append(first)
                    append("\n")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(second) }
                }
            )
        }

        Notice(stringResource(R.string.extra_notice_future), Modifier.padding(top = 14.dp), warn = true)
    }
}

/** 점선 테두리 상자: "▧ 약 봉투 사진 추가". 누를 수 없다 (v3 .upload) */
@Composable
private fun UploadBox() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(DashedBg, RoundedCornerShape(15.dp))
            // 점선 테두리는 기본 부품에 없어서 직접 그린다. dashPathEffect = "6만큼 긋고 5만큼 띄우기"를 반복
            .drawBehind {
                drawRoundRect(
                    color = DashedBorder,
                    cornerRadius = CornerRadius(15.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
                    )
                )
            }
            .padding(horizontal = 15.dp, vertical = 30.dp)
    ) {
        Text(text = Glyph.UPLOAD, fontSize = 35.sp, color = Muted, modifier = Modifier.clearAndSetSemantics { })
        Text(
            text = stringResource(R.string.s29_upload_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Text(
            text = stringResource(R.string.s29_upload_body),
            fontSize = 12.sp,
            lineHeight = 19.sp,
            color = Muted,
            textAlign = TextAlign.Center
        )
    }
}
