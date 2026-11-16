package com.cau.medicheck.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cau.medicheck.ui.screens.home.HomeScreen
import com.cau.medicheck.ui.screens.login.AuthUiState
import com.cau.medicheck.ui.screens.login.AuthViewModel
import com.cau.medicheck.ui.screens.login.LoginScreen
import com.cau.medicheck.ui.screens.login.SignUpScreen

/** 화면마다 붙이는 주소(이름). 화면을 옮길 때 이 이름을 쓴다. */
object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
}

/**
 * 앱의 가장 바깥 화면. "지금 어떤 화면을 보여 줄지"를 여기서 정한다.
 *
 *   로그인 기록 확인 중  → 돌아가는 표시
 *   로그인 안 됨        → 로그인·회원가입 화면
 *   로그인 됨          → 홈 화면
 *
 * 로그인에 성공하면 AuthViewModel의 currentUser가 채워지고,
 * 그 값을 보고 있는 이 함수가 자동으로 다시 그려져서 홈 화면으로 바뀐다.
 * (그래서 "홈으로 이동하라"는 코드를 따로 쓰지 않는다. 로그아웃도 같은 원리다.)
 */
@Composable
fun MediCheckApp(authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)) {
    // uiState가 바뀔 때마다 state도 바뀌고, 이 함수가 다시 그려진다.
    val state by authViewModel.uiState.collectAsStateWithLifecycle()
    val user = state.currentUser

    when {
        state.isCheckingSession -> LoadingScreen()
        user == null -> AuthNavHost(state, authViewModel)
        else -> HomeScreen(user = user, onLogout = authViewModel::logout)
    }
}

/**
 * 로그인하기 전의 화면들(로그인 ↔ 회원가입)과 그 사이의 이동.
 *
 * NavHost: 여러 화면 중 하나를 보여 주는 틀.
 * navController: 화면을 옮기는 리모컨 (navigate = 다음 화면으로, popBackStack = 이전 화면으로).
 */
@Composable
private fun AuthNavHost(state: AuthUiState, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                state = state,
                onLogin = authViewModel::login,
                onGoToSignUp = {
                    authViewModel.clearError()
                    navController.navigate(Routes.SIGN_UP)
                },
                onInputChanged = authViewModel::clearError
            )
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(
                state = state,
                onSignUp = authViewModel::signUp,
                onBackToLogin = {
                    authViewModel.clearError()
                    navController.popBackStack()
                },
                onInputChanged = authViewModel::clearError
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}
