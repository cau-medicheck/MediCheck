package com.cau.medicheck.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** DeleteConfirm 단위 테스트: 계정·데이터 삭제(S44)의 확인 글자 검사 */
class DeleteConfirmTest {

    @Test
    fun `정확히 삭제라고 입력하면 통과한다`() {
        assertTrue(DeleteConfirm.matches("삭제"))
    }

    @Test
    fun `앞뒤 공백은 봐준다`() {
        assertTrue(DeleteConfirm.matches("  삭제 \n"))
    }

    @Test
    fun `빈칸이나 다른 글자는 통과하지 못한다`() {
        assertFalse(DeleteConfirm.matches(""))
        assertFalse(DeleteConfirm.matches("   "))
        assertFalse(DeleteConfirm.matches("삭 제"))
        assertFalse(DeleteConfirm.matches("삭제!"))
        assertFalse(DeleteConfirm.matches("delete"))
    }

    @Test
    fun `영어 화면의 낱말 DELETE도 통과한다`() {
        assertTrue(DeleteConfirm.matches("DELETE"))
        assertTrue(DeleteConfirm.matches(" DELETE "))
        assertFalse(DeleteConfirm.matches("Delete"))
    }
}
