package com.cau.medicheck.data.backup

import com.cau.medicheck.domain.backup.validBackup
import com.cau.medicheck.domain.backup.validEvent
import com.cau.medicheck.domain.backup.validMedication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * BackupJson 단위 테스트: 백업 내용 ↔ JSON 글자.
 * (org.json은 폰 없이는 빈 껍데기라, 테스트용으로 넣은 진짜 구현 org.json:json으로 돈다)
 */
class BackupJsonTest {

    @Test
    fun `내보낸 글자를 다시 읽으면 모든 칸이 같다`() {
        // 칸을 하나라도 빠뜨리면 복원할 때 그 값이 사라진다. 이 테스트가 그것을 잡는다.
        val full = validBackup.copy(
            medications = listOf(
                validMedication.copy(
                    endDate = "2026-12-31", deletedAt = 5L, pausedFrom = "2026-11-01", sourceMemo = "3개월 처방"
                )
            ),
            statusEvents = listOf(validEvent, validEvent.copy(doseRecordId = null, isUndo = true, fromActedAt = 7L))
        )

        assertEquals(full, BackupJson.decode(BackupJson.encode(full)))
    }

    @Test
    fun `맨 앞에 BOM이 있어도 읽는다`() {
        assertEquals(validBackup, BackupJson.decode("﻿" + BackupJson.encode(validBackup)))
    }

    @Test
    fun `모르는 칸은 무시하고 없는 선택 칸은 null이나 빈 목록으로 읽는다`() {
        val text = """
            {"format":"MediCheck-backup","version":1,"somethingNew":true,
             "medications":[{"id":1,"name":"비타민 D","dosage":"1정","startDate":"2026-10-10","extra":1}],
             "schedules":[],"doseRecords":[]}
        """.trimIndent()

        val data = BackupJson.decode(text)!!

        assertNull(data.accountEmail)
        assertNull(data.medications.single().endDate)
        assertNull(data.medications.single().hospital)
        assertEquals(emptyList<Any>(), data.statusEvents)
    }

    @Test
    fun `복용 조건은 그대로 오가고, 조건이 없는 옛 백업은 상관없음으로 읽는다`() {
        val withCondition = validBackup.copy(
            schedules = validBackup.schedules.map { it.copy(doseCondition = "AFTER_MEAL") },
            doseRecords = validBackup.doseRecords.map { it.copy(doseCondition = "AFTER_MEAL") }
        )
        assertEquals(withCondition, BackupJson.decode(BackupJson.encode(withCondition)))

        val old = """
            {"format":"MediCheck-backup","version":1,
             "medications":[{"id":1,"name":"비타민 D","dosage":"1정","startDate":"2026-10-10"}],
             "schedules":[{"id":1,"medicationId":1,"time":"08:00","repeatDays":"MON"}],
             "doseRecords":[{"id":1,"scheduleId":1,"date":"2026-10-10","scheduledTime":"08:00",
                             "medicationName":"비타민 D","dosage":"1정","status":"TAKEN","doseCondition":"모르는 값"}]}
        """.trimIndent()
        val data = BackupJson.decode(old)!!
        assertEquals("NONE", data.schedules.single().doseCondition)
        assertEquals("NONE", data.doseRecords.single().doseCondition)   // 모르는 값도 상관없음으로
    }

    @Test
    fun `JSON이 아니면 읽지 못한다`() {
        assertNull(BackupJson.decode("이것은 백업 파일이 아닙니다"))
        assertNull(BackupJson.decode(""))
        assertNull(BackupJson.decode("[1,2,3]"))
    }

    @Test
    fun `format 표시가 다르면 읽지 못한다`() {
        assertNull(BackupJson.decode("""{"format":"other","version":1,"medications":[],"schedules":[],"doseRecords":[]}"""))
    }

    @Test
    fun `version이 없거나 정수가 아니면 읽지 못한다`() {
        val head = """{"format":"MediCheck-backup","""
        val tail = """"medications":[],"schedules":[],"doseRecords":[]}"""
        assertNull(BackupJson.decode(head + tail))
        assertNull(BackupJson.decode("$head\"version\":\"1\",$tail"))
        assertNull(BackupJson.decode("$head\"version\":1.5,$tail"))
    }

    @Test
    fun `약 목록이 없거나 목록이 아니면 읽지 못한다`() {
        assertNull(BackupJson.decode("""{"format":"MediCheck-backup","version":1,"schedules":[],"doseRecords":[]}"""))
        assertNull(
            BackupJson.decode(
                """{"format":"MediCheck-backup","version":1,"medications":{},"schedules":[],"doseRecords":[]}"""
            )
        )
    }

    @Test
    fun `꼭 있어야 하는 칸이 없으면 읽지 못한다`() {
        val text = """
            {"format":"MediCheck-backup","version":1,
             "medications":[{"id":1,"dosage":"1정","startDate":"2026-10-10"}],"schedules":[],"doseRecords":[]}
        """.trimIndent()
        assertNull(BackupJson.decode(text))
    }

    @Test
    fun `다른 버전 번호는 읽기는 하고 판단은 검사에 맡긴다`() {
        val text = """{"format":"MediCheck-backup","version":2,"medications":[],"schedules":[],"doseRecords":[]}"""
        assertEquals(2, BackupJson.decode(text)?.version)
    }

    @Test
    fun `이력 칸이 있는데 목록이 아니면 읽지 못한다`() {
        // 빈 목록으로 읽어 버리면 복원이 지금 이력을 모두 지운 채 성공해 버린다.
        val head = """{"format":"MediCheck-backup","version":1,"medications":[],"schedules":[],"doseRecords":[],"""
        assertNull(BackupJson.decode("$head\"statusEvents\":{}}"))
        assertNull(BackupJson.decode("$head\"statusEvents\":\"none\"}"))
        assertNull(BackupJson.decode("$head\"statusEvents\":null}"))
    }

    @Test
    fun `들여쓰기 없이 쓴 글자도 같은 내용으로 읽힌다`() {
        val compact = BackupJson.encode(validBackup, pretty = false)

        assertEquals(validBackup, BackupJson.decode(compact))
        assertEquals(false, compact.contains("\n"))
    }
}
