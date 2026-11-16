package com.cau.medicheck.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 앱의 데이터베이스 본체.
 *
 * - entities: 이 DB에 들어 있는 표 목록. 표를 새로 만들면 여기에 추가한다.
 * - version: 표 구조가 바뀔 때마다 1씩 올리는 번호.
 * - 표마다 DAO(창구)를 하나씩 꺼낼 수 있게 함수를 적어 둔다. 몸통은 Room이 만든다.
 */
@Database(
    entities = [User::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

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
