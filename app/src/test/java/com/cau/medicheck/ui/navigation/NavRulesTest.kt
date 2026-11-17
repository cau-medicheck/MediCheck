package com.cau.medicheck.ui.navigation

import com.cau.medicheck.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시작 화면 고르기와 route 규칙 (구현 명세 00 §3.4, §7.6) */
class NavRulesTest {

    @Test
    fun `로그인 기록이 없고 온보딩을 본 적 없으면 S00에서 시작한다`() {
        assertEquals(Routes.S00, NavRules.startRoute(isLoggedIn = false, onboardingSeen = false))
    }

    @Test
    fun `로그인 기록이 없고 온보딩을 봤으면 S01에서 시작한다`() {
        assertEquals(Routes.S01, NavRules.startRoute(isLoggedIn = false, onboardingSeen = true))
    }

    @Test
    fun `로그인 기록이 있으면 온보딩과 상관없이 S04에서 시작한다`() {
        assertEquals(Routes.S04, NavRules.startRoute(isLoggedIn = true, onboardingSeen = false))
        assertEquals(Routes.S04, NavRules.startRoute(isLoggedIn = true, onboardingSeen = true))
    }

    @Test
    fun `화면 ID는 S00부터 S46까지 47개이고 그중 로그인 전 화면은 S00~S03 4개다`() {
        assertEquals(47, Routes.ALL_SCREEN_IDS.size)
        assertEquals("S00", Routes.ALL_SCREEN_IDS.first())
        assertEquals("S46", Routes.ALL_SCREEN_IDS.last())
        assertEquals(setOf("S00", "S01", "S02", "S03"), Routes.PRE_LOGIN)
        assertTrue(Routes.ALL_SCREEN_IDS.containsAll(Routes.PRE_LOGIN))
    }

    @Test
    fun `route의 꼬리를 떼고 화면 ID만 꺼낸다`() {
        assertEquals("S07", NavRules.screenIdOf("S07?medId=5"))
        assertEquals("S01", NavRules.screenIdOf("S01"))
    }

    @Test
    fun `값이 채워지지 않은 route 틀은 이동할 수 없는 주소로 본다`() {
        assertFalse(NavRules.isFilledRoute("S07?medId={medId}"))
        assertTrue(NavRules.isFilledRoute("S07?medId=5"))
        assertTrue(NavRules.isFilledRoute("S01"))
    }

    @Test
    fun `47개 화면 모두 머리말 제목이 있고 서로 다르다`() {
        val titles = Routes.ALL_SCREEN_IDS.map { Routes.titleRes(it) }
        titles.forEach { assertNotEquals(R.string.app_name, it) }
        assertEquals(47, titles.toSet().size)
        assertEquals(R.string.screen_title_s07, Routes.titleRes("S07?medId=3"))
    }
}
