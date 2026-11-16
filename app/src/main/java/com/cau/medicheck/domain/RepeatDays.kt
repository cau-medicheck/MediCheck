package com.cau.medicheck.domain

import java.time.DayOfWeek

/**
 * 반복 요일을 DB에 넣는 글자와 코드에서 쓰는 요일 묶음 사이에서 바꿔 준다.
 *
 *   DB 글자:      "MON,WED,FRI"
 *   코드의 묶음:   setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
 *
 * DayOfWeek는 Java가 기본으로 주는 요일 타입이다 (MONDAY ~ SUNDAY).
 */
object RepeatDays {

    /** 월요일부터 일요일까지 순서대로. 화면에 요일 버튼을 그릴 때도 이 순서를 쓴다. */
    val ALL: List<DayOfWeek> = DayOfWeek.entries

    val EVERY_DAY: Set<DayOfWeek> = ALL.toSet()

    private val codes: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.MONDAY to "MON",
        DayOfWeek.TUESDAY to "TUE",
        DayOfWeek.WEDNESDAY to "WED",
        DayOfWeek.THURSDAY to "THU",
        DayOfWeek.FRIDAY to "FRI",
        DayOfWeek.SATURDAY to "SAT",
        DayOfWeek.SUNDAY to "SUN"
    )

    private val daysByCode: Map<String, DayOfWeek> = codes.entries.associate { (day, code) -> code to day }

    /** 요일 묶음 → "MON,WED,FRI". 어떤 순서로 넣어도 월요일부터 순서대로 적는다. */
    fun encode(days: Set<DayOfWeek>): String =
        ALL.filter { it in days }.joinToString(",") { codes.getValue(it) }

    /** "MON,WED,FRI" → 요일 묶음. 모르는 글자는 무시한다. */
    fun decode(text: String): Set<DayOfWeek> =
        text.split(",")
            .mapNotNull { daysByCode[it.trim().uppercase()] }
            .toSet()
}
