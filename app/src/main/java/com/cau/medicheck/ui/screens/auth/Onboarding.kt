package com.cau.medicheck.ui.screens.auth

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.Glyph

/**
 * S00 온보딩의 한 장: 아이콘 + 제목 + 설명. (v3 welcome()의 slides 배열)
 * 제목·설명은 문구 번호로 들고 있어서 화면에서 stringResource로 글자를 꺼낸다.
 */
data class OnboardingPage(
    val symbol: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int
)

/**
 * 온보딩 장 넘기기 규칙. 화면과 떼어 두어 단위 테스트한다.
 * v3는 앞으로만 간다("다음"). 장을 되돌리는 동작은 없다.
 */
object Onboarding {

    val pages: List<OnboardingPage> = listOf(
        OnboardingPage(Glyph.PILL, R.string.s00_slide1_title, R.string.s00_slide1_body),
        OnboardingPage(Glyph.BELL, R.string.s00_slide2_title, R.string.s00_slide2_body),
        OnboardingPage(Glyph.CHART, R.string.s00_slide3_title, R.string.s00_slide3_body)
    )

    /** 마지막 장인지. 마지막 장의 주 버튼만 "시작하기"이고 나머지는 "다음"이다. */
    fun isLast(index: Int): Boolean = index >= pages.lastIndex

    /** "다음"을 누른 뒤의 장 번호. 마지막 장을 넘어가지 않는다. */
    fun next(index: Int): Int = (index + 1).coerceAtMost(pages.lastIndex)

    /** 주 버튼 문구: 마지막 장이면 "시작하기", 아니면 "다음" */
    @StringRes
    fun primaryButtonRes(index: Int): Int = if (isLast(index)) R.string.s00_start else R.string.s00_next
}
