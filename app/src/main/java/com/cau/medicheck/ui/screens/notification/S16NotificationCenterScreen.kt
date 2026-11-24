package com.cau.medicheck.ui.screens.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.ReminderDose
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.HeroDeepEnd
import com.cau.medicheck.ui.theme.HeroDeepStart
import com.cau.medicheck.ui.theme.HomeChipMissedBg
import com.cau.medicheck.ui.theme.HomeChipMissedText
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ProgressTrack
import java.time.LocalDate

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
        ScreenIntro(modifier = Modifier.padding(bottom = 14.dp))
        if (state.isLoading) return@McScreenColumn

        // ----- 맨 위 카드: 알림이 울리지 않는 이유가 있으면 주황 경고, 아니면 청록 "다음 알림" -----
        when {
            !state.reminderEnabled -> WarnCard(stringResource(R.string.s16_notice_reminder_off), onOpenSettings)
            !notificationsOn -> WarnCard(stringResource(R.string.s16_notice_permission_off), onOpenPermissions)
            else -> NextReminderCard(state.items)
        }

        if (state.items.isEmpty()) {
            SurfaceCard(Modifier.padding(top = 12.dp), padding = 0.dp) {
                EmptyState(
                    emoji = Glyph.BELL,
                    title = stringResource(R.string.s16_empty_title),
                    body = stringResource(if (state.hasActiveMedication) R.string.s16_empty_body_has_meds else R.string.s16_empty_body)
                )
            }
        } else {
            // 날짜별 → 같은 시각끼리 한 줄: "08:00  [Lutein] [Probiotics] [Vitamin D]  ›"
            state.items.groupBy { it.date }.forEach { (date, doses) ->
                Text(
                    text = fullDateText(date),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Navy,
                    modifier = Modifier.padding(start = 2.dp, top = 18.dp, bottom = 8.dp).semantics { heading() }
                )
                SurfaceCard(padding = 0.dp) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        val slots = doses.groupBy { it.time }.toList()
                        slots.forEachIndexed { i, (time, list) ->
                            ReminderSlotRow(time, list, onOpen = { onOpenDose(list.first().key) })
                            if (i < slots.lastIndex) HorizontalDivider(color = Line)
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
            McButton(stringResource(R.string.s16_btn_settings), onOpenSettings, Modifier.weight(1f), ButtonKind.LIGHT, full = true)
            McButton(stringResource(R.string.s16_btn_permissions), onOpenPermissions, Modifier.weight(1f), ButtonKind.WHITE, full = true)
        }
    }
}

/** 청록 "다음 알림 / 내일 08:00 · 3개" 카드. 예약된 알림이 없으면 "예약된 알림이 없어요" */
@Composable
private fun NextReminderCard(items: List<ReminderDose>) {
    val first = items.firstOrNull()
    val value = if (first == null) stringResource(R.string.s16_next_none) else {
        val today = LocalDate.now()
        val day = when (first.date) {
            today -> stringResource(R.string.s07_day_today)
            today.plusDays(1) -> stringResource(R.string.s07_day_tomorrow)
            else -> fullDateText(first.date)
        }
        stringResource(R.string.s16_next_value, day, timeText(first.time), items.count { it.date == first.date && it.time == first.time })
    }
    val label = stringResource(R.string.s16_next_label)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(HeroDeepStart, HeroDeepEnd)))
            .padding(16.dp)
            .clearAndSetSemantics { contentDescription = "$label, $value" }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.18f))
        ) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

/** 주황 경고 카드: 알림이 울리지 않는 이유 한 줄. 누르면 고칠 수 있는 화면으로 */
@Composable
private fun WarnCard(text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HomeChipMissedBg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = HomeChipMissedText, modifier = Modifier.size(24.dp))
        Text(text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = HomeChipMissedText, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Text("›", fontSize = 18.sp, color = HomeChipMissedText)
    }
}

/** 같은 시각 한 줄: 시각 + 약 이름 꼬리표들 + › (누르면 첫 회차의 상세) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderSlotRow(time: java.time.LocalTime, doses: List<ReminderDose>, onOpen: () -> Unit) {
    val names = doses.map { it.medicationName }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(vertical = 12.dp)
            .clearAndSetSemantics { contentDescription = timeText(time) + " " + names.joinToString(", ") }
    ) {
        Text(timeText(time), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Navy, modifier = Modifier.width(62.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
            names.forEach { name ->
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(ProgressTrack).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Text("›", fontSize = 18.sp, color = Muted, modifier = Modifier.padding(start = 8.dp))
    }
}
