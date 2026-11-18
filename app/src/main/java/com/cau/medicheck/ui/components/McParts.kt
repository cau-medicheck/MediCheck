package com.cau.medicheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.ui.theme.AuthSubText
import com.cau.medicheck.ui.theme.AuthTitleText
import com.cau.medicheck.ui.theme.IllustrationEnd
import com.cau.medicheck.ui.theme.IllustrationStart
import com.cau.medicheck.ui.theme.PagerDotActive
import com.cau.medicheck.ui.theme.PagerDotInactive
import com.cau.medicheck.ui.theme.Teal

/*
 * 여러 화면에서 쓰는 중간 크기 부품. (구현 명세 00 §5.1)
 * 지금은 로그인 전 화면(S00~S03)이 쓰는 것만 있다. 다음 단계가 자기 화면에 필요한 부품을 여기 더한다.
 */

/** 시작·로그인·가입 화면의 큰 제목 (v3 .auth-title: 26px, 굵기 900, 자간 -1px, 가운데) */
@Composable
fun AuthTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 26.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1).sp,
        color = AuthTitleText,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() }
    )
}

/** 큰 제목 아래 설명 (v3 .auth-sub: 12px, 줄 높이 1.8, 가운데). 위 여백 8dp는 부르는 쪽이 준다. */
@Composable
fun AuthSub(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 22.sp,
        color = AuthSubText,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * 온보딩의 그림 상자 (v3 .illustration: 높이 147px, 140도 그라데이션, 둥글기 19px, 글자 67px).
 * 아이콘은 장식이라 TalkBack이 읽지 않는다.
 */
@Composable
fun Illustration(symbol: ImageVector, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(top = 30.dp, bottom = 22.dp)
            .fillMaxWidth()
            .height(147.dp)
            .clip(RoundedCornerShape(19.dp))
            // 왼쪽 위 → 오른쪽 아래로 흐르는 그라데이션 (CSS 140deg와 거의 같은 방향)
            .background(
                Brush.linearGradient(
                    colors = listOf(IllustrationStart, IllustrationEnd),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .clearAndSetSemantics { }
    ) {
        McIcon(symbol, size = 67, tint = Teal)
    }
}

/**
 * 온보딩의 진행 점. 지금 장은 가로로 긴 알약(20 × 7dp), 나머지는 작은 점(7 × 7dp).
 * 누를 수 없는 표시라서(v3도 span) TalkBack에는 점 대신 description("3장 중 1장")만 읽힌다.
 */
@Composable
fun PagerDots(count: Int, index: Int, description: String, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
            .clearAndSetSemantics { contentDescription = description }
    ) {
        repeat(count) { i ->
            val active = i == index
            Box(
                Modifier
                    .size(width = if (active) 20.dp else 7.dp, height = 7.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) PagerDotActive else PagerDotInactive)
            )
        }
    }
}
