package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.DateChipStrip
import com.cau.medicheck.ui.components.DoseRow
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.HeroCard
import com.cau.medicheck.ui.components.LinkText
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.NotificationOffCard
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.fullDateText
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
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
        HomeHero(displayName, state)

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
                .padding(top = 18.dp, bottom = 12.dp)
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
                Column(Modifier.padding(horizontal = 16.dp)) {
                    state.rows.forEachIndexed { index, row ->
                        DoseRow(
                            row = row,
                            onDetail = { actions.onOpenDose(row.key) },
                            onQuickTake = { actions.onQuickTake(row.key) },
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
 * 홈 맨 위의 카드: 인사말 + "몇 번 중 몇 번" 큰 숫자 + 진행 막대.
 * 숫자는 한 번만 보여 준다(전에는 같은 완료율이 글자와 원으로 두 번 나왔다).
 */
@Composable
private fun HomeHero(displayName: String, state: DayDosesUiState) {
    val rate = state.counts.completionRate   // null이면 도래한 회차가 없다 → "N/A"
    val rateText = if (rate == null) stringResource(R.string.common_na)
    else stringResource(R.string.common_percent, rate)

    HeroCard {
        Text(
            text = stringResource(R.string.s04_greeting, displayName),
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.85f)
        )
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 14.dp)) {
            Text(
                text = stringResource(R.string.s04_hero_count, state.counts.taken, state.counts.due),
                fontSize = 38.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = rateText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Text(
            text = stringResource(R.string.s04_rate_label),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 2.dp)
        )
        // 진행 막대. 숫자가 위에 글자로 있으므로 TalkBack은 읽지 않는다.
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.25f))
                .clearAndSetSemantics { }
        ) {
            Box(
                Modifier
                    .fillMaxWidth(((rate ?: 0) / 100f).coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            )
        }
    }
}
