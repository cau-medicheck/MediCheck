package com.cau.medicheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.ScheduleItem
import com.cau.medicheck.ui.theme.ConditionTagBg
import com.cau.medicheck.ui.theme.ConditionTagText
import com.cau.medicheck.ui.theme.FieldBorder
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.RowSubText
import com.cau.medicheck.ui.theme.Teal

// 복용 조건(식전·식후·공복·취침 전)을 그리는 부품들. (2026-11-23, 시안: 04_화면캡처/홈카드_묶음기록_시안/복용조건_시안.html)

/** "08:00 식후" — 상관없음이면 "08:00"만. 약 목록(S06)·약 상세(S07)가 쓴다 */
@Composable
fun timeWithCondition(schedule: ScheduleItem): String =
    if (schedule.condition == DoseCondition.NONE) timeText(schedule.time)
    else timeText(schedule.time) + " " + conditionLabel(schedule.condition)

/** 조건의 화면 글자: 상관없음 / 식전 / 식후 / 공복 / 취침 전 */
@Composable
fun conditionLabel(condition: DoseCondition): String = stringResource(
    when (condition) {
        DoseCondition.NONE -> R.string.cond_none
        DoseCondition.BEFORE_MEAL -> R.string.cond_before_meal
        DoseCondition.AFTER_MEAL -> R.string.cond_after_meal
        DoseCondition.EMPTY_STOMACH -> R.string.cond_empty_stomach
        DoseCondition.BEDTIME -> R.string.cond_bedtime
    }
)

/**
 * 약 등록·수정 화면(S08)의 조건 버튼 한 줄: [상관없음] [식전] [식후] [공복] [취침 전]. 하나만 고른다.
 * TalkBack은 "08:00 복용 조건, 식후, 선택됨"처럼 읽는다(radio 버튼 묶음).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConditionChips(
    selected: DoseCondition,
    onSelect: (DoseCondition) -> Unit,
    timeLabel: String,
    modifier: Modifier = Modifier
) {
    val groupName = stringResource(R.string.s08_cd_condition, timeLabel)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.selectableGroup().semantics { contentDescription = groupName }
    ) {
        DoseCondition.entries.forEach { condition ->
            val on = condition == selected
            val shape = RoundedCornerShape(20.dp)
            Text(
                text = conditionLabel(condition),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (on) Color.White else Navy,
                modifier = Modifier
                    .clip(shape)
                    .background(if (on) Teal else Color.White)
                    .border(1.4.dp, if (on) Teal else FieldBorder, shape)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(condition) })
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
    }
}

/**
 * 목록에 붙는 작은 조건 꼬리표 [식후]. "상관없음"이면 아무것도 그리지 않는다.
 * @param onDark 짙은 바탕(홈 맨 위 카드) 위면 true — 흰 글자에 반투명 바탕
 */
@Composable
fun ConditionTag(condition: DoseCondition, modifier: Modifier = Modifier, onDark: Boolean = false) {
    if (condition == DoseCondition.NONE) return
    Text(
        text = conditionLabel(condition),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (onDark) Color.White else ConditionTagText,
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (onDark) Color.White.copy(alpha = 0.2f) else ConditionTagBg)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

/**
 * 회차 줄의 둘째 줄: "1 cap [식후] · 물 한 컵 이상"
 * 복용량 → 조건 꼬리표(상관없음이면 없음) → 메모(없으면 없음) 순서.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoseSubLine(row: DoseRowUi, modifier: Modifier = Modifier) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 4.dp)
    ) {
        // RowSub와 같은 글자 모양. RowSub는 위 여백을 스스로 붙여 한 줄 정렬이 어긋나므로 Text를 바로 쓴다.
        Text(row.dosage, fontSize = 11.sp, color = RowSubText, lineHeight = 16.sp)
        ConditionTag(row.condition)
        if (row.memo.isNotBlank()) Text("· " + row.memo, fontSize = 11.sp, color = RowSubText, lineHeight = 16.sp)
    }
}
