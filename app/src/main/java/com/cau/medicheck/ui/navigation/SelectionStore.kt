package com.cau.medicheck.ui.navigation

import com.cau.medicheck.domain.DoseKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/**
 * 여러 화면이 함께 쓰는 "지금 고른 것"을 기억하는 곳. (구현 명세 00 §3.10)
 *
 * 프로토타입은 선택 날짜 하나를 홈·일정·회차 상세가 같이 쓴다(홈에서 내일을 고르고 일정 탭에 가면 내일).
 * 화면마다 ViewModel이 따로라서, 그 값을 앱 전체에 하나뿐인 이곳(AppContainer.selection)에 둔다.
 * DB가 아니라 메모리에만 있다. 앱을 새로 켜면 오늘에서 시작한다.
 *
 * StateFlow = 지금 값 하나를 들고 있고, 값이 바뀌면 지켜보는 쪽에 알려 주는 통로.
 */
class SelectionStore(private val clock: Clock = Clock.systemDefaultZone()) {

    private val _selectedDay = MutableStateFlow(LocalDate.now(clock))
    /** 선택한 날짜 (S04·S05·S10·S30·S31이 함께 본다) */
    val selectedDay: StateFlow<LocalDate> = _selectedDay.asStateFlow()

    private val _selectedMonth = MutableStateFlow(YearMonth.now(clock))
    /** 달력에서 보고 있는 달 (4단계의 기록 캘린더·월간 통계가 쓴다) */
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _lastDose = MutableStateFlow<DoseKey?>(null)
    /** 마지막으로 연 회차. 회차 상세(S10)가 "어느 회차인지" 없이 열렸을 때 이것을 보여 준다. */
    val lastDose: StateFlow<DoseKey?> = _lastDose.asStateFlow()

    /** 마지막으로 확인한 "오늘". 자정이 지났는지 알아내는 데 쓴다. */
    private var knownToday: LocalDate = LocalDate.now(clock)

    /** 날짜를 고른다. 보고 있는 달도 그 날짜의 달로 함께 바꾼다(달력과 선택 날짜가 어긋나지 않게). */
    fun selectDay(date: LocalDate) {
        _selectedDay.value = date
        _selectedMonth.value = YearMonth.from(date)
    }

    /** 달만 앞뒤로 옮긴다. 선택 날짜는 그대로. */
    fun shiftMonth(delta: Long) {
        _selectedMonth.value = _selectedMonth.value.plusMonths(delta)
    }

    fun setLastDose(key: DoseKey) {
        _lastDose.value = key
    }

    /**
     * 자정이 지나 "오늘"이 바뀌었을 때 부른다.
     * 선택 날짜가 옛 오늘이었으면 새 오늘로 따라간다. 다른 날짜를 보고 있었으면 그대로 둔다.
     */
    fun followToday(today: LocalDate) {
        if (today == knownToday) return
        if (_selectedDay.value == knownToday) selectDay(today)
        knownToday = today
    }

    /** 로그인·로그아웃 때 처음 값으로. (앞 사람이 보던 날짜·회차가 다음 사람에게 남지 않게) */
    fun reset() {
        knownToday = LocalDate.now(clock)
        selectDay(knownToday)
        _lastDose.value = null
    }
}
