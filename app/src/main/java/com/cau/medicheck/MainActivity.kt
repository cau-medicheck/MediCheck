package com.cau.medicheck

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cau.medicheck.ui.navigation.DeepLinkInbox
import com.cau.medicheck.ui.navigation.MediCheckApp
import com.cau.medicheck.ui.theme.MediCheckTheme

/**
 * 앱 아이콘을 누르면 열리는 화면(Activity).
 * 이 앱은 Activity가 하나뿐이고, 그 안에서 Compose가 화면을 바꿔 가며 그린다.
 * 어떤 화면을 그릴지는 MediCheckApp이 정한다.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 알림을 눌러 앱이 새로 켜진 경우: 알림에 실린 회차를 읽어 둔다.
        //   savedInstanceState == null → 화면을 돌려서 다시 만들어진 경우에는 또 읽지 않는다.
        //   LAUNCHED_FROM_HISTORY → '최근 앱' 목록에서 다시 연 경우에는 옛 알림 값으로 지난 회차를 또 열지 않는다.
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromHistory) readDeepLink(intent)
        // 상태 표시줄·내비게이션 바 아래까지 화면을 넓게 쓴다(edge-to-edge).
        // SystemBarStyle.light: 폰이 어두운 모드여도 시스템 막대의 아이콘(시계·배터리)을 늘 어둡게 그린다.
        // 우리 앱은 늘 밝은 화면(흰 머리말)이라, 기본값을 쓰면 어두운 모드에서 흰 아이콘이 흰 머리말 위에 올라가 안 보인다.
        // (여기의 Color는 Compose 색이 아니라 안드로이드 기본 색 android.graphics.Color)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            MediCheckTheme {
                MediCheckApp()
            }
        }
    }

    /** 앱이 이미 떠 있을 때 알림을 누르면 화면을 새로 만들지 않고 여기로 알림 값이 들어온다. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readDeepLink(intent)
    }

    /**
     * 알림에 실려 온 "이 회차를 열어 달라"는 값을 읽어 DeepLinkInbox에 넣는다. 값이 없거나 틀리면 아무것도 하지 않는다.
     * 읽은 값은 지운다 — 같은 값을 두 번 처리하지 않게.
     */
    private fun readDeepLink(intent: Intent) {
        val key = DeepLinkInbox.parse(
            openScreen = intent.getStringExtra(DeepLinkInbox.EXTRA_OPEN_SCREEN),
            scheduleId = intent.getLongExtra(DeepLinkInbox.EXTRA_SCHEDULE_ID, -1L),
            date = intent.getStringExtra(DeepLinkInbox.EXTRA_DATE)
        )
        intent.removeExtra(DeepLinkInbox.EXTRA_OPEN_SCREEN)
        intent.removeExtra(DeepLinkInbox.EXTRA_SCHEDULE_ID)
        intent.removeExtra(DeepLinkInbox.EXTRA_DATE)
        if (key != null) (application as MediCheckApplication).container.deepLinks.offer(key)
    }
}
