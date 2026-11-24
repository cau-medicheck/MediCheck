package com.cau.medicheck.ui.screens.settings

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
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.OptionRow
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.ToggleRow
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.SettingDivider

/** 이 화면에서 누를 수 있는 것들을 한 묶음으로 받는다 (인자가 길어지지 않게) */
data class PrivacyActions(
    val onBack: () -> Unit,
    val onHideMedName: (Boolean) -> Unit,
    val onHideStatsPreview: (Boolean) -> Unit,
    val onOpen: (String) -> Unit
)

/**
 * S36 개인정보 센터 (v3 privacyCenter()).
 *
 * 위쪽 스위치 두 개는 "복약 내역이 남에게 보이는 범위"를 정한다.
 *   잠금 화면에 약 이름 숨김 → 휴대폰 알림에 약 이름 대신 일반 문구를 쓴다
 *   통계 미리보기 숨김       → 최근 앱 목록에 앱 화면 미리보기가 보이지 않게 한다
 * 아래쪽 줄 네 개는 내 데이터를 다루는 화면들(앱 잠금·백업·내보내기·삭제)의 입구다.
 */
@Composable
fun S36PrivacyScreen(settings: UserSettings, actions: PrivacyActions) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s36)) {
        BackLink(stringResource(R.string.common_back_previous), actions.onBack)
        ScreenIntro(stringResource(R.string.s36_subtitle), Modifier.padding(vertical = Gap16))

        // ----- 스위치 두 개 -----
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                ToggleRow(
                    title = stringResource(R.string.s36_hide_name_title),
                    description = stringResource(R.string.s36_hide_name_desc),
                    checked = settings.hideMedName,
                    onCheckedChange = actions.onHideMedName,
                    verticalPadding = 13.dp
                )
                HorizontalDivider(thickness = 1.dp, color = SettingDivider)
                ToggleRow(
                    title = stringResource(R.string.s36_hide_stats_title),
                    description = stringResource(R.string.s36_hide_stats_desc),
                    checked = settings.hideStatsPreview,
                    onCheckedChange = actions.onHideStatsPreview,
                    verticalPadding = 13.dp
                )
            }
        }

        // ----- 내 데이터 관리 -----
        SectionTitle(stringResource(R.string.s36_section_data))
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                OptionRow(
                    title = stringResource(R.string.s36_row_lock_title),
                    description = stringResource(R.string.s36_row_lock_desc),
                    onClick = { actions.onOpen(Routes.S41) }
                )
                OptionRow(
                    title = stringResource(R.string.s36_row_backup_title),
                    description = stringResource(R.string.s36_row_backup_desc),
                    onClick = { actions.onOpen(Routes.S28) }
                )
                OptionRow(
                    title = stringResource(R.string.s36_row_export_title),
                    description = stringResource(R.string.s36_row_export_desc),
                    onClick = { actions.onOpen(Routes.S43) }
                )
                OptionRow(
                    title = stringResource(R.string.s36_row_delete_title),
                    description = stringResource(R.string.s36_row_delete_desc),
                    onClick = { actions.onOpen(Routes.S44) },
                    showDivider = false
                )
            }
        }

        Notice(stringResource(R.string.s36_notice), Modifier.padding(top = 14.dp))
    }
}
