package com.cau.medicheck.ui.navigation

import java.time.LocalDate

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

    /** 주소에 붙어 온 날짜 글자를 날짜로. 형식이 틀리면 null (앱이 멈추지 않게). */
    fun parseDateOrNull(text: String?): LocalDate? =
        runCatching { LocalDate.parse(text) }.getOrNull()
}
