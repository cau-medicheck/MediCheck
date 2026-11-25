package com.cau.medicheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.theme.CalDayActive
import com.cau.medicheck.ui.theme.CalDayText
import com.cau.medicheck.ui.theme.CalDot
import com.cau.medicheck.ui.theme.CalWeekdayText
import com.cau.medicheck.ui.theme.Navy
import java.time.LocalDate
import java.time.YearMonth

/*
 * 달력 부품. 기록 캘린더(S11)와 월간 통계(S14)가 쓴다. (구현 명세 04 공통 작업 5)
 */

/** 달 넘기기 줄:  [‹]   2026년 11월   [›] */
@Composable
fun MonthSwitcher(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        IconSquareButton(Glyph.PREV, stringResource(R.string.common_cd_prev_month), outlined = true, onClick = onPrev)
        Text(
            text = monthTitleText(month),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconSquareButton(Glyph.NEXT, stringResource(R.string.common_cd_next_month), outlined = true, onClick = onNext)
    }
}

/**
 * 한 달 달력 (v3 .calendar): 요일 머리줄(일요일부터) + 날짜 칸.
 *
 * - 1일 앞에는 그 달 1일의 요일만큼 빈칸을 둔다(일요일이면 0칸 … 토요일이면 6칸).
 * - 회차가 있는 날짜에는 숫자 아래에 점을 찍는다. 점은 "복용 일정이 있음"을 뜻하지, 복용했는지와는 상관없다.
 * - 고른 날짜 칸만 청록 바탕이다. "오늘" 표시는 따로 없다(프로토타입과 같음).
 *
 * @param datesWithDot 점을 찍을 날짜들
 */
@Composable
fun MonthCalendarGrid(
    month: YearMonth,
    selected: LocalDate,
    datesWithDot: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekdays = stringArrayResource(R.array.common_weekdays)   // "일"…"토"
    // 1일 앞의 빈칸 수. dayOfWeek.value는 월=1 … 일=7이라 7로 나눈 나머지가 "일요일부터 센 자리"다.
    val leadingBlanks = month.atDay(1).dayOfWeek.value % 7
    // 빈칸(null)과 날짜를 한 줄로 늘어놓고 7개씩 끊으면 주(가로줄)가 된다.
    val cells: List<LocalDate?> =
        List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weekdays.forEach { name ->
                Text(
                    text = name,
                    fontSize = 11.sp,
                    color = CalWeekdayText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 9.dp)
                        .clearAndSetSemantics { }   // 날짜 칸마다 요일을 읽어 주므로 머리줄은 읽지 않는다
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    if (date == null) Spacer(Modifier.weight(1f))
                    else DayCell(date, date == selected, date in datesWithDot, onSelect, Modifier.weight(1f))
                }
                // 마지막 주가 7칸이 안 되면 남은 자리를 비워 칸 너비를 맞춘다.
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** 날짜 칸 하나: 숫자 + (회차가 있으면) 점 */
@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    hasDot: Boolean,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier
) {
    // TalkBack 설명: "11월 16일 월요일" 또는 "11월 16일 월요일, 복용 일정 있음"
    val dateText = fullDateText(date)
    val spoken = if (hasDot) stringResource(R.string.s11_cd_day_with_dose, dateText) else dateText
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 48.dp)   // 손가락으로 누르기에 충분한 높이
            .clip(RoundedCornerShape(11.dp))
            .background(if (isSelected) CalDayActive else Color.White)
            .selectable(selected = isSelected, role = Role.Button, onClick = { onSelect(date) })
            .semantics(mergeDescendants = true) { contentDescription = spoken }
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 12.sp,
            color = if (isSelected) Color.White else CalDayText,
            modifier = Modifier.clearAndSetSemantics { }
        )
        // 점이 없는 날도 같은 높이의 자리를 둬서 숫자 위치가 흔들리지 않게 한다.
        Box(Modifier.padding(top = 3.dp)) {
            Dot(color = if (!hasDot) Color.Transparent else if (isSelected) Color.White else CalDot)
        }
    }
}
