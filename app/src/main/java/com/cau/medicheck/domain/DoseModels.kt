package com.cau.medicheck.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * DB에 저장되는 복용 상태 세 가지. (docs/DB_DESIGN.md ⑩)
 * 표의 status 칸에는 이 이름 그대로("PENDING" 등) 글자로 들어간다.
 */
enum class DoseStatus {
    PENDING,   // 예정. 아직 응답 없음
    TAKEN,     // 복용 완료
    SKIPPED    // 규칙은 유지한 채 일부러 건너뜀
}

/**
 * 화면에 보여 주는 복용 상태 네 가지.
 *
 * 저장된 상태(DoseStatus)와 다른 점: PENDING을 "아직 시간 전(UPCOMING)"과
 * "시간이 지났는데 응답 없음 = 깜빡함(MISSED)"으로 나눈다.
 * MISSED는 DB에 저장하지 않고, 볼 때마다 지금 시각과 비교해서 계산한다.
 */
enum class DoseDisplayStatus {
    UPCOMING,   // ○ 복용 예정
    MISSED,     // ! 기한 경과 · 미기록
    TAKEN,
    SKIPPED
}

/**
 * 회차 하나를 가리키는 열쇠: (규칙 번호, 날짜).
 * dose_records 표의 UNIQUE(schedule_id, date)와 같은 뜻이다. 기록 줄이 아직 없는
 * 미래 회차도 이 열쇠로 가리킬 수 있어서, 화면 이동·알림에서 기록 번호 대신 쓴다. (DB_DESIGN ⑥)
 */
data class DoseKey(val scheduleId: Long, val date: LocalDate)

/**
 * 화면과 로직이 쓰는 회차 하나. 날짜·시각이 글자가 아니라 타입으로 들어 있다.
 *
 * 두 가지가 이 모양으로 온다.
 *   - 오늘·지난 날짜: DB에 저장된 기록(dose_records). 이름·복용량·메모는 기록을 만들 때 찍어 둔 스냅샷.
 *   - 내일 이후: 아직 기록이 없어서 지금의 약·규칙으로 계산한 "예정 회차". recordId가 null이다.
 */
data class DoseItem(
    /** 저장된 기록의 번호. 내일 이후의 예정 회차는 null */
    val recordId: Long?,
    val scheduleId: Long,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    /** 복용 메모. 없으면 "" */
    val memo: String,
    val date: LocalDate,
    /** 예정 시각 */
    val time: LocalTime,
    val status: DoseStatus,
    /** 복용 완료면 실제 복용 시각, 건너뜀이면 누른 시각(밀리초). 미기록이면 null */
    val actedAt: Long?,
    /** 복용 조건 (기록의 스냅샷 또는 지금 규칙 값) */
    val condition: DoseCondition = DoseCondition.NONE
) {
    /** 이 회차의 예정 일시 (날짜 + 시각) */
    val scheduledAt: LocalDateTime get() = LocalDateTime.of(date, time)

    /** 이 회차의 열쇠 */
    val key: DoseKey get() = DoseKey(scheduleId, date)

    /**
     * 도래했는가 = 예정 일시가 지금(분 단위) 이하인가. (DB_DESIGN ⑩)
     * 예정 시각 "정각"부터 도래한 것으로 본다. 배지와 완료율이 모두 이 함수 하나를 쓴다.
     */
    fun isDue(now: LocalDateTime): Boolean =
        !scheduledAt.isAfter(now.truncatedTo(ChronoUnit.MINUTES))

    /**
     * 미복용으로 볼 때가 됐는가 = 예정 시각 + 여유(MISSED_GRACE_MINUTES)가 지금(분 단위) 이하인가.
     * 정각이 지나자마자 "미복용"이 뜨면 재촉하는 느낌이라 여유를 둔다(2026-11-23 결정, 30분).
     * 알림은 이 값과 상관없이 예정 시각 정각에 울린다.
     */
    fun isOverdue(now: LocalDateTime): Boolean =
        !scheduledAt.plusMinutes(MISSED_GRACE_MINUTES).isAfter(now.truncatedTo(ChronoUnit.MINUTES))

    /**
     * 지금 시각(now)을 기준으로 화면에 보여 줄 상태를 계산한다.
     * 미기록(PENDING)이면서 여유 시간까지 지났으면 MISSED(미복용), 아니면 UPCOMING(예정).
     */
    fun displayStatus(now: LocalDateTime): DoseDisplayStatus = when (status) {
        DoseStatus.TAKEN -> DoseDisplayStatus.TAKEN
        DoseStatus.SKIPPED -> DoseDisplayStatus.SKIPPED
        DoseStatus.PENDING -> if (isOverdue(now)) DoseDisplayStatus.MISSED else DoseDisplayStatus.UPCOMING
    }

    companion object {
        /** 예정 시각이 지나고 몇 분 뒤부터 "미복용"으로 보는가. 바꾸려면 이 숫자 하나만 고친다. */
        const val MISSED_GRACE_MINUTES = 30L
    }

    /**
     * 화면에 "실제 복용 HH:mm"으로 보여 줄 시각. 보여 주지 않을 때는 null.
     * 복용 완료이고, 누른(또는 적은) 시각의 날짜가 회차 날짜와 같을 때만 보여 준다.
     * (3일 전 회차를 오늘 복용 완료로 바꾼 경우, 오늘 누른 시각이 실제 복용 시각처럼 보이면 안 되기 때문)
     */
    fun actualTimeOrNull(zone: ZoneId): LocalTime? {
        if (status != DoseStatus.TAKEN || actedAt == null) return null
        val at = Instant.ofEpochMilli(actedAt).atZone(zone)
        return if (at.toLocalDate() == date) at.toLocalTime().truncatedTo(ChronoUnit.MINUTES) else null
    }
}
