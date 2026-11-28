<div align="center">

# 💊 MediCheck

**A Smart Medication Schedule Management Application**

![Kotlin 2.2](https://img.shields.io/badge/Kotlin-2.2-7F52FF)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4)
![Room 2.8](https://img.shields.io/badge/Room-2.8-3DDC84)
![minSdk 26](https://img.shields.io/badge/minSdk-26-34A853)
![47 screens](https://img.shields.io/badge/screens-47-5865F2)
![509 unit tests](https://img.shields.io/badge/unit_tests-509-2088FF)

</div>

MediCheck는 사용자가 자신의 약 정보와 복약 일정을 관리하고, 정해진 시간에 알림을 받아 규칙적인 복약 습관을 형성할 수 있도록 지원하는 Android 모바일 애플리케이션입니다.

복용 여부를 기록하고 복약 이력 및 통계를 확인하여 사용자가 자신의 복약 상태를 지속적으로 관리할 수 있도록 하는 것을 목표로 합니다.

> 중앙대학교 *Application Development for Mobile Computer* (2026 Fall) 팀 프로젝트

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [빠른 시작](#빠른-시작)
- [주요 기능](#주요-기능)
- [기술 스택과 아키텍처](#기술-스택과-아키텍처)
- [개발 현황](#개발-현황)
- [기여 및 협업 규칙](#기여-및-협업-규칙)
- [팀과 역할](#팀과-역할)
- [개발 프로세스](#개발-프로세스)
- [최종 산출물](#최종-산출물)
- [프로젝트 목표](#프로젝트-목표)

## 프로젝트 소개

| 항목 | 내용 |
|---|---|
| Project Name | MediCheck |
| Project Type | Android Mobile Application |
| Main Purpose | 복약 일정 및 복약 기록 관리 |
| Development Language | Kotlin |
| UI Framework | Jetpack Compose |
| Database | Room Database |
| Collaboration | GitHub |
| Team Size | 4 Members |
| Development Period | 2026.11 ~ 2026.12 (약 1개월 반) |
| Screens | 47개 (S00 ~ S46) |
| Unit Tests | 509개 (JUnit, 폰 없이 실행) |

### 🎯 Development Objectives

- 사용자별 약 정보 및 복약 일정 관리
- 설정된 복약 시간에 맞춘 알림 제공
- 복용 완료 및 건너뜀 기록 관리
- 날짜별 복약 이력 및 통계 제공
- 복용 누락 패턴 분석을 통한 복약 습관 관리 지원

## 빠른 시작

### Requirements
- Android Studio (최신 안정 버전)
- Git

### Installation

1. 저장소를 Clone합니다.

```bash
git clone https://github.com/cau-medicheck/MediCheck.git
```

2. Android Studio → **Open** → clone한 `MediCheck` 폴더를 선택합니다.
3. Gradle Sync가 끝날 때까지 기다립니다. (처음에는 몇 분 걸릴 수 있습니다)
4. Android Emulator 또는 USB로 연결한 실제 기기를 선택합니다.
5. ▶ Run 버튼을 눌러 애플리케이션을 실행합니다.

※ `local.properties`는 각자의 Android Studio가 자동으로 생성합니다. Git에 올리지 않습니다.

### 터미널에서 빌드·테스트

```bash
./gradlew assembleDebug        # 디버그 APK 빌드 → app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # JUnit 단위 테스트
```

빌드가 엉뚱한 "Unresolved reference" 오류로 실패하면 `./gradlew clean assembleDebug`로 다시 빌드합니다.

### 처음 써 보기

로그인 화면의 **샘플 계정으로 체험하기**를 누르면 약 5개와 33일 치 기록이 들어 있는 계정으로 바로 들어갑니다. 화면은 기기 언어가 한국어면 한국어로, 그 밖에는 영어로 보입니다. 직접 가입하려면 이메일과 6자 이상의 비밀번호를 입력합니다(계정은 이 기기 안에만 만들어집니다).

## 주요 기능

화면은 47개(S00 ~ S46)이고, 화면 번호는 프로토타입의 번호와 같습니다. 전체 목록은 [화면 목록](#화면-목록)에 있습니다.

### 계정 (S00 ~ S03, S21, S44)
- 온보딩, 이메일 + 비밀번호(6자 이상) 회원가입·로그인, 로그아웃
- 비밀번호는 원문을 저장하지 않고 PBKDF2 해시로 저장
- 프로필(표시 이름·이메일) 수정
- 계정·데이터 삭제 — 지금 로그인한 계정의 데이터만 지웁니다
- 샘플 계정으로 체험하기 (약 5개와 33일 치 기록 165건이 들어 있는 계정)
- 비밀번호 재설정 안내 — 서버가 없어 재설정 메일을 보내지 않고, 안내만 보여 줍니다
- 약·기록, 알림·개인정보·앱 잠금 설정은 계정별로 나뉩니다. 접근성 설정(글자 크기 등)은 기기 단위입니다

### 약 관리 (S06 ~ S09, S32, S33, S40, S42)
- 약 이름·복용량·복용 시간(여러 개)·반복 요일·시작일·종료일·메모 등록, 조회, 수정, 삭제
- 약 이름 검색, 일시중지·재개
- 수정할 때 적용 범위 선택 (오늘부터 / 내일부터)
- 같은 이름의 약 중복 확인, 처방 출처(의료기관·진료과) 메모
- 약을 삭제해도 지난 복용 기록은 그때의 약 이름·복용량과 함께 남습니다

### 복약 일정과 기록 (S04, S05, S10, S30, S31)
- 홈: 오늘의 완료율과 회차 목록, 카드에서 바로 복용 완료
- 일정: 날짜를 옮겨 가며 회차 보기 (아침·점심·저녁·밤으로 묶음)
- 회차 상세: 복용 완료 / 건너뜀 / 미기록으로 되돌리기, 실행취소
- 실제 복용 시각 기록, 상태 변경 이력

### 복약 알림 (S16 ~ S18, S34, S35)
- 정해진 시각에 Android 알림 (AlarmManager의 정확한 알람. 정확한 알람이 허용되지 않은 기기에서는 몇 분 늦을 수 있는 알람으로 대신합니다). 폰을 다시 켜도 알람을 복구합니다
- 알림을 누르면 그 회차의 상세 화면으로 이동
- 알림 켜기·소리·진동·재알림(10·15·30·60분) 설정, 테스트 알림
- 알림 권한과 OS 제한 안내, 시간대 변경 정책(기기 현지 시간 / 등록한 시간대)

### 기록과 통계 (S11 ~ S15, S43)
- 월 달력으로 날짜별 기록 보기
- 주간(최근 7일)·월간 완료율, 요일별 막대 그래프
- 건너뜀 패턴 분석 (건너뜀은 시간대별로, 기한이 지난 미기록은 합계로)
- 복약 기록 CSV 내보내기
- 완료율 계산 기준: `완료 횟수 ÷ 예정 시각이 된 횟수`. 건너뜀과 기한이 지난 미기록은 분모에 넣고, 아직 예정 시각이 되지 않은 회차는 뺍니다 (S20에 설명)

### 설정과 안전 (S19 ~ S22, S28, S36 ~ S39, S41)
- 설정 허브, 통계 계산 정책, 도움말
- 개인정보 센터: 알림에서 약 이름 숨김, 최근 앱 목록의 화면 미리보기 숨김
- 접근성: 글자 크기 100·125·150·200%, 높은 대비, 움직임 최소화
- 데이터 백업·복원 (JSON 파일, 같은 계정에서만 복원)
- 앱 잠금: 앱을 다시 열 때 기기의 생체인증·PIN으로 본인 확인
- 오프라인 상태 표시 — 이 앱은 인터넷 없이 모든 기능이 동작합니다

### 요구사항 추적 (S45, S46)
- 팀이 정한 기능 107개를 MoSCoW 우선순위(Must 60 · Should 26 · Could 14 · Won’t 7)와 함께 검색하고, 그 기능의 화면으로 이동
- 이번 릴리스에서 하지 않는 기능과 안전 고지 (복용량 자동 추천·변경은 제공하지 않습니다)

### 시안 화면 (S23 ~ S27, S29)
가족·보호자 공유(S23 ~ S27)와 사진으로 약 등록(S29)은 이번 릴리스에서 구현하지 않는 기능입니다. 화면 모양만 있고, '(미지원)' 버튼은 안내만 띄웁니다.

### 화면 목록

<details>
<summary>전체 47개 화면 보기 (S00 ~ S46)</summary>

| 번호 | 화면 | 번호 | 화면 | 번호 | 화면 |
|---|---|---|---|---|---|
| S00 | 시작/온보딩 | S16 | 알림 센터 | S32 | 과거 기록 보존 |
| S01 | 로그인 | S17 | 알림 설정 | S33 | 일정 변경 적용 범위 |
| S02 | 회원가입 | S18 | 권한·OS 안내 | S34 | 알림 거절/오류 안내 |
| S03 | 비밀번호 재설정 안내 | S19 | 설정 허브 | S35 | 시간대 변경 정책 |
| S04 | 홈 대시보드 | S20 | 통계 계산 정책 | S36 | 개인정보 센터 |
| S05 | 일정/캘린더 | S21 | 프로필 관리 | S37 | 접근성 설정 |
| S06 | 내 약 목록 | S22 | 도움말·범위 | S38 | 오프라인 상태 |
| S07 | 약 상세 | S23 | 가족 프로필 (시안) | S39 | 빈 화면·오류 상태 |
| S08 | 약 등록/수정 | S24 | 보호자 연결 동의 (시안) | S40 | 중복 약·회차 확인 |
| S09 | 삭제 확인 | S25 | 보호자 대시보드 (시안) | S41 | 앱 잠금 설정 |
| S10 | 회차 상세/상태 | S26 | 자녀 복용 기록자 (시안) | S42 | 처방 출처·진료과 |
| S11 | 기록 캘린더 | S27 | 가족 중복 투약 경고 (시안) | S43 | 복약 기록 내보내기 |
| S12 | 날짜별 기록 | S28 | 데이터 백업·복원 | S44 | 계정·데이터 삭제 |
| S13 | 주간 통계 | S29 | 사진으로 약 등록 (시안) | S45 | 기능별 요구사항 맵 |
| S14 | 월간 통계 | S30 | 복용 변경 이력 | S46 | 안전·개발 범위 |
| S15 | 건너뜀 패턴 | S31 | 실제 복용 시각 기록 | | |

</details>

## 기술 스택과 아키텍처

| Category | Technology | Purpose |
|---|---|---|
| Language | Kotlin 2.2 | Android Application Development |
| UI | Jetpack Compose (Material 3) | UI Development |
| Database | Room 2.8 (SQLite) | Local Data Storage |
| Notification | AlarmManager + Notification API | Medication Reminder Scheduling |
| Navigation | Navigation Compose | Screen Navigation |
| Architecture Support | ViewModel + StateFlow | UI State Management |
| Test | JUnit 4 | Unit Tests |
| IDE | Android Studio | Development & Testing |
| Collaboration | GitHub | Version Control & Team Collaboration |

- Minimum SDK: **API 26 (Android 8.0)** / Target SDK: API 37
- Package: `com.cau.medicheck`
- Authentication: **Room 기반 로컬 인증** (계정도 Room에 저장, Firebase 미사용 — Issue #3)
- 서버 없음. 계정과 복약 기록은 이 기기 안에만 저장하고, Android 자동 백업(클라우드)도 꺼 두었습니다. 기기를 바꿀 때는 앱의 백업·복원(S28)을 씁니다.
- WorkManager는 쓰지 않습니다. 알림은 AlarmManager 하나로 처리합니다.
- 의존성 주입 프레임워크 없이 `AppContainer`(`MediCheckApplication.kt`) 한 곳에서 객체를 만들어 넘깁니다.

### 데이터베이스

DB 스키마 버전 5(Room 2.8), 표 6개입니다. 자세한 구조와 규칙은 [docs/DB_DESIGN.md](docs/DB_DESIGN.md)에 있고, **코드는 이 문서를 따릅니다.**

| 표 | 내용 |
|---|---|
| `users` | 계정 (이메일, 비밀번호 해시, 표시 이름) |
| `medications` | 약 (이름, 복용량, 시작일·종료일, 일시중지, 처방 출처) |
| `schedules` | 복용 규칙 (시각, 반복 요일, 적용 시작일) |
| `dose_records` | 회차별 복용 기록 (상태, 그때의 약 이름·복용량) |
| `dose_status_events` | 상태 변경 이력 |
| `user_settings` | 계정별 설정 (알림, 개인정보, 앱 잠금, 시간대 정책) |

폰 단위 설정(접근성, 온보딩을 봤는지)과 로그인 기억은 SharedPreferences에 둡니다.

### 권한

| 권한 | 쓰는 곳 |
|---|---|
| `POST_NOTIFICATIONS` | 복약 알림 (Android 13 이상에서 사용자에게 물어봄) |
| `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM`(Android 12 이하) | 정확한 시각의 알람 |
| `RECEIVE_BOOT_COMPLETED` | 폰을 다시 켠 뒤 알람 복구 |
| `ACCESS_NETWORK_STATE` | 오프라인 상태 화면 (S38) |
| `USE_BIOMETRIC` | 앱 잠금의 본인 확인 (S41) |

인터넷 권한은 없습니다.

### 폴더 구조

```
app/src/main/java/com/cau/medicheck
├── MainActivity.kt, MediCheckApplication.kt   # 앱 시작점, AppContainer
├── data/
│   ├── local/        # Room: Entity, DAO, AppDatabase / SharedPreferences 저장소
│   ├── repository/   # Repository (DAO를 감싸 화면과 로직에 제공)
│   ├── backup/       # 백업 파일(JSON) 읽고 쓰기
│   ├── export/       # 사용자가 고른 파일에 쓰기·읽기
│   └── system/       # 네트워크 상태
├── domain/           # 순수 Kotlin 로직 (완료율, 기록 생성, 알림 계산, 검사 규칙) — 단위 테스트 대상
├── notification/     # 알람 예약·수신, 알림 표시
└── ui/
    ├── theme/        # 색·글꼴·간격
    ├── components/   # 공통 부품
    ├── navigation/   # 화면 주소와 이동 (graph/ 아래에 화면 묶음별 파일)
    ├── lock/         # 앱 잠금
    └── screens/      # 화면: auth, dose, medication, records, stats, notification, settings, extras
app/src/main/assets/requirements_107.csv        # 기능 107개 목록 (S45·S46이 읽음)
app/src/test/                                   # JUnit 단위 테스트
docs/DB_DESIGN.md                               # DB 설계 문서
```

DB를 쓰는 화면은 "그리기만 하는 Composable + ViewModel(StateFlow) + Repository + DAO" 구조를 따릅니다. 보여 줄 데이터가 없는 화면(도움말, 시안 등)은 ViewModel이 없고, 접근성 설정과 오프라인 상태 화면은 Repository 대신 기기 설정 저장소·네트워크 상태 도구를 직접 씁니다.

## 개발 현황

<details>
<summary>우선순위별 구현 현황 보기</summary>

| Priority | Features | Status |
|---|---|---|
| Required | 로그인 및 사용자 관리 | 완료 |
| Required | 약 정보 CRUD | 완료 |
| Required | 복약 일정 생성 및 조회 | 완료 |
| Required | 복용 완료 및 건너뜀 기록 | 완료 |
| Required | 설정 시간 복약 알림 | 완료 |
| Required | Room 데이터 저장 | 완료 |
| Required | 주요 화면 및 화면 이동 | 완료 (47개 화면) |
| Required | 복약 이력 조회 | 완료 |
| Improvement | 주간 복약률 계산 및 시각화 | 완료 |
| Improvement | 입력값 검증 및 예외 처리 | 완료 |
| Additional | 복용 누락 시간대 분석 | 완료 (S15 — 건너뜀은 시간대별, 기한이 지난 미기록은 합계) |
| Additional | 재알림 및 월간 통계 | 완료 (S17, S14) |

</details>

계획한 기능은 모두 `main`에 들어가 있습니다. 그 밖에 프로토타입 검토에서 나온 기능(백업·복원, 앱 잠금, 접근성 설정, 계정 삭제, CSV 내보내기 등)도 구현했습니다.

아직 하지 않은 확인: 실제 폰에서의 동작, TalkBack(화면 읽어 주기), 글자 200%에서 전 화면 보기, Android 8 ~ 10에서의 앱 잠금.

## 기여 및 협업 규칙

원활한 협업과 안정적인 코드 관리를 위해 다음과 같은 GitHub 협업 규칙을 적용합니다.

### 작업 흐름 한눈에 보기

```
Issue 생성 → 브랜치 생성 → 커밋 → Push → Pull Request → (리뷰) → Merge → 브랜치 삭제
```

**작업 완료 기준(Definition of Done):** 해당 PR이 `main`에 Merge되고 연결된 Issue가 닫혔을 때 작업이 완료된 것으로 봅니다.

### Branch Strategy

| Branch | Description |
|---|---|
| `main` | 항상 빌드 가능한 안정 버전 유지. 직접 Push 금지 |
| `feature/*` | 신규 기능 개발 |
| `fix/*` | 버그 수정 |
| `docs/*` | 문서 작성 및 수정 |

모든 작업은 `main`에서 분기한 별도의 작업 브랜치에서 진행합니다.

### Branch Naming Convention

`종류/이슈번호-짧은설명` 형식을 사용합니다. 이슈번호는 실제 GitHub Issue 번호입니다.

| 작업 종류 | 형식 | 예시 |
|---|---|---|
| 신규 기능 | `feature/이슈번호-설명` | `feature/3-login-screen` |
| 버그 수정 | `fix/이슈번호-설명` | `fix/12-duplicate-notification` |
| 문서 | `docs/이슈번호-설명` | `docs/1-readme` |

하나의 작업 단위는 **하나의 GitHub Issue와 하나의 브랜치**를 사용하는 것을 원칙으로 합니다.

### Branch Creation Rules

작업 시작 전 반드시 최신 `main`을 받아온 뒤 브랜치를 만듭니다.

```bash
git checkout main
git pull
git checkout -b feature/이슈번호-짧은설명
```

### Commit Message Convention

`타입: 작업 내용` 형식을 사용합니다.

| Type | Description | Example |
|---|---|---|
| feat | 새로운 기능 추가 | `feat: 로그인 화면 추가` |
| fix | 버그 수정 | `fix: 알림 중복 오류 수정` |
| docs | 문서 수정 | `docs: README 작성` |
| refactor | 코드 구조 개선 | `refactor: Repository 구조 개선` |
| test | 테스트 코드 작성 및 수정 | `test: 로그인 기능 테스트` |
| chore | 설정 및 기타 작업 | `chore: 프로젝트 설정 변경` |

### Pull Request & Merge Rules

1. 기능 개발이 완료되면 브랜치를 Push하고 Pull Request를 생성합니다. (base: `main`)
2. PR 본문은 자동으로 채워지는 **PR 템플릿**(`.github/PULL_REQUEST_TEMPLATE.md`)에 따라 작성합니다.
   - 작업 내용, 테스트 결과, 관련 Issue(`Closes #번호`)를 적습니다.
   - `Closes #번호`를 적으면 Merge 시 해당 Issue가 자동으로 닫힙니다.
3. 다른 팀원이 Files changed를 확인하고 의견을 남기거나 Approve합니다. (본인 PR은 본인이 승인할 수 없습니다)
   - 지금 저장소 설정은 **필수 승인 수가 0**입니다(2026-11-12 변경). 리뷰어가 없을 때는 작성자가 직접 확인한 뒤 Merge할 수 있습니다.
4. **PR 작성자가 직접 Merge**합니다.
5. Merge 완료 후 작업 브랜치를 삭제합니다.
6. 각자 로컬에서 `git checkout main && git pull`로 최신 상태를 받습니다.

**Merge Method:** Create a merge commit (Squash / Rebase 사용 안 함)

### Main Branch Protection

`main` 브랜치에는 GitHub Ruleset(`protect-main`)으로 다음 규칙이 적용되어 있습니다.

- 직접 Push 금지 — Pull Request를 통한 병합 필수
- 필수 승인 수: 0 (2026-11-12에 1에서 0으로 변경)
- 승인 후 새 커밋이 추가되면 승인 자동 취소
- Force Push 금지
- 브랜치 삭제 금지

※ 위 항목은 GitHub가 강제합니다. 브랜치 이름 및 커밋 메시지 규칙은 팀원 간 협의로 준수합니다.

### Prohibited Actions

- `main` 브랜치에 직접 커밋 및 Push
- `git push --force`
- `git rebase`
- 다른 팀원의 브랜치에 허락 없이 커밋
- Pull Request 없이 `main`에 변경을 넣는 일

## 팀과 역할

| 이름 | 역할 | GitHub |
|---|---|---|
| 전수환 (Jeon SuHwan) | Database & Authentication | [@you661012](https://github.com/you661012) |
| Baptiste Zetler | Medication Scheduling & Notifications | |
| 강영곤 (Kang YeongGon) | UI/UX & Statistics | |
| 서민준 (Seo Minjoon) | Planning, Documentation & Presentation | |

<details>
<summary>팀원별 상세 담당 업무 보기</summary>

### 전수환 — Database & Authentication
- Android 프로젝트 초기 설정
- 데이터베이스 구조 설계
- Room Entity 및 DAO 구현
- 회원가입, 로그인 및 로그아웃 구현
- 약 정보 CRUD 구현
- 사용자별 데이터 관리

### Baptiste Zetler — Medication Scheduling & Notifications
- 복약 일정 생성 및 계산
- 날짜별 복용 예정 목록 생성
- 복약 알림 예약 및 취소
- 복약 일정 변경 시 알림 갱신
- 복용 완료 및 건너뜀 상태 처리
- 알림 관련 예외 상황 처리

### 강영곤 — UI/UX & Statistics
- 앱 전체 화면 구조 설계
- 로그인 및 회원가입 화면 구현
- 홈 대시보드 구현
- 약 등록 및 수정 화면 구현
- 날짜별 복약 기록 화면 구현
- 주간 복약률 계산 및 통계 시각화
- 공통 UI 컴포넌트 및 화면 이동 구현

### 서민준 — Planning, Documentation & Presentation
- 프로젝트 기획 및 요구사항 정의
- 기능 명세서 및 사용자 시나리오 작성
- 팀원별 작업 일정 및 진행 상황 관리
- GitHub Issues 기반 작업 관리
- 기능별 테스트 및 오류 검증
- 프로젝트 문서화 및 발표 자료 제작

</details>

## 개발 프로세스

프로젝트는 다음 7단계로 진행합니다.

| Phase | Development Process |
|---|---|
| 01 | Scoping — 개발 범위 및 요구사항 정의 |
| 02 | Role Distribution — 역할 분담 및 GitHub 환경 설정 |
| 03 | Initial Implementation — 핵심 기능 개발 |
| 04 | Feature Testing — 개별 기능 테스트 |
| 05 | Integration & Validation — 모듈 통합 및 검증 |
| 06 | Bug Fixing & Optimization — 오류 수정 및 최적화 |
| 07 | Final Review & Approval — 최종 검토 및 APK 완성 |

### Task Management

- GitHub Issues를 활용하여 작업 관리
- 기능별 Issue 생성 및 담당자 지정
- 작업 단위별 진행 상황 관리
- Pull Request를 통한 코드 리뷰 및 병합
- 개발 과정에서 발생한 오류 및 해결 과정 기록

## 최종 산출물

- Android Application APK
- GitHub Source Code Repository
- 프로젝트 기획 및 요구사항 문서
- 기능 명세서 및 사용자 시나리오
- 테스트 결과 및 오류 수정 내역
- 최종 발표 자료

## 프로젝트 목표

MediCheck는 단순한 약 복용 알림을 넘어 사용자가 자신의 복약 일정과 이력을 체계적으로 관리할 수 있도록 돕는 애플리케이션을 목표로 합니다.

사용자별 데이터 관리, 안정적인 복약 알림, 복용 기록 및 통계 기능을 구현하여 일상 속에서 지속적으로 활용할 수 있는 복약 관리 서비스를 개발하고자 합니다.

**MediCheck — Better Medication Habits, Better Daily Life.**
