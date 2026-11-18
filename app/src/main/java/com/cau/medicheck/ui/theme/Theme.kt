package com.cau.medicheck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 앱 전체의 색 배합.
 *
 * Material 부품(버튼, 입력칸, 스위치 등)은 여기 정한 이름의 색을 알아서 가져다 쓴다.
 *   primary = 주색(청록), surface = 카드 바탕, onSurface = 그 위의 글자 … 식이다.
 *
 * UI 프로토타입이 밝은 화면 한 가지라서, 폰이 어두운 모드여도 밝은 배합만 쓴다.
 */
private val MediCheckColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = MintText,
    secondary = TealDark,
    onSecondary = Color.White,
    secondaryContainer = Mint,
    onSecondaryContainer = MintText,
    tertiary = Amber,
    background = ScreenBackground,
    onBackground = Navy,
    surface = CardBackground,
    onSurface = Navy,
    surfaceVariant = TabsTrack,
    onSurfaceVariant = Muted,
    surfaceContainerLowest = CardBackground,
    surfaceContainerLow = CardBackground,
    surfaceContainer = CardBackground,
    surfaceContainerHigh = CardBackground,
    surfaceContainerHighest = CardBackground,
    outline = FieldBorder,
    outlineVariant = Line,
    error = Danger,
    onError = Color.White,
    errorContainer = PillRedBg,
    onErrorContainer = PillRedText
)

@Composable
fun MediCheckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MediCheckColors,
        typography = Typography,
        content = content
    )
}
