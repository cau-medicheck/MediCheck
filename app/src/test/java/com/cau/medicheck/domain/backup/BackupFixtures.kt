package com.cau.medicheck.domain.backup

/*
 * 백업 테스트들이 함께 쓰는 "올바른 최소 백업": 약 1개, 규칙 1개, 기록 1개, 이력 1개.
 * 각 테스트는 이것을 copy로 한 군데만 고쳐서 "그 한 가지가 틀리면 어떻게 되는지"를 본다.
 */

internal const val FIXTURE_EMAIL = "suhwan@cau.ac.kr"

internal val validMedication = BackupMedication(
    id = 1, name = "비타민 D", dosage = "1정", startDate = "2026-10-10", endDate = null, memo = "아침 식후",
    createdAt = 1_791_590_400_000, deletedAt = null,
    pausedFrom = null, hospital = "중앙대병원", department = "가정의학과", sourceMemo = null
)

internal val validSchedule = BackupSchedule(
    id = 1, medicationId = 1, time = "08:00", repeatDays = "MON,TUE,WED,THU,FRI,SAT,SUN",
    effectiveFrom = null, createdAt = 1_791_590_400_000, deletedAt = null
)

internal val validRecord = BackupDoseRecord(
    id = 1, scheduleId = 1, date = "2026-11-16", scheduledTime = "08:00",
    medicationName = "비타민 D", dosage = "1정", memo = "아침 식후",
    status = "TAKEN", actedAt = 1_794_783_780_000, createdAt = 1_794_754_800_000
)

internal val validEvent = BackupStatusEvent(
    doseRecordId = 1, scheduleId = 1, doseDate = "2026-11-16", scheduledTime = "08:00", medicationName = "비타민 D",
    fromStatus = "PENDING", toStatus = "TAKEN", fromActedAt = null, toActedAt = 1_794_783_780_000,
    changedAt = 1_794_783_780_000, isUndo = false
)

internal val validBackup = BackupData(
    version = 1,
    exportedAt = "2026-11-17T21:05:00+09:00",
    accountEmail = FIXTURE_EMAIL,
    medications = listOf(validMedication),
    schedules = listOf(validSchedule),
    doseRecords = listOf(validRecord),
    statusEvents = listOf(validEvent)
)
