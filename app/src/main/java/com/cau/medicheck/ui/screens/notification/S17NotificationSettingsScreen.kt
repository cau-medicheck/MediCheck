package com.cau.medicheck.ui.screens.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.UserSettings
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.McSelectField
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.OptionRow
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.ToggleRow
import com.cau.medicheck.ui.theme.SettingDivider

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

/**
 * S17 알림 설정 (v3 notificationSettings()).
 *
 * 스위치 4개와 재알림 간격은 바꾸는 즉시 저장되고 실제 알림에 적용된다.
 *   복약 시간 알림 → 끄면 알람을 걸지 않는다 / 소리·진동 → 알림을 띄울 채널이 달라진다
 *   재알림 → 응답하지 않은 회차를 간격만큼 지난 뒤 한 번 더 알린다
 */
@Composable
fun S17NotificationSettingsScreen(settings: UserSettings, actions: NotificationSettingsActions) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s17)) {
        BackLink(stringResource(R.string.s17_back), actions.onBack)
        ScreenIntro(stringResource(R.string.s17_subtitle), Modifier.padding(vertical = 16.dp))

        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SwitchLine(R.string.s17_reminder_title, R.string.s17_reminder_desc, settings.reminderEnabled, actions.onReminder)
                SwitchLine(R.string.s17_sound_title, R.string.s17_sound_desc, settings.soundEnabled, actions.onSound)
                SwitchLine(R.string.s17_vibrate_title, R.string.s17_vibrate_desc, settings.vibrateEnabled, actions.onVibrate)
                SwitchLine(R.string.s17_realert_title, R.string.s17_realert_desc, settings.realertEnabled, actions.onRealert)

                // 재알림 간격: '재알림'이 꺼져 있어도 고를 수 있고 저장된다 (v3와 같음).
                FieldLabel(stringResource(R.string.s17_realert_interval), Modifier.padding(top = 13.dp))
                McSelectField(
                    selected = settings.realertMinutes,
                    options = UserSettings.REALERT_OPTIONS,
                    label = { minutes -> stringResource(R.string.s17_minutes, minutes) },
                    onSelect = actions.onRealertMinutes,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        McButton(
            text = stringResource(R.string.s17_test_button),
            onClick = actions.onSendTest,
            kind = ButtonKind.WHITE,
            full = true,
            modifier = Modifier.padding(top = 16.dp)
        )

        SurfaceCard(modifier = Modifier.padding(top = 16.dp), padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                OptionRow(
                    title = stringResource(R.string.s17_timezone_title),
                    description = stringResource(R.string.s17_timezone_desc),
                    onClick = actions.onOpenTimezonePolicy,
                    showDivider = false
                )
            }
        }

        Notice(stringResource(R.string.s17_notice), Modifier.padding(top = 16.dp))
        McButton(
            text = stringResource(R.string.s17_btn_preview),
            onClick = actions.onOpenPreview,
            kind = ButtonKind.LIGHT,
            full = true,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/** 스위치 줄 하나 + 아래 얇은 선. 줄 어디를 눌러도 켜고 끈다(48dp 이상의 터치 영역). */
@Composable
private fun SwitchLine(titleRes: Int, descRes: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    ToggleRow(
        title = stringResource(titleRes),
        description = stringResource(descRes),
        checked = checked,
        onCheckedChange = onChange,
        verticalPadding = 13.dp
    )
    HorizontalDivider(thickness = 1.dp, color = SettingDivider)
}
