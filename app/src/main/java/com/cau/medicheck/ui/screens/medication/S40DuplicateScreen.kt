package com.cau.medicheck.ui.screens.medication

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.WarnBadge
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.ListRow
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.RowSub
import com.cau.medicheck.ui.components.RowTitle
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.timeText
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/**
 * S40 중복 약·회차 확인 (v3 duplicateScreen()).
 *
 * 새 약을 등록하는데 같은 이름의 약이 이미 있으면 여기로 온다. 이름이 같은 약들을 보여 주고,
 * "그대로 등록"하거나 "다시 확인"(폼으로 돌아가기)을 고르게 한다. 이름만 비교한다.
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

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s40)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenIntro(modifier = Modifier.padding(vertical = 16.dp))

        SurfaceCard {
            WarnBadge()
            Text(
                text = stringResource(R.string.s40_heading),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Navy,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            if (pending != null) {
                // "등록하려는 {이름}과 이름이 같은 약이 N개 있어요." — 이름만 굵게
                val head = stringResource(R.string.s40_body_head)
                val tail = stringResource(R.string.s40_body_tail, pending.sameNameMeds.size)
                Text(
                    text = buildAnnotatedString {
                        append(head)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.name) }
                        append(tail)
                    },
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = Muted
                )
            } else {
                Text(stringResource(R.string.s40_body_example), fontSize = 12.sp, lineHeight = 19.sp, color = Muted)
            }

            // 이름이 같은 약들: "08:00 / 20:00 · 1정"
            val meds = pending?.sameNameMeds.orEmpty()
            meds.forEachIndexed { index, med ->
                ListRow(
                    leading = { Avatar(Glyph.DOSE) },
                    showDivider = index != meds.lastIndex,
                    modifier = Modifier.padding(top = if (index == 0) 6.dp else 0.dp)
                ) {
                    RowTitle(med.name)
                    RowSub(med.schedules.joinToString(" / ") { timeText(it.time) } + " · " + med.dosage)
                }
            }

            McButton(
                text = stringResource(R.string.s40_btn_confirm),
                onClick = onConfirm,
                full = true,
                loading = state.isSaving,
                modifier = Modifier.padding(top = 16.dp)
            )
            McButton(
                text = stringResource(R.string.s40_btn_recheck),
                onClick = onRecheck,
                kind = ButtonKind.WHITE,
                full = true,
                enabled = !state.isSaving,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Notice(stringResource(R.string.s40_notice), Modifier.padding(top = 16.dp), warn = true)
    }
}
