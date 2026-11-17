package com.cau.medicheck.ui.navigation.graph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cau.medicheck.notification.NotificationAccess
import com.cau.medicheck.ui.components.rememberNotificationPermissionAction
import com.cau.medicheck.ui.components.rememberNotificationState
import com.cau.medicheck.ui.navigation.AppNavigator
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.screens.notification.NotificationSettingsActions
import com.cau.medicheck.ui.screens.notification.PermissionActions
import com.cau.medicheck.ui.screens.notification.S16NotificationCenterScreen
import com.cau.medicheck.ui.screens.notification.S16NotificationCenterViewModel
import com.cau.medicheck.ui.screens.notification.S17NotificationSettingsScreen
import com.cau.medicheck.ui.screens.notification.S17NotificationSettingsViewModel
import com.cau.medicheck.ui.screens.notification.S18PermissionsScreen
import com.cau.medicheck.ui.screens.notification.S34NotificationDeniedScreen
import com.cau.medicheck.ui.screens.notification.S35TimezonePolicyScreen
import com.cau.medicheck.ui.screens.notification.S35TimezonePolicyViewModel

/** 이 파일이 붙여 둔 화면 ID */
val NOTIFICATION_SCREENS = setOf("S16", "S17", "S18", "S34", "S35")

/**
 * 알림 화면들의 주소 등록과 그 사이의 이동. (구현 명세 05)
 *
 *   머리말 ♧ → S16 알림 센터 ─ 열기 › → S10 회차 상세
 *                 ├ 알림 설정 → S17 ─ 시간대 변경 정책 → S35
 *                 └ OS 권한 안내 → S18 ─ 알림 권한 거절 시 안내 › → S34
 *   홈의 알림 꺼짐 카드 → S34
 */
fun NavGraphBuilder.notificationGraph(go: AppNavigator) {

    // ----- S16 알림 센터 -----
    composable(Routes.S16) {
        val vm: S16NotificationCenterViewModel = viewModel(factory = S16NotificationCenterViewModel.Factory)
        val state by vm.uiState.collectAsStateWithLifecycle()
        // 화면이 다시 보일 때마다 목록을 다시 계산한다 (회차 상세에서 복용 완료를 누르고 돌아왔을 때 등).
        LifecycleResumeEffect(vm) {
            vm.refresh()
            onPauseOrDispose { }
        }
        S16NotificationCenterScreen(
            state = state,
            notificationsOn = rememberNotificationState().notificationsEnabled,
            onOpenDose = { key -> go.open(Routes.s10(key)) },
            onOpenSettings = { go.open(Routes.S17) },
            onOpenPermissions = { go.open(Routes.S18) }
        )
    }

    // ----- S17 알림 설정 -----
    composable(Routes.S17) {
        val vm: S17NotificationSettingsViewModel = viewModel(factory = S17NotificationSettingsViewModel.Factory)
        val settings by vm.settings.collectAsStateWithLifecycle()
        S17NotificationSettingsScreen(
            settings = settings,
            actions = remember(vm, go) {
                NotificationSettingsActions(
                    onBack = { go.backTo(Routes.S19) },
                    onReminder = vm::setReminderEnabled,
                    onSound = vm::setSoundEnabled,
                    onVibrate = vm::setVibrateEnabled,
                    onRealert = vm::setRealertEnabled,
                    onRealertMinutes = vm::setRealertMinutes,
                    onSendTest = vm::sendTestNotification,
                    onOpenTimezonePolicy = { go.open(Routes.S35) },
                    // S16 ↔ S17을 오가도 화면이 계속 쌓이지 않게, 지나온 S16이 있으면 거기로 돌아간다.
                    onOpenPreview = { go.openOrReturn(Routes.S16) }
                )
            }
        )
    }

    // ----- S18 권한·OS 안내 -----
    composable(Routes.S18) {
        val context = LocalContext.current
        val state = rememberNotificationState()
        val request = rememberNotificationPermissionAction(state)
        S18PermissionsScreen(
            notificationsOn = state.notificationsEnabled,
            exactAlarmsAllowed = state.exactAlarmsAllowed,
            backgroundRestricted = state.backgroundRestricted,
            actions = PermissionActions(
                onBack = { go.backTo(Routes.S17) },
                onRequestNotification = request,
                onOpenExactAlarm = { NotificationAccess.openExactAlarmSettings(context) },
                onOpenAppDetails = { NotificationAccess.openAppDetails(context) },
                onOpenOther = { go.open(Routes.S34) }
            )
        )
    }

    // ----- S34 알림 거절/오류 안내 -----
    composable(Routes.S34) {
        val context = LocalContext.current
        val state = rememberNotificationState()
        val request = rememberNotificationPermissionAction(state)
        S34NotificationDeniedScreen(
            notificationsOn = state.notificationsEnabled,
            exactAlarmNeedsCheck = !state.exactAlarmsAllowed,
            actions = PermissionActions(
                onBack = go::back,   // 들어온 화면으로 (홈 카드 또는 S18)
                onRequestNotification = request,
                onOpenExactAlarm = { NotificationAccess.openExactAlarmSettings(context) },
                onOpenAppDetails = { NotificationAccess.openAppDetails(context) },
                onOpenOther = { go.openTab(Routes.S05) }
            )
        )
    }

    // ----- S35 시간대 변경 정책 -----
    composable(Routes.S35) {
        val vm: S35TimezonePolicyViewModel = viewModel(factory = S35TimezonePolicyViewModel.Factory)
        val state by vm.uiState.collectAsStateWithLifecycle()
        S35TimezonePolicyScreen(
            state = state,
            onBack = go::back,
            onSelect = vm::select,
            onSave = vm::save
        )
    }
}
