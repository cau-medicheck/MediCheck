package com.cau.medicheck.ui.navigation.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cau.medicheck.data.local.User
import com.cau.medicheck.ui.navigation.AppNavigator
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.screens.medication.ArchiveViewModel
import com.cau.medicheck.ui.screens.medication.MedicationDetailActions
import com.cau.medicheck.ui.screens.medication.MedicationDetailViewModel
import com.cau.medicheck.ui.screens.medication.MedicationEditEvent
import com.cau.medicheck.ui.screens.medication.MedicationEditViewModel
import com.cau.medicheck.ui.screens.medication.MedicationFormActions
import com.cau.medicheck.ui.screens.medication.MedicationListViewModel
import com.cau.medicheck.ui.screens.medication.PrescriptionSourceViewModel
import com.cau.medicheck.ui.screens.medication.MedicationListActions
import com.cau.medicheck.ui.screens.medication.S06MedicationListScreen
import com.cau.medicheck.ui.screens.medication.S07MedicationDetailScreen
import com.cau.medicheck.ui.screens.medication.S08MedicationFormScreen
import com.cau.medicheck.ui.screens.medication.S09DeleteConfirmScreen
import com.cau.medicheck.ui.screens.medication.S29PhotoRegisterScreen
import com.cau.medicheck.ui.screens.medication.S32ArchiveScreen
import com.cau.medicheck.ui.screens.medication.S33EditScopeScreen
import com.cau.medicheck.ui.screens.medication.S40DuplicateScreen
import com.cau.medicheck.ui.screens.medication.S42PrescriptionSourceScreen

/** 이 파일이 붙여 둔 화면 ID */
val MEDICATION_SCREENS = setOf("S06", "S07", "S08", "S09", "S29", "S32", "S33", "S40", "S42")

/** 약을 가리키는 주소 인자(약 번호). 없어도 화면이 열리도록 기본값(-1)을 둔다. */
private val medArguments = listOf(
    navArgument(Routes.ARG_MED_ID) { type = NavType.LongType; defaultValue = Routes.NO_ID }
)

/** 주소에 붙어 온 약 번호. 없으면 null */
private fun NavBackStackEntry.medIdOrNull(): Long? =
    arguments
        ?.takeIf { it.containsKey(Routes.ARG_MED_ID) }   // 인자가 아예 없는 화면이면 getLong이 0을 주므로 먼저 확인
        ?.getLong(Routes.ARG_MED_ID)
        ?.takeIf { it != Routes.NO_ID }

/**
 * "내 약" 쪽 화면들의 주소 등록과 그 사이의 이동. (구현 명세 03)
 * 목록으로 돌아갈 때는 backTo(S06)을 쓴다: 기록에 있는 목록까지 되돌아가므로 검색어가 그대로 남는다.
 *
 *   S06 목록 ─ 상세 › → S07 상세 ─ ✎ 약 수정 → S08 폼 ─ 변경사항 저장 → S33 적용 범위 ─ 저장 → S07
 *      │                  ├ 삭제 → S09 삭제 확인 ─ 삭제하기 → S06
 *      │                  ├ 처방 출처 메모 → S42
 *      │                  └ 과거 기록 보존 → S32
 *      └ ＋ 추가 → S08 폼 ─ 약 등록 완료 → (같은 이름이 있으면 S40 중복 확인 →) S07
 *                    └ 사진으로 등록 (준비 중) → S29
 *
 * 저장한 뒤 상세(S07)로 갈 때는 폼(S08)·S33·S40을 뒤로 가기 기록에서 걷어 낸다.
 * 그래야 상세에서 뒤로 갔을 때 폼이 다시 나오지 않는다.
 */
fun NavGraphBuilder.medicationGraph(user: User, go: AppNavigator) {

    // ----- S06 내 약 목록 -----
    composable(Routes.S06) {
        val vm: MedicationListViewModel = viewModel(factory = MedicationListViewModel.factory(user.id, user.isSample))
        val state by vm.uiState.collectAsStateWithLifecycle()
        OnResume(vm::refresh)
        S06MedicationListScreen(
            state = state,
            actions = remember(vm, go) {
                MedicationListActions(
                    onQueryChange = vm::onQueryChange,
                    onFilterChange = vm::onFilterChange,
                    onAdd = { go.open(Routes.s08New()) },
                    onOpen = { medId -> go.open(Routes.s07(medId)) },
                    onActiveChange = vm::setActive
                )
            }
        )
    }

    // ----- S07 약 상세 -----
    composable(Routes.S07, arguments = medArguments) { entry ->
        val vm: MedicationDetailViewModel =
            viewModel(factory = MedicationDetailViewModel.factory(user.id, entry.medIdOrNull()))
        val state by vm.uiState.collectAsStateWithLifecycle()
        OnResume(vm::refresh)
        val medId = state.med?.id
        S07MedicationDetailScreen(
            state = state,
            actions = MedicationDetailActions(
                onBackToList = { go.backTo(Routes.S06) },
                onPausedChange = vm::setPaused,
                onEdit = { medId?.let { go.open(Routes.s08Edit(it)) } },
                onDelete = { medId?.let { go.open(Routes.s09(it)) } },
                onOpenSource = { medId?.let { go.open(Routes.s42(it)) } },
                onOpenArchive = { go.open(Routes.S32) }
            )
        )
    }

    // ----- S09 삭제 확인 -----
    composable(Routes.S09, arguments = medArguments) { entry ->
        val vm: MedicationDetailViewModel =
            viewModel(factory = MedicationDetailViewModel.factory(user.id, entry.medIdOrNull()))
        val state by vm.uiState.collectAsStateWithLifecycle()
        // 삭제가 끝나면 목록으로. 기록에 있는 목록까지 돌아가므로 S07·S09는 사라지고 검색어는 남는다.
        LaunchedEffect(state.isDeleted) {
            if (state.isDeleted) go.backTo(Routes.S06)
        }
        S09DeleteConfirmScreen(
            state = state,
            // 바로 아래에 상세가 있으면 거기로 돌아가고, 없으면 그 약의 상세를 연다.
            onCancel = { go.backTo(Routes.S07, state.med?.let { Routes.s07(it.id) } ?: Routes.S07) },
            onDelete = vm::delete
        )
    }

    // ----- S08 약 등록/수정 -----
    composable(Routes.S08, arguments = medArguments) { entry ->
        val vm: MedicationEditViewModel =
            viewModel(factory = MedicationEditViewModel.factory(user.id, entry.medIdOrNull()))
        val state by vm.uiState.collectAsStateWithLifecycle()
        EditEvents(vm, go)
        S08MedicationFormScreen(
            state = state,
            actions = remember(vm, go) {
                MedicationFormActions(
                    onBackToList = { go.backTo(Routes.S06) },
                    onOpenPhoto = { go.open(Routes.S29) },
                    onNameChange = vm::onNameChange,
                    onDosageChange = vm::onDosageChange,
                    onMemoChange = vm::onMemoChange,
                    onStartActiveChange = vm::onStartActiveChange,
                    onTimeChange = vm::onTimeChange,
                    onConditionChange = vm::onConditionChange,
                    onRemoveTime = vm::removeTime,
                    onAddTime = vm::addTime,
                    onDayToggle = vm::onDayToggle,
                    onStartDateChange = vm::onStartDateChange,
                    onEndDateChange = vm::onEndDateChange,
                    onSubmit = vm::submit
                )
            }
        )
    }

    // ----- S33 일정 변경 적용 범위 (S08의 ViewModel을 함께 쓴다) -----
    composable(Routes.S33, arguments = medArguments) { entry ->
        val vm = sharedEditViewModel(user, go, entry)
        val state by vm.uiState.collectAsStateWithLifecycle()
        EditEvents(vm, go)
        S33EditScopeScreen(
            state = state,
            onBack = go::back,
            onScopeChange = vm::onScopeChange,
            onSave = vm::saveWithScope
        )
    }

    // ----- S40 중복 약·회차 확인 (S08의 ViewModel을 함께 쓴다) -----
    composable(Routes.S40) { entry ->
        val vm = sharedEditViewModel(user, go, entry)
        val state by vm.uiState.collectAsStateWithLifecycle()
        EditEvents(vm, go)
        S40DuplicateScreen(
            state = state,
            onBack = go::back,
            onConfirm = vm::confirmDuplicate,
            onRecheck = {
                vm.cancelDuplicate()
                go.back()
            }
        )
    }

    // ----- S42 처방 출처·진료과 -----
    composable(Routes.S42, arguments = medArguments) { entry ->
        val vm: PrescriptionSourceViewModel =
            viewModel(factory = PrescriptionSourceViewModel.factory(user.id, entry.medIdOrNull()))
        val state by vm.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(state.isDone, state.goToList) {
            when {
                state.isDone -> go.back()                      // 저장 끝 → 약 상세로
                state.goToList -> go.backTo(Routes.S06)        // 약을 찾지 못함 → 목록으로
            }
        }
        S42PrescriptionSourceScreen(
            state = state,
            onBack = go::back,
            onHospitalChange = vm::onHospitalChange,
            onDepartmentChange = vm::onDepartmentChange,
            onMemoChange = vm::onMemoChange,
            onSave = vm::save
        )
    }

    // ----- S32 과거 기록 보존 -----
    composable(Routes.S32) {
        val vm: ArchiveViewModel = viewModel(factory = ArchiveViewModel.factory(user.id, user.isSample))
        val state by vm.uiState.collectAsStateWithLifecycle()
        OnResume(vm::refresh)
        S32ArchiveScreen(
            state = state,
            onBack = go::back,
            onOpenDay = { date ->
                // 그 날짜를 선택 날짜로 하고 날짜별 기록(S12)을 연다. (S12는 4단계에서 만든다)
                vm.selectDay(date)
                go.open(Routes.S12)
            }
        )
    }

    // ----- S29 사진으로 약 등록 (시안) -----
    composable(Routes.S29) {
        S29PhotoRegisterScreen(onBack = go::back)
    }
}

/**
 * 적용 범위(S33)·중복 확인(S40)이 약 등록 화면(S08)의 ViewModel을 꺼내 쓴다.
 *
 * ViewModel은 "화면 항목"이 맡아 둔다. 보통은 자기 화면 항목에서 꺼내지만, 여기서는 뒤로 가기 기록에 있는
 * S08 항목에서 꺼낸다. 그러면 S08이 들고 있던 입력값을 그대로 쓰고, 돌아가도 입력이 남아 있다.
 * S08이 기록에 없으면(이 화면을 바로 연 경우) 자기 항목에서 새로 만든다 — 그때는 기다리는 등록·수정이 없다.
 *
 * remember(entry): 이 화면이 처음 그려질 때 한 번만 찾는다. 저장 뒤 화면이 빠져나가는 동안 다시 찾으면
 * 이미 S08이 사라져 엉뚱한 ViewModel을 새로 만들게 된다.
 */
@Composable
private fun sharedEditViewModel(user: User, go: AppNavigator, entry: NavBackStackEntry): MedicationEditViewModel {
    val owner = remember(entry) { go.entryOrNull(Routes.S08) ?: entry }
    return viewModel(
        viewModelStoreOwner = owner,
        // 약 번호는 ViewModel의 주인(S08) 항목에서 읽는다. S40처럼 약 번호 인자가 없는 화면에서도 맞게.
        factory = MedicationEditViewModel.factory(user.id, owner.medIdOrNull())
    )
}

/**
 * 등록/수정 ViewModel이 알려 주는 "이동하라"를 받아 실제로 이동한다.
 * LaunchedEffect(vm): 이 화면이 보이는 동안 통로(events)를 지켜본다. S08·S33·S40 중 지금 보이는 화면이 받는다.
 */
@Composable
private fun EditEvents(vm: MedicationEditViewModel, go: AppNavigator) {
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                MedicationEditEvent.GoToDuplicate -> go.open(Routes.S40)
                is MedicationEditEvent.GoToScope -> go.open(Routes.s33(event.medId))
                // 등록 끝: 폼(과 그 위의 S40)을 기록에서 빼고 새 약의 상세를 연다.
                is MedicationEditEvent.Registered -> go.replace(Routes.s07(event.medId), popUpTo = Routes.S08)
                // 수정 끝: 폼 아래에 있는 상세까지 돌아간다.
                is MedicationEditEvent.Updated -> go.backTo(Routes.S07, Routes.s07(event.medId))
                MedicationEditEvent.GoToList -> go.backTo(Routes.S06)
                MedicationEditEvent.BackToForm -> go.backTo(Routes.S08, Routes.s08New())
            }
        }
    }
}

/** 화면이 다시 보일 때마다 부른다 ("오늘"을 새로 읽는 데 쓴다). */
@Composable
private fun OnResume(action: () -> Unit) {
    LifecycleResumeEffect(action) {
        action()
        onPauseOrDispose { }
    }
}
