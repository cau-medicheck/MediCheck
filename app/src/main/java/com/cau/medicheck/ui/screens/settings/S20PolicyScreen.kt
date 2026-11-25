package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.GroupLabel
import com.cau.medicheck.ui.components.IconRow
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.RowsCard
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.HeroDeepEnd
import com.cau.medicheck.ui.theme.HeroDeepStart
import com.cau.medicheck.ui.theme.RateGood
import com.cau.medicheck.ui.theme.TimelineDotMissed
import com.cau.medicheck.ui.theme.TimelineDotSkipped

/**
 * S20 통계 계산 정책 (v3 policy()).
 *
 * 복약률을 어떻게 계산하는지 글로 설명하는 화면. 숫자는 보여 주지 않는다.
 * 여기 적힌 말은 실제 계산(domain/StatsCalculator)과 같아야 한다. 계산을 바꾸면 strings.xml의 s20_* 문구와
 * 주간 통계의 안내문(s13_notice)도 함께 고친다.
 */
// 2026-11-25 다시 그림: 맨 위 청록 산식 카드 + 상태 세 줄(색 아이콘) + 자세한 설명은 아래 조용한 상자
@Composable
fun S20PolicyScreen(onBack: () -> Unit) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s20)) {
        BackLink(stringResource(R.string.s20_back), onBack)
        ScreenIntro(modifier = Modifier.padding(top = Gap16, bottom = 16.dp))

        // ----- 산식 카드 -----
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(HeroDeepStart, HeroDeepEnd)))
                .padding(18.dp)
        ) {
            Text(stringResource(R.string.s20_kicker), fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
            Text(
                stringResource(R.string.s20_formula),
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        // ----- 상태 세 가지 -----
        GroupLabel(stringResource(R.string.s20_section_states), Modifier.padding(top = 22.dp))
        RowsCard {
            IconRow(Icons.Rounded.Check, RateGood, stringResource(R.string.common_count_taken), sub = stringResource(R.string.s20_taken_desc))
            IconRow(Icons.Rounded.Redo, TimelineDotSkipped, stringResource(R.string.common_count_skipped), sub = stringResource(R.string.s20_skipped_desc))
            IconRow(Icons.Rounded.RadioButtonUnchecked, TimelineDotMissed, stringResource(R.string.s20_pending_title), sub = stringResource(R.string.s20_pending_display), showDivider = false)
        }

        Notice(stringResource(R.string.s20_formula_desc), Modifier.padding(top = 16.dp))
    }
}
