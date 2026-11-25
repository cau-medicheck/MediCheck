package com.cau.medicheck.ui.screens.dose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseDisplayStatus
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.DoseSlots
import com.cau.medicheck.ui.components.DoseRowUi
import com.cau.medicheck.ui.components.groupBySameTime
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.HeroDeepEnd
import com.cau.medicheck.ui.theme.HeroDeepStart
import com.cau.medicheck.ui.theme.HeroPastEnd
import com.cau.medicheck.ui.theme.HeroPastStart
import java.time.LocalTime
import kotlinx.coroutines.launch

/*
 * 홈 맨 위 카드: 같은 시각에 먹는 약 묶음을 보여 주고, 한 번에 또는 하나씩 기록한다.
 * (2026-11-23 결정, 시안: 04_화면캡처/홈카드_묶음기록_시안/종합_시안.html → 간소화: 04_화면캡처/홈카드_간소화_시안/ 간단 B)
 *
 *   ● 08:00  [12:30]  ● 21:00      ← 오늘의 시각들. 눌러서 이동, 옆으로 밀어도 이동
 *   12:30
 *   ○ Omega-3  1 cap               ← 약 이름·동그라미를 누르면 그 약만 복용 완료 ↔ 기록 전
 *   [ ✓ 복용 완료 ]                 ← 여러 개면 "✓ N개 모두 복용 완료"
 *   지금으로 ›                      ← 다른 시각으로 넘겼을 때만
 */

/** 같은 시각에 먹는 회차 묶음 */
private class DoseSlot(val time: LocalTime, val rows: List<DoseRowUi>) {
    /** 아직 기록 안 한 회차 (예정 + 미복용) */
    val pending: List<DoseRowUi> = rows.filter { it.display == DoseDisplayStatus.UPCOMING || it.display == DoseDisplayStatus.MISSED }
    val missedCount: Int = rows.count { it.display == DoseDisplayStatus.MISSED }
}

/** 카드의 버튼들이 부를 함수 */
class SlotCardActions(
    val onToggle: (DoseRowUi) -> Unit,
    val onTakeAll: (List<DoseKey>) -> Unit,
    val onReleaseAll: (List<DoseKey>) -> Unit
)

/**
 * 홈 맨 위 카드 전체. 묶음마다 한 장씩 옆으로 넘기는 카드(HorizontalPager)다.
 *
 * 어느 장을 보여 줄지:
 *  - 평소에는 DoseSlots.pick이 고른 장("처음 카드")을 따라간다.
 *  - 사용자가 넘기거나(밀기·‹ ›·시각 칩) 이 카드에서 기록하면 그 장에 머문다(hold).
 *    기록하자마자 다음 시각으로 넘어가 버리면 "다시 누르면 해제" 버튼을 볼 수 없기 때문이다.
 *  - 머무는 동안에는 "처음 카드가 어느 장인지"도 머물기 시작한 때의 값으로 굳혀 둔다.
 *    그러지 않으면 약 하나를 눌러 그 묶음이 다 기록되는 순간 처음 카드가 다른 시각으로 바뀌고, 지금 장이
 *    "지난 복약" 모양으로 다시 그려지며(미복용 줄이 사라짐) 약 줄이 위로 올라가 같은 곳을 다시 눌러 해제할 수 없다.
 *  - 머무는 것은 홈에 다시 들어올 때, 시각이 바뀌어 가장 가까운 시각이 달라질 때, "지금으로"를 누를 때 풀린다.
 */
@Composable
fun SlotHeroCard(state: DayDosesUiState, actions: SlotCardActions, modifier: Modifier = Modifier) {
    val slots = remember(state.rows) {
        groupBySameTime(state.rows).map { DoseSlot(it.first().time, it) }
    }
    val nowTime = state.now.toLocalTime()
    val times = slots.map { it.time }
    val liveDefault = DoseSlots.pick(times, slots.map { it.pending.isNotEmpty() }, nowTime)
    // 기록 여부와 상관없이 "시각만으로" 가장 가까운 묶음. 이 값이 바뀌면(시간이 흘러 한가운데를 넘으면) 머무르기를 푼다.
    val nearestByTime = DoseSlots.pick(times, times.map { true }, nowTime)

    var hold by remember { mutableStateOf(false) }
    // 머무르기 시작한 때의 "처음 카드" 번호. 머무는 동안 화면은 이 값을 처음 카드로 그린다.
    var heldDefault by remember { mutableStateOf<Int?>(null) }
    val startHold = { if (!hold) { hold = true; heldDefault = liveDefault } }
    val releaseHold = { hold = false; heldDefault = null }
    LifecycleResumeEffect(Unit) {
        releaseHold()   // 홈에 다시 들어오면 처음 카드로
        onPauseOrDispose { }
    }
    LaunchedEffect(nearestByTime) { releaseHold() }
    val defaultIdx = if (hold) heldDefault ?: liveDefault else liveDefault

    if (state.isLoading) {
        // 불러오는 동안 "예정된 복약이 없어요"가 잠깐 비치지 않도록 빈 카드만 보인다
        HeroShell(modifier) { Spacer(Modifier.height(180.dp)) }
        return
    }
    if (slots.isEmpty()) {
        HeroShell(modifier) { SummaryBody(stringResource(R.string.s04_next_none), tomorrow = null) }
        return
    }
    if (liveDefault == -1 && !hold) {
        val tomorrow = state.tomorrowFirst?.let { (time, count) -> stringResource(R.string.s04_tomorrow_first, timeText(time), count) }
        HeroShell(modifier) { SummaryBody(stringResource(R.string.s04_next_all_done), tomorrow) }
        return
    }

    val start = liveDefault.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = start) { slots.size }
    val scope = rememberCoroutineScope()
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged) { if (dragged) startHold() }
    // 머무르지 않을 때는 처음 카드를 따라간다
    LaunchedEffect(liveDefault, hold, slots.size) {
        if (!hold && liveDefault in slots.indices && pager.currentPage != liveDefault) pager.animateScrollToPage(liveDefault)
    }
    val goTo: (Int) -> Unit = { page ->
        if (page in slots.indices) {
            startHold()
            scope.launch { pager.animateScrollToPage(page) }
        }
    }

    HorizontalPager(
        state = pager,
        pageSpacing = 12.dp,
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth()
    ) { page ->
        SlotPage(
            state = state,
            slots = slots,
            index = page,
            defaultIdx = defaultIdx,
            onGo = goTo,
            onBackToNow = {
                releaseHold()
                if (liveDefault in slots.indices) scope.launch { pager.animateScrollToPage(liveDefault) }
            },
            actions = SlotCardActions(
                onToggle = { startHold(); actions.onToggle(it) },
                onTakeAll = { startHold(); actions.onTakeAll(it) },
                onReleaseAll = { startHold(); actions.onReleaseAll(it) }
            )
        )
    }
}

/** 짙은 청록 바탕. past면 조금 어둡게(지금이 아닌 지난 시각) */
@Composable
private fun HeroShell(modifier: Modifier = Modifier, past: Boolean = false, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(if (past) listOf(HeroPastStart, HeroPastEnd) else listOf(HeroDeepStart, HeroDeepEnd)))
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)
    ) { content() }
}

/**
 * 오른쪽에 알약 그림을 깐 구역. 그림이 위쪽의 시각 칩·미복용 줄을 가리지 않도록 "시각 + 약 목록" 구역 안에만 둔다.
 * 장식 그림이라 TalkBack이 읽지 않는다 (만든 방법: docs/art/render_pills.py)
 */
@Composable
private fun WithPillArt(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.home_pills),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 30.dp, y = (-6).dp).size(140.dp)
        )
        Column { content() }
    }
}

/** 오늘 회차가 없거나 모두 기록했을 때: 큰 안내 / (내일 첫 복약) */
@Composable
private fun SummaryBody(message: String, tomorrow: String?) {
    WithPillArt {
        Text(
            text = message,
            fontSize = 19.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (tomorrow != null) {
            Text(tomorrow, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(36.dp))   // 그림이 들어갈 높이
    }
}

/**
 * 카드 한 장 = 시각 묶음 하나. (2026-11-23 간단 B: 날짜·화살표·미복용 줄·"지금 먹을 약"·시간대·꼬리표·진행률을 뺐다 —
 * 시각 칩의 점, 약 앞 동그라미 색, 아래 "하루 복약 일정" 카드가 같은 정보를 보여 준다)
 *
 *   ● 08:00  [12:30]  ● 21:00      ← 시각 칩(눌러서 이동). 점: 미복용 있음 = 주황, 모두 기록 = 민트
 *   12:30                          ← 큰 시각
 *   ○ Omega-3  1 cap               ← 눌러서 복용 완료 ↔ 기록 전 (동그라미 색 = 상태)
 *   [ ✓ 복용 완료 ]
 *   지금으로 ›                      ← 다른 시각으로 넘겼을 때만
 */
@Composable
private fun SlotPage(
    state: DayDosesUiState,
    slots: List<DoseSlot>,
    index: Int,
    defaultIdx: Int,
    onGo: (Int) -> Unit,
    onBackToNow: () -> Unit,
    actions: SlotCardActions
) {
    val slot = slots[index]
    val isDefault = index == defaultIdx
    val isPast = !slot.time.isAfter(state.now.toLocalTime())

    HeroShell(past = isPast && !isDefault) {
        SlotChips(slots, index, onGo)

        WithPillArt {
            Text(
                text = timeText(slot.time),
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                color = Color.White,
                modifier = Modifier.padding(top = 10.dp).semantics { heading() }
            )
            // 약 줄들: 그림과 겹치지 않게 폭을 조금 줄인다
            Column(Modifier.fillMaxWidth(0.74f).padding(top = 4.dp, bottom = 12.dp)) {
                slot.rows.forEach { row -> SlotMedRow(row, onToggle = { actions.onToggle(row) }) }
            }
        }

        SlotMainButton(slot, state.takenByButton, actions)

        if (!isDefault && defaultIdx != -1) {
            Text(
                text = stringResource(R.string.s04_back_now),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(role = Role.Button, onClick = onBackToNow)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/** ● 08:00  [12:30]  ● 21:00 — 오늘의 시각 칩. 눌러서 그 시각으로 간다(화살표 대신). 점: 모두 기록 = 민트, 미복용 있음 = 주황, 그 밖 = 흐린 흰색 */
@Composable
private fun SlotChips(slots: List<DoseSlot>, index: Int, onGo: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .offset(x = (-8).dp)   // 칩 안쪽 여백만큼 당겨 아래 글자와 왼쪽 줄을 맞춘다
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
    ) {
        slots.forEachIndexed { i, slot ->
            val selected = i == index
            val dot = when {
                slot.pending.isEmpty() -> Color(0xFF7FE0CF)
                slot.missedCount > 0 -> Color(0xFFFFB866)
                else -> (if (selected) Color(0xFF0F6E7A) else Color.White).copy(alpha = 0.6f)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onGo(i) })
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
                Text(
                    text = timeText(slot.time),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color(0xFF0F6E7A) else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(start = 5.dp)
                )
            }
        }
    }
}

/**
 * 약 한 줄: ○ 이름 1 cap. 줄 전체가 누르는 곳이다(체크칸처럼 켜고 끈다).
 * 복용 완료 = 흰 동그라미 안 체크 + 흐리게·취소선, 건너뜀 = 파란 고리 + 흐리게, 미복용 = 주황 고리.
 */
@Composable
private fun SlotMedRow(row: DoseRowUi, onToggle: () -> Unit) {
    val taken = row.display == DoseDisplayStatus.TAKEN
    val skipped = row.display == DoseDisplayStatus.SKIPPED
    val missed = row.display == DoseDisplayStatus.MISSED
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .toggleable(value = taken, role = Role.Checkbox, onValueChange = { onToggle() })
            .heightIn(min = 44.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .alpha(if (taken || skipped) 0.6f else 1f)
    ) {
        CheckMark(taken = taken, ring = if (missed) Color(0xFFFFC98A) else if (skipped) Color(0xFFB8CCF0) else Color.White.copy(alpha = 0.8f))
        Text(
            text = row.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textDecoration = if (taken || skipped) TextDecoration.LineThrough else null,
            modifier = Modifier.padding(start = 10.dp)
        )
        Text(
            text = row.dosage,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 7.dp)
        )
    }
}

/** 동그라미 체크 표시. 장식이라 TalkBack은 줄의 체크 상태(toggleable)로 읽는다. */
@Composable
private fun CheckMark(taken: Boolean, ring: Color) {
    Canvas(Modifier.size(20.dp).clearAndSetSemantics { }) {
        val r = size.minDimension / 2
        val stroke = 1.6.dp.toPx()
        if (taken) {
            drawCircle(Color.White, radius = r)
            val c = Color(0xFF0F6E7A)
            drawLine(c, Offset(size.width * 0.28f, size.height * 0.52f), Offset(size.width * 0.44f, size.height * 0.68f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(c, Offset(size.width * 0.44f, size.height * 0.68f), Offset(size.width * 0.73f, size.height * 0.36f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
        } else {
            drawCircle(ring, radius = r - stroke / 2, style = Stroke(stroke))
        }
    }
}

/**
 * 큰 버튼 하나. 모양 세 가지:
 *  - 기록 안 한 약이 있으면  → "✓ N개 모두 복용 완료" (하나면 "✓ 복용 완료")
 *  - 모두 먹었고 버튼으로 기록한 약이 있으면 → "✓ 모두 복용함 · 다시 누르면 N개 해제"
 *  - 그 밖(모두 기록했지만 버튼으로 한 것은 없음) → "모두 기록했어요 · 고치려면 약을 누르세요" (누를 수 없음)
 */
@Composable
private fun SlotMainButton(slot: DoseSlot, takenByButton: Set<DoseKey>, actions: SlotCardActions) {
    val byButton = slot.rows.map { it.key }.filter { it in takenByButton }
    val allTaken = slot.rows.all { it.display == DoseDisplayStatus.TAKEN }
    val shape = RoundedCornerShape(12.dp)
    when {
        slot.pending.isNotEmpty() -> {
            val n = slot.pending.size
            HeroButton(
                title = if (n > 1) stringResource(R.string.s04_btn_take_all, n) else stringResource(R.string.s04_btn_take_one),
                sub = null,
                modifier = Modifier.clip(shape).background(Color.White).clickable(role = Role.Button) { actions.onTakeAll(slot.pending.map { it.key }) },
                color = Color(0xFF0F6E7A)
            )
        }
        allTaken && byButton.isNotEmpty() -> HeroButton(
            title = stringResource(R.string.s04_btn_release),
            sub = stringResource(R.string.s04_btn_release_sub, byButton.size),
            modifier = Modifier
                .clip(shape)
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.4.dp, Color(0xFF7FE0CF), shape)
                .clickable(role = Role.Button) { actions.onReleaseAll(byButton) },
            color = Color.White
        )
        else -> HeroButton(
            title = stringResource(R.string.s04_btn_all_recorded),
            sub = stringResource(R.string.s04_btn_all_recorded_sub),
            modifier = Modifier.clip(shape).background(Color.White.copy(alpha = 0.14f)),
            color = Color.White
        )
    }
}

@Composable
private fun HeroButton(title: String, sub: String?, modifier: Modifier, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 11.dp).semantics(mergeDescendants = true) { }
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        if (sub != null) Text(sub, fontSize = 11.sp, color = color.copy(alpha = 0.8f), modifier = Modifier.padding(top = 2.dp))
    }
}
