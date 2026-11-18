package com.cau.medicheck.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.cau.medicheck.data.local.A11ySettings

/*
 * 접근성 설정 세 가지(글자 배율·높은 대비·움직임 최소화)를 화면에 적용하는 장치. (구현 명세 00 §6.1~6.3)
 *
 * 설정 값은 AppPrefs(폰 단위 저장소)에 있고, MediCheckApp(맨 바깥)이 그 값을 읽어
 * 아래 CompositionLocal에 넣어 둔다.
 *   CompositionLocal = 화면 트리 아래의 모든 부품이 "인자로 받지 않고도" 꺼내 쓸 수 있게 맨 위에서 건네 두는 값.
 *   설정 하나 때문에 모든 화면 함수에 인자를 하나씩 더 붙이지 않으려고 쓴다.
 *
 * 값을 바꾸는 화면(S37 접근성 설정)은 6단계에서 만든다. 지금은 "적용하는 장치"만 있다.
 */

/** 지금 적용 중인 접근성 설정. Preview처럼 MediCheckApp 밖에서 그릴 때는 기본값(100%, 끔, 끔)이 쓰인다. */
val LocalA11ySettings = compositionLocalOf { A11ySettings() }

/**
 * 움직임 최소화가 켜졌는지. 움직이는 부품(스위치 손잡이 등)은 이 값이 true면 애니메이션 없이 바로 바뀐다.
 * staticCompositionLocalOf: 값이 거의 바뀌지 않을 때 쓰는 가벼운 판. (바뀌면 아래 화면 전체를 다시 그림)
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** 본문 글자 배율의 상한. F084가 확인하는 한계가 200%라서 폰 글꼴 설정과 곱해도 2배를 넘지 않게 한다. */
const val MAX_FONT_SCALE = 2f

/**
 * 폰의 글꼴 배율(systemScale)과 앱 글자 배율(percent)을 합친 최종 글자 배율. 순수 계산이라 단위 테스트한다.
 *
 * - 둘을 곱한 뒤 2배에서 자른다.
 * - 단, 폰 설정 자체보다 작아지지는 않게 한다(폰에서 이미 크게 쓰는 사람의 글자를 줄이지 않기 위해).
 */
fun effectiveFontScale(systemScale: Float, percent: Int): Float {
    val combined = (systemScale * percent / 100f).coerceAtMost(MAX_FONT_SCALE)
    return maxOf(systemScale, combined)
}

/**
 * CSS `filter: contrast(amount)`와 같은 계산을 하는 색 행렬의 숫자 20개.
 * 새 값 = 값 × amount + (1 − amount) × 127.5  (0~255 기준. amount = 1.2면 값 × 1.2 − 25.5)
 * 색 토큰을 바꾸지 않고 "그려진 결과"에 필터를 씌우므로 모든 화면에 한 번에 적용된다.
 */
fun contrastMatrixValues(amount: Float): FloatArray {
    val shift = (1f - amount) * 127.5f   // 0~255의 가운데(127.5)를 기준으로 늘린다
    return floatArrayOf(
        amount, 0f, 0f, 0f, shift,
        0f, amount, 0f, 0f, shift,
        0f, 0f, amount, 0f, shift,
        0f, 0f, 0f, 1f, 0f   // 투명도는 그대로
    )
}

/** v3의 높은 대비 값: `filter: contrast(1.2)` */
const val HIGH_CONTRAST_AMOUNT = 1.2f

/**
 * on이면 이 부품이 그린 결과 전체에 대비 필터를 씌운다.
 * saveLayer: 안쪽 내용을 따로 한 장에 그린 뒤, 그 장을 필터를 거쳐 화면에 붙이는 방법.
 */
fun Modifier.highContrast(on: Boolean): Modifier =
    if (!on) this else drawWithContent {
        val paint = Paint().apply {
            colorFilter = ColorFilter.colorMatrix(ColorMatrix(contrastMatrixValues(HIGH_CONTRAST_AMOUNT)))
        }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), paint)
            drawContent()
            canvas.restore()
        }
    }

/**
 * 본문(머리말·하단 탭·토스트를 뺀 화면 내용)에 글자 배율을 적용한다.
 * dp 크기는 그대로 두고 sp(글자 단위)만 커지도록, Density의 fontScale만 바꿔 아래로 건넨다.
 * 높은 대비는 Modifier.highContrast로 따로 건다 (레이아웃을 감싸는 부품이 직접 붙인다).
 */
@Composable
fun ProvideBodyFontScale(content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val percent = LocalA11ySettings.current.fontScalePercent
    val scaled = Density(base.density, effectiveFontScale(base.fontScale, percent))
    CompositionLocalProvider(LocalDensity provides scaled, content = content)
}

/**
 * McScreenColumn을 쓰지 않는 화면 묶음에 글자 배율·높은 대비를 한꺼번에 적용하는 상자.
 *
 * 1단계 동안 로그인 뒤 화면(오늘·내 약·기록·통계)은 아직 예전 화면이라 McScreenColumn이 없다.
 * 그래서 그 묶음 전체(MainNavHost)를 이 상자로 감싸 설정이 "앱 전체"에 적용되게 한다.
 * 2단계에서 로그인 뒤 화면이 McScreenColumn으로 바뀌면 이 상자는 빼야 한다(두 번 적용되지 않게).
 */
@Composable
fun A11yBody(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val highContrast = LocalA11ySettings.current.highContrast
    ProvideBodyFontScale {
        androidx.compose.foundation.layout.Box(modifier = modifier.highContrast(highContrast)) { content() }
    }
}
