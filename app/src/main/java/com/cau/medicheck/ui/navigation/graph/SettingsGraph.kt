package com.cau.medicheck.ui.navigation.graph

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cau.medicheck.data.local.User
import com.cau.medicheck.ui.navigation.AppNavigator
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.screens.settings.S19SettingsScreen
import com.cau.medicheck.ui.screens.settings.S20PolicyScreen
import com.cau.medicheck.ui.screens.settings.S21ProfileScreen
import com.cau.medicheck.ui.screens.settings.S22HelpScreen

/** 이 파일이 붙여 둔 화면 ID */
val SETTINGS_SCREENS = setOf("S19", "S20", "S21", "S22")

/**
 * 설정 화면들이 "계정"에 하는 일을 묶은 것. 로그인 상태는 AuthViewModel이 들고 있어서,
 * 맨 바깥(MediCheckApp)이 이 묶음을 만들어 MainNavHost → 여기까지 내려 준다.
 *
 * @param onLogout 로그아웃 (S19)
 * @param loggingOut 로그아웃 처리 중인지
 * @param onUpdateProfile (이름, 이메일, 성공했을 때 부를 함수) — 프로필 저장 (S21)
 * @param profileSaving 프로필 저장 처리 중인지
 */
data class SettingsCallbacks(
    val onLogout: () -> Unit,
    val loggingOut: Boolean = false,
    val onUpdateProfile: (String, String, () -> Unit) -> Unit = { _, _, _ -> },
    val profileSaving: Boolean = false
)

/**
 * 설정 화면들의 주소 등록과 그 사이의 이동. (구현 명세 06)
 *
 *   머리말 ⚙ → S19 설정 허브 ─ 수정 › → S21 프로필 관리 ─ 저장 → S19
 *                 ├ 알림 및 앱    → S17 · S18 · S20 계산 정책 · S22 도움말 ─ 관련 화면 → S39 · S46
 *                 ├ 안전·편의 기능 → S37 · S36 · S38 · S32 · S45 · S23
 *                 └ 로그아웃      → 로그인 화면(S01)
 *
 * S20·S21·S22의 "‹ 설정"은 backTo(S19): 지나온 S19가 있으면 거기까지 돌아가고, 없으면 지금 화면을 S19로 바꾼다.
 */
fun NavGraphBuilder.settingsGraph(user: User, go: AppNavigator, cb: SettingsCallbacks) {

    // ----- S19 설정 허브 -----
    composable(Routes.S19) {
        S19SettingsScreen(
            displayName = user.displayName,
            email = user.email,
            onOpen = go::open,
            onLogout = cb.onLogout,
            loggingOut = cb.loggingOut
        )
    }

    // ----- S20 통계 계산 정책 -----
    composable(Routes.S20) {
        S20PolicyScreen(onBack = { go.backTo(Routes.S19) })
    }

    // ----- S21 프로필 관리 -----
    composable(Routes.S21) { entry ->
        S21ProfileScreen(
            displayName = user.displayName,
            email = user.email,
            saving = cb.profileSaving,
            // 저장에 성공하면 설정 허브로 돌아간다. (성공 토스트는 AuthViewModel이 띄운다)
            // 단, 저장하는 사이에 다른 화면으로 옮겨 갔으면 그 화면을 건드리지 않는다.
            // (RESUMED = 이 화면이 지금 맨 위에 보이고 있는 상태)
            onSave = { name, email ->
                cb.onUpdateProfile(name, email) {
                    if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) go.backTo(Routes.S19)
                }
            },
            onBack = { go.backTo(Routes.S19) }
        )
    }

    // ----- S22 도움말·범위 -----
    composable(Routes.S22) {
        S22HelpScreen(onBack = { go.backTo(Routes.S19) }, onOpen = go::open)
    }
}
