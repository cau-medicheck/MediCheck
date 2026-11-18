package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.Kicker
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.ScreenTitle
import com.cau.medicheck.ui.components.SettingRow
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText
import com.cau.medicheck.ui.theme.SettingDescText
import com.cau.medicheck.ui.theme.SettingDivider

/**
 * S20 통계 계산 정책 (v3 policy()).
 *
 * 복약률을 어떻게 계산하는지 글로 설명하는 화면. 숫자는 보여 주지 않는다.
 * 여기 적힌 말은 실제 계산(domain/StatsCalculator)과 같아야 한다. 계산을 바꾸면 strings.xml의 s20_* 문구와
 * 주간 통계의 안내문(s13_notice)도 함께 고친다.
 */
@Composable
fun S20PolicyScreen(onBack: () -> Unit) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s20)) {
        BackLink(stringResource(R.string.s20_back), onBack)
        ScreenTitle(
            title = stringResource(R.string.s20_title),
            subtitle = stringResource(R.string.s20_subtitle),
            modifier = Modifier.padding(vertical = Gap16)
        )

        // ----- 상태 세 가지의 뜻 -----
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                SettingRow(stringResource(R.string.status_pill_taken), stringResource(R.string.s20_taken_desc))
                SettingRow(stringResource(R.string.status_pill_skipped), stringResource(R.string.s20_skipped_desc))
                // 미기록 줄은 설명 아래에 한 줄이 더 붙어서(화면에 어떻게 표시되는지) SettingRow를 쓰지 않고 직접 그린다.
                Column(Modifier.padding(vertical = 13.dp)) {
                    Text(
                        text = stringResource(R.string.s20_pending_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text(
                        text = stringResource(R.string.s20_pending_desc),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = SettingDescText,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.s20_pending_display),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = SettingDescText,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ----- 산식 -----
        SurfaceCard(Modifier.padding(top = Gap16)) {
            Kicker(stringResource(R.string.s20_kicker))
            Text(
                text = stringResource(R.string.s20_formula),
                fontSize = 16.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Navy,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            HorizontalDivider(thickness = 1.dp, color = SettingDivider)
            Text(
                text = stringResource(R.string.s20_formula_desc),
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = ParagraphText,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Notice(stringResource(R.string.s20_notice), Modifier.padding(top = Gap16))
    }
}
