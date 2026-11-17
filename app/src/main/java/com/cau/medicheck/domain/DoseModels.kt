package com.cau.medicheck.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
    UPCOMING,
    MISSED,
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
 * 화면과 로직이 쓰는 복용 기록 하나. 날짜·시각이 글자가 아니라 타입으로 들어 있다.
 * 약 이름·복용량·메모는 기록을 만들 때 찍어 둔 스냅샷 값이다.
 *
 * scheduleId·memo는 1단계(DB 개정)에서 더했다. 기존 화면 코드가 그대로 컴파일되도록 기본값을 두었고,
 * 2단계에서 화면이 회차 열쇠(DoseKey)를 쓰게 되면 정리한다.
 */
data class DoseItem(
    val id: Long,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val date: LocalDate,
    val time: LocalTime,
    val status: DoseStatus,
    /** 복용 완료면 실제 복용 시각, 건너뜀이면 누른 시각(밀리초). 미기록이면 null */
    val actedAt: Long?,
    val scheduleId: Long = 0L,
    val memo: String? = null
) {
    /** 이 기록의 예정 일시 (날짜 + 시각) */
    val scheduledAt: LocalDateTime get() = LocalDateTime.of(date, time)

    /** 이 기록의 회차 열쇠 */
    val key: DoseKey get() = DoseKey(scheduleId, date)

    /**
     * 지금 시각(now)을 기준으로 화면에 보여 줄 상태를 계산한다.
     * PENDING이면서 예정 일시가 지났으면 MISSED, 아직이면 UPCOMING.
     */
    fun displayStatus(now: LocalDateTime): DoseDisplayStatus = when (status) {
        DoseStatus.TAKEN -> DoseDisplayStatus.TAKEN
        DoseStatus.SKIPPED -> DoseDisplayStatus.SKIPPED
        DoseStatus.PENDING ->
            if (scheduledAt.isBefore(now)) DoseDisplayStatus.MISSED else DoseDisplayStatus.UPCOMING
    }
}
