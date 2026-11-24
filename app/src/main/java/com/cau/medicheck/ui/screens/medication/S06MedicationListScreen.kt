package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.MedicationFilter
import com.cau.medicheck.domain.MedicationStatus
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McMedIcon
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.McSwitch
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.conditionLabel
import com.cau.medicheck.ui.components.scheduleDaysText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.Faint
import com.cau.medicheck.ui.theme.FieldBorder
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.PillOrangeBg
import com.cau.medicheck.ui.theme.PillOrangeText
import com.cau.medicheck.ui.theme.ProgressTrack
import com.cau.medicheck.ui.theme.RateFair
import com.cau.medicheck.ui.theme.RateGood
import com.cau.medicheck.ui.theme.RateLow
import com.cau.medicheck.ui.theme.RowSubText
import com.cau.medicheck.ui.theme.Teal

/** 내 약 목록의 버튼들이 부를 함수 묶음 */
class MedicationListActions(
    val onQueryChange: (String) -> Unit,
    val onFilterChange: (MedicationFilter) -> Unit,
    val onAdd: () -> Unit,
    val onOpen: (medId: Long) -> Unit,
    /** 약 카드의 스위치: 켜면 복용 중, 끄면 쉬는 중 */
    val onActiveChange: (medId: Long, name: String, active: Boolean) -> Unit
)

/**
 * S06 내 약 목록 (v3 medsList() → 2026-11-24 다듬기, 시안: 04_화면캡처/내약탭_시안/켜기끄기_시안.html).
 *
 *   [약 이름으로 검색......] [＋ 추가]
 *   (전체 6) (복용 중 5) (쉬는 중 1) (종료 0)        ← 눌러서 거르기
 *   ┌──────────────────────────────────────────┐
 *   │ [✚] Vitamin D 1 tab              [스위치] │   ← 스위치: 켜짐 = 복용 중, 꺼짐 = 쉬는 중
 *   │     (08:00 식후) 매일                      │
 *   │     최근 7일 ▬▬▬▬▬▬▭ 86%                   │
 *   │     📅 30일 중 13일째 D-17                 │   ← 종료일이 있는 약만
 *   └──────────────────────────────────────────┘
 *
 * 검색칸에 글자를 치면 이름에 그 글자가 든 약만 남는다. 종료된 약은 "종료" 배지를 달고 목록 아래쪽에 있다.
 */
@Composable
fun S06MedicationListScreen(state: MedicationListUiState, actions: MedicationListActions) {
    val focusManager = LocalFocusManager.current

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s06)) {
        // 탭 화면이라 큰 제목은 두지 않는다(하단 탭 이름이 같은 말이다).
        // [약 이름으로 검색........] [＋ 추가]
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormField(
                value = state.query,
                onValueChange = actions.onQueryChange,
                placeholder = stringResource(R.string.s06_search_hint),
                label = stringResource(R.string.s06_search_hint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                // 키보드의 검색 키를 누르면 키보드를 닫는다 (목록은 글자를 칠 때마다 이미 걸러져 있다).
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            McButton(stringResource(R.string.s06_btn_add), actions.onAdd, small = true)
        }

        FilterChips(state, actions.onFilterChange)

        SurfaceCard(Modifier.padding(top = 12.dp), padding = 0.dp) {
            val noMedsAtAll = state.counts[MedicationFilter.ALL] == 0
            when {
                state.isLoading -> Spacer(Modifier.height(120.dp))
                state.rows.isEmpty() && noMedsAtAll ->
                    // 약이 하나도 없을 때와 검색 결과가 없을 때 같은 안내를 쓴다 (v3)
                    EmptyState(
                        emoji = Glyph.PILL,
                        title = stringResource(R.string.s06_empty_title),
                        body = stringResource(R.string.s06_empty_body),
                        actionLabel = stringResource(R.string.s06_empty_action),
                        onAction = actions.onAdd
                    )
                state.rows.isEmpty() ->
                    Text(
                        text = stringResource(R.string.s06_filter_empty),
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp)
                    )
                else -> Column {
                    state.rows.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider(thickness = 1.dp, color = Line, modifier = Modifier.padding(horizontal = 16.dp))
                        MedicationCard(row, actions)
                    }
                }
            }
        }
    }
}

/** (전체 6) (복용 중 5) (쉬는 중 1) (종료 0) — 하나만 고른다 */
@Composable
private fun FilterChips(state: MedicationListUiState, onChange: (MedicationFilter) -> Unit) {
    // 불러오는 동안에는 "전체 0"처럼 틀린 숫자가 잠깐 보이지 않게 자리만 둔다
    if (state.isLoading) {
        Spacer(Modifier.padding(top = 12.dp).height(36.dp))
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()).selectableGroup()
    ) {
        MedicationFilter.entries.forEach { filter ->
            val label = when (filter) {
                MedicationFilter.ALL -> R.string.s06_filter_all
                MedicationFilter.ACTIVE -> R.string.s06_filter_active
                MedicationFilter.PAUSED -> R.string.s06_filter_paused
                MedicationFilter.ENDED -> R.string.s06_filter_ended
            }
            val selected = filter == state.filter
            val shape = RoundedCornerShape(20.dp)
            Text(
                text = stringResource(label, state.counts[filter] ?: 0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else Navy,
                modifier = Modifier
                    .clip(shape)
                    .background(if (selected) Navy else Color.White)
                    .border(1.dp, if (selected) Navy else FieldBorder, shape)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onChange(filter) })
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
    }
}

/**
 * 약 카드 하나. 카드를 누르면 약 상세, 오른쪽 위 스위치를 누르면 복용 중 ↔ 쉬는 중.
 * 쉬는 중이면 아이콘·시간 칩을 흐리게 하고(이름은 읽히도록 그대로) 복용률 대신 "켜면 오늘부터 일정에 들어가요".
 * 종료된 약은 스위치 대신 "종료" 배지(다시 쓰려면 종료일을 고친다).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicationCard(row: MedicationRowUi, actions: MedicationListActions) {
    val med = row.med
    val resting = row.status == MedicationStatus.PAUSED
    val dim = if (resting) 0.5f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.common_detail), role = Role.Button) { actions.onOpen(med.id) }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        McMedIcon(med.shape, med.color, Modifier.alpha(dim))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // 이름 1 tab                 (쉬는 중) [스위치]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f)) {
                    Text(med.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
                    Text(med.dosage, fontSize = 12.sp, color = RowSubText, modifier = Modifier.padding(start = 6.dp, bottom = 1.dp))
                }
                when (row.status) {
                    MedicationStatus.ENDED -> Pill(stringResource(R.string.med_status_ended), PillKind.GRAY)
                    else -> {
                        if (resting) Pill(stringResource(R.string.med_status_paused), PillKind.GRAY, Modifier.padding(end = 8.dp))
                        McSwitch(
                            checked = !resting,
                            onCheckedChange = { on -> actions.onActiveChange(med.id, med.name, on) },
                            contentDescription = stringResource(R.string.s06_cd_active, med.name)
                        )
                    }
                }
            }
            // (08:00 식후) (20:00) 매일
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp).alpha(dim)
            ) {
                med.schedules.forEach { schedule -> TimeChip(timeText(schedule.time), schedule.condition) }
                Text(scheduleDaysText(med.schedules), fontSize = 11.sp, color = RowSubText)
            }
            when {
                row.status == MedicationStatus.ENDED -> Unit
                resting -> Text(
                    text = stringResource(R.string.s06_resting_hint),
                    fontSize = 11.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 8.dp)
                )
                else -> RecentRateLine(row.recentRate)
            }
            val course = row.course
            if (course != null && !resting && row.status != MedicationStatus.ENDED) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Text(stringResource(R.string.s06_course, course.total, course.day), fontSize = 11.sp, color = Navy)
                    Text(
                        text = if (course.daysLeft == 0) stringResource(R.string.s06_dday_today) else stringResource(R.string.s06_dday, course.daysLeft),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PillOrangeText,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(PillOrangeBg)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Text("›", fontSize = 18.sp, color = Faint, modifier = Modifier.padding(start = 6.dp).clearAndSetSemantics { })
    }
}

/** 시간 칩: "08:00 식후" (조건이 상관없음이면 시간만) */
@Composable
private fun TimeChip(time: String, condition: DoseCondition) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ProgressTrack)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(time, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy)
        if (condition != DoseCondition.NONE) {
            Text(conditionLabel(condition), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Teal, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** 최근 7일 ▬▬▬▭ 86% — 막대 색: 80% 이상 청록, 50~79% 주황, 50% 미만 빨강. 계산할 회차가 없으면 "기록 없음" */
@Composable
private fun RecentRateLine(rate: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Text(stringResource(R.string.s06_recent), fontSize = 11.sp, color = Muted)
        // 막대는 옆 숫자와 같은 정보라 TalkBack이 읽지 않는다
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ProgressTrack)
                .clearAndSetSemantics { }
        ) {
            if (rate != null) {
                val color = when {
                    rate >= 80 -> RateGood
                    rate >= 50 -> RateFair
                    else -> RateLow
                }
                Box(Modifier.fillMaxWidth(rate / 100f).fillMaxHeight().background(color))
            }
        }
        Text(
            text = if (rate == null) stringResource(R.string.s06_no_record) else stringResource(R.string.common_percent, rate),
            fontSize = 12.sp,
            fontWeight = if (rate == null) FontWeight.Normal else FontWeight.Bold,
            color = if (rate == null) Faint else Navy
        )
    }
}

/**
 * 약 상태 배지: 쉬는 중(회색) / 종료(회색) / 복용 중(민트).
 * @param showActive false면 "복용 중"일 때 아무것도 그리지 않는다 (목록에서는 배지 없음이 곧 복용 중)
 */
@Composable
fun MedicationStatusPill(status: MedicationStatus, showActive: Boolean) {
    when (status) {
        MedicationStatus.ENDED -> Pill(stringResource(R.string.med_status_ended), PillKind.GRAY)
        MedicationStatus.PAUSED -> Pill(stringResource(R.string.med_status_paused), PillKind.ORANGE)
        MedicationStatus.ACTIVE -> if (showActive) Pill(stringResource(R.string.med_status_active), PillKind.MINT)
    }
}
