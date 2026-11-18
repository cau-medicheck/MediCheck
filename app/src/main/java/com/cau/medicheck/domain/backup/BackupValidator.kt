package com.cau.medicheck.domain.backup

import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.domain.RepeatDays
import java.time.LocalDate

/** 백업 파일에서 찾은 문제의 종류. 화면에는 늘 같은 안내 하나만 띄우고, 종류는 테스트와 원인 찾기에 쓴다. */
enum class BackupProblem { UNSUPPORTED_VERSION, BAD_MEDICATION, BAD_SCHEDULE, BAD_RECORD, BAD_EVENT, TOO_MUCH_TO_FILL }

/** 형식 검사의 결과 */
sealed interface BackupCheck {
    /** 통과. data는 비어 있던 이력 칸을 채운, 복원에 그대로 쓸 수 있는 내용이다 */
    data class Ok(val data: BackupData) : BackupCheck

    /** 실패. 처음 걸린 문제 하나 */
    data class Invalid(val problem: BackupProblem) : BackupCheck
}

/**
 * 백업 파일의 내용이 복원해도 안전한지 검사한다. (DB_DESIGN 부록 B "형식 검사")
 *
 * 잘못된 파일을 그대로 넣으면 DB가 깨진다(없는 약을 가리키는 규칙, 같은 날 두 번 있는 기록 등).
 * 그래서 DB에 손대기 전에 여기서 전부 확인한다. 순수 함수라 폰 없이 단위 테스트한다.
 *
 * 검사 순서: 버전 → 약 → 규칙 → 기록 → 이력 → 복원 뒤 채울 양. 앞에서 걸리면 거기서 멈춘다.
 */
object BackupValidator {

    private val TIME = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")   // "08:00" 꼴. "8:00"·"24:00"은 틀림
    private val STATUSES = DoseStatus.entries.map { it.name }.toSet()

    /** 받아들이는 날짜의 범위. 이 밖의 해(기원전, 9999년 등)는 잘못 만든 파일로 본다 */
    private val YEARS = 2000..2100

    /**
     * 복원 뒤 "빈 날 채우기"가 새로 만들 기록 수의 상한.
     * 복원하면 마지막 기록 다음 날부터 오늘까지의 회차를 채우는데, 시작일만 아주 옛날이고 기록이 없는 파일을 받으면
     * 수백만 줄을 만들려다 앱이 멈춘다. 규칙 하나를 55년 치 채우는 양(2만)을 넘으면 받지 않는다.
     */
    const val MAX_FILL_DAYS = 20_000

    /**
     * @param today 오늘 날짜. 기록 날짜가 내일보다 뒤이면 실패로 본다
     *              (내일까지 받는 이유: 시간대 정책을 바꾼 계정에는 하루 앞선 날짜의 줄이 있을 수 있다)
     */
    fun check(data: BackupData, today: LocalDate): BackupCheck {
        if (data.version != BackupData.VERSION) return BackupCheck.Invalid(BackupProblem.UNSUPPORTED_VERSION)
        if (!medicationsOk(data.medications)) return BackupCheck.Invalid(BackupProblem.BAD_MEDICATION)
        if (!schedulesOk(data)) return BackupCheck.Invalid(BackupProblem.BAD_SCHEDULE)
        if (!recordsOk(data, today)) return BackupCheck.Invalid(BackupProblem.BAD_RECORD)
        val events = filledEvents(data) ?: return BackupCheck.Invalid(BackupProblem.BAD_EVENT)
        if (daysToFill(data, today) > MAX_FILL_DAYS) return BackupCheck.Invalid(BackupProblem.TOO_MUCH_TO_FILL)
        return BackupCheck.Ok(data.copy(statusEvents = events))
    }

    /**
     * 약: id가 겹치지 않고, 이름·복용량이 비어 있지 않고, 날짜가 맞는 꼴이며 종료일이 시작일보다 앞서지 않는다.
     * 일시중지 시작일도 날짜여야 한다 — 틀린 글자가 들어가면 복원 뒤 기록을 만들 때 앱이 멈춘다.
     */
    private fun medicationsOk(medications: List<BackupMedication>): Boolean {
        if (hasDuplicates(medications.map { it.id })) return false
        return medications.all { med ->
            val start = dateOrNull(med.startDate)
            val endOk = med.endDate == null || dateOrNull(med.endDate)?.let { start != null && !it.isBefore(start) } == true
            med.name.isNotBlank() && med.dosage.isNotBlank() && start != null && endOk && dateOrAbsent(med.pausedFrom)
        }
    }

    /** 규칙: id가 겹치지 않고, 파일 안의 약을 가리키고, 시각이 "HH:mm"이며 요일이 하나 이상이고, 적용 시작일이 날짜다. */
    private fun schedulesOk(data: BackupData): Boolean {
        if (hasDuplicates(data.schedules.map { it.id })) return false
        val medicationIds = data.medications.map { it.id }.toSet()
        return data.schedules.all {
            it.medicationId in medicationIds && TIME.matches(it.time) &&
                RepeatDays.decode(it.repeatDays).isNotEmpty() && dateOrAbsent(it.effectiveFrom)
        }
    }

    /** 기록: id와 (규칙, 날짜)가 겹치지 않고, 파일 안의 규칙을 가리키고, 날짜·시각·상태·스냅샷이 맞다. */
    private fun recordsOk(data: BackupData, today: LocalDate): Boolean {
        val records = data.doseRecords
        if (hasDuplicates(records.map { it.id })) return false
        if (hasDuplicates(records.map { it.scheduleId to it.date })) return false   // DB의 UNIQUE(규칙, 날짜)와 같은 검사
        val scheduleIds = data.schedules.map { it.id }.toSet()
        val lastAllowed = today.plusDays(1)
        return records.all { record ->
            val date = dateOrNull(record.date)
            record.scheduleId in scheduleIds &&
                date != null && !date.isAfter(lastAllowed) &&
                TIME.matches(record.scheduledTime) &&
                record.status in STATUSES &&
                record.medicationName.isNotBlank() && record.dosage.isNotBlank()
        }
    }

    /**
     * 이력: 비어 있는 회차 정보는 연결된 기록에서 채운다. 하나라도 맞지 않으면 null.
     * - 기록 번호는 없거나(null) 파일 안의 기록이어야 한다
     * - 규칙 번호는 파일 안의 규칙이어야 한다 (복원할 때 새 규칙 번호로 바꿔야 하므로)
     */
    private fun filledEvents(data: BackupData): List<BackupStatusEvent>? {
        val recordsById = data.doseRecords.associateBy { it.id }
        val scheduleIds = data.schedules.map { it.id }.toSet()
        return data.statusEvents.map { event ->
            val record = event.doseRecordId?.let { recordsById[it] ?: return null }
            val filled = event.copy(
                scheduleId = event.scheduleId ?: record?.scheduleId,
                doseDate = event.doseDate ?: record?.date,
                scheduledTime = event.scheduledTime ?: record?.scheduledTime,
                medicationName = event.medicationName ?: record?.medicationName
            )
            val ok = filled.scheduleId in scheduleIds &&
                filled.doseDate?.let(::dateOrNull) != null &&
                filled.scheduledTime?.let(TIME::matches) == true &&
                !filled.medicationName.isNullOrBlank() &&
                filled.fromStatus in STATUSES && filled.toStatus in STATUSES
            if (!ok) return null
            filled
        }
    }

    /**
     * 복원 뒤 "빈 날 채우기"가 살펴볼 날 수를 넉넉하게(요일·일시중지를 따지지 않고) 어림한다.
     * 사용 중인 약의 사용 중인 규칙마다: (마지막 기록 다음 날, 없으면 시작일) ~ 오늘.
     * 종료일이 지난 약도 오늘까지로 센다 — 채우기는 종료일을 넘은 날도 하루씩 살펴보기 때문이다.
     * 앞의 검사를 모두 통과한 뒤에 부르므로 날짜는 모두 읽을 수 있다.
     */
    private fun daysToFill(data: BackupData, today: LocalDate): Long {
        val activeMedications = data.medications.filter { it.deletedAt == null }.associateBy { it.id }
        val lastRecordDate = data.doseRecords.groupBy({ it.scheduleId }, { LocalDate.parse(it.date) })
            .mapValues { (_, dates) -> dates.max() }
        return data.schedules.filter { it.deletedAt == null }.sumOf { schedule ->
            val medication = activeMedications[schedule.medicationId] ?: return@sumOf 0L
            val from = lastRecordDate[schedule.id]?.plusDays(1) ?: LocalDate.parse(medication.startDate)
            (today.toEpochDay() - from.toEpochDay() + 1).coerceAtLeast(0L)
        }
    }

    private fun <T> hasDuplicates(values: List<T>): Boolean = values.size != values.toSet().size

    /** "2026-11-18" 꼴이고 2000~2100년이면 날짜로, 아니면 null (없는 날짜 "2026-02-30"도 null) */
    private fun dateOrNull(text: String): LocalDate? =
        runCatching { LocalDate.parse(text) }.getOrNull()?.takeIf { it.year in YEARS }

    /** 값이 없거나(null) 맞는 날짜이면 true */
    private fun dateOrAbsent(text: String?): Boolean = text == null || dateOrNull(text) != null
}
