package com.cau.medicheck.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.MedicationColor
import com.cau.medicheck.domain.MedicationShape
import com.cau.medicheck.ui.theme.FieldBorder
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.Teal

// 약 아이콘: 약마다 고른 모양(알약·캡슐·물약·가루)과 색으로 그린다. (2026-11-24, 시안: 04_화면캡처/내약탭_시안/시안.html A1)
// 그림 파일 없이 Canvas로 그려서 어떤 크기·색에도 맞는다. 장식이라 TalkBack이 읽지 않는다(약 이름이 옆에 있다).

/** 약 색 → 화면 색 */
fun MedicationColor.toColor(): Color = when (this) {
    MedicationColor.TEAL -> Color(0xFF1F9A8C)
    MedicationColor.ORANGE -> Color(0xFFE08A2E)
    MedicationColor.BLUE -> Color(0xFF3E73C5)
    MedicationColor.PURPLE -> Color(0xFF8A63C9)
    MedicationColor.PINK -> Color(0xFFD46A8C)
    MedicationColor.GRAY -> Color(0xFF7A8C96)
}

/**
 * 약 아이콘: 옅은 바탕(그 색 13%) 위에 모양을 그린다.
 * @param size 바깥 상자 크기 (그림은 그 62%)
 */
@Composable
fun McMedIcon(shape: MedicationShape, color: MedicationColor, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    val c = color.toColor()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(c.copy(alpha = 0.13f))
            .clearAndSetSemantics { }
    ) {
        MedShapeDrawing(shape, c, Modifier.size(size * 0.62f))
    }
}

/** 모양 그림 하나 (바탕 없이) */
@Composable
private fun MedShapeDrawing(shape: MedicationShape, c: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (shape) {
            // 알약: 동그라미 + 가운데 홈
            MedicationShape.TABLET -> {
                drawCircle(c, radius = w * 0.42f)
                drawLine(Color.White.copy(alpha = 0.85f), Offset(w * 0.27f, h / 2), Offset(w * 0.73f, h / 2), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
            }
            // 캡슐: 비스듬한 두 색 캡슐 (흰 반쪽 + 색 반쪽)
            MedicationShape.CAPSULE -> rotate(-40f) {
                val capH = h * 0.4f
                val top = (h - capH) / 2
                val left = w * 0.08f
                val capW = w * 0.84f
                drawRoundRect(Color.White, Offset(left, top), Size(capW, capH), CornerRadius(capH / 2))
                drawRoundRect(c, Offset(left, top), Size(capW, capH), CornerRadius(capH / 2), style = Stroke(w * 0.05f))
                val half = Path().apply {
                    moveTo(w / 2, top)
                    lineTo(left + capW - capH / 2, top)
                    arcTo(androidx.compose.ui.geometry.Rect(left + capW - capH, top, left + capW, top + capH), -90f, 180f, false)
                    lineTo(w / 2, top + capH)
                    close()
                }
                drawPath(half, c)
            }
            // 물약: 뚜껑 + 병 + 흰 라벨
            MedicationShape.LIQUID -> {
                drawRoundRect(c.copy(alpha = 0.7f), Offset(w * 0.37f, h * 0.06f), Size(w * 0.26f, h * 0.16f), CornerRadius(w * 0.04f))
                drawRoundRect(c, Offset(w * 0.24f, h * 0.24f), Size(w * 0.52f, h * 0.7f), CornerRadius(w * 0.1f))
                drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(w * 0.32f, h * 0.48f), Size(w * 0.36f, h * 0.2f), CornerRadius(w * 0.04f))
            }
            // 가루: 봉지 + 위쪽 톱니 자국
            MedicationShape.POWDER -> {
                drawRoundRect(c, Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.66f), CornerRadius(w * 0.08f))
                val zig = Path().apply {
                    moveTo(w * 0.18f, h * 0.3f)
                    var x = w * 0.18f
                    var up = true
                    while (x < w * 0.82f) {
                        x += w * 0.08f
                        lineTo(x.coerceAtMost(w * 0.82f), if (up) h * 0.24f else h * 0.3f)
                        up = !up
                    }
                }
                drawPath(zig, Color.White.copy(alpha = 0.85f), style = Stroke(w * 0.045f))
            }
        }
    }
}

/** 모양 이름: 알약 / 캡슐 / 물약 / 가루 */
@Composable
fun shapeLabel(shape: MedicationShape): String = stringResource(
    when (shape) {
        MedicationShape.TABLET -> R.string.shape_tablet
        MedicationShape.CAPSULE -> R.string.shape_capsule
        MedicationShape.LIQUID -> R.string.shape_liquid
        MedicationShape.POWDER -> R.string.shape_powder
    }
)

/** 색 이름 (TalkBack용) */
@Composable
private fun colorLabel(color: MedicationColor): String = stringResource(
    when (color) {
        MedicationColor.TEAL -> R.string.color_teal
        MedicationColor.ORANGE -> R.string.color_orange
        MedicationColor.BLUE -> R.string.color_blue
        MedicationColor.PURPLE -> R.string.color_purple
        MedicationColor.PINK -> R.string.color_pink
        MedicationColor.GRAY -> R.string.color_gray
    }
)

/**
 * 약 등록·수정 화면의 모양 고르기: [알약] [캡슐] [물약] [가루] — 고른 색으로 그린다. 하나만 고른다.
 */
@Composable
fun ShapePicker(selected: MedicationShape, color: MedicationColor, onSelect: (MedicationShape) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.selectableGroup()) {
        MedicationShape.entries.forEach { shape ->
            val on = shape == selected
            val boxShape = RoundedCornerShape(14.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(68.dp)
                    .clip(boxShape)
                    .background(Color.White)
                    .border(if (on) 2.dp else 1.4.dp, if (on) Teal else FieldBorder, boxShape)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(shape) })
                    .padding(top = 8.dp, bottom = 6.dp)
            ) {
                MedShapeDrawing(shape, color.toColor(), Modifier.size(28.dp))
                Text(
                    text = shapeLabel(shape),
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) Teal else Navy,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** 약 등록·수정 화면의 색 고르기: 동그라미 6개. 고른 색은 짙은 테두리. 하나만 고른다. */
@Composable
fun ColorPicker(selected: MedicationColor, onSelect: (MedicationColor) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.selectableGroup()) {
        MedicationColor.entries.forEach { color ->
            val on = color == selected
            val label = colorLabel(color)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(if (on) 2.5.dp else 0.dp, if (on) Navy else Color.Transparent, CircleShape)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(color) })
                    .semantics { contentDescription = label }
            ) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(color.toColor()))
            }
        }
    }
}
