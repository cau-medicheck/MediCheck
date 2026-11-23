package com.cau.medicheck.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cau.medicheck.ui.theme.ToastBackground
import com.cau.medicheck.ui.theme.ToastSideMargin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/*
 * 토스트 = 화면 아래쪽에 2.9초 동안 떴다 사라지는 안내 글. (구현 명세 00 §1.5, v3 show())
 *
 * 흐름:  ViewModel이 toastBus.show(R.string.…) → MediCheckApp(맨 바깥)이 messages를 받아 글자로 바꿔
 *        2.9초 띄움 → McToastHost가 그린다.
 * 맨 바깥에서 띄우는 이유: 로그인에 성공하는 순간 로그인 화면이 통째로 사라지고 홈 화면으로 바뀐다.
 * 로그인 화면 안에서 띄우면 화면과 함께 토스트도 사라져 버린다.
 */

/**
 * 토스트를 띄워 달라는 요청이 지나가는 통로. AppContainer에 하나만 있다.
 * ViewModel은 문자열 번호(@StringRes)만 넘기므로 Context(안드로이드 화면 정보)가 필요 없다.
 *
 * SharedFlow: 값을 "흘려보내는" 통로. 받는 쪽(MediCheckApp)이 지켜보고 있다가 하나씩 꺼낸다.
 * extraBufferCapacity = 8: 받는 쪽이 잠깐 바빠도 요청 8개까지는 잃지 않고 쌓아 둔다.
 */
class ToastBus {
    private val _messages = MutableSharedFlow<ToastMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<ToastMessage> = _messages.asSharedFlow()

    /**
     * @param id strings.xml의 문구 번호 (R.string.…)
     * @param action 토스트 오른쪽에 붙일 버튼 하나. 홈·일정의 "✓ 복용 완료" 뒤 "실행취소"에만 쓴다.
     * @param args 문구 안의 %1$d 같은 자리에 넣을 값 (예: "2개를 복용 완료로 기록했어요"의 2)
     */
    fun show(@StringRes id: Int, action: ToastAction? = null, vararg args: Any) {
        // tryEmit: 기다리지 않고 바로 넣는다 (suspend가 아니라 어디서나 부를 수 있다)
        _messages.tryEmit(ToastMessage(id, action, args.toList()))
    }
}

/** 토스트에 붙는 버튼 하나: 글자와 눌렀을 때 할 일 */
class ToastAction(@param:StringRes val label: Int, val run: () -> Unit)

/** 토스트 한 장: 문구와 (있으면) 버튼, 문구에 넣을 값 */
class ToastMessage(@param:StringRes val text: Int, val action: ToastAction? = null, val args: List<Any> = emptyList())

/**
 * 화면 부품(Composable)에서 토스트를 바로 띄울 때 쓰는 통로. MediCheckApp이 맨 위에서 넣어 준다.
 * 사용: LocalToastBus.current.show(R.string.…)
 */
val LocalToastBus = staticCompositionLocalOf<ToastBus> { error("MediCheckApp 안에서만 쓸 수 있습니다") }

/** 토스트를 보여 주는 시간(ms). v3 setTimeout(…, 2900) */
const val TOAST_DURATION_MS = 2_900L

/** 버튼이 붙은 토스트를 보여 주는 시간(ms). 누를 시간을 주려고 5초 (F088 "5초 내 실행취소") */
const val TOAST_WITH_ACTION_DURATION_MS = 5_000L

/**
 * 토스트 한 장을 그린다. text가 null이면 아무것도 그리지 않는다.
 *
 * - 상자에는 클릭 처리를 붙이지 않았으므로 아래 버튼을 누르는 것을 막지 않는다(v3 pointer-events:none).
 *   예외: actionLabel이 있으면 그 버튼 글자만 누를 수 있다.
 * - liveRegion: 나타나면 TalkBack이 바로 읽어 준다.
 * - 앱 글자 배율·높은 대비는 적용하지 않는다(본문 밖, v3와 같음). 폰 글꼴 크기는 따른다.
 *
 * @param bottomOffset 화면 아래 끝에서 얼마나 띄울지. 계산은 MediCheckApp이 한다(탭·키보드 높이에 따라 다름).
 */
@Composable
fun McToastHost(
    text: String?,
    bottomOffset: Dp,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    if (text == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(start = ToastSideMargin, end = ToastSideMargin, bottom = bottomOffset)
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
            .background(ToastBackground, RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = if (actionLabel == null) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 13.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
        // 버튼이 있을 때만, 그 글자 부분만 누를 수 있다. 토스트의 나머지 부분은 터치가 통과한다.
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(start = 14.dp, top = 15.dp, bottom = 15.dp, end = 2.dp)
            )
        }
    }
}

/**
 * 토스트를 화면 아래 끝에서 얼마나 띄울지. (구현 명세 00 §1.5 규칙 3) 순수 계산이라 단위 테스트한다.
 *
 * - frameBottom: 틀 아래쪽이 차지하는 높이 (하단 탭 + 내비게이션 바, 로그인 전이면 내비게이션 바 + 59dp)
 * - imeBottom: 키보드 높이. 키보드가 열리면 하단 탭이 그 뒤에 가려지므로, 토스트는 키보드 위에 띄운다.
 * 둘 중 높은 쪽 위로 18dp.
 */
fun toastBottomOffset(frameBottom: Dp, imeBottom: Dp): Dp =
    maxOf(frameBottom, imeBottom) + com.cau.medicheck.ui.theme.ToastGap
