package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.ui.components.ConditionChips
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.DatePickerPopup
import com.cau.medicheck.ui.components.DayToggleRow
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.LinkText
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McSwitch
import com.cau.medicheck.ui.components.SettingRow
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.PickerField
import com.cau.medicheck.ui.components.ScreenTitle
import com.cau.medicheck.ui.components.TimePickerPopup
import com.cau.medicheck.ui.components.timeText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** 약 등록/수정 화면의 입력·버튼이 부를 함수 묶음 */
class MedicationFormActions(
    val onBackToList: () -> Unit,
    val onOpenPhoto: () -> Unit,
    val onNameChange: (String) -> Unit,
    val onDosageChange: (String) -> Unit,
    val onMemoChange: (String) -> Unit,
    val onStartActiveChange: (Boolean) -> Unit,
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
        ScreenTitle(
            title = stringResource(if (state.isNew) R.string.s08_title_new else R.string.s08_title_edit),
            subtitle = stringResource(R.string.s08_subtitle),
            modifier = Modifier.padding(top = 16.dp)
        )
        // 사진으로 등록하는 기능은 시안(S29)만 있다. 등록할 때만 길을 열어 둔다.
        if (state.isNew) {
            LinkText(stringResource(R.string.s08_link_photo), actions.onOpenPhoto, Modifier.padding(top = 6.dp))
        }

        val next = KeyboardOptions(imeAction = ImeAction.Next)   // 키보드의 "다음" 키
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

        // ----- 복용 시간: 줄마다 [08:00] [삭제] + 복용 조건 버튼들. 한 줄만 남으면 "삭제"가 눌리지 않는다 -----
        FieldLabel(stringResource(R.string.s08_label_times))
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
        LinkText(stringResource(R.string.s08_link_add_time), actions.onAddTime, Modifier.padding(top = 8.dp))

        // ----- 반복 요일 (모든 복용 시간에 똑같이 적용된다) -----
        FieldLabel(stringResource(R.string.s08_label_days))
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

        FieldLabel(stringResource(R.string.s08_label_memo))
        FormField(
            value = state.memo,
            onValueChange = actions.onMemoChange,
            placeholder = stringResource(R.string.s08_hint_memo),
            label = stringResource(R.string.s08_label_memo),
            singleLine = false,
            minLines = 2
        )

        // 등록할 때만: "바로 복용 시작" — 끄고 등록하면 쉬는 중(일정에 넣지 않고 등록만. 내 약 목록에서 켜면 그날부터 일정에)
        if (state.isNew) {
            SettingRow(
                title = stringResource(R.string.s08_start_now),
                description = stringResource(R.string.s08_start_now_desc),
                // 줄 전체가 스위치다(onClick을 쓰면 '›'가 붙어 다른 화면으로 가는 것처럼 보인다)
                modifier = Modifier
                    .padding(top = 10.dp)
                    .toggleable(value = state.startActive, role = Role.Switch, onValueChange = actions.onStartActiveChange),
                showDivider = false
            ) {
                // 스위치는 보여 주기만(안쪽 누름이 줄의 누름을 가로채지 않게 — 3단계 메모)
                McSwitch(checked = state.startActive, onCheckedChange = null)
            }
        }

        McButton(
            text = stringResource(if (state.isNew) R.string.s08_btn_register else R.string.s08_btn_save),
            onClick = actions.onSubmit,
            full = true,
            loading = state.isSaving,
            modifier = Modifier.padding(top = 22.dp)
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
