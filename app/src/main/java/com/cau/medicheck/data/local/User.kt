package com.cau.medicheck.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * users 표: 사용자 한 명이 한 줄. (docs/DB_DESIGN.md ③)
 *
 * @Entity 꼬리표를 보고 Room이 이 클래스 모양대로 표를 만든다.
 * - email: 로그인할 때 쓰는 이메일. 같은 이메일로 두 번 가입할 수 없다(unique).
 *          앞뒤 공백을 없애고 소문자로 바꾼 값만 들어온다 (AuthValidator.normalizeEmail).
 * - password_hash: 비밀번호를 그대로 두지 않고 해시로 바꾼 값.
 * - display_name: 화면에 보여 줄 이름.
 * - is_sample: "샘플 계정으로 체험하기"로 만든 계정이면 true.
 *   샘플 계정을 찾아서 날마다 새로 만들고, 샘플 이메일을 다른 사람이 못 쓰게 막는 데 쓴다.
 *   defaultValue = "0": 표에 줄을 넣을 때 값을 안 주면 0(거짓)으로 채우라는 DB 쪽 기본값.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val email: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "is_sample", defaultValue = "0")
    val isSample: Boolean = false
)
