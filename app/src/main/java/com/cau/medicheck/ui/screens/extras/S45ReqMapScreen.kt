package com.cau.medicheck.ui.screens.extras

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.domain.Moscow
import com.cau.medicheck.domain.Requirement
import com.cau.medicheck.domain.Requirements
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.McSelectField
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.navigation.NavRules
import com.cau.medicheck.ui.navigation.ReqTarget
import com.cau.medicheck.ui.theme.CountBoxBg
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ReqDivider
import com.cau.medicheck.ui.theme.SettingDescText

/** 우선순위에 맞는 화면 글자 ("Must" 등). 자산의 글자가 아니라 strings.xml의 글자를 쓴다. */
@Composable
internal fun moscowLabel(moscow: Moscow): String = stringResource(
    when (moscow) {
        Moscow.MUST -> R.string.moscow_must
        Moscow.SHOULD -> R.string.moscow_should
        Moscow.COULD -> R.string.moscow_could
        Moscow.WONT -> R.string.moscow_wont
    }
)

/**
 * S45 기능별 요구사항 맵 (v3 reqMap()).
 *
 * 팀이 정한 기능 107개를 한 화면에서 훑어보는 도구다. 위쪽에 우선순위별 개수, 그 아래 검색과 우선순위 고르기,
 * 그 아래 기능 줄들. 기능 줄을 누르면 그 기능을 볼 수 있는 화면으로 간다(뒤로 가면 여기로 돌아온다).
 *
 * 검색어와 고른 우선순위는 ViewModel이 아니라 이 화면이 들고 있다(rememberSaveable = 화면을 돌려도 남는 기억).
 * 입력칸 값을 ViewModel에 보냈다가 돌려받으면 한 박자 늦어, 한글을 조합하는 중에 글자가 깨질 수 있어서다.
 * 거르는 계산만 순수 함수 Requirements.filter에 맡긴다.
 *
 * @param onRowClick 기능 줄을 눌렀을 때 (어디로 갈지는 그래프가 NavRules.reqTarget으로 정한다)
 */
@Composable
fun S45ReqMapScreen(state: ReqMapUiState, onRowClick: (Requirement) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    // 0 = 전체 우선순위, 1~4 = Must·Should·Could·Won’t
    var tierIndex by rememberSaveable { mutableIntStateOf(0) }
    val tier = Moscow.entries.getOrNull(tierIndex - 1)
    // remember(…): 괄호 안의 값이 바뀔 때만 다시 계산한다 (화면이 다시 그려질 때마다 107줄을 거르지 않게).
    val rows = remember(state.all, query, tier) { Requirements.filter(state.all, query, tier) }
    val keyboard = LocalSoftwareKeyboardController.current

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s45)) {

        if (state.loadFailed) {
            Notice(stringResource(R.string.req_load_failed), warn = true)
            return@McScreenColumn
        }
        if (state.isLoading) return@McScreenColumn

        // ----- 우선순위별 개수 + 검색 + 우선순위 고르기 -----
        SurfaceCard {
            Moscow.entries.chunked(2).forEachIndexed { rowIndex, pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = if (rowIndex == 0) 0.dp else 10.dp)
                ) {
                    pair.forEach { moscow ->
                        CountBox(state.counts[moscow] ?: 0, moscowLabel(moscow), Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(Gap16))
            val searchHint = stringResource(R.string.s45_search_hint)
            FormField(
                value = query,
                onValueChange = { query = it },   // 글자가 바뀔 때마다 바로 거른다
                placeholder = searchHint,
                label = searchHint,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() })   // '검색' 키는 키보드만 내린다
            )
            Spacer(Modifier.height(8.dp))
            McSelectField(
                selected = tierIndex,
                options = (0..Moscow.entries.size).toList(),
                label = { index ->
                    Moscow.entries.getOrNull(index - 1)?.let { moscowLabel(it) } ?: stringResource(R.string.s45_tier_all)
                },
                onSelect = { tierIndex = it }
            )
        }

        // ----- 기능 줄 -----
        SectionTitle(stringResource(R.string.s45_count, rows.size))
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                if (rows.isEmpty()) {
                    Text(
                        text = stringResource(R.string.s45_empty),
                        fontSize = 11.sp,
                        color = Muted,
                        modifier = Modifier.padding(vertical = 13.dp)
                    )
                }
                rows.forEach { requirement ->
                    // 이 화면(S45) 자신을 가리키는 줄은 눌러도 갈 곳이 없으므로 버튼으로 만들지 않는다.
                    val goesSomewhere = NavRules.reqTarget(requirement.screens) != ReqTarget.Stay
                    ReqRow(requirement, onClick = if (goesSomewhere) ({ onRowClick(requirement) }) else null)
                }
            }
        }

        Notice(stringResource(R.string.s45_notice), Modifier.padding(top = Gap16))
    }
}

/** 개수 상자 하나: 위에 숫자, 아래에 우선순위 이름. TalkBack은 "60, Must"처럼 한 번에 읽는다. */
@Composable
private fun CountBox(count: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CountBoxBg)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .semantics(mergeDescendants = true) { }
    ) {
        Text(text = count.toString(), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Navy)
        Text(text = label, fontSize = 11.sp, color = Navy)
    }
}

/**
 * 기능 한 줄: "F001 · 회원가입" / "계정 · S02" / 오른쪽 우선순위 배지.
 * @param onClick null이면 누를 수 없는 줄로 그린다 (버튼 역할도 붙이지 않는다)
 */
@Composable
private fun ReqRow(requirement: Requirement, onClick: (() -> Unit)?) {
    val screens = requirement.screens.joinToString(", ").ifEmpty { stringResource(R.string.s45_non_ui) }
    Column(
        modifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 13.dp)) {
            Column(Modifier.weight(1f)) {
                // 제목·설명은 줄 수를 제한하지 않는다 — 글자를 크게 해도 잘리지 않게.
                Text(
                    text = stringResource(R.string.s45_row_title, requirement.id, requirement.title),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                Text(
                    text = stringResource(R.string.s45_row_sub, requirement.scope, screens),
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = SettingDescText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            // Must만 민트, 나머지는 주황 (프로토타입과 같은 색 규칙). 색만으로 구분하지 않도록 글자가 함께 있다.
            Pill(
                text = moscowLabel(requirement.moscow),
                kind = if (requirement.moscow == Moscow.MUST) PillKind.MINT else PillKind.ORANGE
            )
        }
        HorizontalDivider(thickness = 1.dp, color = ReqDivider)
    }
}
