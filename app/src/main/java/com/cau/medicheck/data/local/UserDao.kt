package com.cau.medicheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * users 표에 넣고 꺼내는 창구.
 * 함수 이름만 적어 두면, 실제로 동작하는 코드는 Room이 빌드할 때 만들어 준다.
 */
@Dao
interface UserDao {

    /** 사용자 한 명을 저장하고, 새로 매겨진 id를 돌려준다. (회원가입) */
    @Insert
    suspend fun insert(user: User): Long

    /** id가 같은 줄을 통째로 바꾼다. (프로필 수정) */
    @Update
    suspend fun update(user: User)

    /** 이메일로 사용자를 찾는다. 없으면 null. (로그인, 이메일 중복 확인) */
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun findByEmail(email: String): User?

    /** 번호(id)로 사용자를 찾는다. 없으면 null. (앱을 다시 켰을 때 로그인 상태 복원) */
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: Long): User?

    /** 가장 최근에 만든 샘플 계정 하나. 없으면 null. ("오늘 만든 샘플이 있나" 확인) */
    @Query("SELECT * FROM users WHERE is_sample = 1 ORDER BY id DESC LIMIT 1")
    suspend fun findSampleUser(): User?

    /**
     * 샘플 계정 전부. 샘플은 보통 하나뿐이지만, 다시 만들 때 옛 샘플을 빠짐없이 지우려고 목록으로 읽는다.
     */
    @Query("SELECT * FROM users WHERE is_sample = 1")
    suspend fun findSampleUsers(): List<User>

    /**
     * 사용자 한 줄을 지운다. (샘플 계정 다시 만들기의 마지막 단계)
     *
     * 이 사용자의 약·기록·이력·설정은 이 함수를 부르기 **전에** 각 DAO의 deleteAllForUser로 먼저 지운다.
     * FK의 CASCADE(부모 줄이 지워지면 딸린 줄도 지워지는 규칙)에 기대지 않기 위해서다 (DB_DESIGN ⑧).
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: Long)
}
