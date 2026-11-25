package com.cau.medicheck.ui.screens.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.GroupLabel
import com.cau.medicheck.ui.components.IconRow
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.RowsCard
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.theme.HeroDeepEnd
import com.cau.medicheck.ui.theme.HeroDeepStart
import com.cau.medicheck.ui.theme.Teal

/**
 * 권한 안내 화면(S18·S34)에서 누를 수 있는 것들.
 * @param onRequestNotification 알림 권한 요청 순서를 실행한다 (권한 창 또는 설정 앱)
 * @param onOpenExactAlarm 설정 앱의 '알람 및 리마인더' 화면
 * @param onOpenAppDetails 설정 앱의 앱 정보 화면 (배터리 제한)
 */
class PermissionActions(
    val onBack: () -> Unit,
    val onRequestNotification: () -> Unit,
    val onOpenExactAlarm: () -> Unit,
    val onOpenAppDetails: () -> Unit,
    /** S18: "알림 권한 거절 시 안내 ›" → S34 / S34: "앱에서 일정 확인" → S05 */
    val onOpenOther: () -> Unit
)

/**
 * S18 권한·OS 안내 (v3 permissions()).
 *
 * 알림이 제때 오려면 필요한 조건 5가지의 실제 상태를 배지로 보여 준다.
 * 줄 1~3은 누르면 안드로이드 설정의 해당 화면이 열린다. 줄 4·5는 앱이 이미 구현한 동작이라 항상 "적용됨"이다.
 *
 * @param notificationsOn 복약 알림이 화면에 뜰 수 있는가
 * @param exactAlarmsAllowed 정확한 시각에 알람을 걸 수 있는가
 * @param backgroundRestricted 배터리 사용이 '제한'인가
 */

private val TileBlue = Color(0xFF4779C5)
private val TilePurple = Color(0xFF8064C1)
private val TileSlate = Color(0xFF71858E)
private val WarnOrange = Color(0xFFE08A2E)

/**
 * S18 알림 권한 (2026-11-25 다시 그림): 맨 위 상태 카드(다 괜찮으면 청록 "알림이 잘 울려요", 아니면 주황) +
 * "필요한 권한" 세 줄(아이콘 + 상태) + "자동으로 챙겨요" 두 줄 + 단추 + "알림이 안 올 때" 줄.
 */
@Composable
fun S18PermissionsScreen(
    notificationsOn: Boolean,
    exactAlarmsAllowed: Boolean,
    backgroundRestricted: Boolean,
    actions: PermissionActions
) {
    val allGood = notificationsOn && exactAlarmsAllowed && !backgroundRestricted
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s18)) {
        BackLink(stringResource(R.string.s18_back), actions.onBack)
        ScreenIntro(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        StatusBanner(allGood)

        GroupLabel(stringResource(R.string.s18_section_needed), Modifier.padding(top = 22.dp))
        RowsCard {
            IconRow(Icons.Rounded.Notifications, Teal, stringResource(R.string.s18_row1_title), onClick = actions.onRequestNotification,
                trailing = { StateChip(notificationsOn, R.string.s18_chip_allowed, R.string.s18_chip_off) })
            // 정확한 시간: 제한됐을 때만 설정 화면을 연다 (허용된 폰에서는 그 화면에 이 앱이 없을 수 있다).
            IconRow(Icons.Rounded.Alarm, TileBlue, stringResource(R.string.s18_row2_title), onClick = if (exactAlarmsAllowed) null else actions.onOpenExactAlarm,
                trailing = { StateChip(exactAlarmsAllowed, R.string.s18_chip_allowed, R.string.s18_chip_restricted) })
            IconRow(Icons.Rounded.BatteryChargingFull, TilePurple, stringResource(R.string.s18_row3_title), onClick = actions.onOpenAppDetails, showDivider = false,
                trailing = { StateChip(!backgroundRestricted, R.string.s18_chip_unrestricted, R.string.s18_chip_restricted) })
        }

        GroupLabel(stringResource(R.string.s18_section_auto), Modifier.padding(top = 22.dp))
        RowsCard {
            IconRow(Icons.Rounded.RestartAlt, TileSlate, stringResource(R.string.s18_row4_title), trailing = { DoneMark() })
            IconRow(Icons.Rounded.Sync, TileSlate, stringResource(R.string.s18_row5_title), showDivider = false, trailing = { DoneMark() })
        }

        McButton(
            text = stringResource(if (notificationsOn) R.string.s18_button_open_settings else R.string.s18_button_allow),
            onClick = actions.onRequestNotification,
            kind = if (notificationsOn) ButtonKind.LIGHT else ButtonKind.PRIMARY,
            full = true,
            modifier = Modifier.padding(top = 22.dp)
        )
        RowsCard(Modifier.padding(top = 12.dp)) {
            IconRow(Icons.Rounded.HelpOutline, WarnOrange, stringResource(R.string.s18_link_denied), onClick = actions.onOpenOther, showDivider = false)
        }
    }
}

/** 맨 위 상태 카드 */
@Composable
private fun StatusBanner(allGood: Boolean) {
    val bg = if (allGood) Brush.linearGradient(listOf(HeroDeepStart, HeroDeepEnd)) else Brush.linearGradient(listOf(Color(0xFFE08A2E), Color(0xFFEFA24B)))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).padding(16.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.2f))) {
            Icon(if (allGood) Icons.Rounded.Check else Icons.Rounded.WarningAmber, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.padding(start = 13.dp)) {
            Text(stringResource(if (allGood) R.string.s18_banner_ok else R.string.s18_banner_bad), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(stringResource(if (allGood) R.string.s18_banner_ok_sub else R.string.s18_banner_bad_sub), fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.88f), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun StateChip(good: Boolean, goodRes: Int, badRes: Int) {
    Pill(stringResource(if (good) goodRes else badRes), if (good) PillKind.MINT else PillKind.ORANGE)
}

/** 자동으로 되는 일 옆의 초록 체크 (장식) */
@Composable
private fun DoneMark() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFFE4F4F2)).clearAndSetSemantics { }) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = Teal, modifier = Modifier.size(15.dp))
    }
}
