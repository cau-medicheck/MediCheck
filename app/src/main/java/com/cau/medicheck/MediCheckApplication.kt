package com.cau.medicheck

import android.app.Application
import android.content.Context
import com.cau.medicheck.data.local.AppDatabase
import com.cau.medicheck.data.local.AppPrefs
import com.cau.medicheck.data.local.RoomTransactionRunner
import com.cau.medicheck.data.local.SessionStore
import com.cau.medicheck.data.local.SharedPrefsAppPrefs
import com.cau.medicheck.data.local.SharedPrefsSessionStore
import com.cau.medicheck.data.local.TransactionRunner
import com.cau.medicheck.data.repository.AuthRepository
import com.cau.medicheck.data.repository.DoseRecordGenerator
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.data.repository.MedicationRepository
import com.cau.medicheck.data.repository.ReminderRepository
import com.cau.medicheck.data.repository.SampleAccountSeeder
import com.cau.medicheck.notification.ReminderNotifier
import com.cau.medicheck.notification.ReminderScheduler
import com.cau.medicheck.ui.components.ToastBus
import com.cau.medicheck.ui.navigation.SelectionStore
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

    /**
     * 화면과 상관없이 앱이 살아 있는 동안 쓰는 작업 공간. (화면이 닫혀도 하던 일을 끝낸다)
     * 예: 토스트의 "실행취소"는 그 화면을 떠난 뒤에 눌려도 끝까지 실행돼야 한다.
     */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val appContext: Context = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.create(appContext) }

    private val sessionStore: SessionStore by lazy { SharedPrefsSessionStore(appContext) }

    private val transactionRunner: TransactionRunner by lazy { RoomTransactionRunner(database) }

    /** 폰 단위 설정(온보딩을 봤는지, 접근성, 알림 권한 창을 띄웠는지). 로그인 전에도 읽는다. */
    val appPrefs: AppPrefs by lazy { SharedPrefsAppPrefs(appContext) }

    /** 토스트(잠깐 떴다 사라지는 안내 글)를 띄워 달라는 요청이 지나가는 통로. 앱 전체에 하나. */
    val toastBus: ToastBus by lazy { ToastBus() }

    /** 여러 화면이 함께 쓰는 선택 날짜·마지막으로 연 회차 (메모리에만 있다) */
    val selection: SelectionStore by lazy { SelectionStore() }

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

    /** "샘플 계정으로 체험하기"의 데이터를 만드는 도구 */
    private val sampleAccountSeeder: SampleAccountSeeder by lazy {
        SampleAccountSeeder(
            userDao = database.userDao(),
            medicationDao = database.medicationDao(),
            scheduleDao = database.scheduleDao(),
            doseRecordDao = database.doseRecordDao(),
            statusEventDao = database.doseStatusEventDao(),
            userSettingsDao = database.userSettingsDao(),
            transaction = transactionRunner
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            userDao = database.userDao(),
            userSettingsDao = database.userSettingsDao(),
            sessionStore = sessionStore,
            transaction = transactionRunner,
            prepareSampleUser = { sampleAccountSeeder.prepare() },
            // 로그인·로그아웃으로 "지금 로그인한 사람"이 바뀌면 알림도 그 사람 것으로 다시 건다.
            onSessionChanged = {
                // 로그아웃했다면 앞 사람의 약 알림이 화면에 남아 있지 않게 지운다.
                if (sessionStore.getLoggedInUserId() == null) reminderNotifier.cancelAll()
                // 앞 사람이 보던 날짜·회차가 다음 사람에게 남지 않게 처음 값으로.
                selection.reset()
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
            doseRecordDao = database.doseRecordDao(),
            statusEventDao = database.doseStatusEventDao()
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
            statusEventDao = database.doseStatusEventDao(),
            generator = doseRecordGenerator,
            transaction = transactionRunner,
            // 내일 이후의 예정 회차는 지금의 약·규칙에서 계산한다.
            observeMedications = { userId -> medicationRepository.observeMedications(userId) }
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

        // 알림 채널을 만들어 둔다 (설정 화면의 알림 항목에 보이게 된다).
        container.reminderNotifier.ensureChannel()

        // 앱이 켜질 때마다 다음 알림을 다시 건다. 알람이 어떤 이유로 사라졌더라도 여기서 복구된다.
        container.appScope.launch { container.reminderScheduler.reschedule() }
    }
}
