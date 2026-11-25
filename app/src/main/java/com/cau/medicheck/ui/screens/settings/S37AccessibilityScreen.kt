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
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.components.RowsCard
import com.cau.medicheck.ui.components.IconSwitchRow
import com.cau.medicheck.ui.components.GroupLabel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Row
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

        // 2026-11-25 다시 그림: "글자 크기" 칸(같은 너비 4칸 + 미리보기) + "보기 편하게" 스위치 두 줄(아이콘)
        GroupLabel(stringResource(R.string.s37_preview_title))
        SurfaceCard {
            // selectableGroup: TalkBack에게 "이 넷은 하나만 고르는 묶음"이라고 알려 준다.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.selectableGroup()) {
                A11ySettings.FONT_SCALES.forEach { percent ->
                    val chosen = percent == settings.fontScalePercent
                    val spoken = stringResource(R.string.s37_cd_font_scale, percent)
                    Text(
                        text = stringResource(R.string.s37_scale, percent),
                        fontSize = 13.sp,
                        fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (chosen) Color.White else Navy,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (chosen) Teal else Color(0xFFF1F5F6))
                            .selectable(selected = chosen, role = Role.RadioButton, onClick = { actions.onFontScale(percent) })
                            .semantics { contentDescription = spoken }
                            .padding(vertical = 11.dp)
                    )
                }
            }
            // 미리보기 상자: 고정된 예시 글(실제 데이터가 아니다)
            Column(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PreviewBg)
                    .padding(15.dp)
            ) {
                Text(stringResource(R.string.s37_sample_title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                Text(
                    text = stringResource(R.string.s37_sample_status),
                    fontSize = 13.sp,
                    color = ParagraphText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        GroupLabel(stringResource(R.string.s37_section_view), Modifier.padding(top = 22.dp))
        RowsCard {
            IconSwitchRow(Icons.Rounded.Contrast, Navy, stringResource(R.string.s37_contrast_title), settings.highContrast, actions.onHighContrast)
            IconSwitchRow(Icons.Rounded.MotionPhotosOff, Color(0xFF8064C1), stringResource(R.string.s37_motion_title), settings.reduceMotion, actions.onReduceMotion, showDivider = false)
        }
    }
}
