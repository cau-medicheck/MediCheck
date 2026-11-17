package com.cau.medicheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.ui.theme.DayBtnBorder
import com.cau.medicheck.ui.theme.DayBtnText
import com.cau.medicheck.ui.theme.FieldBorder
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.SettingDescText
import com.cau.medicheck.ui.theme.Teal
import java.time.DayOfWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cau.medicheck.R
import java.time.LocalTime

/**
 * 시각을 고르는 창 (24시간제). 약 등록 화면과 실제 복용 시각 기록(S31)이 함께 쓴다.
 *
 * @param initialTime 창이 열릴 때 맞춰져 있을 시각
 * @param onTimePicked "확인"을 눌렀을 때 고른 시각을 넘겨받는 함수
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerPopup(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onTimePicked: (LocalTime) -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onTimePicked(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        text = { TimePicker(state = pickerState) }
    )
}

/**
 * 제목 + 설명 + 오른쪽 스위치 한 줄. 줄 어디를 눌러도 켜고 끈다. (v3 .settingrow + .switch)
 * @param enabled false면 눌리지 않는다 (처리 중일 때 두 번 눌림을 막는다)
 */
@Composable
fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            // toggleable: 줄 전체가 스위치 하나로 동작한다. TalkBack은 "일시중지, 스위치, 꺼짐"처럼 읽는다.
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(
                text = description,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = SettingDescText,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        // 스위치는 그림으로만 쓴다(null = 터치를 받지 않음). 누르는 처리는 줄 전체가 한다.
        McSwitch(checked = checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { })
    }
}

/**
 * 라디오 단추 + 제목 + 설명 한 줄. 줄 어디를 눌러도 선택된다. (S33 적용 범위)
 * 라디오 = 여러 개 중 하나만 고르는 동그란 단추.
 */
@Composable
fun RadioRow(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 10.dp)
    ) {
        // 바깥 동그라미 + (선택됐으면) 안쪽 점
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(18.dp)
                .border(2.dp, if (selected) Teal else FieldBorder, CircleShape)
        ) {
            if (selected) Box(Modifier.size(9.dp).background(Teal, CircleShape))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(
                text = description,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = SettingDescText,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * 요일 버튼 7개: 일 월 화 수 목 금 토. (v3 .day-buttons: 지름 39dp 동그라미)
 * 고른 요일은 청록 바탕·흰 글자. 누를 때마다 켜지고 꺼진다.
 * 글자를 크게 써서 한 줄에 다 안 들어가면 다음 줄로 넘어간다(FlowRow).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayToggleRow(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit, modifier: Modifier = Modifier) {
    val labels = stringArrayResource(R.array.common_weekdays)        // "일"…"토"
    val spoken = stringArrayResource(R.array.common_weekdays_full)   // "일요일"…"토요일" (TalkBack용)
    FlowRow(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        SUNDAY_FIRST.forEachIndexed { index, day ->
            val on = day in selected
            val name = spoken[index]
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .defaultMinSize(minWidth = 39.dp, minHeight = 39.dp)
                    .clip(CircleShape)
                    .background(if (on) Teal else Color.White)
                    .border(1.dp, if (on) Teal else DayBtnBorder, CircleShape)
                    .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle(day) })
                    .semantics { contentDescription = name }
            ) {
                Text(
                    text = labels[index],
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (on) Color.White else DayBtnText,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }
        }
    }
}
