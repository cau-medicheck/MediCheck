package com.cau.medicheck.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.OptionRow
import com.cau.medicheck.ui.components.ScreenTitle
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Gap22
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy

/** 설정 허브의 한 줄: 왼쪽 기호(없을 수도 있음) + 제목 + 설명 + 누르면 갈 화면 */
private data class HubRow(val symbol: ImageVector?, val titleRes: Int, val descRes: Int, val route: String)

/** "알림 및 앱" 카드의 줄들 (v3 settings()의 첫 카드) */
private val APP_ROWS = listOf(
    HubRow(Glyph.BELL, R.string.s19_row_noti_title, R.string.s19_row_noti_desc, Routes.S17),
    HubRow(Glyph.PERMISSION, R.string.s19_row_perm_title, R.string.s19_row_perm_desc, Routes.S18),
    HubRow(Glyph.POLICY, R.string.s19_row_policy_title, R.string.s19_row_policy_desc, Routes.S20),
    HubRow(Glyph.HELP, R.string.s19_row_help_title, R.string.s19_row_help_desc, Routes.S22)
)

/** "안전·편의 기능" 카드의 줄들. */
private val SAFETY_ROWS = listOf(
    HubRow(null, R.string.s19_row_a11y_title, R.string.s19_row_a11y_desc, Routes.S37),
    HubRow(null, R.string.s19_row_privacy_title, R.string.s19_row_privacy_desc, Routes.S36),
    HubRow(null, R.string.s19_row_offline_title, R.string.s19_row_offline_desc, Routes.S38),
    HubRow(null, R.string.s19_row_archive_title, R.string.s19_row_archive_desc, Routes.S32)
)

/** "프로젝트 정보" 카드의 줄들. 앱을 쓰는 데 필요한 설정이 아니라 과제 자료(요구사항 목록·기능 시안)라 따로 묶는다. */
private val PROJECT_ROWS = listOf(
    HubRow(null, R.string.s19_row_reqmap_title, R.string.s19_row_reqmap_desc, Routes.S45),
    HubRow(null, R.string.s19_row_family_title, R.string.s19_row_family_desc, Routes.S23)
)

/**
 * S19 설정 허브 (v3 settings()).
 *
 * 머리말의 ⚙를 누르면 오는 화면. 나머지 설정 화면들로 들어가는 입구이고, 로그아웃이 여기에만 있다.
 * 값을 들고 있지 않아서 ViewModel이 없다. 이름·이메일은 로그인한 사용자(user)에서 그대로 받는다.
 *
 * @param onOpen 줄을 눌렀을 때 그 화면의 주소(route)를 넘겨 받아 여는 함수
 * @param loggingOut 로그아웃 처리 중인지 (버튼을 두 번 누르지 못하게 한다)
 */
@Composable
fun S19SettingsScreen(
    displayName: String,
    email: String,
    onOpen: (String) -> Unit,
    onLogout: () -> Unit,
    loggingOut: Boolean = false
) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s19)) {
        ScreenTitle(
            title = stringResource(R.string.s19_title),
            subtitle = stringResource(R.string.s19_subtitle),
            modifier = Modifier.padding(bottom = Gap16)
        )

        // ----- 프로필 카드 -----
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(Glyph.PROFILE, size = 43.dp, fontSize = 23)
                Spacer(Modifier.width(10.dp))
                // weight(1f): 버튼 자리를 뺀 나머지 너비를 차지한다. 이름이 길면 잘리지 않고 줄을 바꾼다.
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.s19_name, displayName),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text(text = email, fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 5.dp))
                }
                Spacer(Modifier.width(10.dp))
                val editLabel = stringResource(R.string.s19_cd_edit)
                McButton(
                    text = stringResource(R.string.s19_edit),
                    onClick = { onOpen(Routes.S21) },
                    kind = ButtonKind.LIGHT,
                    small = true,
                    // TalkBack은 "수정 ›" 대신 "프로필 수정"이라고 읽는다.
                    modifier = Modifier.semantics { contentDescription = editLabel }
                )
            }
        }

        SectionTitle(stringResource(R.string.s19_section_app))
        HubCard(APP_ROWS, onOpen)

        SectionTitle(stringResource(R.string.s19_section_safety))
        HubCard(SAFETY_ROWS, onOpen)

        SectionTitle(stringResource(R.string.s19_section_project))
        HubCard(PROJECT_ROWS, onOpen)

        Spacer(Modifier.height(Gap22))
        McButton(
            text = stringResource(R.string.action_logout),
            onClick = onLogout,
            kind = ButtonKind.WHITE,
            full = true,
            enabled = !loggingOut
        )
    }
}

/** 줄 여러 개를 카드 하나에 담는다. 마지막 줄 아래에는 선을 긋지 않는다. */
@Composable
private fun HubCard(rows: List<HubRow>, onOpen: (String) -> Unit) {
    SurfaceCard(padding = 0.dp) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
            rows.forEachIndexed { index, row ->
                OptionRow(
                    title = stringResource(row.titleRes),
                    description = stringResource(row.descRes),
                    onClick = { onOpen(row.route) },
                    showDivider = index != rows.lastIndex,
                    leading = row.symbol?.let { symbol -> { Avatar(symbol, size = 33.dp, fontSize = 17) } }
                )
            }
        }
    }
}
