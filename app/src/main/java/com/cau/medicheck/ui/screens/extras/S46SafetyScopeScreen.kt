package com.cau.medicheck.ui.screens.extras

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.Kicker
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SettingRow
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.Gap16

/**
 * S46 안전·개발 범위 (v3 safetyScope()).
 *
 * "이 앱이 하지 않는 일"을 밝히는 안내 화면이다. 기능 목록 자산에서 Won’t(이번에는 안 함)로 분류한 기능을 읽어 보여 주고,
 * 그 아래에 "복용량을 자동으로 추천하거나 바꾸지 않는다"는 안전 고지를 둔다.
 * 안전 고지는 자산을 읽지 못해도 늘 보인다.
 *
 * @param onOpenMap "전체 요구사항 보기" → 요구사항 맵(S45)
 */
@Composable
fun S46SafetyScopeScreen(state: SafetyScopeUiState, onBack: () -> Unit, onOpenMap: () -> Unit) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s46)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenIntro(stringResource(R.string.s46_desc), Modifier.padding(vertical = Gap16))

        SurfaceCard {
            // 프로토타입처럼 영문을 대문자로 보여 준다("WON’T"). 한글에는 영향이 없다.
            Kicker(stringResource(R.string.s46_kicker).uppercase())
            if (state.loadFailed) {
                Notice(stringResource(R.string.req_load_failed), Modifier.padding(top = 10.dp), warn = true)
            }
            state.wont.forEachIndexed { index, requirement ->
                SettingRow(
                    title = stringResource(R.string.s45_row_title, requirement.id, requirement.title),
                    description = requirement.screens.joinToString(", ").ifEmpty { stringResource(R.string.s46_no_ui) },
                    showDivider = index != state.wont.lastIndex,
                    trailing = { Pill(moscowLabel(requirement.moscow), PillKind.ORANGE) }
                )
            }
        }

        // 첫 문장만 굵게 그리려고 문구를 HTML 조각으로 적어 두었다(strings.xml). fromHtml이 굵기·줄바꿈을 해석한다.
        Notice(
            text = AnnotatedString.fromHtml(stringResource(R.string.s46_notice)),
            modifier = Modifier.padding(top = Gap16),
            warn = true
        )
        McButton(
            text = stringResource(R.string.s46_btn_map),
            onClick = onOpenMap,
            kind = ButtonKind.LIGHT,
            full = true,
            modifier = Modifier.padding(top = Gap16)
        )
    }
}
