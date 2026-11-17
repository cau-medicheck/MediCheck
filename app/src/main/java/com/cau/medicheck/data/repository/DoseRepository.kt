package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecord
import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.DoseRecordRow
import com.cau.medicheck.data.local.DoseStatusEvent
import com.cau.medicheck.data.local.DoseStatusEventDao
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.DosePlanner
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.domain.MedicationDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** 상태 바꾸기의 결과 (DB_DESIGN ⑩ 10.3) */
enum class StatusChangeResult {
    CHANGED,     // 바꿨고 이력 한 줄을 남겼다
    UNCHANGED,   // 이미 그 상태라 아무것도 쓰지 않았다
    FUTURE,      // 기록 줄이 아직 없는 미래 회차라 바꿀 수 없다
    NOT_FOUND    // 내 기록이 아니거나 없는 회차
}

/** 실행취소의 결과 */
enum class UndoResult {
    UNDONE,      // 마지막 변경을 되돌렸다
    NOTHING      // 되돌릴 것이 없다 (화면 문구: '실행취소할 기록이 없습니다.')
}

/** 실제 복용 시각 저장(S31)의 결과 */
enum class ActualTimeResult {
    SAVED,           // 복용 완료 + 실제 시각을 저장했다
    UNCHANGED,       // 이미 복용 완료이고 같은 시각이라 아무것도 쓰지 않았다
    FUTURE_DOSE,     // 아직 오지 않은 날짜의 회차
    TIME_IN_FUTURE,  // 적은 시각이 지금보다 늦다
    NOT_FOUND        // 내 기록이 아니거나 없는 회차
}

/**
 * 과거 기록 보존 화면(S32)의 내용.
 * @param total 오늘보다 앞선 날짜의 기록 수
 * @param days 최근 날짜부터 (날짜, 그날의 기록 수)
 */
data class DoseArchive(val total: Int, val days: List<Pair<LocalDate, Int>>)

/** 변경 이력 화면(S30)의 한 줄 */
data class DoseHistoryItem(
    val eventId: Long,
    val medicationName: String,
    val date: LocalDate,
    val from: DoseStatus,
    val to: DoseStatus,
    /** 바꾼 시각 (밀리초) */
    val changedAt: Long
)

/**
 * 날짜별 복용 기록의 조회와 상태 변경을 담당한다.
 *
 * 화면(ViewModel)은 이 클래스의 함수만 부른다.
 *   - refresh: 비어 있는 날짜의 기록을 오늘까지 채운다 (앱을 열 때)
 *   - observeDoses: 날짜 범위의 기록을 계속 흘려 보낸다 (통계용. 약 이름·복용량은 기록의 스냅샷에서)
 *   - observeDay / observeDose: 하루치 / 회차 하나. 내일 이후는 규칙에서 계산한 "예정 회차"
 *   - changeStatus: 복용 완료 / 건너뜀 / 미기록으로
 *   - recordActualTime: 실제 복용 시각을 적고 복용 완료로
 *   - undoLast: 그 회차의 마지막 변경 실행취소
 *   - observeHistory: 변경 이력
 *
 * 상태를 바꾸는 길은 changeStatus·recordActualTime·undoLast 셋뿐이다.
 *
 * 상태를 바꿀 때는 언제나 "기록 고치기 + 이력(dose_status_events) 한 줄 쓰기"를 한 트랜잭션으로 한다.
 * 그래서 변경 이력 화면(S30)과 실행취소가 빠짐없이 동작한다.
 */
class DoseRepository(
    private val doseRecordDao: DoseRecordDao,
    private val statusEventDao: DoseStatusEventDao,
    private val generator: DoseRecordGenerator,
    private val transaction: TransactionRunner,
    /** 삭제되지 않은 약 + 규칙의 흐름. 내일 이후 예정 회차를 계산할 재료다. (MedicationRepository.observeMedications) */
    private val observeMedications: (userId: Long) -> Flow<List<MedicationDetail>>,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    /** 한 사용자의 기록을 오늘까지 채운다. 여러 줄을 넣으므로 한 묶음(트랜잭션)으로 실행한다. */
    suspend fun refresh(userId: Long) {
        transaction.run { generator.generateForUser(userId) }
    }

    /**
     * 날짜 범위(from ~ to, 양 끝 포함)의 기록을 계속 흘려 보낸다.
     * 날짜 → 시각 → 약 이름 순으로 정렬되어 나온다. 삭제된 약의 기록도 포함한다.
     */
    fun observeDoses(userId: Long, from: LocalDate, to: LocalDate): Flow<List<DoseItem>> =
        doseRecordDao.observeRange(userId, from.toString(), to.toString())
            .map { rows -> rows.map { it.toItem() } }

    /**
     * 회차 열쇠로 상태를 바꾼다. (S10의 버튼 3개, 홈·일정의 "✓ 복용 완료")
     *
     * 기록 줄이 없고 날짜가 오늘 이전이면, 자정을 막 넘긴 경우일 수 있으므로 한 번 보충한 뒤 다시 찾는다.
     * 그래도 없으면 NOT_FOUND, 미래 날짜면 FUTURE.
     * 삭제된 약의 지난 기록도 바꿀 수 있다 (2026-11-17 결정 5).
     */
    suspend fun changeStatus(userId: Long, key: DoseKey, status: DoseStatus): StatusChangeResult {
        val today = LocalDate.now(clock)
        // 보충은 트랜잭션 밖에서 먼저 한다(보충도 자기 트랜잭션을 쓴다).
        if (!key.date.isAfter(today) && findByKey(userId, key) == null) refresh(userId)

        // 찾기와 바꾸기를 같은 트랜잭션 안에서 한다. 밖에서 읽은 옛 값으로 바꾸면,
        // 같은 회차를 두 번 빠르게 눌렀을 때 둘 다 "PENDING이었다"고 보고 이력을 두 줄 쓸 수 있다.
        return transaction.run {
            val found = findByKey(userId, key)
                ?: return@run if (key.date.isAfter(today)) StatusChangeResult.FUTURE else StatusChangeResult.NOT_FOUND
            applyStatus(userId, found, status)
        }
    }

    /**
     * 하루치 회차를 계속 흘려 보낸다. 정렬: 예정 시각 → 약 이름.
     *   - 오늘·지난 날짜 → 저장된 기록 (삭제한 약의 남은 기록도 포함)
     *   - 내일 이후 → 지금의 약·규칙으로 계산한 예정 회차 (보기 전용. 약을 고치면 바로 다시 계산된다)
     */
    fun observeDay(userId: Long, date: LocalDate, today: LocalDate): Flow<List<DoseItem>> =
        if (date.isAfter(today)) {
            observeMedications(userId).map { meds -> DosePlanner.plannedDoses(date, meds, today) }
        } else {
            observeDoses(userId, date, date)
        }

    /** 회차 하나를 계속 흘려 보낸다. 없으면 null. (S10·S31) */
    fun observeDose(userId: Long, key: DoseKey, today: LocalDate): Flow<DoseItem?> =
        if (key.date.isAfter(today)) {
            observeDay(userId, key.date, today).map { doses -> doses.find { it.scheduleId == key.scheduleId } }
        } else {
            doseRecordDao.observeOwnedRowByKey(userId, key.scheduleId, key.date.toString()).map { it?.toItem() }
        }

    /** 그 날짜의 첫 회차(시각 → 이름 순)의 열쇠. 회차가 없으면 null. (S30 "복용 회차 열기") */
    suspend fun firstDoseOf(userId: Long, date: LocalDate): DoseKey? =
        observeDay(userId, date, LocalDate.now(clock)).first().firstOrNull()?.key

    /**
     * 과거 기록 보존(S32): 오늘보다 앞선 날짜의 기록 수와, 최근 [dayLimit]개 날짜별 기록 수.
     * 약을 고치거나 지워도 지난 기록은 그때의 이름·복용량·시각 그대로 남아 있으므로, 그 수를 그대로 센다.
     */
    fun observeArchive(userId: Long, today: LocalDate, dayLimit: Int = 10): Flow<DoseArchive> =
        combine(
            doseRecordDao.observePastRecordCount(userId, today.toString()),
            doseRecordDao.observeRecentPastDates(userId, today.toString(), dayLimit)
        ) { total, dates -> DoseArchive(total, dates.map { LocalDate.parse(it.date) to it.count }) }

    /** 한 사용자의 상태 변경 이력, 최신순. 기록이 지워진 회차의 이력도 포함한다. (S30) */
    fun observeHistory(userId: Long): Flow<List<DoseHistoryItem>> =
        statusEventDao.observeForUser(userId).map { events ->
            events.map {
                DoseHistoryItem(
                    eventId = it.id,
                    medicationName = it.medicationName,
                    date = LocalDate.parse(it.doseDate),
                    from = DoseStatus.valueOf(it.fromStatus),
                    to = DoseStatus.valueOf(it.toStatus),
                    changedAt = it.changedAt
                )
            }
        }

    /**
     * 실제로 먹은 시각을 적고 복용 완료로 기록한다. (S31)
     *
     * 검증 순서: 회차가 있는가 → 미래 날짜가 아닌가 → 적은 시각이 지금보다 늦지 않은가.
     * 저장하는 값: acted_at = (회차 날짜 + 적은 시각), 상태 = 복용 완료. 이력도 한 줄 남긴다
     * (이력에 바뀌기 전 시각이 들어 있어서, 실행취소하면 시각까지 되돌아간다).
     * 예정 시각보다 이른 시각도 받는다 (미리 먹은 경우).
     */
    suspend fun recordActualTime(userId: Long, key: DoseKey, time: LocalTime): ActualTimeResult {
        val today = LocalDate.now(clock)
        if (key.date.isAfter(today)) {
            // 미래 회차는 기록 줄이 없다. 예정 회차로도 없으면 "없는 회차"다.
            return if (observeDose(userId, key, today).first() == null) ActualTimeResult.NOT_FOUND
            else ActualTimeResult.FUTURE_DOSE
        }
        if (findByKey(userId, key) == null) refresh(userId)

        val actualAt = LocalDateTime.of(key.date, time.truncatedTo(ChronoUnit.MINUTES))
        if (actualAt.isAfter(LocalDateTime.now(clock))) {
            return if (findByKey(userId, key) == null) ActualTimeResult.NOT_FOUND else ActualTimeResult.TIME_IN_FUTURE
        }
        val actedAt = actualAt.atZone(clock.zone).toInstant().toEpochMilli()

        return transaction.run {
            val record = findByKey(userId, key) ?: return@run ActualTimeResult.NOT_FOUND
            if (record.status == DoseStatus.TAKEN.name && record.actedAt == actedAt) {
                return@run ActualTimeResult.UNCHANGED
            }
            doseRecordDao.updateStatus(userId, record.id, DoseStatus.TAKEN.name, actedAt)
            statusEventDao.insert(
                eventFor(userId, record, record.status, DoseStatus.TAKEN.name, record.actedAt, actedAt, isUndo = false)
            )
            ActualTimeResult.SAVED
        }
    }

    /**
     * 그 회차의 마지막 변경을 되돌린다 (한 단계만). DB_DESIGN ⑩ "실행취소".
     *
     * 되돌리지 않는 경우(NOTHING):
     *   - 기록 줄이 없음 / 이력이 없음
     *   - 마지막 이력이 이미 실행취소로 생긴 줄 (실행취소의 실행취소는 하지 않는다)
     *   - 기록의 지금 상태가 마지막 이력의 "바뀐 뒤 값"과 다름 (이력 없이 정해진 상태 — 샘플 데이터 등 —
     *     를 엉뚱한 값으로 되돌리지 않기 위해서)
     */
    suspend fun undoLast(userId: Long, key: DoseKey): UndoResult = transaction.run {
        val record = findByKey(userId, key) ?: return@run UndoResult.NOTHING
        val last = statusEventDao.latestFor(userId, record.id) ?: return@run UndoResult.NOTHING
        if (last.isUndo) return@run UndoResult.NOTHING
        if (record.status != last.toStatus || record.actedAt != last.toActedAt) return@run UndoResult.NOTHING

        doseRecordDao.updateStatus(userId, record.id, last.fromStatus, last.fromActedAt)
        statusEventDao.insert(
            eventFor(userId, record, last.toStatus, last.fromStatus, last.toActedAt, last.fromActedAt, isUndo = true)
        )
        UndoResult.UNDONE
    }

    /**
     * 기록 하나의 상태를 실제로 바꾸고 이력 한 줄을 쓴다. 부르는 쪽이 트랜잭션 안에서 부른다.
     * 같은 상태로 바꾸려 하면 아무것도 쓰지 않는다(acted_at도 그대로).
     */
    private suspend fun applyStatus(userId: Long, record: DoseRecord, status: DoseStatus): StatusChangeResult {
        if (record.status == status.name) return StatusChangeResult.UNCHANGED

        val newActedAt = if (status == DoseStatus.PENDING) null else clock.millis()
        doseRecordDao.updateStatus(userId, record.id, status.name, newActedAt)
        statusEventDao.insert(
            eventFor(userId, record, record.status, status.name, record.actedAt, newActedAt, isUndo = false)
        )
        return StatusChangeResult.CHANGED
    }

    /** 이력 한 줄. 회차 정보(규칙·날짜·시각·이름)는 기록 줄의 값을 복사한다. */
    private fun eventFor(
        userId: Long,
        record: DoseRecord,
        fromStatus: String,
        toStatus: String,
        fromActedAt: Long?,
        toActedAt: Long?,
        isUndo: Boolean
    ) = DoseStatusEvent(
        userId = userId,
        doseRecordId = record.id,
        scheduleId = record.scheduleId,
        doseDate = record.date,
        scheduledTime = record.scheduledTime,
        medicationName = record.medicationName,
        fromStatus = fromStatus,
        toStatus = toStatus,
        fromActedAt = fromActedAt,
        toActedAt = toActedAt,
        changedAt = clock.millis(),
        isUndo = isUndo
    )

    private suspend fun findByKey(userId: Long, key: DoseKey): DoseRecord? =
        doseRecordDao.findOwnedByKey(userId, key.scheduleId, key.date.toString())

    /** 표에서 읽은 줄(글자) → 화면이 쓰는 모양(날짜·시각·상태 타입) */
    private fun DoseRecordRow.toItem() = DoseItem(
        recordId = id,
        scheduleId = scheduleId,
        medicationId = medicationId,
        medicationName = medicationName,
        dosage = dosage,
        memo = memo.orEmpty(),
        date = LocalDate.parse(date),
        time = LocalTime.parse(scheduledTime),
        status = DoseStatus.valueOf(status),
        actedAt = actedAt
    )
}
