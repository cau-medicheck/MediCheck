package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.DateChipStrip
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.ui.components.DoseRowUi
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.HeroCard
import com.cau.medicheck.ui.components.LinkText
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.NotificationOffCard
import com.cau.medicheck.ui.components.ProgressBar
import com.cau.medicheck.ui.components.RowSub
import com.cau.medicheck.ui.components.RowTitle
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.StatusPill
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.Faint
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.RowDivider
import java.time.LocalDate

/** 홈 화면의 버튼들이 부를 함수 묶음. (화면 함수의 인자가 너무 길어지지 않게 묶었다) */
class HomeActions(
    val onSelectDay: (LocalDate) -> Unit,
    val onOpenSchedule: () -> Unit,
    val onOpenDose: (DoseKey) -> Unit,
    val onQuickTake: (DoseKey) -> Unit,
    val onAddMedication: () -> Unit,
    val onOpenStats: () -> Unit,
    val onOpenNotificationHelp: () -> Unit
)

/**
 * S04 홈 대시보드 (v3 home()).
 *
 * 위에서 아래로: 큰 카드(인사 + 선택한 날짜의 완료율) → (알림이 꺼져 있으면) 안내 카드
 * → 날짜 줄(오늘 앞뒤 3일) → 그날의 회차 목록 → "＋ 새 약 등록" → 이번 주 리포트 카드.
 *
 * @param showNotificationOffCard 폰의 알림이 꺼져 있는가 (판정은 MainGraph가 한다)
 */
@Composable
fun S04HomeScreen(
    displayName: String,
    state: DayDosesUiState,
    showNotificationOffCard: Boolean,
    actions: HomeActions
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s04)) {
        NextDoseCard(state, onOpenDose = actions.onOpenDose)

        if (showNotificationOffCard) {
            NotificationOffCard(onClick = actions.onOpenNotificationHelp, modifier = Modifier.padding(top = 16.dp))
        }

        SectionTitle(stringResource(R.string.s04_section_calendar))
        DateChipStrip(today = state.today, selected = state.date, onSelect = actions.onSelectDay)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
        ) {
            Text(
                text = fullDateText(state.date),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Navy,
                modifier = Modifier.weight(1f)
            )
            LinkText(stringResource(R.string.s04_link_schedule), onClick = actions.onOpenSchedule)
        }
        DayProgress(taken = state.counts.taken, total = state.rows.size, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))

        SurfaceCard(padding = 0.dp) {
            if (state.rows.isEmpty() && !state.isLoading) {
                EmptyState(
                    emoji = Glyph.EMPTY_HOME,
                    title = stringResource(R.string.s04_empty_title),
                    body = stringResource(R.string.s04_empty_body),
                    actionLabel = stringResource(R.string.s04_empty_action),
                    onAction = actions.onAddMedication
                )
            } else {
                Column {
                    state.rows.forEachIndexed { index, row ->
                        TimelineRow(
                            row = row,
                            onOpen = { actions.onOpenDose(row.key) },
                            showDivider = index != state.rows.lastIndex
                        )
                    }
                }
            }
        }

        McButton(
            text = stringResource(R.string.s04_btn_add_med),
            onClick = actions.onAddMedication,
            full = true,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.s04_report_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text(
                        text = stringResource(R.string.s04_report_body),
                        fontSize = 11.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                McButton(
                    text = stringResource(R.string.s04_report_action),
                    onClick = actions.onOpenStats,
                    kind = ButtonKind.LIGHT,
                    small = true
                )
            }
        }
    }
}

/**
 * 홈 맨 위의 카드: 지금 기록을 기다리는 약 하나와 "복용 기록하기" 버튼.
 *
 * 고르는 순서: 시간이 지났는데 기록이 없는 회차 중 가장 이른 것 → 없으면 아직 시간이 안 된 회차 중 가장 이른 것.
 * (rows는 시각 순서로 들어온다) 둘 다 없으면 버튼 없이 안내 한 줄만 보인다.
 */
@Composable
private fun NextDoseCard(state: DayDosesUiState, onOpenDose: (DoseKey) -> Unit) {
    val next = state.rows.firstOrNull { it.display == DoseDisplayStatus.MISSED }
        ?: state.rows.firstOrNull { it.display == DoseDisplayStatus.UPCOMING }

    HeroCard {
        if (next == null) {
            // 회차가 하나도 없는 날과, 모두 기록한 날을 나눠 말한다.
            Text(
                text = stringResource(if (state.rows.isEmpty()) R.string.s04_next_none else R.string.s04_next_all_done),
                fontSize = 19.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            return@HeroCard
        }
        Text(
            text = stringResource(R.string.s04_next_label),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.87f)
        )
        Text(
            text = next.name,
            fontSize = 25.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            color = Color.White,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = stringResource(R.string.common_dose_time_amount, timeText(next.time), next.dosage),
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = 3.dp, bottom = 14.dp)
        )
        McButton(
            text = stringResource(R.string.s04_next_action),
            onClick = { onOpenDose(next.key) },
            kind = ButtonKind.WHITE,
            small = true
        )
    }
}

/** 일정 목록 위의 진행 상황: "복용 완료 1 / 3회" + 얇은 진행 막대. 회차가 없는 날은 그리지 않는다. */
@Composable
private fun DayProgress(taken: Int, total: Int, modifier: Modifier = Modifier) {
    if (total == 0) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = stringResource(R.string.s04_progress, taken, total),
            fontSize = 12.sp,
            color = Muted
        )
        Spacer(Modifier.width(10.dp))
        // 막대는 글자와 같은 내용이라 TalkBack이 읽지 않는다.
        ProgressBar(fraction = taken / total.toFloat(), modifier = Modifier.width(96.dp).clearAndSetSemantics { })
    }
}

/**
 * 시간표식 한 줄: [08:00]  약 이름 / 복용량   [상태]  ›
 * 줄 전체를 누르면 회차 상세로 간다(기록은 거기서 한다). 줄 안에는 버튼을 두지 않는다.
 */
@Composable
private fun TimelineRow(row: DoseRowUi, onOpen: () -> Unit, showDivider: Boolean) {
    Column(
        Modifier.clickable(onClickLabel = stringResource(R.string.s04_cd_open_dose), role = Role.Button, onClick = onOpen)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = timeText(row.time),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Navy,
                modifier = Modifier.widthIn(min = 54.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                RowTitle(row.name)
                RowSub(row.dosage)
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(row.display)
            Text(
                text = "›",
                fontSize = 16.sp,
                color = Faint,
                modifier = Modifier.padding(start = 6.dp).clearAndSetSemantics { }
            )
        }
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = RowDivider, modifier = Modifier.padding(horizontal = 16.dp))
    }
}
