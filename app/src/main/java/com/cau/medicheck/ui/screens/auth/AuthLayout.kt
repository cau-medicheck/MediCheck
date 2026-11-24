package com.cau.medicheck.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.cau.medicheck.ui.components.ProvideBodyFontScale
import com.cau.medicheck.ui.components.Wordmark
import com.cau.medicheck.ui.theme.HeroDeepEnd
import com.cau.medicheck.ui.theme.HeroDeepStart
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ScreenBackground
import com.cau.medicheck.ui.theme.Teal

/*
 * 로그인 전 화면들(시작·로그인·회원가입·비밀번호 재설정)의 공통 틀. (2026-11-24, 시안: 04_화면캡처/로그인_시안/시안.html B)
 *
 *   ┌─────────────── 청록 머리 ───────────────┐   흰 글자 로고 + 한 줄 + 오른쪽 아래 약 그림
 *   │  MediCheck                              │
 *   │  다시 오셨네요                  [캡슐]   │
 *   └──────┬──────────────────────────┬───────┘
 *          │  흰 카드 (머리에 겹쳐 떠 있음)  │   ← content
 *          └──────────────────────────┘
 *               계정이 없나요? 회원가입          ← footer
 *
 * 청록 머리가 상태 표시줄 뒤까지 이어지므로, 이 틀이 보이는 동안 상태 표시줄 아이콘을 흰색으로 바꾼다.
 */

/** 머리 높이 (상태 표시줄 높이는 따로 더해진다) */
private val HeroHeight = 232.dp
/** 카드가 머리에 겹치는 길이 */
private val CardOverlap = 72.dp

/**
 * @param tagline 로고 아래 한 줄 ("다시 오셨네요")
 * @param onBack 있으면 로고 위에 "‹ 뒤로" 글자 단추
 * @param backLabel 뒤로 단추 글자
 * @param footer 카드 아래 (글자 링크 등)
 * @param content 흰 카드 안
 */
@Composable
fun AuthFrame(
    paneTitle: String,
    tagline: String,
    onBack: (() -> Unit)? = null,
    backLabel: String = "",
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    WhiteStatusBarIcons()
    ProvideBodyFontScale {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .semantics { this.paneTitle = paneTitle }
                .imePadding()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Box(Modifier.fillMaxWidth()) {
                AuthHero(tagline, onBack, backLabel) {
                    PillArt(Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = CardOverlap + 14.dp).size(96.dp, 70.dp), onDark = true)
                }
                // 흰 카드: 머리 아래쪽에 CardOverlap만큼 겹친다
                Column(
                    modifier = Modifier
                        .padding(top = HeroHeight - CardOverlap)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = Navy.copy(alpha = 0.18f), spotColor = Navy.copy(alpha = 0.18f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    content = content
                )
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), content = footer)
        }
    }
}

/**
 * 시작(온보딩) 화면 틀: 청록 머리 아래쪽에 큰 그림 상자가 걸쳐 떠 있고, 그 아래에 제목·설명·버튼.
 * @param art 그림 상자 안
 */
@Composable
fun AuthHeroArtFrame(
    paneTitle: String,
    tagline: String,
    art: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    WhiteStatusBarIcons()
    ProvideBodyFontScale {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .semantics { this.paneTitle = paneTitle }
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Box(Modifier.fillMaxWidth()) {
                AuthHero(tagline, height = HeroHeight + 40.dp)
                // 그림 상자: 머리 아래 끝에 반쯤 걸친다
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 56.dp)
                        .size(136.dp)
                        .shadow(16.dp, RoundedCornerShape(38.dp), ambientColor = Navy.copy(alpha = 0.22f), spotColor = Navy.copy(alpha = 0.22f))
                        .clip(RoundedCornerShape(38.dp))
                        .background(Color.White)
                        .clearAndSetSemantics { }
                ) { art() }
            }
            Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 80.dp, bottom = 22.dp), content = content)
        }
    }
}

/** 청록 머리: 상태 표시줄 뒤부터 시작, 아래 모서리만 둥글다. 오른쪽 아래에 옅은 동그라미 장식 */
@Composable
private fun AuthHero(
    tagline: String,
    onBack: (() -> Unit)? = null,
    backLabel: String = "",
    height: Dp = HeroHeight,
    decoration: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(HeroDeepStart, HeroDeepEnd)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(height)
    ) {
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 30.dp)
                .size(190.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        )
        decoration()
        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            if (onBack != null) {
                Text(
                    text = "‹ $backLabel",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onBack)
                        .padding(vertical = 6.dp)
                )
            }
            Wordmark(fontSize = 28.sp, onDark = true)
            Text(tagline, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** 흰 카드 맨 위 제목 ("로그인") */
@Composable
fun AuthCardTitle(text: String) {
    Text(
        text = text,
        fontSize = 21.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Navy,
        modifier = Modifier.padding(bottom = 4.dp).semantics { heading() }
    )
}

/** 카드 아래 "계정이 없나요? 회원가입" */
@Composable
fun AuthFooterLink(question: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(question, fontSize = 13.sp, color = Muted)
        Text(
            text = action,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Teal,
            modifier = Modifier
                .padding(start = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 10.dp)
        )
    }
}

/** 온보딩 한 장의 그림 (그림 상자 안). 장마다: 약 · 알림 · 기록 */
@Composable
fun OnboardingArt(kind: Int) {
    when (kind) {
        0 -> PillArt(Modifier.size(100.dp, 74.dp), onDark = false)
        1 -> Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Teal, modifier = Modifier.size(64.dp))
        else -> Icon(Icons.Filled.BarChart, contentDescription = null, tint = Teal, modifier = Modifier.size(64.dp))
    }
}

/**
 * 납작한 약 그림: 비스듬한 두 색 캡슐 + 둥근 알약. 그림 파일 없이 Canvas로 그린다.
 * @param onDark 청록 바탕 위(흰·민트)인지, 흰 바탕 위(청록)인지
 */
@Composable
fun PillArt(modifier: Modifier, onDark: Boolean) {
    val body = Color.White
    val accent = if (onDark) Color(0xFFA8EDE3) else Teal
    val outline = if (onDark) Color.Transparent else Teal
    val tablet = if (onDark) Color.White.copy(alpha = 0.85f) else Color(0xFF34B7A0)
    Canvas(modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        rotate(-30f, pivot = Offset(w * 0.45f, h * 0.5f)) { capsule(w, h, body, accent, outline) }
        // 오른쪽 아래 둥근 알약 (가운데 홈)
        val r = h * 0.2f
        val c = Offset(w * 0.8f, h * 0.74f)
        drawCircle(Color.White, r, c)
        drawCircle(tablet, r, c, style = Stroke(h * 0.045f))
        drawLine(tablet, Offset(c.x - r * 0.6f, c.y), Offset(c.x + r * 0.6f, c.y), strokeWidth = h * 0.045f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.capsule(w: Float, h: Float, body: Color, accent: Color, outline: Color) {
    val ch = h * 0.38f
    val cw = w * 0.72f
    val left = w * 0.08f
    val top = h * 0.5f - ch / 2
    val radius = CornerRadius(ch / 2)
    drawRoundRect(body, Offset(left, top), Size(cw, ch), radius)
    // 오른쪽 반쪽: 둥근 끝을 포함해 색칠
    val half = androidx.compose.ui.graphics.Path().apply {
        moveTo(left + cw / 2, top)
        lineTo(left + cw - ch / 2, top)
        arcTo(androidx.compose.ui.geometry.Rect(left + cw - ch, top, left + cw, top + ch), -90f, 180f, false)
        lineTo(left + cw / 2, top + ch)
        close()
    }
    drawPath(half, accent)
    if (outline != Color.Transparent) drawRoundRect(outline, Offset(left, top), Size(cw, ch), radius, style = Stroke(h * 0.04f))
}

/** 이 틀이 보이는 동안 상태 표시줄 아이콘(시계·배터리)을 흰색으로. 화면을 떠나면 원래대로(어두운 아이콘) */
@Composable
private fun WhiteStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        onDispose { controller?.isAppearanceLightStatusBars = true }
    }
}
