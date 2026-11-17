package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecord
import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.DoseRecordRow
import com.cau.medicheck.data.local.DoseStatusEvent
import com.cau.medicheck.data.local.DoseStatusEventDao
import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.MedicationDao
import com.cau.medicheck.data.local.ReminderRow
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.data.local.ScheduleDao
import com.cau.medicheck.data.local.SessionStore
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.data.local.User
import com.cau.medicheck.data.local.UserDao
import com.cau.medicheck.data.local.UserSettingsDao
import com.cau.medicheck.data.local.UserSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/*
 * 단위 테스트용 가짜(Fake) 모음.
 *
 * 진짜 DAO는 Room이 만들어 주고 폰의 DB가 있어야 동작한다.
 * 테스트에서는 같은 interface를 구현하되 메모리의 목록에만 저장하는 가짜를 대신 끼운다.
 * 그러면 폰 없이 컴퓨터에서 Repository의 순서와 결과를 확인할 수 있다.
 *
 * 각 가짜는 진짜 표의 조회문(SQL)이 하는 일을 Kotlin 코드로 흉내 낸다. userId 조건(계정 격리)도 흉내 낸다.
 * 가짜는 FK의 CASCADE·SET NULL을 흉내 내지 않는다. 그래도 되는 이유: 앱 코드가 물리 삭제를 할 때
 * CASCADE에 기대지 않고 "이력 → 기록 → 규칙 → 약 → 설정 → 사용자" 순서로 직접 지우기 때문이다.
 *
 * id는 한 번 매긴 번호를 다시 쓰지 않는다(lastId). 진짜 표의 자동 증가 번호와 같게 하기 위해서다.
 */

/** 메모리의 목록에만 저장하는 가짜 UserDao. */
internal class FakeUserDao : UserDao {
    val users = mutableListOf<User>()
    private var lastId = 0L

    override suspend fun insert(user: User): Long {
        // 진짜 표의 UNIQUE(email) 흉내는 하지 않는다 (Repository가 먼저 확인한다)
        val newId = ++lastId
        users.add(user.copy(id = newId))
        return newId
    }

    override suspend fun update(user: User) {
        users.replaceAll { if (it.id == user.id) user else it }
    }

    override suspend fun findByEmail(email: String): User? = users.find { it.email == email }

    override suspend fun findById(id: Long): User? = users.find { it.id == id }

    override suspend fun findSampleUser(): User? = users.filter { it.isSample }.maxByOrNull { it.id }

    override suspend fun findSampleUsers(): List<User> = users.filter { it.isSample }

    override suspend fun deleteById(userId: Long) {
        users.removeAll { it.id == userId }
    }
}

/** 메모리의 목록에만 저장하는 가짜 UserSettingsDao. */
internal class FakeUserSettingsDao : UserSettingsDao {
    val rows = MutableStateFlow<List<UserSettingsEntity>>(emptyList())

    override suspend fun insert(settings: UserSettingsEntity) {
        // 진짜 표는 PK(user_id)가 겹치면 오류를 낸다. 테스트에서 겹치게 넣으면 바로 알 수 있게 똑같이 한다.
        check(rows.value.none { it.userId == settings.userId }) { "user_settings PK 중복: ${settings.userId}" }
        rows.update { it + settings }
    }

    override suspend fun upsert(settings: UserSettingsEntity) {
        rows.update { list -> list.filterNot { it.userId == settings.userId } + settings }
    }

    override suspend fun find(userId: Long): UserSettingsEntity? = rows.value.find { it.userId == userId }

    override fun observe(userId: Long): Flow<UserSettingsEntity?> =
        rows.map { list -> list.find { it.userId == userId } }

    override suspend fun deleteForUser(userId: Long) {
        rows.update { list -> list.filterNot { it.userId == userId } }
    }
}

/** 메모리의 목록에만 저장하는 가짜 MedicationDao. */
internal class FakeMedicationDao : MedicationDao {
    val medications = MutableStateFlow<List<Medication>>(emptyList())
    private var lastId = 0L

    override suspend fun insert(medication: Medication): Long {
        val newId = ++lastId
        medications.update { it + medication.copy(id = newId) }
        return newId
    }

    override suspend fun update(medication: Medication) {
        medications.update { list -> list.map { if (it.id == medication.id) medication else it } }
    }

    override suspend fun findById(id: Long): Medication? = medications.value.find { it.id == id }

    override fun observeActive(userId: Long): Flow<List<Medication>> =
        // 진짜 조회문의 ORDER BY name COLLATE NOCASE(대소문자 무시 이름 순)를 흉내 낸다.
        medications.map { list ->
            list.filter { it.userId == userId && it.deletedAt == null }.sortedBy { it.name.lowercase() }
        }

    override suspend fun findActive(userId: Long): List<Medication> =
        medications.value.filter { it.userId == userId && it.deletedAt == null }

    override suspend fun softDelete(id: Long, deletedAt: Long) {
        medications.update { list -> list.map { if (it.id == id) it.copy(deletedAt = deletedAt) else it } }
    }

    override suspend fun setPausedFrom(userId: Long, medicationId: Long, date: String?): Int {
        var changed = 0
        medications.update { list ->
            list.map {
                if (it.id == medicationId && it.userId == userId && it.deletedAt == null) {
                    changed++
                    it.copy(pausedFrom = date)
                } else it
            }
        }
        return changed
    }

    override suspend fun deleteAllForUser(userId: Long) {
        medications.update { list -> list.filterNot { it.userId == userId } }
    }
}

/** 메모리의 목록에만 저장하는 가짜 ScheduleDao. 주인을 가려내려고 약 목록을 함께 본다. */
internal class FakeScheduleDao(private val medicationDao: FakeMedicationDao) : ScheduleDao {
    val schedules = MutableStateFlow<List<Schedule>>(emptyList())
    private var lastId = 0L

    private fun ownerOf(schedule: Schedule): Long? =
        medicationDao.medications.value.find { it.id == schedule.medicationId }?.userId

    override suspend fun insert(schedule: Schedule): Long {
        val newId = ++lastId
        schedules.update { it + schedule.copy(id = newId) }
        return newId
    }

    override suspend fun update(schedule: Schedule) {
        schedules.update { list -> list.map { if (it.id == schedule.id) schedule else it } }
    }

    override suspend fun findActiveByMedication(medicationId: Long): List<Schedule> =
        schedules.value
            .filter { it.medicationId == medicationId && it.deletedAt == null }
            .sortedBy { it.time }

    override fun observeActiveByUser(userId: Long): Flow<List<Schedule>> =
        schedules.map { list ->
            val myMedicationIds = medicationDao.medications.value
                .filter { it.userId == userId && it.deletedAt == null }
                .map { it.id }
            list.filter { it.medicationId in myMedicationIds && it.deletedAt == null }.sortedBy { it.time }
        }

    override suspend fun findReminderRows(userId: Long): List<ReminderRow> =
        schedules.value.filter { it.deletedAt == null }.mapNotNull { schedule ->
            val medication = medicationDao.medications.value.find { it.id == schedule.medicationId }
                ?: return@mapNotNull null
            if (medication.userId != userId || medication.deletedAt != null) return@mapNotNull null
            if (medication.pausedFrom != null) return@mapNotNull null
            ReminderRow(
                scheduleId = schedule.id,
                medicationId = medication.id,
                medicationName = medication.name,
                dosage = medication.dosage,
                startDate = medication.startDate,
                endDate = medication.endDate,
                time = schedule.time,
                repeatDays = schedule.repeatDays,
                effectiveFrom = schedule.effectiveFrom
            )
        }

    override suspend fun softDelete(id: Long, deletedAt: Long) {
        schedules.update { list -> list.map { if (it.id == id) it.copy(deletedAt = deletedAt) else it } }
    }

    override suspend fun softDeleteByMedication(medicationId: Long, deletedAt: Long) {
        schedules.update { list ->
            list.map {
                if (it.medicationId == medicationId && it.deletedAt == null) it.copy(deletedAt = deletedAt) else it
            }
        }
    }

    override suspend fun raiseEffectiveFrom(userId: Long, medicationId: Long, date: String): Int {
        var changed = 0
        schedules.update { list ->
            list.map {
                val raise = it.medicationId == medicationId && ownerOf(it) == userId && it.deletedAt == null &&
                    (it.effectiveFrom ?: "") < date
                if (raise) {
                    changed++
                    it.copy(effectiveFrom = date)
                } else it
            }
        }
        return changed
    }

    override suspend fun deleteAllForUser(userId: Long) {
        schedules.update { list -> list.filterNot { ownerOf(it) == userId } }
    }
}

/**
 * 메모리의 목록에만 저장하는 가짜 DoseRecordDao.
 * 주인을 확인하려면 규칙과 약이 필요해서 두 가짜를 함께 본다.
 * 약 이름·복용량은 진짜 조회문처럼 기록의 스냅샷 칸에서 읽는다.
 */
internal class FakeDoseRecordDao(
    private val medicationDao: FakeMedicationDao,
    private val scheduleDao: FakeScheduleDao
) : DoseRecordDao {
    val records = MutableStateFlow<List<DoseRecord>>(emptyList())

    /** 한 번이라도 매긴 적 있는 id 중 가장 큰 값. 줄을 지워도 id를 다시 쓰지 않게 한다. */
    private var lastId = 0L

    /** 기록이 속한 규칙의 약을 찾는다. (삭제된 규칙·약도 찾는다) */
    private fun medicationOf(record: DoseRecord): Medication? {
        val schedule = scheduleDao.schedules.value.find { it.id == record.scheduleId } ?: return null
        return medicationDao.medications.value.find { it.id == schedule.medicationId }
    }

    private fun isMine(record: DoseRecord, userId: Long) = medicationOf(record)?.userId == userId

    override suspend fun insertAllIgnore(records: List<DoseRecord>) {
        records.forEach { new ->
            // 같은 (규칙, 날짜)가 이미 있으면 건너뛴다 = 진짜 표의 UNIQUE + IGNORE
            val exists = this.records.value.any { it.scheduleId == new.scheduleId && it.date == new.date }
            if (!exists) this.records.update { it + new.copy(id = ++lastId) }
        }
    }

    override suspend fun lastDate(scheduleId: Long): String? =
        records.value.filter { it.scheduleId == scheduleId }.maxOfOrNull { it.date }

    override fun observeRange(userId: Long, fromDate: String, toDate: String): Flow<List<DoseRecordRow>> =
        records.map { list ->
            list.mapNotNull { record ->
                val medication = medicationOf(record) ?: return@mapNotNull null
                if (medication.userId != userId || record.date !in fromDate..toDate) return@mapNotNull null
                DoseRecordRow(
                    id = record.id,
                    scheduleId = record.scheduleId,
                    date = record.date,
                    scheduledTime = record.scheduledTime,
                    status = record.status,
                    actedAt = record.actedAt,
                    medicationId = medication.id,
                    medicationName = record.medicationName,   // 스냅샷
                    dosage = record.dosage,                   // 스냅샷
                    memo = record.memo                        // 스냅샷
                )
            }.sortedWith(compareBy({ it.date }, { it.scheduledTime }, { it.medicationName.lowercase() }))
        }

    override fun observeOwnedRowByKey(userId: Long, scheduleId: Long, date: String): Flow<DoseRecordRow?> =
        observeRange(userId, date, date).map { rows -> rows.find { it.scheduleId == scheduleId } }

    override suspend fun findOwned(userId: Long, recordId: Long): DoseRecord? =
        records.value.find { it.id == recordId && isMine(it, userId) }

    override suspend fun findOwnedByKey(userId: Long, scheduleId: Long, date: String): DoseRecord? =
        records.value.find { it.scheduleId == scheduleId && it.date == date && isMine(it, userId) }

    override suspend fun updateStatus(userId: Long, recordId: Long, status: String, actedAt: Long?): Int {
        var changed = 0
        records.update { list ->
            list.map {
                if (it.id == recordId && isMine(it, userId)) {
                    changed++
                    it.copy(status = status, actedAt = actedAt)
                } else it
            }
        }
        return changed
    }

    override suspend fun findUpcomingPending(
        userId: Long,
        medicationId: Long,
        date: String,
        afterTime: String
    ): List<DoseRecord> =
        records.value.filter { record ->
            val medication = medicationOf(record)
            medication != null && medication.userId == userId && medication.id == medicationId &&
                record.date == date && record.status == "PENDING" && record.scheduledTime > afterTime
        }

    override suspend fun updateUpcomingSnapshot(
        userId: Long,
        recordId: Long,
        time: String,
        name: String,
        dosage: String,
        memo: String?
    ): Int {
        var changed = 0
        records.update { list ->
            list.map {
                if (it.id == recordId && it.status == "PENDING" && isMine(it, userId)) {
                    changed++
                    it.copy(scheduledTime = time, medicationName = name, dosage = dosage, memo = memo)
                } else it
            }
        }
        return changed
    }

    override suspend fun findPendingAfter(userId: Long, medicationId: Long, endDate: String): List<Long> =
        records.value.filter { record ->
            val medication = medicationOf(record)
            medication != null && medication.userId == userId && medication.id == medicationId &&
                record.date > endDate && record.status == "PENDING"
        }.map { it.id }

    override suspend fun deleteByIds(userId: Long, recordIds: List<Long>) {
        records.update { list -> list.filterNot { it.id in recordIds && isMine(it, userId) } }
    }

    override suspend fun deleteAllForUser(userId: Long) {
        records.update { list -> list.filterNot { isMine(it, userId) } }
    }
}

/** 메모리의 목록에만 저장하는 가짜 DoseStatusEventDao. */
internal class FakeDoseStatusEventDao : DoseStatusEventDao {
    val events = MutableStateFlow<List<DoseStatusEvent>>(emptyList())
    private var lastId = 0L

    override suspend fun insert(event: DoseStatusEvent): Long {
        val newId = ++lastId
        events.update { it + event.copy(id = newId) }
        return newId
    }

    override suspend fun latestFor(userId: Long, recordId: Long): DoseStatusEvent? =
        events.value
            .filter { it.userId == userId && it.doseRecordId == recordId }
            .sortedWith(compareByDescending<DoseStatusEvent> { it.changedAt }.thenByDescending { it.id })
            .firstOrNull()

    override fun observeForUser(userId: Long): Flow<List<DoseStatusEvent>> =
        events.map { list ->
            list.filter { it.userId == userId }
                .sortedWith(compareByDescending<DoseStatusEvent> { it.changedAt }.thenByDescending { it.id })
        }

    override suspend fun findAllForUser(userId: Long): List<DoseStatusEvent> =
        events.value.filter { it.userId == userId }.sortedWith(compareBy({ it.changedAt }, { it.id }))

    override suspend fun detachRecords(userId: Long, recordIds: List<Long>) {
        events.update { list ->
            list.map {
                val linked = recordIds.contains(it.doseRecordId ?: -1L)
                if (it.userId == userId && linked) it.copy(doseRecordId = null) else it
            }
        }
    }

    override suspend fun deleteAllForUser(userId: Long) {
        events.update { list -> list.filterNot { it.userId == userId } }
    }
}

/** 트랜잭션 없이 그냥 순서대로 실행만 하는 가짜. */
internal class FakeTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}

/**
 * 진짜 트랜잭션처럼 "중간에 오류가 나면 전부 없던 일로" 하는 가짜.
 * 시작할 때 가짜 표들의 목록을 사진 찍듯 저장해 두고, 오류가 나면 그 목록으로 되돌린 뒤 오류를 다시 던진다.
 * "기록 고치기 + 이력 쓰기가 한 묶음"이라는 약속을 단위 테스트로 확인하려고 쓴다.
 *
 * @param tables 되돌릴 가짜 표들의 목록(MutableStateFlow)
 */
internal class RollbackTransactionRunner(
    private vararg val tables: MutableStateFlow<out List<Any>>
) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T {
        val snapshot = tables.map { it.value }
        try {
            return block()
        } catch (e: Exception) {
            tables.forEachIndexed { i, table -> restore(table, snapshot[i]) }
            throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun restore(table: MutableStateFlow<out List<Any>>, saved: List<Any>) {
        (table as MutableStateFlow<List<Any>>).value = saved
    }
}

/** 변수 하나에만 저장하는 가짜 SessionStore. */
internal class FakeSessionStore : SessionStore {
    private var userId: Long? = null

    override fun getLoggedInUserId(): Long? = userId

    override fun saveLoggedInUserId(userId: Long) {
        this.userId = userId
    }

    override fun clear() {
        userId = null
    }
}
