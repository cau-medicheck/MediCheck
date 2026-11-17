package com.cau.medicheck.ui.components

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
    const val BELL = "🔔"         // S00 2장 그림
    const val CHART = "📊"        // S00 3장 그림
}
