package com.cau.medicheck.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
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
import com.cau.medicheck.ui.theme.Faint
import com.cau.medicheck.ui.theme.HeaderDivider
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ScreenBackground

/*
 * 한 화면의 큰 틀(앱 틀). (구현 명세 00 §1.1~1.4, §5.4)
 *
 *   ┌ 머리말 McTopBar ─ "MEDICATION CARE" + 화면 이름 (+ 로그인 후에는 ♧·⚙ 버튼) ┐
 *   │ 본문 McScreenColumn ─ 세로 스크롤 하나                                    │
 *   └ (로그인 후에는) 하단 5탭 ─ 2단계에서 만든다                                ┘
 *
 * 1단계에서는 로그인 전 화면(S00~S03)만 이 틀을 쓴다. 로그인 전에는 버튼과 하단 탭이 없다(결정 4).
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
 * @param showActions true면 오른쪽에 ♧(알림)·⚙(설정) 버튼을 그린다 — 2단계에서 만든다.
 */
@Composable
fun McTopBar(
    title: String,
    showActions: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(Color.White)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 19.dp, end = 19.dp, top = 12.dp, bottom = 11.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.header_eyebrow),
                    fontSize = 10.sp,
                    color = Faint,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
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
            // 버튼이 없을 때도 같은 높이의 빈 자리를 둔다. (showActions = true의 버튼은 2단계)
            Spacer(Modifier.height(HeaderActionHeight))
        }
        HorizontalDivider(thickness = 1.dp, color = HeaderDivider)
    }
}

/**
 * 본문: 세로 스크롤 하나, 여백 좌우 18·위 18·아래 24dp. (v3 .phone-body)
 *
 * - 키보드가 올라오면 본문이 그만큼 줄어들어(imePadding) 입력칸이 가리지 않는다.
 * - 로그인 전 화면은 하단 탭이 없으므로, 폰의 내비게이션 바 높이만큼 아래를 더 띄운다.
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
