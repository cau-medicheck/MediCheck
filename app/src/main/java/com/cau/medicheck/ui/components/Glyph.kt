package com.cau.medicheck.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.theme.WarnText

/**
 * 아이콘으로 쓰는 기호 글자 모음. (구현 명세 00 §7.1)
 *
 * v3 프로토타입은 아이콘을 그림 파일 대신 글자 하나로 그렸다. 이 기호들은 번역할 "낱말"이 아니므로
 * strings.xml이 아니라 여기에 상수로 둔다. (const val = 바뀌지 않는 값. Java의 static final과 같다)
 * 기호가 문장 안에 들어 있는 문구('✓ 복용 완료' 등)는 통째로 strings.xml에 둔다.
 */
object Glyph {
    const val LOGO = "✚\uFE0E"          // S00·S01·S02 로고
    const val MAIL = "✉\uFE0E"          // S03 로고
    const val PILL = "💊"         // S00 1장 그림
    const val BELL = "bell:on"    // 종. 글자가 아니라 그린 아이콘으로 나온다 (아래 GlyphText)
    const val CHART = "📊"        // S00 3장 그림

    // 머리말 버튼 · 하단 탭 (\uFE0E = 컬러 이모지가 아닌 글자 모양으로 그려 달라는 표시)
    const val HEADER_BELL = BELL   // 머리말 알림 버튼. v3의 ♧ 대신 종
    const val HEADER_SETTINGS = "⚙\uFE0E"
    const val TAB_HOME = "⌂"
    const val TAB_SCHEDULE = "▦"
    const val TAB_MEDS = "▤"
    const val TAB_RECORDS = "▣"
    const val TAB_STATS = "▥"

    // 목록 줄의 왼쪽 아이콘
    const val DOSE = "✚\uFE0E"           // 회차 (복용 완료 전)
    const val DOSE_DONE = "✓"             // 회차 (복용 완료)
    const val HISTORY = "↻"               // 변경 이력

    // 빈 상태·안내의 그림 글자
    const val EMPTY_HOME = "🗓️"          // S04 회차 없음
    const val EMPTY_SCHEDULE = "📅"       // S05 회차 없음
    const val EMPTY_CIRCLE = "○"          // S31 회차 없음
    const val BELL_OFF = "bell:off"       // 알림 꺼짐: 종에 빗금
    const val NOT_FOUND = "📋"            // 약을 찾지 못함 (S07·S08)
    const val WARNING = "⚠️"              // 삭제 확인(S09) · 중복 확인(S40)
    const val UPLOAD = "▧"                // 사진 올리기 상자 (S29)
    // 설정 허브(S19)·프로필(S21)
    const val PROFILE = "☺\uFE0E"             // 프로필 카드
    const val PERMISSION = "♙"            // 알림 권한·OS 줄
    const val HELP = "?"                  // 도움말 줄

    // 가족·보호자 시안(S23)
    const val PERSON = "👤"               // 내 계정
    const val CHILD = "👧"                // 자녀 예시 프로필

    // 빈 화면·오류 상태 예시(S39)
    const val EXAMPLE_EMPTY = "▤"         // 빈 상태
    const val EXAMPLE_INPUT = "⚠\uFE0E"   // 입력 오류
    const val EXAMPLE_OFFLINE = "⌁"       // 오프라인

    const val PREV = "‹"                  // 이전 달
    const val NEXT = "›"                  // 다음 달
}

/**
 * 기호 하나를 그린다. 대부분은 글자 그대로 쓰고, 종(Glyph.BELL·BELL_OFF)만 그린 아이콘으로 바꿔 그린다.
 *
 * 종만 따로 그리는 이유: v3는 알림을 ♧(머리말)·🔔·🔕 세 가지 글자로 나타냈는데, 모양이 서로 다르고
 * 이모지는 폰마다 다르게 나와서 종 하나로 통일했다.
 *
 * @param fontSize 크기(sp). 종도 같은 크기로 그려서 글자 배율을 키우면 함께 커진다
 * @param color 글자색. 정하지 않으면 글자는 원래 색 그대로, 종은 청록(꺼짐은 주황)
 */
@Composable
fun GlyphText(
    symbol: String,
    fontSize: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    if (symbol != Glyph.BELL && symbol != Glyph.BELL_OFF) {
        Text(text = symbol, fontSize = fontSize.sp, color = color, fontWeight = fontWeight, modifier = modifier)
        return
    }
    val off = symbol == Glyph.BELL_OFF
    val tint = if (color != Color.Unspecified) color else if (off) WarnText else Teal
    val sizeDp = with(LocalDensity.current) { fontSize.sp.toDp() }
    Icon(
        imageVector = Icons.Outlined.Notifications,
        contentDescription = null,   // 장식. 읽을 이름은 감싸는 쪽이 붙인다
        tint = tint,
        modifier = modifier
            .size(sizeDp)
            .drawWithContent {
                drawContent()
                // 꺼짐: 왼쪽 위 → 오른쪽 아래로 빗금을 긋는다
                if (off) {
                    val inset = size.minDimension * 0.14f
                    drawLine(
                        color = tint,
                        start = Offset(inset, inset),
                        end = Offset(size.width - inset, size.height - inset),
                        strokeWidth = size.minDimension * 0.09f,
                        cap = StrokeCap.Round
                    )
                }
            }
    )
}
