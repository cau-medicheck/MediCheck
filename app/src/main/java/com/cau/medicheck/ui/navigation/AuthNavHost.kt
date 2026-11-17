package com.cau.medicheck.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.cau.medicheck.MediCheckApplication
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.cau.medicheck.ui.navigation.graph.authGraph
import com.cau.medicheck.ui.screens.auth.AuthViewModel

/**
 * 로그인하기 전의 화면 묶음(S00 온보딩 · S01 로그인 · S02 회원가입 · S03 비밀번호 재설정).
 *
 * NavHost: 주소(route)에 맞는 화면 하나를 골라 보여 주는 틀.
 * navController: 화면을 옮기는 리모컨. 화면 코드는 이것을 직접 만지지 않고 AppNavigator를 부른다.
 *
 * 화면 전환 애니메이션은 모두 끈다. v3 프로토타입은 화면이 즉시 바뀌고, 움직임 최소화와도 맞기 때문이다.
 *
 * @param startRoute 처음 보여 줄 화면 (S00 또는 S01 — NavRules.startRoute가 고른 값)
 */
@Composable
fun AuthNavHost(startRoute: String, vm: AuthViewModel) {
    val nav = rememberNavController()
    val go = remember(nav) { AppNavigator(nav) }

    // 로그인 전에 알림을 눌러 받아 둔 회차는 버린다 (로그인한 뒤 엉뚱한 회차가 열리지 않게).
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        (context.applicationContext as MediCheckApplication).container.deepLinks.clear()
    }

    NavHost(
        navController = nav,
        startDestination = startRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        authGraph(vm, go)
    }
}
