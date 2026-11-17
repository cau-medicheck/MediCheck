package com.cau.medicheck.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 접근성 설정 세 가지 (DB_DESIGN 부록 A-2, 00 명세 6.7).
 * 앱 틀은 이 값 하나만 지켜보고 글자 크기·대비·움직임에 적용한다(적용은 6단계).
 *
 * @param fontScalePercent 글자 배율(%). 100·125·150·200만 쓴다.
 */
data class A11ySettings(
    val fontScalePercent: Int = DEFAULT_FONT_SCALE,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false
) {
    companion object {
        const val DEFAULT_FONT_SCALE = 100
        val FONT_SCALES = listOf(100, 125, 150, 200)

        /** 저장된 값이 허용된 4가지가 아니면(예: 손으로 고친 값) 100으로 바로잡는다. */
        fun normalizeFontScale(percent: Int): Int =
            if (percent in FONT_SCALES) percent else DEFAULT_FONT_SCALE
    }
}

/**
 * 폰(기기) 단위 설정. 계정과 상관없고, 로그인 전 화면(S00~S03)에서도 읽어야 하는 값들이다.
 * (계정별 설정은 DB의 user_settings 표, 로그인한 사람은 SessionStore — 세 곳으로 나눈 이유는 DB_DESIGN 부록 A)
 *
 * 2026-11-17 결정 2: 접근성 설정은 계정이 아니라 폰 단위로 둔다
 * (글씨를 크게 해 둔 사람이 로그아웃하자마자 작은 글씨를 보지 않게).
 *
 * interface로 나눈 이유: 단위 테스트에서 폰 없이 가짜로 바꿔 끼우기 위해서다 (SessionStore와 같은 방식).
 */
interface AppPrefs {
    /** 온보딩(S00)을 한 번이라도 봤는지. 시작 화면을 S00/S01 중 고르는 데 쓴다. */
    fun onboardingSeen(): Boolean
    fun setOnboardingSeen(seen: Boolean)

    /** 접근성 설정. 바뀌면 새 값이 흘러나온다(StateFlow). */
    val a11y: StateFlow<A11ySettings>
    fun setA11y(settings: A11ySettings)

    /** 알림 권한 창을 한 번이라도 띄웠는지. "영구 거부" 판정에 쓴다(5단계). */
    fun notifPermissionAsked(): Boolean
    fun setNotifPermissionAsked(asked: Boolean)

    /** 모든 기기 설정을 처음 값으로. (계정·데이터 삭제 S44 — 6단계) */
    fun clearAll()
}

/**
 * 실제 앱에서 쓰는 구현. SharedPreferences 파일 "app_prefs"에 적는다.
 *
 * commit(): 디스크에 다 쓴 뒤 돌아온다. apply()는 나중에 쓰므로, 저장 직후 앱이 죽으면 값이 사라질 수 있다.
 * 값 하나라서 금방 끝난다.
 */
class SharedPrefsAppPrefs(context: Context) : AppPrefs {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _a11y = MutableStateFlow(readA11y())
    override val a11y: StateFlow<A11ySettings> = _a11y.asStateFlow()

    override fun onboardingSeen(): Boolean = prefs.getBoolean(KEY_ONBOARDING_SEEN, false)

    override fun setOnboardingSeen(seen: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, seen).commit()
    }

    override fun setA11y(settings: A11ySettings) {
        val clean = settings.copy(fontScalePercent = A11ySettings.normalizeFontScale(settings.fontScalePercent))
        prefs.edit()
            .putInt(KEY_FONT_SCALE, clean.fontScalePercent)
            .putBoolean(KEY_HIGH_CONTRAST, clean.highContrast)
            .putBoolean(KEY_REDUCE_MOTION, clean.reduceMotion)
            .commit()
        _a11y.value = clean
    }

    override fun notifPermissionAsked(): Boolean = prefs.getBoolean(KEY_NOTIF_PERMISSION_ASKED, false)

    override fun setNotifPermissionAsked(asked: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_PERMISSION_ASKED, asked).commit()
    }

    override fun clearAll() {
        prefs.edit().clear().commit()
        _a11y.value = A11ySettings()
    }

    private fun readA11y() = A11ySettings(
        fontScalePercent = A11ySettings.normalizeFontScale(
            prefs.getInt(KEY_FONT_SCALE, A11ySettings.DEFAULT_FONT_SCALE)
        ),
        highContrast = prefs.getBoolean(KEY_HIGH_CONTRAST, false),
        reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, false)
    )

    /** 키 이름은 DB_DESIGN 부록 A-2가 정한 그대로다. 바꾸면 이미 저장된 값을 못 읽는다. */
    companion object {
        const val PREFS_NAME = "app_prefs"
        const val KEY_ONBOARDING_SEEN = "onboarding_seen"
        const val KEY_FONT_SCALE = "a11y_font_scale_percent"
        const val KEY_HIGH_CONTRAST = "a11y_high_contrast"
        const val KEY_REDUCE_MOTION = "a11y_reduce_motion"
        const val KEY_NOTIF_PERMISSION_ASKED = "notif_permission_asked"
    }
}
