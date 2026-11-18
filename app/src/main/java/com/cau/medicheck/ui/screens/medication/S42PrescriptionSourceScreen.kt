package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.Navy

/**
 * S42 처방 출처·진료과 (v3 prescriptionSource()).
 *
 * 어느 병원·진료과에서 받은 약인지 개인 메모를 남긴다. 의료기관·진료과는 약 상세(S07)의 표에도 보이고,
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
        ScreenIntro(stringResource(R.string.s42_subtitle), Modifier.padding(vertical = 16.dp))

        SurfaceCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.medName ?: stringResource(R.string.common_selected_med),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Pill(stringResource(R.string.s42_badge_optional), PillKind.MINT)
            }

            val next = KeyboardOptions(imeAction = ImeAction.Next)
            FieldLabel(stringResource(R.string.s42_label_hospital))
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
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Notice(stringResource(R.string.s42_notice), Modifier.padding(top = 16.dp))
    }
}
