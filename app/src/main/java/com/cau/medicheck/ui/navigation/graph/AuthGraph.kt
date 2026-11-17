package com.cau.medicheck.ui.navigation.graph

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cau.medicheck.ui.navigation.AppNavigator
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.screens.auth.AuthViewModel
import com.cau.medicheck.ui.screens.auth.S00WelcomeScreen
import com.cau.medicheck.ui.screens.auth.S01LoginScreen
import com.cau.medicheck.ui.screens.auth.S02SignUpScreen
import com.cau.medicheck.ui.screens.auth.S03ForgotPasswordScreen

/**
 * 로그인 전 화면 4개(S00~S03)의 주소 등록과 그 사이의 이동. (구현 명세 01 부록 A-11)
 *
 * 화면 함수는 그리기만 하고, "버튼을 누르면 어디로 가나"는 여기서 정한다.
 * 로그인·가입에 성공했을 때의 이동은 여기 없다: AuthViewModel의 currentUser가 채워지면
 * MediCheckApp이 로그인 전 틀을 통째로 버리고 로그인 뒤 화면을 그린다.
 *
 * NavGraphBuilder.authGraph(…): NavHost { } 안에서 부를 수 있게 만든 확장 함수.
 * (확장 함수 = 이미 있는 클래스에 함수를 붙여 쓰는 Kotlin 문법. Java라면 static authGraph(builder, …)와 같다)
 */
fun NavGraphBuilder.authGraph(vm: AuthViewModel, go: AppNavigator) {

    composable(Routes.S00) {
        S00WelcomeScreen(onFinish = {
            vm.finishOnboarding()                       // 저장을 먼저 한다 (디스크에 다 쓴 뒤 돌아옴)
            go.replace(Routes.S01, popUpTo = Routes.S00) // S00을 기록에서 빼고 S01로 → S01에서 뒤로 = 앱 나가기
        })
    }

    composable(Routes.S01) {
        val state by vm.uiState.collectAsStateWithLifecycle()
        S01LoginScreen(
            state = state,
            onLogin = vm::login,
            onSample = vm::loginAsSample,
            onGoToSignUp = { go.open(Routes.S02) },
            onForgot = { go.open(Routes.S03) }
        )
    }

    composable(Routes.S02) {
        val state by vm.uiState.collectAsStateWithLifecycle()
        S02SignUpScreen(
            state = state,
            onSignUp = vm::signUp,
            // backTo: 두 번 빠르게 눌러도 S01이 남는다 (그냥 뒤로 두 번이면 빈 화면이 될 수 있음)
            onGoToLogin = { go.backTo(Routes.S01) }
        )
    }

    composable(Routes.S03) {
        S03ForgotPasswordScreen(
            onRequest = vm::requestPasswordHelp,
            onBack = { go.backTo(Routes.S01) }
        )
    }
}
