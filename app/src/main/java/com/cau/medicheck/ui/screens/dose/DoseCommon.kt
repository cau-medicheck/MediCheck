package com.cau.medicheck.ui.screens.dose

import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.ui.components.DoseRowUi
import com.cau.medicheck.ui.navigation.SelectionStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/*
 * 회차 화면들(S04·S05·S10·S31)의 ViewModel이 함께 쓰는 작은 도구.
 */

/** 화면의 시계가 다시 도는 간격. "○ 복용 예정"이 시각이 되면 30초 안에 "! 기한 경과"로 바뀐다. */
private const val TICK_MILLIS = 30_000L

/**
 * 30초마다 "지금 시각"을 내보내는 흐름.
 * "기한 경과"와 완료율은 저장된 값이 아니라 지금 시각과 비교해 계산하는 값이라, 시간이 흐르면 다시 계산해야 한다.
 */
fun tickerFlow(clock: Clock): Flow<LocalDateTime> = flow {
    while (true) {
        emit(LocalDateTime.now(clock))
        delay(TICK_MILLIS)
    }
}

/** 회차 → 회차 줄에 그릴 값. 줄에서 바로 복용 완료는 "미기록이고 오늘 이전 날짜"일 때만 된다. */
fun DoseItem.toRowUi(now: LocalDateTime, today: LocalDate) = DoseRowUi(
    key = key,
    name = medicationName,
    time = time,
    dosage = dosage,
    display = displayStatus(now),
    canQuickTake = status == DoseStatus.PENDING && !date.isAfter(today)
)

/**
 * 회차 상세(S10)·실제 복용 시각(S31)이 "어느 회차를 보여 줄지" 정한다. (구현 명세 00 §2.3)
 *
 *   주소에 회차가 적혀 왔으면 → 그 회차. 실제로 없으면 null (화면은 안내 후 뒤로 간다)
 *   주소에 회차가 없으면     → 마지막으로 본 회차 → 없으면 선택 날짜의 첫 회차 → 그것도 없으면 null
 *
 * @param hasArgs 주소에 회차 인자가 붙어 왔는가
 * @param argKey 주소에서 읽은 회차. 인자가 있어도 형식이 틀렸으면 null
 */
suspend fun resolveDoseKey(
    userId: Long,
    hasArgs: Boolean,
    argKey: DoseKey?,
    doseRepository: DoseRepository,
    selection: SelectionStore,
    today: LocalDate
): DoseKey? {
    suspend fun exists(key: DoseKey) = doseRepository.observeDose(userId, key, today).first() != null

    if (hasArgs) return argKey?.takeIf { exists(it) }
    val last = selection.lastDose.value
    if (last != null && exists(last)) return last
    return doseRepository.firstDoseOf(userId, selection.selectedDay.value)
}
