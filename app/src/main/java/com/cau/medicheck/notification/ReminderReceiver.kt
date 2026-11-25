package com.cau.medicheck.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.domain.ReminderTrigger
import com.cau.medicheck.ui.navigation.DeepLinkInbox
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 시스템이 보내는 신호를 받아 알림과 관련된 일을 한다. (AndroidManifest.xml에 등록되어 있다)
 *
 * BroadcastReceiver: 앱이 꺼져 있어도 시스템이 특정 신호를 보내면 깨어나 실행되는 클래스.
 *
 * 받는 신호
 *   - ACTION_REMIND: ReminderScheduler가 걸어 둔 알람이 울림 → 알림을 띄우고 다음 알람을 건다
 *   - 폰을 다시 켬 / 시각·시간대가 바뀜 / 앱이 업데이트됨 / 정확한 알람이 허용됨
 *     → 알람이 사라지거나 어긋나므로 다시 건다
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as MediCheckApplication).container

        // onReceive는 아주 짧게 끝나야 한다. DB를 읽는 동안 시스템이 우리를 끝내 버리지 않도록
        // goAsync()로 "아직 할 일이 남았다"고 알리고, 일이 끝나면 finish()로 알린다.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                // 1. 앱이 방금 깨어났을 수 있으므로 설정부터 읽는다 (알림 켜짐·소리·진동·시간대 정책).
                container.settingsRepository.loadForCurrentUser()

                if (intent.action == ACTION_TAKE) {
                    // 알림의 "복용 완료" 단추: 그 회차를 복용 완료로 기록하고(앱 안 단추와 같은 함수) 알림을 지운다.
                    // 로그인한 사람이 없거나 값이 틀리면 알림만 지운다.
                    val scheduleId = intent.getLongExtra(DeepLinkInbox.EXTRA_SCHEDULE_ID, -1L)
                    val date = intent.getStringExtra(DeepLinkInbox.EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    val userId = container.currentUserId()
                    if (userId != null && scheduleId > 0 && date != null) {
                        container.doseRepository.changeStatus(userId, DoseKey(scheduleId, date), DoseStatus.TAKEN)
                    }
                    if (scheduleId > 0) container.reminderNotifier.cancel(scheduleId)
                } else if (intent.action == ACTION_REMIND) {
                    // 2. 몇 시 회차의 알람이었는지 알아낸다.
                    val at = ReminderTrigger.resolve(
                        localText = intent.getStringExtra(EXTRA_TRIGGER_LOCAL),
                        triggerAtMillis = intent.getLongExtra(EXTRA_TRIGGER_AT, System.currentTimeMillis()),
                        zone = container.clock.zone
                    )
                    // 3. 그 시각의 회차(와 재알림 차례인 회차)마다 알림을 하나씩 띄운다.
                    //    이미 복용 완료·건너뜀으로 기록했거나 알림을 꺼 두었으면 목록이 비어 아무것도 띄우지 않는다.
                    val settings = container.settingsRepository.current.value
                    container.reminderRepository.remindersAt(at)
                        .forEach { container.reminderNotifier.show(it, settings) }
                    // 4. 다음 알람을 건다. 방금 울린 시각보다 뒤에서 찾는다.
                    container.reminderScheduler.reschedule(notBefore = at)
                } else {
                    // 그 밖의 신호: 지금 시각 기준으로 다시 건다.
                    container.reminderScheduler.reschedule()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.cau.medicheck.action.REMIND"

        /** 알림의 "복용 완료" 단추 */
        const val ACTION_TAKE = "com.cau.medicheck.action.TAKE"

        /** 알람이 울리기로 한 순간 (1970년부터 센 밀리초) */
        const val EXTRA_TRIGGER_AT = "trigger_at"

        /** 알람의 예정 일시를 글자로 적은 것 (예: "2026-11-17T08:00") */
        const val EXTRA_TRIGGER_LOCAL = "trigger_local"
    }
}
