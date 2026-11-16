package com.cau.medicheck.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cau.medicheck.R
import com.cau.medicheck.data.local.User
import com.cau.medicheck.ui.components.NotificationPermissionBanner
import com.cau.medicheck.ui.screens.dose.DayDosesViewModel
import com.cau.medicheck.ui.screens.dose.DoseActions
import com.cau.medicheck.ui.screens.dose.HistoryScreen
import com.cau.medicheck.ui.screens.dose.TodayScreen
import com.cau.medicheck.ui.screens.medication.MedicationEditActions
import com.cau.medicheck.ui.screens.medication.MedicationEditScreen
import com.cau.medicheck.ui.screens.medication.MedicationEditViewModel
import com.cau.medicheck.ui.screens.medication.MedicationListScreen
import com.cau.medicheck.ui.screens.medication.MedicationListViewModel

/** 로그인한 뒤의 화면들에 붙이는 주소. */
object MainRoutes {
    const val TODAY = "today"
    const val MEDICATIONS = "medications"
    const val HISTORY = "history"

    /** 등록·수정 화면의 주소. 뒤에 붙는 id가 "어느 약을 수정할지"를 알려 준다. */
    private const val MEDICATION_EDIT = "medication_edit"
    const val ARG_MEDICATION_ID = "medicationId"
    const val MEDICATION_EDIT_PATTERN = "$MEDICATION_EDIT?$ARG_MEDICATION_ID={$ARG_MEDICATION_ID}"

    /** id가 없다는 뜻으로 쓰는 값. (주소에는 null을 넣을 수 없어서 -1을 약속으로 쓴다) */
    const val NO_ID = -1L

    /** @param medicationId 수정할 약의 id. 새 약을 등록할 때는 null */
    fun medicationEdit(medicationId: Long?): String =
        if (medicationId == null) MEDICATION_EDIT else "$MEDICATION_EDIT?$ARG_MEDICATION_ID=$medicationId"
}

/** 화면 아래쪽 탭 세 개. 순서대로 그려진다. */
private enum class MainTab(val route: String, @param:StringRes val labelRes: Int, val icon: ImageVector) {
    TODAY(MainRoutes.TODAY, R.string.tab_today, Icons.Filled.Home),
    MEDICATIONS(MainRoutes.MEDICATIONS, R.string.tab_medications, Icons.AutoMirrored.Filled.List),
    HISTORY(MainRoutes.HISTORY, R.string.tab_history, Icons.Filled.DateRange)
}

/**
 * 로그인한 뒤의 화면들과 그 사이의 이동.
 *
 *   아래쪽 탭:  오늘  |  내 약  |  기록
 *   내 약 → 약 등록·수정 (이 화면에서는 탭을 숨긴다)
 *
 * 화면마다 자기 ViewModel을 하나씩 가진다. viewModel(factory = ...)로 꺼내면
 * 그 화면이 살아 있는 동안 같은 ViewModel이 유지된다.
 *
 * @param user 로그인한 사용자. 이 사용자의 데이터만 보여 주기 위해 id를 ViewModel에 넘긴다.
 */
@Composable
fun MainNavHost(user: User, onLogout: () -> Unit) {
    val navController = rememberNavController()

    // 지금 보이는 화면의 주소. 화면이 바뀌면 이 값도 바뀐다.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTabScreen = MainTab.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTabScreen) {
                NavigationBar {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        },
        // 위쪽(상태 표시줄) 여백은 안쪽 화면들이 각자 처리하므로 여기서는 0으로 둔다.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainRoutes.TODAY,
            // 탭 높이만큼 아래를 띄우고, 그만큼은 이미 썼다고 안쪽 화면에 알려 준다(중복 여백 방지).
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {

            // ----- 오늘 -----
            composable(MainRoutes.TODAY) {
                val viewModel: DayDosesViewModel =
                    viewModel(factory = DayDosesViewModel.factory(user.id, followToday = true))
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                // 화면이 다시 보일 때마다(앱으로 돌아왔을 때 등) 오늘까지의 기록을 채운다.
                LifecycleResumeEffect(viewModel) {
                    viewModel.refresh()
                    onPauseOrDispose { }
                }

                TodayScreen(
                    displayName = user.displayName,
                    state = state,
                    actions = rememberDoseActions(viewModel),
                    onLogout = onLogout,
                    banner = { NotificationPermissionBanner() }
                )
            }

            // ----- 내 약 -----
            composable(MainRoutes.MEDICATIONS) {
                val viewModel: MedicationListViewModel =
                    viewModel(factory = MedicationListViewModel.factory(user.id))
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                MedicationListScreen(
                    state = state,
                    onAddClick = { navController.navigate(MainRoutes.medicationEdit(null)) },
                    onMedicationClick = { id -> navController.navigate(MainRoutes.medicationEdit(id)) },
                    onLogout = onLogout
                )
            }

            // ----- 기록 -----
            composable(MainRoutes.HISTORY) {
                val viewModel: DayDosesViewModel =
                    viewModel(factory = DayDosesViewModel.factory(user.id, followToday = false))
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                HistoryScreen(
                    state = state,
                    actions = rememberDoseActions(viewModel),
                    onPreviousDay = viewModel::goToPreviousDay,
                    onNextDay = viewModel::goToNextDay,
                    onDatePicked = viewModel::goToDate,
                    onLogout = onLogout
                )
            }

            // ----- 약 등록·수정 -----
            composable(
                route = MainRoutes.MEDICATION_EDIT_PATTERN,
                arguments = listOf(
                    navArgument(MainRoutes.ARG_MEDICATION_ID) {
                        type = NavType.LongType
                        defaultValue = MainRoutes.NO_ID
                    }
                )
            ) { entry ->
                // 주소에 붙어 온 약 id를 꺼낸다. 없으면(-1) 새 약 등록이다.
                val medicationId = entry.arguments
                    ?.getLong(MainRoutes.ARG_MEDICATION_ID)
                    ?.takeIf { it != MainRoutes.NO_ID }

                val viewModel: MedicationEditViewModel =
                    viewModel(factory = MedicationEditViewModel.factory(user.id, medicationId))
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                // 저장·삭제가 끝나면(isFinished) 목록 화면으로 돌아간다.
                // LaunchedEffect: 괄호 안의 값이 바뀔 때 한 번 실행되는 코드.
                LaunchedEffect(state.isFinished) {
                    if (state.isFinished) navController.popBackStack()
                }

                // remember: 화면이 다시 그려질 때마다 묶음을 새로 만들지 않도록 기억해 둔다.
                val actions = remember(viewModel, navController) {
                    MedicationEditActions(
                        onNameChange = viewModel::onNameChange,
                        onDosageChange = viewModel::onDosageChange,
                        onMemoChange = viewModel::onMemoChange,
                        onStartDateChange = viewModel::onStartDateChange,
                        onEndDateChange = viewModel::onEndDateChange,
                        onAddSchedule = viewModel::addSchedule,
                        onRemoveSchedule = viewModel::removeSchedule,
                        onScheduleTimeChange = viewModel::onScheduleTimeChange,
                        onScheduleDayToggle = viewModel::onScheduleDayToggle,
                        onSave = viewModel::save,
                        onDelete = viewModel::delete,
                        onBack = { navController.popBackStack() }
                    )
                }
                MedicationEditScreen(state = state, actions = actions)
            }
        }
    }
}

/** 복용 기록 카드의 버튼들을 ViewModel의 함수에 이어 준다. */
@Composable
private fun rememberDoseActions(viewModel: DayDosesViewModel): DoseActions =
    remember(viewModel) {
        DoseActions(
            onTake = viewModel::markTaken,
            onSkip = viewModel::markSkipped,
            onUndo = viewModel::undo
        )
    }

/**
 * 아래쪽 탭을 눌렀을 때의 이동.
 * 탭을 여러 번 오가도 화면이 계속 쌓이지 않게 하고, 탭마다 보던 상태(스크롤 위치 등)를 기억한다.
 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
