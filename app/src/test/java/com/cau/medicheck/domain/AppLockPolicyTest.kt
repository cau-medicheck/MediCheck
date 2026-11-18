package com.cau.medicheck.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AppLockPolicy 단위 테스트: 앱 잠금(S41)을 언제 걸지 */
class AppLockPolicyTest {

    // ---------- 앱을 새로 켰을 때 ----------

    @Test
    fun `잠금을 켠 계정으로 로그인해 있으면 새로 켤 때 잠근다`() {
        assertTrue(AppLockPolicy.lockOnColdStart(enabled = true, loggedIn = true))
    }

    @Test
    fun `잠금이 꺼져 있거나 로그인하지 않았으면 새로 켤 때 잠그지 않는다`() {
        assertFalse(AppLockPolicy.lockOnColdStart(enabled = false, loggedIn = true))
        assertFalse(AppLockPolicy.lockOnColdStart(enabled = true, loggedIn = false))
    }

    // ---------- 화면이 다시 보일 때 ----------

    @Test
    fun `나갔다 돌아오면 잠근다`() {
        assertTrue(AppLockPolicy.lockOnForeground(enabled = true, loggedIn = true, backgroundedAt = 1_000, now = 1_000))
        assertTrue(AppLockPolicy.lockOnForeground(enabled = true, loggedIn = true, backgroundedAt = 1_000, now = 9_000))
    }

    @Test
    fun `나간 적이 없으면 잠그지 않는다`() {
        // 화면 회전이나 본인 확인 창 때문에 화면이 잠깐 사라진 경우 = 나간 시각이 없다
        assertFalse(AppLockPolicy.lockOnForeground(enabled = true, loggedIn = true, backgroundedAt = null, now = 9_000))
    }

    @Test
    fun `잠금이 꺼져 있거나 로그아웃 상태면 돌아와도 잠그지 않는다`() {
        assertFalse(AppLockPolicy.lockOnForeground(enabled = false, loggedIn = true, backgroundedAt = 1_000, now = 9_000))
        assertFalse(AppLockPolicy.lockOnForeground(enabled = true, loggedIn = false, backgroundedAt = 1_000, now = 9_000))
    }
}
