package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.DoseRecordDao
import com.cau.medicheck.data.local.DoseStatusEventDao
import com.cau.medicheck.data.local.MedicationDao
import com.cau.medicheck.data.local.ScheduleDao
import com.cau.medicheck.data.local.UserDao
import com.cau.medicheck.data.local.UserSettingsDao

/**
 * 사용자 한 명의 데이터를 DB에서 모두 지우는 도구. (DB_DESIGN ⑧ "명시적 삭제 순서")
 *
 * 쓰는 곳이 두 군데라 한곳에 모았다: 계정·데이터 삭제(S44), 샘플 계정 다시 만들기.
 * 순서가 중요하다: 이력 → 기록 → 규칙 → 약 → 설정 → 사용자.
 * 가리키는 쪽(자식)을 먼저 지우므로, FK의 CASCADE(부모를 지우면 자식도 지워지는 규칙)가 꺼져 있어도 결과가 같다.
 *
 * 트랜잭션(전부 되거나 전부 안 되거나)은 부르는 쪽이 감싼다. 이 함수만 따로 부르면 중간에 멈출 수 있다.
 */
class UserDataEraser(
    private val statusEventDao: DoseStatusEventDao,
    private val doseRecordDao: DoseRecordDao,
    private val scheduleDao: ScheduleDao,
    private val medicationDao: MedicationDao,
    private val userSettingsDao: UserSettingsDao,
    private val userDao: UserDao
) {
    suspend fun erase(userId: Long) {
        statusEventDao.deleteAllForUser(userId)
        doseRecordDao.deleteAllForUser(userId)
        scheduleDao.deleteAllForUser(userId)
        medicationDao.deleteAllForUser(userId)
        userSettingsDao.deleteForUser(userId)
        userDao.deleteById(userId)
    }
}
