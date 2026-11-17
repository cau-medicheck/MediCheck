package com.cau.medicheck.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.domain.ReminderChannels
import com.cau.medicheck.notification.NotificationAccess

/**
 * 알림과 관련된 폰의 지금 상태. (S16 안내 상자, S18 배지, S34, 홈의 알림 꺼짐 카드)
 *
 * @param notificationsEnabled 복약 알림이 화면에 뜰 수 있는가 (앱 알림 + 지금 설정의 채널)
 * @param exactAlarmsAllowed 정확한 시각에 알람을 걸 수 있는가
 * @param backgroundRestricted 배터리 사용이 '제한'인가
 * @param reread 상태를 지금 다시 읽게 하는 함수 (권한 창에서 돌아왔을 때)
 */
class NotificationState(
    val notificationsEnabled: Boolean,
    val exactAlarmsAllowed: Boolean,
    val backgroundRestricted: Boolean,
    val reread: () -> Unit
)

private fun appContainer(context: Context) = (context.applicationContext as MediCheckApplication).container

/**
 * 폰의 알림 상태를 읽어 준다. 설정 앱에 다녀오면 값이 바뀔 수 있으므로 화면이 다시 보일 때마다 다시 읽는다.
 * 소리·진동 설정을 바꾸면 보는 채널이 달라지므로 그때도 다시 읽는다.
 */
@Composable
fun rememberNotificationState(): NotificationState {
    val context = LocalContext.current
    val settings by appContainer(context).settingsRepository.current.collectAsStateWithLifecycle()
    val channelId = ReminderChannels.idFor(settings)

    // 이 숫자를 바꾸면 아래 remember가 다시 계산된다 = 상태를 다시 읽는다.
    var version by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        version++
        onPauseOrDispose { }
    }
    return remember(version, channelId) {
        NotificationState(
            notificationsEnabled = NotificationAccess.notificationsEnabled(context, channelId),
            exactAlarmsAllowed = NotificationAccess.exactAlarmsAllowed(context),
            backgroundRestricted = NotificationAccess.backgroundRestricted(context),
            reread = { version++ }
        )
    }
}

/**
 * "지금 복약 알림이 폰에서 꺼져 있는가"를 알려 준다. 홈(S04)이 이 값으로 알림 꺼짐 카드를 보일지 정한다.
 *
 * 홈에 처음 들어왔을 때 알림 권한이 없으면 시스템의 "알림을 허용할까요?" 창을 띄운다.
 * 이 창은 폰에서 딱 한 번만 저절로 띄운다(AppPrefs에 물어봤다고 적어 둔다). 거부해도 설정 앱을 열지 않는다
 * — 앱을 켤 때마다 설정 앱이 열리면 안 되기 때문이다. (Android 13 이상에서만 이 창이 있다.)
 */
@Composable
fun rememberNotificationsOff(): Boolean {
    val context = LocalContext.current
    val appPrefs = remember(context) { appContainer(context).appPrefs }
    val state = rememberNotificationState()

    // 시스템의 권한 요청 창을 띄우는 도구. 사용자가 고르면 결과가 돌아오고, 그때 상태를 다시 읽는다.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { state.reread() }

    LaunchedEffect(Unit) {
        if (!hasPostPermission(context) && !appPrefs.notifPermissionAsked()) {
            appPrefs.setNotifPermissionAsked(true)
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    return !state.notificationsEnabled
}

/**
 * "알림 권한 허용" 버튼이 눌렸을 때 할 일을 돌려준다. (S18 버튼·줄 1, S34 버튼·줄 1이 같이 쓴다)
 *
 *   1. 알림이 이미 켜져 있으면 → 설정 앱의 이 앱 알림 화면
 *   2. Android 13 이상이고 알림 권한이 없으면
 *        - 이미 물어봤고 더 물어볼 수 없는 상태(두 번 거부)면 → 창이 뜨지 않으므로 바로 설정 앱
 *        - 그 밖이면 → 시스템 권한 창. 거부해도 설정 앱을 열지 않는다 (방금 '허용 안함'을 누른 사람에게 들이밀지 않는다)
 *   3. 권한은 있는데 지금 설정의 채널만 꺼져 있으면 → 설정 앱의 그 채널 화면
 *   4. 그 밖 (Android 12 이하에서 알림을 끈 경우) → 설정 앱의 이 앱 알림 화면
 *
 * @param state 지금 상태. 권한 창에서 돌아오면 state.reread()로 다시 읽는다.
 */
@Composable
fun rememberNotificationPermissionAction(state: NotificationState): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val container = remember(context) { appContainer(context) }
    val settings by container.settingsRepository.current.collectAsStateWithLifecycle()
    val channelId = ReminderChannels.idFor(settings)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { state.reread() }

    return {
        when {
            state.notificationsEnabled -> NotificationAccess.openAppNotificationSettings(context)
            !hasPostPermission(context) -> {
                // shouldShowRequestPermissionRationale: 한 번 거부한 뒤에는 true, 두 번 거부하면 다시 false가 된다.
                val canAskAgain = activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
                if (container.appPrefs.notifPermissionAsked() && !canAskAgain) {
                    NotificationAccess.openAppNotificationSettings(context)
                } else {
                    container.appPrefs.setNotifPermissionAsked(true)
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            // 앱 알림은 켜져 있는데 여기까지 왔다면 지금 설정의 채널만 꺼진 것이다.
            context.getSystemService(android.app.NotificationManager::class.java).areNotificationsEnabled() ->
                NotificationAccess.openChannelSettings(context, channelId)
            else -> NotificationAccess.openAppNotificationSettings(context)
        }
    }
}

/** 알림 권한(POST_NOTIFICATIONS)이 있는가. Android 12 이하에는 이 권한이 없어서 항상 있는 것으로 본다. */
private fun hasPostPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
