package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.UserSettings
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.GroupLabel
import com.cau.medicheck.ui.components.IconTile
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.McSwitch
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.theme.CardBorder
import com.cau.medicheck.ui.theme.DangerTitle
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.Teal

/** 이 화면에서 누를 수 있는 것들을 한 묶음으로 받는다 (인자가 길어지지 않게) */
data class PrivacyActions(
    val onBack: () -> Unit,
    val onHideMedName: (Boolean) -> Unit,
    val onHideStatsPreview: (Boolean) -> Unit,
    val onOpen: (String) -> Unit
)

// 색 네모 (S17과 같은 색)
private val TileBlue = Color(0xFF4779C5)
private val TilePurple = Color(0xFF8064C1)
private val TileAmber = Color(0xFFBD762A)

/**
 * S36 개인정보 센터 (v3 privacyCenter()). 2026-11-25 시안 C1(04_화면캡처/남은화면_시안/S36_C정리.html)로 다시 그림.
 *
 * "화면 가리기" 스위치 두 개는 나란히 놓인 칸이다. 칸 어디를 눌러도 켜고 끈다.
 *   약 이름 숨김 → 휴대폰 알림에 약 이름 대신 일반 문구를 쓴다
 *   통계 숨김   → 최근 앱 목록에 앱 화면 미리보기가 보이지 않게 한다
 * "내 데이터" 줄 셋은 앱 잠금·백업·내보내기 화면의 입구. 계정·데이터 삭제는 맨 아래 빨간 글자(설정의 로그아웃과 같은 모양).
 */
@Composable
fun S36PrivacyScreen(settings: UserSettings, actions: PrivacyActions) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s36)) {
        BackLink(stringResource(R.string.common_back_previous), actions.onBack)
        ScreenIntro(modifier = Modifier.padding(top = Gap16, bottom = 20.dp))

        GroupLabel(stringResource(R.string.s36_section_hide))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            HideTile(
                icon = Icons.Outlined.VisibilityOff,
                color = Teal,
                label = stringResource(R.string.s36_tile_hide_name),
                fullLabel = stringResource(R.string.s36_hide_name_title),
                checked = settings.hideMedName,
                onChange = actions.onHideMedName,
                modifier = Modifier.weight(1f)
            )
            HideTile(
                icon = Icons.Outlined.BarChart,
                color = TileBlue,
                label = stringResource(R.string.s36_tile_hide_stats),
                fullLabel = stringResource(R.string.s36_hide_stats_title),
                checked = settings.hideStatsPreview,
                onChange = actions.onHideStatsPreview,
                modifier = Modifier.weight(1f)
            )
        }

        GroupLabel(stringResource(R.string.s36_section_data), Modifier.padding(top = 22.dp))
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 15.dp)) {
                DataRow(Icons.Outlined.Lock, TilePurple, R.string.s36_row_lock_title) { actions.onOpen(Routes.S41) }
                HorizontalDivider(color = Line, modifier = Modifier.padding(start = 46.dp))
                DataRow(Icons.Outlined.CloudUpload, TileAmber, R.string.s36_row_backup_title) { actions.onOpen(Routes.S28) }
                HorizontalDivider(color = Line, modifier = Modifier.padding(start = 46.dp))
                DataRow(Icons.Outlined.FileDownload, Teal, R.string.s36_row_export_title) { actions.onOpen(Routes.S43) }
            }
        }

        // 계정·데이터 삭제: 빨간 글자 단추
        Text(
            text = stringResource(R.string.s36_row_delete_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DangerTitle,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = { actions.onOpen(Routes.S44) })
                .padding(vertical = 16.dp)
        )
    }
}

/** 스위치 칸: 위 줄 [색 네모 ··· 스위치], 아래 짧은 이름. TalkBack은 긴 이름으로 읽는다 */
@Composable
private fun HideTile(
    icon: ImageVector,
    color: Color,
    label: String,
    fullLabel: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardBorder, shape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .semantics(mergeDescendants = true) { contentDescription = fullLabel }
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconTile(icon, color, size = 32.dp, iconSize = 19.dp)
            Row(Modifier.weight(1f)) {}
            McSwitch(checked = checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { })
        }
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier.padding(top = 12.dp).clearAndSetSemantics { }
        )
    }
}

/** 데이터 줄: 색 네모 + 이름 + › */
@Composable
private fun DataRow(icon: ImageVector, color: Color, titleRes: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 58.dp)
    ) {
        IconTile(icon, color, size = 32.dp, iconSize = 19.dp)
        Text(
            stringResource(titleRes),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Navy,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        Text("›", fontSize = 20.sp, color = Muted)
    }
}
