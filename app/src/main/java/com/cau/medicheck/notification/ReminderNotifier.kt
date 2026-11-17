package com.cau.medicheck.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cau.medicheck.MainActivity
import com.cau.medicheck.R
import com.cau.medicheck.domain.ReminderRule
import java.time.format.DateTimeFormatter

/**
 * 복약 알림을 실제로 화면(알림 창)에 띄우는 일을 맡는다.
 *
 * 채널(channel): 안드로이드는 알림을 종류별로 묶어 관리한다. 사용자는 설정에서
 * 채널마다 소리·진동을 바꾸거나 끌 수 있다. 이 앱은 "복약 알림" 채널 하나만 쓴다.
 */
class ReminderNotifier(private val context: Context) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    /** 채널을 만든다. 이미 있으면 아무 일도 일어나지 않으므로 여러 번 불러도 된다. */
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH   // 화면 위쪽에 잠깐 내려오고 소리가 난다
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 약 하나의 복약 알림을 띄운다.
     * 사용자가 알림 권한을 주지 않았다면 시스템이 조용히 무시한다 (오류가 나지 않는다).
     */
    fun show(rule: ReminderRule) {
        ensureChannel()

        // 알림을 누르면 앱이 열린다. 앱의 첫 화면이 "오늘" 화면이라 바로 복용 기록을 남길 수 있다.
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title, rule.medicationName))
            .setContentText(
                context.getString(R.string.reminder_text, rule.time.format(TIME_FORMAT), rule.dosage)
            )
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)   // 누르면 알림이 사라진다
            .build()

        // 알림마다 번호가 필요하다. 규칙의 id를 쓰면 같은 규칙의 알림이 쌓이지 않고 새것으로 바뀐다.
        notificationManager.notify(rule.scheduleId.toInt(), notification)
    }

    /** 떠 있는 복약 알림을 모두 지운다. (로그아웃할 때) */
    fun cancelAll() {
        notificationManager.cancelAll()
    }

    companion object {
        /** 복약 알림 채널의 이름표. 홈의 "알림 꺼짐" 판정도 이 채널을 본다. */
        const val CHANNEL_ID = "medication_reminders"
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
