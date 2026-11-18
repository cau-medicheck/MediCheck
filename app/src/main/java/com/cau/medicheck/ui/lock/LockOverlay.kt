package com.cau.medicheck.ui.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.AuthLogo
import com.cau.medicheck.ui.components.AuthSub
import com.cau.medicheck.ui.components.AuthTitle
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.theme.ScreenBackground
import kotlinx.coroutines.launch

/**
 * 앱이 잠겨 있는지. 따로 뜨는 창(날짜·시각 고르기, 확인 창)은 잠금 화면 위에 그려질 수 있어서,
 * 이 값이 true인 동안에는 그런 창을 그리지 않는다(상태는 그대로 두므로 잠금을 풀면 다시 보인다).
 */
val LocalAppLocked = compositionLocalOf { false }

/**
 * 잠금 화면: 앱 전체를 덮는다. (S41 앱 잠금)
 *
 * 아래에 깔린 화면은 지우지 않고 가리기만 한다 — 잠금을 풀면 보던 화면 그대로 돌아온다.
 * 덮고 있는 동안 아래 화면은 눌리지 않고, 뒤로 가기는 "아래 화면으로"가 아니라 "앱을 뒤로 보내기"가 된다.
 *
 * @param onUnlock "잠금 해제" 버튼: 본인 확인 창을 다시 띄운다
 */
@Composable
fun LockOverlay(onUnlock: () -> Unit) {
    val activity = LocalContext.current.findComponentActivity()
    // 뒤로 가기: 앱을 홈 화면 뒤로 보낸다 (아래에 깔린 화면으로 넘어가지 않게).
    BackHandler { activity?.moveTaskToBack(true) }

    val screenName = stringResource(R.string.lock_unlock)
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            // 빈 곳을 눌러도 아래 화면으로 전해지지 않게, 이 화면이 모든 누름을 받아 버린다(눌러도 아무 일 없음).
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            .semantics { paneTitle = screenName }
            .safeDrawingPadding()
            .padding(horizontal = 28.dp)
    ) {
        AuthLogo()
        Spacer(Modifier.height(16.dp))
        AuthTitle(stringResource(R.string.app_name))
        AuthSub(stringResource(R.string.lock_body), Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(22.dp))
        // 잠금 화면이 뜨면 키보드 포커스를 이 버튼에 둔다 — 키보드만 쓰는 사람도 Enter로 바로 다시 시도할 수 있게.
        val unlockFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { unlockFocus.requestFocus() } }
        McButton(
            text = stringResource(R.string.lock_unlock),
            onClick = onUnlock,
            full = true,
            modifier = Modifier.focusRequester(unlockFocus)
        )
    }
}

/**
 * "본인 확인을 띄우고, 통과하면 잠금을 푼다"를 하는 함수를 만들어 준다.
 * 잠금 화면이 처음 보일 때 한 번, 그리고 "잠금 해제" 버튼을 누를 때마다 부른다.
 *
 * 기기의 화면 잠금이 그사이 없어졌으면 본인을 확인할 방법이 없다. 그때는 사용자가 갇히지 않게
 * 앱 잠금을 끄고(저장) 풀어 준 뒤 토스트로 알린다.
 */
@Composable
fun rememberUnlockAction(): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.lock_prompt_title)
    val subtitle = stringResource(R.string.lock_prompt_subtitle)
    return remember(context, scope, title, subtitle) {
        action@{
            val container = (context.applicationContext as MediCheckApplication).container
            val appLock = container.appLock
            val activity = context.findComponentActivity() ?: return@action
            if (appLock.authenticating) return@action   // 확인 창이 이미 떠 있다

            if (!DeviceAuth.isDeviceSecure(context)) {
                scope.launch {
                    runCatching { container.settingsRepository.update { it.copy(appLockEnabled = false) } }
                    appLock.unlock()
                    container.toastBus.show(R.string.s41_toast_auto_off)
                }
                return@action
            }
            appLock.authenticating = true
            val method = container.settingsRepository.current.value.appLockMethod
            DeviceAuth.prompt(activity, method, title, subtitle) { ok ->
                appLock.authenticating = false
                if (ok) appLock.unlock()   // 취소·실패면 잠금 화면 그대로. "잠금 해제" 버튼으로 다시 시도한다
            }
        }
    }
}
