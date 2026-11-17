package com.cau.medicheck.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cau.medicheck.data.local.User
import com.cau.medicheck.ui.components.McBottomBar
import com.cau.medicheck.ui.components.McBottomBarHeight
import com.cau.medicheck.ui.components.McTab
import com.cau.medicheck.ui.components.McTopBar
import com.cau.medicheck.ui.navigation.graph.mainGraph
import com.cau.medicheck.ui.theme.ScreenBackground

/**
 * 4단계에서 새로 만들기 전까지 예전 화면을 임시로 연결해 둔 곳(통계).
 * 이 화면은 자기 머리말(TopAppBar)을 직접 그리므로 틀의 머리말을 겹쳐 그리지 않는다.
 */
private val LEGACY_SCREENS = setOf("S13")

/**
 * 로그인한 뒤의 화면 틀. (구현 명세 00 §1, 02 "공통 틀")
 *
 *   ┌ 머리말 McTopBar ─ "MEDICATION CARE" + 화면 이름 + ♧ 알림 · ⚙ 설정 ┐
 *   │ NavHost ─ 주소(route)에 맞는 화면 하나                           │
 *   └ 하단 5탭 McBottomBar ─ 홈 · 일정 · 내 약 · 기록 · 통계            ┘
 *
 * 머리말과 하단 탭은 여기서 한 번만 그리고, 각 화면은 본문만 그린다.
 * 머리말 제목과 강조할 탭은 "지금 보이는 화면의 주소"에서 정한다.
 *
 * @param user 로그인한 사용자. 이 사용자의 데이터만 보여 주기 위해 id를 ViewModel에 넘긴다.
 */
@Composable
fun MainNavHost(user: User, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val go = remember(nav) { AppNavigator(nav) }

    // 지금 보이는 화면의 주소. 화면이 바뀌면 이 값도 바뀌어 머리말·탭이 다시 그려진다.
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.S04
    val screenId = NavRules.screenIdOf(route)
    val activeTab = McTab.entries.find { it.route == NavRules.tabRootOf(route) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        if (screenId !in LEGACY_SCREENS) {
            McTopBar(
                title = stringResource(Routes.titleRes(route)),
                showActions = true,
                // 이미 그 화면을 거쳐 왔으면 거기까지 되돌아가고, 아니면 새로 연다.
                onNotifications = { go.openOrReturn(Routes.S16) },
                onSettings = { go.openOrReturn(Routes.S19) }
            )
        }
        // weight(1f): 머리말과 하단 탭을 뺀 나머지 높이를 화면이 모두 차지한다.
        // consumeWindowInsets: 폰의 내비게이션 바와 하단 탭 높이는 이미 띄웠다고 안쪽 화면에 알려 준다.
        // (안 알려 주면 아래 여백이 두 번 들어가고, 키보드가 올라올 때 탭 높이만큼 빈 자리가 생긴다)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(WindowInsets.navigationBars.add(WindowInsets(bottom = McBottomBarHeight)))
        ) {
            NavHost(
                navController = nav,
                startDestination = Routes.S04,
                // 화면 전환 애니메이션은 끈다 (프로토타입은 화면이 즉시 바뀐다).
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                mainGraph(user, go, onLogout)
            }
        }
        McBottomBar(active = activeTab, onTab = { tab -> go.openTab(tab.route) })
    }
}
