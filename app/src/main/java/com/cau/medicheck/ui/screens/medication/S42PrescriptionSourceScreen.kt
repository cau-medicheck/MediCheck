package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McMedIcon
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.toColor
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/**
 * S42 처방 출처·진료과 (v3 prescriptionSource()). 2026-11-25 다시 그림:
 * 위에 어떤 약인지(약 색 띠) + 입력 칸 셋(제목 + 칸, 카드 없이) + 저장.
 *
 * 어느 병원·진료과에서 받은 약인지 개인 메모를 남긴다. 의료기관·진료과는 약 상세(S07)의 칸에도 보이고,
 * 추가 메모는 이 화면에서만 보인다. 일정과 상관없는 정보라 복용 기록이나 알림은 바뀌지 않는다.
 */
@Composable
fun S42PrescriptionSourceScreen(
    state: PrescriptionSourceUiState,
    onBack: () -> Unit,
    onHospitalChange: (String) -> Unit,
    onDepartmentChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onSave: () -> Unit
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s42)) {
        if (state.isLoading) return@McScreenColumn

        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenIntro(modifier = Modifier.padding(top = 16.dp, bottom = 14.dp))

        // 어떤 약인지: 약 색으로 옅게 칠한 띠
        val c = (state.color ?: com.cau.medicheck.domain.MedicationColor.TEAL).toColor()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(c.copy(alpha = 0.14f), c.copy(alpha = 0.04f))))
                .padding(14.dp)
        ) {
            McMedIcon(
                state.shape ?: com.cau.medicheck.domain.MedicationShape.TABLET,
                state.color ?: com.cau.medicheck.domain.MedicationColor.TEAL,
                size = 42.dp
            )
            Column(Modifier.padding(start = 12.dp)) {
                Text(state.medName ?: stringResource(R.string.common_selected_med), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                Text(stringResource(R.string.s42_sub), fontSize = 12.5.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
            }
        }

        val next = KeyboardOptions(imeAction = ImeAction.Next)
        FieldLabel(stringResource(R.string.s42_label_hospital), Modifier.padding(top = 8.dp))
        FormField(
            value = state.hospital,
            onValueChange = onHospitalChange,
            placeholder = stringResource(R.string.s42_hint_hospital),
            label = stringResource(R.string.s42_label_hospital),
            keyboardOptions = next
        )
        FieldLabel(stringResource(R.string.s42_label_department))
        FormField(
            value = state.department,
            onValueChange = onDepartmentChange,
            placeholder = stringResource(R.string.s42_hint_department),
            label = stringResource(R.string.s42_label_department),
            keyboardOptions = next
        )
        FieldLabel(stringResource(R.string.s42_label_memo))
        FormField(
            value = state.memo,
            onValueChange = onMemoChange,
            placeholder = stringResource(R.string.s42_hint_memo),
            label = stringResource(R.string.s42_label_memo),
            singleLine = false,
            minLines = 3
        )
        McButton(
            text = stringResource(R.string.s42_btn_save),
            onClick = onSave,
            full = true,
            loading = state.isSaving,
            modifier = Modifier.padding(top = 22.dp)
        )
    }
}
