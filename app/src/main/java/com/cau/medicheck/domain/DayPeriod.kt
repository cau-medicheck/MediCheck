package com.cau.medicheck.domain

import java.time.LocalTime

/**
 * 하루를 넷으로 나눈 시간대. 일정 화면(S05)이 회차를 이 묶음으로 나눠 보여 준다.
 * 경계는 프로토타입과 같다: 0~10시 아침, 11~15시 점심, 16~20시 저녁, 21~23시 밤.
 */
enum class DayPeriod { MORNING, LUNCH, EVENING, NIGHT }

/** 예정 시각이 어느 시간대에 드는지. "시"만 본다 (10:59는 아침, 11:00은 점심). */
fun periodOf(time: LocalTime): DayPeriod = when {
    time.hour < 11 -> DayPeriod.MORNING
    time.hour < 16 -> DayPeriod.LUNCH
    time.hour < 21 -> DayPeriod.EVENING
    else -> DayPeriod.NIGHT
}
