package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.MedicationColor
import com.cau.medicheck.domain.MedicationShape
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.ColorPicker
import com.cau.medicheck.ui.components.ConditionChips
import com.cau.medicheck.ui.components.DatePickerPopup
import com.cau.medicheck.ui.components.DayToggleRow
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.LinkText
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.McSwitch
import com.cau.medicheck.ui.components.MedShapeArt
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.PickerField
import com.cau.medicheck.ui.components.SettingRow
import com.cau.medicheck.ui.components.ShapePicker
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.TimePickerPopup
import com.cau.medicheck.ui.components.conditionLabel
import com.cau.medicheck.ui.components.daysText
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.components.toColor
import com.cau.medicheck.ui.theme.CardBackground
import com.cau.medicheck.ui.theme.CardBorder
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** 약 등록/수정 화면의 입력·버튼이 부를 함수 묶음 */
class MedicationFormActions(
    val onBackToList: () -> Unit,
    val onNameChange: (String) -> Unit,
    val onDosageChange: (String) -> Unit,
    val onMemoChange: (String) -> Unit,
    val onStartActiveChange: (Boolean) -> Unit,
    val onShapeChange: (MedicationShape) -> Unit,
    val onColorChange: (MedicationColor) -> Unit,
    val onTimeChange: (key: Int, LocalTime) -> Unit,
    val onConditionChange: (key: Int, DoseCondition) -> Unit,
    val onRemoveTime: (key: Int) -> Unit,
    val onAddTime: () -> Unit,
    val onDayToggle: (DayOfWeek) -> Unit,
    val onStartDateChange: (LocalDate) -> Unit,
    val onEndDateChange: (LocalDate?) -> Unit,
    val onSubmit: () -> Unit
)

/** 지금 열려 있는 날짜 고르기 창이 어느 칸의 것인지 */
private enum class DateTarget { START, END }

/**
 * S08 약 등록/수정 (v3 form()).
 *
 * 등록과 수정이 같은 화면을 쓴다(state.isNew로 구분). 복용 시간은 여러 줄을 둘 수 있고, 반복 요일은 한 묶음이다.
 * 제출하면 ViewModel이 검사해서, 걸리면 토스트로 알려 주고, 통과하면 저장하거나 다음 화면(S33·S40)으로 넘긴다.
 */
@Composable
fun S08MedicationFormScreen(state: MedicationEditUiState, actions: MedicationFormActions) {
    // 지금 열려 있는 창. null이면 닫혀 있다.
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }
    var timePickerKey by remember { mutableStateOf<Int?>(null) }

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s08)) {
        if (state.isLoading) return@McScreenColumn
        // 수정하려는 약이 없으면(삭제됨·남의 약) 폼 대신 안내를 보여 준다.
        if (state.notFound) {
            MedicationNotFound(actions.onBackToList)
            return@McScreenColumn
        }

        BackLink(stringResource(R.string.s08_back), actions.onBackToList)
        // 맨 위 미리보기: 약 상세와 같은 약 색 머리. 이름·모양·색·복용량·시간을 고르면 바로 바뀐다
        FormPreview(state)

        val next = KeyboardOptions(imeAction = ImeAction.Next)   // 키보드의 "다음" 키
        FormCard(stringResource(R.string.s08_card_basic)) {
        FieldLabel(stringResource(R.string.s08_label_name))
        FormField(
            value = state.name,
            onValueChange = actions.onNameChange,
            placeholder = stringResource(R.string.s08_hint_name),
            label = stringResource(R.string.s08_label_name),
            keyboardOptions = next
        )
        FieldLabel(stringResource(R.string.s08_label_dosage))
        FormField(
            value = state.dosage,
            onValueChange = actions.onDosageChange,
            placeholder = stringResource(R.string.s08_hint_dosage),
            label = stringResource(R.string.s08_label_dosage)
        )
        }

        // 아이콘 모양·색: 내 약 목록과 약 상세에 이 모양으로 보인다 (2026-11-24)
        FormCard(stringResource(R.string.s08_label_look)) {
            ShapePicker(state.shape, state.color, actions.onShapeChange)
            ColorPicker(state.color, actions.onColorChange, Modifier.padding(top = 12.dp))
        }

        // ----- 복용 시간: 줄마다 [08:00] [삭제] + 복용 조건 버튼들. 한 줄만 남으면 "삭제"가 눌리지 않는다 -----
        FormCard(stringResource(R.string.s08_label_times)) {
        state.times.forEachIndexed { index, row ->
            val timeName = stringResource(R.string.s08_cd_time, index + 1)
            val removeName = stringResource(R.string.s08_cd_time_remove, index + 1)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp)
            ) {
                PickerField(
                    text = timeText(row.time),
                    onClick = { timePickerKey = row.key },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = timeName + ", " + timeText(row.time) }
                )
                Spacer(Modifier.width(7.dp))
                McButton(
                    text = stringResource(R.string.action_delete),
                    onClick = { actions.onRemoveTime(row.key) },
                    kind = ButtonKind.WHITE,
                    small = true,
                    enabled = state.times.size > 1,
                    modifier = Modifier.semantics { contentDescription = removeName }
                )
            }
            // 이 시간의 복용 조건: [상관없음] [식전] [식후] [공복] [취침 전] 중 하나
            ConditionChips(
                selected = row.condition,
                onSelect = { actions.onConditionChange(row.key, it) },
                timeLabel = timeText(row.time),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        LinkText(stringResource(R.string.s08_link_add_time), actions.onAddTime, Modifier.padding(top = 10.dp))
        }

        // ----- 반복 요일 (모든 복용 시간에 똑같이 적용된다) + 기간 -----
        FormCard(stringResource(R.string.s08_card_days)) {
        DayToggleRow(selected = state.days, onToggle = actions.onDayToggle)
        if (state.isMixedDays) {
            Notice(stringResource(R.string.s08_notice_mixed_days), Modifier.padding(top = 8.dp), warn = true)
        }

        FieldLabel(stringResource(R.string.s08_label_start))
        PickerField(text = state.startDate.toString(), onClick = { dateTarget = DateTarget.START })

        FieldLabel(stringResource(R.string.s08_label_end))
        PickerField(
            text = state.endDate?.toString() ?: stringResource(R.string.common_not_set),
            onClick = { dateTarget = DateTarget.END }
        )
        if (state.endDate != null) {
            LinkText(
                stringResource(R.string.action_clear_end_date),
                { actions.onEndDateChange(null) },
                Modifier.padding(top = 6.dp)
            )
        }

        }

        FormCard(stringResource(R.string.s08_label_memo)) {
        FormField(
            value = state.memo,
            onValueChange = actions.onMemoChange,
            placeholder = stringResource(R.string.s08_hint_memo),
            label = stringResource(R.string.s08_label_memo),
            singleLine = false,
            minLines = 2
        )
        }

        // 등록할 때만: "바로 복용 시작" — 끄고 등록하면 쉬는 중(일정에 넣지 않고 등록만. 내 약 목록에서 켜면 그날부터 일정에)
        if (state.isNew) {
            SurfaceCard(Modifier.padding(top = 12.dp), padding = 0.dp) {
                SettingRow(
                    title = stringResource(R.string.s08_start_now),
                    description = stringResource(R.string.s08_start_now_desc),
                    // 줄 전체가 스위치다(onClick을 쓰면 '›'가 붙어 다른 화면으로 가는 것처럼 보인다)
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .toggleable(value = state.startActive, role = Role.Switch, onValueChange = actions.onStartActiveChange),
                    showDivider = false
                ) {
                    // 스위치는 보여 주기만(안쪽 누름이 줄의 누름을 가로채지 않게 — 3단계 메모)
                    McSwitch(checked = state.startActive, onCheckedChange = null)
                }
            }
        }

        McButton(
            text = stringResource(if (state.isNew) R.string.s08_btn_register else R.string.s08_btn_save),
            onClick = actions.onSubmit,
            full = true,
            loading = state.isSaving,
            modifier = Modifier.padding(top = 18.dp)
        )
    }

    // ----- 고르기 창 -----
    timePickerKey?.let { key ->
        val row = state.times.find { it.key == key }
        if (row != null) {
            TimePickerPopup(
                initialTime = row.time,
                onDismiss = { timePickerKey = null },
                onTimePicked = { time ->
                    actions.onTimeChange(key, time)
                    timePickerKey = null
                }
            )
        }
    }
    dateTarget?.let { target ->
        DatePickerPopup(
            initialDate = if (target == DateTarget.START) state.startDate else state.endDate ?: state.startDate,
            onDismiss = { dateTarget = null },
            onDatePicked = { date ->
                if (target == DateTarget.START) actions.onStartDateChange(date) else actions.onEndDateChange(date)
                dateTarget = null
            }
        )
    }
}

/** 입력 묶음 카드: 위에 작은 제목, 안에 입력 칸들 */
@Composable
private fun FormCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    SurfaceCard(Modifier.padding(top = 12.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier.padding(bottom = 2.dp).semantics { heading() }
        )
        content()
    }
}

/** 맨 위 미리보기: 약 색 머리 + 아이콘 + 이름 + "1 cap · 매일 12:30 식후" */
@Composable
private fun FormPreview(state: MedicationEditUiState) {
    val color = state.color.toColor()
    val shape = RoundedCornerShape(20.dp)
    val days = daysText(state.days, stringResource(R.string.common_every_day), stringArrayResource(R.array.common_weekdays).toList())
    val times = state.times.sortedBy { it.time }.map { row ->
        timeText(row.time) + if (row.condition == DoseCondition.NONE) "" else " " + conditionLabel(row.condition)
    }.joinToString(" · ")
    val summary = listOf(state.dosage.ifBlank { null }, if (state.days.isEmpty()) null else "$days $times").filterNotNull().joinToString(" · ")
    Box(
        modifier = Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(CardBackground)
            .border(1.dp, CardBorder, shape)
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0.05f))))
    ) {
        MedShapeArt(state.shape, state.color, Modifier.align(Alignment.TopEnd).offset(x = 14.dp, y = (-14).dp).size(100.dp).alpha(0.18f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .shadow(3.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) { MedShapeArt(state.shape, state.color, Modifier.size(32.dp)) }
            Column(Modifier.padding(start = 13.dp)) {
                Text(
                    text = state.name.ifBlank { stringResource(if (state.isNew) R.string.s08_title_new else R.string.s08_title_edit) },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.name.isBlank()) Muted else Navy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                if (summary.isNotBlank()) Text(summary, fontSize = 12.5.sp, color = ParagraphText, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}
