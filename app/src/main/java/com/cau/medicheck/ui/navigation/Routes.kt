package com.cau.medicheck.ui.navigation

import com.cau.medicheck.domain.DoseKey
import androidx.annotation.StringRes
import com.cau.medicheck.R

/**
 * 화면마다 붙이는 주소(route). (구현 명세 00 §2.1)
 *
 * 주소 = 화면 ID 그대로("S01"). 프로토타입의 화면 번호로 바로 찾을 수 있게 하기 위해서다.
 * 인자가 없는 화면은 ID가 곧 주소라서, 여기에 상수가 없는 화면은 "S17"처럼 ID를 그대로 쓴다.
 */
object Routes {
    const val S00 = "S00"
    const val S01 = "S01"
    const val S02 = "S02"
    const val S03 = "S03"
    const val S04 = "S04"   // 홈 대시보드 (로그인 뒤 첫 화면)
    const val S05 = "S05"   // 일정/캘린더
    const val S06 = "S06"   // 내 약 목록
    const val S11 = "S11"   // 기록 캘린더
    const val S12 = "S12"   // 날짜별 기록 (4단계)
    const val S13 = "S13"   // 주간 통계
    const val S16 = "S16"   // 알림 센터
    const val S19 = "S19"   // 설정 허브
    const val S29 = "S29"   // 사진으로 약 등록 (시안)
    const val S30 = "S30"   // 복용 변경 이력
    const val S32 = "S32"   // 과거 기록 보존
    const val S40 = "S40"   // 중복 약·회차 확인
    const val S34 = "S34"   // 알림 거절/오류 안내

    // ----- 인자가 붙는 화면 -----
    // "?이름={이름}"은 선택 인자다(없어도 화면이 열린다). 실제로 이동할 때는 아래 함수로 값을 채운 주소를 만든다.
    const val ARG_MED_ID = "medId"
    const val ARG_SCHEDULE_ID = "scheduleId"
    const val ARG_DATE = "date"
    /** 인자가 없다는 뜻으로 쓰는 값 (주소에는 null을 넣을 수 없어서 -1을 약속으로 쓴다) */
    const val NO_ID = -1L

    const val S07 = "S07?$ARG_MED_ID={$ARG_MED_ID}"                                   // 약 상세
    const val S08 = "S08?$ARG_MED_ID={$ARG_MED_ID}"                                   // 약 등록/수정
    const val S09 = "S09?$ARG_MED_ID={$ARG_MED_ID}"                                   // 삭제 확인
    const val S33 = "S33?$ARG_MED_ID={$ARG_MED_ID}"                                   // 일정 변경 적용 범위
    const val S42 = "S42?$ARG_MED_ID={$ARG_MED_ID}"                                   // 처방 출처·진료과
    const val S10 = "S10?$ARG_SCHEDULE_ID={$ARG_SCHEDULE_ID}&$ARG_DATE={$ARG_DATE}"   // 회차 상세/상태
    const val S31 = "S31?$ARG_SCHEDULE_ID={$ARG_SCHEDULE_ID}&$ARG_DATE={$ARG_DATE}"   // 실제 복용 시각 기록

    /** 약 상세의 주소. 예: "S07?medId=3" */
    fun s07(medId: Long): String = "S07?$ARG_MED_ID=$medId"
    fun s09(medId: Long): String = "S09?$ARG_MED_ID=$medId"
    fun s33(medId: Long): String = "S33?$ARG_MED_ID=$medId"
    fun s42(medId: Long): String = "S42?$ARG_MED_ID=$medId"
    /** 새 약 등록 화면의 주소 */
    fun s08New(): String = "S08"
    /** 약 수정 화면의 주소 */
    fun s08Edit(medId: Long): String = "S08?$ARG_MED_ID=$medId"
    /** 회차 상세의 주소. 예: "S10?scheduleId=3&date=2026-11-17" */
    fun s10(key: DoseKey): String = "S10?$ARG_SCHEDULE_ID=${key.scheduleId}&$ARG_DATE=${key.date}"
    /** 실제 복용 시각 기록의 주소 */
    fun s31(key: DoseKey): String = "S31?$ARG_SCHEDULE_ID=${key.scheduleId}&$ARG_DATE=${key.date}"

    /** 로그인 전 틀(AuthNavHost)에 들어가는 화면들 */
    val PRE_LOGIN = setOf(S00, S01, S02, S03)

    /** 47개 화면 ID 전부 (S00 ~ S46). 머리말 제목을 고를 때와 테스트에서 쓴다. */
    val ALL_SCREEN_IDS: List<String> = (0..46).map { "S%02d".format(it) }

    /**
     * 화면 ID → 머리말 제목 문구 번호. 제목은 v3 프로토타입의 화면 이름(V 목록의 첫 칸) 그대로다.
     * route에 "?medId=…" 같은 꼬리가 붙어 있어도 앞 3글자(화면 ID)만 본다.
     */
    @StringRes
    fun titleRes(route: String): Int = when (NavRules.screenIdOf(route)) {
        "S00" -> R.string.screen_title_s00
        "S01" -> R.string.screen_title_s01
        "S02" -> R.string.screen_title_s02
        "S03" -> R.string.screen_title_s03
        "S04" -> R.string.screen_title_s04
        "S05" -> R.string.screen_title_s05
        "S06" -> R.string.screen_title_s06
        "S07" -> R.string.screen_title_s07
        "S08" -> R.string.screen_title_s08
        "S09" -> R.string.screen_title_s09
        "S10" -> R.string.screen_title_s10
        "S11" -> R.string.screen_title_s11
        "S12" -> R.string.screen_title_s12
        "S13" -> R.string.screen_title_s13
        "S14" -> R.string.screen_title_s14
        "S15" -> R.string.screen_title_s15
        "S16" -> R.string.screen_title_s16
        "S17" -> R.string.screen_title_s17
        "S18" -> R.string.screen_title_s18
        "S19" -> R.string.screen_title_s19
        "S20" -> R.string.screen_title_s20
        "S21" -> R.string.screen_title_s21
        "S22" -> R.string.screen_title_s22
        "S23" -> R.string.screen_title_s23
        "S24" -> R.string.screen_title_s24
        "S25" -> R.string.screen_title_s25
        "S26" -> R.string.screen_title_s26
        "S27" -> R.string.screen_title_s27
        "S28" -> R.string.screen_title_s28
        "S29" -> R.string.screen_title_s29
        "S30" -> R.string.screen_title_s30
        "S31" -> R.string.screen_title_s31
        "S32" -> R.string.screen_title_s32
        "S33" -> R.string.screen_title_s33
        "S34" -> R.string.screen_title_s34
        "S35" -> R.string.screen_title_s35
        "S36" -> R.string.screen_title_s36
        "S37" -> R.string.screen_title_s37
        "S38" -> R.string.screen_title_s38
        "S39" -> R.string.screen_title_s39
        "S40" -> R.string.screen_title_s40
        "S41" -> R.string.screen_title_s41
        "S42" -> R.string.screen_title_s42
        "S43" -> R.string.screen_title_s43
        "S44" -> R.string.screen_title_s44
        "S45" -> R.string.screen_title_s45
        "S46" -> R.string.screen_title_s46
        else -> R.string.app_name   // 모르는 주소면 앱 이름을 보인다 (안전장치)
    }
}
