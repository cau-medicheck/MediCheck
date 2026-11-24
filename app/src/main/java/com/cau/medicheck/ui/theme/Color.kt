package com.cau.medicheck.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 앱에서 쓰는 색을 한곳에 모아 둔다. UI 프로토타입의 색 값을 그대로 옮겼다.
 * 화면 코드에서는 색 값을 직접 쓰지 않고 여기 이름을 쓴다.
 */

// ----- 바탕과 글자 -----
val ScreenBackground = Color(0xFFF8FBFC)   // 화면 바탕
val CardBackground = Color(0xFFFFFFFF)     // 카드 바탕
val CardBorder = Color(0xFFE7EFF0)         // 카드 테두리
val Line = Color(0xFFE4EDF0)               // 구분선
val TimelineLine = Color(0xFFD5E2E5)       // 홈 시간표의 세로선
// 홈 시간표의 상태 표시 (다른 화면의 배지보다 진하게)
val HomeChipDoneBg = Color(0xFFC6EBE3); val HomeChipDoneText = Color(0xFF0A5F56)       // 복용 완료
// 미복용(시각이 지났는데 기록 안 함)이 주황이라, 건너뜀(일부러 안 먹음)은 파랑으로 구분한다 (2026-11-23)
val HomeChipMissedBg = Color(0xFFFFE3C2); val HomeChipMissedText = Color(0xFF8A4B0B)     // 미복용 (주황)
val HomeChipSkippedBg = Color(0xFFDDE8FA); val HomeChipSkippedText = Color(0xFF2F5FA8)   // 건너뜀 (파랑)
val TimelineDotMissed = Color(0xFFE08A2E); val TimelineDotSkipped = Color(0xFF3E73C5)    // 시간표 점: 미복용(주황) / 건너뜀(파랑)
val TimelineDotWait = Color(0xFFA3B1B9)                                                   // 시간표 점: 예정(회색)
val HomeChipWaitBg = Color(0xFFE2E8EC); val HomeChipWaitText = Color(0xFF4F626D)       // 예정
val ConditionTagBg = Color(0xFFE2F4F1); val ConditionTagText = Color(0xFF0F6E7A)     // 복용 조건 꼬리표 [식후]
// 내 약 카드의 최근 7일 복용률 막대: 80% 이상 / 50~79% / 50% 미만
val RateGood = Color(0xFF34B7A0); val RateFair = Color(0xFFE9A04F); val RateLow = Color(0xFFE07070)
val TimelineGroupBg = Color(0xFFF3F8F9); val TimelineGroupDivider = Color(0xFFE3ECEE)   // 홈 시간표: 같은 시각 약 묶음 상자 / 상자 안 구분선
val HeroDeepStart = Color(0xFF0C5F6D); val HeroDeepEnd = Color(0xFF14808C)   // 홈 맨 위 카드의 짙은 청록 바탕
val HeroPastStart = Color(0xFF2E5560); val HeroPastEnd = Color(0xFF3E6E78)   // 홈 맨 위 카드: 지난 시각으로 넘겼을 때(조금 어둡게)
val FieldBorder = Color(0xFFDCE7EA)        // 입력칸 테두리
val Navy = Color(0xFF203846)               // 기본 글자
val Muted = Color(0xFF718692)              // 흐린 글자 (설명, 보조 정보)
val Faint = Color(0xFF93A4AC)              // 더 흐린 글자 (머리말, 꺼진 탭)

// ----- 주색 (청록) -----
val Teal = Color(0xFF128B94)
val TealDark = Color(0xFF096E7A)
val Mint = Color(0xFFE9F8F5)               // 옅은 청록 바탕 (아이콘 상자, 보조 버튼)
val MintText = Color(0xFF117E86)
val LogoUnderlineMint = Color(0xFFCDEDEA)  // 머리말 로고 "Check" 밑 캡슐 밑줄의 옅은 반쪽
val HeroStart = Color(0xFF147C93)          // 홈 큰 카드의 그라데이션 시작
val HeroEnd = Color(0xFF31B4A2)            // 홈 큰 카드의 그라데이션 끝
val ProgressTrack = Color(0xFFE7F0F2)      // 진행 막대의 빈 부분
val ProgressFill = Color(0xFF32B9A6)       // 진행 막대의 채운 부분

// ----- 상태 색 -----
val Amber = Color(0xFFE49A45)
val Danger = Color(0xFFD55F5F)

// ----- 배지(작은 알약 모양 표시)의 바탕/글자 -----
val PillMintBg = Color(0xFFE9F7F4); val PillMintText = Color(0xFF168577)
val PillOrangeBg = Color(0xFFFFF2E6); val PillOrangeText = Color(0xFFBD7F3C)
val PillGrayBg = Color(0xFFF0F3F5); val PillGrayText = Color(0xFF81919A)
val PillBlueBg = Color(0xFFEBF3FF); val PillBlueText = Color(0xFF3E73C5)
val PillRedBg = Color(0xFFFDEEEE); val PillRedText = Color(0xFFD25E5E)

// ----- 안내 상자 -----
val NoticeBg = Color(0xFFEDF8F5); val NoticeBorder = Color(0xFFDBF1EA); val NoticeText = Color(0xFF377C77)
val WarnBg = Color(0xFFFFF5EB); val WarnBorder = Color(0xFFFCE6CC); val WarnText = Color(0xFF9B754E)

// ----- 그 밖 -----
val RedButtonBg = Color(0xFFFFF0F0); val RedButtonText = Color(0xFFC25050); val RedButtonBorder = Color(0xFFFADDDD)
val WhiteButtonText = Color(0xFF2C6770); val WhiteButtonBorder = Color(0xFFD9E7E8)
val TabsTrack = Color(0xFFE9F0F2)          // 탭 묶음의 바탕
val SwitchOff = Color(0xFFD8E5E7)
val ToastBackground = Color(0xFF264E55)

/*
 * ===== 여기부터: 구현 명세 00 §4.4의 NEW 토큰 =====
 * v3 프로토타입 CSS에만 있고 위 목록에 이름이 없던 색이다.
 * 부품 안에 Color(0xFF…)를 직접 쓰면 나중에 색을 바꿀 때 여러 곳을 찾아다녀야 하므로 이름을 붙여 모았다.
 */

// ----- NEW: 앱 틀 -----
val HeaderDivider = Color(0xFFEDF1F2)      // 머리말 아래 선
val IconBtnBg = Color(0xFFEDF6F5)          // 머리말 ⚙ 버튼 바탕 (♧는 흰 바탕 + Line 테두리)
val IconBtnText = Color(0xFF137D84)        // 머리말 버튼 글자
val TabInactive = Color(0xFF899CA6)        // 하단 탭 (꺼짐)
val TabActive = Color(0xFF0B8990)          // 하단 탭 (켜짐)

// ----- NEW: 글자 -----
val ParagraphText = Color(0xFF748A95)      // .paragraph
val LabelText = Color(0xFF597480)          // .label
val KickerText = Color(0xFF7FA5AC)         // .kicker
val MetricText = Color(0xFF0E8D92)         // .metric
val EmptyBodyText = Color(0xFF8497A0)      // .empty p
val AuthTitleText = Color(0xFF147F88)      // .auth-title
val AuthSubText = Color(0xFF8299A3)        // .auth-sub

// ----- NEW: 줄 -----
val RowDivider = Color(0xFFEDF2F3)         // .medrow 선
val RowSubText = Color(0xFF8B9CA5)         // .medrow p
val SettingDivider = Color(0xFFF0F3F3)     // .settingrow 선
val SettingDescText = Color(0xFF7F9099)    // .settingrow p
val OptionDivider = Color(0xFFEDF3F4)      // optionRows 선

// ----- NEW: 부품 -----
val LightButtonBg = Color(0xFFE9F7F4)      // .btn.light 바탕·테두리 (Mint보다 한 단계 옅음)
val FieldText = Color(0xFF264553); val FieldFocus = Color(0xFF23A2A6)
val AvatarBg = Color(0xFFE3F5F2); val AvatarText = Color(0xFF14998B)
val SwitchOn = Color(0xFF1797A0)
val TabsText = Color(0xFF78929B); val TabsSelectedText = Color(0xFF117D8A)
val CircleInner = Color(0xFF168E96)
val DateChipBorder = Color(0xFFEAF0F2); val DateChipText = Color(0xFF8799A3)
val DayBtnBorder = Color(0xFFDCE9E9); val DayBtnText = Color(0xFF728994)
val CalDayText = Color(0xFF5E7782); val CalDayActive = Color(0xFF138B92)
val CalDot = Color(0xFF47B7A4); val CalWeekdayText = Color(0xFF92A2AA)
val ChartTop = Color(0xFF32BDAC); val ChartBottom = Color(0xFF148B96); val ChartLabel = Color(0xFF8197A0)
val AuthLogoStart = Color(0xFF1788A0); val AuthLogoEnd = Color(0xFF36BCA4)
val IllustrationStart = Color(0xFFE3F6F5); val IllustrationEnd = Color(0xFFF1F8FD)
val PagerDotActive = Color(0xFF1D99A2); val PagerDotInactive = Color(0xFFDCE6E9)

// 홈(S04) 큰 카드 안쪽 — 흰색을 옅게 겹친 색 (v3 rgba(255,255,255,.09 / .21 / .28))
val HeroInnerBg = Color(0x17FFFFFF)        // 완료율 상자 바탕
val HeroInnerBorder = Color(0x36FFFFFF)    // 완료율 상자 테두리
val HeroRingRest = Color(0x47FFFFFF)       // 완료율 원의 빈 부분

// 사진 올리기 상자(S29)의 점선 테두리와 바탕 (v3 .upload)
val DashedBorder = Color(0xFFBFD7DA)
val DashedBg = Color(0xFFF5FBFB)

// 월간 통계(S14) 막대: 완료율이 100%가 아닌 날 (v3 #8dbec6)
val ChartBarMuted = Color(0xFF8DBEC6)
val PreviewBg = Color(0xFFF0F8F7)         // S37 글자 확대 미리보기 상자
val DangerTitle = Color(0xFFBE5959)       // S44 "삭제 전 확인" 제목
val CountBoxBg = Color(0xFFEFF8F7)        // S45 우선순위 개수 상자
val ReqDivider = Color(0xFFE9EFF0)        // S45 기능 줄 사이의 선
