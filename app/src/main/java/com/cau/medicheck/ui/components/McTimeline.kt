package com.cau.medicheck.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DayPeriod
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.periodOf
import com.cau.medicheck.ui.screens.dose.DayProgress
import com.cau.medicheck.ui.theme.CardBackground
import com.cau.medicheck.ui.theme.Faint
import com.cau.medicheck.ui.theme.HomeChipDoneBg
import com.cau.medicheck.ui.theme.HomeChipDoneText
import com.cau.medicheck.ui.theme.HomeChipMissedBg
import com.cau.medicheck.ui.theme.HomeChipMissedText
import com.cau.medicheck.ui.theme.HomeChipSkippedBg
import com.cau.medicheck.ui.theme.HomeChipSkippedText
import com.cau.medicheck.ui.theme.HomeChipWaitBg
import com.cau.medicheck.ui.theme.HomeChipWaitText
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText
import com.cau.medicheck.ui.theme.RateGood
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.theme.TimelineDotMissed
import com.cau.medicheck.ui.theme.TimelineDotSkipped
import com.cau.medicheck.ui.theme.TimelineDotWait
import com.cau.medicheck.ui.theme.TimelineGroupBg
import com.cau.medicheck.ui.theme.TimelineGroupDivider
import com.cau.medicheck.ui.theme.TimelineLine
import java.time.LocalDate
import java.time.LocalTime

// 홈·일정·날짜별 기록 화면이 함께 쓰는 시간표 부품: 같은 시각 묶음(TimelineGroup), 상태 표시, 아래에서 올라오는 복약 기록 창.

/**
 * 같은 시각의 약들을 한 묶음으로 보여 준다. (홈 "하루 복약 일정" 카드, 2026-11-23 시안 C-0)
 *
 *   08:00 ●  ┌ Lutein              [예정] › ┐
 *   아침  │  │ Probiotics          [예정] › │   ← 옅은 상자 하나에 담는다
 *         │  └ Vitamin D       [복용 완료] › ┘
 *   12:30 ○  ┌ Omega-3             [예정] › ┐
 *
 * 시각과 점은 묶음에 하나만 그린다. 약이 하나뿐이어도 같은 모양(상자 안에 한 줄)이다.
 * 약 줄마다 따로 눌러서 그 회차의 기록 창을 연다.
 *
 * @param rows 시각이 모두 같은 회차들 (화면에 보일 순서대로)
 */
@Composable
fun TimelineGroup(rows: List<DoseRowUi>, isFirst: Boolean, isLast: Boolean, onOpen: (DoseRowUi) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 18.dp)
    ) {
        TimelineTime(rows.first().time)
        TimelineRail(groupDisplay(rows), isFirst, isLast)
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))   // 누를 때 물결도 상자 모서리 안에서만
                .background(TimelineGroupBg)
        ) {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(thickness = 1.dp, color = TimelineGroupDivider, modifier = Modifier.padding(horizontal = 10.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = stringResource(R.string.s04_cd_open_dose), role = Role.Button) { onOpen(row) }
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        RowTitle(row.name)
                        DoseSubLine(row)
                    }
                    Spacer(Modifier.width(8.dp))
                    DoseStatusTail(row.display)
                }
            }
        }
    }
}

/**
 * 묶음 점의 색을 정할 상태. 먼저 걸리는 것으로 정한다.
 * 미복용이 하나라도 있으면 "미복용"(주황) → 예정이 있으면 "예정"(회색) → 모두 복용 완료면 "복용 완료"(청록)
 * → 그 밖(모두 기록했고 건너뜀이 섞임)은 "건너뜀"(파랑).
 */
private fun groupDisplay(rows: List<DoseRowUi>): DoseDisplayStatus = when {
    rows.any { it.display == DoseDisplayStatus.MISSED } -> DoseDisplayStatus.MISSED
    rows.any { it.display == DoseDisplayStatus.UPCOMING } -> DoseDisplayStatus.UPCOMING
    rows.all { it.display == DoseDisplayStatus.TAKEN } -> DoseDisplayStatus.TAKEN
    else -> DoseDisplayStatus.SKIPPED
}

/**
 * 시각 + 시간대 칸.
 * 폭을 "시각 글자(15sp) 크기의 3.7배"로 고정한다. 줄마다 폭이 같아야 세로선이 한 줄로 이어지고,
 * 글자를 키워 써도(접근성 200%) "08:00"이 잘리지 않는다.
 * (56.sp처럼 큰 sp 값을 바로 dp로 바꾸면 안 된다 — Android 14부터 큰 글자는 덜 커지게 계산해서 폭이 모자란다)
 */
@Composable
private fun TimelineTime(time: LocalTime) {
    val periodRes = when (periodOf(time)) {
        DayPeriod.DAWN -> R.string.s05_period_dawn
        DayPeriod.MORNING -> R.string.s05_period_morning
        DayPeriod.LUNCH -> R.string.s05_period_lunch
        DayPeriod.EVENING -> R.string.s05_period_evening
        DayPeriod.NIGHT -> R.string.s05_period_night
    }
    val timeWidth = with(LocalDensity.current) { 15.sp.toDp() * 3.7f }
    Column(Modifier.width(timeWidth).padding(vertical = 13.dp)) {
        Text(text = timeText(time), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy, maxLines = 1, softWrap = false)
        Text(text = stringResource(periodRes), fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 3.dp))
    }
}

/** 세로선 + 점. 맨 위 줄은 점에서 시작하고, 맨 아래 줄은 점에서 끝난다. */
@Composable
private fun TimelineRail(display: DoseDisplayStatus, isFirst: Boolean, isLast: Boolean) {
    Canvas(Modifier.padding(horizontal = 9.dp).width(12.dp).fillMaxHeight().clearAndSetSemantics { }) {
        val x = size.width / 2
        val dotY = 13.dp.toPx() + 15.sp.toPx() * 0.6f   // 위 여백 + 시각 글자 높이의 절반쯤 (글자를 키우면 점도 따라 내려간다)
        val r = 4.dp.toPx()
        drawLine(
            color = TimelineLine,
            start = Offset(x, if (isFirst) dotY else 0f),
            end = Offset(x, if (isLast) dotY else size.height),
            strokeWidth = 1.5.dp.toPx()
        )
        drawCircle(Color.White, radius = r + 1.5.dp.toPx(), center = Offset(x, dotY))   // 선을 가리는 흰 바탕
        // 점의 색은 오른쪽 상태 표시와 맞춘다: 복용 완료 = 채운 청록, 미복용 = 주황 고리, 건너뜀 = 파랑 고리, 예정 = 회색 고리
        val ring = when (display) {
            DoseDisplayStatus.TAKEN -> null
            DoseDisplayStatus.MISSED -> TimelineDotMissed
            DoseDisplayStatus.SKIPPED -> TimelineDotSkipped
            DoseDisplayStatus.UPCOMING -> TimelineDotWait
        }
        if (ring == null) drawCircle(Teal, radius = r, center = Offset(x, dotY))
        else drawCircle(ring, radius = r - 0.75.dp.toPx(), center = Offset(x, dotY), style = Stroke(1.5.dp.toPx()))
    }
}

/** 줄 오른쪽 끝: [상태] › */
@Composable
private fun DoseStatusTail(display: DoseDisplayStatus) {
    DoseStatusChip(display)
    Text(
        text = "›",
        fontSize = 16.sp,
        color = Faint,
        modifier = Modifier.padding(start = 6.dp).clearAndSetSemantics { }
    )
}

/**
 * 회차의 상태 표시: 복용 완료(진한 청록) / 건너뜀(파랑) / 미복용(주황, 시각이 지났는데 기록 안 함) / 예정(회색, 아직 시각 전).
 * 앱의 모든 화면이 함께 쓴다.
 */
@Composable
fun DoseStatusChip(status: DoseDisplayStatus, modifier: Modifier = Modifier) {
    val (textRes, background, textColor) = when (status) {
        DoseDisplayStatus.TAKEN -> Triple(R.string.status_pill_taken, HomeChipDoneBg, HomeChipDoneText)
        DoseDisplayStatus.SKIPPED -> Triple(R.string.s04_chip_skipped, HomeChipSkippedBg, HomeChipSkippedText)
        // 아직 기록하지 않은 회차: 시각이 지났으면 미복용(주황), 아직이면 예정(회색). (2026-11-23 결정)
        DoseDisplayStatus.MISSED -> Triple(R.string.s04_chip_missed, HomeChipMissedBg, HomeChipMissedText)
        DoseDisplayStatus.UPCOMING -> Triple(R.string.s04_chip_upcoming, HomeChipWaitBg, HomeChipWaitText)
    }
    Text(
        text = stringResource(textRes),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/**
 * 복약 기록 창: 화면 아래에서 올라오는 창에서 "복용 완료"·"건너뜀"을 바로 기록한다. (새 시안 MediCheck5.html)
 *
 * 화면을 옮기지 않고 기록할 수 있게 하려는 것이다. 실제 복용 시각 입력이나 변경 이력은 "회차 상세 보기"로 들어가서 한다.
 * 내일 이후의 회차는 기록할 수 없으므로 버튼 대신 안내 한 줄을 보인다.
 * 이 창은 따로 뜨는 창이라 본문의 글자 배율이 전해지지 않는다 → 안에서 ProvideBodyFontScale을 한 번 더 부른다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSheet(
    row: DoseRowUi,
    date: LocalDate,
    isFuture: Boolean,
    onDismiss: () -> Unit,
    onTaken: () -> Unit,
    onSkipped: () -> Unit,
    onOpenDetail: () -> Unit,
    /** "기록 전으로" (이미 기록한 회차를 미기록으로). null이면 버튼을 그리지 않는다 */
    onPending: (() -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardBackground,
        dragHandle = null
    ) {
        ProvideBodyFontScale {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp)) {
                // 복약 기록                                  [×]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.s04_sheet_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Muted,
                        modifier = Modifier.weight(1f)
                    )
                    val closeLabel = stringResource(R.string.s04_sheet_close)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = closeLabel, onClick = onDismiss)
                            .clearAndSetSemantics { contentDescription = closeLabel; role = Role.Button }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Navy, modifier = Modifier.size(22.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Avatar(Glyph.DOSE)
                    Spacer(Modifier.weight(1f))
                    DoseStatusChip(row.display)
                }
                Text(
                    text = row.name,
                    fontSize = 25.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.9).sp,
                    color = Navy,
                    modifier = Modifier.padding(top = 14.dp).semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.s04_sheet_when, fullDateText(date), timeText(row.time)),
                    fontSize = 13.sp,
                    color = ParagraphText,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                HorizontalDivider(thickness = 1.dp, color = Line)
                val hasCondition = row.condition != DoseCondition.NONE
                LabelValueRow(stringResource(R.string.s04_sheet_dosage), row.dosage, showDivider = hasCondition || row.memo.isNotBlank())
                if (hasCondition) LabelValueRow(stringResource(R.string.s04_sheet_condition), conditionLabel(row.condition), showDivider = row.memo.isNotBlank())
                if (row.memo.isNotBlank()) LabelValueRow(stringResource(R.string.s04_sheet_memo), row.memo, showDivider = false)
                HorizontalDivider(thickness = 1.dp, color = Line)

                if (isFuture) {
                    // 앞날: 복용 완료는 그날이 되어야 하고, 건너뜀만 미리 할 수 있다(그날 알림이 오지 않는다 — 2026-11-23)
                    val skipped = row.display == DoseDisplayStatus.SKIPPED
                    Text(
                        text = stringResource(if (skipped) R.string.s04_sheet_future_skipped else R.string.s04_sheet_future),
                        fontSize = 13.sp,
                        color = Muted,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 10.dp)
                    )
                    if (skipped && onPending != null) {
                        McButton(
                            text = stringResource(R.string.s04_sheet_to_pending),
                            onClick = onPending,
                            kind = ButtonKind.LIGHT,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (!skipped) {
                        McButton(
                            text = stringResource(R.string.s04_sheet_skip_ahead),
                            onClick = onSkipped,
                            kind = ButtonKind.LIGHT,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(Modifier.padding(top = 18.dp)) {
                        McButton(
                            text = stringResource(R.string.s10_btn_taken),
                            onClick = onTaken,
                            enabled = row.display != DoseDisplayStatus.TAKEN,
                            modifier = Modifier.weight(1.4f)
                        )
                        Spacer(Modifier.width(8.dp))
                        McButton(
                            text = stringResource(R.string.s10_btn_skipped),
                            onClick = onSkipped,
                            kind = ButtonKind.LIGHT,
                            enabled = row.display != DoseDisplayStatus.SKIPPED,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 이미 기록한 회차면 "기록 전으로"도 보인다 (잘못 누른 기록을 지우는 길)
                    val recorded = row.display == DoseDisplayStatus.TAKEN || row.display == DoseDisplayStatus.SKIPPED
                    if (onPending != null && recorded) {
                        McButton(
                            text = stringResource(R.string.s04_sheet_to_pending),
                            onClick = onPending,
                            kind = ButtonKind.LIGHT,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
                LinkText(
                    text = stringResource(R.string.s04_sheet_detail),
                    onClick = onOpenDetail,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp)
                )
            }
        }
    }
}

/**
 * 하루의 진행률 카드: "복약 진행률 33%"  [총 3회] / 진행 막대 / 완료 · 건너뜀 · 미기록(오늘은 "예정") 횟수.
 * 날짜별 기록(S12)과, 일정(S05)에서 오늘과 지난 날짜를 볼 때 함께 쓴다.
 * 기록한 상태로 센다(DayProgress) — 복용 완료를 누르면 시각과 상관없이 바로 올라간다.
 *
 * @param isToday true면 남은 회차를 "예정"으로, 아니면 "미기록"으로 적는다(지난 날의 기록 안 한 약은 예정이 아니다)
 */
@Composable
fun DayRateCard(progress: DayProgress, isToday: Boolean, modifier: Modifier = Modifier) {
    val rate = progress.percent
    SurfaceCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.s12_rate_label), fontSize = 11.sp, color = Muted)
                MetricNumber(
                    text = if (rate == null) stringResource(R.string.common_na)
                    else stringResource(R.string.common_percent, rate),
                    fontSize = 30
                )
            }
            Pill(stringResource(R.string.s12_total, progress.total), PillKind.BLUE)
        }
        // 회차가 없으면(N/A) 빈 막대. 큰 숫자와 같은 정보라 TalkBack은 읽지 않는다.
        ProgressBar(
            fraction = (rate ?: 0) / 100f,
            modifier = Modifier
                .padding(top = 10.dp, bottom = 16.dp)
                .clearAndSetSemantics { }
        )
        // 아래: 선으로 나눈 숫자 칸 (통계 요약과 같은 모양). 오늘은 예정까지 네 칸, 지난 날은 예정이 없어 세 칸. 비율은 그날 전체 회차 기준
        HorizontalDivider(color = Line)
        StatusCountRow(
            items = buildList {
                add(StatusCount(stringResource(R.string.common_count_taken), progress.taken, RateGood, HomeChipDoneText))
                add(StatusCount(stringResource(R.string.common_count_skipped), progress.skipped, TimelineDotSkipped, HomeChipSkippedText))
                add(StatusCount(stringResource(R.string.common_count_missed), progress.missed, TimelineDotMissed, HomeChipMissedText))
                if (isToday) add(StatusCount(stringResource(R.string.s04_chip_upcoming), progress.upcoming, TimelineDotWait, HomeChipWaitText))
            },
            total = progress.total,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

/** 숫자 칸 하나의 값: 이름 · 횟수 · 점 색 · 숫자 색 */
data class StatusCount(val label: String, val count: Int, val dot: Color, val textColor: Color)

/**
 * 선으로 나눈 숫자 칸 한 줄: "● 완료 / 5회 / 56%". 통계 요약 카드와 일정·날짜별 기록의 진행률 카드가 함께 쓴다.
 * @param total 비율의 분모 (0이면 비율을 쓰지 않는다)
 */
@Composable
fun StatusCountRow(items: List<StatusCount>, total: Int, modifier: Modifier = Modifier) {
    val unit = stringResource(R.string.s15_count_unit)
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        items.forEachIndexed { i, item ->
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(Line))
            val share = if (total == 0) null else Math.round(item.count * 100f / total)
            val shareText = share?.let { stringResource(R.string.common_percent, it) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = "${item.label} ${item.count}$unit ${shareText.orEmpty()}" }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(item.dot))
                    Text(item.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(start = 5.dp))
                }
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(item.count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = item.textColor)
                    Text(unit, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Muted, modifier = Modifier.padding(start = 1.dp, bottom = 4.dp))
                }
                if (shareText != null) Text(shareText, fontSize = 11.sp, color = Muted)
            }
        }
    }
}

/** 홈·일정·날짜별 기록이 함께 쓴다. 시각 순서로 놓인 줄들을 "같은 시각끼리" 이웃한 묶음으로 나눈다. 예: [08:00, 08:00, 12:30] → [[08:00, 08:00], [12:30]] */
fun groupBySameTime(rows: List<DoseRowUi>): List<List<DoseRowUi>> {
    val groups = mutableListOf<MutableList<DoseRowUi>>()
    for (row in rows) {
        val last = groups.lastOrNull()
        if (last != null && last.first().time == row.time) last.add(row) else groups.add(mutableListOf(row))
    }
    return groups
}
