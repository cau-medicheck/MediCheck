package com.cau.medicheck.ui.screens.auth

import com.cau.medicheck.data.local.User
import com.cau.medicheck.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** 로그인 상태가 바뀔 때 AuthUiState가 어떻게 바뀌는가 (구현 명세 00 §3.5, 01 부록 A-10) */
class AuthUiStateTest {

    private val user = User(id = 7, email = "a@b.co", passwordHash = "x", displayName = "가", createdAt = 0)

    @Test
    fun `앱을 켜서 확인을 마치면 로딩이 끝나고 온보딩을 안 봤으면 S00부터다`() {
        val state = AuthUiState().afterSessionChecked(user = null, onboardingSeen = false)
        assertFalse(state.isCheckingSession)
        assertNull(state.currentUser)
        assertEquals(Routes.S00, state.authStartRoute)
    }

    @Test
    fun `온보딩을 봤으면 로그인 전 시작 화면은 S01이다`() {
        val state = AuthUiState().afterSessionChecked(user = null, onboardingSeen = true)
        assertEquals(Routes.S01, state.authStartRoute)
    }

    @Test
    fun `로그인 기록이 있으면 그 사용자를 들고 있다`() {
        val state = AuthUiState().afterSessionChecked(user = user, onboardingSeen = true)
        assertEquals(user, state.currentUser)
    }

    @Test
    fun `로그아웃하면 사용자가 없어지고 시작 화면은 S01이다`() {
        val loggedIn = AuthUiState(isCheckingSession = false, authStartRoute = Routes.S00, currentUser = user)
        val state = loggedIn.afterLogout()
        assertNull(state.currentUser)
        assertEquals(Routes.S01, state.authStartRoute)
        assertFalse(state.isLoading)
    }

    @Test
    fun `계정 삭제 뒤 시작 화면은 S00이다`() {
        val loggedIn = AuthUiState(isCheckingSession = false, authStartRoute = Routes.S01, currentUser = user)
        val state = loggedIn.afterAccountDeleted()
        assertNull(state.currentUser)
        assertEquals(Routes.S00, state.authStartRoute)
    }
}
