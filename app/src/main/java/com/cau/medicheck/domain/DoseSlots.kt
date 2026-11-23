package com.cau.medicheck.domain

import java.time.Duration
import java.time.LocalTime

/**
 * 홈 맨 위 카드가 처음 보여 줄 "시각 묶음"을 고른다. (2026-11-23 결정, 시안: 04_화면캡처/홈카드_묶음기록_시안/종합_시안.html)
 *
 * 묶음 = 같은 시각에 먹는 약들. 예: 08:00(약 3개) · 12:30(1개) · 21:00(1개)
 *
 * 고르는 순서:
 *  1. 지금과 가장 가까운 시각. 두 시각의 한가운데를 넘으면 다음 시각이 더 가깝다.
 *     예: 08:00과 12:30 사이라면 10:15 전까지는 08:00, 10:15부터는 12:30.
 *  2. 그 묶음을 모두 기록했으면, 그 뒤에서 기록 안 한 약이 남은 첫 묶음.
 *  3. 뒤에도 없으면, 앞쪽에서 기록 안 한 약이 남은 묶음 중 가장 늦은 것(지금과 가장 가까운 것).
 *  4. 기록 안 한 약이 하나도 없으면 -1 (화면은 "오늘의 복약을 모두 기록했어요").
 *
 * 화면(안드로이드)과 상관없는 순수 계산이라 단위 테스트로 확인한다(DoseSlotsTest).
 */
object DoseSlots {

    /**
     * @param times 오늘의 묶음 시각들. 이른 순서이고 겹치지 않는다.
     * @param hasPending 묶음마다 "아직 기록 안 한 약이 있는가" (times와 같은 길이)
     * @param now 지금 시각
     * @return 고른 묶음의 번호(times의 위치). 고를 것이 없으면 -1
     */
    fun pick(times: List<LocalTime>, hasPending: List<Boolean>, now: LocalTime): Int {
        require(times.size == hasPending.size) { "times와 hasPending의 길이가 다르다" }
        if (hasPending.none { it }) return -1

        // 1. 가장 가까운 시각. 거리가 같으면(딱 한가운데) 뒤 시각을 고른다 → "한가운데를 넘으면 다음 시각"
        var nearest = 0
        times.forEachIndexed { i, time ->
            if (distance(time, now) <= distance(times[nearest], now)) nearest = i
        }
        if (hasPending[nearest]) return nearest

        // 2. 뒤에서 기록 안 한 약이 남은 첫 묶음
        val later = (nearest + 1 until times.size).firstOrNull { hasPending[it] }
        if (later != null) return later

        // 3. 앞에서 기록 안 한 약이 남은 묶음 중 가장 늦은 것
        return (nearest - 1 downTo 0).first { hasPending[it] }
    }

    /** 두 시각 사이의 거리(분). 하루 안에서만 비교한다(23:50과 00:10은 23시간 40분 떨어진 것으로 본다). */
    private fun distance(a: LocalTime, b: LocalTime): Long = Math.abs(Duration.between(a, b).toMinutes())
}
