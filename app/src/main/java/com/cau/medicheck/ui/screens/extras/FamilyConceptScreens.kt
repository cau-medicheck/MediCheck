package com.cau.medicheck.ui.screens.extras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.R
import com.cau.medicheck.ui.components.WarnBadge
import com.cau.medicheck.ui.components.Avatar
import com.cau.medicheck.ui.components.BackLink
import com.cau.medicheck.ui.components.ButtonKind
import com.cau.medicheck.ui.components.FieldLabel
import com.cau.medicheck.ui.components.FormField
import com.cau.medicheck.ui.components.Glyph
import com.cau.medicheck.ui.components.Kicker
import com.cau.medicheck.ui.components.McButton
import com.cau.medicheck.ui.components.McScreenColumn
import com.cau.medicheck.ui.components.Notice
import com.cau.medicheck.ui.components.OptionRow
import com.cau.medicheck.ui.components.Pill
import com.cau.medicheck.ui.components.PillKind
import com.cau.medicheck.ui.components.ProgressBar
import com.cau.medicheck.ui.components.ScreenTitle
import com.cau.medicheck.ui.components.SectionTitle
import com.cau.medicheck.ui.components.SettingRow
import com.cau.medicheck.ui.components.SurfaceCard
import com.cau.medicheck.ui.navigation.Routes
import com.cau.medicheck.ui.theme.Gap16
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.ParagraphText
import com.cau.medicheck.ui.theme.SettingDivider
import com.cau.medicheck.ui.theme.Teal

/*
 * 가족·보호자 기능의 "시안" 화면 다섯 개 (S23~S27).
 *
 * 이 기능들은 이번 릴리스에서 만들지 않기로 한(Won’t) 것이라, 모양만 프로토타입대로 보여 준다. 그래서
 *   - "(미지원)" 버튼은 "이번 릴리스에서 제외된 기능 시안입니다." 안내만 띄운다 (onUnsupported)
 *   - 입력값(이메일·체크·라디오)은 화면 안에서만 바뀌고 어디에도 저장하지 않는다 (remember만 쓴다.
 *     화면을 떠났다 돌아오면 처음 모양이다)
 *   - 이름·날짜·횟수는 프로토타입의 예시 글자 그대로다. 로그인한 사람의 실제 이름이나 기록과 잇지 않는다
 *     (실제 값을 넣으면 이 기능이 동작하는 것처럼 보인다)
 * 데이터가 없으므로 ViewModel도 없다. 다섯 화면이 짧고 같은 규칙을 따라 한 파일에 모았다.
 */

/** 다섯 화면이 함께 쓰는 머리 부분: "‹ 이전 화면" + 큰 제목 + 설명 */
@Composable
private fun ConceptLead(title: String, subtitle: String, onBack: () -> Unit) {
    BackLink(stringResource(R.string.common_back_previous), onBack)
    ScreenTitle(title = title, subtitle = subtitle, modifier = Modifier.padding(vertical = Gap16))
}

/** 다섯 화면 맨 아래의 주황 안내: "이 화면은 디자인 검토용입니다. …" (사실이므로 그대로 둔다) */
@Composable
private fun ConceptNotice() {
    Notice(stringResource(R.string.extra_notice_future), Modifier.padding(top = 14.dp), warn = true)
}

/** 카드 안의 작은 설명 글자 (v3 .paragraph) */
@Composable
private fun Paragraph(text: String, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 21.sp,
        color = ParagraphText,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        modifier = modifier
    )
}

// =============================================================================================
// S23 가족 프로필
// =============================================================================================

/**
 * S23 가족 프로필 (v3 familyProfiles()).
 * 가족·보호자 시안 묶음의 첫 화면. 아래 "관련 화면" 네 줄로 나머지 시안(S24~S27)에 들어간다.
 *
 * @param onOpen 관련 화면 줄을 눌렀을 때 그 화면의 주소(route)를 넘겨받는 함수
 */
@Composable
fun S23FamilyProfilesScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onUnsupported: () -> Unit) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s23)) {
        ConceptLead(stringResource(R.string.screen_title_s23), stringResource(R.string.s23_desc), onBack)

        SurfaceCard {
            ProfileRow(Glyph.PERSON, R.string.s23_me_name, R.string.s23_me_sub, R.string.s23_me_badge, PillKind.MINT)
        }
        SurfaceCard(Modifier.padding(top = Gap16)) {
            ProfileRow(
                Glyph.CHILD, R.string.s23_child_name, R.string.s23_child_sub, R.string.s23_child_badge, PillKind.ORANGE
            )
            McButton(
                text = stringResource(R.string.s23_btn_add),
                onClick = onUnsupported,
                kind = ButtonKind.WHITE,
                full = true,
                modifier = Modifier.padding(top = Gap16)
            )
        }

        SectionTitle(stringResource(R.string.s23_section_related))
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                OptionRow(
                    stringResource(R.string.s23_row_consent_title), stringResource(R.string.s23_row_consent_desc),
                    onClick = { onOpen(Routes.S24) }
                )
                OptionRow(
                    stringResource(R.string.s23_row_dashboard_title), stringResource(R.string.s23_row_dashboard_desc),
                    onClick = { onOpen(Routes.S25) }
                )
                OptionRow(
                    stringResource(R.string.s23_row_actor_title), stringResource(R.string.s23_row_actor_desc),
                    onClick = { onOpen(Routes.S26) }
                )
                OptionRow(
                    stringResource(R.string.s23_row_double_title), stringResource(R.string.s23_row_double_desc),
                    onClick = { onOpen(Routes.S27) }
                )
            }
        }
        ConceptNotice()
    }
}

/** 프로필 한 줄: 그림 글자 + 이름·설명 + 오른쪽 배지 */
@Composable
private fun ProfileRow(symbol: String, nameRes: Int, subRes: Int, badgeRes: Int, badgeKind: PillKind) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(symbol)   // 그림 글자는 장식이라 TalkBack이 읽지 않는다 (Avatar가 처리)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(nameRes), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(stringResource(subRes), fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 5.dp))
        }
        Spacer(Modifier.width(10.dp))
        Pill(stringResource(badgeRes), badgeKind)
    }
}

// =============================================================================================
// S24 보호자 연결 동의
// =============================================================================================

/**
 * S24 보호자 연결 동의 (v3 caregiverConsent()).
 * 이메일 칸과 체크 상자는 눌러 볼 수 있지만 저장되지 않고, 버튼은 안내만 띄운다(체크하지 않아도 눌린다).
 */
@Composable
fun S24CaregiverConsentScreen(onBack: () -> Unit, onUnsupported: () -> Unit) {
    // remember: 이 화면이 보이는 동안만 기억한다. 일부러 rememberSaveable을 쓰지 않았다(어디에도 남기지 않는다).
    var email by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s24)) {
        ConceptLead(stringResource(R.string.s24_title), stringResource(R.string.s24_desc), onBack)

        SurfaceCard {
            Kicker(stringResource(R.string.s24_kicker))
            Text(
                text = stringResource(R.string.s24_card_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Navy,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            Paragraph(stringResource(R.string.s24_card_desc))

            val label = stringResource(R.string.s24_label_email)
            FieldLabel(label)
            FormField(
                value = email,
                onValueChange = { email = it },
                placeholder = stringResource(R.string.s24_hint_email),
                label = label,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done)
            )
            CheckRow(
                text = stringResource(R.string.s24_check),
                checked = agreed,
                onCheckedChange = { agreed = it },
                modifier = Modifier.padding(top = Gap16)
            )
            McButton(
                text = stringResource(R.string.s24_btn_request),
                onClick = onUnsupported,
                full = true,
                modifier = Modifier.padding(top = Gap16)
            )
        }

        SurfaceCard(Modifier.padding(top = Gap16)) {
            Text(stringResource(R.string.s24_linked_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
            Paragraph(stringResource(R.string.s24_linked_none), Modifier.padding(top = 9.dp))
        }
        ConceptNotice()
    }
}

/** 체크 상자 + 글자 한 줄. 줄 어디를 눌러도 켜지고 꺼진다. TalkBack은 "…, 체크박스, 선택 안 됨"처럼 한 번에 읽는다. */
@Composable
private fun CheckRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
    ) {
        // 상자는 그림으로만 쓴다(null = 터치를 받지 않음). 누르는 처리는 줄 전체가 한다.
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = Teal, uncheckedColor = Muted)
        )
        Spacer(Modifier.width(10.dp))
        Text(text = text, fontSize = 11.sp, lineHeight = 16.sp, color = Navy)
    }
}

// =============================================================================================
// S25 보호자 대시보드
// =============================================================================================

/** 예시 기록 한 줄: 제목, 시각, 완료했는지 */
private data class ExampleDose(val titleRes: Int, val time: String, val done: Boolean)

private val EXAMPLE_DOSES = listOf(
    ExampleDose(R.string.s25_row_morning, "08:00", done = true),
    ExampleDose(R.string.s25_row_lunch, "12:30", done = true),
    ExampleDose(R.string.s25_row_evening, "21:00", done = false)
)

/**
 * S25 보호자 대시보드 (v3 caregiverDashboard()).
 * 누를 것이 없는 정적 시안이다. 이름·날짜·횟수 모두 프로토타입의 예시 글자다.
 */
@Composable
fun S25CaregiverDashboardScreen(onBack: () -> Unit) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s25)) {
        ConceptLead(stringResource(R.string.s25_title), stringResource(R.string.s25_desc), onBack)

        SurfaceCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.s25_name), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
                Pill(stringResource(R.string.s25_badge), PillKind.ORANGE)
            }
            Paragraph(stringResource(R.string.s25_synced), Modifier.padding(top = 8.dp))
            Text(
                text = stringResource(R.string.s25_metric),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = Navy,
                modifier = Modifier.padding(top = Gap16)
            )
            // 위의 "2 / 3회"와 같은 뜻의 막대라 TalkBack이 따로 읽지 않게 한다.
            ProgressBar(fraction = 2f / 3f, modifier = Modifier.padding(top = 6.dp).clearAndSetSemantics { })
        }

        SectionTitle(stringResource(R.string.s25_section))
        SurfaceCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                EXAMPLE_DOSES.forEachIndexed { index, dose ->
                    SettingRow(
                        title = stringResource(dose.titleRes),
                        description = dose.time,
                        showDivider = index != EXAMPLE_DOSES.lastIndex,
                        trailing = {
                            Pill(
                                stringResource(if (dose.done) R.string.s25_done else R.string.s25_pending),
                                if (dose.done) PillKind.MINT else PillKind.ORANGE
                            )
                        }
                    )
                }
            }
        }
        ConceptNotice()
    }
}

// =============================================================================================
// S26 자녀 복용 기록자
// =============================================================================================

private val GUARDIANS = listOf(R.string.s26_mom, R.string.s26_dad, R.string.s26_other)

/**
 * S26 자녀 복용 기록자 (v3 childActor()).
 * 라디오(여러 개 중 하나만 고르는 동그란 단추) 세 개 중 하나를 고를 수 있지만 저장되지 않는다. 처음 선택은 "엄마".
 */
@Composable
fun S26ChildActorScreen(onBack: () -> Unit, onUnsupported: () -> Unit) {
    var selected by remember { mutableIntStateOf(0) }

    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s26)) {
        ConceptLead(stringResource(R.string.screen_title_s26), stringResource(R.string.s26_desc), onBack)

        SurfaceCard {
            Text(stringResource(R.string.s26_card_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
            Paragraph(stringResource(R.string.s26_question), Modifier.padding(top = 8.dp))
            // selectableGroup: TalkBack에게 "이 셋은 하나만 고르는 묶음"이라고 알려 준다.
            Column(Modifier.selectableGroup()) {
                GUARDIANS.forEachIndexed { index, labelRes ->
                    GuardianRow(stringResource(labelRes), selected == index) { selected = index }
                }
            }
            McButton(
                text = stringResource(R.string.s26_btn_save),
                onClick = onUnsupported,
                full = true,
                modifier = Modifier.padding(top = Gap16)
            )
        }
        ConceptNotice()
    }
}

/** 글자가 왼쪽, 라디오가 오른쪽인 줄. 줄 전체가 고르는 단추다. (공용 RadioRow는 라디오가 왼쪽이라 따로 만들었다) */
@Composable
private fun GuardianRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Column(Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 13.dp)) {
            Text(text = label, fontSize = 16.sp, color = Navy, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            RadioButton(
                selected = selected,
                onClick = null,   // 누르는 처리는 줄 전체가 한다
                colors = RadioButtonDefaults.colors(selectedColor = Teal, unselectedColor = Muted)
            )
        }
        HorizontalDivider(thickness = 1.dp, color = SettingDivider)
    }
}

// =============================================================================================
// S27 가족 중복 투약 경고
// =============================================================================================

/**
 * S27 가족 중복 투약 경고 (v3 doubleDose()).
 * 프로토타입의 "돌아가기"는 눌러도 아무 일이 없었다. 앱에서는 눌러도 반응이 없는 버튼을 두지 않으려고
 * 이전 화면으로 돌아가게 했다.
 */
@Composable
fun S27DoubleDoseScreen(onBack: () -> Unit, onUnsupported: () -> Unit) {
    McScreenColumn(paneTitle = stringResource(R.string.screen_title_s27)) {
        ConceptLead(stringResource(R.string.s27_title), stringResource(R.string.s27_desc), onBack)

        SurfaceCard(padding = 24.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                WarnBadge()
                Text(
                    text = stringResource(R.string.s27_card_title),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 11.dp)
                )
                Paragraph(stringResource(R.string.s27_card_desc), center = true)
                Spacer(Modifier.height(Gap16))
                McButton(stringResource(R.string.s27_btn_check), onUnsupported, kind = ButtonKind.LIGHT, full = true)
                Spacer(Modifier.height(8.dp))
                McButton(stringResource(R.string.s27_btn_back), onBack, kind = ButtonKind.WHITE, full = true)
            }
        }

        ConceptNotice()
    }
}
