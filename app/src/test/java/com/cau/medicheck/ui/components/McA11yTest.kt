package com.cau.medicheck.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** 접근성 설정을 화면에 적용하는 계산 (구현 명세 00 §6.1·6.2)과 토스트 위치 (§1.5) */
class McA11yTest {

    @Test
    fun `앱 글자 배율은 폰 글꼴 배율과 곱한다`() {
        assertEquals(1f, effectiveFontScale(1f, 100), 0.0001f)
        assertEquals(1.25f, effectiveFontScale(1f, 125), 0.0001f)
        assertEquals(1.5f, effectiveFontScale(1.2f, 125), 0.0001f)
    }

    @Test
    fun `합친 배율은 2배를 넘지 않는다`() {
        assertEquals(2f, effectiveFontScale(1f, 200), 0.0001f)
        assertEquals(2f, effectiveFontScale(1.5f, 200), 0.0001f)
    }

    @Test
    fun `폰 설정보다 작아지지는 않는다`() {
        assertEquals(2f, effectiveFontScale(2f, 100), 0.0001f)
    }

    @Test
    fun `대비 1_2배는 CSS contrast(1_2)처럼 값x1_2 빼기 25_5다`() {
        val m = contrastMatrixValues(1.2f)
        assertArrayEquals(
            floatArrayOf(
                1.2f, 0f, 0f, 0f, -25.5f,
                0f, 1.2f, 0f, 0f, -25.5f,
                0f, 0f, 1.2f, 0f, -25.5f,
                0f, 0f, 0f, 1f, 0f
            ),
            m, 0.001f
        )
    }

    @Test
    fun `토스트는 틀 아래와 키보드 중 높은 쪽 위 18dp에 뜬다`() {
        assertEquals(77.dp, toastBottomOffset(frameBottom = 59.dp, imeBottom = 0.dp))
        assertEquals(318.dp, toastBottomOffset(frameBottom = 59.dp, imeBottom = 300.dp))
    }
}
