package com.cau.medicheck.ui.navigation.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.cau.medicheck.MediCheckApplication
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cau.medicheck.data.local.User
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.components.PlaceholderScreen
import com.cau.medicheck.ui.components.rememberNotificationsOff
import com.cau.medicheck.ui.navigation.AppNavigator
import com.cau.medicheck.ui.navigation.NavRules
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.screens.dose.DayDosesViewModel
import com.cau.medicheck.ui.screens.dose.DoseDetailActions
import com.cau.medicheck.ui.screens.dose.HomeActions
import com.cau.medicheck.ui.screens.dose.S04HomeScreen
import com.cau.medicheck.ui.screens.dose.S05ScheduleScreen
import com.cau.medicheck.ui.screens.dose.S10DoseDetailScreen
import com.cau.medicheck.ui.screens.dose.S10DoseDetailViewModel
import com.cau.medicheck.ui.screens.dose.S30DoseHistoryScreen
import com.cau.medicheck.ui.screens.dose.S30DoseHistoryViewModel
import com.cau.medicheck.ui.screens.dose.S31ActualTimeScreen
import com.cau.medicheck.ui.screens.dose.S31ActualTimeViewModel
import com.cau.medicheck.ui.screens.dose.ScheduleActions
import kotlinx.coroutines.launch

/** 이 파일이 진짜 화면(또는 임시로 연결한 예전 화면)을 붙여 둔 화면 ID. 나머지는 "준비 중" 임시 화면이 된다. */
private val REGISTERED =
    setOf("S04", "S05", "S10", "S30", "S31") + MEDICATION_SCREENS + RECORD_SCREENS + NOTIFICATION_SCREENS +
        SETTINGS_SCREENS

/** 회차를 가리키는 주소 인자 두 개(규칙 번호, 날짜). 둘 다 없어도 화면이 열리도록 기본값을 둔다. */
private val doseArguments = listOf(
    navArgument(Routes.ARG_SCHEDULE_ID) { type = NavType.LongType; defaultValue = Routes.NO_ID },
    navArgument(Routes.ARG_DATE) { type = NavType.StringType; defaultValue = "" }
)

/**
 * 주소에 붙어 온 회차 인자를 읽는다.
 * @return (인자가 붙어 왔는가, 읽은 회차). 인자가 있어도 값이 틀렸으면(날짜 형식 등) 회차는 null
 */
private fun NavBackStackEntry.doseArgs(): Pair<Boolean, DoseKey?> {
    val scheduleId = arguments?.getLong(Routes.ARG_SCHEDULE_ID) ?: Routes.NO_ID
    val dateText = arguments?.getString(Routes.ARG_DATE).orEmpty()
    val hasArgs = scheduleId != Routes.NO_ID || dateText.isNotEmpty()
    val date = NavRules.parseDateOrNull(dateText)
    val key = if (scheduleId != Routes.NO_ID && date != null) DoseKey(scheduleId, date) else null
    return hasArgs to key
}

/**
 * 로그인 뒤 화면들의 주소 등록과 그 사이의 이동. (구현 명세 02)
 *
 * 화면 함수는 그리기만 하고, "버튼을 누르면 어디로 가나"는 여기서 정한다.
 * 화면마다 자기 ViewModel을 하나씩 가진다. viewModel(factory = …)로 꺼내면 그 화면이 뒤로 가기 기록에
 * 남아 있는 동안 같은 ViewModel이 유지된다.
 *
 * 이 파일: S04 홈 · S05 일정 · S10 회차 상세 · S30 변경 이력 · S31 실제 복용 시각 (2단계).
 * 내 약 쪽 화면(S06~S09 등)은 MedicationGraph.kt (3단계), 기록·통계 화면(S11~S15·S43)은 RecordGraph.kt (4단계),
 * 알림 화면(S16~S18·S34·S35)은 NotificationGraph.kt (5단계), 설정 화면(S19~S22·S36~S39)은 SettingsGraph.kt (6단계).
 * 그 밖의 화면은 "준비 중" 임시 화면이다.
 */
fun NavGraphBuilder.mainGraph(user: User, go: AppNavigator, settings: SettingsCallbacks) {

    // ----- S04 홈 대시보드 -----
    composable(Routes.S04) {
        val vm: DayDosesViewModel = viewModel(factory = DayDosesViewModel.factory(user.id))
        val state by vm.uiState.collectAsStateWithLifecycle()
        RefreshOnResume(vm::refresh)
        S04HomeScreen(
            displayName = user.displayName,
            state = state,
            showNotificationOffCard = rememberNotificationsOff(),
            actions = remember(vm, go) {
                HomeActions(
                    onSelectDay = vm::selectDay,
                    onOpenSchedule = { go.openTab(Routes.S05) },
                    onOpenDose = { key -> go.open(Routes.s10(key)) },
                    onQuickTake = vm::quickTake,
                    onAddMedication = { go.open(Routes.s08New()) },
                    onOpenStats = { go.openTab(Routes.S13) },
                    onOpenNotificationHelp = { go.open(Routes.S34) }
                )
            }
        )
    }

    // ----- S05 일정/캘린더 -----
    composable(Routes.S05) {
        val vm: DayDosesViewModel = viewModel(factory = DayDosesViewModel.factory(user.id))
        val state by vm.uiState.collectAsStateWithLifecycle()
        RefreshOnResume(vm::refresh)
        S05ScheduleScreen(
            state = state,
            actions = remember(vm, go) {
                ScheduleActions(
                    onShiftDay = vm::shiftDay,
                    onSelectDay = vm::selectDay,
                    onToday = vm::goToToday,
                    onOpenDose = { key -> go.open(Routes.s10(key)) },
                    onQuickTake = vm::quickTake,
                    onAddMedication = { go.open(Routes.s08New()) }
                )
            }
        )
    }

    // ----- S10 회차 상세/상태 -----
    composable(Routes.S10, arguments = doseArguments) { entry ->
        val (hasArgs, key) = remember(entry) { entry.doseArgs() }
        // key: 회차마다 ViewModel을 따로 둔다. 회차 상세를 보던 중에 다른 회차의 알림을 누르면 같은 화면 항목이
        // 주소만 바뀐 채 다시 쓰이는데, 그때 앞 회차의 ViewModel이 그대로 쓰이지 않게 하기 위해서다.
        val vm: S10DoseDetailViewModel = viewModel(
            key = "S10-$hasArgs-$key",
            factory = S10DoseDetailViewModel.factory(user.id, hasArgs, key)
        )
        val state by vm.uiState.collectAsStateWithLifecycle()
        RefreshOnResume(vm::refresh)
        // 주소로 받은 회차를 못 찾았으면(토스트는 ViewModel이 띄웠다) 이 화면을 닫는다.
        // LaunchedEffect: 괄호 안의 값이 바뀔 때 한 번 실행되는 코드.
        LaunchedEffect(state.shouldClose) {
            if (state.shouldClose) go.back()
        }
        S10DoseDetailScreen(
            state = state,
            actions = DoseDetailActions(
                onBack = go::back,
                onMark = vm::mark,
                onUndo = vm::undo,
                onOpenActualTime = { state.dose?.let { go.open(Routes.s31(it.key)) } },
                onOpenHistory = { go.open(Routes.S30) },
                onOpenSchedule = { go.openTab(Routes.S05) }
            )
        )
    }

    // ----- S30 복용 변경 이력 -----
    composable(Routes.S30) {
        val vm: S30DoseHistoryViewModel = viewModel(factory = S30DoseHistoryViewModel.factory(user.id))
        val state by vm.uiState.collectAsStateWithLifecycle()
        // 이 화면이 보이는 동안만 살아 있는 작업 공간. 화면을 떠나면 하던 일이 취소된다.
        val scope = rememberCoroutineScope()
        // 변경 시각을 보여 줄 시간대: 앱 시계의 시간대 (시간대 정책 S35를 따른다)
        val zone = (LocalContext.current.applicationContext as MediCheckApplication).container.clock.zone
        S30DoseHistoryScreen(
            state = state,
            zone = zone,
            onBack = go::back,
            onOpenDose = {
                scope.launch {
                    // 선택 날짜의 첫 회차를 연다. 그날 회차가 없으면 일정 화면으로.
                    val first = vm.firstDoseOfSelectedDay()
                    if (first != null) go.open(Routes.s10(first)) else go.openTab(Routes.S05)
                }
            }
        )
    }

    // ----- S31 실제 복용 시각 기록 -----
    composable(Routes.S31, arguments = doseArguments) { entry ->
        val (hasArgs, key) = remember(entry) { entry.doseArgs() }
        val vm: S31ActualTimeViewModel = viewModel(factory = S31ActualTimeViewModel.factory(user.id, hasArgs, key))
        val state by vm.uiState.collectAsStateWithLifecycle()
        // 저장이 끝나면 회차 상세로 돌아간다.
        LaunchedEffect(state.isSaved) {
            if (state.isSaved) go.back()
        }
        S31ActualTimeScreen(
            state = state,
            onBack = go::back,
            onTimePicked = vm::onTimePicked,
            onSave = vm::save
        )
    }

    medicationGraph(user, go)   // 내 약 쪽 화면들 (MedicationGraph.kt)
    recordGraph(user, go)       // 기록·통계 화면들 (RecordGraph.kt)
    notificationGraph(go)       // 알림 화면들 (NotificationGraph.kt)
    settingsGraph(user, go, settings)   // 설정 화면들 (SettingsGraph.kt)

    // ----- 아직 만들지 않은 화면: "준비 중" -----
    Routes.ALL_SCREEN_IDS
        .filter { it !in Routes.PRE_LOGIN && it !in REGISTERED }
        .forEach { screenId ->
            composable(screenId) {
                val title = stringResource(Routes.titleRes(screenId))
                PlaceholderScreen(title)
            }
        }
}

/** 화면이 다시 보일 때마다(앱으로 돌아왔을 때 등) 오늘까지의 기록을 채운다. */
@Composable
private fun RefreshOnResume(refresh: () -> Unit) {
    LifecycleResumeEffect(refresh) {
        refresh()
        onPauseOrDispose { }
    }
}
