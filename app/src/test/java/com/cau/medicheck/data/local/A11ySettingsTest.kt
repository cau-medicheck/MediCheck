package com.cau.medicheck.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

/** 접근성 설정의 기본값과 글자 배율 바로잡기 (DB_DESIGN 부록 A-2) */
class A11ySettingsTest {

    @Test
    fun `처음 값은 글자 100퍼센트 높은 대비 끔 움직임 줄이기 끔이다`() {
        assertEquals(A11ySettings(100, highContrast = false, reduceMotion = false), A11ySettings())
    }

    @Test
    fun `글자 배율은 100 125 150 200만 받고 그 밖은 100으로 바로잡는다`() {
        listOf(100, 125, 150, 200).forEach { assertEquals(it, A11ySettings.normalizeFontScale(it)) }
        assertEquals(100, A11ySettings.normalizeFontScale(130))
        assertEquals(100, A11ySettings.normalizeFontScale(0))
        assertEquals(100, A11ySettings.normalizeFontScale(300))
    }
}
