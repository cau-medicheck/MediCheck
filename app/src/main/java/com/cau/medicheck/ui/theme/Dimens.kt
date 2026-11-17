package com.cau.medicheck.ui.theme

import androidx.compose.ui.unit.dp

/*
 * 여러 화면이 함께 쓰는 길이(여백·간격) 모음. (구현 명세 00 §4.3)
 * v3 프로토타입 CSS의 px 숫자를 그대로 dp로 옮겼다.
 * 화면마다 숫자를 직접 쓰면 "본문 여백이 화면마다 조금씩 다른" 일이 생기므로 이름을 붙여 둔다.
 */

// v3 간격 도우미 .g8 / .gap / .g16 / .g22
val Gap8 = 8.dp
val Gap13 = 13.dp
val Gap16 = 16.dp
val Gap22 = 22.dp

// 본문(.phone-body{padding:18px 18px 24px})
val BodyHorizontal = 18.dp
val BodyTop = 18.dp
val BodyBottom = 24.dp

// 토스트(.toast{bottom:77px;left:18px;right:18px})
// 토스트는 "틀 아래쪽 높이 + 18dp" 위에 띄운다. 로그인 뒤의 하단 탭 높이가 59dp이고(77 - 18),
// 로그인 전에는 탭이 없지만 같은 높이에 띄우려고 같은 값을 쓴다.
val ToastSideMargin = 18.dp
val ToastGap = 18.dp
val PreLoginFrameBottom = 59.dp
