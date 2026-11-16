package com.cau.medicheck.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cau.medicheck.data.local.User
import com.cau.medicheck.ui.screens.medication.MedicationEditActions
import com.cau.medicheck.ui.screens.medication.MedicationEditScreen
import com.cau.medicheck.ui.screens.medication.MedicationEditViewModel
import com.cau.medicheck.ui.screens.medication.MedicationListScreen
import com.cau.medicheck.ui.screens.medication.MedicationListViewModel

/** 로그인한 뒤의 화면들에 붙이는 주소. */
object MainRoutes {
    const val MEDICATIONS = "medications"

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

/**
 * 로그인한 뒤의 화면들(약 목록 ↔ 약 등록·수정)과 그 사이의 이동.
 *
 * 화면마다 자기 ViewModel을 하나씩 가진다. viewModel(factory = ...)로 꺼내면
 * 그 화면이 살아 있는 동안 같은 ViewModel이 유지되고, 화면을 떠나면 함께 정리된다.
 *
 * @param user 로그인한 사용자. 이 사용자의 약만 보여 주기 위해 id를 ViewModel에 넘긴다.
 */
@Composable
fun MainNavHost(user: User, onLogout: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MainRoutes.MEDICATIONS) {

        // ----- 약 목록 -----
        composable(MainRoutes.MEDICATIONS) {
            val viewModel: MedicationListViewModel =
                viewModel(factory = MedicationListViewModel.factory(user.id))
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            MedicationListScreen(
                displayName = user.displayName,
                state = state,
                onAddClick = { navController.navigate(MainRoutes.medicationEdit(null)) },
                onMedicationClick = { id -> navController.navigate(MainRoutes.medicationEdit(id)) },
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
        ) { backStackEntry ->
            // 주소에 붙어 온 약 id를 꺼낸다. 없으면(-1) 새 약 등록이다.
            val medicationId = backStackEntry.arguments
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
