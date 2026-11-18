package com.cau.medicheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.ui.theme.DayBtnBorder
import com.cau.medicheck.ui.theme.DayBtnText
import com.cau.medicheck.ui.theme.FieldBorder
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.SettingDescText
import com.cau.medicheck.ui.theme.SettingDivider
import com.cau.medicheck.ui.theme.Teal
import java.time.DayOfWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.cau.medicheck.R
import com.cau.medicheck.ui.lock.LocalAppLocked
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
    // 앱이 잠겨 있는 동안에는 그리지 않는다 (따로 뜨는 창이라 잠금 화면 위에 보일 수 있다). 풀면 다시 보인다.
    if (LocalAppLocked.current) return
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
    enabled: Boolean = true,
    /** 줄 위아래 안쪽 여백. 이 여백까지 눌리는 영역에 들어간다 (48dp 이상의 터치 영역을 만들 때 쓴다). */
    verticalPadding: Dp = 0.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            // toggleable: 줄 전체가 스위치 하나로 동작한다. TalkBack은 "일시중지, 스위치, 꺼짐"처럼 읽는다.
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = verticalPadding)
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
    modifier: Modifier = Modifier,
    /** 설명 아래에 덧붙일 한 줄 (예: "등록한 시간대: Asia/Seoul (UTC+09:00)") */
    extra: String? = null
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
            if (extra != null) {
                Text(text = extra, fontSize = 11.sp, lineHeight = 16.sp, color = Teal, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

/**
 * 설정 화면의 한 줄: 제목 + 설명 + 오른쪽에 붙일 것(배지 등). (v3 .settingrow)
 * @param onClick 있으면 줄 전체를 누를 수 있고, 오른쪽 끝에 "›"를 그린다
 */
@Composable
fun SettingRow(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    trailing: @Composable () -> Unit = {}
) {
    Column(modifier.then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 13.dp)) {
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
            Spacer(Modifier.width(12.dp))
            trailing()
            if (onClick != null) {
                // "›"는 장식이라 TalkBack이 읽지 않게 한다.
                Text("›", fontSize = 16.sp, color = SettingDescText, modifier = Modifier.padding(start = 6.dp).clearAndSetSemantics { })
            }
        }
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = SettingDivider)
    }
}

/**
 * 몇 개 중 하나를 고르는 칸. 누르면 아래로 목록이 펼쳐진다. (v3 select)
 * @param options 고를 수 있는 값들
 * @param label 값 → 화면에 보일 글자
 */
@Composable
fun <T> McSelectField(
    selected: T,
    options: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        PickerField(text = label(selected), onClick = { expanded = true }, trailing = "▾")
        DropdownMenu(expanded = expanded && !LocalAppLocked.current, onDismissRequest = { expanded = false }) {
            // 펼친 목록은 따로 뜨는 창이라 본문의 글자 배율이 전해지지 않는다. 여기서 한 번 더 적용한다.
            ProvideBodyFontScale {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(label(option), fontSize = 13.sp, color = Navy) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        }
                    )
                }
            }
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
