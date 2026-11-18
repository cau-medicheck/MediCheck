package com.cau.medicheck.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cau.medicheck.MediCheckApplication
import com.cau.medicheck.R
import com.cau.medicheck.data.backup.BackupJson
import com.cau.medicheck.data.export.DocumentFileIo
import com.cau.medicheck.data.repository.BackupCounts
import com.cau.medicheck.data.repository.BackupRepository
import com.cau.medicheck.data.repository.RestoreCheck
import com.cau.medicheck.domain.backup.BackupData
import com.cau.medicheck.ui.components.ToastBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 지금 하고 있는 일. NONE이 아니면 두 버튼 모두 눌리지 않는다. */
enum class BackupBusy { NONE, EXPORTING, READING, RESTORING }

/**
 * 불러온 파일이 검사를 통과해 "덮어쓸까요?"를 묻는 중인 내용.
 * @param medicationCount 파일의 사용 중인 약 수 (확인 창에 보여 준다)
 * @param recordCount 파일의 복용 기록 수
 */
data class PendingRestore(val data: BackupData, val medicationCount: Int, val recordCount: Int)

/**
 * 데이터 백업·복원(S28)이 그릴 상태.
 * @param counts 지금 계정의 약 수·기록 수. 아직 읽는 중이면 null
 * @param pending null이 아니면 덮어쓰기 확인 창을 띄운다
 */
data class BackupUiState(
    val counts: BackupCounts? = null,
    val busy: BackupBusy = BackupBusy.NONE,
    val pending: PendingRestore? = null
)

/** 화면에 따로 내보내지 않는 안쪽 상태 (개수는 DB에서 흘러오므로 여기에 없다) */
private data class BackupWork(val busy: BackupBusy = BackupBusy.NONE, val pending: PendingRestore? = null)

/**
 * 데이터 백업·복원(S28)의 ViewModel.
 *
 * 내보내기:  ① 내용 모으기(BackupRepository.export) → ② JSON 글자로(BackupJson) → ③ 고른 파일에 쓰기(DocumentFileIo)
 * 불러오기:  ① 파일 읽기 → ② JSON 읽기 → ③ 검사(BackupRepository.inspect) → ④ 확인 창 → ⑤ 복원(restore)
 *
 * 파일 쓰기와 복원은 viewModelScope가 아니라 앱 전체의 작업 공간(appScope)에서 돈다.
 * 도중에 이 화면을 떠나도 끝까지 하고, 결과 토스트도 뜨게 하기 위해서다.
 */
class S28BackupViewModel(
    private val userId: Long,
    private val backupRepository: BackupRepository,
    private val fileIo: DocumentFileIo,
    private val toastBus: ToastBus,
    private val appScope: CoroutineScope
) : ViewModel() {

    private val work = MutableStateFlow(BackupWork())

    val uiState: StateFlow<BackupUiState> =
        combine(backupRepository.observeCounts(userId), work) { counts, work ->
            BackupUiState(counts = counts, busy = work.busy, pending = work.pending)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    /**
     * 파일 저장 화면에서 돌아왔을 때 부른다.
     * @param uri 사용자가 고른 파일의 주소. 저장 화면에서 취소했으면 null → 아무것도 하지 않는다.
     */
    fun export(uri: Uri?) {
        if (uri == null || !begin(BackupBusy.EXPORTING)) return
        appScope.launch {
            val ok = try {
                val text = encodeWithinLimit(backupRepository.export(userId))
                // 너무 커서 다시 불러올 수 없는 파일은 만들지 않는다(저장됐다고 해 놓고 복원이 안 되면 안 된다).
                if (text == null) {
                    fileIo.discard(uri)
                    false
                } else {
                    fileIo.write(uri, text)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 내용을 모으다 오류가 나도 앱이 꺼지지 않게 하고, 저장 화면이 만들어 둔 빈 파일은 지운다.
                fileIo.discard(uri)
                false
            } finally {
                finish()
            }
            toastBus.show(if (ok) R.string.s28_toast_exported else R.string.s28_toast_export_failed)
        }
    }

    /**
     * 파일 선택 화면에서 돌아왔을 때 부른다. 파일을 읽고 검사해서, 통과하면 확인 창을 띄운다.
     * 여기서는 DB를 바꾸지 않는다.
     */
    fun onFilePicked(uri: Uri?) {
        if (uri == null || !begin(BackupBusy.READING)) return
        viewModelScope.launch {
            val check = try {
                val text = fileIo.readText(uri, MAX_FILE_BYTES)
                // 큰 파일을 해석하고 검사하는 데는 시간이 걸린다. 화면이 멈추지 않게 계산용 작업 공간에서 한다.
                withContext(Dispatchers.Default) {
                    val data = text?.let(BackupJson::decode)
                    if (data == null) RestoreCheck.Invalid else backupRepository.inspect(userId, data)
                }
            } catch (e: CancellationException) {
                finish()
                throw e
            } catch (e: Exception) {
                RestoreCheck.Invalid
            }
            when (check) {
                is RestoreCheck.Ready -> work.value = BackupWork(
                    pending = PendingRestore(
                        data = check.data,
                        medicationCount = check.data.medications.count { it.deletedAt == null },
                        recordCount = check.data.doseRecords.size
                    )
                )
                RestoreCheck.Invalid -> {
                    finish()
                    toastBus.show(R.string.s28_toast_invalid)
                }
                RestoreCheck.OtherAccount -> {
                    finish()
                    toastBus.show(R.string.s28_toast_other_account)
                }
            }
        }
    }

    /** 확인 창의 "확인": 지금 계정의 데이터를 파일 내용으로 바꾼다. */
    fun confirmRestore() {
        val pending = work.value.pending ?: return
        if (work.value.busy != BackupBusy.NONE) return
        work.value = BackupWork(busy = BackupBusy.RESTORING)
        appScope.launch {
            val ok = try {
                backupRepository.restore(userId, pending.data)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false   // 트랜잭션이 전부 취소되어 원래 데이터가 그대로 남는다
            } finally {
                finish()
            }
            toastBus.show(if (ok) R.string.s28_toast_restored else R.string.s28_toast_restore_failed)
        }
    }

    /** 확인 창의 "취소"·바깥 누름·뒤로: 아무것도 바꾸지 않는다. */
    fun cancelRestore() {
        work.update { if (it.busy == BackupBusy.NONE) BackupWork() else it }
    }

    /** 이 폰에 파일 저장·선택 화면이 없어 열지 못했을 때 (화면이 알려 준다) */
    fun onPickerUnavailable() = toastBus.show(R.string.s28_toast_export_failed)

    /** 일을 시작한다. 이미 다른 일을 하는 중이거나 확인 창이 떠 있으면 false (두 번 눌림을 막는다). */
    private fun begin(busy: BackupBusy): Boolean {
        val current = work.value
        if (current.busy != BackupBusy.NONE || current.pending != null) return false
        work.value = BackupWork(busy = busy)
        return true
    }

    private fun finish() {
        work.value = BackupWork()
    }

    /**
     * 백업 내용을 JSON 글자로 바꾼다. 읽을 때의 상한(MAX_FILE_BYTES)을 넘으면 들여쓰기 없이 다시 만들어 보고,
     * 그래도 넘으면 null (이 앱이 다시 읽지 못하는 파일은 내보내지 않는다).
     */
    private fun encodeWithinLimit(data: BackupData): String? =
        sequenceOf(true, false)
            .map { pretty -> BackupJson.encode(data, pretty) }
            .firstOrNull { it.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES }

    companion object {
        /** 이보다 큰 파일은 백업 파일로 보지 않는다 (10MB. 몇 년 치 기록도 수 MB 수준이다) */
        const val MAX_FILE_BYTES = 10 * 1024 * 1024

        fun factory(userId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MediCheckApplication).container
                S28BackupViewModel(
                    userId, container.backupRepository, container.documentFileIo, container.toastBus, container.appScope
                )
            }
        }
    }
}
