package com.cau.medicheck.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cau.medicheck.data.repository.ReminderRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDateTime

/**
 * 복약 알림을 "예약"하는 일을 맡는다.
 *
 * 방식: 알람을 약마다 따로 걸지 않고, **가장 먼저 오는 다음 알림 시각 하나만** 예약한다.
 *   1. reschedule(): DB에서 다음 알림 시각(다음 회차 또는 다음 재알림)을 구해 AlarmManager에 예약한다.
 *   2. 그 시각이 되면 시스템이 ReminderReceiver를 깨운다.
 *   3. ReminderReceiver가 알림을 띄우고, 다시 reschedule()을 불러 그다음 시각을 예약한다.
 *
 * 그래서 약이 바뀔 때(등록·수정·삭제), 설정을 바꿀 때, 로그인·로그아웃할 때, 폰을 다시 켰을 때
 * reschedule()만 부르면 알림이 항상 최신 상태가 된다. 같은 예약표로 덮어쓰므로 중복 예약이 생기지 않는다.
 *
 * AlarmManager: 앱이 꺼져 있어도 정해진 시각에 앱을 깨워 주는 안드로이드 기능.
 * PendingIntent: "그때가 되면 이걸 실행해 달라"고 시스템에 맡겨 두는 실행 예약표.
 *
 * @param clock 앱 시계. 예정 일시("08:00")를 어느 시간대의 08:00으로 볼지 정한다 (S35).
 */
class ReminderScheduler(
    private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val clock: Clock
) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** 여러 곳에서 동시에 불러도 한 번에 하나씩 실행한다 (옛 계산 결과로 덮어쓰지 않게). */
    private val mutex = Mutex()

    /**
     * 다음 알림 시각을 다시 계산해서 알람을 건다. 다음 알림이 없으면 걸려 있던 알람을 취소한다.
     * @param notBefore 방금 울린 알람의 예정 일시. 같은 분을 다시 걸지 않도록 이 시각보다 뒤에서 찾는다.
     */
    suspend fun reschedule(notBefore: LocalDateTime? = null) = mutex.withLock {
        val next = reminderRepository.nextTrigger(notBefore)
        if (next == null) {
            alarmManager.cancel(alarmIntent(triggerAtMillis = 0, triggerLocal = null))
            return@withLock
        }

        val triggerAtMillis = next.atZone(clock.zone).toInstant().toEpochMilli()
        val intent = alarmIntent(triggerAtMillis, next)

        // 같은 예약표(PendingIntent)로 다시 걸면 이전 예약이 새것으로 바뀐다. 알람은 항상 하나만 남는다.
        if (NotificationAccess.exactAlarmsAllowed(context)) {
            // 정확한 시각에, 폰이 절전 상태여도 울린다.
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
        } else {
            // 정확한 알람이 허용되지 않은 폰에서는 몇 분 늦을 수 있는 알람으로 대신한다.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
        }
    }

    /**
     * 알람이 울릴 때 ReminderReceiver에게 전달될 예약표.
     * "몇 시 회차의 알람인지"를 함께 넣어 두어, 받는 쪽이 그 시각의 약을 찾을 수 있게 한다.
     * 예정 일시는 글자("2026-11-17T08:00")로도 넣는다 — 이유는 ReminderTrigger 설명 참고.
     */
    private fun alarmIntent(triggerAtMillis: Long, triggerLocal: LocalDateTime?): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_REMIND)
                .putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, triggerAtMillis)
                .putExtra(ReminderReceiver.EXTRA_TRIGGER_LOCAL, triggerLocal?.toString()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private companion object {
        const val REQUEST_CODE = 1001
    }
}
