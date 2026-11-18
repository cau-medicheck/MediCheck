package com.cau.medicheck.ui.lock

import com.cau.medicheck.domain.AppLockPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 앱이 지금 잠겨 있는지를 들고 있는 곳. 앱 전체에 하나(AppContainer). (S41 앱 잠금)
 *
 * 잠금 상태는 저장하지 않고 메모리에만 둔다 — 앱을 새로 켜면 어차피 처음부터 다시 판단한다.
 * "언제 잠글까"의 규칙은 AppLockPolicy(순수 계산)에 있고, 이 클래스는 그 결과를 상태로 들고 있기만 한다.
 *
 *   앱을 새로 켬          → onColdStart()        (AuthViewModel이 로그인 기록·설정을 읽은 직후)
 *   화면에서 사라짐        → onStop(시각, …)       (MainActivity.onStop)
 *   화면에 다시 보임       → onStart(시각)         (MainActivity.onStart)
 *   본인 확인을 통과함      → unlock()
 *   로그아웃·계정 삭제     → reset()
 *
 * @param isEnabled 지금 로그인한 계정이 앱 잠금을 켜 두었는지
 * @param isLoggedIn 로그인한 사람이 있는지
 */
class AppLockController(
    private val isEnabled: () -> Boolean,
    private val isLoggedIn: () -> Boolean
) {
    private val _locked = MutableStateFlow(false)

    /** 잠겨 있으면 true. 화면(MediCheckApp)이 이 값을 보고 잠금 화면을 덮는다. */
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /**
     * 우리 앱이 띄운 본인 확인 창이 떠 있는 동안 true.
     * 그 창이 화면을 덮으면 안드로이드가 우리 화면을 "사라졌다"고 알릴 수 있는데, 그것까지 "나갔다 온 것"으로 세면
     * 방금 본인 확인을 통과하자마자 또 잠긴다. 그래서 이 값이 true인 동안의 onStop은 세지 않는다.
     */
    var authenticating = false

    private val _promptRequests = MutableStateFlow(0)

    /**
     * "본인 확인 창을 자동으로 띄워 달라"는 신호. 숫자가 바뀔 때마다 화면이 창을 한 번 띄운다.
     * 잠긴 채로 앱에 들어올 때마다(새로 켰을 때, 나갔다 돌아왔을 때) 하나씩 올린다.
     * 확인 창을 취소했을 때는 올리지 않는다 — 그러면 취소하자마자 창이 또 뜨는 일이 없다.
     */
    val promptRequests: StateFlow<Int> = _promptRequests.asStateFlow()

    /** 백그라운드로 간 시각. 나갔다 온 것이 아니면 null */
    private var backgroundedAt: Long? = null

    /**
     * 화면(Activity)이 새로 만들어질 때 부른다.
     * 본인 확인 화면에 가 있는 동안 화면이 다시 만들어지면(회전 등) 그 결과를 받을 곳이 사라져
     * authenticating이 true로 남을 수 있다. 그러면 "잠금 해제"를 눌러도 창이 뜨지 않아 갇힌다.
     * 새 화면은 늘 "확인 창 없음"에서 시작하게 해서 다시 시도할 수 있게 한다.
     */
    fun onActivityCreated() {
        authenticating = false
    }

    fun onColdStart() {
        authenticating = false   // 앞 화면이 닫히며 끝맺지 못한 본인 확인이 있었더라도 처음부터 다시
        _locked.value = AppLockPolicy.lockOnColdStart(isEnabled(), isLoggedIn())
        if (_locked.value) _promptRequests.value++
    }

    /** @param changingConfigurations 화면 회전처럼 화면을 다시 만드느라 잠깐 사라지는 것이면 true (나간 것이 아니다) */
    fun onStop(now: Long, changingConfigurations: Boolean) {
        if (!changingConfigurations && !authenticating) backgroundedAt = now
    }

    fun onStart(now: Long) {
        val cameBack = backgroundedAt != null
        if (AppLockPolicy.lockOnForeground(isEnabled(), isLoggedIn(), backgroundedAt, now)) _locked.value = true
        backgroundedAt = null
        // 나갔다 돌아왔는데 잠겨 있으면(방금 잠겼든, 잠긴 채로 나갔다 왔든) 확인 창을 다시 띄운다.
        if (cameBack && _locked.value) _promptRequests.value++
    }

    fun unlock() {
        _locked.value = false
    }

    /** 로그아웃·계정 삭제: 잠금을 풀고 기억해 둔 것을 지운다 (다음 사람에게 앞 사람의 잠금이 남지 않게). */
    fun reset() {
        _locked.value = false
        backgroundedAt = null
        authenticating = false
    }
}
