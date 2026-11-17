package com.cau.medicheck.ui.navigation

import com.cau.medicheck.domain.DoseKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * 알림을 눌러 들어온 "이 회차를 열어 달라"는 요청을 잠깐 맡아 두는 상자. (딥링크: 알림 → 앱 안의 특정 화면)
 *
 * 흐름: 알림을 누름 → MainActivity가 알림에 실린 값을 읽어 여기에 넣음 → 로그인 뒤 화면 틀(MainNavHost)이
 * 이 상자를 보고 있다가 그 회차의 상세 화면(S10)을 열고 상자를 비운다.
 *
 * 화면(Activity)이 아니라 AppContainer에 하나 두는 이유: 로딩 화면 중에 폰을 돌려 화면이 다시 만들어져도
 * 값이 사라지지 않게 하기 위해서다.
 */
class DeepLinkInbox {
    private val _pending = MutableStateFlow<DoseKey?>(null)

    /** 아직 열지 않은 회차. 없으면 null */
    val pending: StateFlow<DoseKey?> = _pending.asStateFlow()

    fun offer(key: DoseKey) {
        _pending.value = key
    }

    /** 비운다. 회차를 연 뒤, 로그인 전 화면이 보일 때, 로그아웃할 때 부른다. */
    fun clear() {
        _pending.value = null
    }

    companion object {
        /** 알림에 실어 보내는 값의 이름표들 */
        const val EXTRA_OPEN_SCREEN = "open_screen"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_DATE = "date"

        /** open_screen에 넣는 값: 회차 상세 화면 */
        const val SCREEN_DOSE = "S10"

        /**
         * 알림에 실려 온 값으로 회차를 알아낸다. 값이 빠졌거나 날짜 모양이 틀리면 null(= 무시).
         * @param scheduleId 값이 없었으면 -1
         */
        fun parse(openScreen: String?, scheduleId: Long, date: String?): DoseKey? {
            if (openScreen != SCREEN_DOSE || scheduleId <= 0 || date == null) return null
            val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
            return DoseKey(scheduleId, parsed)
        }
    }
}
