package com.cau.medicheck.ui.components

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.cau.medicheck.R

/**
 * 알림 권한을 요청하고, 알림이 꺼져 있으면 안내 카드를 보여 준다.
 *
 *   1. 화면에 처음 들어왔을 때 알림이 꺼져 있으면, 시스템의 "알림을 허용할까요?" 창을 한 번 띄운다.
 *      (Android 13 이상에서만 이 창이 있다. 그 전 버전은 알림이 기본으로 켜져 있다.)
 *   2. 그래도 꺼져 있으면 안내 카드를 보여 준다. "알림 켜기"를 누르면 이 앱의 알림 설정 화면이 열린다.
 *   3. 알림이 켜져 있으면 아무것도 그리지 않는다.
 */
@Composable
fun NotificationPermissionBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 지금 알림이 켜져 있는지. 설정 화면에 다녀오면 바뀔 수 있으므로 화면이 다시 보일 때마다 확인한다.
    var enabled by remember { mutableStateOf(areNotificationsEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        enabled = areNotificationsEnabled(context)
        onPauseOrDispose { }
    }

    // 시스템의 권한 요청 창을 띄우는 도구. 사용자가 고르면 결과(허용 여부)가 돌아온다.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { enabled = areNotificationsEnabled(context) }

    // 이미 한 번 물어봤는지. rememberSaveable이라 화면을 돌려도 다시 묻지 않는다.
    var alreadyAsked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!enabled && !alreadyAsked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            alreadyAsked = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (enabled) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.notifications_off_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(R.string.notifications_off_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(
                onClick = { openNotificationSettings(context) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.action_turn_on_notifications))
            }
        }
    }
}

private fun areNotificationsEnabled(context: Context): Boolean =
    context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

/** 이 앱의 알림 설정 화면(시스템 설정)을 연다. */
private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
