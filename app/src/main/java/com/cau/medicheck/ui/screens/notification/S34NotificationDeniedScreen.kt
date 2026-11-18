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
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.GlyphText
import com.cau.medicheck.ui.components.ListRow
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
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
        ScreenIntro(stringResource(R.string.s34_subtitle), Modifier.padding(vertical = 16.dp))

        SurfaceCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // 그림 글자는 장식이라 TalkBack이 읽지 않게 한다.
                GlyphText(
                    symbol = if (notificationsOn) Glyph.BELL else Glyph.BELL_OFF,
                    fontSize = 40,
                    modifier = Modifier.clearAndSetSemantics { }
                )
                Text(
                    text = stringResource(if (notificationsOn) R.string.s34_on_title else R.string.s34_off_title),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    modifier = Modifier.padding(top = 12.dp, bottom = 9.dp)
                )
                Text(
                    text = stringResource(if (notificationsOn) R.string.s34_on_body else R.string.s34_off_body),
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
                if (!notificationsOn) {
                    McButton(
                        text = stringResource(R.string.s34_allow),
                        onClick = actions.onRequestNotification,
                        full = true,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                McButton(
                    text = stringResource(R.string.s34_btn_schedule),
                    onClick = actions.onOpenOther,
                    kind = ButtonKind.WHITE,
                    full = true,
                    modifier = Modifier.padding(top = if (notificationsOn) 16.dp else 8.dp)
                )
            }
        }

        SectionTitle(stringResource(R.string.s34_section))
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                StepRow("1", R.string.s34_step1, actions.onRequestNotification)
                StepRow("2", R.string.s34_step2, if (exactAlarmNeedsCheck) actions.onOpenExactAlarm else null)
                StepRow("3", R.string.s34_step3, actions.onOpenAppDetails, showDivider = false)
            }
        }

        Notice(stringResource(R.string.s34_notice), Modifier.padding(top = 14.dp))
    }
}

/** 번호 + 할 일 한 줄. 누를 수 있는 줄에만 오른쪽에 "›"를 그린다. */
@Composable
private fun StepRow(number: String, textRes: Int, onClick: (() -> Unit)?, showDivider: Boolean = true) {
    ListRow(
        leading = { Avatar(number, Modifier.clearAndSetSemantics { }) },
        trailing = if (onClick != null) {
            { Text("›", fontSize = 16.sp, color = DateChipText, modifier = Modifier.clearAndSetSemantics { }) }
        } else null,
        showDivider = showDivider,
        onClick = onClick
    ) {
        RowTitle(stringResource(textRes))
    }
}
