package com.cau.medicheck.ui.navigation.graph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cau.medicheck.data.local.User
import com.cau.medicheck.ui.navigation.AppNavigator
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.screens.settings.S19SettingsScreen
import com.cau.medicheck.ui.screens.settings.S20PolicyScreen
import com.cau.medicheck.ui.screens.settings.S21ProfileScreen
import com.cau.medicheck.ui.screens.settings.AccessibilityActions
import com.cau.medicheck.ui.screens.settings.OfflineActions
import com.cau.medicheck.ui.screens.settings.PrivacyActions
import com.cau.medicheck.ui.screens.settings.S22HelpScreen
import com.cau.medicheck.ui.screens.settings.S36PrivacyScreen
import com.cau.medicheck.ui.screens.settings.S36PrivacyViewModel
import com.cau.medicheck.ui.screens.settings.S37AccessibilityScreen
import com.cau.medicheck.ui.screens.settings.S37AccessibilityViewModel
import com.cau.medicheck.ui.screens.settings.S38OfflineScreen
import com.cau.medicheck.ui.screens.settings.S38OfflineViewModel
import com.cau.medicheck.ui.screens.settings.S39ErrorStatesScreen
import com.cau.medicheck.ui.screens.settings.S44AccountDeleteScreen

/** 이 파일이 붙여 둔 화면 ID */
val SETTINGS_SCREENS = setOf("S19", "S20", "S21", "S22", "S36", "S37", "S38", "S39", "S44")

/**
 * 설정 화면들이 "계정"에 하는 일을 묶은 것. 로그인 상태는 AuthViewModel이 들고 있어서,
 * 맨 바깥(MediCheckApp)이 이 묶음을 만들어 MainNavHost → 여기까지 내려 준다.
 *
 * @param onLogout 로그아웃 (S19)
 * @param loggingOut 로그아웃 처리 중인지
 * @param onUpdateProfile (이름, 이메일, 성공했을 때 부를 함수) — 프로필 저장 (S21)
 * @param profileSaving 프로필 저장 처리 중인지
 * @param onDeleteAccount (확인 칸에 입력한 글자) — 계정·데이터 삭제 (S44)
 * @param deletingAccount 계정 삭제 처리 중인지
 */
data class SettingsCallbacks(
    val onLogout: () -> Unit,
    val loggingOut: Boolean = false,
    val onUpdateProfile: (String, String, () -> Unit) -> Unit = { _, _, _ -> },
    val profileSaving: Boolean = false,
    val onDeleteAccount: (String) -> Unit = {},
    val deletingAccount: Boolean = false
)

/**
 * 설정 화면들의 주소 등록과 그 사이의 이동. (구현 명세 06)
 *
 *   머리말 ⚙ → S19 설정 허브 ─ 수정 › → S21 프로필 관리 ─ 저장 → S19
 *                 ├ 알림 및 앱    → S17 · S18 · S20 계산 정책 · S22 도움말 ─ 관련 화면 → S39 · S46
 *                 ├ 안전·편의 기능 → S37 · S36 · S38 · S32 · S45 · S23
 *                 └ 로그아웃      → 로그인 화면(S01)
 *
 *   S36 개인정보 센터 → S41 앱 잠금 · S28 백업·복원 · S43 내보내기 · S44 계정·데이터 삭제 ─ 삭제 → 온보딩(S00)
 *   S38 오프라인 상태 → S06 · S05 · S12 · S45        S39 빈 화면·오류 상태 → S08 · S05
 *
 * S20·S21·S22의 "‹ 설정"은 backTo(S19): 지나온 S19가 있으면 거기까지 돌아가고, 없으면 지금 화면을 S19로 바꾼다.
 * S36~S39의 "‹ 이전 화면"은 back(): 바로 앞 화면으로.
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

    // ----- S36 개인정보 센터 -----
    composable(Routes.S36) {
        val vm: S36PrivacyViewModel = viewModel(factory = S36PrivacyViewModel.Factory)
        val settings by vm.settings.collectAsStateWithLifecycle()
        S36PrivacyScreen(
            settings = settings,
            actions = remember(vm, go) {
                PrivacyActions(
                    onBack = go::back,
                    onHideMedName = vm::setHideMedName,
                    onHideStatsPreview = vm::setHideStatsPreview,
                    onOpen = go::open
                )
            }
        )
    }

    // ----- S37 접근성 설정 -----
    composable(Routes.S37) {
        val vm: S37AccessibilityViewModel = viewModel(factory = S37AccessibilityViewModel.Factory)
        val settings by vm.settings.collectAsStateWithLifecycle()
        S37AccessibilityScreen(
            settings = settings,
            actions = remember(vm, go) {
                AccessibilityActions(
                    onBack = go::back,
                    onFontScale = vm::setFontScale,
                    onHighContrast = vm::setHighContrast,
                    onReduceMotion = vm::setReduceMotion
                )
            }
        )
    }

    // ----- S38 오프라인 상태 -----
    composable(Routes.S38) {
        val vm: S38OfflineViewModel = viewModel(factory = S38OfflineViewModel.Factory)
        // 이 화면이 보이는 동안에만 연결 상태를 듣는다.
        val online by vm.isOnline.collectAsStateWithLifecycle()
        S38OfflineScreen(
            online = online,
            actions = remember(go) {
                OfflineActions(
                    onBack = go::back,
                    onOpenMeds = { go.openTab(Routes.S06) },
                    onOpenSchedule = { go.openTab(Routes.S05) },
                    onOpenRecords = { go.open(Routes.S12) },   // 선택 날짜의 기록
                    onOpenVerify = { go.open(Routes.S45) }
                )
            }
        )
    }

    // ----- S44 계정·데이터 삭제 -----
    composable(Routes.S44) {
        // 삭제에 성공하면 로그인한 사용자가 없어져 MediCheckApp이 온보딩(S00)을 그린다. 여기서 따로 이동하지 않는다.
        S44AccountDeleteScreen(deleting = cb.deletingAccount, onDelete = cb.onDeleteAccount, onBack = go::back)
    }

    // ----- S39 빈 화면·오류 상태 -----
    composable(Routes.S39) {
        S39ErrorStatesScreen(
            onBack = go::back,
            onAddMedication = { go.open(Routes.s08New()) },
            onOpenSchedule = { go.openTab(Routes.S05) }
        )
    }
}
