package com.cau.medicheck.ui.navigation

import java.time.LocalDate

/** 요구사항 맵(S45)에서 기능 줄을 눌렀을 때 할 일 */
sealed interface ReqTarget {
    /** 갈 곳이 없다: 그 기능의 화면이 S45 자신뿐이거나 화면 ID가 하나도 없다 → 누를 수 없는 줄로 그린다 */
    data object Stay : ReqTarget

    /** 그 화면을 연다 */
    data class Open(val screenId: String) : ReqTarget

    /** 로그인 전 화면(S00~S03)뿐인 기능 → 이동하지 않고 안내만 띄운다 */
    data object PreLoginOnly : ReqTarget
}

/**
 * 화면 이동 규칙 중 "계산만 하는 부분"을 모아 둔 곳. (구현 명세 00 §3.4, §7.6)
 *
 * NavController(화면을 옮기는 리모컨)는 폰 없이는 시험하기 어렵다. 그래서 "어디로 가야 하나"를
 * 정하는 판단만 순수 함수로 떼어 내 JUnit 단위 테스트로 확인한다.
 */
object NavRules {

    /**
     * 앱을 켰을 때 처음 보여 줄 화면.
     *
     *   로그인 기록 있음                      → S04 홈
     *   로그인 기록 없음 + 온보딩을 본 적 없음 → S00 온보딩
     *   로그인 기록 없음 + 온보딩을 본 적 있음 → S01 로그인
     */
    fun startRoute(isLoggedIn: Boolean, onboardingSeen: Boolean): String = when {
        isLoggedIn -> Routes.S04
        !onboardingSeen -> Routes.S00
        else -> Routes.S01
    }

    /** route에서 화면 ID(앞부분)만 꺼낸다. 예: "S07?medId=5" → "S07" */
    fun screenIdOf(route: String): String = route.substringBefore('?')

    /**
     * 이동할 주소에 값이 다 채워졌는지. "S07?medId={medId}"처럼 '{'가 남아 있으면
     * 값을 넣지 않은 틀(패턴)이라 그 주소로 이동하면 인자를 해석하지 못해 앱이 멈춘다.
     * (AppNavigator.backTo가 이동 전에 확인하는 안전장치)
     */
    fun isFilledRoute(route: String): Boolean = '{' !in route

    /**
     * 그 화면에서 강조할 하단 탭의 첫 화면 ID. 어느 탭에도 속하지 않으면 null. (v3 render()의 강조 규칙)
     *   홈: S04 / 일정: S05·S10 / 내 약: S06~S09 / 기록: S11·S12 / 통계: S13~S15
     */
    fun tabRootOf(route: String?): String? = when (route?.let(::screenIdOf)) {
        "S04" -> "S04"
        "S05", "S10" -> "S05"
        "S06", "S07", "S08", "S09" -> "S06"
        "S11", "S12" -> "S11"
        "S13", "S14", "S15" -> "S13"
        else -> null
    }

    private val SCREEN_ID = Regex("S\\d\\d")

    /**
     * 요구사항 맵(S45)의 기능 줄을 눌렀을 때 어디로 갈지.
     *
     * 목록에서 "지금 갈 수 있는 첫 화면"을 고른다. 갈 수 없는 화면은 두 가지다.
     *   - 로그인 전 화면(S00~S03): 로그인해 있는 동안에는 갈 수 없다
     *   - S45 자신: 이미 이 화면이다
     *
     *   갈 수 있는 화면이 있음                      → 그중 첫 화면을 연다  (예: [S01, S04] → S04, [S45, S37] → S37)
     *   로그인 전 화면뿐                            → PreLoginOnly (안내만)
     *   S45뿐이거나 화면 ID가 없음                  → Stay
     *
     * @param screens 그 기능의 화면 ID 목록 (자산의 'v3화면ID' 칸). 화면 ID 꼴이 아닌 글자는 무시한다
     */
    fun reqTarget(screens: List<String>): ReqTarget {
        val ids = screens.filter { SCREEN_ID.matches(it) && it in Routes.ALL_SCREEN_IDS }
        val reachable = ids.firstOrNull { it !in Routes.PRE_LOGIN && it != Routes.S45 }
        return when {
            reachable != null -> ReqTarget.Open(reachable)
            ids.isNotEmpty() && ids.all { it in Routes.PRE_LOGIN } -> ReqTarget.PreLoginOnly
            else -> ReqTarget.Stay
        }
    }

    /** 주소에 붙어 온 날짜 글자를 날짜로. 형식이 틀리면 null (앱이 멈추지 않게). */
    fun parseDateOrNull(text: String?): LocalDate? =
        runCatching { LocalDate.parse(text) }.getOrNull()
}
