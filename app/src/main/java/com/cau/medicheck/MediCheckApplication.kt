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
import com.cau.medicheck.data.repository.ReminderRepository
import com.cau.medicheck.notification.ReminderNotifier
import com.cau.medicheck.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 앱 전체에서 하나씩만 있어야 하는 것들을 만들어 보관하는 상자.
 *
 * DB를 여러 번 열면 문제가 생기므로, 여기서 한 번만 만들고
 * 필요한 곳(ViewModel, 알림 수신기)이 여기서 꺼내 쓴다.
 * `by lazy`: 처음 꺼내 쓰는 순간에 만들고, 그다음부터는 같은 것을 돌려준다.
 *
 * 이 파일을 보면 "누가 누구를 쓰는지"가 한눈에 보인다.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.create(appContext) }

    private val sessionStore: SessionStore by lazy { SharedPrefsSessionStore(appContext) }

    private val transactionRunner: TransactionRunner by lazy { RoomTransactionRunner(database) }

    // ---------- 알림 ----------

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(
            sessionStore = sessionStore,
            scheduleDao = database.scheduleDao(),
            doseRecordDao = database.doseRecordDao()
        )
    }

    val reminderNotifier: ReminderNotifier by lazy { ReminderNotifier(appContext) }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(appContext, reminderRepository)
    }

    // ---------- 회원가입·로그인 ----------

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            userDao = database.userDao(),
            sessionStore = sessionStore,
            // 로그인·로그아웃으로 "지금 로그인한 사람"이 바뀌면 알림도 그 사람 것으로 다시 건다.
            onSessionChanged = {
                // 로그아웃했다면 앞 사람의 약 알림이 화면에 남아 있지 않게 지운다.
                if (sessionStore.getLoggedInUserId() == null) reminderNotifier.cancelAll()
                reminderScheduler.reschedule()
            }
        )
    }

    // ---------- 약 · 복용 기록 ----------

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
            doseRecordGenerator = doseRecordGenerator,
            // 약이 등록·수정·삭제되면 다음 알림 시각이 달라질 수 있으므로 다시 건다.
            onChanged = { reminderScheduler.reschedule() }
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

    /** 화면과 상관없이 앱이 살아 있는 동안 쓰는 작업 공간. (화면이 닫혀도 하던 일을 끝낸다) */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // 알림 채널을 만들어 둔다 (설정 화면의 알림 항목에 보이게 된다).
        container.reminderNotifier.ensureChannel()

        // 앱이 켜질 때마다 다음 알림을 다시 건다. 알람이 어떤 이유로 사라졌더라도 여기서 복구된다.
        applicationScope.launch { container.reminderScheduler.reschedule() }
    }
}
