package com.cau.medicheck.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp

/**
 * 앱이 쓰는 아이콘 모음.
 *
 * v3 프로토타입은 아이콘을 글자 하나(⌂ ▦ ♧)나 이모지(💊 🔔)로 그렸다. 글자 아이콘은 폰마다 모양이 다르고
 * 굵기·크기가 서로 맞지 않아서, 한 벌로 그려진 아이콘(Material Icons의 Outlined 모양)으로 바꿨다.
 * ImageVector = 선과 면으로 그린 그림. 글자와 달리 어느 폰에서나 같은 모양으로 나온다.
 *
 * 문장 안에 들어 있는 상태 표시('✓ 복용 완료', '○ 복용 예정')는 그림이 아니라 글자로 남긴다
 * (색을 구분하기 어려운 사람도 상태를 알 수 있게 하는 표시라 문구와 함께 strings.xml에 둔다).
 */
object Glyph {
    val LOGO: ImageVector = Icons.Rounded.Add                 // S00·S01·S02 로고 (십자)
    val MAIL: ImageVector = Icons.Outlined.MailOutline        // S03 로고
    val PILL: ImageVector = Icons.Outlined.Medication         // S00 1장 그림, 약 없음
    val BELL: ImageVector = Icons.Outlined.Notifications      // S00 2장 그림, 알림
    val CHART: ImageVector = Icons.Outlined.BarChart          // S00 3장 그림

    // 머리말 버튼 · 하단 탭
    val HEADER_BELL: ImageVector = Icons.Outlined.Notifications
    val HEADER_SETTINGS: ImageVector = Icons.Outlined.Settings
    val TAB_HOME: ImageVector = Icons.Outlined.Home
    val TAB_SCHEDULE: ImageVector = Icons.Outlined.CalendarMonth
    val TAB_MEDS: ImageVector = Icons.Outlined.Medication
    val TAB_RECORDS: ImageVector = Icons.Outlined.Checklist
    val TAB_STATS: ImageVector = Icons.Outlined.BarChart

    // 목록 줄의 왼쪽 아이콘
    val DOSE: ImageVector = Icons.Outlined.Medication         // 회차 (복용 완료 전)
    val DOSE_DONE: ImageVector = Icons.Rounded.Check          // 회차 (복용 완료)
    val HISTORY: ImageVector = Icons.Outlined.History         // 변경 이력

    // 빈 상태·안내의 그림
    val EMPTY_HOME: ImageVector = Icons.Outlined.EventAvailable     // S04 회차 없음
    val EMPTY_SCHEDULE: ImageVector = Icons.Outlined.CalendarMonth  // S05 회차 없음
    val EMPTY_CIRCLE: ImageVector = Icons.Outlined.Schedule         // S31 회차 없음
    val BELL_OFF: ImageVector = Icons.Outlined.NotificationsOff     // 알림 꺼짐 카드
    val NOT_FOUND: ImageVector = Icons.Outlined.SearchOff           // 약을 찾지 못함 (S07·S08)
    val WARNING: ImageVector = Icons.Outlined.WarningAmber          // 삭제 확인(S09) · 중복 확인(S40)
    val UPLOAD: ImageVector = Icons.Outlined.AddPhotoAlternate      // 사진 올리기 상자 (S29)

    // 설정 허브(S19)·프로필(S21)
    val PROFILE: ImageVector = Icons.Outlined.Person          // 프로필 카드
    val PERMISSION: ImageVector = Icons.Outlined.Security     // 알림 권한·OS 줄
    val POLICY: ImageVector = Icons.Outlined.Percent          // 복약률 계산 정책 줄
    val HELP: ImageVector = Icons.AutoMirrored.Outlined.HelpOutline   // 도움말 줄

    // 가족·보호자 시안(S23)
    val PERSON: ImageVector = Icons.Outlined.Person           // 내 계정
    val CHILD: ImageVector = Icons.Outlined.ChildCare         // 자녀 예시 프로필

    // 빈 화면·오류 상태 예시(S39)
    val EXAMPLE_EMPTY: ImageVector = Icons.Outlined.Inbox           // 빈 상태
    val EXAMPLE_INPUT: ImageVector = Icons.Outlined.ErrorOutline    // 입력 오류
    val EXAMPLE_OFFLINE: ImageVector = Icons.Outlined.CloudOff      // 오프라인

    val PREV: ImageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft    // 이전 달
    val NEXT: ImageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight   // 다음 달
}

/**
 * 아이콘 하나를 그린다. 꾸미는 그림이라 TalkBack이 읽지 않는다(이름이 필요하면 감싸는 쪽이 붙인다).
 *
 * @param size 크기를 글자 단위(sp)로 받는다. 그래야 접근성의 글자 배율을 키웠을 때 아이콘도 글자와 함께 커진다.
 */
@Composable
fun McIcon(icon: ImageVector, size: Int, tint: Color, modifier: Modifier = Modifier) {
    val sizeDp = with(LocalDensity.current) { size.sp.toDp() }
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = modifier.size(sizeDp))
}
