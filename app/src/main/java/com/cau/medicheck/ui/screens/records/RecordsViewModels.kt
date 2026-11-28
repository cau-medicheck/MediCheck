package com.cau.medicheck.ui.screens.records

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.R
import com.cau.medicheck.data.export.DocumentFileIo
import com.cau.medicheck.data.repository.DoseRepository
import com.cau.medicheck.domain.CsvExporter
import com.cau.medicheck.domain.DoseCounts
import com.cau.medicheck.domain.StatsCalculator
import com.cau.medicheck.ui.components.ToastBus
import com.cau.medicheck.ui.navigation.SelectionStore
import com.cau.medicheck.ui.screens.dose.ClockViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale

// =============================================================================================
// S11 기록 캘린더
// =============================================================================================

/**
 * 기록 캘린더(S11)가 그릴 상태.
 * @param month 달력에 보이는 달
 * @param day 고른 날짜 (달력의 강조 칸, 아래 요약 카드의 날짜)
 * @param datesWithDoses 점을 찍을 날짜들 = 회차가 하나라도 있는 날
 * @param counts 고른 날짜의 완료·건너뜀·미기록 수 (도래한 회차만)
 */
data class RecordsCalendarUiState(
    val month: YearMonth,
    val day: LocalDate,
    val isLoading: Boolean = true,
    val datesWithDoses: Set<LocalDate> = emptySet(),
    val counts: DoseCounts = DoseCounts()
)

/**
 * 기록 캘린더(S11)의 ViewModel.
 *
 * 보는 달과 고른 날짜를 스스로 갖지 않고 SelectionStore의 값을 읽고 쓴다.
 * 그래서 여기서 고른 날짜가 홈·일정에도, 여기서 넘긴 달이 월간 통계에도 그대로 보인다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordsCalendarViewModel(
    userId: Long,
    doseRepository: DoseRepository,
    private val selection: SelectionStore,
    clock: Clock = Clock.systemDefaultZone()
) : ClockViewModel(userId, doseRepository, clock) {

    /** 달(또는 오늘)이 바뀌면 그 달의 "회차가 있는 날짜"를 다시 구독한다. */
    private val dots = combine(selection.selectedMonth, today) { month, today -> month to today }
        .flatMapLatest { (month, today) -> doseRepository.observeDatesWithDoses(userId, month, today) }

    /** 고른 날짜(또는 오늘)가 바뀌면 그날의 회차를 다시 구독한다. */
    private val dayDoses = combine(selection.selectedDay, today) { day, today -> day to today }
        .flatMapLatest { (day, today) -> doseRepository.observeDay(userId, day, today) }

    val uiState: StateFlow<RecordsCalendarUiState> =
        combine(selection.selectedMonth, selection.selectedDay, dots, dayDoses, now) { month, day, dots, doses, now ->
            RecordsCalendarUiState(
                month = month,
                day = day,
                isLoading = false,
                datesWithDoses = dots,
                counts = StatsCalculator.count(doses, now)
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RecordsCalendarUiState(selection.selectedMonth.value, selection.selectedDay.value)
        )

    /** 날짜 칸: 그 날짜를 고른다 (보는 달도 그 날짜의 달이 된다). */
    fun selectDay(date: LocalDate) = selection.selectDay(date)

    /** ‹ ›: 달만 넘긴다. 고른 날짜와 요약 카드는 그대로다. */
    fun shiftMonth(delta: Long) = selection.shiftMonth(delta)

    companion object {
        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                RecordsCalendarViewModel(userId, container.doseRepository, container.selection, container.clock)
            }
        }
    }
}

// =============================================================================================
// S43 복약 기록 내보내기
// =============================================================================================

/**
 * 복약 기록 내보내기(S43)가 그릴 상태.
 * @param pastDays 오늘보다 앞선 날짜 중 기록이 있는 날짜 수 ("과거 스냅샷 날짜"). 아직 읽는 중이면 null
 * @param isExporting 파일을 쓰는 중 (버튼에 돌아가는 표시, 두 번 눌리지 않게)
 */
data class CsvExportUiState(val pastDays: Int? = null, val isExporting: Boolean = false)

/**
 * 복약 기록 내보내기(S43)의 ViewModel.
 *
 * 내보내기 순서: ① 오늘까지 기록 채우기 → ② 내보낼 회차 모으기 → ③ CSV 글자 만들기 → ④ 고른 파일에 쓰기.
 * 글자를 만드는 일은 CsvExporter(순수 로직), 파일에 쓰는 일은 DocumentFileIo가 한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CsvExportViewModel(
    private val userId: Long,
    private val doseRepository: DoseRepository,
    private val documentFileIo: DocumentFileIo,
    private val toastBus: ToastBus,
    private val appScope: CoroutineScope,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now(clock))
    private val exporting = MutableStateFlow(false)

    val uiState: StateFlow<CsvExportUiState> =
        combine(today.flatMapLatest { doseRepository.observePastDateCount(userId, it) }, exporting) { days, busy ->
            CsvExportUiState(pastDays = days, isExporting = busy)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CsvExportUiState())

    /** 화면이 다시 보일 때: 오늘을 다시 읽고 기록을 오늘까지 채운다. */
    fun refresh() {
        today.value = LocalDate.now(clock)
        viewModelScope.launch { doseRepository.refresh(userId) }
    }

    /**
     * 파일 저장 화면에서 돌아왔을 때 부른다.
     * @param uri 사용자가 고른 파일의 주소. 저장 화면에서 취소했으면 null → 아무것도 하지 않는다(토스트도 없음).
     */
    fun export(uri: Uri?) {
        if (uri == null || exporting.value) return
        exporting.value = true
        // viewModelScope가 아니라 앱 전체의 작업 공간에서 돈다. 저장 중에 이 화면을 떠나도 끝까지 쓰고,
        // 결과 토스트도 뜨게 하기 위해서다(토스트는 앱 맨 바깥에 있어 어느 화면에서나 보인다).
        appScope.launch {
            val ok = try {
                doseRepository.refresh(userId)
                val now = LocalDateTime.now(clock)
                // 파일의 머리줄·상태 글자는 기기 언어를 따른다
                val korean = Locale.getDefault().language == "ko"
                val rows = CsvExporter.sorted(doseRepository.exportItems(userId))
                    .map { CsvExporter.toRow(it, now, clock.zone, korean) }
                documentFileIo.write(uri, CsvExporter.build(rows, korean))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 기록을 모으다 오류가 나도 앱이 꺼지지 않게 하고, 저장 화면이 만들어 둔 빈 파일은 지운다.
                documentFileIo.discard(uri)
                false
            } finally {
                exporting.value = false   // 오류가 나도 버튼이 잠긴 채로 남지 않게
            }
            toastBus.show(if (ok) R.string.s43_toast_saved else R.string.s43_toast_failed)
        }
    }

    /** 이 폰에 파일 저장 화면이 없어 열지 못했을 때 (화면이 알려 준다) */
    fun onSaveScreenUnavailable() = toastBus.show(R.string.s43_toast_failed)

    companion object {
        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                CsvExportViewModel(
                    userId, container.doseRepository, container.documentFileIo, container.toastBus, container.appScope,
                    clock = container.clock
                )
            }
        }
    }
}
