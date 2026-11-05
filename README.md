# 💊 MediCheck

**A Smart Medication Schedule Management Application**

MediCheck는 사용자가 자신의 약 정보와 복약 일정을 관리하고, 정해진 시간에 알림을 받아 규칙적인 복약 습관을 형성할 수 있도록 지원하는 Android 모바일 애플리케이션입니다.

복용 여부를 기록하고 복약 이력 및 통계를 확인하여 사용자가 자신의 복약 상태를 지속적으로 관리할 수 있도록 하는 것을 목표로 합니다.

> 중앙대학교 *Application Development for Mobile Computer* (2026 Fall) 팀 프로젝트

---

## 📌 1. Project Overview

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

### 🎯 Development Objectives

- 사용자별 약 정보 및 복약 일정 관리
- 설정된 복약 시간에 맞춘 알림 제공
- 복용 완료 및 건너뜀 기록 관리
- 날짜별 복약 이력 및 통계 제공
- 복용 누락 패턴 분석을 통한 복약 습관 관리 지원

---

## ✨ 2. Core Features

### 2.1 User Management
- 회원가입 및 로그인
- 로그아웃
- 사용자 정보 관리
- 사용자별 복약 데이터 구분

### 2.2 Medication Management
- 약 이름 및 복용량 등록
- 복용 시간 및 반복 요일 설정
- 약 정보 조회, 수정 및 삭제
- 복용 시작일 및 종료일 설정

### 2.3 Medication Schedule
- 등록된 약 정보를 기반으로 복약 일정 생성
- 날짜 및 시간별 복약 목록 조회
- 복용 완료 및 건너뜀 상태 기록
- 복약 일정 변경 시 관련 정보 갱신

### 2.4 Medication Notifications
- 설정된 시간에 복약 알림 제공
- 약 등록 및 수정 시 알림 예약 및 갱신
- 약 삭제 시 기존 알림 취소
- 알림 선택 시 관련 화면으로 이동

### 2.5 Medication History & Statistics
- 날짜별 복약 기록 조회
- 주간 복약 완료율 계산
- 복용 누락 횟수 확인
- 복약 이력 시각화

### 2.6 Additional Features
- 복용 누락이 잦은 시간대 분석
- 복약 재알림 기능
- 주간 및 월간 통계 그래프

※ 추가 기능은 필수 기능 구현 완료 후 개발 진행 상황에 따라 적용 여부를 결정합니다.

---

## 🛠️ 3. Tech Stack

| Category | Technology | Purpose |
|---|---|---|
| Language | Kotlin | Android Application Development |
| UI | Jetpack Compose (Material 3) | UI Development |
| Database | Room Database | Local Data Storage |
| Notification | AlarmManager | Medication Reminder Scheduling |
| Background Task | WorkManager | Background Task Management |
| Navigation | Navigation Compose | Screen Navigation |
| Architecture Support | ViewModel | UI State Management |
| IDE | Android Studio | Development & Testing |
| Collaboration | GitHub | Version Control & Team Collaboration |

- Minimum SDK: **API 26 (Android 8.0)**
- Package: `com.cau.medicheck`

※ Authentication 방식은 개발 초기 단계에서 Firebase Authentication 사용 여부 등을 검토하여 확정합니다.

---

## 👥 4. Team Members & Responsibilities

| 이름 | 역할 | GitHub |
|---|---|---|
| 전수환 (Jeon SuHwan) | Database & Authentication | [@you661012](https://github.com/you661012) |
| Baptiste Zetler | Medication Scheduling & Notifications | |
| 강영곤 (Kang YeongGon) | UI/UX & Statistics | |
| 서민준 (Seo Minjoon) | Planning, Documentation & Presentation | |

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

---

## 🔀 5. GitHub Collaboration Rules

원활한 협업과 안정적인 코드 관리를 위해 다음과 같은 GitHub 협업 규칙을 적용합니다.

### 5.1 작업 흐름 한눈에 보기

```
Issue 생성 → 브랜치 생성 → 커밋 → Push → Pull Request → 팀원 1명 Approve → Merge → 브랜치 삭제
```

**작업 완료 기준(Definition of Done):** 해당 PR이 `main`에 Merge되고 연결된 Issue가 닫혔을 때 작업이 완료된 것으로 봅니다.

### 5.2 Branch Strategy

| Branch | Description |
|---|---|
| `main` | 항상 빌드 가능한 안정 버전 유지. 직접 Push 금지 |
| `feature/*` | 신규 기능 개발 |
| `fix/*` | 버그 수정 |
| `docs/*` | 문서 작성 및 수정 |

모든 작업은 `main`에서 분기한 별도의 작업 브랜치에서 진행합니다.

### 5.3 Branch Naming Convention

`종류/이슈번호-짧은설명` 형식을 사용합니다. 이슈번호는 실제 GitHub Issue 번호입니다.

| 작업 종류 | 형식 | 예시 |
|---|---|---|
| 신규 기능 | `feature/이슈번호-설명` | `feature/3-login-screen` |
| 버그 수정 | `fix/이슈번호-설명` | `fix/12-duplicate-notification` |
| 문서 | `docs/이슈번호-설명` | `docs/1-readme` |

하나의 작업 단위는 **하나의 GitHub Issue와 하나의 브랜치**를 사용하는 것을 원칙으로 합니다.

### 5.4 Branch Creation Rules

작업 시작 전 반드시 최신 `main`을 받아온 뒤 브랜치를 만듭니다.

```bash
git checkout main
git pull
git checkout -b feature/이슈번호-짧은설명
```

### 5.5 Commit Message Convention

`타입: 작업 내용` 형식을 사용합니다.

| Type | Description | Example |
|---|---|---|
| feat | 새로운 기능 추가 | `feat: 로그인 화면 추가` |
| fix | 버그 수정 | `fix: 알림 중복 오류 수정` |
| docs | 문서 수정 | `docs: README 작성` |
| refactor | 코드 구조 개선 | `refactor: Repository 구조 개선` |
| test | 테스트 코드 작성 및 수정 | `test: 로그인 기능 테스트` |
| chore | 설정 및 기타 작업 | `chore: 프로젝트 설정 변경` |

### 5.6 Pull Request & Merge Rules

1. 기능 개발이 완료되면 브랜치를 Push하고 Pull Request를 생성합니다. (base: `main`)
2. PR 본문은 자동으로 채워지는 **PR 템플릿**(`.github/PULL_REQUEST_TEMPLATE.md`)에 따라 작성합니다.
   - 작업 내용, 테스트 결과, 관련 Issue(`Closes #번호`)를 적습니다.
   - `Closes #번호`를 적으면 Merge 시 해당 Issue가 자동으로 닫힙니다.
3. **다른 팀원 최소 1명**이 Files changed를 확인하고 Approve합니다. (본인 PR은 본인이 승인할 수 없습니다)
4. 승인 후 **PR 작성자가 직접 Merge**합니다.
5. Merge 완료 후 작업 브랜치를 삭제합니다.
6. 각자 로컬에서 `git checkout main && git pull`로 최신 상태를 받습니다.

**Merge Method:** Create a merge commit (Squash / Rebase 사용 안 함)

### 5.7 Main Branch Protection

`main` 브랜치에는 GitHub Ruleset(`protect-main`)으로 다음 규칙이 적용되어 있습니다.

- 직접 Push 금지 — Pull Request를 통한 병합 필수
- 최소 1명 이상의 승인 필수
- 승인 후 새 커밋이 추가되면 승인 자동 취소
- Force Push 금지
- 브랜치 삭제 금지

※ 위 항목은 GitHub가 강제합니다. 브랜치 이름 및 커밋 메시지 규칙은 팀원 간 협의로 준수합니다.

### 5.8 Prohibited Actions

- `main` 브랜치에 직접 커밋 및 Push
- `git push --force`
- `git rebase`
- 다른 팀원의 브랜치에 허락 없이 커밋
- 승인되지 않은 Pull Request 병합

---

## 📋 6. Development Process

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

---

## 🧪 7. Development Priorities

| Priority | Features |
|---|---|
| Required | 로그인 및 사용자 관리 |
| Required | 약 정보 CRUD |
| Required | 복약 일정 생성 및 조회 |
| Required | 복용 완료 및 건너뜀 기록 |
| Required | 설정 시간 복약 알림 |
| Required | Room 데이터 저장 |
| Required | 주요 화면 및 화면 이동 |
| Required | 복약 이력 조회 |
| Improvement | 주간 복약률 계산 및 시각화 |
| Improvement | 입력값 검증 및 예외 처리 |
| Additional | 복용 누락 시간대 분석 |
| Additional | 재알림 및 월간 통계 |

필수 기능을 우선적으로 완성하고, 이후 개발 일정과 진행 상황을 고려하여 추가 기능을 구현합니다.

---

## 🚀 8. Getting Started

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

---

## 📦 9. Final Deliverables

- Android Application APK
- GitHub Source Code Repository
- 프로젝트 기획 및 요구사항 문서
- 기능 명세서 및 사용자 시나리오
- 테스트 결과 및 오류 수정 내역
- 최종 발표 자료

---

## 🎯 10. Project Goal

MediCheck는 단순한 약 복용 알림을 넘어 사용자가 자신의 복약 일정과 이력을 체계적으로 관리할 수 있도록 돕는 애플리케이션을 목표로 합니다.

사용자별 데이터 관리, 안정적인 복약 알림, 복용 기록 및 통계 기능을 구현하여 일상 속에서 지속적으로 활용할 수 있는 복약 관리 서비스를 개발하고자 합니다.

**MediCheck — Better Medication Habits, Better Daily Life.**
