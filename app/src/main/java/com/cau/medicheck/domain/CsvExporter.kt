package com.cau.medicheck.domain

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 복약 기록을 CSV 글자로 만든다. (S43 복약 기록 내보내기)
 *
 * CSV = 쉼표로 칸을 나눈 표 파일. 엑셀·구글 시트에서 열 수 있다.
 * 순수 Kotlin 코드라서 단위 테스트를 한다. 파일에 쓰는 일은 CsvFileWriter가 한다.
 *
 * 머리줄과 상태 글자는 화면 문구가 아니라 "파일의 내용"이라서 strings.xml이 아니라 여기에 상수로 둔다.
 */
object CsvExporter {

    /** 첫 줄(머리줄) */
    val HEADER = listOf("날짜", "약 이름", "복용량", "예정시각", "상태", "실제복용시각")

    /** 파일 저장 화면에 미리 넣어 둘 파일 이름 */
    const val FILE_NAME = "MediCheck_medication_records.csv"

    /**
     * 파일 맨 앞에 붙이는 표시(BOM). 이것이 있어야 엑셀이 한글을 깨지지 않게 읽는다.
     * 눈에 안 보이는 글자라서 코드에 직접 넣지 않고 \uFEFF로 적는다(직접 넣으면 복사하다 소리 없이 빠질 수 있다).
     */
    private const val BOM = "\uFEFF"

    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * 줄들을 CSV 글자 하나로 만든다.
     * 모든 칸을 큰따옴표로 감싸고(칸 안에 쉼표·줄바꿈이 있어도 칸이 밀리지 않는다), 줄 끝은 \r\n(엑셀과 맞는 줄바꿈).
     */
    fun build(rows: List<List<String>>): String =
        BOM + (listOf(HEADER) + rows).joinToString("\r\n") { row -> row.joinToString(",") { quote(it) } }

    /** 칸 하나를 큰따옴표로 감싼다. 칸 안의 큰따옴표는 두 개로 적는 것이 CSV의 약속이다. */
    private fun quote(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    /**
     * 회차 하나 → CSV 한 줄.
     *   상태: 복용 완료 / 건너뜀 / 기한 경과 · 미기록(미기록이고 도래함) / 복용 예정(미기록이고 아직) — 내보내는 순간 기준
     *   실제복용시각: 복용 완료이고 그날 누른(적은) 시각이 있을 때만. 화면(S10)과 같은 판정을 쓴다.
     */
    fun toRow(item: DoseItem, now: LocalDateTime, zone: ZoneId): List<String> = listOf(
        item.date.toString(),
        safeText(item.medicationName),
        safeText(item.dosage),
        item.time.format(TIME_FORMAT),
        when (item.displayStatus(now)) {
            DoseDisplayStatus.TAKEN -> "복용 완료"
            DoseDisplayStatus.SKIPPED -> "건너뜀"
            DoseDisplayStatus.MISSED -> "기한 경과 · 미기록"
            DoseDisplayStatus.UPCOMING -> "복용 예정"
        },
        item.actualTimeOrNull(zone)?.format(TIME_FORMAT).orEmpty()
    )

    /**
     * 사용자가 자유롭게 적은 글자(약 이름·복용량)가 엑셀에서 "수식"으로 읽히지 않게 한다.
     * 엑셀·구글 시트는 = + - @ 로 시작하는 칸을 수식으로 본다(예: 복용량 "-1정"이 오류로 보임).
     * 그런 칸 앞에 작은따옴표를 붙이면 글자로 읽힌다.
     */
    private fun safeText(value: String): String =
        if (value.firstOrNull() in FORMULA_STARTS) "'$value" else value

    private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')

    /** 내보낼 순서: 날짜 → 예정 시각 → 약 이름(대소문자 무시) */
    fun sorted(items: List<DoseItem>): List<DoseItem> =
        items.sortedWith(
            compareBy<DoseItem> { it.date }
                .thenBy { it.time }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.medicationName }
        )
}
