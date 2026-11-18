package com.cau.medicheck.ui.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** AppLockController 단위 테스트: 앱의 움직임(켜짐·사라짐·다시 보임)에 따라 잠금 상태가 맞게 바뀌는지 */
class AppLockControllerTest {

    private var enabled = true
    private var loggedIn = true
    private lateinit var appLock: AppLockController

    @Before
    fun setUp() {
        enabled = true
        loggedIn = true
        appLock = AppLockController(isEnabled = { enabled }, isLoggedIn = { loggedIn })
    }

    @Test
    fun `새로 켜면 잠기고 본인 확인을 통과하면 풀린다`() {
        appLock.onColdStart()
        assertTrue(appLock.locked.value)

        appLock.unlock()
        assertFalse(appLock.locked.value)
    }

    @Test
    fun `다른 앱에 갔다 오면 잠긴다`() {
        appLock.onStop(now = 1_000, changingConfigurations = false)
        appLock.onStart(now = 5_000)

        assertTrue(appLock.locked.value)
    }

    @Test
    fun `화면을 돌린 것은 나간 것이 아니라 잠기지 않는다`() {
        appLock.onStop(now = 1_000, changingConfigurations = true)
        appLock.onStart(now = 1_200)

        assertFalse(appLock.locked.value)
    }

    @Test
    fun `본인 확인 창 때문에 화면이 사라진 것은 나간 것으로 세지 않는다`() {
        appLock.onColdStart()
        appLock.authenticating = true
        appLock.onStop(now = 1_000, changingConfigurations = false)   // 확인 창이 화면을 덮음
        appLock.unlock()                                               // 통과
        appLock.authenticating = false
        appLock.onStart(now = 2_000)                                   // 우리 화면이 다시 보임

        assertFalse(appLock.locked.value)   // 방금 풀었는데 또 잠기면 안 된다
    }

    @Test
    fun `한 번 돌아온 뒤 화면이 그대로 다시 그려져도 또 잠기지 않는다`() {
        appLock.onStop(now = 1_000, changingConfigurations = false)
        appLock.onStart(now = 5_000)
        appLock.unlock()

        appLock.onStart(now = 6_000)   // 나간 기록은 한 번 쓰고 지운다

        assertFalse(appLock.locked.value)
    }

    @Test
    fun `잠금이 꺼져 있으면 나갔다 와도 잠기지 않는다`() {
        enabled = false
        appLock.onColdStart()
        appLock.onStop(now = 1_000, changingConfigurations = false)
        appLock.onStart(now = 5_000)

        assertFalse(appLock.locked.value)
    }

    @Test
    fun `로그아웃하면 잠금이 풀리고 나간 기록도 지워진다`() {
        appLock.onColdStart()
        appLock.onStop(now = 1_000, changingConfigurations = false)

        appLock.reset()
        appLock.onStart(now = 5_000)

        assertFalse(appLock.locked.value)
    }

    @Test
    fun `잠긴 채로 들어올 때마다 본인 확인 창을 띄워 달라고 알린다`() {
        appLock.onColdStart()                                          // 새로 켬 → 1
        assertEquals(1, appLock.promptRequests.value)

        // 확인 창을 취소하고(잠긴 그대로) 홈으로 나갔다가 다시 들어옴 → 2
        appLock.onStop(now = 1_000, changingConfigurations = false)
        appLock.onStart(now = 5_000)
        assertEquals(2, appLock.promptRequests.value)
    }

    @Test
    fun `확인 창 때문에 화면이 사라졌다 돌아온 것으로는 창을 또 띄우지 않는다`() {
        appLock.onColdStart()
        appLock.authenticating = true
        appLock.onStop(now = 1_000, changingConfigurations = false)   // 확인 창이 화면을 덮음
        appLock.authenticating = false                                 // 취소 → 잠긴 그대로
        appLock.onStart(now = 2_000)

        assertTrue(appLock.locked.value)
        assertEquals(1, appLock.promptRequests.value)   // 취소하자마자 창이 또 뜨면 안 된다
    }

    @Test
    fun `잠기지 않았으면 본인 확인 창을 띄워 달라고 하지 않는다`() {
        enabled = false
        appLock.onColdStart()
        appLock.onStop(now = 1_000, changingConfigurations = false)
        appLock.onStart(now = 5_000)

        assertEquals(0, appLock.promptRequests.value)
    }

    @Test
    fun `화면이 새로 만들어지면 끝맺지 못한 본인 확인 표시를 지운다`() {
        appLock.onColdStart()
        appLock.authenticating = true   // 확인 화면에 가 있는 동안 화면이 다시 만들어짐 → 결과를 받지 못함

        appLock.onActivityCreated()

        assertFalse(appLock.authenticating)   // "잠금 해제"를 다시 누를 수 있다
        assertTrue(appLock.locked.value)      // 잠금은 그대로
    }
}
