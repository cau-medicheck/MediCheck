package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.User
import com.cau.medicheck.data.local.UserDao
import com.cau.medicheck.domain.AuthError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AuthRepository 단위 테스트.
 *
 * 진짜 DB와 폰 저장공간 대신, 메모리에만 저장하는 가짜(Fake)를 끼워서
 * "회원가입·로그인의 순서"가 맞는지만 확인한다. 그래서 폰 없이 컴퓨터에서 실행된다.
 * (runBlocking: 테스트 안에서 suspend 함수를 끝날 때까지 기다리며 실행하는 방법)
 */
class AuthRepositoryTest {

    private lateinit var userDao: FakeUserDao
    private lateinit var sessionStore: FakeSessionStore
    private lateinit var repository: AuthRepository

    /** 각 테스트가 시작되기 전에 매번 실행된다. 테스트끼리 데이터가 섞이지 않게 새로 만든다. */
    @Before
    fun setUp() {
        userDao = FakeUserDao()
        sessionStore = FakeSessionStore()
        repository = AuthRepository(userDao, sessionStore, now = { 1_000L })
    }

    private suspend fun signUpSuhwan() =
        repository.signUp("suhwan", "password1", "password1", "전수환")

    // ---------- 회원가입 ----------

    @Test
    fun `가입에 성공하면 사용자가 저장되고 로그인 상태가 된다`() = runBlocking<Unit> {
        val result = signUpSuhwan()

        assertTrue(result is AuthResult.Success)
        val user = (result as AuthResult.Success).user
        assertEquals("suhwan", user.username)
        assertEquals("전수환", user.displayName)
        assertEquals(1_000L, user.createdAt)
        assertEquals(user.id, sessionStore.getLoggedInUserId())
        assertEquals(1, userDao.users.size)
    }

    @Test
    fun `비밀번호는 원래 글자 그대로 저장되지 않는다`() = runBlocking<Unit> {
        signUpSuhwan()

        val saved = userDao.users.single()
        assertNotEquals("password1", saved.passwordHash)
        assertTrue(saved.passwordHash.startsWith("pbkdf2$"))
    }

    @Test
    fun `이미 있는 아이디로는 가입할 수 없다`() = runBlocking<Unit> {
        signUpSuhwan()
        sessionStore.clear()

        val result = repository.signUp("SuHwan", "another-pw", "another-pw", "다른 사람")

        assertEquals(AuthResult.Failure(AuthError.USERNAME_TAKEN), result)
        assertEquals(1, userDao.users.size)
        assertNull(sessionStore.getLoggedInUserId())
    }

    @Test
    fun `입력값이 규칙에 맞지 않으면 저장하지 않는다`() = runBlocking<Unit> {
        val result = repository.signUp("suhwan", "short", "short", "전수환")

        assertEquals(AuthResult.Failure(AuthError.PASSWORD_TOO_SHORT), result)
        assertTrue(userDao.users.isEmpty())
        assertNull(sessionStore.getLoggedInUserId())
    }

    // ---------- 로그인 ----------

    @Test
    fun `올바른 아이디와 비밀번호로 로그인된다`() = runBlocking<Unit> {
        signUpSuhwan()
        repository.logout()

        val result = repository.login("suhwan", "password1")

        assertTrue(result is AuthResult.Success)
        assertEquals(userDao.users.single().id, sessionStore.getLoggedInUserId())
    }

    @Test
    fun `아이디는 대소문자와 앞뒤 공백을 구분하지 않는다`() = runBlocking<Unit> {
        signUpSuhwan()
        repository.logout()

        val result = repository.login("  SUHWAN ", "password1")

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun `비밀번호가 틀리면 로그인되지 않는다`() = runBlocking<Unit> {
        signUpSuhwan()
        repository.logout()

        val result = repository.login("suhwan", "password2")

        assertEquals(AuthResult.Failure(AuthError.WRONG_CREDENTIALS), result)
        assertNull(sessionStore.getLoggedInUserId())
    }

    @Test
    fun `없는 아이디도 비밀번호가 틀린 것과 같은 오류를 돌려준다`() = runBlocking<Unit> {
        val result = repository.login("nobody", "password1")

        assertEquals(AuthResult.Failure(AuthError.WRONG_CREDENTIALS), result)
    }

    @Test
    fun `칸이 비어 있으면 DB를 찾아보지 않고 바로 실패한다`() = runBlocking<Unit> {
        val result = repository.login("", "")

        assertEquals(AuthResult.Failure(AuthError.EMPTY_FIELDS), result)
    }

    // ---------- 로그인 상태 유지 · 로그아웃 ----------

    @Test
    fun `로그인해 둔 사용자가 있으면 currentUser가 돌려준다`() = runBlocking<Unit> {
        signUpSuhwan()

        assertEquals("suhwan", repository.currentUser()?.username)
    }

    @Test
    fun `로그아웃하면 currentUser는 null이다`() = runBlocking<Unit> {
        signUpSuhwan()

        repository.logout()

        assertNull(repository.currentUser())
        assertNull(sessionStore.getLoggedInUserId())
    }

    @Test
    fun `기억해 둔 사용자가 DB에 없으면 기억을 지운다`() = runBlocking<Unit> {
        sessionStore.saveLoggedInUserId(999L)

        assertNull(repository.currentUser())
        assertNull(sessionStore.getLoggedInUserId())
    }
}

/** 메모리의 목록에만 저장하는 가짜 UserDao. id는 1부터 차례로 매긴다. */
private class FakeUserDao : UserDao {
    val users = mutableListOf<User>()

    override suspend fun insert(user: User): Long {
        val newId = users.size + 1L
        users.add(user.copy(id = newId))
        return newId
    }

    override suspend fun findByUsername(username: String): User? =
        users.find { it.username == username }

    override suspend fun findById(id: Long): User? =
        users.find { it.id == id }
}
