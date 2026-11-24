package com.cau.medicheck.domain.backup

/**
 * 백업 파일 한 개의 내용. (DB_DESIGN 부록 B — 백업 파일 형식 버전 1)
 *
 * DB의 표 모양(Entity)과 따로 둔 이유: 파일 형식은 한 번 정하면 바꾸기 어렵고(옛 파일도 읽어야 한다),
 * DB 표는 앱이 자라면서 바뀐다. 둘을 분리해 두면 표를 고쳐도 파일 형식이 저절로 바뀌지 않는다.
 * 안드로이드 기능을 쓰지 않는 순수 Kotlin이라 폰 없이 단위 테스트한다.
 *
 * 파일 안의 id는 "파일 안에서 서로를 가리키는 번호"일 뿐이다. 복원할 때는 DB가 새 id를 매긴다.
 *
 * @param accountEmail 내보낸 계정의 이메일. 복원은 같은 계정에서만 된다
 */
data class BackupData(
    val version: Int,
    val exportedAt: String?,
    val accountEmail: String?,
    val medications: List<BackupMedication>,
    val schedules: List<BackupSchedule>,
    val doseRecords: List<BackupDoseRecord>,
    val statusEvents: List<BackupStatusEvent>
) {
    companion object {
        /** 파일 맨 위의 "이 파일은 MediCheck 백업이다"라는 표시 */
        const val FORMAT = "MediCheck-backup"

        /** 지금 쓰고 읽는 형식 버전. 형식을 바꾸면 2로 올리고 1도 읽게 고친다 */
        const val VERSION = 1
    }
}

/** 약 한 줄 (medications 표) */
data class BackupMedication(
    val id: Long,
    val name: String,
    val dosage: String,
    val startDate: String,
    val endDate: String?,
    val memo: String?,
    val createdAt: Long,
    val deletedAt: Long?,
    val pausedFrom: String?,
    val hospital: String?,
    val department: String?,
    val sourceMemo: String?,
    /** 아이콘 모양·색. 아이콘이 생기기 전(version 7 전)에 만든 백업에는 없으므로 기본값 */
    val shape: String = "TABLET",
    val color: String = "TEAL"
)

/** 복용 규칙 한 줄 (schedules 표) */
data class BackupSchedule(
    val id: Long,
    val medicationId: Long,
    val time: String,
    val repeatDays: String,
    val effectiveFrom: String?,
    val createdAt: Long,
    val deletedAt: Long?,
    /** 복용 조건. 조건이 생기기 전(version 6 전)에 만든 백업에는 없으므로 "NONE" */
    val doseCondition: String = "NONE"
)

/** 복용 기록 한 줄 (dose_records 표). 약 이름·복용량·메모는 그 회차 때의 값(스냅샷)이다 */
data class BackupDoseRecord(
    val id: Long,
    val scheduleId: Long,
    val date: String,
    val scheduledTime: String,
    val medicationName: String,
    val dosage: String,
    val memo: String?,
    val status: String,
    val actedAt: Long?,
    val createdAt: Long,
    /** 복용 조건 스냅샷. 옛 백업에는 없으므로 "NONE" */
    val doseCondition: String = "NONE"
)

/**
 * 상태 변경 이력 한 줄 (dose_status_events 표).
 * doseRecordId는 null일 수 있다(그 기록이 지워진 이력).
 * 회차 정보 네 칸(scheduleId·doseDate·scheduledTime·medicationName)은 파일에 없을 수 있어 null을 허용하고,
 * 형식 검사(BackupValidator)가 연결된 기록에서 채워 넣는다.
 */
data class BackupStatusEvent(
    val doseRecordId: Long?,
    val scheduleId: Long?,
    val doseDate: String?,
    val scheduledTime: String?,
    val medicationName: String?,
    val fromStatus: String,
    val toStatus: String,
    val fromActedAt: Long?,
    val toActedAt: Long?,
    val changedAt: Long,
    val isUndo: Boolean
)
