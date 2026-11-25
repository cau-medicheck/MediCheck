package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.components.RowsCard
import com.cau.medicheck.ui.components.IconTile
import com.cau.medicheck.ui.components.IconSwitchRow
import com.cau.medicheck.ui.components.GroupLabel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
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
        ScreenIntro(modifier = Modifier.padding(vertical = Gap16))

        // 2026-11-25 다시 그림: 스위치 한 줄(아이콘) + "잠금 방식" 고르는 두 줄(고른 줄에 체크)
        RowsCard {
            IconSwitchRow(Icons.Rounded.Lock, Color(0xFF8064C1), stringResource(R.string.s41_title), settings.appLockEnabled, onToggle, showDivider = false)
        }

        // 잠금 방식은 앱 잠금이 꺼져 있어도 고를 수 있고 저장된다.
        GroupLabel(stringResource(R.string.s41_method_label), Modifier.padding(top = 22.dp))
        RowsCard(Modifier.selectableGroup()) {
            AppLockMethod.entries.forEachIndexed { index, method ->
                val chosen = settings.appLockMethod == method
                MethodRow(
                    icon = if (method == AppLockMethod.BIOMETRIC) Icons.Rounded.Fingerprint else Icons.Rounded.Pin,
                    title = stringResource(if (method == AppLockMethod.BIOMETRIC) R.string.s41_method_biometric else R.string.s41_method_pin),
                    chosen = chosen,
                    onSelect = { onMethod(method) },
                    showDivider = index != AppLockMethod.entries.lastIndex
                )
            }
        }
        if (!biometricChoiceSupported && settings.appLockMethod == AppLockMethod.BIOMETRIC) {
            Text(
                text = stringResource(R.string.s41_method_fallback),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = SettingDescText,
                modifier = Modifier.padding(start = 3.dp, top = 8.dp)
            )
        }

        Notice(stringResource(R.string.s41_notice), Modifier.padding(top = 14.dp))
    }
}

/** 잠금 방식 한 줄: [색 네모] 이름 ··· [고른 줄이면 청록 체크] */
@Composable
private fun MethodRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, chosen: Boolean, onSelect: () -> Unit, showDivider: Boolean) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = chosen, role = Role.RadioButton, onClick = onSelect)
                .heightIn(min = 58.dp)
        ) {
            IconTile(icon, if (chosen) Teal else Color(0xFF8A9AA1), size = 32.dp, iconSize = 19.dp)
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Navy, modifier = Modifier.weight(1f).padding(start = 12.dp))
            if (chosen) androidx.compose.material3.Icon(Icons.Rounded.Check, contentDescription = null, tint = Teal, modifier = Modifier.size(22.dp))
        }
        if (showDivider) HorizontalDivider(color = Line, modifier = Modifier.padding(start = 44.dp))
    }
}
