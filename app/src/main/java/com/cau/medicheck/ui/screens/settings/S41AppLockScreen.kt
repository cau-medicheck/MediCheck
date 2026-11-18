package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.AppLockMethod
import com.cau.medicheck.domain.UserSettings
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.McSelectField
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.ToggleRow
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.SettingDescText

/**
 * S41 앱 잠금 설정 (v3 appLock()).
 *
 * 스위치를 켜면, 이 계정으로 앱을 다시 열 때마다(다른 앱에 갔다 오거나 새로 켤 때) 잠금 화면이 뜨고
 * 기기의 본인 확인(생체인증·PIN)을 통과해야 풀린다. 프로토타입은 값만 저장하는 시안이었지만 앱은 실제로 잠근다.
 *
 * 이 화면은 그리기만 한다. 켤 때의 본인 확인 창은 SettingsGraph가 띄운다(Activity가 필요해서).
 *
 * @param biometricChoiceSupported 이 기기에서 '기기 생체인증'을 실제로 쓸 수 있는지(Android 11 이상).
 *                                 아니면 어느 방식을 골라도 PIN으로 확인한다는 안내를 보여 준다
 * @param onToggle 스위치를 눌렀을 때. 켜려는 것이면 true
 */
@Composable
fun S41AppLockScreen(
    settings: UserSettings,
    biometricChoiceSupported: Boolean,
    onBack: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onMethod: (AppLockMethod) -> Unit
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s41)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenIntro(stringResource(R.string.s41_subtitle), Modifier.padding(vertical = Gap16))

        SurfaceCard {
            ToggleRow(
                title = stringResource(R.string.s41_title),
                description = stringResource(R.string.s41_switch_desc),
                checked = settings.appLockEnabled,
                onCheckedChange = onToggle
            )

            // 잠금 방식은 앱 잠금이 꺼져 있어도 고를 수 있고 저장된다.
            FieldLabel(stringResource(R.string.s41_method_label))
            McSelectField(
                selected = settings.appLockMethod,
                options = AppLockMethod.entries,
                label = { method ->
                    stringResource(
                        when (method) {
                            AppLockMethod.BIOMETRIC -> R.string.s41_method_biometric
                            AppLockMethod.DEVICE_CREDENTIAL -> R.string.s41_method_pin
                        }
                    )
                },
                onSelect = onMethod
            )
            if (!biometricChoiceSupported && settings.appLockMethod == AppLockMethod.BIOMETRIC) {
                Text(
                    text = stringResource(R.string.s41_method_fallback),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = SettingDescText,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Notice(stringResource(R.string.s41_notice), Modifier.padding(top = 14.dp))
    }
}
