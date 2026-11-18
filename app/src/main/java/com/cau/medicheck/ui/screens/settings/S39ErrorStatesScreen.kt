package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.EmptyState
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SegmentedTabs
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Navy

/** 탭 하나에 딸린 예시: 탭 이름, 그림 글자, 제목, 설명, 버튼 글자 */
private data class StateExample(
    val tabRes: Int,
    val symbol: String,
    val titleRes: Int,
    val bodyRes: Int,
    val actionRes: Int
)

private val EXAMPLES = listOf(
    StateExample(
        R.string.s39_tab_empty, Glyph.EXAMPLE_EMPTY,
        R.string.s39_empty_title, R.string.s39_empty_body, R.string.s39_empty_action
    ),
    StateExample(
        R.string.s39_tab_input, Glyph.EXAMPLE_INPUT,
        R.string.s39_input_title, R.string.s39_input_body, R.string.s39_input_action
    ),
    StateExample(
        R.string.s39_tab_offline, Glyph.EXAMPLE_OFFLINE,
        R.string.s39_offline_title, R.string.s39_offline_body, R.string.s39_offline_action
    )
)

/**
 * S39 빈 화면·오류 상태 (v3 errorStates()).
 *
 * "내용이 없을 때·입력이 틀렸을 때·인터넷이 없을 때 이렇게 안내한다"를 보여 주는 예시 전시 화면이다.
 * 실제 빈 상태·오류 문구는 각 화면이 따로 갖고 있어서 여기 문구와 다를 수 있다.
 * 고른 탭은 이 화면 안에서만 기억한다(rememberSaveable = 화면을 돌려도 남는 기억).
 *
 * @param onAddMedication '약 추가'·'다시 입력' → 약 등록 화면(S08)
 * @param onOpenSchedule '일정 확인' → 일정 화면(S05)
 */
@Composable
fun S39ErrorStatesScreen(onBack: () -> Unit, onAddMedication: () -> Unit, onOpenSchedule: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val example = EXAMPLES[selected]

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s39)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenIntro(stringResource(R.string.s39_subtitle), Modifier.padding(vertical = Gap16))

        SurfaceCard {
            Text(stringResource(R.string.s39_card_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
            SegmentedTabs(
                labels = EXAMPLES.map { stringResource(it.tabRes) },
                selectedIndex = selected,
                onSelect = { selected = it },
                modifier = Modifier.padding(vertical = Gap16)
            )
            EmptyState(
                emoji = example.symbol,
                title = stringResource(example.titleRes),
                body = stringResource(example.bodyRes),
                actionLabel = stringResource(example.actionRes),
                // 마지막 탭(오프라인)의 버튼만 일정으로 가고, 앞의 두 탭은 약 등록으로 간다.
                onAction = if (selected == EXAMPLES.lastIndex) onOpenSchedule else onAddMedication
            )
        }

        Notice(stringResource(R.string.s39_notice), Modifier.padding(top = Gap16))
    }
}
