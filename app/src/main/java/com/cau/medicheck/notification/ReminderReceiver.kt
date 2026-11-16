package com.cau.medicheck.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cau.medicheck.MediCheckApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * 시스템이 보내는 신호를 받아 알림과 관련된 일을 한다. (AndroidManifest.xml에 등록되어 있다)
 *
 * BroadcastReceiver: 앱이 꺼져 있어도 시스템이 특정 신호를 보내면 깨어나 실행되는 클래스.
 *
 * 받는 신호
 *   - ACTION_REMIND: ReminderScheduler가 걸어 둔 알람이 울림 → 알림을 띄우고 다음 알람을 건다
 *   - 폰을 다시 켬 / 시각·시간대가 바뀜 / 앱이 업데이트됨 → 알람이 사라지거나 어긋나므로 다시 건다
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as MediCheckApplication).container

        // onReceive는 아주 짧게 끝나야 한다. DB를 읽는 동안 시스템이 우리를 끝내 버리지 않도록
        // goAsync()로 "아직 할 일이 남았다"고 알리고, 일이 끝나면 finish()로 알린다.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (intent.action == ACTION_REMIND) {
                    val triggerAtMillis = intent.getLongExtra(EXTRA_TRIGGER_AT, System.currentTimeMillis())
                    val triggerAt = Instant.ofEpochMilli(triggerAtMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()

                    // 그 시각에 먹어야 하는 약마다 알림을 하나씩 띄운다.
                    container.reminderRepository.dueReminders(triggerAt)
                        .forEach { container.reminderNotifier.show(it) }
                }
                // 어떤 신호든 마지막에는 다음 알람을 다시 건다.
                container.reminderScheduler.reschedule()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.cau.medicheck.action.REMIND"
        const val EXTRA_TRIGGER_AT = "trigger_at"
    }
}
