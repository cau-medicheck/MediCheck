package com.cau.medicheck.ui.screens.dose

import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.domain.DoseItem
import com.cau.medicheck.domain.DoseKey
import com.cau.medicheck.domain.DoseStatus
import com.cau.medicheck.ui.components.DoseRowUi
import com.cau.medicheck.ui.navigation.SelectionStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    canQuickTake = status == DoseStatus.PENDING && !date.isAfter(today),
    memo = memo
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

/**
 * "지금 시각"이 필요한 화면의 ViewModel들이 물려받는 부모 클래스. (기록 캘린더·통계 화면)
 *
 * 완료율과 "기한 경과"는 저장된 값이 아니라 지금 시각과 비교해 계산하는 값이다. 그래서
 *   - 30초마다 지금 시각을 다시 읽고 (now)
 *   - 화면이 다시 보일 때도 바로 다시 읽고 (refresh — 앱을 밤새 뒤에 두었다 열었을 때를 위해)
 *   - 자정이 지나면 새 날의 기록을 만든다.
 * 자식 클래스는 now와 today를 자기 화면 상태에 엮어 쓰기만 하면 된다.
 *
 * (abstract class = 그대로는 만들 수 없고, 물려받아 써야 하는 클래스. Java의 abstract class와 같다)
 */
abstract class ClockViewModel(
    protected val userId: Long,
    protected val doseRepository: DoseRepository,
    protected val clock: Clock
) : ViewModel() {

    /** 지금 시각. 30초마다, 그리고 refresh() 때 새 값이 들어온다. */
    protected val now = MutableStateFlow(LocalDateTime.now(clock))

    /** 오늘 날짜. 자정이 지날 때만 값이 바뀐다. */
    protected val today: Flow<LocalDate> = now.map { it.toLocalDate() }.distinctUntilChanged()

    init {
        viewModelScope.launch { tickerFlow(clock).collect { now.value = it } }
        // drop(1): 처음 값(지금의 오늘)은 건너뛰고, 날짜가 "바뀔 때"만 새 날의 기록을 만든다.
        viewModelScope.launch { today.drop(1).collect { doseRepository.refresh(userId) } }
        refresh()
    }

    /** 화면이 다시 보일 때마다 부른다: 지금 시각을 다시 읽고, 비어 있는 날짜의 기록을 오늘까지 채운다. */
    fun refresh() {
        now.value = LocalDateTime.now(clock)
        viewModelScope.launch { doseRepository.refresh(userId) }
    }
}
