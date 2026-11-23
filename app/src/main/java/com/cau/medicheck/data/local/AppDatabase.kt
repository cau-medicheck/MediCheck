package com.cau.medicheck.data.local

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 앱의 데이터베이스 본체.
 *
 * - entities: 이 DB에 들어 있는 표 목록. 표를 새로 만들면 여기에 추가한다.
 * - version: 표 구조가 바뀔 때마다 1씩 올리는 번호.
 *     1: users
 *     2: medications, schedules 추가
 *     3: dose_records 추가
 *     4: users의 username을 email로 변경
 *     5: v3 프로토타입(47개 화면) 기준 개정을 한 번에 — users.is_sample,
 *        medications.paused_from·hospital·department·source_memo, schedules.effective_from,
 *        dose_records 스냅샷(medication_name·dosage·memo), 새 표 dose_status_events·user_settings
 *     6: 복용 조건 — schedules.dose_condition, dose_records.dose_condition(스냅샷). 데이터를 지우지 않고 칸만 더한다(MIGRATION_5_6)
 * - 표마다 DAO(창구)를 하나씩 꺼낼 수 있게 함수를 적어 둔다. 몸통은 Room이 만든다.
 */
@Database(
    entities = [
        User::class,
        Medication::class,
        Schedule::class,
        DoseRecord::class,
        DoseStatusEvent::class,
        UserSettingsEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun medicationDao(): MedicationDao

    abstract fun scheduleDao(): ScheduleDao

    abstract fun doseRecordDao(): DoseRecordDao

    abstract fun doseStatusEventDao(): DoseStatusEventDao

    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        /** 폰에 저장되는 DB 파일 이름 */
        private const val DB_NAME = "medicheck.db"

        private const val LOG_TAG = "MediCheckDb"

        /**
         * DB를 연다. 앱 전체에서 한 번만 호출한다 (AppContainer 참고).
         *
         * addMigrations: 5 → 6은 칸 두 개를 더하기만 하면 되므로 데이터를 지우지 않고 옮긴다(MIGRATION_5_6).
         * fallbackToDestructiveMigration:
         *   그 밖의 옛 버전(1~4)에서 올라오면 기존 데이터를 지우고 새로 만든다. 개발 단계라 단순한 쪽을 택했다.
         */
        fun create(context: Context): AppDatabase {
            val builder = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration(dropAllTables = true)

            // 디버그 빌드(개발용 APK)에서만: DB를 열 때 외래 키 검사가 켜져 있는지 로그로 남긴다.
            // SQLite는 이 검사가 기본으로 꺼져 있고, 켜져 있어야 CASCADE·SET NULL이 동작한다.
            // (정확성은 이 검사에 기대지 않는다 — 지울 때는 순서대로 직접 지운다. 확인용일 뿐. DB_DESIGN ⑦)
            val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            if (isDebuggable) builder.addCallback(ForeignKeyLogger)

            return builder.build()
        }

        /**
         * 5 → 6: 복용 조건 칸 두 개를 더한다. 이미 있는 줄은 "상관없음"(NONE)이 된다.
         * DEFAULT 값은 Entity의 @ColumnInfo(defaultValue)와 같아야 Room의 구조 검사를 통과한다.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN dose_condition TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE dose_records ADD COLUMN dose_condition TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        /** DB가 열릴 때 PRAGMA foreign_keys 값(1 = 켜짐, 0 = 꺼짐)을 Logcat에 찍는다. */
        private object ForeignKeyLogger : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.query("PRAGMA foreign_keys").use { cursor ->
                    val value = if (cursor.moveToFirst()) cursor.getInt(0) else -1
                    Log.d(LOG_TAG, "PRAGMA foreign_keys = $value")
                }
            }
        }
    }
}

/**
 * "여러 DB 작업을 한 묶음으로" 실행해 주는 도구.
 *
 * 트랜잭션: 묶음 안의 작업이 전부 성공하거나, 하나라도 실패하면 전부 없던 일이 되게 하는 것.
 * 예) 약을 저장한 뒤 복용 시간을 저장하다 실패하면, 약만 덩그러니 남지 않게 한다.
 *
 * interface로 나눈 이유: 단위 테스트에는 진짜 DB가 없으므로,
 * 그냥 순서대로 실행만 하는 가짜로 바꿔 끼우기 위해서다.
 */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

/** 실제 앱에서 쓰는 구현: Room의 트랜잭션 기능을 그대로 쓴다. */
class RoomTransactionRunner(private val database: AppDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction { block() }
}
