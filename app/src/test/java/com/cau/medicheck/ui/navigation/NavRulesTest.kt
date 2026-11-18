package com.cau.medicheck.ui.navigation

import java.io.File
import com.cau.medicheck.domain.RequirementCsv
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

    @Test
    fun `화면마다 강조할 하단 탭이 정해져 있다`() {
        assertEquals("S04", NavRules.tabRootOf("S04"))
        assertEquals("S05", NavRules.tabRootOf("S05"))
        // 회차 상세는 어디서 열었든 일정 탭
        assertEquals("S05", NavRules.tabRootOf("S10?scheduleId=3&date=2026-11-17"))
        assertEquals("S06", NavRules.tabRootOf("S08?medId={medId}"))
        assertEquals("S05", NavRules.tabRootOf("S12"))   // 기록 화면은 일정 탭에 속한다
        assertEquals("S13", NavRules.tabRootOf("S15"))
        // 설정·알림·이력 등은 어느 탭에도 속하지 않는다
        assertEquals(null, NavRules.tabRootOf("S19"))
        assertEquals(null, NavRules.tabRootOf("S30"))
        assertEquals(null, NavRules.tabRootOf(null))
    }

    @Test
    fun `주소의 날짜 글자가 틀려도 멈추지 않고 null을 돌려준다`() {
        assertEquals(java.time.LocalDate.of(2026, 11, 17), NavRules.parseDateOrNull("2026-11-17"))
        assertEquals(null, NavRules.parseDateOrNull("abc"))
        assertEquals(null, NavRules.parseDateOrNull(""))
        assertEquals(null, NavRules.parseDateOrNull(null))
    }

    // ---------- 요구사항 맵(S45)의 기능 줄 → 갈 곳 ----------

    @Test
    fun `첫 화면이 S04 이후면 그 화면을 연다`() {
        assertEquals(ReqTarget.Open("S07"), NavRules.reqTarget(listOf("S07", "S10", "S12", "S32")))
        assertEquals(ReqTarget.Open("S46"), NavRules.reqTarget(listOf("S46")))
        assertEquals(ReqTarget.Open("S23"), NavRules.reqTarget(listOf("S23")))
    }

    @Test
    fun `S45 자신뿐이면 갈 곳이 없고 뒤에 다른 화면이 있으면 그 화면을 연다`() {
        assertEquals(ReqTarget.Stay, NavRules.reqTarget(listOf("S45")))
        assertEquals(ReqTarget.Open("S37"), NavRules.reqTarget(listOf("S45", "S37")))
        assertEquals(ReqTarget.Open("S38"), NavRules.reqTarget(listOf("S45", "S38")))
    }

    @Test
    fun `첫 화면이 로그인 전 화면이면 그 뒤의 첫 화면을 연다`() {
        assertEquals(ReqTarget.Open("S04"), NavRules.reqTarget(listOf("S01", "S04")))
        assertEquals(ReqTarget.Open("S08"), NavRules.reqTarget(listOf("S02", "S08", "S39")))
        assertEquals(ReqTarget.Open("S06"), NavRules.reqTarget(listOf("S01", "S06", "S11")))
    }

    @Test
    fun `로그인 전 화면뿐이면 안내만 띄운다`() {
        listOf(listOf("S02"), listOf("S01"), listOf("S01", "S02"), listOf("S03"), listOf("S00")).forEach {
            assertEquals(ReqTarget.PreLoginOnly, NavRules.reqTarget(it))
        }
    }

    @Test
    fun `화면 ID가 없거나 화면 ID 꼴이 아니면 갈 곳이 없다`() {
        assertEquals(ReqTarget.Stay, NavRules.reqTarget(emptyList()))
        assertEquals(ReqTarget.Stay, NavRules.reqTarget(listOf("공통", "S99", "해당 없음")))
    }

    @Test
    fun `자산의 107줄 중 97줄은 화면을 열고 6줄은 안내 4줄은 갈 곳이 없다`() {
        val all = RequirementCsv.parse(File("src/main/assets/requirements_107.csv").readText(Charsets.UTF_8))
        val targets = all.map { NavRules.reqTarget(it.screens) }

        assertEquals(97, targets.count { it is ReqTarget.Open })
        assertEquals(6, targets.count { it == ReqTarget.PreLoginOnly })
        assertEquals(4, targets.count { it == ReqTarget.Stay })
    }
}
