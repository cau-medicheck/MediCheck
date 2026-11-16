package com.cau.medicheck

import android.os.Bundle
import androidx.activity.ComponentActivity
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
        enableEdgeToEdge()   // 상태 표시줄 아래까지 화면을 넓게 쓴다
        setContent {
            MediCheckTheme {
                MediCheckApp()
            }
        }
    }
}
