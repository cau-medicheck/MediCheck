package com.cau.medicheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.ui.theme.ChartLabel
import com.cau.medicheck.ui.theme.KickerText
import com.cau.medicheck.ui.theme.MetricText
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/*
 * 통계 화면의 숫자·그래프 부품. (구현 명세 04 공통 작업 5)
 * 그래프 라이브러리를 쓰지 않고 상자(Box)의 높이로 막대를 그린다.
 */

/**
 * 막대 하나를 그리는 데 필요한 값.
 * @param percent 0~100. 막대 높이는 이 값에 비례한다. 계산할 것이 없는 날(N/A)은 null — 가장 낮은 막대로 그린다
 * @param topLabel 막대 위 글자 ("80%", "N/A"). 없으면 그리지 않는다
 * @param bottomLabel 막대 아래 글자 ("월"). 없으면 그리지 않는다
 * @param description TalkBack이 읽을 말 ("화요일 100%")
 * @param brush 막대의 색(또는 그라데이션)
 */
class BarColumnUi(
    val percent: Int?,
    val topLabel: String?,
    val bottomLabel: String?,
    val description: String,
    val brush: Brush
)

/**
 * 세로 막대 그래프. 막대들이 아래쪽에 맞춰 나란히 선다. (v3 .chart)
 *
 * 높이를 고정하지 않는다: "가장 높은 막대(maxBarHeight) + 위·아래 글자 높이"가 곧 그래프 높이다.
 * 그래서 글자를 크게 써도 글자가 잘리지 않는다.
 *
 * @param maxBarHeight 100%일 때의 막대 높이
 * @param gap 막대 사이 간격
 */
@Composable
fun BarChart(columns: List<BarColumnUi>, maxBarHeight: Dp, gap: Dp, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 10.dp)
    ) {
        columns.forEach { column ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    // 위 글자·막대·아래 글자를 한 덩어리로, 설명 한 마디로 읽게 한다.
                    .clearAndSetSemantics { contentDescription = column.description }
            ) {
                if (column.topLabel != null) {
                    Text(
                        text = column.topLabel,
                        fontSize = 9.sp,
                        color = ChartLabel,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        // 글자를 크게 써서 칸보다 넓어져도 줄을 바꾸거나 자르지 않고 그대로 그린다.
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
                // 막대: 최소 3dp(0%나 N/A도 자리가 보이게), 최대 maxBarHeight
                val barHeight = maxOf(3.dp, maxBarHeight * ((column.percent ?: 0) / 100f))
                Box(
                    Modifier
                        .widthIn(max = 27.dp)
                        .fillMaxWidth()
                        .height(barHeight)
                        .background(
                            column.brush,
                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                        )
                )
                if (column.bottomLabel != null) {
                    Text(
                        text = column.bottomLabel,
                        fontSize = 9.sp,
                        color = ChartLabel,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    }
}

/** 한 가지 색으로 칠하는 막대의 brush */
fun solidBar(color: Color): Brush = SolidColor(color)

/** 통계 화면의 큰 숫자 (v3 .metric: 굵기 900, 자간 -1). 뒤에 작은 단위("회")를 붙일 수 있다. */
@Composable
fun MetricNumber(text: String, fontSize: Int, modifier: Modifier = Modifier, suffix: String? = null) {
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
            color = MetricText
        )
        if (suffix != null) {
            Text(
                text = suffix,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = MetricText,
                modifier = Modifier.padding(start = 2.dp, bottom = 7.dp)
            )
        }
    }
}

/**
 * 이름 + 숫자 묶음 세 칸 (v3 .row3). 예: 완료 16 / 건너뜀 3 / 미기록 1
 * @param valueSize 숫자 글자 크기(sp)
 */
@Composable
fun CountTriple(items: List<Pair<String, Int>>, modifier: Modifier = Modifier, valueSize: Int = 18) {
    Row(modifier = modifier.fillMaxWidth()) {
        items.forEach { (label, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 11.sp, color = Muted)
                Text(
                    text = value.toString(),
                    fontSize = valueSize.sp,
                    fontWeight = FontWeight.Black,
                    color = Navy,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}

/** 카드 맨 위의 작은 머리글 (v3 .kicker: 10px, 굵기 900, 자간 넓게) */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.8.sp,
        color = KickerText,
        modifier = modifier
    )
}
