package com.cau.medicheck.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.ui.theme.AuthLogoEnd
import com.cau.medicheck.ui.theme.AuthLogoStart
import com.cau.medicheck.ui.theme.AvatarBg
import com.cau.medicheck.ui.theme.AvatarText
import com.cau.medicheck.ui.theme.CardBackground
import com.cau.medicheck.ui.theme.CardBorder
import com.cau.medicheck.ui.theme.EmptyBodyText
import com.cau.medicheck.ui.theme.Faint
import com.cau.medicheck.ui.theme.FieldBorder
import com.cau.medicheck.ui.theme.FieldFocus
import com.cau.medicheck.ui.theme.FieldText
import com.cau.medicheck.ui.theme.LabelText
import com.cau.medicheck.ui.theme.LightButtonBg
import com.cau.medicheck.ui.theme.MintText
import com.cau.medicheck.ui.theme.Muted
import com.cau.medicheck.ui.theme.Navy
import com.cau.medicheck.ui.theme.NoticeBg
import com.cau.medicheck.ui.theme.NoticeBorder
import com.cau.medicheck.ui.theme.NoticeText
import com.cau.medicheck.ui.theme.ParagraphText
import com.cau.medicheck.ui.theme.PillBlueBg
import com.cau.medicheck.ui.theme.PillBlueText
import com.cau.medicheck.ui.theme.PillGrayBg
import com.cau.medicheck.ui.theme.PillGrayText
import com.cau.medicheck.ui.theme.PillMintBg
import com.cau.medicheck.ui.theme.PillMintText
import com.cau.medicheck.ui.theme.PillOrangeBg
import com.cau.medicheck.ui.theme.PillOrangeText
import com.cau.medicheck.ui.theme.PillRedBg
import com.cau.medicheck.ui.theme.PillRedText
import com.cau.medicheck.ui.theme.ProgressFill
import com.cau.medicheck.ui.theme.ProgressTrack
import com.cau.medicheck.ui.theme.RedButtonBg
import com.cau.medicheck.ui.theme.RedButtonBorder
import com.cau.medicheck.ui.theme.RedButtonText
import com.cau.medicheck.ui.theme.RowDivider
import com.cau.medicheck.ui.theme.RowSubText
import com.cau.medicheck.ui.theme.SettingDivider
import com.cau.medicheck.ui.theme.SwitchOff
import com.cau.medicheck.ui.theme.SwitchOn
import com.cau.medicheck.ui.theme.TabsSelectedText
import com.cau.medicheck.ui.theme.TabsText
import com.cau.medicheck.ui.theme.TabsTrack
import com.cau.medicheck.ui.theme.Teal
import com.cau.medicheck.ui.theme.WarnBg
import com.cau.medicheck.ui.theme.WarnBorder
import com.cau.medicheck.ui.theme.WarnText
import com.cau.medicheck.ui.theme.WhiteButtonBorder
import com.cau.medicheck.ui.theme.WhiteButtonText

/*
 * 앱의 모든 화면이 함께 쓰는 작은 부품 모음. (Mc = MediCheck)
 *
 * UI 프로토타입에서 반복해서 나오는 모양(카드, 버튼, 배지, 안내 상자 등)을 하나씩 부품으로 만들었다.
 * 화면 코드는 이 부품들을 조립만 하므로 짧아지고, 모양을 고칠 때는 여기 한 곳만 고치면 된다.
 *
 * 구현 명세 00 §5.3의 "작은 수정"을 반영했다:
 *  - 색은 모두 Color.kt의 이름을 쓴다(#12). 부품 안에 Color(0xFF…)를 직접 쓰지 않는다.
 *  - 글자가 커져도(접근성 200%) 잘리지 않게 버튼·목록 제목의 줄 제한을 풀었다(#3·#4).
 *  - TalkBack(화면 읽어 주기)을 위해 제목은 heading, 기호(‹ ✚)는 읽지 않게, 탭은 '탭'과 선택 여부를 읽게 했다(#7·#13·#14).
 */

// ---------------------------------------------------------------------------------------------
// 글자
// ---------------------------------------------------------------------------------------------

/** 화면 맨 위의 큰 제목과 그 아래 설명 한 줄 (v3 header(): .title + .paragraph) */
@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Navy,
            lineHeight = 31.sp,
            letterSpacing = (-0.9).sp,
            // heading: TalkBack에서 '제목'으로 읽혀, 제목 단위로 건너뛸 수 있다.
            modifier = Modifier.semantics { heading() }
        )
        if (subtitle != null) {
            Spacer(Modifier.height(7.dp))
            Text(text = subtitle, fontSize = 12.sp, color = ParagraphText, lineHeight = 21.sp)
        }
    }
}

/**
 * 화면 맨 위의 설명 한 줄. 화면 이름은 머리말에 있으므로, 본문은 이 화면에서 알아야 할 것만 짧게 적는다.
 * 설명이 필요 없는 화면은 이 부품을 쓰지 않는다.
 */
@Composable
fun ScreenIntro(text: String, modifier: Modifier = Modifier) {
    Text(text = text, fontSize = 12.sp, color = ParagraphText, lineHeight = 21.sp, modifier = modifier)
}

/** 화면 안에서 구역을 나누는 작은 제목. 오른쪽에 덧붙일 글자(개수 등)를 넣을 수 있다. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, suffix: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(top = 28.dp, bottom = 12.dp)) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Navy,
            letterSpacing = (-0.4).sp,
            modifier = Modifier.semantics { heading() }
        )
        if (suffix != null) {
            Spacer(Modifier.width(6.dp))
            Text(text = suffix, fontSize = 11.sp, color = Muted)
        }
    }
}

/** 입력칸 위에 붙는 이름표 */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = LabelText,
        modifier = modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

/**
 * "‹ 돌아가기" 모양의 글자 버튼.
 * 보이는 글자에는 "‹ "를 붙이지만, TalkBack은 기호를 빼고 label만 읽는다(예: "로그인 화면, 버튼").
 */
@Composable
fun BackLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LinkText(text = "‹ $label", onClick = onClick, modifier = modifier, spokenText = label)
}

/**
 * 청록색 글자로 된 버튼 (밑줄 없는 링크).
 * @param spokenText TalkBack이 읽을 글자. 기본은 보이는 글자에서 ‹ › 기호를 뺀 것.
 */
@Composable
fun LinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    spokenText: String = text.replace("‹", "").replace("›", "").trim()
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Teal,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick)
            // contentDescription이 있으면 TalkBack은 보이는 글자 대신 이 글자를 읽는다.
            .semantics { contentDescription = spokenText }
            .padding(vertical = 5.dp)   // v3 .link{padding:5px 0}
    )
}

// ---------------------------------------------------------------------------------------------
// 카드 · 배지 · 안내 상자
// ---------------------------------------------------------------------------------------------

/** 흰 바탕에 옅은 테두리가 있는 둥근 카드. 화면의 내용 덩어리를 담는다. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(padding),
        content = content
    )
}

/** 배지의 색 종류 */
enum class PillKind(val background: Color, val text: Color) {
    MINT(PillMintBg, PillMintText),
    ORANGE(PillOrangeBg, PillOrangeText),
    GRAY(PillGrayBg, PillGrayText),
    BLUE(PillBlueBg, PillBlueText),
    RED(PillRedBg, PillRedText)
}

/** 작은 알약 모양의 배지. 상태("✓ 복용 완료")나 분류("추가 기능")를 표시한다. 짧은 글자라 1줄로 둔다. */
@Composable
fun Pill(text: String, kind: PillKind = PillKind.MINT, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = kind.text,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(kind.background)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    )
}

/**
 * 옅은 색 바탕의 안내 상자.
 * @param warn true면 주황색(주의), false면 민트색(일반 안내)
 */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier, warn: Boolean = false) {
    Notice(text = AnnotatedString(text), modifier = modifier, warn = warn)
}

/** 굵은 글자가 섞인 안내 상자 (AnnotatedString = 글자 일부에 굵기·색을 따로 줄 수 있는 문자열) */
@Composable
fun Notice(text: AnnotatedString, modifier: Modifier = Modifier, warn: Boolean = false) {
    val background = if (warn) WarnBg else NoticeBg
    val border = if (warn) WarnBorder else NoticeBorder
    val color = if (warn) WarnText else NoticeText
    Text(
        text = text,
        fontSize = 11.sp,
        lineHeight = 19.sp,
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(12.dp)
    )
}

/**
 * 둥근 네모 상자 안에 아이콘 하나를 넣은 것. 목록 줄의 왼쪽 아이콘으로 쓴다. 아이콘은 장식이라 읽지 않는다.
 * @param fontSize 아이콘 크기(sp). 글자 배율을 따라 함께 커진다
 */
@Composable
fun Avatar(symbol: ImageVector, modifier: Modifier = Modifier, size: Dp = 43.dp, fontSize: Int = 21) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(AvatarBg)
            .clearAndSetSemantics { }
    ) {
        McIcon(symbol, size = fontSize, tint = AvatarText)
    }
}

/** 같은 상자에 아이콘 대신 번호("1", "2")를 넣은 것. 순서가 있는 목록에 쓴다. 읽을 이름은 부르는 쪽이 붙인다. */
@Composable
fun Avatar(number: String, modifier: Modifier = Modifier, size: Dp = 43.dp, fontSize: Int = 21) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(AvatarBg)
            .clearAndSetSemantics { }
    ) {
        Text(text = number, fontSize = fontSize.sp, fontWeight = FontWeight.Black, color = AvatarText)
    }
}

/** 가로 진행 막대. fraction은 0~1 사이의 채운 비율. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ProgressTrack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(9.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ProgressFill)
        )
    }
}

/** 내용이 없을 때 보여 주는 안내: 아이콘 + 제목 + 설명 + (선택) 버튼 */
@Composable
fun EmptyState(
    emoji: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 30.dp)
    ) {
        McIcon(emoji, size = 35, tint = Faint)
        Spacer(Modifier.height(13.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text = body, fontSize = 12.sp, color = EmptyBodyText, lineHeight = 19.sp, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            McButton(text = actionLabel, onClick = onAction, kind = ButtonKind.LIGHT)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 버튼 · 입력칸 · 탭 · 스위치
// ---------------------------------------------------------------------------------------------

/** 버튼의 색 종류 */
enum class ButtonKind { PRIMARY, LIGHT, WHITE, RED }

/**
 * 앱의 기본 버튼.
 *
 * @param kind PRIMARY(청록 바탕) / LIGHT(옅은 민트 바탕) / WHITE(흰 바탕 + 테두리) / RED(옅은 빨강)
 * @param full true면 가로를 꽉 채운다
 * @param small true면 작게 그린다 (목록 줄의 "상세 ›" 같은 버튼)
 * @param loading true면 글자 대신 돌아가는 표시를 보여 주고 눌리지 않는다
 */
@Composable
fun McButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    full: Boolean = false,
    small: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val (container, content, border) = when (kind) {
        ButtonKind.PRIMARY -> Triple(Teal, Color.White, Teal)
        ButtonKind.LIGHT -> Triple(LightButtonBg, MintText, LightButtonBg)   // v3 .btn.light (#e9f7f4)
        ButtonKind.WHITE -> Triple(Color.White, WhiteButtonText, WhiteButtonBorder)
        ButtonKind.RED -> Triple(RedButtonBg, RedButtonText, RedButtonBorder)
    }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(if (small) 10.dp else 12.dp),
        border = BorderStroke(1.dp, border),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            // 처리 중(loading)인 버튼은 흐리게 하지 않는다. 진짜로 못 누르는 버튼만 흐리게.
            disabledContainerColor = if (loading) container else container.copy(alpha = 0.45f),
            disabledContentColor = if (loading) content else content.copy(alpha = 0.6f)
        ),
        contentPadding = if (small) {
            PaddingValues(horizontal = 10.dp, vertical = 8.dp)   // v3 .smaller{padding:8px 10px}
        } else {
            PaddingValues(horizontal = 15.dp, vertical = 12.dp)
        },
        modifier = (if (full) modifier.fillMaxWidth() else modifier)
            .heightIn(min = if (small) 32.dp else 46.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = content, modifier = Modifier.size(18.dp))
        } else {
            Text(
                text = text,
                fontSize = if (small) 11.sp else 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,   // M3 버튼이 내려 주는 자간(0.1sp)을 v3처럼 0으로
                textAlign = TextAlign.Center,
                maxLines = 2            // 200% 글자에서도 잘리지 않게 2줄까지
            )
        }
    }
}

/**
 * 앱의 기본 입력칸: 흰 바탕, 둥근 모서리(11dp), 옅은 테두리, 글자 13sp, 최소 높이 46dp. (v3 .form-field)
 * 이름표는 FieldLabel로 위에 따로 붙인다.
 *
 * M3의 OutlinedTextField는 높이가 최소 56dp라 v3(43px)보다 많이 크다. 그래서 글자 입력 기능만 있는
 * BasicTextField에, M3가 테두리·안내 글자를 그려 주는 DecorationBox를 씌워 안쪽 여백을 직접 정했다.
 *
 * @param label TalkBack이 이 칸을 읽을 때 쓰는 이름(보통 위의 이름표 글자). 안내 글자만 읽히는 것을 막는다.
 * @param keyboardActions 키보드의 '다음'·'완료' 버튼을 눌렀을 때 할 일
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    // 입력칸이 눌렸는지(포커스) 같은 상태를 테두리 색에 알려 주는 통로
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        focusedBorderColor = FieldFocus,
        unfocusedBorderColor = FieldBorder,
        disabledBorderColor = FieldBorder,
        cursorColor = FieldFocus
    )
    val shape = RoundedCornerShape(11.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = TextStyle(fontSize = 13.sp, color = FieldText),
        cursorBrush = SolidColor(FieldFocus),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                placeholder = { Text(text = placeholder, fontSize = 13.sp, color = Faint) },
                colors = colors,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = shape
                    )
                }
            )
        }
    )
}

/**
 * 누르면 고르는 창이 열리는 입력칸 모양의 상자 (날짜·시각 칸에 쓴다).
 * 글자를 직접 치는 칸이 아니라서 입력칸 대신 상자로 그린다.
 */
@Composable
fun PickerField(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: String = "") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White)
            .border(1.dp, FieldBorder, RoundedCornerShape(11.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Text(text = text, fontSize = 13.sp, color = FieldText, modifier = Modifier.weight(1f))
        if (trailing.isNotEmpty()) Text(text = trailing, fontSize = 13.sp, color = Muted)
    }
}

/**
 * 나란히 놓인 선택 탭 (예: 로그인 | 회원가입, 주간 | 월간 | 누락 분석).
 * 고른 탭은 흰 바탕으로 떠 보인다. TalkBack은 각 칸을 '탭'으로, 고른 칸을 '선택됨'으로 읽는다.
 */
@Composable
fun SegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TabsTrack)
            .padding(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) TabsSelectedText else TabsText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    // selectable: '선택된 것 하나'를 고르는 부품이라는 뜻. Role.Tab과 함께 쓰면 "탭, 선택됨"으로 읽힌다.
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(vertical = 10.dp, horizontal = 4.dp)
            )
        }
    }
}

/**
 * 켜고 끄는 스위치. v3 모양(트랙 43×25dp, 손잡이 19dp, 0.15초 이동)을 직접 그린다.
 * M3 Switch(52×32dp)는 v3보다 크고, 켤 때 손잡이가 커지는 움직임이 있어서다.
 * 움직임 최소화(LocalReduceMotion)가 켜져 있으면 손잡이가 움직이지 않고 바로 옮겨진다.
 *
 * @param onCheckedChange 눌렀을 때 부를 함수. null이면 누를 수 없는 그림으로만 그린다
 * @param contentDescription TalkBack이 읽을 이름 (예: "일시중지")
 */
@Composable
fun McSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val reduceMotion = LocalReduceMotion.current
    // 손잡이의 왼쪽 위치. 꺼짐 3dp, 켜짐 3 + 18 = 21dp (v3: 켜면 18px 이동)
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 150),
        label = "switchThumb"
    )
    Box(
        modifier = modifier
            .size(width = 43.dp, height = 25.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (checked) SwitchOn else SwitchOff)
            // onCheckedChange가 null이면 그림만 그린다(터치를 받지 않는다). 줄 전체가 스위치 역할을 하는
            // ToggleRow 안에서 그렇게 쓴다 — 여기서 터치를 받으면 줄의 클릭이 스위치 위에서만 먹히지 않는다.
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                } else Modifier
            )
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 3.dp)
                .size(19.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// 목록 줄
// ---------------------------------------------------------------------------------------------

/**
 * 카드 안의 목록 한 줄: [왼쪽 아이콘] [제목 + 설명들] [오른쪽 버튼 등].
 * 줄 사이에는 얇은 선을 긋는다 (마지막 줄은 showDivider = false).
 */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    /** TalkBack이 "두 번 탭하면 …"에 넣어 읽을 말 (예: "상세 보기") */
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = if (onClick != null) modifier.clickable(onClickLabel = onClickLabel, onClick = onClick) else modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f), content = content)
            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                trailing()
            }
        }
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = RowDivider)
    }
}

/** 줄을 누르면 다음 화면으로 간다는 표시 "›". 장식이라 TalkBack이 읽지 않는다. */
@Composable
fun RowChevron() {
    Text("›", fontSize = 18.sp, color = Faint, modifier = Modifier.clearAndSetSemantics { })
}

/** 목록 줄의 제목 글자. 약 이름이 잘리지 않도록 줄 수를 제한하지 않는다(F084). */
@Composable
fun RowTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Navy,
        letterSpacing = (-0.2).sp,
        modifier = modifier
    )
}

/** 목록 줄의 설명 글자 */
@Composable
fun RowSub(text: String, modifier: Modifier = Modifier) {
    Text(text = text, fontSize = 11.sp, color = RowSubText, lineHeight = 16.sp, modifier = modifier.padding(top = 4.dp))
}

/** "이름 ........ 값" 한 줄. 상세 화면의 정보 표에 쓴다. */
@Composable
fun LabelValueRow(label: String, value: String, showDivider: Boolean = true) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 13.dp)) {
            // 이름표는 92dp가 기본이고, 글자를 크게 쓰면 180dp까지 넓어진다(낱말 중간에서 줄이 바뀌지 않게).
            Text(text = label, fontSize = 11.sp, color = Muted, modifier = Modifier.widthIn(min = 92.dp, max = 180.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Navy,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = SettingDivider)
    }
}

// ---------------------------------------------------------------------------------------------
// 로고
// ---------------------------------------------------------------------------------------------

/**
 * 청록 그라데이션 둥근 상자 안의 기호. 시작·로그인 화면의 로고. (v3 .auth-logo: 67px, 글자 35px)
 * 아이콘은 뜻이 없는 장식이라 TalkBack이 읽지 않게 한다.
 */
@Composable
fun AuthLogo(symbol: ImageVector = Glyph.LOGO, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(67.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(Brush.linearGradient(listOf(AuthLogoStart, AuthLogoEnd)))
            .clearAndSetSemantics { }
    ) {
        McIcon(symbol, size = 40, tint = Color.White)
    }
}

/** 작은 동그라미 (달력의 점 등) */
@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Dp = 5.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
