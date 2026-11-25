package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.theme.DangerTitle
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.HeroDeepEnd
import com.cau.medicheck.ui.theme.HeroDeepStart
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/** 설정 허브의 한 줄: 색 네모 안 흰 아이콘 + 제목 + 누르면 갈 화면. bg는 쓰지 않고 tint가 네모 바탕색 (2026-11-25 아이콘 3번) */
private data class HubRow(val icon: ImageVector, val tint: Color, val bg: Color, val titleRes: Int, val route: String)

private val TealTint = Color(0xFF128B94); private val TealBg = Color(0xFFE3F5F2)
private val BlueTint = Color(0xFF3E73C5); private val BlueBg = Color(0xFFE6EEF9)
private val PurpleTint = Color(0xFF8A63C9); private val PurpleBg = Color(0xFFF1ECF9)
private val OrangeTint = Color(0xFFC96F12); private val OrangeBg = Color(0xFFFFF3E2)
private val GrayTint = Color(0xFF8A9AA1); private val GrayBg = Color(0xFFF1F5F6)

/** 묶음: 알림 */
private val NOTI_ROWS = listOf(
    HubRow(Icons.Filled.Notifications, TealTint, TealBg, R.string.s19_row_noti_title, Routes.S17),
    HubRow(Icons.Filled.Shield, BlueTint, BlueBg, R.string.s19_row_perm_title, Routes.S18)
)

/** 묶음: 내 정보 */
private val MINE_ROWS = listOf(
    HubRow(Icons.Filled.Lock, PurpleTint, PurpleBg, R.string.s19_row_privacy_title, Routes.S36),
    HubRow(Icons.Filled.History, OrangeTint, OrangeBg, R.string.s19_row_archive_title, Routes.S32)
)

/** 묶음: 화면 · 도움말 */
private val VIEW_ROWS = listOf(
    HubRow(Icons.Filled.TextFields, TealTint, TealBg, R.string.s19_row_a11y_title, Routes.S37),
    HubRow(Icons.Filled.Calculate, BlueTint, BlueBg, R.string.s19_row_policy_title, Routes.S20),
    HubRow(Icons.Filled.CloudOff, GrayTint, GrayBg, R.string.s19_row_offline_title, Routes.S38),
    HubRow(Icons.Filled.Info, GrayTint, GrayBg, R.string.s19_row_help_title, Routes.S22)
)

/** 묶음: 준비 중 (시안 화면들의 입구 + 전체 기능 목록) — 회색으로 조용히 */
private val SOON_ROWS = listOf(
    HubRow(Icons.Filled.Groups, GrayTint, GrayBg, R.string.s19_soon_family, Routes.S23),
    HubRow(Icons.Filled.PhotoCamera, GrayTint, GrayBg, R.string.s19_soon_photo, Routes.S29),
    HubRow(Icons.Filled.Checklist, GrayTint, GrayBg, R.string.s19_row_reqmap_title, Routes.S45)
)

/**
 * S19 설정 허브 (v3 settings()).
 *
 * 머리말의 ⚙를 누르면 오는 화면. 나머지 설정 화면들로 들어가는 입구이고, 로그아웃이 여기에만 있다.
 * 값을 들고 있지 않아서 ViewModel이 없다. 이름·이메일은 로그인한 사용자(user)에서 그대로 받는다.
 *
 * @param onOpen 줄을 눌렀을 때 그 화면의 주소(route)를 넘겨 받아 여는 함수
 * @param loggingOut 로그아웃 처리 중인지 (버튼을 두 번 누르지 못하게 한다)
 */
@Composable
fun S19SettingsScreen(
    displayName: String,
    email: String,
    onOpen: (String) -> Unit,
    onLogout: () -> Unit,
    loggingOut: Boolean = false
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s19)) {
        ScreenIntro(modifier = Modifier.padding(bottom = Gap16))

        // ----- 프로필 카드 (청록): 이름 첫 글자 동그라미 + 이름·이메일 + [수정 ›] -----
        val editLabel = stringResource(R.string.s19_cd_edit)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(HeroDeepStart, HeroDeepEnd)))
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.White).clearAndSetSemantics { }
            ) {
                Text(displayName.take(1), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = HeroDeepStart)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(stringResource(R.string.s19_name, displayName), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(email, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(top = 3.dp))
            }
            Text(
                text = stringResource(R.string.s19_edit),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = HeroDeepStart,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White)
                    .clickable(role = Role.Button, onClick = { onOpen(Routes.S21) })
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .semantics { contentDescription = editLabel }
            )
        }

        HubSection(stringResource(R.string.s19_section_noti), NOTI_ROWS, onOpen)
        HubSection(stringResource(R.string.s19_section_mine), MINE_ROWS, onOpen)
        HubSection(stringResource(R.string.s19_section_view), VIEW_ROWS, onOpen)
        HubSection(stringResource(R.string.s19_section_soon), SOON_ROWS, onOpen)

        // 로그아웃: 빨간 글자 단추
        Text(
            text = stringResource(R.string.action_logout),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (loggingOut) Muted else DangerTitle,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !loggingOut, role = Role.Button, onClick = onLogout)
                .padding(vertical = 16.dp)
        )
    }
}

/** 묶음 하나: 작은 제목 + 카드 안 줄들 */
@Composable
private fun HubSection(title: String, rows: List<HubRow>, onOpen: (String) -> Unit) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Navy,
        modifier = Modifier.padding(start = 2.dp, top = 20.dp, bottom = 8.dp).semantics { heading() }
    )
    SurfaceCard(padding = 0.dp) {
        Column(Modifier.padding(horizontal = 14.dp)) {
            rows.forEachIndexed { index, row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = { onOpen(row.route) })
                        .padding(vertical = 11.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(row.tint)
                    ) {
                        Icon(row.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                    Text(stringResource(row.titleRes), fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    Text("›", fontSize = 18.sp, color = Muted)
                }
                if (index != rows.lastIndex) HorizontalDivider(color = Line)
            }
        }
    }
}

