package com.cau.medicheck.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

/**
 * 앱의 데이터베이스 본체.
 *
 * - entities: 이 DB에 들어 있는 표 목록. 표를 새로 만들면 여기에 추가한다.
 * - version: 표 구조가 바뀔 때마다 1씩 올리는 번호.
 *     1: users
 *     2: medications, schedules 추가
 * - 표마다 DAO(창구)를 하나씩 꺼낼 수 있게 함수를 적어 둔다. 몸통은 Room이 만든다.
 */
@Database(
    entities = [User::class, Medication::class, Schedule::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun medicationDao(): MedicationDao

    abstract fun scheduleDao(): ScheduleDao

    companion object {
        /** 폰에 저장되는 DB 파일 이름 */
        private const val DB_NAME = "medicheck.db"

        /**
         * DB를 연다. 앱 전체에서 한 번만 호출한다 (AppContainer 참고).
         *
         * fallbackToDestructiveMigration:
         *   개발 중에 표 구조(version)가 바뀌면 기존 데이터를 지우고 새로 만든다.
         *   출시 후라면 데이터를 옮기는 코드(Migration)를 써야 하지만,
         *   지금은 개발 단계라 단순한 쪽을 택했다.
         */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
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
