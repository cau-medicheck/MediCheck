package com.cau.medicheck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.theme.DateChipBorder
import com.cau.medicheck.ui.theme.DateChipText
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.theme.WarnBg
import com.cau.medicheck.ui.theme.WarnBorder
import com.cau.medicheck.ui.theme.WarnText
import java.time.LocalDate
import java.time.LocalTime

/*
 * 복용 회차를 그리는 부품 모음. (구현 명세 00 §5.1·5.2, 02)
 * 홈(S04)·일정(S05)·회차 상세(S10)가 함께 쓰고, 4단계의 날짜별 기록(S12)도 쓴다.
 */

// ---------------------------------------------------------------------------------------------
// 상태 배지 · 회차 줄
// ---------------------------------------------------------------------------------------------

/**
 * 복용 상태 배지. 기호와 글자가 함께 있어서 색을 못 봐도 구분된다(F087).
 *   ✓ 복용 완료(민트) / ↷ 건너뜀(주황) / ! 기한 경과 · 미기록(회색) / ○ 복용 예정(회색)
 */
@Composable
fun StatusPill(status: DoseDisplayStatus, modifier: Modifier = Modifier) {
    val (textRes, kind) = when (status) {
        DoseDisplayStatus.TAKEN -> R.string.status_pill_taken to PillKind.MINT
        DoseDisplayStatus.SKIPPED -> R.string.status_pill_skipped to PillKind.ORANGE
        DoseDisplayStatus.MISSED -> R.string.status_pill_overdue to PillKind.GRAY
        DoseDisplayStatus.UPCOMING -> R.string.status_pill_upcoming to PillKind.GRAY
    }
    Pill(text = stringResource(textRes), kind = kind, modifier = modifier)
}

/**
 * 회차 줄 하나를 그리는 데 필요한 값. ViewModel이 만들어서 화면에 넘긴다.
 * @param canQuickTake 줄에서 바로 "✓ 복용 완료"를 누를 수 있는가 (미기록이고 오늘 이전 날짜)
 */
data class DoseRowUi(
    val key: DoseKey,
    val name: String,
    val time: LocalTime,
    val dosage: String,
    val display: DoseDisplayStatus,
    val canQuickTake: Boolean
)

/**
 * 회차 한 줄: [아이콘] 약 이름 / "08:00 · 1정" / 상태 배지 / (미기록이면) "✓ 복용 완료"   [상세 ›]
 *
 * @param onQuickTake 줄에서 바로 복용 완료. null이면 버튼을 그리지 않는다(S12).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoseRow(
    row: DoseRowUi,
    onDetail: () -> Unit,
    onQuickTake: (() -> Unit)?,
    showDivider: Boolean = true
) {
    val time = timeText(row.time)
    val quickName = stringResource(R.string.s04_cd_quick_take, row.name, time)
    val showQuick = row.canQuickTake && onQuickTake != null

    // 줄 전체를 누르면 회차 상세로 간다. 줄 안의 "복용 완료" 버튼은 따로 눌린다.
    ListRow(
        leading = {
            Avatar(if (row.display == DoseDisplayStatus.TAKEN) Glyph.DOSE_DONE else Glyph.DOSE)
        },
        trailing = { RowChevron() },
        showDivider = showDivider,
        onClick = onDetail,
        onClickLabel = stringResource(R.string.common_cd_open_detail)
    ) {
        RowTitle(row.name)
        RowSub(stringResource(R.string.common_dose_time_amount, time, row.dosage))
        StatusPill(row.display, Modifier.padding(top = 5.dp))
        if (showQuick) {
            McButton(
                text = stringResource(R.string.s04_btn_quick_take),
                onClick = { onQuickTake?.invoke() },
                small = true,
                modifier = Modifier.padding(top = 10.dp).semantics { contentDescription = quickName }
            )
        }
    }
}

/**
 * 누르면 다른 화면으로 가는 줄: 제목 + 설명 + 오른쪽 표시. (v3 optionRows)
 * @param mark 오른쪽에 붙일 배지 글자 (예: "Should"). 없으면 "›"
 * @param enabled false면 흐리게 그리고 눌리지 않는다
 * @param leading 왼쪽에 붙일 아이콘 상자 (설정 허브의 줄). 없으면 글자만
 */
@Composable
fun OptionRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    ListRow(
        modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
        leading = leading,
        trailing = {
            // "›"는 장식이라 TalkBack이 읽지 않게 한다.
            Text("›", fontSize = 16.sp, color = DateChipText, modifier = Modifier.clearAndSetSemantics { })
        },
        showDivider = showDivider,
        onClick = if (enabled) onClick else null
    ) {
        RowTitle(title)
        RowSub(description)
    }
}

// ---------------------------------------------------------------------------------------------
// 홈의 큰 카드 · 완료율 원 · 날짜 줄
// ---------------------------------------------------------------------------------------------

/** 홈 맨 위의 청록 그라데이션 카드 (v3 .hero: 130도, 둥글기 19, 여백 19). 안의 글자는 흰색으로 쓴다. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(Teal)   // 단색. 전에는 그라데이션이었다
            .padding(22.dp),
        content = content
    )
}

/**
 * 날짜 줄: 오늘-3일 ~ 오늘+3일의 7칸. (v3 dateStrip)
 * 고른 날짜 칸만 청록 바탕이다. 고른 날짜가 이 범위 밖이면 강조된 칸이 없다.
 * 칸 너비는 화면 폭을 7로 나눈 값이되 43dp보다 좁아지지는 않는다. 모자라면 가로로 밀어 볼 수 있다.
 */
@Composable
fun DateChipStrip(
    today: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 6.dp
        val chipWidth = maxOf(43.dp, (maxWidth - gap * 6) / 7)
        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            (-3L..3L).forEach { offset ->
                val date = today.plusDays(offset)
                val isSelected = date == selected
                val shape = RoundedCornerShape(12.dp)
                val textColor = if (isSelected) Color.White else DateChipText
                val spoken = fullDateText(date)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(chipWidth)
                        .clip(shape)
                        .background(if (isSelected) Teal else Color.White)
                        .border(BorderStroke(1.dp, if (isSelected) Teal else DateChipBorder), shape)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(date) })
                        .padding(horizontal = 4.dp, vertical = 9.dp)
                        // 칸 안의 "화" "17" 대신 "11월 17일 화요일"로 읽게 한다.
                        .semantics(mergeDescendants = true) { contentDescription = spoken }
                ) {
                    Text(
                        text = weekdayText(date),
                        fontSize = 11.sp,
                        color = textColor,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 알림 꺼짐 카드
// ---------------------------------------------------------------------------------------------

/**
 * 폰의 알림이 꺼져 있을 때 홈에 보이는 안내 카드. 어디를 눌러도 안내 화면(S34)으로 간다.
 * 보일지 말지는 부르는 쪽이 정한다(rememberNotificationsOff).
 */
@Composable
fun NotificationOffCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WarnBg)
            .border(1.dp, WarnBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            McIcon(Glyph.BELL_OFF, size = 16, tint = WarnText)
            Text(
                text = stringResource(R.string.s34_off_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = WarnText,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Text(
            text = stringResource(R.string.s34_off_body),
            fontSize = 11.sp,
            lineHeight = 17.sp,
            color = WarnText,
            modifier = Modifier.padding(top = 5.dp)
        )
        Text(
            text = stringResource(R.string.s18_link_denied),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Teal,
            modifier = Modifier.padding(top = 7.dp)
        )
    }
}
