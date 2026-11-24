package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.data.local.A11ySettings
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.components.ToggleRow
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText
import com.cau.medicheck.ui.theme.PreviewBg
import com.cau.medicheck.ui.theme.SettingDivider

/** 이 화면에서 누를 수 있는 것들 */
data class AccessibilityActions(
    val onBack: () -> Unit,
    val onFontScale: (Int) -> Unit,
    val onHighContrast: (Boolean) -> Unit,
    val onReduceMotion: (Boolean) -> Unit
)

/**
 * S37 접근성 설정 (v3 accessibility()).
 *
 * 글자 크기(100/125/150/200%)·높은 대비·움직임 최소화를 고른다. 누르는 즉시 저장되고 앱 전체에 바로 적용된다.
 * 이 화면도 본문(McScreenColumn) 안에 있어서, 고른 글자 크기가 미리보기 상자에 바로 보인다.
 * 실제로 적용하는 장치는 ui/components/McA11y.kt에 있다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun S37AccessibilityScreen(settings: A11ySettings, actions: AccessibilityActions) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s37)) {
        BackLink(stringResource(R.string.common_back_previous), actions.onBack)
        ScreenIntro(modifier = Modifier.padding(vertical = Gap16))

        // ----- 글자 크기 -----
        SurfaceCard {
            Text(stringResource(R.string.s37_preview_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(
                text = stringResource(R.string.s37_preview_desc),
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = ParagraphText,
                modifier = Modifier.padding(top = 5.dp)
            )
            // FlowRow: 한 줄에 다 안 들어가면 다음 줄로 넘긴다 (글자를 키우면 버튼 4개가 한 줄에 안 들어간다).
            // selectableGroup: TalkBack에게 "이 넷은 하나만 고르는 묶음"이라고 알려 준다.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(vertical = Gap16)
                    .selectableGroup()
            ) {
                A11ySettings.FONT_SCALES.forEach { percent ->
                    val chosen = percent == settings.fontScalePercent
                    val spoken = stringResource(R.string.s37_cd_font_scale, percent)
                    McButton(
                        text = stringResource(R.string.s37_scale, percent),
                        onClick = { actions.onFontScale(percent) },
                        kind = if (chosen) ButtonKind.PRIMARY else ButtonKind.WHITE,
                        small = true,
                        // TalkBack은 "글자 크기 125%, 선택 안 됨"처럼 읽는다.
                        modifier = Modifier.semantics {
                            contentDescription = spoken
                            selected = chosen
                            role = Role.RadioButton
                        }
                    )
                }
            }
            // 미리보기 상자: 고정된 예시 글(실제 데이터가 아니다)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PreviewBg)
                    .padding(15.dp)
            ) {
                Text(stringResource(R.string.s37_sample_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text(
                    text = stringResource(R.string.s37_sample_status),
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = ParagraphText,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }

        // ----- 높은 대비 · 움직임 최소화 -----
        SurfaceCard(Modifier.padding(top = Gap16), padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                ToggleRow(
                    title = stringResource(R.string.s37_contrast_title),
                    description = stringResource(R.string.s37_contrast_desc),
                    checked = settings.highContrast,
                    onCheckedChange = actions.onHighContrast,
                    verticalPadding = 13.dp
                )
                HorizontalDivider(thickness = 1.dp, color = SettingDivider)
                ToggleRow(
                    title = stringResource(R.string.s37_motion_title),
                    description = stringResource(R.string.s37_motion_desc),
                    checked = settings.reduceMotion,
                    onCheckedChange = actions.onReduceMotion,
                    verticalPadding = 13.dp
                )
            }
        }

    }
}
