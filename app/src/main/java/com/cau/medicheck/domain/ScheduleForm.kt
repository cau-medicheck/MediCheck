package com.cau.medicheck.domain

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * 약 등록·수정 폼(S08)의 복용 시간 한 줄.
 * @param id 이미 저장돼 있던 규칙이면 그 id, 이번에 새로 추가한 줄이면 null
 * @param condition 이 시간의 복용 조건 (기본: 상관없음)
 */
data class FormTimeRow(val id: Long?, val time: LocalTime, val condition: DoseCondition = DoseCondition.NONE)

/**
 * 폼은 "시간 여러 개 + 요일 한 묶음"이지만, DB는 시간마다 요일을 따로 저장할 수 있다.
 * 예전에 "08:00 매일 + 20:00 월·수·금"처럼 저장된 약을 열었을 때 요일이 몰래 바뀌지 않게 두 가지 모드를 둔다.
 *
 *   통일 모드 (originalDays == null): 모든 줄에 화면에서 고른 요일(days)을 넣는다.
 *   혼합 모드 (originalDays != null): 원래 있던 줄은 자기 요일 그대로, 새로 추가한 줄만 days(= 원래 요일들의 합집합).
 *
 * 혼합 모드는 "불러온 줄들의 요일이 서로 다르고, 사용자가 아직 요일 버튼을 누르지 않았을 때"다.
 * 요일 버튼을 한 번이라도 누르면 통일 모드가 된다(부르는 쪽이 originalDays를 null로 넘긴다).
 *
 * @param originalDays 규칙 id → 그 규칙의 원래 요일
 */
fun toScheduleDrafts(
    rows: List<FormTimeRow>,
    days: Set<DayOfWeek>,
    originalDays: Map<Long, Set<DayOfWeek>>?
): List<ScheduleDraft> = rows.map { row ->
    val own = row.id?.let { originalDays?.get(it) }
    ScheduleDraft(id = row.id, time = row.time, days = own ?: days, condition = row.condition)
}

/** 불러온 규칙들의 요일이 서로 다른가 (= 혼합 모드로 열어야 하는가) */
fun hasMixedDays(schedules: List<ScheduleItem>): Boolean = schedules.map { it.days }.distinct().size > 1
