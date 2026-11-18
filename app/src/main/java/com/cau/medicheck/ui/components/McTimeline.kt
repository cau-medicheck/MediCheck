package com.cau.medicheck.ui.components

import com.cau.medicheck.R
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
import androidx.compose.foundation.layout.widthIn
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
import com.cau.medicheck.domain.DayPeriod
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.periodOf
import com.cau.medicheck.ui.theme.Amber
import com.cau.medicheck.ui.theme.CardBackground
import com.cau.medicheck.ui.theme.Faint
import com.cau.medicheck.ui.theme.HomeChipDoneBg
import com.cau.medicheck.ui.theme.HomeChipDoneText
import com.cau.medicheck.ui.theme.HomeChipMissedBg
import com.cau.medicheck.ui.theme.HomeChipMissedText
import com.cau.medicheck.ui.theme.HomeChipWaitBg
import com.cau.medicheck.ui.theme.HomeChipWaitText
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.theme.TimelineLine
import java.time.LocalDate

// 홈·일정·기록 화면이 함께 쓰는 시간표 부품: 시간표 한 줄, 상태 표시, 아래에서 올라오는 복약 기록 창.

/**
 * 시간표의 한 줄:  08:00 ●  약 이름                [상태] ›
 *                  아침  │  1정 · 아침 식후
 *
 * 시각 오른쪽의 세로선이 줄과 줄을 이어 하루의 흐름을 보여 준다. 점은 복용 완료면 채운 청록, 아니면 빈 주황 고리.
 * 줄 전체를 누르면 기록 창(RecordSheet)이 아래에서 올라온다.
 */
@Composable
fun TimelineRow(row: DoseRowUi, isFirst: Boolean, isLast: Boolean, onOpen: () -> Unit) {
    val done = row.display == DoseDisplayStatus.TAKEN
    val periodRes = when (periodOf(row.time)) {
        DayPeriod.MORNING -> R.string.s05_period_morning
        DayPeriod.LUNCH -> R.string.s05_period_lunch
        DayPeriod.EVENING -> R.string.s05_period_evening
        DayPeriod.NIGHT -> R.string.s05_period_night
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.s04_cd_open_dose), role = Role.Button, onClick = onOpen)
            .height(IntrinsicSize.Min)   // 세로선이 줄의 높이만큼 그려지도록
            .padding(horizontal = 18.dp)
    ) {
        // 시각 + 시간대
        Column(Modifier.widthIn(min = 52.dp).padding(vertical = 13.dp)) {
            Text(text = timeText(row.time), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(text = stringResource(periodRes), fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 3.dp))
        }
        // 세로선 + 점
        Canvas(Modifier.padding(horizontal = 9.dp).width(12.dp).fillMaxHeight().clearAndSetSemantics { }) {
            val x = size.width / 2
            val dotY = 22.dp.toPx()
            val r = 4.dp.toPx()
            drawLine(
                color = TimelineLine,
                start = Offset(x, if (isFirst) dotY else 0f),
                end = Offset(x, if (isLast) dotY else size.height),
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(Color.White, radius = r + 1.5.dp.toPx(), center = Offset(x, dotY))   // 선을 가리는 흰 바탕
            if (done) drawCircle(Teal, radius = r, center = Offset(x, dotY))
            else drawCircle(Amber, radius = r - 0.75.dp.toPx(), center = Offset(x, dotY), style = Stroke(1.5.dp.toPx()))
        }
        // 약 이름 + 복용량·메모
        Column(Modifier.weight(1f).padding(vertical = 13.dp)) {
            RowTitle(row.name)
            RowSub(if (row.memo.isBlank()) row.dosage else row.dosage + " · " + row.memo)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight()) {
            Spacer(Modifier.width(8.dp))
            DoseStatusChip(row.display)
            Text(
                text = "›",
                fontSize = 16.sp,
                color = Faint,
                modifier = Modifier.padding(start = 6.dp).clearAndSetSemantics { }
            )
        }
    }
}

/**
 * 회차의 상태 표시: 복용 완료(진한 청록) / 미기록(회색) / 예정 · 건너뜀(옅은 회색). 앱의 모든 화면이 함께 쓴다.
 */
@Composable
fun DoseStatusChip(status: DoseDisplayStatus, modifier: Modifier = Modifier) {
    val (textRes, background, textColor) = when (status) {
        DoseDisplayStatus.TAKEN -> Triple(R.string.status_pill_taken, HomeChipDoneBg, HomeChipDoneText)
        DoseDisplayStatus.SKIPPED -> Triple(R.string.s04_chip_skipped, HomeChipWaitBg, HomeChipWaitText)
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
    onOpenDetail: () -> Unit
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
                    fontWeight = FontWeight.ExtraBold,
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
                LabelValueRow(stringResource(R.string.s04_sheet_dosage), row.dosage, showDivider = row.memo.isNotBlank())
                if (row.memo.isNotBlank()) LabelValueRow(stringResource(R.string.s04_sheet_memo), row.memo, showDivider = false)
                HorizontalDivider(thickness = 1.dp, color = Line)

                if (isFuture) {
                    Text(
                        text = stringResource(R.string.s04_sheet_future),
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
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
