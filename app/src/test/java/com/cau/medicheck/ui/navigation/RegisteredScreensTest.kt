package com.cau.medicheck.ui.navigation

import com.cau.medicheck.ui.navigation.graph.REGISTERED_SCREENS
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 47개 화면이 빠짐없이 화면 틀에 붙어 있는지 확인한다.
 * 주소(route)만 있고 화면을 붙이지 않으면, 그 주소로 이동하는 순간 앱이 멈춘다. 그 실수를 빌드 단계에서 잡는다.
 */
class RegisteredScreensTest {

    @Test
    fun `로그인 뒤 화면 S04부터 S46까지 43개가 모두 등록돼 있다`() {
        val expected = Routes.ALL_SCREEN_IDS.toSet() - Routes.PRE_LOGIN

        assertEquals(43, expected.size)
        assertEquals(expected, REGISTERED_SCREENS)
    }

    @Test
    fun `로그인 전 화면은 S00부터 S03까지 4개다`() {
        assertEquals(setOf("S00", "S01", "S02", "S03"), Routes.PRE_LOGIN)
    }
}
