package com.cau.medicheck.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import com.cau.medicheck.data.local.SessionStore
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.data.local.User
import com.cau.medicheck.data.local.UserDao
import com.cau.medicheck.data.local.UserSettingsDao
import com.cau.medicheck.data.local.UserSettingsEntity
import com.cau.medicheck.domain.AuthError
import com.cau.medicheck.domain.AuthValidator
import com.cau.medicheck.domain.PasswordHasher
import com.cau.medicheck.domain.SampleData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId

/** 회원가입·로그인의 결과. 성공이면 사용자, 실패면 이유가 들어 있다. */
sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

/**
 * 회원가입·로그인·로그아웃의 실제 순서를 담당한다.
 *
 * 화면(ViewModel)은 DB나 해시를 직접 다루지 않고 이 클래스의 함수만 부른다.
 *   화면 → AuthRepository → (검사: AuthValidator) (해시: PasswordHasher) (저장: UserDao) (로그인 기억: SessionStore)
 *
 * @param transaction 사용자 줄과 설정 기본 줄을 한 묶음으로 넣기 위한 트랜잭션 도구
 * @param hashDispatcher 해시 계산은 일부러 느리게 만든 계산이라, 화면이 멈추지 않도록
 *                       계산 전용 일꾼(Dispatchers.Default)에게 맡긴다.
 * @param now 현재 시각을 돌려주는 함수. 테스트에서 시각을 고정하려고 바꿔 끼울 수 있게 했다.
 * @param zoneId 지금 폰의 시간대. 가입 때 설정 줄의 home_zone_id에 적는다.
 * @param onSessionChanged 로그인·로그아웃으로 "지금 로그인한 사람"이 바뀐 뒤에 부를 함수.
 *                         앱에서는 "알림을 다시 예약하라(로그아웃이면 취소하라)"가 들어온다.
 * @param prepareSampleUser 샘플 계정을 준비해 돌려주는 함수(SampleAccountSeeder.prepare).
 *                          onSessionChanged처럼 함수로 받아, 테스트에서는 가짜로 바꿔 끼운다.
 */
class AuthRepository(
    private val userDao: UserDao,
    private val userSettingsDao: UserSettingsDao,
    private val sessionStore: SessionStore,
    private val transaction: TransactionRunner,
    private val hashDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val now: () -> Long = System::currentTimeMillis,
    private val zoneId: () -> ZoneId = ZoneId::systemDefault,
    private val onSessionChanged: suspend () -> Unit = {},
    private val prepareSampleUser: suspend () -> User = { error("샘플 계정 준비 함수가 연결되지 않았습니다") }
) {

    /**
     * 회원가입.
     * 순서: 입력값 검사 → 이메일 중복 확인(샘플 이메일 포함) → 비밀번호 해시
     *      → [사용자 줄 + 설정 기본 줄]을 한 트랜잭션으로 저장 → 로그인 상태로 기억
     */
    suspend fun signUp(
        displayName: String,
        email: String,
        password: String,
        passwordConfirm: String
    ): AuthResult {
        AuthValidator.validateSignUp(displayName, email, password, passwordConfirm)
            ?.let { return AuthResult.Failure(it) }

        val cleanEmail = AuthValidator.normalizeEmail(email)
        // 샘플 이메일은 샘플 계정만 쓴다(예약). 이미 가입된 것처럼 알린다.
        if (cleanEmail == SampleData.EMAIL || userDao.findByEmail(cleanEmail) != null) {
            return AuthResult.Failure(AuthError.EMAIL_TAKEN)
        }

        val passwordHash = withContext(hashDispatcher) { PasswordHasher.hash(password) }
        val newUser = User(
            email = cleanEmail,
            passwordHash = passwordHash,
            displayName = displayName.trim(),
            createdAt = now()
        )

        val newId = try {
            transaction.run {
                val id = userDao.insert(newUser)
                // 같은 트랜잭션에서 설정 기본 줄을 넣는다. 그래서 home_zone_id가 "가입 때 시간대"라는 뜻을 갖는다.
                userSettingsDao.insert(UserSettingsEntity.defaultsFor(id, zoneId().id))
                id
            }
        } catch (e: SQLiteConstraintException) {
            // 위에서 중복을 확인했지만, 그 사이에 같은 이메일이 들어왔다면
            // DB의 "이메일 중복 금지" 규칙이 저장을 거부한다. 마지막 안전장치.
            return AuthResult.Failure(AuthError.EMAIL_TAKEN)
        }

        sessionStore.saveLoggedInUserId(newId)
        onSessionChanged()
        return AuthResult.Success(newUser.copy(id = newId))
    }

    /**
     * 로그인.
     * 이메일이 없는 경우와 비밀번호가 틀린 경우를 같은 오류로 돌려준다.
     * ("이 이메일은 가입되어 있다"는 정보를 남에게 알려 주지 않기 위해서다.)
     */
    suspend fun login(email: String, password: String): AuthResult {
        AuthValidator.validateLogin(email, password)
            ?.let { return AuthResult.Failure(it) }

        val user = userDao.findByEmail(AuthValidator.normalizeEmail(email))
            ?: return AuthResult.Failure(AuthError.WRONG_CREDENTIALS)

        val passwordMatches = withContext(hashDispatcher) {
            PasswordHasher.verify(password, user.passwordHash)
        }
        if (!passwordMatches) return AuthResult.Failure(AuthError.WRONG_CREDENTIALS)

        sessionStore.saveLoggedInUserId(user.id)
        onSessionChanged()
        return AuthResult.Success(user)
    }

    /**
     * "샘플 계정으로 체험하기".
     * 오늘 만든 샘플 계정이 있으면 그대로, 없으면 새로 만든 뒤(SampleAccountSeeder) 그 계정으로 로그인한다.
     * DB 오류가 나면 SAMPLE_FAILED. 준비는 한 트랜잭션이라 반쯤 만들어진 데이터는 남지 않는다.
     */
    suspend fun loginAsSample(): AuthResult {
        val user = try {
            prepareSampleUser()
        } catch (e: SQLiteException) {
            return AuthResult.Failure(AuthError.SAMPLE_FAILED)
        }
        sessionStore.saveLoggedInUserId(user.id)
        onSessionChanged()   // 알림을 샘플 계정 것으로 다시 건다
        return AuthResult.Success(user)
    }

    /** 앱을 켰을 때, 이전에 로그인해 둔 사용자가 있으면 돌려준다. 없으면 null. */
    suspend fun currentUser(): User? {
        val userId = sessionStore.getLoggedInUserId() ?: return null
        val user = userDao.findById(userId)
        // 기억해 둔 id의 사용자가 DB에 없으면(예: DB가 초기화됨) 기억을 지운다.
        if (user == null) sessionStore.clear()
        return user
    }

    /**
     * 프로필(표시 이름, 이메일)을 고친다. 다른 사람이 쓰는 이메일로는 바꿀 수 없다.
     * 샘플 이메일은 샘플 계정만 쓸 수 있다.
     * @return 성공이면 고쳐진 사용자
     */
    suspend fun updateProfile(userId: Long, displayName: String, email: String): AuthResult {
        AuthValidator.validateProfile(displayName, email)?.let { return AuthResult.Failure(it) }
        val user = userDao.findById(userId) ?: return AuthResult.Failure(AuthError.WRONG_CREDENTIALS)

        val cleanEmail = AuthValidator.normalizeEmail(email)
        if (cleanEmail == SampleData.EMAIL && !user.isSample) return AuthResult.Failure(AuthError.EMAIL_TAKEN)
        val owner = userDao.findByEmail(cleanEmail)
        if (owner != null && owner.id != userId) return AuthResult.Failure(AuthError.EMAIL_TAKEN)

        val updated = user.copy(displayName = displayName.trim(), email = cleanEmail)
        userDao.update(updated)
        return AuthResult.Success(updated)
    }

    suspend fun logout() {
        sessionStore.clear()
        onSessionChanged()
    }
}
