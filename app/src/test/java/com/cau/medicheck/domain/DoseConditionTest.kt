package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** 복용 조건 글자 ↔ 값 테스트 (DB·백업에 이름 글자로 저장한다) */
class DoseConditionTest {

    @Test
    fun `저장된 이름 글자를 그 값으로 읽는다`() {
        DoseCondition.entries.forEach { assertEquals(it, DoseCondition.parse(it.name)) }
    }

    @Test
    fun `없거나 모르는 글자는 상관없음이다`() {
        assertEquals(DoseCondition.NONE, DoseCondition.parse(null))
        assertEquals(DoseCondition.NONE, DoseCondition.parse(""))
        assertEquals(DoseCondition.NONE, DoseCondition.parse("after_meal"))   // 대소문자도 정확히 맞아야 한다
    }
}
