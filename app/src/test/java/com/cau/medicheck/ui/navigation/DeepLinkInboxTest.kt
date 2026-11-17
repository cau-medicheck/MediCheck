package com.cau.medicheck.ui.navigation

import com.cau.medicheck.domain.DoseKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** 알림에 실려 온 값으로 "어느 회차를 열지" 알아내는 테스트 */
class DeepLinkInboxTest {

    @Test
    fun `알림에 실린 규칙 번호와 날짜로 회차를 알아낸다`() {
        assertEquals(
            DoseKey(3, LocalDate.of(2026, 11, 17)),
            DeepLinkInbox.parse("S10", 3, "2026-11-17")
        )
    }

    @Test
    fun `값이 빠졌거나 틀리면 무시한다`() {
        assertNull(DeepLinkInbox.parse(null, 3, "2026-11-17"))          // 테스트 알림처럼 값이 없는 경우
        assertNull(DeepLinkInbox.parse("S99", 3, "2026-11-17"))
        assertNull(DeepLinkInbox.parse("S10", -1, "2026-11-17"))
        assertNull(DeepLinkInbox.parse("S10", 3, null))
        assertNull(DeepLinkInbox.parse("S10", 3, "11월 17일"))
    }

    @Test
    fun `받아 둔 회차는 비우면 사라진다`() {
        val inbox = DeepLinkInbox()
        val key = DoseKey(3, LocalDate.of(2026, 11, 17))

        inbox.offer(key)
        assertEquals(key, inbox.pending.value)

        inbox.clear()
        assertNull(inbox.pending.value)
    }
}
