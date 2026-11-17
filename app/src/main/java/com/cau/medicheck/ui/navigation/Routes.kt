package com.cau.medicheck.ui.navigation

import androidx.annotation.StringRes
import com.cau.medicheck.R

/**
 * 화면마다 붙이는 주소(route). (구현 명세 00 §2.1)
 *
 * 주소 = 화면 ID 그대로("S01"). 프로토타입의 화면 번호로 바로 찾을 수 있게 하기 위해서다.
 * 1단계에서는 로그인 전 화면 4개(S00~S03)만 이 주소를 쓴다.
 * 로그인 뒤 화면은 2단계에서 S04~S46 주소로 바뀐다(지금은 MainNavHost의 MainRoutes).
 */
object Routes {
    const val S00 = "S00"
    const val S01 = "S01"
    const val S02 = "S02"
    const val S03 = "S03"
    const val S04 = "S04"

    /** 로그인 전 틀(AuthNavHost)에 들어가는 화면들 */
    val PRE_LOGIN = setOf(S00, S01, S02, S03)

    /** 47개 화면 ID 전부 (S00 ~ S46). 머리말 제목을 고를 때와 테스트에서 쓴다. */
    val ALL_SCREEN_IDS: List<String> = (0..46).map { "S%02d".format(it) }

    /**
     * 화면 ID → 머리말 제목 문구 번호. 제목은 v3 프로토타입의 화면 이름(V 목록의 첫 칸) 그대로다.
     * route에 "?medId=…" 같은 꼬리가 붙어 있어도 앞 3글자(화면 ID)만 본다.
     */
    @StringRes
    fun titleRes(route: String): Int = when (NavRules.screenIdOf(route)) {
        "S00" -> R.string.screen_title_s00
        "S01" -> R.string.screen_title_s01
        "S02" -> R.string.screen_title_s02
        "S03" -> R.string.screen_title_s03
        "S04" -> R.string.screen_title_s04
        "S05" -> R.string.screen_title_s05
        "S06" -> R.string.screen_title_s06
        "S07" -> R.string.screen_title_s07
        "S08" -> R.string.screen_title_s08
        "S09" -> R.string.screen_title_s09
        "S10" -> R.string.screen_title_s10
        "S11" -> R.string.screen_title_s11
        "S12" -> R.string.screen_title_s12
        "S13" -> R.string.screen_title_s13
        "S14" -> R.string.screen_title_s14
        "S15" -> R.string.screen_title_s15
        "S16" -> R.string.screen_title_s16
        "S17" -> R.string.screen_title_s17
        "S18" -> R.string.screen_title_s18
        "S19" -> R.string.screen_title_s19
        "S20" -> R.string.screen_title_s20
        "S21" -> R.string.screen_title_s21
        "S22" -> R.string.screen_title_s22
        "S23" -> R.string.screen_title_s23
        "S24" -> R.string.screen_title_s24
        "S25" -> R.string.screen_title_s25
        "S26" -> R.string.screen_title_s26
        "S27" -> R.string.screen_title_s27
        "S28" -> R.string.screen_title_s28
        "S29" -> R.string.screen_title_s29
        "S30" -> R.string.screen_title_s30
        "S31" -> R.string.screen_title_s31
        "S32" -> R.string.screen_title_s32
        "S33" -> R.string.screen_title_s33
        "S34" -> R.string.screen_title_s34
        "S35" -> R.string.screen_title_s35
        "S36" -> R.string.screen_title_s36
        "S37" -> R.string.screen_title_s37
        "S38" -> R.string.screen_title_s38
        "S39" -> R.string.screen_title_s39
        "S40" -> R.string.screen_title_s40
        "S41" -> R.string.screen_title_s41
        "S42" -> R.string.screen_title_s42
        "S43" -> R.string.screen_title_s43
        "S44" -> R.string.screen_title_s44
        "S45" -> R.string.screen_title_s45
        "S46" -> R.string.screen_title_s46
        else -> R.string.app_name   // 모르는 주소면 앱 이름을 보인다 (안전장치)
    }
}
