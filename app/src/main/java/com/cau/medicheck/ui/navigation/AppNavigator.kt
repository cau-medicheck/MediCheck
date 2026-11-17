package com.cau.medicheck.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * 화면 이동 도우미. (구현 명세 00 §3.2)
 *
 * 프로토타입은 "화면을 바꾼다"(setView) 한 가지뿐이지만, 앱에는 뒤로 가기 기록(back stack: 지나온 화면을
 * 쌓아 둔 목록)이 있다. 그래서 이동마다 "기록에 쌓을지, 바꿔 끼울지, 되돌아갈지"를 정해야 한다.
 * 화면 코드가 NavController를 직접 만지지 않고 이 이름 붙은 도우미만 부르게 해서 규칙을 한곳에 모은다.
 *
 * 1단계(로그인 전)에서 쓰는 것: open, backTo, replace. 나머지는 2단계부터 쓴다.
 */
class AppNavigator(private val nav: NavHostController) {

    /** 앞으로 가기(지금 화면 위에 쌓기). 빠르게 두 번 눌러도 같은 화면이 하나만 쌓인다(launchSingleTop). */
    fun open(route: String) {
        nav.navigate(route) { launchSingleTop = true }
    }

    /** 탭 첫 화면으로: 맨 아래 시작 화면만 남기고 걷어 낸 뒤 그 화면을 연다. (로그인 뒤 틀 전용) */
    fun openTab(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { inclusive = false }
            launchSingleTop = true
        }
    }

    /** 바로 앞 화면으로 */
    fun back() {
        nav.popBackStack()
    }

    /**
     * 그 화면이 기록에 있으면 거기까지 돌아간다. 없으면 지금 화면을 그 화면으로 바꾼다.
     *
     * 그냥 back()을 쓰지 않는 이유: 화면이 바뀌는 사이 버튼을 두 번 누르면 두 번째 back()이
     * S01까지 빼 버려 빈 화면이 된다. popBackStack(S01, inclusive = false)는 S01을 남기므로 안전하다.
     *
     * @param pattern 돌아갈 화면의 route 상수 (예: Routes.S01)
     * @param route 기록에 없을 때 새로 열 주소 (인자가 있는 화면이면 값을 채운 주소)
     */
    fun backTo(pattern: String, route: String = pattern) {
        if (nav.popBackStack(pattern, inclusive = false)) return
        if (!NavRules.isFilledRoute(route)) { back(); return }
        replace(route)
    }

    /**
     * 지금 화면(또는 popUpTo로 지정한 화면까지)을 기록에서 빼고 새 화면을 연다.
     * 예: S00 → S01. 그러면 S01에서 뒤로를 눌렀을 때 S00으로 돌아가지 않고 앱이 홈 화면으로 나간다.
     */
    fun replace(route: String, popUpTo: String = nav.currentDestination?.route ?: route) {
        nav.navigate(route) {
            popUpTo(popUpTo) { inclusive = true }
            launchSingleTop = true
        }
    }

    /** 기록에 있으면 거기까지 돌아가고, 없으면 open (머리말 ♧·⚙ 버튼용, 2단계) */
    fun openOrReturn(pattern: String) {
        if (!nav.popBackStack(pattern, inclusive = false)) open(pattern)
    }
}
