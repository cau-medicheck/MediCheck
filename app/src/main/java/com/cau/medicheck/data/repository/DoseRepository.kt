package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecord
import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.DoseRecordRow
import com.cau.medicheck.data.local.DoseStatusEvent
import com.cau.medicheck.data.local.DoseStatusEventDao
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.DoseStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

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

/**
 * 날짜별 복용 기록의 조회와 상태 변경을 담당한다.
 *
 * 화면(ViewModel)은 이 클래스의 함수만 부른다.
 *   - refresh: 비어 있는 날짜의 기록을 오늘까지 채운다 (앱을 열 때)
 *   - observeDoses: 날짜 범위의 기록을 계속 흘려 보낸다 (약 이름·복용량은 기록의 스냅샷에서)
 *   - changeStatus / setStatus: 복용 완료 / 건너뜀 / 미기록으로
 *   - undoLast: 그 회차의 마지막 변경 실행취소
 *
 * 상태를 바꿀 때는 언제나 "기록 고치기 + 이력(dose_status_events) 한 줄 쓰기"를 한 트랜잭션으로 한다.
 * 그래서 변경 이력 화면(S30)과 실행취소가 빠짐없이 동작한다.
 */
class DoseRepository(
    private val doseRecordDao: DoseRecordDao,
    private val statusEventDao: DoseStatusEventDao,
    private val generator: DoseRecordGenerator,
    private val transaction: TransactionRunner,
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
     * 회차 열쇠로 상태를 바꾼다 (2단계 화면부터 이것을 쓴다).
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
     * 기록 번호로 상태를 바꾼다. (지금 화면 — 오늘·기록 탭 — 이 쓰는 함수. 2단계에서 changeStatus로 옮긴다)
     *
     *   TAKEN / SKIPPED : 버튼을 누른 지금 시각을 acted_at에 적는다.
     *                     시각이 지난 기록도 바꿀 수 있다 = "늦게 먹음".
     *                     TAKEN ↔ SKIPPED 사이도 바꿀 수 있다 = 잘못 누른 것 고치기.
     *   PENDING         : 미기록으로 되돌리기. acted_at을 비운다.
     *
     * @return 내 기록이면 true(이미 그 상태였어도 true). 기록이 없거나 내 기록이 아니면 false
     */
    suspend fun setStatus(userId: Long, recordId: Long, status: DoseStatus): Boolean =
        transaction.run {
            val record = doseRecordDao.findOwned(userId, recordId) ?: return@run false
            applyStatus(userId, record, status)
            true
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
        id = id,
        medicationId = medicationId,
        medicationName = medicationName,
        dosage = dosage,
        date = LocalDate.parse(date),
        time = LocalTime.parse(scheduledTime),
        status = DoseStatus.valueOf(status),
        actedAt = actedAt,
        scheduleId = scheduleId,
        memo = memo
    )
}
