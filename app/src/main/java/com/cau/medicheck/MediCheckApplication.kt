package com.cau.medicheck

import android.app.Application
import android.content.Context
import com.cau.medicheck.data.local.AppDatabase
import com.cau.medicheck.data.local.RoomTransactionRunner
import com.cau.medicheck.data.local.SessionStore
import com.cau.medicheck.data.local.SharedPrefsSessionStore
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.data.repository.AuthRepository
import com.cau.medicheck.data.repository.DoseRecordGenerator
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.data.repository.MedicationRepository

/**
 * 앱 전체에서 하나씩만 있어야 하는 것들을 만들어 보관하는 상자.
 *
 * DB를 여러 번 열면 문제가 생기므로, 여기서 한 번만 만들고
 * 필요한 곳(ViewModel)이 여기서 꺼내 쓴다.
 * `by lazy`: 처음 꺼내 쓰는 순간에 만들고, 그다음부터는 같은 것을 돌려준다.
 */
class AppContainer(context: Context) {

    private val database: AppDatabase by lazy { AppDatabase.create(context) }

    private val sessionStore: SessionStore by lazy { SharedPrefsSessionStore(context) }

    val authRepository: AuthRepository by lazy {
        AuthRepository(database.userDao(), sessionStore)
    }

    private val transactionRunner: TransactionRunner by lazy { RoomTransactionRunner(database) }

    /** 복용 기록을 미리 만들어 두는 도구. 약 Repository와 복용 기록 Repository가 함께 쓴다. */
    private val doseRecordGenerator: DoseRecordGenerator by lazy {
        DoseRecordGenerator(
            medicationDao = database.medicationDao(),
            scheduleDao = database.scheduleDao(),
            doseRecordDao = database.doseRecordDao()
        )
    }

    val medicationRepository: MedicationRepository by lazy {
        MedicationRepository(
            medicationDao = database.medicationDao(),
            scheduleDao = database.scheduleDao(),
            transaction = transactionRunner,
            doseRecordGenerator = doseRecordGenerator
        )
    }

    val doseRepository: DoseRepository by lazy {
        DoseRepository(
            doseRecordDao = database.doseRecordDao(),
            generator = doseRecordGenerator,
            transaction = transactionRunner
        )
    }
}

/**
 * 앱이 켜질 때 가장 먼저 만들어지는 클래스.
 * AndroidManifest.xml의 android:name에 등록되어 있다.
 */
class MediCheckApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
