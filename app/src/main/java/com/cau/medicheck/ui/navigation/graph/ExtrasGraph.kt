package com.cau.medicheck.ui.navigation.graph

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.LocalToastBus
import com.cau.medicheck.ui.navigation.AppNavigator
import com.cau.medicheck.ui.navigation.NavRules
import com.cau.medicheck.ui.navigation.ReqTarget
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.screens.extras.S45ReqMapScreen
import com.cau.medicheck.ui.screens.extras.S45ReqMapViewModel
import com.cau.medicheck.ui.screens.extras.S46SafetyScopeScreen
import com.cau.medicheck.ui.screens.extras.S46SafetyScopeViewModel

/** 이 파일이 붙여 둔 화면 ID */
val EXTRAS_SCREENS = setOf("S45", "S46")

/**
 * 요구사항 맵·안전 고지 화면의 주소 등록과 이동. (구현 명세 07)
 *
 *   설정 허브 "전체 기능 목록" → S45 요구사항 맵 ─ 기능 줄 → 그 기능의 화면 (뒤로 가면 S45로 돌아온다)
 *   도움말 "안전·개발 범위"    → S46 안전·개발 범위 ─ 전체 요구사항 보기 → S45
 */
fun NavGraphBuilder.extrasGraph(go: AppNavigator) {

    // ----- S45 기능별 요구사항 맵 -----
    composable(Routes.S45) {
        val vm: S45ReqMapViewModel = viewModel(factory = S45ReqMapViewModel.Factory)
        val state by vm.uiState.collectAsStateWithLifecycle()
        val toastBus = LocalToastBus.current
        S45ReqMapScreen(
            state = state,
            onRowClick = { requirement ->
                when (val target = NavRules.reqTarget(requirement.screens)) {
                    // 탭의 첫 화면(홈·일정 등)이어도 openTab이 아니라 open으로 쌓는다 — 뒤로 가기로 여기에 돌아오게.
                    // "S07"처럼 인자 없이 열면 그 화면이 무엇을 보여 줄지 스스로 정한다(최근에 연 약 등).
                    is ReqTarget.Open -> go.open(target.screenId)
                    ReqTarget.PreLoginOnly -> toastBus.show(R.string.common_toast_prelogin_screen)
                    ReqTarget.Stay -> Unit   // 화면이 이런 줄은 누를 수 없게 그리므로 여기로 오지 않는다 (안전망)
                }
            }
        )
    }

    // ----- S46 안전·개발 범위 -----
    composable(Routes.S46) {
        val vm: S46SafetyScopeViewModel = viewModel(factory = S46SafetyScopeViewModel.Factory)
        val state by vm.uiState.collectAsStateWithLifecycle()
        S46SafetyScopeScreen(
            state = state,
            onBack = go::back,
            // 지나온 S45가 있으면 거기까지 돌아가고(검색어가 그대로 남는다), 없으면 새로 연다.
            onOpenMap = { go.openOrReturn(Routes.S45) }
        )
    }
}
