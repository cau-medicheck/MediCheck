package com.cau.medicheck.data.local

import android.content.Context

/**
 * "지금 누가 로그인해 있는지"를 기억하는 곳.
 *
 * 로그인한 사용자의 id 하나만 저장한다. 앱을 껐다 켜도 남아 있어야
 * 다시 로그인하지 않아도 되므로, 변수 대신 폰 저장공간에 적어 둔다.
 *
 * interface로 나눈 이유: 단위 테스트에서는 폰이 없으므로
 * 가짜(메모리에만 저장하는) 구현으로 바꿔 끼우기 위해서다.
 */
interface SessionStore {
    /** 로그인한 사용자의 id. 로그인하지 않았으면 null. */
    fun getLoggedInUserId(): Long?

    fun saveLoggedInUserId(userId: Long)

    /** 로그아웃: 저장된 id를 지운다. */
    fun clear()
}

/**
 * 실제 앱에서 쓰는 구현. SharedPreferences(작은 값을 저장하는 안드로이드 기본 기능)에 적는다.
 */
class SharedPrefsSessionStore(context: Context) : SessionStore {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getLoggedInUserId(): Long? {
        val id = prefs.getLong(KEY_USER_ID, NO_USER)
        return if (id == NO_USER) null else id
    }

    override fun saveLoggedInUserId(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }

    private companion object {
        const val PREFS_NAME = "session"
        const val KEY_USER_ID = "logged_in_user_id"
        const val NO_USER = -1L
    }
}
