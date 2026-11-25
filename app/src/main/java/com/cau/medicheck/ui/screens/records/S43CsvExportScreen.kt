package com.cau.medicheck.ui.screens.records

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.components.HeroHeader
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.Icons
import com.cau.medicheck.domain.CsvExporter
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.Kicker
import com.cau.medicheck.ui.components.LabelValueRow
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.ScreenIntro
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.theme.Navy

/**
 * S43 복약 기록 내보내기 (v3 csvExport()).
 *
 * "CSV 다운로드"를 누르면 안드로이드의 파일 저장 화면이 열린다. 거기서 저장할 위치를 고르면
 * 복약 기록 전부(+ 앞으로 7일의 예정 회차)가 CSV 파일로 저장된다. 저장 권한은 필요 없다.
 *
 * @param onExport 저장 화면에서 돌아왔을 때 부른다. 고른 파일의 주소가 넘어오고, 취소했으면 null
 * @param onSaveScreenUnavailable 이 폰에 파일 저장 화면이 없어 열지 못했을 때 부른다
 */
@Composable
fun S43CsvExportScreen(
    state: CsvExportUiState,
    onBack: () -> Unit,
    onExport: (Uri?) -> Unit,
    onSaveScreenUnavailable: () -> Unit
) {
    // 파일 저장 화면을 띄우는 도구. "text/csv" 종류의 새 파일을 만들 위치를 사용자가 고른다.
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv"), onExport)

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s43)) {
        BackLink(stringResource(R.string.common_back_previous), onBack)
        ScreenIntro(modifier = Modifier.padding(vertical = 16.dp))

        // 2026-11-25 다시 그림: 가운데 아이콘 + 무엇을 내보내는지 한 줄 + 단추 + 작은 주의 한 줄
        HeroHeader(
            icon = Icons.Rounded.FileDownload,
            color = Teal,
            title = stringResource(R.string.s43_format),
            subtitle = stringResource(R.string.s43_target) + (state.pastDays?.let { " · " + stringResource(R.string.s43_days, it) } ?: "")
        )
        McButton(
            text = stringResource(R.string.s43_btn_download),
            onClick = {
                try {
                    saveFile.launch(CsvExporter.FILE_NAME)
                } catch (e: ActivityNotFoundException) {
                    onSaveScreenUnavailable()
                }
            },
            full = true,
            loading = state.isExporting
        )
        Notice(stringResource(R.string.s43_notice), Modifier.padding(top = 14.dp))
    }
}
