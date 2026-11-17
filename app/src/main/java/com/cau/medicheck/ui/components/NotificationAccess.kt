package com.cau.medicheck.ui.components

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.notification.ReminderNotifier

/**
 * "지금 복약 알림이 폰에서 꺼져 있는가"를 알려 준다. 홈(S04)이 이 값으로 알림 꺼짐 카드를 보일지 정한다.
 *
 *   1. 홈에 처음 들어왔을 때 알림이 꺼져 있으면, 시스템의 "알림을 허용할까요?" 창을 띄운다.
 *      이 창은 폰에서 딱 한 번만 띄운다(AppPrefs에 물어봤다고 적어 둔다). 매번 뜨면 귀찮기 때문이다.
 *      (Android 13 이상에서만 이 창이 있다. 그 전 버전은 알림이 기본으로 켜져 있다.)
 *   2. 설정 화면에 다녀오면 값이 바뀔 수 있으므로, 화면이 다시 보일 때마다 다시 확인한다.
 *
 * (5단계에서 이 판정을 알림 쪽 코드로 옮기고, "앱 안의 알림 설정이 켜져 있을 때만" 조건을 더한다.)
 */
@Composable
fun rememberNotificationsOff(): Boolean {
    val context = LocalContext.current
    val appPrefs = remember(context) { (context.applicationContext as MediCheckApplication).container.appPrefs }

    var off by remember { mutableStateOf(notificationsOff(context)) }
    LifecycleResumeEffect(Unit) {
        off = notificationsOff(context)
        onPauseOrDispose { }
    }

    // 시스템의 권한 요청 창을 띄우는 도구. 사용자가 고르면 결과가 돌아오고, 그때 다시 확인한다.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { off = notificationsOff(context) }

    LaunchedEffect(Unit) {
        if (off && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !appPrefs.notifPermissionAsked()) {
            appPrefs.setNotifPermissionAsked(true)
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    return off
}

/** 앱 알림 전체가 꺼져 있거나, 복약 알림 채널만 꺼져 있으면 true */
private fun notificationsOff(context: Context): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java)
    if (!manager.areNotificationsEnabled()) return true
    val channel = manager.getNotificationChannel(ReminderNotifier.CHANNEL_ID)
    return channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE
}
