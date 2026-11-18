package com.cau.medicheck.ui.screens.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.AuthLogo
import com.cau.medicheck.ui.components.AuthSub
import com.cau.medicheck.ui.components.AuthTitle
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.Illustration
import com.cau.medicheck.ui.components.McAppFrame
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.PagerDots
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.theme.Gap8
import com.cau.medicheck.ui.theme.MediCheckTheme
import com.cau.medicheck.ui.theme.Navy

/**
 * S00 시작/온보딩. 앱의 좋은 점 3가지를 한 장씩 보여 주고 로그인으로 보낸다. (v3 welcome())
 *
 * 이 화면은 그리기만 한다. "온보딩을 봤다"는 저장과 화면 이동은 밖에서 받은 onFinish가 한다.
 * 지금 몇 번째 장인지는 저장하지 않고, 화면을 돌려도 남도록 rememberSaveable로만 기억한다.
 *
 * @param onFinish "시작하기"(마지막 장) 또는 "이미 계정이 있어요"를 눌렀을 때
 * @param initialPage Preview에서 2·3장을 보여 주려고 둔 값. 앱에서는 늘 0(1장)부터.
 */
@Composable
fun S00WelcomeScreen(onFinish: () -> Unit, initialPage: Int = 0) {
    // rememberSaveable: 화면이 회전해 다시 만들어져도 값이 남는 기억 장치
    var page by rememberSaveable { mutableIntStateOf(initialPage) }
    val current = Onboarding.pages[page]

    McAppFrame(screenId = Routes.S00, loggedIn = false) {
        McScreenColumn(paneTitle = stringResource(R.string.screen_title_s00)) {
            AuthLogo(
                symbol = Glyph.LOGO,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 24.dp, bottom = 16.dp)
            )
            AuthTitle(stringResource(R.string.app_name))
            AuthSub(stringResource(R.string.s00_subtitle), Modifier.padding(top = 8.dp))

            Illustration(current.symbol)
            Text(
                text = stringResource(current.titleRes),
                fontSize = 23.sp,
                fontWeight = FontWeight.Black,
                color = Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            AuthSub(stringResource(current.bodyRes), Modifier.padding(top = 8.dp))

            PagerDots(
                count = Onboarding.pages.size,
                index = page,
                description = stringResource(R.string.s00_page_desc, page + 1)
            )

            McButton(
                text = stringResource(Onboarding.primaryButtonRes(page)),
                onClick = {
                    // 마지막 장이면 끝내고, 아니면 다음 장으로 (v3 act('onboardNext'))
                    if (Onboarding.isLast(page)) onFinish() else page = Onboarding.next(page)
                },
                kind = ButtonKind.PRIMARY,
                full = true
            )
            Spacer(Modifier.height(Gap8))
            McButton(
                text = stringResource(R.string.s00_have_account),
                onClick = onFinish,
                kind = ButtonKind.WHITE,
                full = true
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun S00WelcomeScreenPreview() {
    MediCheckTheme { S00WelcomeScreen(onFinish = {}) }
}

@Preview(showBackground = true, heightDp = 760, name = "3장")
@Composable
private fun S00WelcomeScreenLastPagePreview() {
    MediCheckTheme { S00WelcomeScreen(onFinish = {}, initialPage = 2) }
}
