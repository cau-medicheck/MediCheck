package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.OptionRow
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
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
        Spacer(Modifier.height(16.dp))

        // ----- 지금 연결 상태 -----
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.s38_status_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text(
                        text = stringResource(if (online) R.string.s38_status_online else R.string.s38_status_offline),
                        fontSize = 12.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                // 색만으로 구분하지 않도록 배지에 글자("연결됨"/"오프라인")를 함께 쓴다.
                Pill(
                    text = stringResource(if (online) R.string.s38_pill_online else R.string.s38_pill_offline),
                    kind = if (online) PillKind.MINT else PillKind.ORANGE
                )
            }
            Notice(
                text = stringResource(if (online) R.string.s38_notice_online else R.string.s38_notice_offline),
                modifier = Modifier.padding(top = Gap16),
                warn = !online
            )
        }

        // ----- 인터넷 없이 쓸 수 있는 화면들 -----
        SurfaceCard(Modifier.padding(top = Gap16), padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                OptionRow(
                    title = stringResource(R.string.s38_row_meds_title),
                    description = stringResource(R.string.s38_row_meds_desc),
                    onClick = actions.onOpenMeds
                )
                OptionRow(
                    title = stringResource(R.string.s38_row_schedule_title),
                    description = stringResource(R.string.s38_row_schedule_desc),
                    onClick = actions.onOpenSchedule
                )
                OptionRow(
                    title = stringResource(R.string.s38_row_records_title),
                    description = stringResource(R.string.s38_row_records_desc),
                    onClick = actions.onOpenRecords
                )
                OptionRow(
                    title = stringResource(R.string.s38_row_verify_title),
                    description = stringResource(R.string.s38_row_verify_desc),
                    onClick = actions.onOpenVerify,
                    showDivider = false
                )
            }
        }
    }
}
