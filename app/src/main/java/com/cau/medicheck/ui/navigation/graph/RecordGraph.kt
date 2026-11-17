package com.cau.medicheck.ui.navigation.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cau.medicheck.data.local.User
import com.cau.medicheck.ui.navigation.AppNavigator
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.screens.dose.DayDosesViewModel
import com.cau.medicheck.ui.screens.records.CsvExportViewModel
import com.cau.medicheck.ui.screens.records.RecordsCalendarViewModel
import com.cau.medicheck.ui.screens.records.S11RecordsCalendarScreen
import com.cau.medicheck.ui.screens.records.S12RecordsDetailScreen
import com.cau.medicheck.ui.screens.records.S43CsvExportScreen
import com.cau.medicheck.ui.screens.stats.MonthStatsViewModel
import com.cau.medicheck.ui.screens.stats.S13WeekStatsScreen
import com.cau.medicheck.ui.screens.stats.S14MonthStatsScreen
import com.cau.medicheck.ui.screens.stats.S15SkipPatternScreen
import com.cau.medicheck.ui.screens.stats.SkipPatternViewModel
import com.cau.medicheck.ui.screens.stats.WeekStatsViewModel

/** 이 파일이 붙여 둔 화면 ID */
val RECORD_SCREENS = setOf("S11", "S12", "S13", "S14", "S15", "S43")

/** 통계 화면 위쪽 탭의 순서: 주간 · 월간 · 누락 분석 */
private val STATS_TABS = listOf(Routes.S13, Routes.S14, Routes.S15)

/**
 * 기록·통계 화면들의 주소 등록과 그 사이의 이동. (구현 명세 04)
 *
 *   기록 탭 → S11 기록 캘린더 ─ 기록 보기 › → S12 날짜별 기록 ─ 상세 › → S10 회차 상세
 *   통계 탭 → S13 주간 ⇄ S14 월간 ⇄ S15 건너뜀 패턴   (위쪽 탭으로 서로 "바꿔 끼운다")
 *   S43 복약 기록 내보내기 — 6단계의 개인정보 센터(S36)에서 들어온다. 그 전까지는 설정(⚙) 임시 화면에 길을 둔다.
 */
fun NavGraphBuilder.recordGraph(user: User, go: AppNavigator) {

    // ----- S11 기록 캘린더 -----
    composable(Routes.S11) {
        val vm: RecordsCalendarViewModel = viewModel(factory = RecordsCalendarViewModel.factory(user.id))
        val state by vm.uiState.collectAsStateWithLifecycle()
        OnResume(vm::refresh)
        S11RecordsCalendarScreen(
            state = state,
            onShiftMonth = vm::shiftMonth,
            onSelectDay = vm::selectDay,
            onOpenDay = { go.open(Routes.S12) }   // 날짜는 주소가 아니라 "선택 날짜"로 전해진다
        )
    }

    // ----- S12 날짜별 기록 (홈·일정과 같은 ViewModel: 선택 날짜의 회차 목록) -----
    composable(Routes.S12) {
        val vm: DayDosesViewModel = viewModel(factory = DayDosesViewModel.factory(user.id))
        val state by vm.uiState.collectAsStateWithLifecycle()
        OnResume(vm::refresh)
        S12RecordsDetailScreen(
            state = state,
            // 기록에 캘린더가 있으면 거기까지 돌아가고, 없으면(과거 기록 보존 등에서 바로 온 경우) 기록 탭을
            // 누른 것처럼 간다. 그래야 캘린더에서 뒤로 갔을 때 엉뚱한 화면이 다시 나오지 않는다.
            onBackToCalendar = {
                if (go.entryOrNull(Routes.S11) != null) go.backTo(Routes.S11) else go.openTab(Routes.S11)
            },
            onOpenDose = { key -> go.open(Routes.s10(key)) }
        )
    }

    // ----- S13 주간 통계 -----
    composable(Routes.S13) {
        val vm: WeekStatsViewModel = viewModel(factory = WeekStatsViewModel.factory(user.id))
        val state by vm.uiState.collectAsStateWithLifecycle()
        OnResume(vm::refresh)
        S13WeekStatsScreen(state = state, onTab = { index -> go.swapStatsTab(index) })
    }

    // ----- S14 월간 통계 -----
    composable(Routes.S14) {
        val vm: MonthStatsViewModel = viewModel(factory = MonthStatsViewModel.factory(user.id))
        val state by vm.uiState.collectAsStateWithLifecycle()
        OnResume(vm::refresh)
        S14MonthStatsScreen(
            state = state,
            onTab = { index -> go.swapStatsTab(index) },
            onShiftMonth = vm::shiftMonth
        )
    }

    // ----- S15 건너뜀 패턴 -----
    composable(Routes.S15) {
        val vm: SkipPatternViewModel = viewModel(factory = SkipPatternViewModel.factory(user.id))
        val state by vm.uiState.collectAsStateWithLifecycle()
        OnResume(vm::refresh)
        S15SkipPatternScreen(state = state, onTab = { index -> go.swapStatsTab(index) })
    }

    // ----- S43 복약 기록 내보내기 -----
    composable(Routes.S43) {
        val vm: CsvExportViewModel = viewModel(factory = CsvExportViewModel.factory(user.id))
        val state by vm.uiState.collectAsStateWithLifecycle()
        OnResume(vm::refresh)
        S43CsvExportScreen(
            state = state,
            onBack = go::back,
            onExport = vm::export,
            onSaveScreenUnavailable = vm::onSaveScreenUnavailable
        )
    }
}

/**
 * 통계 화면 위쪽 탭을 눌렀을 때: 지금 통계 화면을 그 화면으로 "바꿔 끼운다"(쌓지 않는다).
 * 그래서 주간 → 월간 → 누락 분석을 오간 뒤 뒤로 가기를 한 번 누르면 통계에 들어오기 전 화면으로 간다.
 * 지금 화면의 탭을 다시 누르면 아무 일도 하지 않는다.
 */
private fun AppNavigator.swapStatsTab(index: Int) = swap(STATS_TABS[index])

/** 화면이 다시 보일 때마다 부른다 (지금 시각을 다시 읽고, 기록을 오늘까지 채운다). */
@Composable
private fun OnResume(action: () -> Unit) {
    LifecycleResumeEffect(action) {
        action()
        onPauseOrDispose { }
    }
}
