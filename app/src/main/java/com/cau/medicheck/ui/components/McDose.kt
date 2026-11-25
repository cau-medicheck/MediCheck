package com.cau.medicheck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.ui.theme.CircleInner
import com.cau.medicheck.ui.theme.DateChipBorder
import com.cau.medicheck.ui.theme.DateChipText
import com.cau.medicheck.ui.theme.HeroEnd
import com.cau.medicheck.ui.theme.HeroRingRest
import com.cau.medicheck.ui.theme.HeroStart
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
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
 *   복용 완료(청록) / 건너뜀(파랑) / 미복용(주황) / 예정(회색)
 */
@Composable
fun StatusPill(status: DoseDisplayStatus, modifier: Modifier = Modifier) = DoseStatusChip(status, modifier)

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
    val canQuickTake: Boolean,
    /** 복용 메모. 없으면 "" */
    val memo: String = "",
    /** 복용 조건(식전·식후 등) */
    val condition: DoseCondition = DoseCondition.NONE
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
    val detailName = stringResource(R.string.common_cd_dose_detail, row.name, time)
    val quickName = stringResource(R.string.s04_cd_quick_take, row.name, time)
    val showQuick = row.canQuickTake && onQuickTake != null
    // 글자를 1.5배 넘게 키워 쓰는 사람에게는 "상세 ›"를 아래로 내려 이름·배지 칸을 넓힌다(F084).
    val detailBelow = LocalDensity.current.fontScale >= 1.5f

    val detailButton: @Composable () -> Unit = {
        McButton(
            text = stringResource(R.string.common_detail),
            onClick = onDetail,
            kind = ButtonKind.LIGHT,
            small = true,
            // 같은 "상세 ›"가 여러 개라 TalkBack 이름에 약과 시각을 넣는다.
            modifier = Modifier.semantics { contentDescription = detailName }
        )
    }

    ListRow(
        leading = {
            Avatar(if (row.display == DoseDisplayStatus.TAKEN) Glyph.DOSE_DONE else Glyph.DOSE)
        },
        trailing = if (detailBelow) null else detailButton,
        showDivider = showDivider
    ) {
        RowTitle(row.name)
        RowSub(stringResource(R.string.common_dose_time_amount, time, row.dosage))
        StatusPill(row.display, Modifier.padding(top = 5.dp))
        if (showQuick || detailBelow) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                if (showQuick) {
                    McButton(
                        text = stringResource(R.string.s04_btn_quick_take),
                        onClick = { onQuickTake?.invoke() },
                        small = true,
                        modifier = Modifier.semantics { contentDescription = quickName }
                    )
                }
                if (detailBelow) detailButton()
            }
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
    mark: String? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    ListRow(
        modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
        leading = leading,
        trailing = {
            if (mark != null) Pill(mark, PillKind.ORANGE)
            // "›"는 장식이라 TalkBack이 읽지 않게 한다.
            else Text("›", fontSize = 16.sp, color = DateChipText, modifier = Modifier.clearAndSetSemantics { })
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
            // 왼쪽 위 → 오른쪽 아래로 흐르는 그라데이션
            .background(Brush.linearGradient(listOf(HeroStart, HeroEnd)))
            .padding(19.dp),
        content = content
    )
}

/**
 * 완료율 원 (v3 .circle: 지름 72, 가운데 원 58).
 * 바깥 고리를 12시 방향부터 시계 방향으로 완료율만큼 흰색으로 채운다. percent가 null이면 빈 고리 + "N/A".
 * 옆에 같은 숫자가 글자로 있으므로 TalkBack은 이 원을 읽지 않는다.
 */
@Composable
fun CircleProgress(percent: Int?, modifier: Modifier = Modifier) {
    // 글자를 키워 쓰면 원 안 글자가 넘치므로 원도 함께 키운다 (최대 1.6배).
    val grow = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(72.dp * grow)
            .clearAndSetSemantics { }
    ) {
        Canvas(Modifier.size(72.dp * grow)) {
            drawCircle(HeroRingRest)
            if (percent != null) {
                drawArc(
                    color = Color.White,
                    startAngle = -90f,                 // 12시 방향
                    sweepAngle = 360f * percent / 100f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height)
                )
            }
            drawCircle(CircleInner, radius = size.minDimension / 2f * (58f / 72f))
        }
        Text(
            text = if (percent == null) stringResource(R.string.common_na)
            else stringResource(R.string.common_percent, percent),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
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

/**
 * 한 주(월~일)를 한 화면에 보여 주고, 옆으로 밀면 앞뒤 주로 넘어가는 날짜 띠. (일정 화면, 시안 MediCheck1.html의 모양)
 *
 * - 칸에는 테두리가 없다. 요일 작은 글자 + 날짜 큰 숫자, 고른 날만 청록 바탕. 오늘은 청록 글자.
 * - 손가락으로 밀어 다른 주로 넘어가면, 그 주의 같은 요일이 골라진다(수요일을 보고 있었으면 다음 주 수요일).
 * - 달력 창이나 "오늘" 버튼으로 날짜가 바뀌면 그 주로 띠가 따라 움직인다.
 * HorizontalPager = 한 장씩 옆으로 넘기는 부품. 장 번호 WEEK_PAGER_CENTER가 "오늘이 든 주"다.
 */
@Composable
fun WeekPager(
    today: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val thisMonday = mondayOf(today)
    val selectedPage = WEEK_PAGER_CENTER + ((mondayOf(selected).toEpochDay() - thisMonday.toEpochDay()) / 7).toInt()
    val pagerState = rememberPagerState(initialPage = selectedPage, pageCount = { WEEK_PAGER_CENTER * 2 + 1 })

    // 바깥에서 날짜가 바뀌었으면(달력 창, 오늘 버튼, 칸 누르기) 그 주가 보이게 넘긴다.
    LaunchedEffect(selectedPage) {
        if (pagerState.currentPage != selectedPage) pagerState.animateScrollToPage(selectedPage)
    }
    // 손가락으로 넘겨 다른 주에 멈췄으면 그 주의 같은 요일을 고른다.
    val latestSelected by rememberUpdatedState(selected)
    val latestOnSelect by rememberUpdatedState(onSelect)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val current = latestSelected
            val currentPage = WEEK_PAGER_CENTER + ((mondayOf(current).toEpochDay() - thisMonday.toEpochDay()) / 7).toInt()
            if (page != currentPage) latestOnSelect(current.plusDays((page - currentPage) * 7L))
        }
    }

    HorizontalPager(state = pagerState, modifier = modifier.fillMaxWidth()) { page ->
        val monday = thisMonday.plusDays((page - WEEK_PAGER_CENTER) * 7L)
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            (0L..6L).forEach { offset ->
                val date = monday.plusDays(offset)
                val isSelected = date == selected
                val color = if (isSelected) Color.White else if (date == today) Teal else Navy
                val spoken = fullDateText(date)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Teal else Color.Transparent)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(date) })
                        .padding(vertical = 9.dp)
                        // 칸 안의 "수" "18" 대신 "11월 18일 수요일"로 읽게 한다.
                        .semantics(mergeDescendants = true) { contentDescription = spoken }
                ) {
                    Text(
                        text = weekdayText(date),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else if (date == today) Teal else Muted,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 3.dp).clearAndSetSemantics { }
                    )
                }
            }
        }
    }
}

/** 그 날이 든 주의 월요일 */
fun mondayOf(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

/** 주 넘기기의 가운데 장 번호(오늘이 든 주). 앞뒤로 이만큼의 주를 넘길 수 있다(약 10년). */
private const val WEEK_PAGER_CENTER = 520

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
            GlyphText(Glyph.BELL_OFF, fontSize = 15, color = WarnText)
            Text(
                text = stringResource(R.string.s34_off_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = WarnText,
                modifier = Modifier.padding(start = 5.dp)
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
            text = stringResource(R.string.s18_link_denied) + " ›",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Teal,
            modifier = Modifier.padding(top = 7.dp)
        )
    }
}
