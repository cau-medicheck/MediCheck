package com.cau.medicheck.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cau.medicheck.MainActivity
import com.cau.medicheck.R
import com.cau.medicheck.domain.DueReminder
import com.cau.medicheck.domain.ReminderChannels
import com.cau.medicheck.domain.UserSettings
import com.cau.medicheck.ui.navigation.DeepLinkInbox
import java.time.format.DateTimeFormatter

/**
 * 복약 알림을 실제로 화면(알림 창)에 띄우는 일을 맡는다.
 *
 * 채널(channel): 안드로이드는 알림을 종류별로 묶어 관리한다. 한 번 만든 채널의 소리·진동은 앱이 바꿀 수 없어서
 * (사용자는 설정 앱에서 바꿀 수 있다), 소리·진동 조합 4가지마다 채널을 하나씩 만들어 두고
 * 알림을 띄울 때 지금 설정에 맞는 채널을 고른다.
 */
class ReminderNotifier(private val context: Context) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    /**
     * 채널 4개를 만들고, 예전 채널 하나를 지운다. 이미 있으면 아무 일도 일어나지 않으므로 여러 번 불러도 된다.
     */
    fun ensureChannels() {
        notificationManager.deleteNotificationChannel(ReminderChannels.LEGACY)
        listOf(
            channel(ReminderChannels.SOUND_VIBRATE, R.string.reminder_channel_sound_vibrate, sound = true, vibrate = true),
            channel(ReminderChannels.SOUND, R.string.reminder_channel_sound, sound = true, vibrate = false),
            channel(ReminderChannels.VIBRATE, R.string.reminder_channel_vibrate, sound = false, vibrate = true),
            channel(ReminderChannels.SILENT, R.string.reminder_channel_silent, sound = false, vibrate = false)
        ).forEach(notificationManager::createNotificationChannel)
    }

    private fun channel(id: String, nameRes: Int, sound: Boolean, vibrate: Boolean) =
        // IMPORTANCE_HIGH: 화면 위쪽에 잠깐 내려오는 알림
        NotificationChannel(id, context.getString(nameRes), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.reminder_channel_description)
            // 소리를 따로 정하지 않으면 폰의 기본 알림음이 난다. 끄려면 null을 넣는다.
            if (!sound) setSound(null, null)
            enableVibration(vibrate)
            if (vibrate) vibrationPattern = VIBRATION_PATTERN
        }

    /**
     * 회차 하나의 복약 알림을 띄운다. 누르면 그 회차의 상세 화면(S10)이 열린다.
     * 사용자가 알림 권한을 주지 않았다면 시스템이 조용히 무시한다 (오류가 나지 않는다).
     */
    fun show(due: DueReminder, settings: UserSettings) {
        ensureChannels()
        val dose = due.dose
        val time = dose.time.format(TIME_FORMAT)

        // 약 이름 숨김이 켜져 있으면 알림 글에서 약 이름과 복용량을 뺀다 (잠금 화면에서도 보이지 않게).
        val title =
            if (settings.hideMedName) context.getString(R.string.reminder_title_hidden)
            else context.getString(R.string.reminder_title, dose.medicationName)
        val text = when {
            settings.hideMedName && due.isRealert -> context.getString(R.string.reminder_text_hidden_realert, time)
            settings.hideMedName -> context.getString(R.string.reminder_text_hidden, time)
            due.isRealert -> context.getString(R.string.reminder_text_realert, time, dose.dosage)
            else -> context.getString(R.string.reminder_text, time, dose.dosage)
        }

        // 알림 번호 = 규칙 id. 같은 규칙의 알림(재알림 포함)은 쌓이지 않고 새것으로 바뀐다.
        val id = dose.scheduleId.toInt()
        val open = Intent(context, MainActivity::class.java)
            .addFlags(OPEN_FLAGS)
            .putExtra(DeepLinkInbox.EXTRA_OPEN_SCREEN, DeepLinkInbox.SCREEN_DOSE)
            .putExtra(DeepLinkInbox.EXTRA_SCHEDULE_ID, dose.scheduleId)
            .putExtra(DeepLinkInbox.EXTRA_DATE, dose.date.toString())

        notificationManager.notify(id, build(ReminderChannels.idFor(settings), title, text, pendingIntent(id, open)))
    }

    /**
     * 테스트 알림을 지금 설정(소리·진동)의 채널로 바로 띄운다. (S17 "테스트 알림 보내기")
     * 알람을 걸지 않고 DB에도 쓰지 않는다. 누르면 앱만 열린다.
     */
    fun showTest(settings: UserSettings) {
        ensureChannels()
        val open = Intent(context, MainActivity::class.java).addFlags(OPEN_FLAGS)
        notificationManager.notify(
            TEST_NOTIFICATION_ID,
            build(
                ReminderChannels.idFor(settings),
                context.getString(R.string.reminder_test_title),
                context.getString(R.string.reminder_test_text),
                pendingIntent(TEST_NOTIFICATION_ID, open)
            )
        )
    }

    /**
     * 알림을 누르면 실행될 예약표.
     * requestCode(요청 번호)를 알림마다 다르게 준다. 모두 같으면 같은 시각에 뜬 알림 두 개가 같은 예약표를 쓰게 되어,
     * 앞의 알림을 눌러도 뒤의 회차가 열린다.
     */
    private fun pendingIntent(requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun build(channelId: String, title: String, text: String, open: PendingIntent): Notification =
        Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)   // 누르면 알림이 사라진다
            .build()

    /** 떠 있는 복약 알림을 모두 지운다. (로그아웃할 때) */
    fun cancelAll() {
        notificationManager.cancelAll()
    }

    private companion object {
        /** 테스트 알림의 번호. 규칙 id는 1부터라 겹치지 않는다. */
        const val TEST_NOTIFICATION_ID = 0

        /** 진동 모양: 0.5초 울리고 0.25초 쉬고 0.5초 (밀리초) */
        val VIBRATION_PATTERN = longArrayOf(0, 500, 250, 500)

        /**
         * 앱이 이미 떠 있으면 화면(Activity)을 새로 만들지 않고 지금 것에 알림 값을 넘긴다(onNewIntent).
         */
        const val OPEN_FLAGS =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
