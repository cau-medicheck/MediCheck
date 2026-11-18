package com.cau.medicheck.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.theme.BodyBottom
import com.cau.medicheck.ui.theme.BodyHorizontal
import com.cau.medicheck.ui.theme.BodyTop
import com.cau.medicheck.ui.theme.HeaderDivider
import com.cau.medicheck.ui.theme.IconBtnBg
import com.cau.medicheck.ui.theme.IconBtnText
import com.cau.medicheck.ui.theme.Line
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ScreenBackground
import com.cau.medicheck.ui.theme.TabActive
import com.cau.medicheck.ui.theme.TabInactive

/*
 * 한 화면의 큰 틀(앱 틀). (구현 명세 00 §1.1~1.4, §5.4)
 *
 *   ┌ 머리말 McTopBar ─ "MEDICATION CARE" + 화면 이름 (+ 로그인 후에는 ♧·⚙ 버튼) ┐
 *   │ 본문 McScreenColumn ─ 세로 스크롤 하나                                    │
 *   └ (로그인 후에는) 하단 5탭 McBottomBar                                    ┘
 *
 * 로그인 전 화면(S00~S03)은 McAppFrame을 쓴다. 버튼과 하단 탭이 없다(결정 4).
 * 로그인 뒤 화면은 MainNavHost가 McTopBar + 화면 + McBottomBar를 한 번만 그리고, 화면은 본문만 그린다.
 */

/** 머리말의 오른쪽 버튼 자리 높이. 버튼이 없어도 이 높이를 지켜 S04 이후 화면과 머리말 높이를 맞춘다. */
private val HeaderActionHeight = 35.dp

/**
 * 화면 틀. 머리말 아래에 content(보통 McScreenColumn)를 놓는다.
 *
 * @param screenId 화면 ID("S01" 등). 머리말 제목을 여기서 고른다.
 * @param loggedIn 로그인 뒤 화면인지. false면 머리말 버튼을 그리지 않는다.
 *                 (true일 때의 ♧·⚙ 버튼과 하단 5탭은 2단계에서 붙인다.)
 */
@Composable
fun McAppFrame(
    screenId: String,
    loggedIn: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        McTopBar(title = stringResource(Routes.titleRes(screenId)), showActions = loggedIn)
        // weight(1f): 머리말을 뺀 나머지 높이를 본문이 모두 차지한다.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

/**
 * 머리말: 작은 글자 "MEDICATION CARE" + 화면 이름.
 * 흰 바탕이 폰의 상태 표시줄 뒤까지 이어지도록, 상태 표시줄 높이만큼 위를 스스로 띄운다.
 * 머리말은 스크롤되지 않는다(v3 .phone-head{flex:none}).
 *
 * @param showActions true면 오른쪽에 ♧(알림)·⚙(설정) 버튼을 그린다 (로그인 뒤 화면).
 */
@Composable
fun McTopBar(
    title: String,
    showActions: Boolean,
    modifier: Modifier = Modifier,
    /** true면 화면 이름 대신 앱 로고와 "MediCheck"를 보인다 (홈). 화면 이름은 본문의 큰 제목이 맡는다. */
    brand: Boolean = false,
    onNotifications: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth().background(Color.White)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 19.dp, end = 19.dp, top = 12.dp, bottom = 11.dp)
        ) {
            if (brand) {
                // 로고 그림은 장식이고, 글자 "MediCheck"가 읽힌다.
                Image(
                    painter = painterResource(R.drawable.logo_capsule),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp,
                    color = Navy,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(start = 4.dp).semantics { heading() }
                )
            } else Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Navy,
                    maxLines = 2,                       // 글자가 커져도 2줄까지는 다 보이게
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
            }
            Spacer(Modifier.width(12.dp))
            if (showActions) {
                // 종(알림) · 줄 세 개(설정 메뉴). 새 시안의 머리말 오른쪽과 같은 구성. 프로필은 설정 안에서 들어간다.
                HeaderAction(stringResource(R.string.header_cd_notifications), onNotifications) {
                    GlyphText(Glyph.HEADER_BELL, fontSize = 22, color = Navy)
                }
                HeaderAction(stringResource(R.string.header_cd_settings), onSettings) {
                    Icon(Icons.Filled.Menu, contentDescription = null, tint = Navy, modifier = Modifier.size(22.dp))
                }
            } else {
                // 버튼이 없을 때도 같은 높이의 빈 자리를 둔다 (로그인 전후의 머리말 높이를 맞춘다).
                Spacer(Modifier.height(44.dp))
            }
        }
        HorizontalDivider(thickness = 1.dp, color = HeaderDivider)
    }
}

/** 머리말 오른쪽의 누르는 자리 하나(44dp). 안의 그림은 장식이고 TalkBack은 description을 읽는다. */
@Composable
private fun HeaderAction(description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description; role = Role.Button }
    ) { content() }
}

/**
 * 네모 버튼 (v3 .iconbtn: 35×35, 둥글기 12). 머리말의 ♧·⚙와 달 넘기기 ‹·›가 쓴다.
 * @param outlined true면 흰 바탕 + 테두리(♧ 알림), false면 옅은 청록 바탕(⚙ 설정)
 * @param description TalkBack이 읽는 이름 ("알림", "설정")
 */
@Composable
fun IconSquareButton(symbol: String, description: String, outlined: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(HeaderActionHeight)
            .clip(shape)
            .background(if (outlined) Color.White else IconBtnBg)
            .then(if (outlined) Modifier.border(1.dp, Line, shape) else Modifier)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description; role = Role.Button }
    ) {
        GlyphText(symbol, fontSize = 18, color = IconBtnText)
    }
}

/** 하단 탭 다섯 개. 순서대로 그려진다. (v3 nav) */
enum class McTab(val route: String, val symbol: String, @param:StringRes val labelRes: Int) {
    HOME(Routes.S04, Glyph.TAB_HOME, R.string.tab_home),
    SCHEDULE(Routes.S05, Glyph.TAB_SCHEDULE, R.string.tab_schedule),
    MEDS(Routes.S06, Glyph.TAB_MEDS, R.string.tab_meds),
    RECORDS(Routes.S11, Glyph.TAB_RECORDS, R.string.tab_records),
    STATS(Routes.S13, Glyph.TAB_STATS, R.string.tab_stats)
}

/** 하단 탭이 차지하는 높이(내비게이션 바 제외). 토스트를 이 위에 띄운다. */
val McBottomBarHeight = 59.dp

/**
 * 하단 5탭: 홈 · 일정 · 내 약 · 기록 · 통계. (v3 .bottom-nav)
 * 탭을 누르면 언제나 그 탭의 첫 화면으로 간다(이동 규칙은 AppNavigator.openTab).
 *
 * @param active 강조할 탭. 어느 탭에도 속하지 않는 화면(설정 등)이면 null
 */
@Composable
fun McBottomBar(active: McTab?, onTab: (McTab) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        HorizontalDivider(thickness = 1.dp, color = Line)
        Row(
            modifier = Modifier
                // 폰의 내비게이션 바(화면 맨 아래 막대) 높이만큼 아래를 띄운다.
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(top = 9.dp, bottom = 12.dp)
                .selectableGroup()
        ) {
            McTab.entries.forEach { tab ->
                val selected = tab == active
                val color = if (selected) TabActive else TabInactive
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onTab(tab) })
                ) {
                    // 기호는 장식이다. TalkBack은 아래 글자("홈")만 읽는다.
                    Text(
                        text = tab.symbol,
                        fontSize = 20.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = color,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                    Text(
                        text = stringResource(tab.labelRes),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                        color = color
                    )
                }
            }
        }
    }
}

/**
 * 본문: 세로 스크롤 하나, 여백 좌우 18·위 18·아래 24dp. (v3 .phone-body)
 *
 * - 키보드가 올라오면 본문이 그만큼 줄어들어(imePadding) 입력칸이 가리지 않는다.
 * - 로그인 전 화면은 하단 탭이 없으므로, 폰의 내비게이션 바 높이만큼 아래를 더 띄운다.
 *   (로그인 뒤에는 하단 탭이 그 높이를 이미 차지했다고 알려 주므로 여기서는 0이 된다.)
 * - 접근성의 글자 배율과 높은 대비는 본문에만 적용한다(v3도 .phone-body에만 적용, 00 §1.4).
 * - paneTitle: 화면이 바뀌면 TalkBack이 이 화면 이름을 읽어 준다.
 */
@Composable
fun McScreenColumn(
    paneTitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val a11y = LocalA11ySettings.current
    ProvideBodyFontScale {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .highContrast(a11y.highContrast)
                .semantics { this.paneTitle = paneTitle }
                .imePadding()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(start = BodyHorizontal, end = BodyHorizontal, top = BodyTop, bottom = BodyBottom),
            content = content
        )
    }
}
