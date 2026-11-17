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

    // 머리말 버튼 · 하단 탭 (\uFE0E = 컬러 이모지가 아닌 글자 모양으로 그려 달라는 표시)
    const val HEADER_BELL = "♧\uFE0E"
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
    const val BELL_OFF = "🔕"             // 알림 꺼짐 카드
    const val PLACEHOLDER = "🛠️"         // 임시 화면 "준비 중"
    const val NOT_FOUND = "📋"            // 약을 찾지 못함 (S07·S08)
    const val WARNING = "⚠️"              // 삭제 확인(S09) · 중복 확인(S40)
    const val UPLOAD = "▧"                // 사진 올리기 상자 (S29)
}
