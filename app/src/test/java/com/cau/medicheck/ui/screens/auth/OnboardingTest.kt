package com.cau.medicheck.ui.screens.auth

import com.cau.medicheck.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** S00 온보딩 장 넘기기 (v3 act('onboardNext')) */
class OnboardingTest {

    @Test
    fun `온보딩은 3장이다`() {
        assertEquals(3, Onboarding.pages.size)
    }

    @Test
    fun `다음을 누르면 한 장씩 앞으로 가고 마지막 장을 넘지 않는다`() {
        assertEquals(1, Onboarding.next(0))
        assertEquals(2, Onboarding.next(1))
        assertEquals(2, Onboarding.next(2))
    }

    @Test
    fun `마지막 장에서만 주 버튼이 시작하기다`() {
        assertFalse(Onboarding.isLast(0))
        assertFalse(Onboarding.isLast(1))
        assertTrue(Onboarding.isLast(2))
        assertEquals(R.string.s00_next, Onboarding.primaryButtonRes(0))
        assertEquals(R.string.s00_next, Onboarding.primaryButtonRes(1))
        assertEquals(R.string.s00_start, Onboarding.primaryButtonRes(2))
    }
}
