package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.Teal

private val FLOW_STEPS = listOf(
    R.string.s22_step1_title to R.string.s22_step1_desc,
    R.string.s22_step2_title to R.string.s22_step2_desc,
    R.string.s22_step3_title to R.string.s22_step3_desc,
    R.string.s22_step4_title to R.string.s22_step4_desc,
    R.string.s22_step5_title to R.string.s22_step5_desc
)

/**
 * S22 도움말·범위 (v3 help()).
 *
 * 글만 있는 화면이라 ViewModel이 없다. 구성: 추천 시연 흐름 5단계 → 이 앱의 구현 범위 → 개발 기술 → 관련 화면.
 * "관련 화면" 두 줄은 프로토타입에 들어갈 길이 없던 S39·S46의 입구로 맨 아래에 더했다.
 * 구현 범위 안내는 실제 구현과 맞아야 한다. 구현이 바뀌면 strings.xml의 s22_scope_notice도 고친다.
 */
// 2026-11-25 다시 그림: 번호 단계 + 앱 정보(안내 상자 + 기술 칩) + 관련 화면 아이콘 줄
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun S22HelpScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s22)) {
        BackLink(stringResource(R.string.s20_back), onBack)
        ScreenIntro(modifier = Modifier.padding(top = Gap16, bottom = 16.dp))

        // ----- 이렇게 써 보세요: 번호 단계 -----
        GroupLabel(stringResource(R.string.s22_section_flow))
        RowsCard {
            FLOW_STEPS.forEachIndexed { index, (titleRes, descRes) ->
                val stepLabel = stringResource(R.string.s22_cd_step, index + 1)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(26.dp).clip(CircleShape).background(Teal).semantics { contentDescription = stepLabel }
                    ) { Text((index + 1).toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(stringResource(titleRes), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Navy)
                        Text(stringResource(descRes), fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 1.dp))
                    }
                }
            }
        }

        // ----- 앱 정보 -----
        GroupLabel(stringResource(R.string.s22_section_scope), Modifier.padding(top = 22.dp))
        Notice(stringResource(R.string.s22_scope_notice))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
            stringResource(R.string.s22_tech_list).split(" · ").forEach { tech ->
                Text(
                    tech,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Navy,
                    modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Color.White).padding(horizontal = 11.dp, vertical = 6.dp)
                )
            }
        }

        // ----- 관련 화면 -----
        GroupLabel(stringResource(R.string.s22_section_related), Modifier.padding(top = 22.dp))
        RowsCard {
            IconRow(Icons.Rounded.ErrorOutline, Color(0xFFE08A2E), stringResource(R.string.screen_title_s39), onClick = { onOpen(Routes.S39) })
            IconRow(Icons.Rounded.Shield, Color(0xFF71858E), stringResource(R.string.screen_title_s46), onClick = { onOpen(Routes.S46) }, showDivider = false)
        }
    }
}
