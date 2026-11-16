package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.DoseRecordRow
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.DoseStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

/**
 * 날짜별 복용 기록의 조회와 상태 변경을 담당한다.
 *
 * 화면(ViewModel)은 이 클래스의 함수만 부른다.
 *   - refresh: 비어 있는 날짜의 기록을 오늘까지 채운다 (앱을 열 때)
 *   - observeDoses: 날짜 범위의 기록을 계속 흘려 보낸다
 *   - setStatus: 복용 완료 / 건너뜀 / 되돌리기
 */
class DoseRepository(
    private val doseRecordDao: DoseRecordDao,
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
     * 기록의 상태를 바꾼다.
     *
     *   TAKEN / SKIPPED : 버튼을 누른 지금 시각을 acted_at에 적는다.
     *                     시각이 지난(깜빡한) 기록도 바꿀 수 있다 = "늦게 먹음".
     *                     TAKEN ↔ SKIPPED 사이도 바꿀 수 있다 = 잘못 누른 것 고치기.
     *   PENDING         : 되돌리기. acted_at을 비운다.
     *
     * @return 바꿨으면 true. 기록이 없거나 내 기록이 아니면 false
     */
    suspend fun setStatus(userId: Long, recordId: Long, status: DoseStatus): Boolean {
        doseRecordDao.findOwned(recordId, userId) ?: return false

        val actedAt = if (status == DoseStatus.PENDING) null else clock.millis()
        doseRecordDao.updateStatus(recordId, status.name, actedAt)
        return true
    }

    /** 표에서 읽은 줄(글자) → 화면이 쓰는 모양(날짜·시각·상태 타입) */
    private fun DoseRecordRow.toItem() = DoseItem(
        id = id,
        medicationId = medicationId,
        medicationName = medicationName,
        dosage = dosage,
        date = LocalDate.parse(date),
        time = LocalTime.parse(scheduledTime),
        status = DoseStatus.valueOf(status),
        actedAt = actedAt
    )
}
