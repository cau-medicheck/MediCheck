package com.cau.medicheck.notification

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * 알림과 관련된 "폰의 상태"를 읽고, 안드로이드 설정 화면을 여는 도우미.
 * 홈의 알림 꺼짐 카드, 알림 센터(S16), 알림 설정(S17), 권한 안내(S18·S34), 알람 예약이 같은 판정을 쓴다.
 */
object NotificationAccess {

    /**
     * 복약 알림이 실제로 화면에 뜰 수 있는가.
     * 앱 알림 전체가 켜져 있고(Android 13 이상은 알림 권한 포함), 지금 설정이 쓰는 채널도 꺼져 있지 않아야 한다.
     * (사용자가 설정 앱에서 그 채널만 끌 수 있기 때문이다.)
     */
    fun notificationsEnabled(context: Context, channelId: String): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannel(channelId)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** 정확한 시각에 알람을 걸 수 있는가. Android 12(API 31)부터 물어봐야 하고, 그 전 버전은 항상 가능. */
    fun exactAlarmsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** 사용자가 이 앱의 배터리 사용을 '제한'으로 두었는가. 그러면 알람이 늦거나 오지 않을 수 있다. */
    fun backgroundRestricted(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.getSystemService(ActivityManager::class.java).isBackgroundRestricted

    /** 설정 앱의 "이 앱의 알림" 화면을 연다. */
    fun openAppNotificationSettings(context: Context) = open(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )

    /** 설정 앱의 "이 채널의 알림" 화면을 연다. (알림 권한은 있는데 채널만 꺼져 있을 때) */
    fun openChannelSettings(context: Context, channelId: String) = open(
        context,
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
    )

    /** 설정 앱의 '알람 및 리마인더' 화면을 연다. (Android 12 이상에서 정확한 알람이 제한됐을 때만 뜻이 있다) */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return openAppDetails(context)
        open(context, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context)))
    }

    /** 설정 앱의 "앱 정보" 화면을 연다. 배터리 사용 제한을 여기서 바꾼다. */
    fun openAppDetails(context: Context) {
        runCatching { context.startActivity(appDetailsIntent(context)) }
    }

    /** 설정 화면을 연다. 폰에 그런 화면이 없으면 앱 정보 화면으로 대신 연다. */
    private fun open(context: Context, intent: Intent) {
        val opened = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        if (opened.isFailure) openAppDetails(context)
    }

    private fun appDetailsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** "package:com.cau.medicheck" 모양의 주소. 설정 앱에게 "이 앱의 화면"이라고 알려 준다. */
    private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)
}
