package com.cau.medicheck.ui.screens.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
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
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.Teal

/** S17에서 누를 수 있는 것들. 화면은 그리기만 하고, 눌렀을 때 할 일은 밖에서 받는다. */
class NotificationSettingsActions(
    val onBack: () -> Unit,
    val onReminder: (Boolean) -> Unit,
    val onSound: (Boolean) -> Unit,
    val onVibrate: (Boolean) -> Unit,
    val onRealert: (Boolean) -> Unit,
    val onRealertMinutes: (Int) -> Unit,
    val onSendTest: () -> Unit,
    val onOpenTimezonePolicy: () -> Unit,
    val onOpenPreview: () -> Unit
)

// 줄 앞 색 네모 (아이콘 3번: 진한 색 네모 + 흰 아이콘). 전수환 디자인 파일 MediCheck_S17_Redesign.html A안의 색
private val TileBlue = Color(0xFF4779C5)
private val TilePurple = Color(0xFF8064C1)
private val TileAmber = Color(0xFFBD762A)
private val TileSlate = Color(0xFF71858E)

/**
 * S17 알림 설정 (v3 notificationSettings()). 2026-11-25 전수환 디자인 파일 A안(설정 중심)으로 다시 그림.
 *
 * 스위치 4개와 재알림 간격은 바꾸는 즉시 저장되고 실제 알림에 적용된다.
 *   복약 시간 알림 → 끄면 알람을 걸지 않는다. 그래서 끄면 소리·진동·재알림 줄은 흐리게 하고 누를 수 없게 한다
 *   소리·진동 → 알림을 띄울 채널이 달라진다
 *   재알림 → 응답하지 않은 회차를 간격만큼 지난 뒤 한 번 더 알린다. 간격 칩은 재알림이 켜져 있을 때만 고른다
 * 칩은 꺼져 있어도 자리를 지킨다(흐리게만) — 스위치를 켜고 끌 때 아래가 움직이지 않게.
 */
@Composable
fun S17NotificationSettingsScreen(settings: UserSettings, actions: NotificationSettingsActions) {
    val main = settings.reminderEnabled
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s17)) {
        BackLink(stringResource(R.string.s17_back), actions.onBack)
        ScreenIntro(modifier = Modifier.padding(top = 16.dp, bottom = 20.dp))

        GroupLabel(stringResource(R.string.s17_section_alerts))
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 15.dp)) {
                SwitchLine(Icons.Outlined.Notifications, Teal, R.string.s17_reminder_title, main, enabled = true, onChange = actions.onReminder)
                RowDivider()
                SwitchLine(Icons.AutoMirrored.Outlined.VolumeUp, TileBlue, R.string.s17_sound_title, settings.soundEnabled, main, actions.onSound)
                RowDivider()
                SwitchLine(Icons.Outlined.Vibration, TilePurple, R.string.s17_vibrate_title, settings.vibrateEnabled, main, actions.onVibrate)
                RowDivider()
                SwitchLine(Icons.Outlined.History, TileAmber, R.string.s17_realert_title, settings.realertEnabled, main, actions.onRealert)
                IntervalChips(
                    selected = settings.realertMinutes,
                    enabled = main && settings.realertEnabled,
                    onSelect = actions.onRealertMinutes
                )
            }
        }

        GroupLabel(stringResource(R.string.s17_section_more), Modifier.padding(top = 24.dp))
        SurfaceCard(padding = 0.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = actions.onOpenTimezonePolicy)
                    .padding(horizontal = 15.dp)
                    .heightIn(min = 62.dp)
            ) {
                IconTile(Icons.Outlined.Public, TileSlate)
                Text(
                    stringResource(R.string.s17_timezone_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
                Text("›", fontSize = 20.sp, color = Muted)
            }
        }

        // 아래 단추 둘: 테스트 알림(흰) · 알림 미리보기(청록). 시안 비율 1 : 1.12
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 28.dp)) {
            BottomButton(Icons.Outlined.Notifications, stringResource(R.string.s17_test_short), primary = false, onClick = actions.onSendTest, modifier = Modifier.weight(1f))
            BottomButton(Icons.Outlined.Visibility, stringResource(R.string.s17_btn_preview), primary = true, onClick = actions.onOpenPreview, modifier = Modifier.weight(1.12f))
        }
    }
}

/** 줄 사이 선: 아이콘 뒤에서 시작한다 */
@Composable
private fun RowDivider() = HorizontalDivider(color = Line, modifier = Modifier.padding(start = 46.dp))

/** 색 네모 + 이름 + 스위치. 줄 어디를 눌러도 켜고 끈다. enabled=false면 흐리게 하고 눌리지 않는다 */
@Composable
private fun SwitchLine(icon: ImageVector, color: Color, titleRes: Int, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = 62.dp)
            .alpha(if (enabled) 1f else 0.45f)
    ) {
        IconTile(icon, color)
        Text(
            stringResource(titleRes),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Navy,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        McSwitch(checked = checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { })
    }
}

/** 재알림 간격 칩 4개 (10·15·30·60분). 꺼져 있으면 흐리게만 하고 자리는 그대로 */
@Composable
private fun IntervalChips(selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(start = 46.dp, bottom = 16.dp).alpha(if (enabled) 1f else 0.45f)) {
        Text(
            stringResource(R.string.s17_realert_interval),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Muted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            UserSettings.REALERT_OPTIONS.forEach { minutes ->
                val on = minutes == selected
                val shape = RoundedCornerShape(9.dp)
                Text(
                    text = stringResource(R.string.s17_minutes, minutes),
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (on) Teal else Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape)
                        .background(if (on) Color(0xFFE6F5F1) else Color(0xFFF6F8F8))
                        .border(1.dp, if (on) Teal else Color(0xFFE0E7E7), shape)
                        .selectable(selected = on, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(minutes) })
                        .padding(vertical = 9.dp)
                )
            }
        }
    }
}

/** 아래 단추: 아이콘 + 글자. primary=true면 청록 바탕, false면 흰 바탕 + 테두리 */
@Composable
private fun BottomButton(icon: ImageVector, text: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(13.dp)
    val fg = if (primary) Color.White else Teal
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(51.dp)
            .clip(shape)
            .background(if (primary) Teal else Color.White)
            .then(if (primary) Modifier else Modifier.border(1.dp, Color(0xFFDBE5E3), shape))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.padding(start = 7.dp))
    }
}
