package com.cau.medicheck.ui.theme

import androidx.compose.material3.Typography
import com.cau.medicheck.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
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
/**
 * 앱 글꼴 Pretendard (SIL Open Font License 1.1 — 앱에 넣어 배포해도 된다).
 * 새 시안(MediCheck4.html)이 쓰는 Inter·Pretendard 계열에 맞췄다. 한글과 영문·숫자가 한 벌로 디자인돼 있어
 * 폰 기본 글꼴(영문 Roboto + 한글 Noto Sans)보다 숫자와 한글의 굵기·높이가 고르게 보인다.
 * 굵기 다섯 가지를 넣었고, 없는 굵기(Black 등)는 가장 가까운 것이 쓰인다.
 */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold)
)

private val Base = Typography()

/** Material 부품(버튼·입력칸·창)이 쓰는 글자 모양에도 같은 글꼴을 넣는다. 크기·굵기는 Material 기본값 그대로. */
val Typography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Pretendard),
    displayMedium = Base.displayMedium.copy(fontFamily = Pretendard),
    displaySmall = Base.displaySmall.copy(fontFamily = Pretendard),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Pretendard),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Pretendard),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Pretendard),
    titleLarge = Base.titleLarge.copy(fontFamily = Pretendard),
    titleMedium = Base.titleMedium.copy(fontFamily = Pretendard),
    titleSmall = Base.titleSmall.copy(fontFamily = Pretendard),
    // bodyLarge는 "따로 정하지 않은 글자"의 기본 모양이다. v3에 맞춰 자간 0, 줄 높이는 지정하지 않는다.
    bodyLarge = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = Base.bodyMedium.copy(fontFamily = Pretendard),
    bodySmall = Base.bodySmall.copy(fontFamily = Pretendard),
    labelLarge = Base.labelLarge.copy(fontFamily = Pretendard),
    labelMedium = Base.labelMedium.copy(fontFamily = Pretendard),
    labelSmall = Base.labelSmall.copy(fontFamily = Pretendard)
)
