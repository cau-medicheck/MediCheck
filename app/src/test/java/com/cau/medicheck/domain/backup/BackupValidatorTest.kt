package com.cau.medicheck.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** BackupValidator 단위 테스트: 백업 파일의 내용 검사 (DB_DESIGN 부록 B) */
class BackupValidatorTest {

    private val today = LocalDate.of(2026, 11, 17)

    private fun check(data: BackupData) = BackupValidator.check(data, today)
    private fun problemOf(data: BackupData) = (check(data) as BackupCheck.Invalid).problem

    // ---------- 통과 ----------

    @Test
    fun `올바른 최소 백업은 통과한다`() {
        assertEquals(BackupCheck.Ok(validBackup), check(validBackup))
    }

    @Test
    fun `약이 하나도 없는 백업도 형식만 맞으면 통과한다`() {
        val empty = validBackup.copy(
            medications = emptyList(), schedules = emptyList(), doseRecords = emptyList(), statusEvents = emptyList()
        )
        assertTrue(check(empty) is BackupCheck.Ok)
    }

    @Test
    fun `이력이 없어도 통과한다`() {
        assertTrue(check(validBackup.copy(statusEvents = emptyList())) is BackupCheck.Ok)
    }

    @Test
    fun `내일 날짜의 기록까지는 통과한다`() {
        val tomorrow = validBackup.copy(doseRecords = listOf(validRecord.copy(date = "2026-11-18")))
        assertTrue(check(tomorrow.copy(statusEvents = emptyList())) is BackupCheck.Ok)
    }

    // ---------- 버전 ----------

    @Test
    fun `버전이 1이 아니면 실패한다`() {
        assertEquals(BackupProblem.UNSUPPORTED_VERSION, problemOf(validBackup.copy(version = 2)))
    }

    // ---------- 약 ----------

    @Test
    fun `약 이름이 비어 있으면 실패한다`() {
        val data = validBackup.copy(medications = listOf(validMedication.copy(name = "  ")))
        assertEquals(BackupProblem.BAD_MEDICATION, problemOf(data))
    }

    @Test
    fun `종료일이 시작일보다 앞이면 실패한다`() {
        val data = validBackup.copy(medications = listOf(validMedication.copy(endDate = "2026-10-09")))
        assertEquals(BackupProblem.BAD_MEDICATION, problemOf(data))
    }

    @Test
    fun `날짜가 맞는 꼴이 아니면 실패한다`() {
        val data = validBackup.copy(medications = listOf(validMedication.copy(startDate = "2026/10/10")))
        assertEquals(BackupProblem.BAD_MEDICATION, problemOf(data))
    }

    @Test
    fun `약 번호가 겹치면 실패한다`() {
        val data = validBackup.copy(medications = listOf(validMedication, validMedication.copy(name = "오메가3")))
        assertEquals(BackupProblem.BAD_MEDICATION, problemOf(data))
    }

    @Test
    fun `일시중지 시작일이 날짜가 아니면 실패한다`() {
        val data = validBackup.copy(medications = listOf(validMedication.copy(pausedFrom = "invalid")))
        assertEquals(BackupProblem.BAD_MEDICATION, problemOf(data))
    }

    @Test
    fun `터무니없는 해의 날짜는 실패한다`() {
        val data = validBackup.copy(medications = listOf(validMedication.copy(startDate = "0001-01-01")))
        assertEquals(BackupProblem.BAD_MEDICATION, problemOf(data))
    }

    // ---------- 규칙 ----------

    @Test
    fun `규칙의 적용 시작일이 날짜가 아니면 실패한다`() {
        val data = validBackup.copy(schedules = listOf(validSchedule.copy(effectiveFrom = "내일")))
        assertEquals(BackupProblem.BAD_SCHEDULE, problemOf(data))
    }

    @Test
    fun `규칙이 파일에 없는 약을 가리키면 실패한다`() {
        val data = validBackup.copy(schedules = listOf(validSchedule.copy(medicationId = 99)))
        assertEquals(BackupProblem.BAD_SCHEDULE, problemOf(data))
    }

    @Test
    fun `시각이 두 자리씩이 아니면 실패한다`() {
        val data = validBackup.copy(schedules = listOf(validSchedule.copy(time = "8:00")))
        assertEquals(BackupProblem.BAD_SCHEDULE, problemOf(data))
    }

    @Test
    fun `요일이 하나도 없으면 실패한다`() {
        val data = validBackup.copy(schedules = listOf(validSchedule.copy(repeatDays = "")))
        assertEquals(BackupProblem.BAD_SCHEDULE, problemOf(data))
    }

    // ---------- 기록 ----------

    @Test
    fun `기록이 파일에 없는 규칙을 가리키면 실패한다`() {
        val data = validBackup.copy(doseRecords = listOf(validRecord.copy(scheduleId = 99)))
        assertEquals(BackupProblem.BAD_RECORD, problemOf(data))
    }

    @Test
    fun `모르는 상태 값이면 실패한다`() {
        val data = validBackup.copy(doseRecords = listOf(validRecord.copy(status = "DONE")))
        assertEquals(BackupProblem.BAD_RECORD, problemOf(data))
    }

    @Test
    fun `같은 규칙 같은 날짜의 기록이 둘이면 실패한다`() {
        val data = validBackup.copy(doseRecords = listOf(validRecord, validRecord.copy(id = 2)))
        assertEquals(BackupProblem.BAD_RECORD, problemOf(data))
    }

    @Test
    fun `모레 이후 날짜의 기록이 있으면 실패한다`() {
        val data = validBackup.copy(doseRecords = listOf(validRecord.copy(date = "2026-11-19")))
        assertEquals(BackupProblem.BAD_RECORD, problemOf(data))
    }

    @Test
    fun `기록의 약 이름 스냅샷이 비어 있으면 실패한다`() {
        val data = validBackup.copy(doseRecords = listOf(validRecord.copy(medicationName = "")))
        assertEquals(BackupProblem.BAD_RECORD, problemOf(data))
    }

    // ---------- 이력 ----------

    @Test
    fun `이력이 파일에 없는 기록을 가리키면 실패한다`() {
        val data = validBackup.copy(statusEvents = listOf(validEvent.copy(doseRecordId = 99)))
        assertEquals(BackupProblem.BAD_EVENT, problemOf(data))
    }

    @Test
    fun `이력이 파일에 없는 규칙을 가리키면 실패한다`() {
        val data = validBackup.copy(statusEvents = listOf(validEvent.copy(scheduleId = 99)))
        assertEquals(BackupProblem.BAD_EVENT, problemOf(data))
    }

    @Test
    fun `기록이 지워진 이력은 회차 정보가 있으면 통과한다`() {
        val data = validBackup.copy(statusEvents = listOf(validEvent.copy(doseRecordId = null)))
        assertTrue(check(data) is BackupCheck.Ok)
    }

    @Test
    fun `이력의 회차 정보가 비어 있으면 연결된 기록에서 채운다`() {
        val bare = validEvent.copy(scheduleId = null, doseDate = null, scheduledTime = null, medicationName = null)

        val result = check(validBackup.copy(statusEvents = listOf(bare))) as BackupCheck.Ok

        assertEquals(validEvent, result.data.statusEvents.single())
    }

    @Test
    fun `회차 정보도 연결된 기록도 없는 이력은 실패한다`() {
        val orphan = validEvent.copy(doseRecordId = null, scheduleId = null)
        assertEquals(BackupProblem.BAD_EVENT, problemOf(validBackup.copy(statusEvents = listOf(orphan))))
    }

    // ---------- 복원 뒤 채울 양 ----------

    @Test
    fun `기록 없이 시작일만 옛날인 규칙이 너무 많으면 실패한다`() {
        // 2000-01-01부터 오늘까지 약 9,800일 × 규칙 3개 = 약 29,000일 > 상한 20,000
        val old = validMedication.copy(startDate = "2000-01-01")
        val schedules = (1L..3L).map { validSchedule.copy(id = it) }
        val data = validBackup.copy(
            medications = listOf(old), schedules = schedules, doseRecords = emptyList(), statusEvents = emptyList()
        )
        assertEquals(BackupProblem.TOO_MUCH_TO_FILL, problemOf(data))
    }

    @Test
    fun `삭제한 약은 채울 양에 넣지 않는다`() {
        val schedules = (1L..3L).map { validSchedule.copy(id = it, medicationId = it) }
        val medications = listOf(
            validMedication.copy(id = 1, startDate = "2000-01-01", deletedAt = 5),          // 삭제한 약
            validMedication.copy(id = 2, startDate = "2000-01-01", endDate = "2000-12-31"), // 끝난 약도 오늘까지로 센다 (약 9,800일)
            validMedication.copy(id = 3, startDate = "2000-01-01", deletedAt = 5)
        )
        val data = validBackup.copy(
            medications = medications, schedules = schedules, doseRecords = emptyList(), statusEvents = emptyList()
        )
        assertTrue(check(data) is BackupCheck.Ok)
    }
}
