package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ListRow
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.OptionRow
import com.cau.medicheck.ui.components.RowSub
import com.cau.medicheck.ui.components.RowTitle
import com.cau.medicheck.ui.components.ScreenTitle
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText

/** "추천 시연 흐름"의 다섯 단계 (제목, 설명) */
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
@Composable
fun S22HelpScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s22)) {
        BackLink(stringResource(R.string.s20_back), onBack)
        ScreenTitle(
            title = stringResource(R.string.s22_title),
            subtitle = stringResource(R.string.s22_subtitle),
            modifier = Modifier.padding(top = Gap16)
        )

        // ----- 추천 시연 흐름: 눌리지 않는 안내 줄 -----
        SectionTitle(stringResource(R.string.s22_section_flow))
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                FLOW_STEPS.forEachIndexed { index, (titleRes, descRes) ->
                    val stepLabel = stringResource(R.string.s22_cd_step, index + 1)
                    ListRow(
                        leading = {
                            // 번호 상자는 그림으로만 그려지므로(Avatar는 읽지 않음), 바깥 상자에 "1단계"라고 읽을 이름을 붙인다.
                            Box(Modifier.semantics { contentDescription = stepLabel }) {
                                Avatar((index + 1).toString(), fontSize = 15)
                            }
                        },
                        showDivider = index != FLOW_STEPS.lastIndex
                    ) {
                        RowTitle(stringResource(titleRes))
                        RowSub(stringResource(descRes))
                    }
                }
            }
        }

        // ----- 이 앱의 구현 범위 -----
        SectionTitle(stringResource(R.string.s22_section_scope))
        Notice(stringResource(R.string.s22_scope_notice), warn = true)

        SurfaceCard(Modifier.padding(top = Gap16)) {
            Text(stringResource(R.string.s22_tech_title), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(
                text = stringResource(R.string.s22_tech_list),
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = ParagraphText,
                modifier = Modifier.padding(top = 9.dp)
            )
        }

        // ----- 관련 화면 -----
        SectionTitle(stringResource(R.string.s22_section_related))
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                OptionRow(
                    title = stringResource(R.string.screen_title_s39),
                    description = stringResource(R.string.s22_row_errors_desc),
                    onClick = { onOpen(Routes.S39) }
                )
                OptionRow(
                    title = stringResource(R.string.screen_title_s46),
                    description = stringResource(R.string.s22_row_safety_desc),
                    onClick = { onOpen(Routes.S46) },
                    showDivider = false
                )
            }
        }
    }
}
