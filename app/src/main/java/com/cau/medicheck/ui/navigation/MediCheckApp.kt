package com.cau.medicheck.ui.navigation

import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import com.cau.medicheck.ui.components.ToastMessage
import com.cau.medicheck.ui.components.TOAST_WITH_ACTION_DURATION_MS
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.ui.components.LocalA11ySettings
import com.cau.medicheck.ui.components.LocalReduceMotion
import com.cau.medicheck.ui.components.LocalToastBus
import com.cau.medicheck.ui.components.McToastHost
import com.cau.medicheck.ui.components.TOAST_DURATION_MS
import com.cau.medicheck.ui.components.toastBottomOffset
import com.cau.medicheck.ui.lock.LocalAppLocked
import com.cau.medicheck.ui.lock.LockOverlay
import com.cau.medicheck.ui.lock.rememberUnlockAction
import com.cau.medicheck.ui.navigation.graph.SettingsCallbacks
import com.cau.medicheck.ui.screens.auth.AuthViewModel
import com.cau.medicheck.ui.theme.PreLoginFrameBottom
import com.cau.medicheck.ui.theme.ScreenBackground
import com.cau.medicheck.ui.theme.Teal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * 앱의 가장 바깥 화면. 세 가지를 여기서 한다. (구현 명세 00 §3.1)
 *
 * 1) 어떤 화면 묶음을 보여 줄지 정한다.
 *      로그인 기록 확인 중  → 로딩 화면
 *      로그인 안 됨        → 로그인 전 묶음 AuthNavHost (S00~S03, 시작 화면은 authStartRoute)
 *      로그인 됨          → 로그인 뒤 묶음 MainNavHost (S04 홈부터)
 *    로그인에 성공하면 AuthViewModel의 currentUser가 채워지고, 그 값을 보고 있는 이 함수가 다시 그려져
 *    저절로 로그인 뒤 화면으로 바뀐다. (그래서 "다음 화면으로 이동하라"는 코드를 따로 쓰지 않는다)
 *
 * 2) 토스트를 띄운다. 화면 묶음이 바뀌어도 토스트가 남도록 맨 바깥에 하나만 둔다(00 §1.5).
 *
 * 3) 접근성 설정(AppPrefs)을 읽어 아래 모든 화면이 꺼내 쓸 수 있게 넣어 둔다(00 §6).
 */
@Composable
fun MediCheckApp(authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)) {
    // uiState가 바뀔 때마다 state도 바뀌고, 이 함수가 다시 그려진다.
    val state by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val container = (context.applicationContext as MediCheckApplication).container
    val toastBus = container.toastBus
    val a11y by container.appPrefs.a11y.collectAsStateWithLifecycle()

    // ----- 토스트 -----
    // collectLatest: 새 문구가 오면 앞 문구의 2.9초 기다림을 끊고 새로 센다 (= v3의 clearTimeout).
    // 버튼이 붙은 토스트(실행취소)는 5초. 폰의 접근성 설정에서 "조치를 취할 시간"을 늘려 두었으면 그만큼 더 길게.
    var toast by remember { mutableStateOf<ToastMessage?>(null) }
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(toastBus) {
        toastBus.messages.collectLatest { message ->
            toast = message
            val base = if (message.action != null) TOAST_WITH_ACTION_DURATION_MS else TOAST_DURATION_MS
            delay(
                accessibility?.calculateRecommendedTimeoutMillis(
                    originalTimeoutMillis = base,
                    containsText = true,
                    containsControls = message.action != null
                ) ?: base
            )
            toast = null
        }
    }

    // ----- 앱 잠금 (S41) -----
    // 잠겨 있으면 아래에서 잠금 화면을 맨 위에 덮는다. 로그인 전 화면에는 보호할 기록이 없어 잠그지 않는다.
    val lockedFlag by container.appLock.locked.collectAsStateWithLifecycle()
    val locked = lockedFlag && state.currentUser != null && !state.isCheckingSession
    val requestUnlock = rememberUnlockAction()
    // 잠긴 채로 앱에 들어올 때마다(새로 켰을 때, 나갔다 돌아왔을 때) 본인 확인 창을 자동으로 한 번 띄운다.
    // 확인 창을 취소한 뒤에는 자동으로 다시 띄우지 않는다(잠금 화면의 "잠금 해제" 버튼으로 다시 시도).
    val promptRequests by container.appLock.promptRequests.collectAsStateWithLifecycle()
    LaunchedEffect(promptRequests, locked) {
        if (locked) requestUnlock()
    }
    // 잠기는 순간 입력칸의 커서를 뺀다. 그대로 두면 잠금 화면 위로 키보드가 남아 아래 화면의 입력칸에 글자가 들어간다.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(locked) {
        if (locked) focusManager.clearFocus(force = true)
    }

    CompositionLocalProvider(
        LocalToastBus provides toastBus,
        LocalA11ySettings provides a11y,
        LocalReduceMotion provides a11y.reduceMotion,
        LocalAppLocked provides locked
    ) {
        // 잠겨 있는 동안 아래 화면은 ① TalkBack이 읽지 못하고 ② 키보드로도 조작하지 못한다.
        // 터치는 잠금 화면이 받아 버리지만, 블루투스 키보드 같은 외장 키보드의 Tab·Enter는 가려진 버튼까지 닿는다.
        // focusProperties(canFocus = false): 아래 부품들이 키보드 포커스를 받지 못하게 한다(Tab으로 가려진 버튼에 들어가지 않게).
        // onPreviewKeyEvent에서 true를 돌려주면 그 키 입력은 아래 부품에 전해지지 않는다(이중 안전장치).
        val lockedGuard = if (locked) {
            Modifier.clearAndSetSemantics { }.focusProperties { canFocus = false }.onPreviewKeyEvent { true }
        } else {
            Modifier
        }
        Box(Modifier.fillMaxSize().then(lockedGuard)) {
            val user = state.currentUser
            when {
                state.isCheckingSession -> LoadingScreen()
                user == null -> AuthNavHost(startRoute = state.authStartRoute, vm = authViewModel)
                // key(user.id): 다른 계정으로 로그인하면 안쪽 화면들을 처음부터 새로 만든다.
                // (앞 계정의 화면 상태가 남아 있지 않게 하기 위해서다.)
                else -> key(user.id) {
                    // remember: 값이 바뀔 때만 새 묶음을 만든다. 매번 새로 만들면 토스트가 뜰 때마다
                    // 안쪽 화면 주소 목록(NavHost의 그래프)까지 다시 지어진다.
                    val settings = remember(
                        authViewModel, state.isLoggingOut, state.isLoading, state.isDeletingAccount
                    ) {
                        SettingsCallbacks(
                            onLogout = authViewModel::logout,
                            loggingOut = state.isLoggingOut,
                            onUpdateProfile = authViewModel::updateProfile,
                            profileSaving = state.isLoading,
                            onDeleteAccount = authViewModel::deleteAccount,
                            deletingAccount = state.isDeletingAccount
                        )
                    }
                    MainNavHost(user = user, settings = settings)
                }
            }

            // 토스트 위치: 틀 아래쪽(로그인 전 = 빈 자리 59dp, 로그인 뒤 = 하단 탭 59dp) 위 18dp.
            // 키보드가 열려 있으면 키보드 위 18dp.
            val density = LocalDensity.current
            val navBar = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
            val ime = with(density) { WindowInsets.ime.getBottom(this).toDp() }
            val shown = toast
            McToastHost(
                text = shown?.let { stringResource(it.text) },
                bottomOffset = toastBottomOffset(navBar + PreLoginFrameBottom, ime),
                modifier = Modifier.align(Alignment.BottomCenter),
                actionLabel = shown?.action?.let { stringResource(it.label) },
                onAction = {
                    shown?.action?.run?.invoke()
                    toast = null   // 누르면 토스트를 바로 숨긴다
                }
            )
        }
        // 잠금 화면: 로그인 뒤 화면과 토스트까지 모두 덮는다. 아래 화면은 그대로 살아 있다(풀면 보던 화면 그대로).
        if (locked) LockOverlay(onUnlock = requestUnlock)
    }
}

/** 앱을 켜고 로그인 기록을 확인하는 잠깐 동안 보이는 화면 (화면 바탕 + 가운데 청록 원형 표시) */
@Composable
private fun LoadingScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        CircularProgressIndicator(color = Teal)
    }
}
