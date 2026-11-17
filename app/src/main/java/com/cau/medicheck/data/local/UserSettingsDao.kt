package com.cau.medicheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * user_settings 표의 창구.
 * 1단계에서는 가입·샘플 생성 때 기본 줄을 넣는 insert와, 샘플 다시 만들기의 deleteForUser만 쓴다.
 * 읽기·고치기(observe, find, upsert)는 5단계 SettingsRepository가 쓴다.
 */
@Dao
interface UserSettingsDao {

    @Insert
    suspend fun insert(settings: UserSettingsEntity)

    /**
     * Upsert = update + insert: 이 사용자의 줄이 있으면 고치고, 없으면 새로 넣는다 (Room 2.5부터 있는 기능).
     * 개발 중에 만든 계정처럼 설정 줄이 없는 경우에도 저장이 되게 하려고 쓴다.
     */
    @Upsert
    suspend fun upsert(settings: UserSettingsEntity)

    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    suspend fun find(userId: Long): UserSettingsEntity?

    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    fun observe(userId: Long): Flow<UserSettingsEntity?>

    /** 샘플 계정 다시 만들기: 사용자 줄을 지우기 직전에 설정 줄을 지운다 (명시적 삭제 순서). */
    @Query("DELETE FROM user_settings WHERE user_id = :userId")
    suspend fun deleteForUser(userId: Long)
}
