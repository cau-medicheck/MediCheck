package com.cau.medicheck.ui.screens.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.theme.RowSubText
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.components.RowsCard
import com.cau.medicheck.ui.components.HeroHeader
import com.cau.medicheck.ui.components.GroupLabel
import com.cau.medicheck.ui.components.ActionPair
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.GlyphText
import com.cau.medicheck.ui.components.ListRow
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.RowTitle
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.DateChipText
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/**
 * S34 알림 거절/오류 안내 (v3 deniedNotice()).
 *
 * 폰에서 알림이 꺼져 있을 때 무엇이 안 되는지와 어디서 켜는지를 알려 준다.
 * 실제 상태에 따라 "꺼져 있어요"(권한 허용 버튼 있음) / "켜져 있어요"(버튼 없음) 두 모습으로 그린다.
 *
 * @param exactAlarmNeedsCheck 정확한 알람이 제한돼 있어 2번 줄을 누를 수 있는가
 */
@Composable
fun S34NotificationDeniedScreen(
    notificationsOn: Boolean,
    exactAlarmNeedsCheck: Boolean,
    actions: PermissionActions
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s34)) {
        BackLink(stringResource(R.string.common_back_previous), actions.onBack)
        ScreenIntro(modifier = Modifier.padding(vertical = 16.dp))

        // 2026-11-25 다시 그림: 가운데 큰 종 아이콘(켜짐 청록 / 꺼짐 주황) + 제목 + 한 줄 + 단추
        HeroHeader(
            icon = if (notificationsOn) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
            color = if (notificationsOn) Teal else Color(0xFFE08A2E),
            title = stringResource(if (notificationsOn) R.string.s34_on_title else R.string.s34_off_title),
            subtitle = stringResource(if (notificationsOn) R.string.s34_on_body else R.string.s34_off_body)
        )
        if (!notificationsOn) {
            ActionPair(
                secondaryText = stringResource(R.string.s34_btn_schedule),
                onSecondary = actions.onOpenOther,
                primaryText = stringResource(R.string.s34_allow),
                onPrimary = actions.onRequestNotification
            )
        } else {
            McButton(stringResource(R.string.s34_btn_schedule), actions.onOpenOther, kind = ButtonKind.LIGHT, full = true)
        }

        GroupLabel(stringResource(R.string.s34_section), Modifier.padding(top = 26.dp))
        RowsCard {
            StepRow(1, R.string.s34_step1, actions.onRequestNotification)
            StepRow(2, R.string.s34_step2, if (exactAlarmNeedsCheck) actions.onOpenExactAlarm else null)
            StepRow(3, R.string.s34_step3, actions.onOpenAppDetails, showDivider = false)
        }
    }
}

/** 번호(청록 동그라미) + 할 일 한 줄. 누를 수 있는 줄에만 오른쪽에 "›"를 그린다. */
@Composable
private fun StepRow(number: Int, textRes: Int, onClick: (() -> Unit)?, showDivider: Boolean = true) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .heightIn(min = 56.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp).clip(CircleShape).background(Teal).clearAndSetSemantics { }) {
                Text(number.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text(stringResource(textRes), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Navy, modifier = Modifier.weight(1f).padding(start = 12.dp))
            if (onClick != null) Text("›", fontSize = 20.sp, color = RowSubText, modifier = Modifier.clearAndSetSemantics { })
        }
        if (showDivider) HorizontalDivider(color = Line, modifier = Modifier.padding(start = 38.dp))
    }
}
