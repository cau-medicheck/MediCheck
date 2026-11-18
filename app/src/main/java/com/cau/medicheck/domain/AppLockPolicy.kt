package com.cau.medicheck.domain

/**
 * 앱 잠금(S41)의 "언제 잠글까" 규칙. 순수 계산이라 폰 없이 단위 테스트한다.
 *
 * 잠그는 때는 두 가지뿐이다.
 *   ① 앱을 새로 켰을 때 (이미 로그인돼 있고, 그 계정이 앱 잠금을 켜 둔 경우)
 *   ② 다른 앱·홈 화면에 갔다가 돌아왔을 때
 * 로그인하지 않은 상태에는 보호할 기록이 없으므로 잠그지 않는다.
 */
object AppLockPolicy {

    /**
     * 백그라운드(화면에서 안 보이는 상태)에 이 시간(밀리초) 이상 있다가 돌아오면 잠근다.
     * 0 = 잠깐이라도 나갔다 오면 잠근다. 불편하면 30_000(30초) 등으로 이 숫자만 바꾼다.
     */
    const val GRACE_MILLIS = 0L

    /** 앱을 새로 켰을 때 잠글지 */
    fun lockOnColdStart(enabled: Boolean, loggedIn: Boolean): Boolean = enabled && loggedIn

    /**
     * 화면이 다시 보일 때 잠글지.
     * @param backgroundedAt 백그라운드로 간 시각. null이면 "나갔다 온 것이 아님"(화면 회전, 본인 확인 창을 띄운 것 등)
     * @param now 지금 시각 (backgroundedAt과 같은 시계의 밀리초)
     */
    fun lockOnForeground(enabled: Boolean, loggedIn: Boolean, backgroundedAt: Long?, now: Long): Boolean =
        enabled && loggedIn && backgroundedAt != null && now - backgroundedAt >= GRACE_MILLIS
}
