package com.cau.medicheck

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
}
