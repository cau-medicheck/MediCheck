package com.cau.medicheck.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 막대 그래프 모양 아이콘 (통계 탭에 쓴다).
 *
 * 기본 아이콘 묶음(material-icons-core)에는 그래프 모양이 없어서 직접 그렸다.
 * 24×24 칸 안에 높이가 다른 막대 세 개를 그린 것이다.
 *   moveTo(x, y): 그 자리로 펜을 옮긴다
 *   lineTo 계열: 그 자리까지 선을 긋는다
 *   close(): 시작한 자리로 돌아가 도형을 닫는다
 */
val BarChartIcon: ImageVector = ImageVector.Builder(
    name = "BarChart",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        // 왼쪽 막대 (중간 높이)
        moveTo(5f, 9.2f)
        horizontalLineTo(8f)
        verticalLineTo(19f)
        horizontalLineTo(5f)
        close()
        // 가운데 막대 (가장 높음)
        moveTo(10.6f, 5f)
        horizontalLineTo(13.4f)
        verticalLineTo(19f)
        horizontalLineTo(10.6f)
        close()
        // 오른쪽 막대 (가장 낮음)
        moveTo(16.2f, 13f)
        horizontalLineTo(19f)
        verticalLineTo(19f)
        horizontalLineTo(16.2f)
        close()
    }
}.build()
