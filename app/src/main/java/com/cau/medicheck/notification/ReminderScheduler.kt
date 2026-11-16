package com.cau.medicheck.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.cau.medicheck.data.repository.ReminderRepository
import java.time.ZoneId

/**
 * 복약 알림을 "예약"하는 일을 맡는다.
 *
 * 방식: 알람을 약마다 따로 걸지 않고, **가장 먼저 오는 다음 복용 시각 하나만** 예약한다.
 *   1. reschedule(): DB에서 다음 복용 시각을 구해 AlarmManager에 예약한다.
 *   2. 그 시각이 되면 시스템이 ReminderReceiver를 깨운다.
 *   3. ReminderReceiver가 알림을 띄우고, 다시 reschedule()을 불러 그다음 시각을 예약한다.
 *
 * 그래서 약이 바뀔 때(등록·수정·삭제), 로그인·로그아웃할 때, 폰을 다시 켰을 때
 * reschedule()만 부르면 알림이 항상 최신 상태가 된다.
 *
 * AlarmManager: 앱이 꺼져 있어도 정해진 시각에 앱을 깨워 주는 안드로이드 기능.
 * PendingIntent: "그때가 되면 이걸 실행해 달라"고 시스템에 맡겨 두는 실행 예약표.
 */
class ReminderScheduler(
    private val context: Context,
    private val reminderRepository: ReminderRepository
) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** 다음 복용 시각을 다시 계산해서 알람을 건다. 다음 복용이 없으면 걸려 있던 알람을 취소한다. */
    suspend fun reschedule() {
        val next = reminderRepository.nextTrigger()
        if (next == null) {
            alarmManager.cancel(alarmIntent(triggerAtMillis = 0))
            return
        }

        val triggerAtMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = alarmIntent(triggerAtMillis)

        // 같은 예약표(PendingIntent)로 다시 걸면 이전 예약이 새것으로 바뀐다. 알람은 항상 하나만 남는다.
        if (canUseExactAlarms()) {
            // 정확한 시각에, 폰이 절전 상태여도 울린다.
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
        } else {
            // 정확한 알람이 허용되지 않은 폰에서는 몇 분 늦을 수 있는 알람으로 대신한다.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
        }
    }

    /** Android 12(API 31)부터는 정확한 알람을 쓸 수 있는지 물어봐야 한다. 그 전 버전은 항상 가능. */
    private fun canUseExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * 알람이 울릴 때 ReminderReceiver에게 전달될 예약표.
     * "몇 시에 울리기로 했는지"를 함께 넣어 두어, 받는 쪽이 그 시각의 약을 찾을 수 있게 한다.
     */
    private fun alarmIntent(triggerAtMillis: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_REMIND)
                .putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, triggerAtMillis),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private companion object {
        const val REQUEST_CODE = 1001
    }
}
