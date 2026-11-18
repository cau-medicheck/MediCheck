package com.cau.medicheck.ui.screens.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.ListRow
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.RowChevron
import com.cau.medicheck.ui.components.RowSub
import com.cau.medicheck.ui.components.RowTitle
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.components.timeText

/**
 * S16 알림 센터 (v3 notifications()).
 *
 * 앞으로 실제로 울릴 휴대폰 알림을 미리 보여 준다: 오늘 남은 회차 + 내일 회차, 최대 8줄.
 * 이미 복용 완료·건너뜀으로 기록한 회차와 시각이 지난 회차는 목록에 없다.
 *
 * @param notificationsOn 폰에서 복약 알림이 켜져 있는가 (꺼져 있으면 "울리지 않는다"고 알린다)
 */
@Composable
fun S16NotificationCenterScreen(
    state: NotificationCenterUiState,
    notificationsOn: Boolean,
    onOpenDose: (DoseKey) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s16)) {

        // 안내 상자: 위에서부터 처음 맞는 하나
        when {
            !state.reminderEnabled -> Notice(stringResource(R.string.s16_notice_reminder_off), warn = true)
            !notificationsOn -> Notice(stringResource(R.string.s16_notice_permission_off), warn = true)
            state.hideMedName ->
                Notice(stringResource(R.string.s16_notice_ok) + " " + stringResource(R.string.s16_notice_name_hidden))
            else -> Notice(stringResource(R.string.s16_notice_ok))
        }

        SectionTitle(stringResource(R.string.s16_section))
        SurfaceCard(padding = 0.dp) {
            when {
                state.isLoading -> Unit   // 읽는 동안에는 "없음"을 잠깐이라도 보이지 않는다
                state.items.isEmpty() -> EmptyState(
                    emoji = Glyph.BELL,
                    title = stringResource(R.string.s16_empty_title),
                    body = stringResource(
                        if (state.hasActiveMedication) R.string.s16_empty_body_has_meds else R.string.s16_empty_body
                    )
                )
                else -> Column(Modifier.padding(horizontal = 16.dp)) {
                    state.items.forEachIndexed { index, dose ->
                        val date = fullDateText(dose.date)
                        val time = timeText(dose.time)
                        ListRow(
                            // "♧"는 장식이라 TalkBack이 읽지 않게 한다.
                            leading = { Avatar(Glyph.HEADER_BELL, Modifier.clearAndSetSemantics { }) },
                            trailing = { RowChevron() },
                            showDivider = index != state.items.lastIndex,
                            onClick = { onOpenDose(dose.key) },
                            onClickLabel = stringResource(R.string.common_cd_open_detail)
                        ) {
                            RowTitle(stringResource(R.string.s16_row_title, dose.medicationName))
                            RowSub(stringResource(R.string.s16_row_sub, date, time, dose.dosage))
                        }
                    }
                }
            }
        }

        McButton(
            text = stringResource(R.string.s16_btn_settings),
            onClick = onOpenSettings,
            kind = ButtonKind.LIGHT,
            full = true,
            modifier = Modifier.padding(top = 16.dp)
        )
        McButton(
            text = stringResource(R.string.s16_btn_permissions),
            onClick = onOpenPermissions,
            kind = ButtonKind.WHITE,
            full = true,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
