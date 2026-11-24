package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** 약 모양·색 글자 ↔ 값 테스트 (DB·백업에 이름 글자로 저장한다) */
class MedicationLookTest {

    @Test
    fun `저장된 이름 글자를 그 값으로 읽는다`() {
        MedicationShape.entries.forEach { assertEquals(it, MedicationShape.parse(it.name)) }
        MedicationColor.entries.forEach { assertEquals(it, MedicationColor.parse(it.name)) }
    }

    @Test
    fun `없거나 모르는 글자는 알약과 청록이다`() {
        assertEquals(MedicationShape.TABLET, MedicationShape.parse(null))
        assertEquals(MedicationShape.TABLET, MedicationShape.parse("capsule"))
        assertEquals(MedicationColor.TEAL, MedicationColor.parse(null))
        assertEquals(MedicationColor.TEAL, MedicationColor.parse("빨강"))
    }
}
