package com.cau.medicheck.ui.screens.settings

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.ProvideBodyFontScale
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.lock.LocalAppLocked
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/** 이 화면에서 누를 수 있는 것들 */
data class BackupActions(
    val onBack: () -> Unit,
    val onExport: (Uri?) -> Unit,
    val onFilePicked: (Uri?) -> Unit,
    val onConfirmRestore: () -> Unit,
    val onCancelRestore: () -> Unit,
    val onPickerUnavailable: () -> Unit
)

/** 파일 선택 화면에 보여 달라고 할 파일 종류. 파일 앱마다 .json 파일을 다른 종류로 표시할 수 있어 셋을 함께 준다. */
private val PICKABLE_TYPES = arrayOf("application/json", "application/octet-stream", "text/plain")

/**
 * S28 데이터 백업·복원 (v3 backupRestore()).
 *
 * 내보내기: 버튼 → 안드로이드의 파일 저장 화면 → 고른 위치에 JSON 파일로 저장.
 * 불러오기: 버튼 → 파일 선택 화면 → 고른 파일을 검사 → "덮어쓸까요?" 확인 창 → 복원.
 * 저장 권한은 필요 없다(사용자가 고른 그 파일 하나만 읽고 쓴다).
 */
@Composable
fun S28BackupScreen(state: BackupUiState, actions: BackupActions) {
    // 파일 저장 화면·파일 선택 화면을 띄우는 도구. 화면에서 돌아오면 고른 파일의 주소(취소했으면 null)를 넘겨준다.
    val saveFile =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json"), actions.onExport)
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), actions.onFilePicked)
    val fileName = stringResource(R.string.s28_export_file_name)
    val idle = state.busy == BackupBusy.NONE && state.pending == null

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s28)) {
        BackLink(stringResource(R.string.common_back_previous), actions.onBack)
        ScreenIntro(modifier = Modifier.padding(vertical = Gap16))

        SurfaceCard {
            // ----- 지금 이 계정의 데이터 -----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.s28_current), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text(
                        // 아직 읽는 중이면 "0개"가 잠깐 보이지 않게 줄표를 둔다.
                        text = state.counts?.let { stringResource(R.string.s28_counts, it.medications, it.records) } ?: "—",
                        fontSize = 12.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Pill(stringResource(R.string.s28_chip_device), PillKind.MINT)
            }

            // ----- 내보내기 -----
            McButton(
                text = stringResource(R.string.s28_export),
                onClick = {
                    // 드물게 파일 저장 화면이 없는 폰이 있다. 그때 앱이 꺼지지 않게 한다.
                    try {
                        saveFile.launch(fileName)   // 저장 화면의 이름 칸에 파일 이름을 미리 넣어 둔다
                    } catch (e: ActivityNotFoundException) {
                        actions.onPickerUnavailable()
                    }
                },
                full = true,
                enabled = idle,
                loading = state.busy == BackupBusy.EXPORTING,
                modifier = Modifier.padding(top = Gap16)
            )

            // ----- 불러오기 -----
            FieldLabel(stringResource(R.string.s28_import_label))
            McButton(
                text = stringResource(R.string.s28_import_pick),
                onClick = {
                    try {
                        pickFile.launch(PICKABLE_TYPES)
                    } catch (e: ActivityNotFoundException) {
                        actions.onPickerUnavailable()
                    }
                },
                kind = ButtonKind.WHITE,
                full = true,
                enabled = idle,
                loading = state.busy == BackupBusy.READING || state.busy == BackupBusy.RESTORING
            )
        }

        Notice(stringResource(R.string.s28_notice), Modifier.padding(top = Gap16), warn = true)
    }

    // ----- 덮어쓰기 확인 창 (불러온 파일이 검사를 통과했을 때만) -----
    // 앱이 잠겨 있는 동안에는 그리지 않는다(잠금 화면 위에 뜨지 않게). 풀면 다시 보인다.
    state.pending?.takeIf { !LocalAppLocked.current }?.let { pending ->
        AlertDialog(
            onDismissRequest = actions.onCancelRestore,   // 바깥을 누르거나 뒤로 가기 = 취소
            // 확인 창은 따로 뜨는 창이라 본문의 글자 배율이 전해지지 않는다. 글자마다 한 번 더 적용한다.
            text = {
                ProvideBodyFontScale {
                    // 글자가 커져 창보다 길어지면 안에서 밀어 볼 수 있게 한다.
                    // 제목도 같이 밀리도록 여기에 둔다(제목이 자리를 다 차지해 경고가 안 보이는 일을 막는다).
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            stringResource(R.string.s28_confirm),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                            modifier = Modifier.padding(bottom = 12.dp).semantics { heading() }
                        )
                        Text(
                            stringResource(R.string.s28_confirm_counts, pending.medicationCount, pending.recordCount),
                            fontSize = 13.sp
                        )
                        Text(
                            stringResource(R.string.s28_confirm_warning),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = actions.onConfirmRestore) {
                    ProvideBodyFontScale { Text(stringResource(R.string.action_ok)) }
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onCancelRestore) {
                    ProvideBodyFontScale { Text(stringResource(R.string.action_cancel)) }
                }
            }
        )
    }
}
