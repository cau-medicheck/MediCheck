package com.cau.medicheck.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 앱의 기본 글자 모양.
 *
 * MaterialTheme은 bodyLarge를 "따로 정하지 않은 글자의 기본 모양"(LocalTextStyle)으로 내려 준다.
 * 그래서 여기 자간(letterSpacing)이 0.5sp면, 자간을 따로 적지 않은 부품 글자(배지·이름표·링크 등)가
 * 모두 v3보다 조금씩 넓어진다. v3는 자간 0이므로 0sp로 두고, 줄 높이도 지정하지 않는다
 * (구현 명세 00 §4.2 첫 행).
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp
    )
)
