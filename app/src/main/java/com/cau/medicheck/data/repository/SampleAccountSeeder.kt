package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecord
import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.DoseStatusEventDao
import com.cau.medicheck.data.local.Medication
import com.cau.medicheck.data.local.MedicationDao
import com.cau.medicheck.data.local.Schedule
import com.cau.medicheck.data.local.ScheduleDao
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.data.local.User
import com.cau.medicheck.data.local.UserDao
import com.cau.medicheck.data.local.UserSettingsDao
import com.cau.medicheck.data.local.UserSettingsEntity
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.domain.PasswordHasher
import com.cau.medicheck.domain.SampleData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * 샘플 계정을 준비한다. (DB_DESIGN 부록 D, 01 명세 부록 C)
 *
 * - 오늘 만든 샘플 계정이 있으면 그대로 쓴다. 같은 날 안에서는 샘플 계정에서 바꾼 내용이 남는다
 *   (로그아웃했다 다시 들어가도 "등록·수정이 실제로 저장되는지" 확인할 수 있게).
 * - 없으면(처음이거나 날짜가 바뀜) 옛 샘플 계정을 지우고 오늘 기준으로 새로 만든다.
 *   날이 지나면 그 사이 기록이 비어 '기한 경과'만 쌓이기 때문이다.
 *
 * 지우기·만들기는 한 트랜잭션이다. 중간에 실패하면 전부 없던 일이 된다.
 *
 * @param clock "오늘"을 정하는 시계. 테스트에서는 멈춘 시계를 넣는다.
 * @param isKorean 기기 언어가 한국어인가. 아니면 샘플 약 이름을 영어로 만든다.
 */
class SampleAccountSeeder(
    private val userDao: UserDao,
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao,
    private val doseRecordDao: DoseRecordDao,
    private val statusEventDao: DoseStatusEventDao,
    private val userSettingsDao: UserSettingsDao,
    private val transaction: TransactionRunner,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val hashDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val isKorean: () -> Boolean = { true }
) {

    /** 오늘 만든 샘플 계정이 있으면 그대로, 없으면 지우고 새로 만들어 돌려준다. */
    suspend fun prepare(): User {
        val today = LocalDate.now(clock)
        userDao.findSampleUser()?.let { existing ->
            val madeOn = Instant.ofEpochMilli(existing.createdAt).atZone(clock.zone).toLocalDate()
            if (madeOn == today) return existing
        }

        // 아무도 모르는 무작위 비밀번호의 해시 → 로그인 폼으로는 들어갈 수 없고 샘플 버튼으로만 들어간다.
        // 해시는 느린 계산이라 트랜잭션 밖에서 미리 해 둔다(트랜잭션을 짧게).
        val hash = withContext(hashDispatcher) { PasswordHasher.hash(UUID.randomUUID().toString()) }

        return transaction.run {
            userDao.findSampleUsers().forEach { eraser.erase(it.id) }
            createSample(today, hash)
        }
    }

    /** 옛 샘플 계정의 데이터를 지우는 도구 (계정·데이터 삭제 S44와 같은 순서를 쓴다) */
    private val eraser =
        UserDataEraser(statusEventDao, doseRecordDao, scheduleDao, medicationDao, userSettingsDao, userDao)

    /** 새 샘플 계정과 약 5개·규칙 5개·기록 165줄·설정 기본 줄을 만든다. 이력은 만들지 않는다(v3 auditEvents가 빈 목록). */
    private suspend fun createSample(today: LocalDate, passwordHash: String): User {
        val now = clock.millis()
        val user = User(
            email = SampleData.EMAIL,
            passwordHash = passwordHash,
            displayName = SampleData.displayName(isKorean()),
            createdAt = now,
            isSample = true
        )
        val userId = userDao.insert(user)
        userSettingsDao.insert(UserSettingsEntity.defaultsFor(userId, clock.zone.id))

        val startDate = today.plusDays(SampleData.START_OFFSET.toLong()).toString()
        SampleData.meds(isKorean()).forEach { med ->
            val medicationId = medicationDao.insert(
                Medication(
                    userId = userId, name = med.name, dosage = med.dosage,
                    startDate = startDate, endDate = null, memo = null, createdAt = now,
                    shape = med.shape.name, color = med.color.name
                )
            )
            val scheduleId = scheduleDao.insert(
                Schedule(
                    medicationId = medicationId, time = med.time,
                    repeatDays = SampleData.REPEAT_DAYS, createdAt = now,
                    doseCondition = med.condition.name
                )
            )
            doseRecordDao.insertAllIgnore(
                SampleData.DAY_OFFSETS.map { offset ->
                    val date = today.plusDays(offset.toLong())
                    val status = SampleData.statusFor(offset, med.no)
                    // 복용 완료 기록에만 "먹은 시각"을 넣는다: 예정 시각에서 몇 분 늦게(또는 일찍)
                    val delay = SampleData.actedDelayMinutes(offset, med.no).takeIf { status == DoseStatus.TAKEN }
                    DoseRecord(
                        scheduleId = scheduleId,
                        date = date.toString(),
                        scheduledTime = med.time,
                        medicationName = med.name,   // 스냅샷: 그때의 약 값
                        dosage = med.dosage,
                        memo = null,
                        status = status.name,
                        actedAt = delay?.let {
                            date.atTime(LocalTime.parse(med.time)).plusMinutes(it.toLong()).atZone(clock.zone).toInstant().toEpochMilli()
                        },
                        createdAt = now,
                        doseCondition = med.condition.name
                    )
                }
            )
        }
        return user.copy(id = userId)
    }
}
