package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.ActionPair
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.HeroHeader
import com.cau.medicheck.ui.components.McMedIcon
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/**
 * S40 중복 약·회차 확인 (v3 duplicateScreen()). 2026-11-25 다시 그림:
 * 가운데 주황 동그라미 + "같은 이름의 약이 있어요" + 이미 있는 약 카드 + 아래 [다시 확인 | 그대로 등록].
 *
 * 새 약을 등록하는데 같은 이름의 약이 이미 있으면 여기로 온다. 이름만 비교한다.
 * 이 화면도 S08의 ViewModel을 함께 쓴다.
 */
@Composable
fun S40DuplicateScreen(
    state: MedicationEditUiState,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onRecheck: () -> Unit
) {
    val pending = state.pendingNew
    val meds = pending?.sameNameMeds.orEmpty()

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s40)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        HeroHeader(
            icon = Icons.Rounded.ContentCopy,
            color = Color(0xFFE08A2E),
            title = stringResource(R.string.s40_heading),
            subtitle = pending?.let { stringResource(R.string.s40_sub, it.name, meds.size) }
        )

        // 이미 있는 같은 이름의 약들
        if (meds.isNotEmpty()) {
            SurfaceCard(padding = 0.dp) {
                Column(Modifier.padding(horizontal = 15.dp)) {
                    meds.forEachIndexed { index, med ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                            McMedIcon(med.shape, med.color, size = 40.dp)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(med.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Navy)
                                Text(
                                    med.schedules.joinToString(" · ") { timeText(it.time) } + " · " + med.dosage,
                                    fontSize = 12.5.sp,
                                    color = Muted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        if (index != meds.lastIndex) HorizontalDivider(color = Line, modifier = Modifier.padding(start = 52.dp))
                    }
                }
            }
        }

        ActionPair(
            secondaryText = stringResource(R.string.s40_btn_recheck),
            onSecondary = onRecheck,
            primaryText = stringResource(R.string.s40_btn_confirm),
            onPrimary = onConfirm,
            loading = state.isSaving,
            modifier = Modifier.padding(top = 26.dp)
        )
        Text(
            stringResource(R.string.s40_notice),
            fontSize = 12.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
    }
}
