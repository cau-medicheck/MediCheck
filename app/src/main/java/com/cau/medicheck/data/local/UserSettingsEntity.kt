package com.cau.medicheck.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * user_settings 표: 계정마다 한 줄인 "계정별 설정". (DB_DESIGN ⑥-3)
 *
 * 왜 DB에 두나: 알림 수신기(앱이 꺼져 있어도 시스템이 깨우는 부분)가 로그인 사용자 id로 바로
 * 읽을 수 있고, 계정을 지우면 함께 지워지게(CASCADE) 하려고. 폰 단위 설정(글자 크기 등)은 AppPrefs에 있다.
 *
 * 클래스 이름 끝에 Entity를 붙인 이유: 5단계에서 domain에 같은 뜻의 UserSettings(화면용 모양)를
 * 만들 예정이라 이름이 겹치지 않게 하기 위해서다.
 *
 * - user_id가 곧 PK(기본 키)다. 계정 하나에 설정 줄 하나뿐이라 따로 번호를 매기지 않는다(autoGenerate 아님).
 * - 기본값은 v3 프로토타입의 처음 상태(fresh())와 같다. home_zone_id·app_lock_method는 이 앱이 새로 정한 칸.
 * - home_zone_id: 줄을 만들 때(가입·샘플 생성) 폰의 시간대("Asia/Seoul" 등). DB 쪽 기본값이 없어서
 *   만드는 곳에서 꼭 넣어야 한다.
 * - 이 표를 실제로 읽고 쓰는 화면(알림 설정 S17 등)은 5·6단계에서 만든다. 1단계는 가입 때 기본 줄만 넣는다.
 */
@Entity(
    tableName = "user_settings",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "reminder_enabled", defaultValue = "1")
    val reminderEnabled: Boolean = true,

    @ColumnInfo(name = "sound_enabled", defaultValue = "1")
    val soundEnabled: Boolean = true,

    @ColumnInfo(name = "vibrate_enabled", defaultValue = "1")
    val vibrateEnabled: Boolean = true,

    @ColumnInfo(name = "realert_enabled", defaultValue = "0")
    val realertEnabled: Boolean = false,

    /** 재알림 간격(분). 10·15·30·60만 쓴다. 그 밖의 값은 읽는 쪽(5단계 SettingsRepository)이 15로 바로잡는다. */
    @ColumnInfo(name = "realert_minutes", defaultValue = "15")
    val realertMinutes: Int = DEFAULT_REALERT_MINUTES,

    @ColumnInfo(name = "hide_med_name", defaultValue = "1")
    val hideMedName: Boolean = true,

    /** "LOCAL"(폰의 지금 시간대) / "HOME"(등록한 시간대) */
    @ColumnInfo(name = "time_zone_mode", defaultValue = "'LOCAL'")
    val timeZoneMode: String = TIME_ZONE_LOCAL,

    @ColumnInfo(name = "home_zone_id")
    val homeZoneId: String,

    @ColumnInfo(name = "hide_stats_preview", defaultValue = "0")
    val hideStatsPreview: Boolean = false,

    @ColumnInfo(name = "app_lock_enabled", defaultValue = "0")
    val appLockEnabled: Boolean = false,

    /** "BIOMETRIC" / "DEVICE_CREDENTIAL" */
    @ColumnInfo(name = "app_lock_method", defaultValue = "'BIOMETRIC'")
    val appLockMethod: String = APP_LOCK_BIOMETRIC
) {
    companion object {
        const val DEFAULT_REALERT_MINUTES = 15
        const val TIME_ZONE_LOCAL = "LOCAL"
        const val TIME_ZONE_HOME = "HOME"
        const val APP_LOCK_BIOMETRIC = "BIOMETRIC"
        const val APP_LOCK_DEVICE_CREDENTIAL = "DEVICE_CREDENTIAL"

        /** 새 계정의 기본 설정 줄. homeZoneId에는 "지금 폰의 시간대"를 넣는다. */
        fun defaultsFor(userId: Long, homeZoneId: String) =
            UserSettingsEntity(userId = userId, homeZoneId = homeZoneId)
    }
}
