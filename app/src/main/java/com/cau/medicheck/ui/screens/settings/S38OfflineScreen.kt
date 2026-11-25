package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.components.StatusBanner
import com.cau.medicheck.ui.components.RowsCard
import com.cau.medicheck.ui.components.IconRow
import com.cau.medicheck.ui.components.GroupLabel
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.Icons
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.OptionRow
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/** 이 화면에서 누를 수 있는 것들 */
data class OfflineActions(
    val onBack: () -> Unit,
    val onOpenMeds: () -> Unit,
    val onOpenSchedule: () -> Unit,
    val onOpenRecords: () -> Unit,
    val onOpenVerify: () -> Unit
)

/**
 * S38 오프라인 상태 (v3 offlineScreen()).
 *
 * 프로토타입은 "네트워크 상태 시뮬레이션" 스위치로 문구만 바꿨다. 앱은 스위치 없이 폰의 실제 연결 상태를 보여 준다.
 * 이 앱은 인터넷을 쓰지 않아서, 연결돼 있든 아니든 "인터넷 없이도 모든 기능이 동작한다"고 안내한다.
 *
 * @param online 폰이 지금 인터넷에 연결돼 있는지 (NetworkMonitor가 알려 준 값)
 */
@Composable
fun S38OfflineScreen(online: Boolean, actions: OfflineActions) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s38)) {
        BackLink(stringResource(R.string.common_back_previous), actions.onBack)
        ScreenIntro(modifier = Modifier.padding(vertical = Gap16))

        // 2026-11-25 다시 그림: 상태 띠(연결됨 청록 / 오프라인 주황) + "인터넷 없이도 돼요" 아이콘 줄 넷
        StatusBanner(
            good = online,
            icon = if (online) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
            title = stringResource(if (online) R.string.s38_pill_online else R.string.s38_pill_offline),
            sub = stringResource(R.string.s38_banner_sub)
        )

        GroupLabel(stringResource(R.string.s38_section_works), Modifier.padding(top = 22.dp))
        RowsCard {
            IconRow(Icons.Rounded.Medication, Teal, stringResource(R.string.s38_row_meds_title), onClick = actions.onOpenMeds)
            IconRow(Icons.Rounded.CalendarMonth, Color(0xFF4779C5), stringResource(R.string.s38_row_schedule_title), onClick = actions.onOpenSchedule)
            IconRow(Icons.Rounded.History, Color(0xFF8064C1), stringResource(R.string.s38_row_records_title), onClick = actions.onOpenRecords)
            IconRow(Icons.Rounded.FactCheck, Color(0xFF71858E), stringResource(R.string.s38_row_verify_title), onClick = actions.onOpenVerify, showDivider = false)
        }
    }
}
