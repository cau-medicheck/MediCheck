package com.cau.medicheck.ui.screens.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.TimeZoneMode
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.RadioRow
import com.cau.medicheck.ui.components.ScreenTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.SettingDivider

/**
 * S35 시간대 변경 정책 (v3 timezonePolicy()).
 *
 * 해외로 이동해 폰의 시간대가 바뀌었을 때 복약 시각("08:00")을 어느 시간대의 08:00으로 볼지 고른다.
 *   기기 현지 시간 기준 → 도착한 곳의 08:00에 울린다 (기본)
 *   등록한 시간대 기준 → 가입할 때의 시간대의 08:00에 울리고, 앱의 "오늘"도 그 시간대로 계산한다
 * 라디오를 고르기만 해서는 저장되지 않고 "정책 저장"을 눌러야 저장된다.
 */
@Composable
fun S35TimezonePolicyScreen(
    state: TimezonePolicyUiState,
    onBack: () -> Unit,
    onSelect: (TimeZoneMode) -> Unit,
    onSave: () -> Unit
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s35)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenTitle(
            title = stringResource(R.string.s35_title),
            subtitle = stringResource(R.string.s35_subtitle),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        SurfaceCard {
            Text(stringResource(R.string.s35_card_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(
                text = stringResource(R.string.s35_card_desc),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Muted,
                modifier = Modifier.padding(top = 7.dp, bottom = 6.dp)
            )
            // selectableGroup: TalkBack에게 "이 둘은 하나만 고르는 묶음"이라고 알려 준다.
            Column(Modifier.selectableGroup()) {
                RadioRow(
                    title = stringResource(R.string.s35_local_title),
                    description = stringResource(R.string.s35_local_desc),
                    selected = state.selected == TimeZoneMode.LOCAL,
                    onSelect = { onSelect(TimeZoneMode.LOCAL) }
                )
                HorizontalDivider(thickness = 1.dp, color = SettingDivider)
                RadioRow(
                    title = stringResource(R.string.s35_home_title),
                    description = stringResource(R.string.s35_home_desc),
                    selected = state.selected == TimeZoneMode.HOME,
                    onSelect = { onSelect(TimeZoneMode.HOME) },
                    extra = stringResource(R.string.s35_home_zone, state.homeZoneId, state.homeZoneOffset)
                )
            }
            McButton(
                text = stringResource(R.string.s35_save),
                onClick = onSave,
                full = true,
                loading = state.isSaving,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Notice(stringResource(R.string.s35_notice_warn), Modifier.padding(top = 16.dp), warn = true)
        Notice(stringResource(R.string.s35_notice), Modifier.padding(top = 14.dp))
    }
}
