package com.cau.medicheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * users 표에 넣고 꺼내는 창구.
 * 함수 이름만 적어 두면, 실제로 동작하는 코드는 Room이 빌드할 때 만들어 준다.
 */
@Dao
interface UserDao {

    /** 사용자 한 명을 저장하고, 새로 매겨진 id를 돌려준다. (회원가입) */
    @Insert
    suspend fun insert(user: User): Long

    /** 아이디로 사용자를 찾는다. 없으면 null. (로그인, 아이디 중복 확인) */
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun findByUsername(username: String): User?

    /** 번호(id)로 사용자를 찾는다. 없으면 null. (앱을 다시 켰을 때 로그인 상태 복원) */
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: Long): User?
}
