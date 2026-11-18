package com.cau.medicheck.data.repository

import com.cau.medicheck.data.local.User
import com.cau.medicheck.data.local.UserSettingsEntity
import com.cau.medicheck.domain.AuthError
import com.cau.medicheck.domain.SampleData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

/**
 * AuthRepository 단위 테스트.
 *
 * 진짜 DB와 폰 저장공간 대신, 메모리에만 저장하는 가짜(Fake)를 끼워서
 * "회원가입·로그인의 순서"가 맞는지만 확인한다. 그래서 폰 없이 컴퓨터에서 실행된다.
 * (runBlocking: 테스트 안에서 suspend 함수를 끝날 때까지 기다리며 실행하는 방법)
 */
class AuthRepositoryTest {

    private lateinit var userDao: FakeUserDao
    private lateinit var settingsDao: FakeUserSettingsDao
    private lateinit var sessionStore: FakeSessionStore
    private lateinit var repository: AuthRepository

    /** 샘플 계정 준비 함수 대신 끼우는 가짜: 샘플 사용자 한 명을 넣고 돌려준다. */
    private var sampleCalls = 0

    /** 각 테스트가 시작되기 전에 매번 실행된다. 테스트끼리 데이터가 섞이지 않게 새로 만든다. */
    @Before
    fun setUp() {
        userDao = FakeUserDao()
        settingsDao = FakeUserSettingsDao()
        sessionStore = FakeSessionStore()
        sampleCalls = 0
        repository = AuthRepository(
            userDao = userDao,
            userSettingsDao = settingsDao,
            sessionStore = sessionStore,
            transaction = FakeTransactionRunner(),
            now = { 1_000L },
            zoneId = { ZoneId.of("Asia/Seoul") },
            prepareSampleUser = {
                sampleCalls++
                val sample = User(
                    email = SampleData.EMAIL, passwordHash = "x", displayName = SampleData.DISPLAY_NAME,
                    createdAt = 1_000L, isSample = true
                )
                sample.copy(id = userDao.insert(sample))
            }
        )
    }

    private suspend fun signUpSuhwan() =
        repository.signUp("전수환", "suhwan@cau.ac.kr", "password1", "password1")

    // ---------- 회원가입 ----------

    @Test
    fun `가입에 성공하면 사용자가 저장되고 로그인 상태가 된다`() = runBlocking<Unit> {
        val result = signUpSuhwan()

        assertTrue(result is AuthResult.Success)
        val user = (result as AuthResult.Success).user
        assertEquals("suhwan@cau.ac.kr", user.email)
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
    fun `이미 가입된 이메일로는 가입할 수 없다`() = runBlocking<Unit> {
        signUpSuhwan()
        sessionStore.clear()

        val result = repository.signUp("다른 사람", "SuHwan@CAU.ac.kr", "another-pw", "another-pw")

        assertEquals(AuthResult.Failure(AuthError.EMAIL_TAKEN), result)
        assertEquals(1, userDao.users.size)
        assertNull(sessionStore.getLoggedInUserId())
    }

    @Test
    fun `입력값이 규칙에 맞지 않으면 저장하지 않는다`() = runBlocking<Unit> {
        val result = repository.signUp("전수환", "suhwan@cau.ac.kr", "short", "short")

        assertEquals(AuthResult.Failure(AuthError.PASSWORD_TOO_SHORT), result)
        assertTrue(userDao.users.isEmpty())
        assertNull(sessionStore.getLoggedInUserId())
    }

    // ---------- 로그인 ----------

    @Test
    fun `올바른 이메일과 비밀번호로 로그인된다`() = runBlocking<Unit> {
        signUpSuhwan()
        repository.logout()

        val result = repository.login("suhwan@cau.ac.kr", "password1")

        assertTrue(result is AuthResult.Success)
        assertEquals(userDao.users.single().id, sessionStore.getLoggedInUserId())
    }

    @Test
    fun `이메일은 대소문자와 앞뒤 공백을 구분하지 않는다`() = runBlocking<Unit> {
        signUpSuhwan()
        repository.logout()

        val result = repository.login("  SUHWAN@cau.ac.kr ", "password1")

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun `비밀번호가 틀리면 로그인되지 않는다`() = runBlocking<Unit> {
        signUpSuhwan()
        repository.logout()

        val result = repository.login("suhwan@cau.ac.kr", "password2")

        assertEquals(AuthResult.Failure(AuthError.WRONG_CREDENTIALS), result)
        assertNull(sessionStore.getLoggedInUserId())
    }

    @Test
    fun `없는 이메일도 비밀번호가 틀린 것과 같은 오류를 돌려준다`() = runBlocking<Unit> {
        val result = repository.login("nobody@cau.ac.kr", "password1")

        assertEquals(AuthResult.Failure(AuthError.WRONG_CREDENTIALS), result)
    }

    @Test
    fun `칸이 비어 있으면 DB를 찾아보지 않고 바로 실패한다`() = runBlocking<Unit> {
        assertEquals(AuthResult.Failure(AuthError.EMAIL_EMPTY), repository.login("", ""))
        assertEquals(AuthResult.Failure(AuthError.PASSWORD_EMPTY), repository.login("a@b.co", ""))
    }

    @Test
    fun `로그인도 이메일 형식과 비밀번호 길이를 먼저 검사한다`() = runBlocking<Unit> {
        assertEquals(AuthResult.Failure(AuthError.EMAIL_INVALID), repository.login("suhwan", "password1"))
        assertEquals(AuthResult.Failure(AuthError.PASSWORD_TOO_SHORT), repository.login("a@b.co", "12345"))
    }

    // ---------- 가입 때 설정 기본 줄 · 샘플 이메일 예약 ----------

    @Test
    fun `가입하면 그 계정의 설정 기본 줄이 함께 만들어진다`() = runBlocking<Unit> {
        val user = (signUpSuhwan() as AuthResult.Success).user

        val settings = settingsDao.rows.value.single()
        assertEquals(UserSettingsEntity.defaultsFor(user.id, "Asia/Seoul"), settings)
        assertTrue(settings.reminderEnabled)
        assertTrue(settings.hideMedName)
        assertEquals(15, settings.realertMinutes)
        assertEquals("LOCAL", settings.timeZoneMode)
    }

    @Test
    fun `가입이 실패하면 설정 줄도 만들지 않는다`() = runBlocking<Unit> {
        repository.signUp("전수환", "suhwan@cau.ac.kr", "short", "short")

        assertTrue(settingsDao.rows.value.isEmpty())
    }

    @Test
    fun `샘플 계정 이메일로는 가입할 수 없다`() = runBlocking<Unit> {
        val result = repository.signUp("나", " Demo@MediCheck.app ", "password1", "password1")

        assertEquals(AuthResult.Failure(AuthError.EMAIL_TAKEN), result)
        assertTrue(userDao.users.isEmpty())
    }

    @Test
    fun `샘플이 아닌 계정은 프로필 이메일을 샘플 이메일로 바꿀 수 없다`() = runBlocking<Unit> {
        val me = (signUpSuhwan() as AuthResult.Success).user

        val result = repository.updateProfile(me.id, "전수환", SampleData.EMAIL)

        assertEquals(AuthResult.Failure(AuthError.EMAIL_TAKEN), result)
    }

    @Test
    fun `샘플 계정은 자기 샘플 이메일을 그대로 두고 이름을 바꿀 수 있다`() = runBlocking<Unit> {
        val sampleId = userDao.insert(
            User(email = SampleData.EMAIL, passwordHash = "x", displayName = "사용자", createdAt = 0L, isSample = true)
        )

        val result = repository.updateProfile(sampleId, "새 이름", SampleData.EMAIL)

        assertTrue(result is AuthResult.Success)
        assertEquals("새 이름", userDao.users.single().displayName)
    }

    // ---------- 샘플 계정 ----------

    @Test
    fun `샘플 계정으로 체험하기는 샘플 계정을 준비해 로그인 상태로 만든다`() = runBlocking<Unit> {
        var sessionChanges = 0
        repository = AuthRepository(
            userDao, settingsDao, sessionStore, FakeTransactionRunner(),
            onSessionChanged = { sessionChanges++ },
            prepareSampleUser = { User(id = 7, email = SampleData.EMAIL, passwordHash = "x",
                displayName = "사용자", createdAt = 0, isSample = true) }
        )

        val result = repository.loginAsSample()

        assertTrue(result is AuthResult.Success)
        assertEquals(7L, sessionStore.getLoggedInUserId())
        assertEquals(1, sessionChanges)
    }

    @Test
    fun `샘플 계정은 로그인 폼으로는 들어갈 수 없다`() = runBlocking<Unit> {
        repository.loginAsSample()
        repository.logout()

        // 비밀번호는 아무도 모르는 값의 해시라서, 어떤 비밀번호로도 맞지 않는다
        assertEquals(
            AuthResult.Failure(AuthError.WRONG_CREDENTIALS),
            repository.login(SampleData.EMAIL, "password1")
        )
        assertEquals(1, sampleCalls)
    }

    // ---------- 로그인 상태 유지 · 로그아웃 ----------

    @Test
    fun `로그인해 둔 사용자가 있으면 currentUser가 돌려준다`() = runBlocking<Unit> {
        signUpSuhwan()

        assertEquals("suhwan@cau.ac.kr", repository.currentUser()?.email)
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

    // ---------- 계정·데이터 삭제 ----------

    /** 삭제 시험용 저장소: 가짜 DAO에서 그 사용자의 줄을 지우는 함수를 끼우고, 알림 정리 호출 횟수를 센다. */
    private fun repositoryForDelete(onSessionChanged: () -> Unit = {}) = AuthRepository(
        userDao, settingsDao, sessionStore, FakeTransactionRunner(),
        zoneId = { ZoneId.of("Asia/Seoul") },
        onSessionChanged = { onSessionChanged() },
        eraseUserData = { userId ->
            settingsDao.deleteForUser(userId)
            userDao.deleteById(userId)
        }
    )

    @Test
    fun `계정 삭제는 지금 로그인한 계정만 지우고 다른 계정은 남긴다`() = runBlocking<Unit> {
        var sessionChanges = 0
        repository = repositoryForDelete { sessionChanges++ }
        repository.signUp("다른 사람", "other@mail.com", "password1", "password1")
        signUpSuhwan()   // 지금 로그인한 사람 = 전수환
        sessionChanges = 0

        val deleted = repository.deleteCurrentAccount()

        assertTrue(deleted)
        assertEquals(listOf("other@mail.com"), userDao.users.map { it.email })
        assertEquals(1, settingsDao.rows.value.size)
        assertNull(sessionStore.getLoggedInUserId())
        assertEquals(1, sessionChanges)   // 알림 정리·알람 취소를 한 번 부른다
    }

    @Test
    fun `계정을 지운 뒤에는 그 이메일로 로그인할 수 없고 다시 가입할 수 있다`() = runBlocking<Unit> {
        repository = repositoryForDelete()
        signUpSuhwan()
        repository.deleteCurrentAccount()

        assertEquals(
            AuthResult.Failure(AuthError.WRONG_CREDENTIALS),
            repository.login("suhwan@cau.ac.kr", "password1")
        )
        assertTrue(signUpSuhwan() is AuthResult.Success)
    }

    @Test
    fun `로그인한 사람이 없으면 계정 삭제는 아무것도 하지 않는다`() = runBlocking<Unit> {
        var sessionChanges = 0
        repository = repositoryForDelete { sessionChanges++ }
        signUpSuhwan()
        repository.logout()
        sessionChanges = 0

        assertFalse(repository.deleteCurrentAccount())
        assertEquals(1, userDao.users.size)
        assertEquals(0, sessionChanges)
    }

    // ---------- 프로필 수정 ----------

    @Test
    fun `프로필의 이름과 이메일을 바꿀 수 있다`() = runBlocking<Unit> {
        val user = (signUpSuhwan() as AuthResult.Success).user

        val result = repository.updateProfile(user.id, "  수환  ", "New@Mail.com")

        assertTrue(result is AuthResult.Success)
        val saved = userDao.users.single()
        assertEquals("수환", saved.displayName)
        assertEquals("new@mail.com", saved.email)
        assertEquals(user.passwordHash, saved.passwordHash)
    }

    @Test
    fun `다른 사람이 쓰는 이메일로는 바꿀 수 없다`() = runBlocking<Unit> {
        val me = (signUpSuhwan() as AuthResult.Success).user
        repository.signUp("다른 사람", "other@mail.com", "password1", "password1")

        val result = repository.updateProfile(me.id, "전수환", "other@mail.com")

        assertEquals(AuthResult.Failure(AuthError.EMAIL_TAKEN), result)
    }

    @Test
    fun `자기 이메일 그대로 이름만 바꾸는 것은 된다`() = runBlocking<Unit> {
        val me = (signUpSuhwan() as AuthResult.Success).user

        val result = repository.updateProfile(me.id, "새 이름", "suhwan@cau.ac.kr")

        assertTrue(result is AuthResult.Success)
        assertEquals("새 이름", userDao.users.single().displayName)
    }

    @Test
    fun `프로필 입력값이 규칙에 맞지 않으면 바꾸지 않는다`() = runBlocking<Unit> {
        val me = (signUpSuhwan() as AuthResult.Success).user

        assertEquals(AuthResult.Failure(AuthError.DISPLAY_NAME_INVALID), repository.updateProfile(me.id, " ", "a@b.co"))
        assertEquals(AuthResult.Failure(AuthError.EMAIL_INVALID), repository.updateProfile(me.id, "이름", "not-an-email"))
        assertEquals("전수환", userDao.users.single().displayName)
    }
}
