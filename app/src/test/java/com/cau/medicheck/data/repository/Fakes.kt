package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecord
import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.DoseRecordRow
import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.MedicationDao
import com.cau.medicheck.data.local.ReminderRow
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.data.local.ScheduleDao
import com.cau.medicheck.data.local.SessionStore
import com.cau.medicheck.data.local.TransactionRunner
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
 * 각 가짜는 진짜 표의 조회문(SQL)이 하는 일을 Kotlin 코드로 흉내 낸다.
 */

/** 메모리의 목록에만 저장하는 가짜 MedicationDao. id는 1부터 차례로 매긴다. */
internal class FakeMedicationDao : MedicationDao {
    val medications = MutableStateFlow<List<Medication>>(emptyList())

    override suspend fun insert(medication: Medication): Long {
        val newId = medications.value.size + 1L
        medications.update { it + medication.copy(id = newId) }
        return newId
    }

    override suspend fun update(medication: Medication) {
        medications.update { list -> list.map { if (it.id == medication.id) medication else it } }
    }

    override suspend fun findById(id: Long): Medication? = medications.value.find { it.id == id }

    override fun observeActive(userId: Long): Flow<List<Medication>> =
        medications.map { list -> list.filter { it.userId == userId && it.deletedAt == null } }

    override suspend fun findActive(userId: Long): List<Medication> =
        medications.value.filter { it.userId == userId && it.deletedAt == null }

    override suspend fun softDelete(id: Long, deletedAt: Long) {
        medications.update { list -> list.map { if (it.id == id) it.copy(deletedAt = deletedAt) else it } }
    }
}

/** 메모리의 목록에만 저장하는 가짜 ScheduleDao. 주인을 가려내려고 약 목록을 함께 본다. */
internal class FakeScheduleDao(private val medicationDao: FakeMedicationDao) : ScheduleDao {
    val schedules = MutableStateFlow<List<Schedule>>(emptyList())

    override suspend fun insert(schedule: Schedule): Long {
        val newId = schedules.value.size + 1L
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
            ReminderRow(
                scheduleId = schedule.id,
                medicationId = medication.id,
                medicationName = medication.name,
                dosage = medication.dosage,
                startDate = medication.startDate,
                endDate = medication.endDate,
                time = schedule.time,
                repeatDays = schedule.repeatDays
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
}

/**
 * 메모리의 목록에만 저장하는 가짜 DoseRecordDao.
 * 기록에 약 이름을 붙이거나 주인을 확인하려면 규칙과 약이 필요해서 두 가짜를 함께 본다.
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

    override suspend fun insertAllIgnore(records: List<DoseRecord>) {
        records.forEach { new ->
            // 같은 (규칙, 날짜)가 이미 있으면 건너뛴다 = 진짜 표의 UNIQUE + IGNORE
            val exists = this.records.value.any { it.scheduleId == new.scheduleId && it.date == new.date }
            if (!exists) this.records.update { it + new.copy(id = ++lastId) }
        }
    }

    override suspend fun statusOf(scheduleId: Long, date: String): String? =
        records.value.find { it.scheduleId == scheduleId && it.date == date }?.status

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
                    medicationName = medication.name,
                    dosage = medication.dosage
                )
            }.sortedWith(compareBy({ it.date }, { it.scheduledTime }, { it.medicationName.lowercase() }))
        }

    override suspend fun findOwned(recordId: Long, userId: Long): DoseRecord? =
        records.value.find { it.id == recordId && medicationOf(it)?.userId == userId }

    override suspend fun updateStatus(recordId: Long, status: String, actedAt: Long?) {
        records.update { list ->
            list.map { if (it.id == recordId) it.copy(status = status, actedAt = actedAt) else it }
        }
    }

    override suspend fun deleteUpcomingPending(medicationId: Long, date: String, afterTime: String) {
        records.update { list ->
            list.filterNot { record ->
                record.date == date &&
                    record.status == "PENDING" &&
                    record.scheduledTime > afterTime &&
                    medicationOf(record)?.id == medicationId
            }
        }
    }
}

/** 트랜잭션 없이 그냥 순서대로 실행만 하는 가짜. */
internal class FakeTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
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
