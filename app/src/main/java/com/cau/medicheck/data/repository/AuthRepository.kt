package com.cau.medicheck.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.cau.medicheck.data.local.SessionStore
import com.cau.medicheck.data.local.User
import com.cau.medicheck.data.local.UserDao
import com.cau.medicheck.domain.AuthError
import com.cau.medicheck.domain.AuthValidator
import com.cau.medicheck.domain.PasswordHasher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * @param hashDispatcher 해시 계산은 일부러 느리게 만든 계산이라, 화면이 멈추지 않도록
 *                       계산 전용 일꾼(Dispatchers.Default)에게 맡긴다.
 * @param now 현재 시각을 돌려주는 함수. 테스트에서 시각을 고정하려고 바꿔 끼울 수 있게 했다.
 * @param onSessionChanged 로그인·로그아웃으로 "지금 로그인한 사람"이 바뀐 뒤에 부를 함수.
 *                         앱에서는 "알림을 다시 예약하라(로그아웃이면 취소하라)"가 들어온다.
 */
class AuthRepository(
    private val userDao: UserDao,
    private val sessionStore: SessionStore,
    private val hashDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val now: () -> Long = System::currentTimeMillis,
    private val onSessionChanged: suspend () -> Unit = {}
) {

    /**
     * 회원가입.
     * 순서: 입력값 검사 → 아이디 중복 확인 → 비밀번호 해시 → DB 저장 → 로그인 상태로 기억
     */
    suspend fun signUp(
        username: String,
        password: String,
        passwordConfirm: String,
        displayName: String
    ): AuthResult {
        AuthValidator.validateSignUp(username, password, passwordConfirm, displayName)
            ?.let { return AuthResult.Failure(it) }

        val cleanUsername = AuthValidator.normalizeUsername(username)
        if (userDao.findByUsername(cleanUsername) != null) {
            return AuthResult.Failure(AuthError.USERNAME_TAKEN)
        }

        val passwordHash = withContext(hashDispatcher) { PasswordHasher.hash(password) }
        val newUser = User(
            username = cleanUsername,
            passwordHash = passwordHash,
            displayName = displayName.trim(),
            createdAt = now()
        )

        val newId = try {
            userDao.insert(newUser)
        } catch (e: SQLiteConstraintException) {
            // 위에서 중복을 확인했지만, 그 사이에 같은 아이디가 들어왔다면
            // DB의 "아이디 중복 금지" 규칙이 저장을 거부한다. 마지막 안전장치.
            return AuthResult.Failure(AuthError.USERNAME_TAKEN)
        }

        sessionStore.saveLoggedInUserId(newId)
        onSessionChanged()
        return AuthResult.Success(newUser.copy(id = newId))
    }

    /**
     * 로그인.
     * 아이디가 없는 경우와 비밀번호가 틀린 경우를 같은 오류로 돌려준다.
     * ("이 아이디는 가입되어 있다"는 정보를 남에게 알려 주지 않기 위해서다.)
     */
    suspend fun login(username: String, password: String): AuthResult {
        AuthValidator.validateLogin(username, password)
            ?.let { return AuthResult.Failure(it) }

        val user = userDao.findByUsername(AuthValidator.normalizeUsername(username))
            ?: return AuthResult.Failure(AuthError.WRONG_CREDENTIALS)

        val passwordMatches = withContext(hashDispatcher) {
            PasswordHasher.verify(password, user.passwordHash)
        }
        if (!passwordMatches) return AuthResult.Failure(AuthError.WRONG_CREDENTIALS)

        sessionStore.saveLoggedInUserId(user.id)
        onSessionChanged()
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

    suspend fun logout() {
        sessionStore.clear()
        onSessionChanged()
    }
}
