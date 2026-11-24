package com.cau.medicheck.ui.screens.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.LinkText
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SettingRow
import com.cau.medicheck.ui.components.SurfaceCard

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
@Composable
fun S18PermissionsScreen(
    notificationsOn: Boolean,
    exactAlarmsAllowed: Boolean,
    backgroundRestricted: Boolean,
    actions: PermissionActions
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s18)) {
        BackLink(stringResource(R.string.s18_back), actions.onBack)
        ScreenIntro(modifier = Modifier.padding(vertical = 16.dp))

        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SettingRow(
                    title = stringResource(R.string.s18_row1_title),
                    description = stringResource(R.string.s18_row1_desc),
                    onClick = actions.onRequestNotification
                ) { StateChip(notificationsOn, R.string.s18_chip_allowed, R.string.s18_chip_off) }
                SettingRow(
                    title = stringResource(R.string.s18_row2_title),
                    description = stringResource(R.string.s18_row2_desc),
                    // 제한됐을 때만 설정 화면을 연다 (허용된 폰에서는 그 화면에 이 앱이 없을 수 있다).
                    onClick = if (exactAlarmsAllowed) null else actions.onOpenExactAlarm
                ) { StateChip(exactAlarmsAllowed, R.string.s18_chip_allowed, R.string.s18_chip_restricted) }
                SettingRow(
                    title = stringResource(R.string.s18_row3_title),
                    description = stringResource(R.string.s18_row3_desc),
                    onClick = actions.onOpenAppDetails
                ) { StateChip(!backgroundRestricted, R.string.s18_chip_unrestricted, R.string.s18_chip_restricted) }
                SettingRow(
                    title = stringResource(R.string.s18_row4_title),
                    description = stringResource(R.string.s18_row4_desc)
                ) { Pill(stringResource(R.string.s18_chip_applied), PillKind.MINT) }
                SettingRow(
                    title = stringResource(R.string.s18_row5_title),
                    description = stringResource(R.string.s18_row5_desc),
                    showDivider = false
                ) { Pill(stringResource(R.string.s18_chip_applied), PillKind.MINT) }
            }
        }

        McButton(
            text = stringResource(if (notificationsOn) R.string.s18_button_open_settings else R.string.s18_button_allow),
            onClick = actions.onRequestNotification,
            kind = ButtonKind.LIGHT,
            full = true,
            modifier = Modifier.padding(top = 16.dp)
        )
        LinkText(
            text = stringResource(R.string.s18_link_denied),
            onClick = actions.onOpenOther,
            modifier = Modifier.padding(top = 8.dp)
        )
        Notice(stringResource(R.string.s18_notice), Modifier.padding(top = 16.dp), warn = true)
    }
}

/** 상태 배지: 좋으면 민트, 문제가 있으면 주황 */
@Composable
private fun StateChip(good: Boolean, goodRes: Int, badRes: Int) {
    Pill(stringResource(if (good) goodRes else badRes), if (good) PillKind.MINT else PillKind.ORANGE)
}
