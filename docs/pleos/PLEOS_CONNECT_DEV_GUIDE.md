# Pleos Connect 앱 개발 레퍼런스 (Claude Code용)

> 출처: Pleos Playground 개발자 문서 — https://document.pleos.ai/docs/pleos-only/getting-started 이하 사이드바 전체(가이드 126페이지 + 심사 가이드라인 7페이지 + API Reference 782페이지)
> 수집일: 2026-11-05 · 작성 목적: Android Studio에서 Pleos Connect(현대자동차그룹 차량 인포테인먼트, AAOS 기반) 앱을 개발할 때 Claude Code가 참고할 단일 레퍼런스
> 함께 제공: `PLEOS_CONNECT_API_REFERENCE.md` — 5개 SDK의 전체 클래스/메서드 시그니처·퍼미션 목록 (자동 추출)
>
> 문서 표기: `⚠️` = 반드시 지킬 규칙(심사/동작 실패 원인), `💡` = 권장 사항. 원문 수치·상수·코드는 가능한 한 그대로 옮겼다.

---

## 0. TL;DR — Claude Code가 항상 지켜야 할 규칙

1. **플랫폼**: Pleos Connect = AAOS(Android Automotive) 기반. 일반 Android 앱 + Pleos Connect SDK(`ai.pleos.playground:*`). 표준 차량 데이터는 AAOS Car API(`CarPropertyManager`, `CarUxRestrictionsManager`, `CarAppFocusManager`), 제조사 특화 기능만 Pleos SDK.
2. **빌드**: `minSdk ≥ 28`(필수, 사이닝 키 로테이션 요건), Maven `https://nexus-playground.pleos.ai/repository/maven-releases/`, 최신 SDK: Vehicle 2.3.2 / NaviHelper 3.0.5 / TextToSpeech 3.3.1 / SpeechToText 3.3.1 / FusedLocation 1.1.3.
3. **에뮬레이터**: Pleos Connect Emulator 2.1.0(API 34 Automotive 이미지), **Cold boot 필수**(quick boot면 VHAL 불가), RAM 4 GB. 차량 상태는 Virtual Console(`adb forward tcp:8088 tcp:8088` → http://127.0.0.1:8088/)로 조작.
4. **SDK 공통 생명주기**: 생성 → `initialize()` → 리스너 등록 → 호출(결과는 전부 **비동기 콜백**) → 리스너 해제 → `release()`. 콜백 끝나기 전 `release()` 금지.
5. **Vehicle SDK**: `Vehicle(context).initialize()` → `getDoor()` 등 도메인 객체 → **먼저 `checkXxxCapabilities`**(지원 여부·AccessType·AreaBounds) → `getCurrentXxx`/`setXxx`/`registerXxx`. 값 범위·지원 여부는 차량마다 다름 → 하드코딩 금지. set 완료 ≠ 실제 반영(재조회/리스너로 확인).
6. **퍼미션**: `pleos.car.permission.CAR_*`(조회) / `CONTROL_CAR_*`(제어) / `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `NAVI_CUSTOM_MAP` / `TTS_SERVICE` / `STT_SERVICE` / `FUSED_LOCATION`(dangerous=런타임 요청). **필요한 것만 선언**, Vehicle 퍼미션은 App Market 사전 승인된 것만(미승인 선언 시 심사 탈락). `PRIVATE_*`/signature 권한(Telltales 등) 사용 불가.
7. **주행 규제**: `CarUxRestrictions`로 주행 중 영상·키보드·설정·로그인·긴 텍스트 등 제한. DO 미선언 앱은 주행 중 사용 불가. 플랫폼이 지역별 제한 비트를 내려주므로 지역 하드코딩 금지.
8. **오디오**: 재생 전 오디오 포커스 요청 + 포커스 변경 이벤트 처리 필수. 내비 안내는 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`. Gleo 호출(LOSS_TRANSIENT) 시 즉시 정지. Suspend 진입 시 포커스 반납, Resume 시 자동 재생 금지. 음악 앱은 MediaBrowserService/MediaSession 필수 + 자동 재생 금지.
9. **화면**: 멀티 윈도우(앱 최대 2개). **portrait/landscape 레이아웃 모두** 지원, 레터박스·호환성 모드 금지, 백그라운드 복귀 시 상태 복원. CCP Back(최상단) → `moveTaskToBack(true)`(종료 금지).
10. **성능·응답**: CPU 주행 중 비내비 <5%(내비 ≤15%), 정차 ≤25%, RAM ≤400 MB, 입력 응답 ≤0.25 s, 실행/로딩 ≤10 s, 버튼 반응 ≤2 s.
11. **보안**: HTTPS/TLS1.2+, 비밀값 하드코딩 금지(Keystore, AES-GCM), 로그에 PII 금지, 불필요 컴포넌트 비공개, 배포 빌드 디버깅 off, 자동 백업 비활성화 권장.
12. **배포**: APK ≤600 MB, 버전 매 제출마다 증가, 자동 업데이트 없음 → 앱이 버전 체크 후 `market://detail?package=...`로 유도.

---

## 1. 플랫폼 개요

- **Pleos Connect** = 현대자동차그룹의 차량용 인포테인먼트 OS. **Android Automotive OS(AAOS)** 기반이며, 일반 Android 앱 + Pleos Connect SDK(차량 전용 API)로 앱을 만든다.
- **Pleos Playground** = 개발자 포털 (https://developers.pleos.ai / https://pleos.ai/playground). 프로젝트 생성, SDK 사용 신청, 앱 제출(Pleos App Market Console)을 한다.
- **Pleos App Market** = 차량 내 앱 마켓. 앱 배포 채널.
- **Gleo AI** = Pleos Connect의 음성 AI 비서. 앱이 스키마를 제공하면 음성으로 앱 기능을 호출할 수 있다 (§9).
- **Pleos Connect SDK** 5종 (Maven group `ai.pleos.playground`):

| SDK | 용도 | 신청 필요 여부(문서 기준) |
|---|---|---|
| Vehicle SDK | 차량 상태 조회/제어(도어, 공조, 시트, 배터리, 기어 등 18개 도메인) | Vehicle State: 신청 없이 사용 가능 / Vehicle Control: 준비 중(Coming Soon) |
| NaviHelper SDK | 순정 내비게이션에 목적지/경로/경유지 요청, 내비 상태 수신 | 신청 없이 사용 가능 |
| TextToSpeech SDK | 차량 TTS 엔진으로 음성 합성 | 준비 중 → 신청 필요 (클라이언트 ID/시크릿 필요) |
| SpeechToText SDK | 차량 STT 엔진으로 음성 인식 | 준비 중 → 신청 필요 (클라이언트 ID/시크릿 필요) |
| FusedLocation SDK | 차량 센서 융합 위치(GNSS+DR) | 준비 중 → 신청 필요 |

- 문서 체계(사이드바): 시작하기 → 프로젝트 생성/SDK 신청 → 개발 환경 구성 → 앱 기획·설계 가이드(설계 원칙 + 디자인 가이드) → 앱 개발·연동 가이드(앱 개발 가이드, SDK 사용 가이드, Gleo Interface) → 앱 테스트 가이드 → 앱 등록 가이드 → Tutorials & Samples → Release note.

---

## 2. 빠른 시작 (Getting Started)

### 2.1 사전 준비
- Android Studio(최신), **Android SDK API 34**, 앱 **`minSdk` ≥ 28**, Pleos Connect Emulator system image
- 소프트웨어 개발 환경 표:

| 구분 | 버전 |
|---|---|
| Minimum Android SDK | **API 28 이상** (⚠️ 차량 사이버보안 요건: 사이닝 키 로테이션·리니지 지원 때문) |
| Gradle | 8.0 이상 |
| 언어 | Java, Kotlin |
| Pleos Connect Emulator | 2.1.0 |
| Virtual Console 지원 최소 Emulator 버전 | 2.1.0 |

### 2.2 프로젝트 생성 (Android Studio)
1. File > New > New Project → Templates에서 **Car** 또는 **Automotive** 카테고리 → **Empty Activity**
2. 언어 **Kotlin**, Minimum SDK **API 28 이상** → Finish → `app` 모듈 빌드 확인

### 2.3 Maven 저장소 & 의존성
`settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
        }
    }
}
```
`app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.1")   // Getting Started 문서 기준 버전
}
```
→ Sync Now. 의존성을 못 찾으면 Maven 저장소 접근 권한과 SDK 버전을 확인. (SDK별 최신 버전은 §8 각 SDK 및 §12 릴리스 노트 참고)

### 2.4 퍼미션 선언 (`AndroidManifest.xml`)
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.CAR_DOORS" />
    <application ...> ... </application>
</manifest>
```

### 2.5 첫 API 호출
```kotlin
val vehicle = Vehicle(context)
vehicle.initialize()

val door = vehicle.getDoor()
door.getCurrentDoorPosition(
    area = DoorArea(DoorValue.ROW_1_LEFT),
    onComplete = { pos: Int? ->
        // Use the current door position.
    },
    onFailed = { e: Exception ->
        // Handle the Vehicle SDK error.
    }
)
// release()는 여기서 호출하지 말 것.
// 앱이 Vehicle SDK를 더 이상 쓰지 않을 때(예: onDestroy) vehicle.release() 호출.
```
- `onComplete`/`onFailed`는 **비동기 콜백**. `release()`는 콜백 처리 완료 후 또는 생명주기 종료 시점에서.

### 2.6 에뮬레이터에서 확인 & 실패 시 체크리스트
에뮬레이터 완전 부팅 → 실행 대상 기기로 선택 → Run. 값이 안 읽히면:
`minSdk ≥ 28` / `pleos.car.permission.CAR_DOORS` 선언 / 의존성 동기화 / 에뮬레이터 정상 실행 / `onFailed` 예외 메시지 확인.

### 2.7 다음 단계(공식 권장 순서)
설계 가이드 → 개발 가이드 → SDK/API 문서 선택 → Vehicle 권한·Capability·Get API → Vehicle SDK Sample App → Virtual Console로 테스트 → 앱 등록·제출(Pleos App Market).

---

## 3. 프로젝트 생성 및 SDK 사용 신청 (Pleos Playground)

1. **회원가입**: developers.pleos.ai → 로그인 → "Pleos 계정 만들기" → 약관 동의 → 가입
2. **프로젝트 생성**: GNB **Workspace > My Project > 프로젝트 생성** → 프로젝트 이름(변경 가능), 개발사명, **카테고리**(앱 개발에 필수 정보 — 처음부터 정의 권장), 애플리케이션 설명 → 생성
3. **프로젝트 정보 화면**에서 확인할 값:
   - **키 정보 > 프로젝트 ID(Key)**: 앱 심사 요청 시 Pleos App Market Console에 입력
   - **SDK > 클라이언트 ID / 클라이언트 시크릿**: 서버 연결이 필요한 SDK(TextToSpeech, SpeechToText)의 앱 등록에 사용
   - 테스트 정보 > CRN: **Emulator 2.1.0 이후로는 CRN 입력 불필요**
4. **SDK 사용 신청**: 프로젝트 정보 > 좌측 **SDK** → 사용할 SDK 선택 → 상태 확인
   - `준비 중`(Coming Soon 배지, 신청 불가): FusedLocation, TextToSpeech, SpeechToText, Vehicle Control SDK
   - `사용 가능`: 신청 없이 사용 가능하거나 승인 완료. **Vehicle State, NaviHelper는 신청 없이 바로 사용 가능**
   - `신청 필요` → 신청 제출 → `검토 중` → 승인 시 `사용 가능` / 반려 시 `반려됨`
   - `제휴 계약 필요`: 웹 신청 불가, 서비스 계약 담당자와 별도 제휴 계약 후 사용

---

## 4. 개발 환경 구성 (Emulator / Virtual Console)

### 4.1 Pleos Connect Emulator 설치
- 이전 버전이 있으면 삭제 후 진행
1. Android Studio **Tools > SDK Manager > SDK Update Sites** → **+**
2. Name: `Pleos Connect System Image`
   - URL (Mac / Apple Silicon): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
   - URL (Windows/Linux): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
3. **SDK Platforms** 탭 → API Level **34**의 *Pleos Connect system Image* 체크 (Show Package Details 활성화) → OK → 설치 → Finish → **Android Studio 재시작**

### 4.2 AVD 생성
1. **Tools > Device Manager > Create device** (구버전: AVD Manager > Create Virtual Device)
2. Form Factor **Automotive** → `Pleos Connect v2.1.0` → Next
3. Advanced Settings: **Startup > Default boot = `Cold`** (구버전: Graphics=Hardware, Boot option=Cold boot)
4. Emulated Performance: **RAM 4 GB**
5. Device 탭 > Select system image > API `34` → `Pleos Connect v2.1.0 APIs` → Finish → Device Manager에서 실행

> ⚠️ **Cold boot 필수**: `quick boot`로 설정하면 **VHAL을 이용할 수 없다** (차량 API가 동작하지 않음).

### 4.3 Emulator 설정
- **GPU 필수**. 권장: NVIDIA RTX 4060/5060 Laptop 8GB 이상, AMD RX 7700S/7600M XT 8GB급 이상 / 최소: RTX 3060 Laptop 6GB, AMD RX 6600M 8GB 또는 RX 6700S 8GB
- 기본 언어 한국어. 영어 전환: **차량 설정 > 일반 설정 > 언어/Language > English**
- **마이크(STT 테스트용)**: Emulator Extended Controls > Microphone에서 오디오 입력 수신 허용
- Bluetooth 동글 연동: 미지원
- 트러블슈팅: Android Emulator 알려진 문제 / Android Studio·AGP 알려진 문제 공식 문서 참고

### 4.4 Pleos Connect Virtual Console (차량 시뮬레이터)
Emulator에서 차량 상태·이벤트를 조작하는 웹 기반 도구(Emulator ≥ 2.1.0).
```bash
adb forward tcp:8088 tcp:8088
# PC 브라우저에서
http://127.0.0.1:8088/
```
- **Emulation Controls** 도메인: Vehicle(차량 알림 표시/숨김), Motion(기어·브레이크·조향각·주행거리·핸들 열선), ADAS(크루즈 준비/활성·목표 속도), Convenience(도어·창문·트렁크·시트·조명·와이퍼·ETCS·글로브박스), Safety(주차브레이크·안전벨트·ABS 등 경고등 꺼짐/켜짐/점멸), Energy(주행가능거리·배터리 용량·충전 상태·충전 전류·목표 충전량·충전 포트), Climate(공조 전원·온도·풍량·풍향·내기순환·제상·시트 열선/통풍·공기청정)
- 테마: 기어 P에서 Highway/Forest 테마, Highway에서 1인칭/3인칭 시점 (테스트 기능에는 영향 없음)
- 접속 불가 시: Emulator 실행 여부 → Virtual Console 지원 이미지인지 → `adb devices` → `adb forward` 재실행 → PC 8088 포트 점유 여부 → 브라우저 재접속
- 상세 카드/범위는 §10.2 참고

---

## 5. 앱 설계 원칙 (앱 기획·설계 가이드)

### 5.1 주행 안전 (Driver Distraction)
- [AAOS 운전자 주의 분산 가이드라인](https://source.android.com/docs/automotive/driver_distraction/guidelines?hl=ko) 준수.
- 앱은 **`CarUxRestrictions`** 로 주행 중 제한 상태를 확인하고 제한 대상 기능의 UI/동작을 축소·비활성화해야 한다. ⚠️ 주행 규제 상태에서 **영상 재생 시작/재개 금지**. 오디오는 오디오 포커스 정책(§7.2) 준수.
- 시선 분산 최소화: 운전 정보는 짧은 시간에 이해되게 간결히. 주행 중 동영상·애니메이션·움직이는 이미지 최소화/제한, 장식적·반복적 움직임 지양. HUD·클러스터·OSD 영역 사용 시 해당 정책 준수.
- 정보 구조: 우선순위 높은 기능을 첫 화면·상단에, 최소 조작으로 도달. 드롭다운·다단 메뉴 남용 금지. 자동 애니메이션·반복 화면 전환 최소화.
- 터치: 큰 터치 영역, 높은 대비, 텍스트 최소화. 음성/물리 버튼 연계. 게임·인터넷 서핑·피트니스 등 주의 분산 활동 유도 금지.
- 텍스트 입력 최소화: 검색·로그인도 프리셋 선택·최근 항목·음성 입력 우선. 음악 앱은 프리셋 플레이리스트·최근 재생·즐겨찾기를 첫 화면에.

### 5.2 지역별 주행 규제 정책
- 주행 규제는 속도·기어·브레이크 등 하드웨어 신호 + 지역별 규정으로 적용. 플랫폼 버전/양산 차량에 따라 다를 수 있음.
- **Distraction Optimization(DO) 미선언 앱**: Parking(주차) = 사용 가능 / **Drive(주행) = 사용 불가**
- 규제 상태에서 기능별 허용(✅ 가능, ⛔ 제한, 🔊 음성 한정). 괄호는 `CarUxRestrictions` 플래그 값:

| 기능 (관련 플래그) | 북미 Driving | 유럽 Driving | 그 외 지역 Driving |
|---|---|---|---|
| 키보드 (`UX_RESTRICTIONS_NO_KEYBOARD`=8) | ⛔ | ✅ | ✅ |
| 다이얼패드 (`NO_DIALPAD`=1) | ⛔ | ✅ | ✅ |
| 검색(IVI 통합 검색) (`NO_KEYBOARD`=8, `NO_FILTERING`=2) | 🔊 음성 한정 | ✅ | ✅ |
| 미디어 앱 로그인·설정·검색 (`NO_SETUP`=64, 8, 2) | ⛔ | ⛔ | ✅ |
| 디지털키·도어·시트·차량 설정 (`NO_SETUP`=64) | ⛔ | ⛔ | ⛔ |
| 동영상 시청 (`NO_VIDEO`=16) | ⛔ | ⛔ | ⛔ |
| 내비게이션 검색 (8, 2) | ⛔ | ✅ | ✅ |
| QR 로그인 (`NO_SETUP`=64) | ⛔ | ⛔ | ⛔ |
| 매뉴얼(대용량 텍스트) (`LIMIT_CONTENT`=32, `LIMIT_STRING_LENGTH`=4) | ⛔ | ⛔ | ⛔ |

  Parking(및 정차)에서는 모든 지역에서 전부 ✅. InfoLINK용 앱은 지역별 키보드 분기 처리가 필요할 수 있음(partnership@pleos.ai 문의).

### 5.3 앱 카테고리
Maps/Navigation(위치 기반 길 안내), Mobility Services(이동 수단/서비스), Lifestyle, Entertainment, Business, Music/Audio, Kids, Social/Communication. 설계 시점부터 카테고리를 명확히 정의.
주행 중 **제한적으로 허용 가능한** 기능 예시:
- **Music/Audio**: 앨범 아트 등 단순 시각 정보, 재생/일시정지/곡 넘기기, 스트리밍 청취, 라디오 채널 변경, 오디오북 재생
- **Maps/Navigation**: 경로 안내·실시간 교통, 간결한 지도, TBT 음성 안내, 목적지 경로 탐색, 실시간 빠른 길, 주변 교통 상황
- **Mobility Services**: 주행 관련 차량 관리, 현재 위치 기반 즉각 정보, EV 충전소 찾기·경유지 설정, 주유소 탐색, 주차장 위치 확인

### 5.4 앱 구조 및 동작 모델 (멀티 윈도우)
- 화면 = **Driving view**(운전자 쪽 고정 영역, 앱이 가리거나 종료 불가) + **앱 영역**(최대 2개 앱 window).
- `portrait` = 앱이 window 1개 사용(2개 앱 동시 표시 시 기본) / `landscape` = 앱 하나가 앱 영역 2칸 확장 사용.
- ⚠️ **portrait와 landscape 레이아웃을 각각 설계**: portrait엔 핵심 기능, landscape엔 확장 정보/기능.
- `screenOrientation` 미지정 → portrait(1 window). `screenOrientation="landscape"` → 2 window 영역 사용. 반응형 레이아웃 필수.
- 새 앱은 현재 표시 앱의 orientation 기준으로 열림(landscape 1개 표시 중 → landscape, portrait 2개 → primary window에 portrait).
- 화면에서 사라진 앱은 **종료되지 않고 백그라운드에 남음** → 다시 foreground로 돌아오는 경우 대비. 사용자는 window bar/제스처로 위치·크기 변경.
- 포그라운드 진입 시 상태 복원 + 크기/방향에 맞게 UI 재구성. 백그라운드 전환 시 화면 갱신·애니메이션·불필요 센서 수집 중단. 백그라운드 지속 작업(오디오·통신·다운로드)은 실행/중단/재개 정책 명확화. 불필요 상주·타이머·폴링·네트워크 최소화. 프로세스 재생성 대비 상태 저장.

### 5.5 하드웨어 접근 정책
- ⚠️ 차량 하드웨어·센서에 **직접 접근 금지**. 차량 상태는 **Pleos Connect SDK의 표준 플랫폼 API**로 확인·제어.
- 모바일 전용 API가 차량에서 미제공/제한될 수 있음: Telephony, SMS, 일부 센서(자이로, 근접 등), 캘린더, 주소록 등 → 제거하거나 지원 여부 확인 후 동작하는 Optional 구조로.
- 주행 중 네트워크 불안정 대비: 최근 콘텐츠 로컬 캐싱, Retry 로직·적절한 타임아웃, 오프라인 모드 안내 UI.

### 5.6 SDK API 정책 (AAOS Alignment)
- **표준 우선**: 동일/유사 기능이 AAOS 표준에 있으면 표준 API 사용 권장, SDK는 중복 기능을 제공하지 않음.
- SDK 전용 API는 AAOS로 접근 불가하거나 제조사 고유 보안·특화 제어가 필요한 영역에만.
- SDK API가 AAOS 표준으로 편입돼도 **최소 2개 메이저 버전까지 하위 호환 유지**.
- 선택 기준: 표준 데이터(속도, 기어, 외기온, 연료 레벨 등) → **AAOS Car API** / 특화 기능(제조사 전용 시트 제어, 특화 라이팅, 고유 보안 인증 등) → **Pleos Connect SDK**.

### 5.7 API 호환성 및 가용성
| SDK | 차량 모델별 호환성 |
|---|---|
| Vehicle SDK | 모델에 따라 다름 → 도메인별 **`checkXXXCapabilities`** 로 확인 |
| NaviHelper SDK | 내비게이션 탑재 차량에 한해 전체 호환 |
| TextToSpeech / SpeechToText | 전체 호환 |
| FusedLocation SDK | 모델에 따라 다름 → **`isSupported`** 로 확인 |
- AAOS Vehicle Property: [`VehiclePropertyIds`](https://developer.android.com/reference/android/car/VehiclePropertyIds)로 정의 확인, [`CarPropertyManager`](https://developer.android.com/reference/android/car/hardware/property/CarPropertyManager)로 실제 지원/접근 확인. ⚠️ **차량 모델명으로 분기하지 말 것**. SDK가 제공하지 않는 특수 프로퍼티가 필요하면 구현 전 기술 지원팀에 검토 요청.

### 5.8 보안·개인정보
- 차량은 여러 사람이 공유 → 소유자 변경/운전자 교체 시 쉬운 로그아웃·계정 전환.
- 소셜 로그인/OTT 토큰은 **Keystore 등 안전한 스토리지**에 저장.
- 위치·음성·계정 요구 시 "왜 필요한지" 명확히, 동의 플로우 단순화. 불필요한 PII 저장 금지, 로그의 개인정보는 마스킹.
- 텔레메트리: 차량 ID/사용자 ID 익명화·가명화 정책 수립, 운전 중 행동 데이터를 마케팅에 쓰지 않도록 구분. (등록 심사 보안 기준은 §11.4)

### 5.9 성능·리소스
- Cold start 시 스플래시에서 오래 머물지 않게 최소 초기화 + 지연 로딩.
- ⚠️ **CPU 사용률 기준**: 내비 앱(주행 중) ≤ 15% / 내비 외 앱(주행 중) < 5% / 모든 앱(정차 중) ≤ 25%
- ⚠️ **RAM ≤ 400 MB**
- 백그라운드 장시간 작업·타이머·폴링 최소화, RecyclerView·캐시 전략으로 메모리 최적화.
- 오디오/미디어 앱은 Pleos Connect 오디오 포커스 정책 준수 + 포커스 획득/상실 대응 로직 필수(§7.2).

---

## 6. UI 디자인 가이드 (Pleos Connect UI Design Guide)

> 디자인 가이드는 대부분 이미지(Figma 스펙)로 되어 있어 텍스트로 옮길 수 있는 규칙·수치만 정리. 디자인 리소스(“Pleos Connect display guide”, “Pleos Connect design assets” Figma)는 UI Design Guide 인트로 페이지 하단에서 다운로드.

### 6.1 기본 원칙
- **Easy & Safe**: 운전 중 시각·인지 부담 최소화. 운전 중 동영상·애니메이션 이미지·**텍스트 자동 스크롤 금지**. **한 손 조작**만 요구(양손 금지), 운전자 손·발 움직임 유도 금지.
- **Consistency**: 아이콘·용어·인터랙션 패턴 일관, 기본 탑재 UI와 일관성 유지.
- **Glanceability**: 1~2초 안에 파악 가능하게 핵심 정보만. ⚠️ **입력 후 시스템 응답 ≤ 0.25초**, 지연 시 로딩 상태 시각 표시.
- **색상/대비**: 아이콘·텍스트·이미지 대비율 **≥ 4.5:1**, 과도한 색상 지양.
- **텍스트**: 영문 단일 메시지 ≤ 120자 또는 24단어 / 일본어 ≤ 30자 / 한 줄 ≤ 80자 / 문장·단락 ≤ 3줄. 중요 정보 기본 텍스트 높이 **≥ 5 mm**, 보조 텍스트 **≥ 3 mm**.
- **아이콘/피드백**: 국제 통용 아이콘 + 텍스트 라벨, 햅틱·음성 피드백.
- **알림**: 간결, 운전자에게 필요한 경우만. ⚠️ 알림으로 **광고 금지**(광고주/제품명 외 텍스트 광고 금지).
- 주간/야간 모드 자동 밝기·대비 조정, Gleo AI 음성 인터랙션 활용 검토(핸즈프리, 시작/종료/중단은 수동 가능), OTA 업데이트 고려, 다국어·다양한 해상도/비율 대응.

### 6.2 Display Layout
- 구성: ① **Status bar**(Driving status, Gleo AI·검색, 시계, 네트워크, 마이크, 알림센터, 프로필) ② **Driving view**(Telltale, 속도, 차량 상태) ③ **GNB**(App library, 즐겨찾는 앱, 공조) ④ **Application window**(앱 1~2개).
- ⚠️ 가로 전용 게임 등 단일 모드 앱을 제외한 **모든 앱은 Landscape/Portrait 두 레이아웃에 맞게 조절**되어야 함.
- Status bar, GNB의 App library·공조 패널, 키보드 등 System UI는 앱보다 상위 레이어 → 일시적으로 앱 window를 가릴 수 있음.
- 화면 유형: **Connect-S**(Standard; 17", 14.6", 12.9"(구 Connect-L)) / **Connect-W**(Wide, 고급 차종; 세로 확장형 Connect-W-Extended) / **Connect-WF**(Waterfall, Wide 대비 작은 해상도).

### 6.3 Window Policy
| | Connect-S | Connect-W |
|---|---|---|
| 화면 비율 | 앱 2개: 1:1 / 앱 1개: 2:0 (기본 1:1) | 2:1 또는 1:2 (기본 2:1) |
| 기본 화면 | Primary: Navigation 앱 | Primary: Navigation, Secondary: Media(재탑승 시 마지막 Media 앱) |
| 앱 실행 | 이전 앱 orientation 따름 (landscape 1개→landscape, portrait 2개→primary에 portrait) | 신규 앱은 항상 landscape로 primary에, 기존 primary→secondary, secondary는 사라짐 |
| 위치 변경 | 3-finger swipe 또는 window bar 탭 | 동일 |
| 크기 변경 | 3-finger swipe, window bar 드래그, 앱 window 버튼 탭(landscape↔portrait) | 동일 |
| 종료 | 3-finger swipe. 최근 활성 앱 순으로 다시 표시. **Navigation(기본 앱)은 종료 불가** | 동일 (primary 종료 시 secondary가 orientation 유지한 채 primary로) |
- Connect-W: 최대 실행 앱 2개, App library 운전석·조수석 양쪽, 조수석 App library에서 실행한 앱은 조수석 window에서만 실행.
- 프리로딩 앱은 landscape/portrait 모두 지원. Driving view의 landscape view 삭제(resize handle 없음).
- **Deep link(App to App)**: App A에서 App B 실행 시 B가 A를 덮고, B 종료 시 A로 복귀.

### 6.4 Status bar / GNB / App library / OSD
- Status bar: SP0 Static(Door Lock, Search) · SP0 Dynamic(알림센터, 통화 중, privacy indicator) · SP1 Dynamic(Download, Update, WPC, Hi-Pass, Sound; Hotspot은 network 우측) · SP1 Static(Profile, Bluetooth, network) · Time(24h/12h) · PAB(보조석 에어백, HW 표시 없을 때만) · More menu. SP0/SP1 구분용 40dp safe bounds.
- GNB: 운전석 공조 / 앱 조작 / 조수석 공조. Fixed app = Navigation(삭제 불가, 위치 이동 가능). Editable slots(최대 3개 앱, 기본 Bluetooth Music·Phone·App Market). Recent apps 2개(최신이 왼쪽; editable slot 앱은 제외). Long-press로 편집 모드.
- App library: Connect-S 기준 한 줄 최대 7개, 2줄 넘으면 페이지 인디케이터. 상단=차량 제어(Climate shortcut, 이동만 가능), 하단=앱(삭제·이동 가능).
- **OSD**: 스티어링 휠 물리키 조작 시 표시(Connect-S: GNB 영역, Connect-W: GNB 우측), 추가 조작 없으면 **2초 후 자동 사라짐**. 미디어/사운드, 차량 제어(와이퍼, 라이트, 시트 위치), 차간 거리.

### 6.5 Foundations
- **Colors**: Scale Token(Grayscale, White/Gray Alpha, Static Light/Dark) + System Color(Background, Button, Controller, Climate, Dropdown, Dim, Driving, Fields, Gleo AI, Icon, Informative, Keyboard, Media, Overlay, Phone, Regulation, Surface, Switch, Text). Light/Dark 테마 지원. (값은 Figma 에셋 참고)
- **Typography**: 시스템 폰트 **Asta Sans**(Google Fonts, 한/영). 최소 폰트 **12sp 이상**, 가독성 위해 실제 픽셀 높이 **18px 이상**(약 18~20px) 권장. 위계: Headline / Title / Body / Label. 캐릭터 강한 폰트 지양.
- **Iconography**: 기본 **48×48dp 그리드**, Filled/Outlined 2스타일, 1벌 제작 후 사이즈 조정.
- **App Icon**: GNB·App library·App Market에 사용. Foreground/Background 레이어 분리 → **Android Adaptive Icon 형식**, 차량 브랜드별 마스킹. Figma 템플릿은 design assets에 포함.
- **Animation**: 로딩 애니메이션 Linear(정적 로딩) / Tension(긴 로딩). (zip 리소스 제공)
- **Shadow**: Level1 = Y6 Blur24 #14000000(8%) / Level2 = Y8 Blur32 #29000000(16%) + Y2 Blur2 #14000000(8%) / Level3 = Y20 Blur40 #3D000000(24%) + Y2 Blur4 #33000000(20%) (X=0, Spread=0)

### 6.6 Components (상태/속성 요약)
| 컴포넌트 | 핵심 상태·속성 |
|---|---|
| Basic button | Pressed, Disabled, Loading(로더 표시+제어 불가); Prefix/Suffix icon |
| Toggle button | Toggled(기본 False), Pressed, Disabled; Floating, Size L/M/S |
| Text button | Pressed, Disabled, Loading; Size M/S (부가 정보 링크용) |
| Icon button | Pressed, Disabled; Style Basic/Plain/Circle, Size L/M/S |
| Keypad | Pressed, Disabled; Keypad Primary/Secondary/Special, Keyboard Text/Voice |
| Bar | Window bar(창 크기 조절), Scroll bar; Variant Light/Dark, Dragging, Disabled, Pressed |
| Controllers | Continuous/Centered Slider, Stepper; MinValue/MaxValue/Step, Disabled(최소/최대 도달 시); 볼륨 아이콘 단계 Max 100%, High 76–100, Mid 26–75, Low 1–25, Min 0 |
| Indicator | Present; Size Wide/Narrow |
| Dropdown | Opened, Pressed, Disabled; Prefix(App icon/Icon), Suffix(Switch/Icon), Size M/S |
| Text Field | Value, Focused, Disabled, Read Only, Invalid; Type Text/Tel/Url/Email/Password, Size M/S |
| Number Field | 동일 상태; Size L/M |
| Page Navigation | App icon + Primary/Secondary dropdown menu + Suffix; Type Main/Depth(Back 버튼) |
| System Popup | Title, Description(opt), Confirm, Cancel(opt); Header, Body, Size L/M, Button Align Single/Vertical/Horizontal |
| Toast / Toast Popup | 앱 패널 내 side·bottom padding 24dp, Toast max width 792, message max width 712 |
| Tabs | Enable, Pressed, Selected; Type Box/Line, Fitted |
| Checkbox / Radio / Switch | Checked/Value, Pressed, Disabled, (Radio/Switch) Invalid |
| Spinner(Picker) | Date / Time 타입, drag로 선택 |
| System Notification | App icon / Profile(전화·문자) / Image 타입, Button Align |
| Widgets | Handle(끌어내려 숨김), Pagination, Title/Action, Information area |

---

## 7. 앱 개발 가이드 (필수 정책 + 구현 방법)

### 7.1 주행 규제 구현 (`CarUxRestrictions`)
- AAOS [`CarUxRestrictionsManager`](https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictionsManager)에 `registerListener()` → `onUxRestrictionsChanged()`로 `CarUxRestrictions` 수신.
- DO를 선언한 앱도 규제 활성화 시 `isRequiresDistractionOptimization()` + `getActiveRestrictions()` 비트마스크를 확인해 기능별로 직접 제한해야 함.
- 플랫폼이 **지역별 사양에 맞는 ActiveRestrictions를 내려주므로 앱은 수신한 값만 반영**하면 됨(지역 하드코딩 불필요).
```java
if (mCurrentCarUxRestrictions.isRequiresDistractionOptimization()
        && (mCurrentCarUxRestrictions.getActiveRestrictions()
            & CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO) != 0) {
    stopPlayingVideo();
}
```
- 플래그: `NO_DIALPAD`(1), `NO_FILTERING`(2), `LIMIT_STRING_LENGTH`(4), `NO_KEYBOARD`(8), `NO_VIDEO`(16), `LIMIT_CONTENT`(32), `NO_SETUP`(64) …
- 테스트: `adb shell cmd hmg_car_service --service drivingstate driving_regulation on|off` (§7.10)

💡 Kotlin 구현 스케치(문서 내용 기반으로 작성, 원문 코드 아님):
```kotlin
// build.gradle: Car API 사용 (AAOS) — useLibrary("android.car") 또는 compileOnly android.car 사용
val car = Car.createCar(context)
val uxManager = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
uxManager.registerListener { r: CarUxRestrictions ->
    val active = r.activeRestrictions
    val restricted = r.isRequiresDistractionOptimization
    videoAllowed = !(restricted && (active and CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO) != 0)
    keyboardAllowed = !(restricted && (active and CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD) != 0)
    setupAllowed = !(restricted && (active and CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP) != 0)
    // UI 갱신
}
// 종료 시 uxManager.unregisterListener(); car.disconnect()
```

### 7.2 오디오 포커스 (필수)
AAOS 오디오 포커스 기반 + **Pleos Connect Interaction Matrix v1.0 (2026-01-28)** 준수 (행=현재 보유자 컨텍스트, 열=새 요청 컨텍스트, 값 E/C/R; 매트릭스 자체는 이미지로만 제공).
- **R**(Reject: 새 요청 거부) / **E**(Exclusive: 새 요청이 선점, 기존 보유자 상실) / **C**(Concurrent: 동시 유지, 보통 duck)
- 보유자가 여럿이면 각 보유자와의 결과 중 **가장 보수적인 결과**(우선순위 R > E > C) 적용.

**개발사가 구현해야 할 규칙**
1. 모든 오디오 스트림은 재생 전 `AudioAttributes` + `AudioFocusRequest`로 포커스 요청.
2. 동시 재생(C)을 기대하는 컨텍스트(예: NAVIGATION + MUSIC)는 **`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`** 사용.
3. E가 필요한 컨텍스트(CALL, EMERGENCY 등)는 목적에 맞는 gain을 쓰되 포커스로 모든 것을 강제하지 말 것.
4. ⚠️ **포커스 변경 이벤트 처리 필수**(AAOS에선 선택이지만 Pleos 정책상 필수):
   - `AUDIOFOCUS_GAIN` → 정상 재생 / `AUDIOFOCUS_LOSS` → 즉시 중단·정지 / `AUDIOFOCUS_LOSS_TRANSIENT` → 일시정지(복귀 가능) / `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` → 볼륨 감쇄(duck)
5. Duck 대상은 `LOSS_TRANSIENT_CAN_DUCK` 수신 시 -X dB 감쇄, 다른 스트림 종료/포커스 반환 시 원복. duck 중 pause하도록 구성하면 C가 깨지니 주의.
6. 컨텍스트별: EMERGENCY current → 대부분 incoming R(긴급음 보호), incoming EMERGENCY → 대부분 E. SAFETY는 EMERGENCY 아래(EMERGENCY current면 SAFETY incoming R). **CALL 중 MUSIC 요청 = R**, CALL 중 NAVIGATION = C(옵션 플래그로 R 가능). NAVIGATION↔MUSIC = C(내비는 MAY_DUCK 사용).
7. **Gleo(VOICE_COMMAND)** 호출로 `AUDIOFOCUS_LOSS_TRANSIENT` 수신 시 → **즉시 재생 중단, 필요 시 포커스 abandon**.
8. 💡 매트릭스는 하드코딩하지 말고 `interaction_matrix.json`/csv(키: `current_context`, `incoming_context` → `E|C|R`)로 로딩 권장.
9. 로그 필수 항목: incoming context, gain type, client id, current holders, holder별 결과, 최종 결과 및 처리(grant/steal/reject), duck 적용/해제(대상, 감쇄값).

**필수 검증 시나리오**: ① MUSIC 중 NAVIGATION → C(뮤직 duck+내비) ② CALL 중 MUSIC → R ③ MUSIC 중 CALL → E ④ EMERGENCY 중 MUSIC/NOTIFICATION → R ⑤ MUSIC+NAVIGATION 동시 보유 중 VOICE_COMMAND → 보수적 결과 적용 확인.

**CarAudioContext ↔ AudioAttributes usage**
| Context | Usages |
|---|---|
| MUSIC | MUSIC, UNKNOWN, GAME, MEDIA |
| NAVIGATION | NAVIGATION, ASSISTANCE_NAVIGATION_GUIDANCE |
| VOICE_COMMAND | VOICE_COMMAND, ASSISTANT |
| CALL_RING | CALL_RING, NOTIFICATION_RINGTONE |
| CALL | CALL, VOICE_COMMUNICATION, VOICE_COMMUNICATION_SIGNALING |
| ALARM | ALARM |
| NOTIFICATION | NOTIFICATION, NOTIFICATION_* |
| SYSTEM_SOUND | SYSTEM_SOUND, ASSISTANCE_SONIFICATION, ASSISTANCE_ACCESSIBILITY |
| EMERGENCY / SAFETY / VEHICLE_STATUS / ANNOUNCEMENT | 동명 usage (시스템 컨텍스트는 Android 11+) |

### 7.3 Suspend 모드 오디오 포커스
- Suspend = IGN OFF 후 완전 종료가 아닌 저전력 유지 상태(디스플레이·대부분 UI/오디오 중단, Garage mode 등 일부 작업만). 앱은 "언제든 중단되고 다시 시작될 수 있는 상태"로 간주.
- ⚠️ 진입 시 순서: ① 재생 즉시 중단 ② **오디오 포커스 abandon** ③ 재생 상태(재생 여부·위치) 저장 ④ player/track 등 리소스 해제. 이 과정에서 새 재생/포커스 요청 금지.
- ⚠️ Resume 시 **이전 상태 자동 복원 금지**: 포커스 자동 복구 안 됨, 상태 재평가 후 필요 시 재요청. 자동 재생 여부는 OEM/사용자 정책.
- 예외(최소 범위): 전화·긴급 통신, 안전 경고음, 시스템 필수 음성 안내.
- 원문 예제 핵심 (Kotlin):
```kotlin
private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
    when (focusChange) {
        AudioManager.AUDIOFOCUS_LOSS,
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { pausePlayback(); abandonAudioFocus() }
    }
}
private val audioFocusRequest by lazy {
    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setOnAudioFocusChangeListener(focusChangeListener).build()
}
fun onSuspendEntering() {
    pausePlayback()              // 진행 중 작업 중단 (새 작업 시작 금지)
    savePlaybackState()          // 위치/콘텐츠 ID/재생 여부 저장
    abandonAudioFocus()          // audioManager.abandonAudioFocusRequest(audioFocusRequest)
    releaseTransientResources()  // 타이머, 센서/리스너, wake lock 정리
}
fun onResumeFromSuspend() {
    restorePlaybackState()
    val shouldAutoResume = false // OEM/앱 정책
    if (shouldAutoResume) startPlayback()
}
```

### 7.4 CCP(Center Control Panel: 로터리 컨트롤러·다이얼·물리 버튼) 가이드
- 앱 분류: **User-installed**(App Market 설치) / **Pre-installed**(기본 탑재) / **System**.
- User-installed: CCP 지원은 선택. 단, **미지원 앱은 필수 조작을 터치만으로 완결** 가능해야 함.
- Pre-installed: CCP 지원 필수(정적 콘텐츠 앱, WebView 등 기술 제약, 게임 등은 계약서 명시 시 예외). 브라우저: 회전→스크롤, 클릭→선택, 좌/우→포커스 이동(UI 영역 한정).
- CCP 미지원 선언:
```xml
<application ...>
    <meta-data android:name="com.hmg.rotary.ROTARY_SUPPORTED" android:value="false" />
</application>
```
- ⚠️ **CCP Back 하드키 (필수, 모든 앱)**: 앱 최상단 화면(더 이상 뒤로 갈 곳 없음)에서 Back 입력 시 **앱을 종료하지 말고 `moveTaskToBack(true)`로 숨김**. `finish()`/`finishAffinity()` 금지. 하위 화면/팝업에선 일반 Back 동작. 숨김 후에도 미디어 재생 등 백그라운드 기능은 유지. 프로세스가 죽을 수 있으니 상태 저장·복원. 미구현 시 **심사 반려 대상**. (CCP Back이 표준 Back 이벤트로 오는지 확인 후 `OnBackInvokedDispatcher` 등 사용)
💡 구현 예 (문서 내용 기반 작성):
```kotlin
onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
        if (navController.previousBackStackEntry != null) navController.popBackStack()
        else moveTaskToBack(true)   // 최상단: 종료 대신 숨김
    }
})
```
- CCP 심사 항목: 모든 주요 UI 포커스 이동, 회전 시 스크롤, 클릭 선택, 역방향(뒤로/취소), 터치 전용 영역 없음, 운전 중 차단 요소 준수. "Pleos Connect CCP UX Guideline" 문서 다운로드 제공.

### 7.5 Maps/Navigation 앱
- 원칙: 운전자 안전 최우선, **단일 경로·단일 내비게이션**, 주행 중 조작 최소화, 카테고리 기반 기능 제한, Pleos/AAOS 공식 API + UX 가이드 준수.
- **카테고리 선언 (매니페스트, 내비컨테이너 사용에 필요)**:
```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.APP_MAPS" />
</intent-filter>
```
  App Market 등록 시에도 동일 카테고리. 선언 카테고리와 실제 기능 불일치 → **즉시 반려**, 심사 중 기능 변경 시 재심사.
- **단일 경로 정책**: Active Route(음성·TBT 허용) / Inactive Route(지도 탐색, 안내 금지) / Background(모든 안내 금지). Phone Projection과도 배타. `CarAppFocusManager.requestAppFocus(...)` + `OnAppFocusChangedListener`로 경로 소유권 관리. 경로 전환은 사용자 명시 선택 또는 기존 내비 오류/종료 시에만, 사용자 인지 안내 필수, 동의 없는 자동 전환 금지.
- 주행 중 금지: 3 depth 초과 메뉴, 긴 리스트 스크롤, 영상·애니메이션·광고. 허용: 음성 입력, 단일 터치 주요 기능, 즐겨찾기 목적지 선택.
- UX 심사: 터치 영역 최소 크기, 주행 핵심 정보 상시 노출, **주행 중 전체 화면 팝업 금지**.
- 권한: 위치 필수, 마이크는 음성 입력 시에만, 불필요 권한 → 반려. 비공식 센서/API 금지.
- 성능: 실행/전환 지연 최소화, 장시간 안정성, Crash/freeze → 반려. 보안: 위치 데이터 암호화, HTTPS, 개인정보 처리방침 제공, 불필요 위치 저장 금지.
- **즉시 반려**: 주행 중 광고, 영상·게임 요소, 복수 내비 동시 경로 안내, 카테고리 위반, OEM/AAOS 정책 위반.

### 7.6 Music/Audio 앱
- 범주: 음악 스트리밍/재생, 팟캐스트, 오디오북, 라디오 (영상 주 기능 앱 제외). 오디오 재생이 주 기능이어야 하며, 단순 보조 기능으로 음악을 제공하면 즉시 반려.
- 하위 카테고리(음악/팟캐스트/오디오북/라디오) 정의 필수, 콘텐츠 유형별 브라우징 구조, 모든 콘텐츠 단일 리스트 혼합 노출 제한.
- 매니페스트:
```xml
<application android:appCategory="audio" ...>
    <meta-data android:name="androidx.car.app.launchable" android:value="true" />
</application>
```
- ⚠️ **`MediaBrowserService`(MBS) 구현 필수** + `MediaSession`으로 재생 상태/제어 제공. MBS 미구현 오디오 앱은 배포 제외 가능.
- 💡 **미디어 센터**(MBS 기반 개발 템플릿) 사용 권장: 스티어링 휠 키 기본 제공, 미디어 앱 라이브러리 자동 등록, Tune Knob(이전/다음), Media 버튼 연동 지원(Mode 버튼은 둘 다 미지원). 미사용 시 스티어링 휠 키를 직접 구현해야 하며 미구현 시 반려 가능, 라이브러리 미노출.
- 미디어 센터 미사용 시: `MediaSessionCompat` 등록 + `MediaSessionCompat.Callback` 구현, 최소 **재생/일시정지/정지/미디어 ID 기반 재생/검색 기반 재생**, 가능하면 이전/다음 트랙.
- ⚠️ **자동 재생 금지**: 앱 연결 시, 프로필 변경 시에도 자동 재생 금지. 사용자 명시 동작 또는 시스템 재개 요청 시에만 재생.
- ⚠️ 서비스 생명주기 규칙:
  - 게임 앱 실행 등으로 `AUDIOFOCUS_LOSS` 수신 시 `MediaSession`이 active로 남지 않게 stop 또는 session 비활성화.
  - bind/unbind만 하면 `onTaskRemoved()`를 못 받을 수 있으므로 **`onCreate()`에서 `startService()`** 로 Started 상태 유지.
  - `onStartCommand()` 및 재생 시점에 **`startForeground()`**.
  - `onTaskRemoved()`: **`stopMedia()` → `stopSelf()` → `super.onTaskRemoved()`** 순서.
  - 3-finger 제스처 시 시스템이 `MediaSessionCompat.Callback.onStop()` 호출 가능 → **`ExoPlayer.stop()`** 수행.
```kotlin
private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focus ->
    when (focus) {
        AudioManager.AUDIOFOCUS_LOSS -> {
            stopPlayback()
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> pausePlayback()
        AudioManager.AUDIOFOCUS_GAIN -> resumePlayback()
    }
}
```
- **Gleo AI 연동**: “멜론 앱에서 아이유 노래 틀어줘” 같은 곡 검색·재생 + 핵심 기능 3~5개 음성 기능 제공 가능. 방법 ① Gleo Interface 구현(§9) ② Android 표준 미디어 API(Media3) — MediaAgent가 Media3로 제어(스키마 없이 지원하는 업데이트 준비 중). 최소 조건: `MediaSession`/`MediaLibraryService` 연동, 재생/일시정지/다음/이전, 검색 명령. **곡 검색 음성 기능은 Media3 `MediaLibraryService`의 검색 기능 구현 필수**(`MediaSession`만으론 기본 제어만). 기존 MBS 호환되나 Media3 권장.
- 위반 조치: 카테고리 불일치 → 수정 요청/반려, 하위 카테고리 미정의 → 보완 요청, 반복 위반 → 노출 제한/등록 중단.

### 7.7 TTS/음성 안내 선행 묵음 (pre-roll silence)
- 모든 음성 안내(TTS, 녹음 멘트, 내비/알림/상태 안내) 재생 전 **150~200 ms 선행 묵음** 권장 (최소 150 ms; 150 미만은 첫 음절 손실, 200 초과는 체감 지연).
- 적용 위치 우선순위: ① PCM/WAV 앞단 silence padding ② TTS 결과 후처리 ③ 플레이어 재생 지연(비권장).
- 연속 발화엔 추가 묵음 X, 스트림이 끊기거나 새 세션이면 다시 적용. 출력 경로 초기화·포커스 확보·플레이어 준비는 별도로 확인(묵음이 대체하지 않음).

### 7.8 IME 표시 시 window resize
- EditText/TextField가 **있는** window: IME 표시 시 view가 위로 밀려 올라가는 것이 사양.
- EditText/TextField가 **없는** window(예: Dialog): IME에 의해 앵커링되면 안 됨 → insets 대상 제한:
```kotlin
// System Dialog / overlay
val params = WindowManager.LayoutParams(...)
params.setFitInsetsTypes(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
windowManager.addView(overlay, params)

// App Dialog
val dialog = Dialog(this)
dialog.window?.let { win ->
    val p = win.attributes
    p.setFitInsetsTypes(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
    win.attributes = p
}
dialog.setContentView(R.layout.xxx); dialog.show()
```
- 기타 SystemBars insets 처리 필요 시 `View.OnApplyWindowInsetsListener`. Activity는 Google "Handle input method visibility" 가이드 기준 검토.

### 7.9 FAQ — 실행 환경 & 플랫폼 리소스
- 플랫폼 식별: `android.os.Build.DEVICE` (`ro.product.device`) → `CONNECT_S` / `CONNECT_L` / `CONNECT_W` (**실차에서만 유효**, 에뮬레이터에선 구분 불가)
- 지역 코드: 시스템 프로퍼티 `product.sw.target_region` → `KOR` / `EUR` / `USA`, `CAN` — 지역별 UX 정책 적용에 사용
- 내비 탑재 여부 확인 방법: 비공개 (partnership@pleos.ai)
- 클러스터와 같은 속도: AAOS `CarPropertyManager`로 **`PERF_VEHICLE_SPEED`** 조회(GPS 계산 속도와 다를 수 있음)
- 배터리 충전 상태: Vehicle SDK `EvBattery.getEvBatteryStateOfCharge`
- 코덱: 실차와 에뮬레이터가 다름(에뮬레이터 결과를 실차에 그대로 적용 금지)
  - Connect-S/L 오디오: FLAC, AC3+, ALAC, APE, PCM, TTA, AIFF, WMA, OGG, DSD, DFF/DSF, DTS, WAV, MP2, MP3, BSAC, ADPCM, HE-AAC(+), AAC, EAC3(+JOC), OPUS, AMR 등 / 비디오: MPEG-1/2, VC-1, WMV, H.263, MP42/43, MPEG4 Part2, Xvid, VP6/7/8/9, H.264, HEVC(main10), x265, AV1 등
  - Connect-W 오디오: PCM, AAC, MPEG, AMR, WMA, Opus, Vorbis, FLAC, ALAC, APE, Dolby, DTS, ADPCM, AIFF, TTA, BSAC / 비디오: MPEG-1/2, MPEG-4 Part2, H.263, H.264, H.265, AV1, VP8, VP9
  - Emulator: c2.android.* 소프트웨어 코덱(디코더 AMR-NB/WB, FLAC, G.711 A/μ-law, AAC, MP3, Opus, Raw PCM, Vorbis, H.263, AV1, AVC, HEVC, MPEG-4, VP8, VP9 / 인코더 AMR-NB/WB, FLAC, AAC, Opus, H.263, AV1, AVC, HEVC, MPEG-4, VP8, VP9)

### 7.10 FAQ — ADB 명령 모음
```bash
adb devices                                             # 연결 확인 (emulator-5554 device)
adb exec-out screencap -p > ~/Desktop/screen_$(date +"%Y%m%d_%H%M%S").png   # 스크린샷
adb exec-out screenrecord --output-format=h264 - > ~/Desktop/screen.h264    # 녹화(Ctrl+C 종료)
ffmpeg -framerate 60 -i ~/Desktop/screen.h264 -c copy ~/Desktop/screen.mp4  # mp4 변환
adb install -d -r app-my-signed.apk                     # 설치 (-d 다운그레이드 허용, -r 재설치)
adb push app.apk /data/local/tmp/ && adb shell pm install -d -r /data/local/tmp/app.apk  # 대안
adb shell pm list packages | grep <일부명>
adb shell monkey -p <패키지> -c android.intent.category.LAUNCHER 1   # GNB에서 실행하는 것과 유사
adb shell am start -n <패키지>/<.Activity>
adb shell am force-stop <패키지>
adb uninstall <패키지> ; adb shell pm clear <패키지>
adb shell input text "test"                             # 텍스트 입력
adb shell cmd hmg_car_service --service drivingstate driving_regulation on    # 주행 규제 ON (전 차종 공통)
adb shell cmd hmg_car_service --service drivingstate driving_regulation off   # 주행 규제 OFF
adb forward tcp:8088 tcp:8088                           # Virtual Console
```
- CCS 연결(블루링크/제네시스 커넥티드, **CCS 실차 전용**, Devbox·Emulator 미지원): `adb shell settings put secure hmg_is_ccs_account_linked 1`, `adb shell settings put global hmg_provisioning_status 4`
- 유틸리티 모드(차속 0 / 주차브레이크 On / 기어 P 후): `/dev/vcs_simulator_rx`에 바이트 시퀀스 echo (원문 참조)
- 기어 변경은 Virtual Console에서.

---

## 8. SDK 사용 가이드

### 8.0 공통 사항
- Maven 저장소(모든 SDK 공통): `https://nexus-playground.pleos.ai/repository/maven-releases/`
- 좌표(문서 기준 최신 버전):

| SDK | Gradle 좌표 | 문서 기준 버전 | 퍼미션 | 진입점 |
|---|---|---|---|---|
| Vehicle | `ai.pleos.playground:Vehicle:2.3.2` (Getting Started 예시는 2.3.1) | v2.3.2 / Vehicle Service v2.3.21 | `pleos.car.permission.CAR_*`, `CONTROL_CAR_*` | `ai.pleos.playground.vehicle.Vehicle` |
| NaviHelper | `ai.pleos.playground:NaviHelper:3.0.5` | v3.0.5 | `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `NAVI_CUSTOM_MAP` | `ai.pleos.playground.navi.helper.NaviHelper` |
| TextToSpeech | `ai.pleos.playground:TextToSpeech:3.3.1` | v3.3.1 | `TTS_SERVICE` | `ai.pleos.playground.tts.TextToSpeech` |
| SpeechToText | `ai.pleos.playground:SpeechToText:3.3.1` | v3.3.1 | `STT_SERVICE` | `ai.pleos.playground.stt.SpeechToText` |
| FusedLocation | `ai.pleos.playground:FusedLocation:1.1.3` | v1.1.3 | `FUSED_LOCATION` (**dangerous** → 런타임 권한 요청 필요) | `ai.pleos.playground.location.fused.FusedLocation` |

- 모든 SDK의 공통 생명주기: **객체 생성 → `initialize()` → 리스너 등록 → API 호출 → 콜백으로 결과 처리 → 리스너 해제 → `release()`**. `release()` 후 재사용하려면 새 객체 생성 + 재초기화.
- ⚠️ **퍼미션 정책**: 퍼미션은 **Pleos App Market과 사전 협의·승인된 범위 내에서만** 사용 가능. 승인되지 않은 퍼미션을 선언하거나 해당 API를 호출하면 **앱 심사 불통과**. 필요한 퍼미션만 선언.
- Java에서 Kotlin 람다 콜백 사용 시 `return Unit.INSTANCE;` 필요.

### 8.1 Vehicle SDK
**구조**: 3rd party 앱 → Vehicle SDK → (IPC) → Vehicle Service(AAOS 위) → 차량. 진입점 `Vehicle(context)` → `initialize()` → `getXxx()`로 도메인 객체(직접 생성 불가) → 4가지 API 타입 호출 → `release()`.

| API 타입 | 메서드 패턴 | 결과 전달 |
|---|---|---|
| Capability | `checkXxxCapabilities(onComplete, onFailed)` | `onComplete(config: CapabilityConfig[, areaMap: Map<Area, AreaBounds>])` — 지원 여부, `AccessType`(`NONE`/`READ`/`WRITE`/`READ_WRITE`), 영역별 `minValue`/`maxValue` |
| Get | `getCurrentXxx([area,] onComplete, onFailed)` | `onComplete(값?)` (nullable) |
| Set | `setXxx([area,] value, onComplete = {...}, onFailed = {})` | 실패 시 `onFailed` (일부 API는 onComplete도 제공) |
| Callback | `registerXxx(listener)` / `unregisterXxx(listener)` | 리스너 `onXxxUpdated(...)` / `onFailed(e)` |

⚠️ IPC이므로 모든 결과는 비동기. **콜백이 끝나기 전에 `release()` 호출 금지.** 도메인/기능 지원 여부는 차량마다 다르므로 **`checkXxxCapabilities`로 먼저 확인**하고 UI를 비활성화/대체 흐름 처리.

**도메인 목록 (Vehicle 클래스의 getter: `getBrake/getCarInfo/getDisplay/getDoor/getDrivingMode/getEvBattery/getFuel/getHVAC/getLight/getOdometer/getSafety/getSeat/getSideMirror/getSteeringWheel/getTire/getToll/getTurnSignal/getWindow/getWiper`)**

| 도메인 | 퍼미션 | API 수 | 주요 기능 (get/check/register 대상) / set 가능 항목 |
|---|---|---|---|
| Brake | CAR_DRIVE | 4 | BrakePedalStatus |
| CarInfo | CAR_INFO | 4 | CRNInfo, VINInfo |
| Display | CAR_INFO | 4 | TemperatureDisplayUnit |
| Door | CAR_DOORS, CONTROL_CAR_DOORS | 15 | DoorLock(State), DoorPosition, GloveBoxLock(State) / set: DoorLock, DoorPosition, GloveBoxLock |
| DrivingMode | CAR_DRIVE | 12 | CruiseControlState, CruiseControlTargetSpeed, DrivingModeState |
| EvBattery | CAR_ENERGY, CONTROL_CAR_ENERGY | 30 | EvBatteryStateOfCharge, Cumulative/Current/Instantaneous/SinceCharge EvEfficiency, EvChargeCurrentDrawLimit, EvChargePercentLimit, EvChargePortOpen, EvChargeSwitch(State) / set: ChargeCurrentDrawLimit, ChargePercentLimit, ChargePortOpen, ChargeSwitch |
| Fuel | CAR_ENERGY | 20 | FuelPercent, Cumulative/Current/Instantaneous/SinceCharge FuelEconomy |
| HVAC | CAR_CLIMATE, CONTROL_CAR_CLIMATE | 67 | Power, Ac, MaxAc, Auto, Dual, Defroster, MaxDefrost, Recirculation, AutoRecirculation, AirCleaner, FanDirection, FanSpeed, Temperature, TemperatureCurrent (대부분 set 가능) |
| Light | CAR_LIGHTS, CONTROL_CAR_LIGHTS | 71 | Ambient(Switch/State/Brightness/BrightnessColor/ColorAutoByDriveMode), Cabin, Reading, LowBeam, HighBeam, FrontFog, RearFog, TailLamp (State 조회 + Switch set) |
| Odometer | CAR_DRIVE, CONTROL_CAR_DRIVE | 35 | DistanceDriven, RangeRemainingDistance, Trip(Current/Cumulative/Instantaneous/SinceRefill) Distance·Time / set: RangeRemainingDistance |
| Safety | CAR_SAFETY, PRIVATE_CAR_SAFETY | 37 | Airbag(Deployed), SeatAirbag, SeatBelt 경고, BrakeFluid/EngineOil/TransmissionOil Level, LowWasherFluid, PowerDownWarning, Telltales(⚠️ signature|privileged — 3rd party 불가) |
| Seat | CAR_SEATS, CONTROL_CAR_SEATS | 97 | 위치(ForeAft, Height, Tilt, Depth, BackrestAngle, Headrest*, Lumbar*, BeltHeight) 조회 + `moveSeatXxx` 제어, SeatMemory, SeatOccupancy, SeatBeltBuckled, 시트 열선/통풍(HvacSeatTemperature/Ventilation set) |
| SideMirror | CAR_MIRRORS, CONTROL_CAR_MIRRORS | 5 | HvacSideMirrorHeat(State) / set |
| SteeringWheel | CAR_DRIVE, CAR_CLIMATE, CONTROL_CAR_CLIMATE | 9 | SteeringWheelAngle, HvacSteeringWheelHeat(State) / set: 핸들 열선 |
| Tire | CAR_SAFETY | 3 | LowTirePressure, WheelArea |
| Toll | CAR_TOLL, CONTROL_CAR_TOLL | 5 | EtcsVoiceGuidanceVolume(하이패스 음성 음량) / set |
| TurnSignal | CAR_LIGHTS | 4 | TurnSignalState |
| Window | CAR_WINDOWS, CONTROL_CAR_WINDOWS | 29 | WindowPosition, WindowLock(State), WindowBlind(move), SmartGlass(Config/Level/State/ZoneState) — `setSmartGlassState`는 signature|privileged |
| Wiper | CAR_WINDOWS, CONTROL_CAR_WINDOWS | 7 | WiperState, WiperSwitch / set |

> 전체 시그니처·영역(Area) 타입·상수(enum)·리스너는 `PLEOS_CONNECT_API_REFERENCE.md` 참조. 패키지: `ai.pleos.playground.vehicle`(Vehicle), `.api`(도메인 인터페이스), `.api.model.area`(DoorArea, SeatArea 등 영역 모델), `.api.model.capability`(CapabilityConfig, AccessType, AreaBounds), `.api.model.common`, `.api.model.telltale`, `.callback.exception`(VehicleException), `.constant`(ErrorCode, PleosPermission), `.constant.area`(DoorValue 등), `.constant.control`(DrivingModeType, EvChargeState, FanDirectionType, LightState, LightSwitch 등 enum), `.constant.telltale`, `.constant.unit`(TemperatureUnit 등), `.listener`(XxxListener).
> 참고: API Reference의 Vehicle 클래스 예제에 `vehicle.getGear()`/`GearSelectionListener`가 나오지만 v2.3.2 Vehicle 클래스 getter 목록엔 없음 → 기어/속도 같은 **표준 데이터는 AAOS `CarPropertyManager`**(예: `GEAR_SELECTION`, `PERF_VEHICLE_SPEED`) 사용.

**퍼미션** (`PleosPermission` 상수 → 매니페스트 `pleos.car.permission.{상수명}`)
- 접두사: `CAR_*` = 조회(get/capability/register), `CONTROL_CAR_*` = 제어(set/move), `PRIVATE_CAR_*` = 시스템 전용.
- 그룹: CLIMATE, DOORS, DRIVE, ENERGY, INFO, LIGHTS, MIRRORS, SAFETY, SEATS, TOLL, WINDOWS (각각 CAR_/CONTROL_CAR_/PRIVATE_CAR_ 변형)
- 대표: Door→CAR_DOORS · Window/Wiper→CAR_WINDOWS · HVAC→CAR_CLIMATE · DrivingMode/Brake/Odometer→CAR_DRIVE · EvBattery/Fuel→CAR_ENERGY · CarInfo/Display→CAR_INFO · Light/TurnSignal→CAR_LIGHTS · SideMirror→CAR_MIRRORS · Safety/Tire→CAR_SAFETY · Seat→CAR_SEATS · Toll→CAR_TOLL
- 대부분 protection level `normal`. 단, **Telltales 관련(`check/get/register/unregisterTelltales`)과 `Window.setSmartGlassState`는 `signature|privileged`** → 일반 3rd party 앱은 사용 불가로 간주.
- 세부 API별 요구 퍼미션이 다를 수 있으니 각 함수의 `@RequiresPermission` 확인. 누락 시 `onFailed`에 `VehicleException(ERROR_SECURITY_PERMISSION)`.
```xml
<uses-permission android:name="pleos.car.permission.CAR_DOORS" />
<uses-permission android:name="pleos.car.permission.CAR_INFO" />
<!-- 사용하는 도메인에 맞는 퍼미션만 선언 -->
```

**코드 패턴 (원문 예제)**
```kotlin
// Capability
door.checkDoorPositionCapabilities(
  onComplete = { config, areaMap ->
    when (config.accessType) {
      AccessType.READ, AccessType.READ_WRITE -> {
        val bounds = areaMap[DoorArea(DoorValue.ROW_1_LEFT)]
        // bounds?.minValue / bounds?.maxValue 사용 후 getCurrentDoorPosition 호출
      }
      else -> { /* NONE 또는 WRITE only — UI 비활성/대체 흐름 */ }
    }
  },
  onFailed = { e -> if (e is VehicleException) { /* e.errorCode */ } }
)

// Get
display.getCurrentTemperatureDisplayUnit(
  onComplete = { unit: TemperatureUnit? -> },
  onFailed = { e: Exception -> }
)

// Set  (값 범위는 차량마다 다름 → checkXxxCapabilities의 AreaBounds로 확인. 예: 도어 최대 개방값은 차량 의존, min은 보통 0=닫힘)
door.setDoorPosition(area = DoorArea(DoorValue.ROW_1_LEFT), value = 1, onFailed = { e -> })

// Callback
val listener = object : DoorPositionListener {
  override fun onPositionUpdated(pos: Int?) { }
  override fun onFailed(e: Exception) { }
}
door.registerDoorPosition(doorPositionListener = listener)
door.unregisterDoorPosition(doorPositionListener = listener)
```

**ErrorCode** (`VehicleException.errorCode`, 패키지 `ai.pleos.playground.vehicle.constant`; 모든 onFailed 예외가 VehicleException인 것은 아님)
| ErrorCode | 의미 | 권장 처리 |
|---|---|---|
| ERROR_NOT_SUPPORTED_PROPERTY | 속성(기능) 미지원 | UI 비활성·대체 흐름 |
| ERROR_NOT_SUPPORTED_AREA | 영역 미지원 | 영역 선택 변경 |
| ERROR_NOT_SUPPORTED_VALUE_TYPE | 값 타입 미지원 | 파라미터·타입 재확인 |
| ERROR_SECURITY_PERMISSION | 퍼미션 부족 | 매니페스트/@RequiresPermission 확인 |
| ERROR_SERVICE_CONNECTION_FAILURE | Vehicle 서비스 연결 실패 | initialize()·에뮬레이터(cold boot) 확인 |
| ERROR_INTERNAL_SERVICE | 내부 서비스 오류 | 재시도·일시 오류 안내 |
| ERROR_DRIVING_RESTRICTED | 주행 중 기능 제한 | 주행 제한 안내 |
| ERROR_EMPTY_RESULT | 결과 없음 | 빈 상태 UI |
| ERROR_UNDEFINED | 정의되지 않은 오류 | 로그·재시도 |
```kotlin
onFailed = { e ->
  if (e is VehicleException) when (e.errorCode) {
    ErrorCode.ERROR_NOT_SUPPORTED_PROPERTY, ErrorCode.ERROR_NOT_SUPPORTED_AREA -> { /* UI 비활성 */ }
    ErrorCode.ERROR_SECURITY_PERMISSION -> { /* 매니페스트 퍼미션 확인 */ }
    ErrorCode.ERROR_SERVICE_CONNECTION_FAILURE, ErrorCode.ERROR_INTERNAL_SERVICE -> { /* initialize·재시도 */ }
    else -> Log.e(TAG, "Vehicle API 오류: ${e.errorCode} — ${e.message}")
  } else Log.e(TAG, "Vehicle API 오류: ${e.message}")
}
```

### 8.2 NaviHelper SDK
- 3rd party 앱 → NaviHelper SDK → **Pleos Connect Navi Service(순정 Navigation 앱)**에 목적지/경유지/경로 변경/정보 조회/Navigation 화면 표시 요청 → 결과와 주행 상태 변화를 이벤트로 수신. ⚠️ 내비 탑재 차량에서만 동작. 실제 이벤트는 Navigation 앱 구현/상태에 따라 다를 수 있음.
- ⚠️ **조회 API(`get*Info()`)는 값을 반환하지 않음** → 대응 콜백(`onXxxInfo`)에서 결과 처리. `requestRoute()` 성공 호출 ≠ 탐색 완료 → `onRouteStarted`/`onRoutingCompleted`/`onError`로 확인.

| 퍼미션 | API |
|---|---|
| `pleos.car.permission.NAVI_ROUTE` | `getBookmarkInfo/getChargerOperatorInfo/getCurrentLocationInfo/getDestinationInfo/getRecentDestinationInfo/getRouteStateInfo/getTBTInfo/getWaypointInfo`, `requestTrip(TripRequest)` |
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | `requestRoute(RouteInfo)`, `requestRouteWithWaypoints(RouteWithWaypointsRequest)`, `cancelRoute()`, `requestReRoute()`, `addWaypoint(RequestWaypointInfo)`, `removeWaypoint(WaypointIndex)`, `changeRouteOption(RouteOption)` |
| `pleos.car.permission.NAVI_CUSTOM_MAP` | `requestNavigationScreen(NavigationScreenRequest)` |

```kotlin
import ai.pleos.playground.navi.helper.NaviHelper
val naviHelper = NaviHelper(applicationContext)
naviHelper.initialize(
    onConnected = { naviHelper.addListener(naviEventListener) /* 이후 API 호출 */ },
    onFailed = { throwable -> /* 초기 연결 재시도 모두 실패 */ }
)   // 인자 없는 initialize()도 있으나 콜백 버전 권장

private val naviEventListener = object : NaviHelperEventListener {   // 필요한 콜백만 override
    override fun onRouteStarted(routeStartInfo: RouteStartInfo) {}
    override fun onRoutingCompleted() {}
    override fun onDestinationInfo(destinationInfo: DestinationInfo) {}
    override fun onError(errorCode: NaviErrorCode) {}
}

// 목적지 설정 (Kotlin은 named argument 권장 — 원문 Java 예제의 인자 순서가 data class 선언과 다름)
naviHelper.requestRoute(RouteInfo(
    latitude = 37.48544722, longitude = 127.03636666,
    displayLatitude = null, displayLongitude = null,
    poiName = "42dot", poiId = "36khp37hh8gqcm5nqhe5", poiSubId = "0",
    address = "서울시 강남구 남부순환로 2621"
    // routeOption = RouteOption.RECOMMENDED (기본값)
))
naviHelper.getDestinationInfo()          // → onDestinationInfo()
naviHelper.changeRouteOption(RouteOption.HIGHWAY)   // → onRouteOptionChanged()
naviHelper.requestReRoute()
naviHelper.addWaypoint(RequestWaypointInfo(
    latitude = 37.41275833, longitude = 127.09481944, waypointIndex = WaypointIndex.FIRST,
    poiId = "36khp2hdj2cfcm5nqhe3", poiName = "소프트웨어드림센터", poiSubId = "0",
    address = "경기도 성남시 수정구 창업로40번길 6"))
naviHelper.getWaypointInfo()             // → onWayPointInfo()
naviHelper.removeWaypoint(WaypointIndex.FIRST)      // ALL = 전체 제거
naviHelper.requestRouteWithWaypoints(RouteWithWaypointsRequest(destination = routeInfo, waypoints = listOf(wp1)))
naviHelper.cancelRoute()

override fun onDestroy() { naviHelper.removeListener(naviEventListener); naviHelper.release(); super.onDestroy() }
```
- 데이터 클래스 시그니처(API Reference 기준):
  - `RouteInfo(latitude: Double, longitude: Double, displayLatitude: Double? = null, displayLongitude: Double? = null, poiName: String, poiId: String, address: String, poiSubId: String, routeOption: RouteOption = RECOMMENDED, poiProvider: String = "", isMobileRequest: Boolean = false)`
  - `RequestWaypointInfo(latitude, longitude, poiName: String?, waypointIndex: WaypointIndex, poiId, address, poiSubId, poiProvider = "")`
  - `TripRequest(latitude, longitude, poiId, poiName, poiSubId: String?)`
- `RouteOption`: `RECOMMENDED`(추천), `SHORT_DISTANCE`, `SHORT_TIME`, `FREE`(무료도로), `HIGHWAY`, `COMFORTABLE`, `SCHOOL_ZONE`. 옵션 변경/재탐색은 활성 경로가 있을 때.
- 경유지 최대 **2개** (`WaypointIndex.FIRST`, `SECOND`, 전체 제거 `ALL`).
- `NavigationScreenRequest`(sealed): `Foreground`, `SearchPlace(keyword)`, `POIDetailPopup(poiId, poiProvider)`, `OverviewRoute(info)`, `EditRoute`, `RouteOptionList`, `RecentDestinations`, `FavoriteMain`, `AddHomeFavorite`, `AddWorkFavorite`, `NearbyChargingStations`, `MoveToCurrentLocationOnMap`, `Settings`.
- 이벤트(`NaviHelperEventListener`): 경로 `onRouteStarted, onRoutingCompleted, onRouteCancelled, onReRouteEnded, onRouteOptionChanged` / 목적지·경유지 `onDestinationChanged, onDestinationArrived, onWaypointChanged, onWayPointArrived` / 정보 `onBookmarkInfo, onBookMarkChanged, onRecentDestinationInfo, onCurrentLocationInfo, onDestinationInfo, onWayPointInfo, onRouteStateInfo, onTBTInfo, onChargerOperatorInfo, onTripInfo, onNavigationStatus, onDrivingInfo, onCurrentRoadSpeedLimit, onCameraAlert, onCautionPointInfo` / `onError(NaviErrorCode)`.
- `NaviErrorCode`: `CAUSE_LOW_BATTERY`, `CAUSE_SEARCH_ROUTE_OVER_DISTANCE`, `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE`, `CAUSE_SEARCH_ROUTE`(서버 오류), `CAUSE_SEARCH_TRIP`. 오류 시 확인: 연결 완료 후 호출했는지, 퍼미션, 좌표/POI ID/경유지 인덱스, 현재 내비 상태.

### 8.3 TextToSpeech SDK
- 3rd party 앱 → TTS SDK → Pleos Connect **Speech Service** → 차량 오디오로 재생.
- `Mode`: **`HYBRID`(기본)** = 서버 streaming 우선, 네트워크 실패 시 On-device 자동 전환(서버 사용하려면 앱 등록 필요) / `SERVER` = 서버만(네트워크+앱 등록 필요, 실패 시 오류 이벤트) / `ON_DEVICE` = 차량 엔진만(앱 등록 불필요).
- 퍼미션: `<uses-permission android:name="pleos.car.permission.TTS_SERVICE" />` (초기화·리스너·등록·합성 모두 필요, 없으면 `PERMISSION_DENIED`)
```kotlin
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.OnInitializeListener

val tts = TextToSpeech(applicationContext, Mode.HYBRID)
tts.initialize(object : OnInitializeListener {
    override fun onSuccess() { /* 사용 가능 */ }
    override fun onFailure(errorType: ErrorType) { }
})
private val eventListener = object : EventListener {
    override fun onReady() = Unit          // speak 처리 준비
    override fun onStart() = Unit          // 재생 시작
    override fun onUpdatedRms(rms: Double) = Unit
    override fun onDone() = Unit           // 정상 종료
    override fun onStop() = Unit           // stop()으로 중지됨
    override fun onError(errorType: ErrorType) { }
}
tts.addEventListener(eventListener)
tts.setLocale(Locale.KOREA)                // speak 전에. 지원: ko-KR, en-US, fr-CA, es-US, es-MX
tts.registerApp(clientID = clientId, clientSecret = clientSecret,   // HYBRID/SERVER에서 서버 사용 시
    listener = object : OnServerConnectionListener {
        override fun onConnected() { tts.speak("안녕하세요. 무엇을 도와드릴까요?") }  // onConnected 이후 speak
        override fun onFailed(errorType: ErrorType) { }
    })
tts.speak("안녕하세요. ", isFinal = false)    // 여러 조각을 하나의 발화로: 마지막만 isFinal=true
tts.speak("TextToSpeech SDK입니다.", isFinal = true)
tts.stop()                                 // → onStop()
// onDestroy: tts.removeEventListener(eventListener); tts.release()
```
- 기타 API: `setAudioAttributes(AudioAttributes)`, `setAudioZoneId(zoneId)`, `setSpeakerType(type: String)`(음성/화자 변경), `version`.
- setLocale 미호출 시: 같은 앱에서 이전에 설정한 locale → 없으면 시스템 locale.
- 💡 Client ID/Secret은 Playground 프로젝트 정보 > SDK에서 발급. 앱에 하드코딩하지 말고 `BuildConfig`/`local.properties` 등으로 주입 권장(보안 가이드 §11.4).

### 8.4 SpeechToText SDK
- 사용자 발화 → 텍스트 반환만 함(⚠️ "음악 틀어줘" 같은 **명령 실행은 하지 않음** → 음성 명령은 Gleo Interface §9).
- Mode/앱 등록/locale 규칙은 TTS와 동일(`ai.pleos.playground.stt.constant.Mode`). 퍼미션 `pleos.car.permission.STT_SERVICE`. 에뮬레이터에서 마이크 입력 허용 필요(§4.3).
```kotlin
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.Mode
val stt = SpeechToText(applicationContext, Mode.HYBRID)
stt.initialize(object : OnInitializeListener {
    override fun onSuccess() {}
    override fun onFailure(errorType: ErrorType) {}
})
private val resultListener = object : ResultListener {
    override fun onReady() = Unit
    override fun onStartedRecognition() = Unit
    override fun onUpdated(stt: String, completed: Boolean) { /* completed=true면 최종 결과 */ }
    override fun onUpdatedEpdData(on: Long, off: Long) = Unit   // EPD 시작/종료 위치(입력 버퍼 byte offset)
    override fun onUpdatedRms(rms: Double) = Unit                // dB 아님
    override fun onEndedRecognition() = Unit
    override fun onError(errorType: ErrorType) {}
}
stt.addListener(resultListener)            // ⚠️ request()/sendAudio() 전에 등록
stt.setLocale(Locale.KOREA)
stt.registerApp(clientID = clientId, clientSecret = clientSecret, listener = /* OnServerConnectionListener */)
stt.request()                              // 차량 마이크 활성화 + 인식 시작
stt.stop()                                 // 마이크 종료 → onEndedRecognition()
// onDestroy: stt.removeListener(resultListener); stt.release()
```
- **외부 PCM 전송(`sendAudio`)은 `ON_DEVICE` 모드에서만**(다른 모드에선 무시되고 onError도 안 옴). 마지막에 반드시 `completeAudioSend()`. 16 kHz/16-bit/mono 실시간 전송 예: 3,200 byte 청크를 100 ms 간격.
```kotlin
pcmData.asList().chunked(3_200).forEach { chunk -> stt.sendAudio(chunk.toByteArray()); Thread.sleep(100L) }
stt.completeAudioSend()
```

**TTS/STT `ErrorType`** (when에 else 분기 필수 — 신규 타입 추가 가능): `AUDIO_ERROR`, `INVALID_REQUEST_ERROR`(호출 순서·중복 요청), `MIC_ERROR`(마이크·센서 개인정보 설정), `NETWORK_ERROR`, `NOT_SUPPORTED_API`(모드/환경 미지원), `ON_DEVICE_ENGINE_ERROR`, `PERMISSION_DENIED`, `SERVER_INTERNAL_ERROR`, `TIMEOUT`, `UNAUTHENTICATED`(Client ID/Secret·UA 앱 정보 확인), `UNAVAILABLE_LANGUAGE`, `UNKNOWN`
```kotlin
override fun onError(errorType: ErrorType) {
    when (errorType) {
        ErrorType.PERMISSION_DENIED -> checkPermission()
        ErrorType.UNAUTHENTICATED -> checkCredentials()
        ErrorType.NETWORK_ERROR, ErrorType.TIMEOUT -> retryWhenNetworkIsAvailable()
        else -> logError(errorType.name)
    }
}
```

### 8.5 FusedLocation SDK
- 차량 위치 데이터(FusedLocation Service). `LocationData(latitude, longitude, altitude: Double, heading: Float, confidence: Float, timestampMillis: Long)`.
- ⚠️ **사용 전 `isSupported()` 필수**(모델에 따라 미지원 → 위치 UI/기능 비활성화).
- 퍼미션 `pleos.car.permission.FUSED_LOCATION` — 보호 수준 **`dangerous`** → 매니페스트 선언 + **런타임 권한 요청 흐름 구현**. (Android `ACCESS_*_LOCATION` 동작은 이 SDK가 보장하지 않음. Google Fused Location 설명을 Pleos 동작 보장으로 해석 금지)
- 위치 접근이 개인정보 설정으로 차단된 경우 `showLocationAccessBlockedPopup()`으로 안내(허용해 주는 API 아님).
- API: `isSupported()`, `initialize()`, `getLastKnownLocation(FusedLocationListener)`(새로 계산 X, 없으면 null), `registerFusedLocationCallback(listener)`, `unregisterFusedLocationCallback(listener)`, `addProximityAlert(ProximityAlertRequest(lat, lng, radiusMeters: Float > 0, transitionTypes: Set<ProximityTransition{ENTER, EXIT}>), ProximityAlertListener{onRegistered(alertId), onTransition(ProximityAlertEvent), onError})`, `removeProximityAlert(alertId)`, `showLocationAccessBlockedPopup()`, `release()`.
- `unregisterFusedLocationCallback()`(리스너 해제)와 `release()`(SDK 해제)는 서로 대체 불가. initialize 했으면 반드시 release. release 후 같은 인스턴스 재사용 금지.
```kotlin
class MyApplication : Application() {
    lateinit var fusedLocation: FusedLocation; private set
    override fun onCreate() {
        super.onCreate()
        fusedLocation = FusedLocation(this)
        if (fusedLocation.isSupported()) fusedLocation.initialize()
        else { /* 위치 UI·기능 비활성화 */ }
    }
}
val listener = object : FusedLocationListener {
    override fun onChanged(location: LocationData?) { location ?: return /* null을 유효 위치로 취급 금지 */ }
    override fun onError(exception: Exception) { /* 기록 + 위치 불가 안내/기능 제한 */ }
}
fusedLocation.registerFusedLocationCallback(listener)
fusedLocation.unregisterFusedLocationCallback(listener)
fusedLocation.release()
```

---

## 9. Gleo Interface (Gleo AI 음성 연동)

### 9.1 개념
- Gleo AI(차량 음성 AI 에이전트)와 3rd party 앱이 음성 명령을 처리하는 인터페이스. **OpenAI function calling과 유사**(Android용 확장).
- 흐름: 앱이 `assets/schema.json`(**App Schema**)으로 호출 가능한 function을 등록 → 사용자가 말하면 Gleo가 function call 생성 → **Gleo Action**(Android Intent: `startActivity` 또는 `sendBroadcast`)으로 앱 호출 → 앱이 처리 후 **Callback**(Broadcast)으로 결과 반환 → Gleo가 최종 음성/텍스트 응답 생성.
- Music/Audio 앱의 곡 검색·재생 음성 기능 + 핵심 기능 3~5개 음성화에 활용(§7.6). STT SDK는 텍스트 변환만 하므로 "명령 실행"이 필요하면 Gleo Interface 사용.

### 9.2 App Schema (`assets/schema.json`)
| 필드 | 타입 | 설명 |
|---|---|---|
| `packageName` | string | 앱 패키지명 |
| `appDescription` | string | 앱의 주요 목적 간결 설명 (영어) |
| `version` | string | Pleos App Market 등록 버전 |
| (`invokeWord`) | string | 예시에만 등장(예: "리마인더") — 필드 정의 목록엔 없음 |
| `actionCalls` | array | 음성으로 호출 가능한 function 목록 — ⚠️ **최대 3개** |
| ↳ `type` | string | `"function"` 고정 |
| ↳ `function.component` | string | `"activity"`(기본, `context.startActivity()` — UI 필요 시) / `"service"`(`context.sendBroadcast()` — UI 없이 백그라운드) |
| ↳ `function.name` | string | 고유 이름, **대문자 + 언더스코어** (예: `ADD_REMINDER`), 2~3단어로 대상까지 포함 권장 |
| ↳ `function.description` | string | 목적·역할을 1~2문장 영어로 |
| ↳ `function.parameters` | object | `type: "object"` 고정, `properties` {이름: {type, description, enum?}}, `required: [...]` |

작성 규칙:
- 모든 description은 **영어**. Gleo 동작 조작/다른 앱 억제 문구 금지. description 안에 구체 예시 넣지 말 것.
- parameter `type`은 `string`/`number`/`boolean`/`array` 중 **하나만** (`["string","null"]` 불가). `enum`은 문자열 값만(null 불가). JSON Schema 전체 기능 미지원.
- `required`엔 필수 파라미터만(선택 파라미터 넣지 말 것). Gleo가 필수값을 항상 채운다는 보장은 없음 → 앱에서 누락 처리(`asking`).
- 각 function은 단독 호출로 명령을 완결하도록 설계.
```json
{
  "packageName": "ai.dot42.pleos.reminderapp",
  "appDescription": "A reminder app that lets you schedule tasks and receive notifications at the right time.",
  "invokeWord": "리마인더",
  "version": "1.0.0",
  "actionCalls": [
    {
      "type": "function",
      "function": {
        "name": "ADD_REMINDER",
        "description": "Add a reminder that will notify the user at the given date and time.",
        "parameters": {
          "type": "object",
          "properties": {
            "title":    { "type": "string", "description": "Title of the reminder." },
            "dateTime": { "type": "string", "description": "Date and time to trigger the reminder notification, in ISO 8601 date and time format." }
          },
          "required": ["title", "dateTime"]
        },
        "component": "activity"
      }
    }
  ]
}
```
(라디오 예시: `SWITCH_BAND`(band enum FM/AM, service), `SEARCH_STATION`(searchTerm, activity), `PLAY_FREQUENCY`(frequency: number, service))

### 9.3 Gleo Action 수신
Gleo가 만드는 내부 객체 `{ "packageName", "action": "ADD_REMINDER", "actionParameters": {...} }` → Android Intent로 변환: `action` = intent action, `packageName` = `setPackage()`, 파라미터는 Extra로 전달. **샘플 코드 기준으로는 `"ACTION_PARAM"` Extra에 JSON 문자열**로 들어옴(문서 본문엔 key-value를 개별 putExtra한다는 설명도 있으므로 둘 다 방어적으로 처리 권장).
- 매니페스트: function마다 action filter 정의
```xml
<activity android:name=".YourActivity">
    <intent-filter>
        <action android:name="ADD_REMINDER" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
<!-- component="service"인 function은 BroadcastReceiver에 동일하게 action 등록 -->
```
```kotlin
// component = "activity"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }  // 💡 실행 중 재호출 대비(문서엔 없음)
    private fun handleIntent(intent: Intent?) {
        val actionParam = intent?.extras?.getString("ACTION_PARAM") ?: "{}"
        val json = runCatching { JSONObject(actionParam) }.getOrElse { JSONObject() }
        when (intent?.action) {
            "SEARCH_STATION" -> { val term = json.optString("searchTerm") /* ... */ }
            "SWITCH_BAND"    -> { val band = json.optString("band") /* ... */ }
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }
    }
}
// component = "service"
class CustomReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ADD_REMINDER") {
            val json = JSONObject(intent.getStringExtra("ACTION_PARAM") ?: "{}")
            val title = json.optString("title", "No Title")
            val dateTime = json.optString("dateTime", "No DateTime")
        }
    }
}
```

### 9.4 Callback (앱 → Gleo)
- Broadcast action **`ai.pleos.playground.intent.action.APP_ACTION_RESPONSE`**, extras: `packageName`(내 패키지), `response`(JSON 문자열).
- ⚠️ Gleo는 **7초 후 Action을 timeout 취소** → 7초 안에 callback.

| 필드 | 필수 | 설명 |
|---|---|---|
| `functionName` | ✔ | App Schema의 function name과 동일 |
| `status` | ✔ | `"success"`(정상, 결과 0건이어도) / `"asking"`(필수 파라미터 누락·사용자 선택/확인 필요) / `"fail"`(비즈니스 실패 + crash·네트워크·timeout 등 시스템 오류) |
| `message` | ✔ | 완전한 **영어 문장**(검색어·지명 등 고유명사는 타 언어 가능) |
| `mode` | | `"voice"`(기본, TTS) / `"beep"`(효과음만) / `"silent"`(무음 — 앱이 직접 TTS/음악 재생 시) |
| `data` | 권장 | **JSON 객체 배열 문자열**(`"[ {\"k\":\"v\"} ]"`). 응답 생성에 필요한 정보만. silent/beep에서도 문맥 유지용으로 포함 권장 |
```kotlin
fun Context.sendGleoBroadcast(functionName: String, status: String, message: String,
                              data: String? = null, mode: String? = null) {
    runCatching {
        val json = JSONObject().apply {
            put("functionName", functionName); put("status", status); put("message", message)
            data?.let { put("data", it) }; mode?.let { put("mode", it) }
        }
        Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").apply {
            putExtra("packageName", packageName)
            putExtra("response", json.toString())
        }.also { sendBroadcast(it) }
    }.onFailure { Log.e("ThirdPartyApp", "respondAction onFailure >> $it") }
}
sendGleoBroadcast("SWITCH_BAND", "success", "Successfully switched to the requested radio channel.",
    """[{"frequency":"91.9MHz","frequencyBand":"FM","stationName":"MBC","channelName":"MBC FM4U"}]""")
sendGleoBroadcast("ADD_REMINDER", "asking", "A registration date is required.", """[{"missingField":"dateTime"}]""")
sendGleoBroadcast("ADD_REMINDER", "fail", "It exceeds 10 items, so it cannot be added.")
```

### 9.5 스키마 전달·반영
- 파일명 **`schema.json`**, 위치 **`app/src/main/assets/schema.json`**. 제출 후 가이드라인 준수 검토 → 승인되면 자동 활성화.
- Gleo AI 앱은 **부팅 시 1회** `AssetManager`로 전체 앱의 `assets/schema.json`을 스캔해 DB 저장. 앱 설치/삭제(ADB install 또는 마켓) 시에도 해당 앱 schema 갱신 → 개발 중 스키마 수정 후엔 재설치(또는 재부팅)로 반영.

---

## 10. 앱 테스트 가이드

### 10.1 폼 팩터 테스트 (다양한 디스플레이 크기/방향 검증)
준비: Android Studio + Platform-Tools, **Go 1.22+**, 앱 설치된 Emulator, 포트 **8095, 8096** 사용 가능. ⚠️ 에뮬레이터를 **별도 창**으로 실행(Running Devices 내장/Headless 금지 — 가상 디스플레이가 안 보임).
1. AVD 실행 → `adb devices`(여러 대면 `<SERIAL>` 지정) → `adb emu avd name`으로 AVD 이름 확인(예: `Pleos_Connect_v2.0`)
2. **Host Display Bridge** 실행: `https://nexus-playground.pleos.ai/repository/raw-releases/host-display-bridge/1.0.0/host-display-bridge.zip` 다운로드·압축 해제 → 디렉터리에서 `go run . --avd "<AVD_NAME>"` → `listening on http://127.0.0.1:8096` 로그 확인, 테스트 끝까지 터미널 유지
3. 에뮬레이터에서 **FormFactorConsole** 앱 실행 (`UNAVAILABLE` → Bridge 확인 후 Refresh / `DISCONNECTED` + "DISPLAY2가 없습니다" = 준비 완료)
4. Display preset(폼 팩터) + Orientation(Portrait/Landscape) 선택 → dp/DPI/px 확인 → **가상 디스플레이 생성** → `DISPLAY2` 창에서 앱 실행
5. 확인: 요소 잘림/겹침 없음, 텍스트·이미지·아이콘 크기/정렬, 방향별 레이아웃, 터치·화면 전환. 다른 폼 팩터는 가상 디스플레이 제거 후 반복(제거 시 해당 디스플레이 앱 강제 종료)
6. 종료: 가상 디스플레이 제거 → Bridge `Ctrl+C` → `adb forward --remove tcp:8095`

### 10.2 Virtual Console 사용 가이드 (Emulation Controls 카드와 범위)
| Domain | 카드: 범위 |
|---|---|
| Vehicle | Notification 1: 차량 알림 표시/숨김 |
| Motion | Gear Selection: P/R/N/D · Brake Pedal: 0~100% · Odometer: 0~1,000,000 km(1 km) · Steering Angle: -540~540°(1°) · Steering Wheel Heat: Off/Low/Medium/High |
| ADAS | Cruise Control State: Not ready/Ready/Longitudinal/Lateral/Full autonomous · Cruise Target Speed: 5.556~27.778 m/s (0.277775 단위) |
| Convenience | ETCS Power On/Off · ETCS Scenario(카드 삽입·제거, 요금소 진입·진출) · Door Position(앞/뒤 좌우 열림·닫힘) · Door Lock · Trunk/Frunk · Cabin Lights · Reading Lights(좌석별) · Ambient Brightness 0~100% · Ambient Color(Cyan/Blue/Purple/White) · Exterior Lights Mode(Off/Auto/Tail lamp/Low beam) · High Beam · Hazard · Stop Lights · Door Lights · Turn Signal(Off/좌우 Blink/Flash) · Window Position 0~100% · Window Lock · Seat Occupancy(Empty/Occupied) · Wiper(앞/뒤 Off/Auto/Low/High) · Glove Box |
| Safety | Telltales: 주차브레이크·안전벨트·ABS·TPMS 등 Off/On/Blink |
| Energy | Remaining Range 0~1,000,000 m(1,000 m) · Battery Energy 0~200 kWh(0.1) · Battery SOC 0~100% · Battery Capacity 0~200 kWh · Charge Current Limit 10~48 A(0.5) · Charge Percent Limit 50~100%(5%) · Charge Switch · Charge Port Closed/Open |
| Climate | HVAC Power · A/C · Auto · Dual · Recirculation(Fresh/Recirculate) · Fan Speed 0~8 · Fan Direction(Face/Floor/Defrost, 좌석별) · Defroster(전/후) · Target Temperature 16~30°C(0.5) · Seat Temperature(통풍 3단/Off/열선 3단) · Air Cleaner |

사용법: Emulation Controls → Domain 선택/카드 검색 → 영역 선택 → 버튼·목록·슬라이더로 값 변경 → Emulator와 앱에서 결과 확인. 💡 앱의 `register...` 콜백, 주행 규제(기어 D), 경고 UI 등을 이걸로 검증.

---

## 11. 앱 등록·배포 가이드

### 11.1 앱 태그 표기
- 공식 앱명이 외국어(예: 영어)이면 한국어로 노출되더라도 **원문(영문) 앱명 태그 필수**(축약·변형 금지). 한국어 태그는 선택. 다국어 공식명은 각각 추가 가능(영문은 필수). 미준수 시 반려/수정 요청. 예: 공식명 Sport → 필수 태그 `Sport`, 선택 `스포츠`, `운동`.

### 11.2 앱 업데이트 (⚠️ 자동 업데이트 미지원 → 앱이 직접 구현)
1. 앱 실행 시 최신 버전 여부 확인(**개발사 구현**, 마켓 SDK는 차후 고려)
2. 최신이 아니면 팝업: `최신 버전이 존재합니다. 업데이트를 위해 페이지를 이동할까요? (Yes, No)` (개발사 구현)
3. Yes → 앱 상세 페이지로 이동: **`market://detail?package={패키지명}`** (개발사 구현)
4. 마켓이 로그인 여부 확인·처리 → 상세 페이지에서 사용자가 [업데이트]
5. No → 개발사 정책에 따라 앱 계속 사용 또는 종료
```kotlin
// 💡 3번 예시 (문서의 URI 그대로)
startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://detail?package=$packageName")))
```

### 11.3 앱 버전 관리
- ⚠️ 배포/심사 요청 시 **반드시 버전 변경**(기능·버그·설정 등 모든 변경). 버전 미변경 시 심사 미진행/반려. → `versionCode`/`versionName` 증가 확인.

### 11.4 앱 보안 가이드 (미준수 시 심사 불통과 가능)
- **민감 데이터**: 저장 위치와 무관하게 암호화(내/외부 저장소, 캐시, SQLite, SharedPreferences, 임시 파일 포함). 디버그 로그에 민감정보 금지(출시 전 비활성화/마스킹). 민감정보 캐시 지양. 자동 백업 비활성화 권장(`android:allowBackup="false"` 또는 민감 파일 제외). 외부 저장소 지양. **URL 경로/쿼리에 토큰·세션·비밀번호 금지**(Authorization 헤더 사용).
- **암호화**: 표준 알고리즘, MD5/SHA-1 대신 SHA-256, **AES-GCM**(ECB 금지), RSA는 OAEP(SHA-256), GCM에서 같은 키로 IV/nonce 재사용 금지, `SecureRandom`, 키는 **Android Keystore**(하드코딩·평문 저장 금지).
- **통신**: HTTPS 필수, **TLS 1.2 이상(1.3 권장)**, SSL/TLS1.0/1.1 금지, 안전한 Cipher Suite, 신뢰 서버만, 서버 원격 명령은 출처·유효성·무결성 검증 및 범위 제한.
- **컴포넌트/권한**: 필요한 권한만, 외부 호출 불필요한 Activity/Service/Receiver/Provider는 `exported="false"`, 민감정보는 명시적 수신자 지정(암시적 브로드캐스트 금지), 공개 컴포넌트는 호출자 인증+권한 모두 확인, WebView 로컬 파일 접근 최소화/비활성화, 외부 URI·경로 검증/정규화.
- **개발·배포**: Pleos 승인 API만 사용, 필요한 차량 API만, 토큰·비밀키 하드코딩 금지, 서버는 IP 대신 도메인, 네이티브 라이브러리 RELRO/Stack Canary/NX, 의존성 취약점 주기 점검, **정식 배포 서명 인증서**, 배포 빌드에서 **디버깅·WebView 원격 디버깅 비활성화**.
- **WebView 앱**: 로드 콘텐츠/이동 가능 사이트 범위 제한, JS·JS 브리지는 필요 시에만.
- **웹브라우저 앱 추가 요건**: 보안 업데이트되는 엔진, 악성 사이트 차단 권장, HTTPS 우선·HTTP 경고, 인증서 검증 우회 금지, SOP/CORS 준수, **CSP Level 2 이상**, 샌드박스 유지, 개발자 모드·프록시 변경·확장 설치 비활성화, **파일 다운로드 제한**.

### 11.5 앱 심사 제출 (Pleos App Market Console: https://appmarket.pleos.ai/console/)
**제출 전 준비**: 개발자 연락처 최신화 / 실행 불가·크래시·성능 저하·버그·이미지 깨짐·해상도 늘어짐 점검(과부하·배터리 소모 주의) / 보안 취약점·악성코드 점검 / 메타데이터 정확성 / **심사팀용 전체 기능 접근 가능한 계정**(관리자 계정 X, 실제 사용자 기준; 게스트 가능해도 일반 계정 제공) / 외부 서버 등 운영 요소 미리 활성화 / 주행 안전 설계 / 출시 국가 법규 준수(증빙) / 연령 등급(**IARC** 심의 + 증적; 한국 게임은 게임물관리위원회, 성인용 게임은 GRAC 등급) / 설명 필요한 기능은 심사 노트·사용 설명서 첨부.

| 구분 | 항목 | 필수 | 규격/설명 |
|---|---|---|---|
| 앱 식별 | 앱 관리명, 버전명 | 필수 | 마켓에 노출 안 됨 |
| 기본 정보 | 국가/지역 | 필수 | 출시 국가 |
| | 패키지 파일 | 필수 | **APK, 최대 600 MB** |
| | 대상 플랫폼 | 필수 | **Connect** 또는 **InfoLINK** |
| | 카테고리 / 연령 등급 | 필수 | 매니페스트 카테고리와 일치 |
| | 개인정보 처리 / 위치정보 활용 / 앱 접근 권한 / 광고성 정보(푸시·SMS) | 필수 | 정직하게 |
| | SDK 추가 권한 | 필수 | 권한 신청 필요한 SDK 사용 시 **My Project > 프로젝트 정보 > 프로젝트 ID** 입력 |
| 언어별 정보 | 앱 이름 | 필수 | 출시 국가별 모든 언어, **영문 기준 30자 이하**, 이모티콘·반복 특수문자 금지 |
| | 개인정보처리방침 URL | 필수 | 국가별 언어 |
| | 위치정보 이용약관 등 URL | 조건부 | 위치정보/GPS 사용 시 |
| | 간단한 설명 / 자세한 설명 | 필수 | 국가별 모든 언어 |
| | 태그 | 선택 | 최대 20개 (§11.1) |
| | 앱 아이콘 | 필수 | **PNG/JPEG, ≤1 MB, 512×512 px** |
| | IVI 스크린샷 | 필수 | **PNG/JPEG, 장당 ≤8 MB, 2~8장**, 규격 2560×1380 / 1700×1212 / 840×1212 px (실제 앱과 동일해야 함) |
| | 출시 노트 | 필수 | |
| 고객 지원 | 웹사이트/이메일/전화 | 1개 이상 | |
| 심사 요청 | 심사 노트 | 선택 | 파일 첨부 ≤60 MB |
- 심사 통과 후에도 주기적 샘플링 모니터링 → 심각한 오류·장기 방치·다수 신고 앱은 재심사, 기준 미달 시 마켓 제외.

### 11.6 App Review Guidelines (심사 기준 요약)
심사의 중점: **운전자 주의 분산 요소**와 **사이버보안 위협**. 안전 위험 가능성이 있으면 거부.
- **콘텐츠 안전성**: 선정·폭력·위험(자해, 아동이 차 밖으로 신체를 내밀게 유도 등)·불법·사행성(실제 금전 도박)·차별·민감(재난 등 이용) 콘텐츠, 미승인 물품(처방약 등) 금지. 음주운전·과속·신호위반 미화 금지(주류 광고는 현지 광고법 + 음주운전 경고). OTT·웹툰·UGC/AI 생성 콘텐츠 앱은 **필터링 + 신고 기능** 필수. 인증 없이 보이는 화면·썸네일·스크린샷은 '전체 이용가'만. 지적재산권 침해 금지(신고 시 다운로드 차단 후 소명). **암호화폐 채굴 앱 거부**. 루트박스는 확률 공개.
- **기능·성능 안정성**:
  - 설치/삭제/실행/사용 중 오류 금지(인증키 누락, 파일 잔존, 강제 종료, 화면 멈춤, 터치 미인식, 사운드 끊김, 그래픽 깨짐). 더미 데이터·임시 파일/콘텐츠 금지(실서비스와 동일).
  - 디스플레이 컷아웃·인셋·시스템 바·소프트 키보드 등 **안전 영역 준수**.
  - 카테고리를 벗어난 기능 → 거부. 스터터링/프레임 저하 금지.
  - ⚠️ 응답 시간: 입력 후 **0.25초** 이내 터치 효과 / **앱 실행 10초 이내**, **콘텐츠 로드 10초 이내**, 버튼 반응 **2초 이내**(초과 시 로딩 UI 표시).
  - 백그라운드 → 복귀 시 **마지막 상태 복원**(수 시간 이상 미사용 시 기본 상태 허용).
  - HW/SW 손상·과점유 금지, 기기 재시작이나 무관한 시스템 설정 변경 요구 금지.
  - **Vehicle SDK 퍼미션은 App Market과 사전 협의·승인된 것만 선언 가능**(미승인 선언 시 불통과).
  - 디스플레이 호환성: **레터박스 없이 화면 최대한 채움**(미세 비율 보정용 한 방향 레터박스만 허용), **호환성 모드 실행 금지**, 해상도/화면 전환 시 상태 손실·이미지 늘어짐·블러·터치 영역 불일치 금지, 모든 화면 상태에서 **기능 패리티**(버튼/기능이 바뀌면 안 됨).
- **운전자 주의 분산(미디어 앱)**: 동영상·게임·브라우저 기능 앱 = 미디어 앱 → **정차 중에만 사용**, 주행 시작 시 **UI 숨김 + 일시정지**, 주행 중 일시정지 해제 불가. 모든 미디어 항목에 제목·썸네일. **헤드업 알림 게시 금지**(운전 무관 알림 불가). 동영상 앱은 미디어 세션 통합(동시 다중 미디어 불가, 재생/일시정지/중지 지원). 브라우저 앱은 비밀번호·결제정보 저장 금지(프로필 잠금 시 예외), 동기화 시 인증 메시지 + 동기화 고지.
- **등록 정보**: 왜곡·허위 금지, 실적·순위 등 확인 불가 내용 금지, 타 앱과 유사/오해 소지 제목·아이콘 금지, 부적절 카테고리는 임의 변경 가능. 증빙: 타사 IP 사용권, 정부 기관 앱, 웹앱 도메인 등록 증빙, 의료 기관 제휴, 성인용 게임 GRAC.
- **개인정보·보안**: 접근 권한 최소화, 법적 고지(항목·목적·보유기간·거부권) + **명시적 동의**(동의 버튼 외 동작(뒤로가기, 빈 곳 터치)을 동의로 간주 금지), 아동은 법정대리인 동의, 동의 철회 시 즉시 사용 중단·데이터 삭제, GDPR 등 국가별 법령. 바이러스·스파이웨어·애드웨어·루팅·보안 우회·스팸·DDoS·크립토재킹·피싱 금지. SDK 악용·사용자 데이터 무단 활용 금지.

---

## 12. Tutorials & Samples

### 12.1 공식 샘플 저장소 (GitHub)
- Vehicle SDK: https://github.com/Pleos-Playground/pleos-sample-vehicle-app
- NaviHelper SDK: https://github.com/Pleos-Playground/pleos-sample-navi-app
- FusedLocation SDK: https://github.com/Pleos-Playground/pleos-sample-fusedlocation-app

### 12.2 Vehicle SDK Sample App 사용 가이드
- 기준: Sample App `1.0.0-sdk35`, Vehicle SDK `2.3.2`, Vehicle Service `2.3.22`, AVD `Pleos_Connect_v2.1`(Android 14), **JDK 17, Gradle wrapper 8.10.2, AGP 8.8.2, Kotlin 2.0.0, compileSdk/targetSdk 35**, Sample minSdk 23(⚠️ 제품 앱은 minSdk 28 이상). SDK 버전은 `gradle/libs.versions.toml`에서 관리.
- 빌드/설치: `./gradlew assembleDebug && ./gradlew installDebug` (APK: `app/build/outputs/apk/debug/`) → Launcher에서 **Vehicle SDK Sample** 실행. SDK 버전을 바꾸면 `PublicApiPolicyTest`의 public API 수 기준 확인.
- ⚠️ 반드시 **Pleos Vehicle Service가 포함된 전용 AVD** 사용(일반 폰/기본 에뮬레이터 X).
- 화면: `Vehicle Playground`(SDK·Service 버전, 초기화 상태) → `VEHICLE DOMAINS` 분류 → 기능 카드(예: Door Position) → 상세: `Vehicle support`(check) / `Current value`(get, Area 선택, Refresh) / `Change value`(set, Apply change) / `Live updates`(register/unregister) / `API activity`(호출 결과 로그).
- Door Position 기준 흐름: `checkDoorPositionCapabilities` → `getCurrentDoorPosition` → `registerDoorPosition` → `setDoorPosition` → `unregisterDoorPosition`. 예시 범위 `0..1`(Closed 0 / Open 1)이지만 **환경마다 다르니 하드코딩 금지**.
- 교훈(제품 코드에 그대로 적용할 것):
  - `SDK initialized` = initialize 호출이 예외 없이 반환됐다는 뜻일 뿐, **서비스 연결/기능 가용성 보장 아님**.
  - `Unsupported`에는 실제 미지원 + 지원 확인 요청 실패가 섞일 수 있음 → 오류 메시지로 구분.
  - **set의 완료 콜백 값은 요청값**이지 실제 차량 상태가 아님 → 재조회(get) 또는 리스너 알림으로 확인.
  - register/unregister의 "Complete"는 SDK 호출이 예외 없이 반환됐다는 뜻(서비스 완료 응답 아님).
  - 화면 이동만으론 구독 해제 안 됨 → **등록한 동일 Listener + 동일 Area로 명시적 해제**. 다른 area로 해제하면 오류.
  - 기능은 Supported인데 특정 영역 조회 실패 가능(예: `area ID: 0x1 not supported`) → 기능 단위 지원과 영역 단위 지원을 구분.
  - Vehicle 퍼미션은 런타임 승인 팝업 방식이 아님(매니페스트 선언 + 마켓 승인). Door Position: 조회·구독 `CAR_DOORS`, 변경 `CONTROL_CAR_DOORS`.
  - 제품 앱은 Sample App의 전체 권한 목록을 복사하지 말고 **사용하는 typed API의 최소 권한만** 선언.
- Sample App 매니페스트에 보이는 추가 public permission(참고): `CAR_CONNECT_VENDOR_CONTROL, CAR_MILEAGE, CAR_POWERTRAIN, CAR_SPEED, CAR_TIRES, READ_CAR_DISPLAY_UNITS, READ_CAR_STEERING, READ_ADAS_STATES, READ_WINDSHIELD_WIPERS, CONTROL_WINDSHIELD_WIPERS` (+ CAR_*/CONTROL_CAR_* 11종). `PRIVATE_*`는 미포함.

---

## 13. Release Notes (2026-10 기준)

### 13.1 Pleos Connect Emulator v2.1.0 (2026-10-19)
- 신규: **Virtual Console**, **폼 팩터 테스트** 기능 / 개선: 탑재 SDK 버전 업데이트
- 제거: **LLM SDK 제공 종료**(→ Gleo Interface 사용), **CRN 제공 종료**, 블루투스 동글 연동 종료

### 13.2 Pleos Connect SDK (2026-10-19) — Vehicle 2.3.2 / NaviHelper 3.0.5 / TTS 3.3.1 / STT 3.3.1 / FusedLocation 1.1.3
**Vehicle SDK 2.3.2**
- Breaking: `CruiseControl` 상수 변경(OFF/READY/ACTIVE 제거 → `OTHER, ENABLED, ACTIVATED, USER_OVERRIDE, SUSPENDED, FORCED_DEACTIVATION_WARNING`; 단순 1:1 치환 금지) / `checkHvacFanSpeedCapabilities` 콜백 `Map<SeatArea, List<Int>>` → `Map<SeatArea, AreaBounds<Int>>` / `FanDirectionType` 3→8개(UNKNOWN·복합 풍향), ordinal 변경(FACE 0→1, FLOOR 1→2, DEFROST 2→4 — ordinal 저장 시 마이그레이션) / `DoorArea, MirrorArea, SeatArea, TurnSignalArea, WheelArea, WindowArea`에 내용 기반 equals·hashCode(같은 값이면 동등)
- New: `checkVINInfoCapabilities`, `checkSmartGlassLevelCapabilities`, `getCurrentEvEfficiency`, `registerEvBatteryStateOfCharge`, `getCurrentSmartGlassLevel`, `getVINInfo`, 모델 `CapabilityConfig, TripStatistics, TelltaleData, Telltales`
- Changed: 기존 capability API에 `CapabilityConfig` 포함 새 오버로드(구 콜백 형태 deprecated — 새 오버로드 사용) / `registerHvacTemperature(Current)`에 `HvacTemperatureValueListener` 오버로드 / `EvBatteryLevelListener, EvChargePortOpenListener, RangeRemainingListener` 문서 숨김(동작 유지)
- Deprecated: `getCRNInfo`/`checkCRNInfoCapabilities` → `getVINInfo`/`checkVINInfoCapabilities`, `checkWheelAreaCapabilities` → `checkLowTirePressureCapabilities`
- 삭제: `getHvacTemperatureValueSuggestion`, `VehicleSpeedListener`(onSpeedUpdated) → 속도는 AAOS `PERF_VEHICLE_SPEED`

**NaviHelper 3.0.5**
- Breaking: `onNavigationStatus` 인자 `RouteDriving` → `NavigationStatus` / **데이터 모델 생성자 좌표 순서 `longitude, latitude` → `latitude, longitude`** (BookmarkInfo, CurrentLocationInfo, DestinationInfo, RecentDestinationInfo, RequestWaypointInfo, RouteInfo, WaypointInfo) / DestinationInfo·WaypointInfo `totalDistance/totalTime` → `remainDistance/remainTime`
- New: `requestTrip`, `requestRouteWithWaypoints`, `requestNavigationScreen`, `initialize(onConnected, onFailed)`, 이벤트 `onCautionPointInfo, onRoutingCompleted, onTripInfo`
- Changed: 모델 필드 확장(POI provider, 좌표/거리, Trip/Traffic/Waypoint), enum `CameraType, CautionPointType, NavigationStatus, RoutePointType`
- Deprecated: `onRouteEnded` → `onRoutingCompleted`
- 삭제: `FindRouteResult`, `onUserDefinedRoute`, `CameraType.SPEED_CAMERA_SECTION`(→ `SPEED_CAMERA_SECTION_START, SECTION_SPEED_ALERT, SPEED_CAMERA_SECTION_END`)

**TextToSpeech / SpeechToText 3.3.1**
- Breaking: 오류 콜백 `String` → `ErrorType` (`EventListener/ResultListener.onError(errorType)`, `OnServerConnectionListener.onFailed(errorType)`)
- New: `initialize(OnInitializeListener)`, `version` 속성, `ErrorType` enum, (STT) `InferenceSource { ON_DEVICE, SERVER }`
- 삭제: (TTS) `ServerType` enum, `tts.utils.Log/StringUtils` 공개 제거 / (STT) `DevelopmentOnly` annotation, `stt.utils.Log/StringUtils` 공개 제거

**FusedLocation 1.1.3**
- Breaking: `FusedLocationListener.onChanged(LocationData?)`(nullable), `onError(exception)` 구현 필수
- New: `addProximityAlert/removeProximityAlert`, `getLastKnownLocation`, `isSupported`, `showLocationAccessBlockedPopup`, ProximityAlert 모델/리스너

### 13.3 Pleos Playground
- v2.7.0 (2026-10-29): 주소 `pleos.ai/playground` → **`developers.pleos.ai`**, 브랜드 사이트 연결, 소제목 검색
- v2.6.0 (2026-10-19): Developer Journey(Discover > Developer Journey), 문서 검색, 개발자 가이드 재구성, **LLM SDK 종료**

---

## 14. Claude Code 작업 체크리스트 (Pleos Connect 앱 구현 시)

**프로젝트 설정**
- [ ] `settings.gradle.kts`에 Pleos Maven(`https://nexus-playground.pleos.ai/repository/maven-releases/`) 추가
- [ ] `minSdk = 28` 이상, `compileSdk`/`targetSdk`는 34~35 (샘플 기준 35), Kotlin/JDK 17
- [ ] 필요한 SDK만 의존성 추가 (§8.0 버전표)
- [ ] 매니페스트: 사용하는 API의 최소 `pleos.car.permission.*`만 (Vehicle 퍼미션은 마켓 사전 승인 필요), FusedLocation은 런타임 권한 요청
- [ ] 카테고리 선언: 내비 앱 `android.intent.category.APP_MAPS`, 오디오 앱 `android:appCategory="audio"` + `androidx.car.app.launchable=true`
- [ ] CCP 미지원 시 `com.hmg.rotary.ROTARY_SUPPORTED=false` 메타데이터, 그래도 터치만으로 모든 필수 조작 가능하게
- [ ] `android:allowBackup="false"`(또는 민감 파일 제외), 불필요 컴포넌트 `exported="false"`, release 빌드 `debuggable=false`, WebView 디버깅 off

**코드 패턴**
- [ ] SDK 객체는 생명주기에 맞춰 1회 생성·`initialize()`, 종료 시 리스너 해제 → `release()` (비동기 콜백 완료 전 release 금지)
- [ ] Vehicle: `checkXxxCapabilities` → `AccessType`/`AreaBounds` 확인 후 get/set, 범위 하드코딩 금지, `VehicleException.errorCode` 분기
- [ ] set 후 실제 상태는 get/리스너로 확인. 리스너는 동일 객체·동일 area로 unregister
- [ ] 표준 데이터(속도·기어 등)는 AAOS `CarPropertyManager` (예: `PERF_VEHICLE_SPEED`), 모델명 분기 금지
- [ ] `CarUxRestrictionsManager` 리스너로 주행 규제 반영(영상 정지, 키보드/설정/로그인/긴 텍스트 제한), 지역 하드코딩 금지
- [ ] 오디오: 재생 전 `AudioFocusRequest`, 포커스 변경 콜백 처리(LOSS 정지/abandon, TRANSIENT 일시정지, CAN_DUCK 감쇄), 내비 안내는 `GAIN_TRANSIENT_MAY_DUCK`, Gleo 호출 시 즉시 정지
- [ ] Suspend 진입 시 정지→포커스 반납→상태 저장→리소스 해제, Resume 시 자동 재생 금지
- [ ] 미디어 앱: MBS/MediaSession(또는 Media3 `MediaLibraryService` + 검색), 자동 재생 금지, `startService`+`startForeground`, `onTaskRemoved` 순서, `onStop()`→`player.stop()`
- [ ] 음성 안내 재생 전 150~200 ms 무음 패딩
- [ ] CCP Back(최상단) → `moveTaskToBack(true)`, `finish()` 금지
- [ ] portrait/landscape 두 레이아웃 + 반응형, 백그라운드 복귀 시 상태 복원, 프로세스 재생성 대비 상태 저장
- [ ] IME 없는 Dialog는 `setFitInsetsTypes(statusBars or navigationBars)`
- [ ] 네트워크 끊김 대비(캐시·retry·timeout·오프라인 UI), HTTPS/TLS1.2+
- [ ] 비밀값(Client Secret 등) 하드코딩 금지, Keystore + AES-GCM
- [ ] 앱 내 버전 체크 → 팝업 → `market://detail?package=...` 이동 구현
- [ ] Gleo 연동 시 `assets/schema.json`(≤3 function, 영어 description) + intent-filter + 7초 내 `APP_ACTION_RESPONSE` 브로드캐스트

**성능/UX 기준값**
- CPU: 내비(주행) ≤15%, 비내비(주행) <5%, 정차 ≤25% / RAM ≤400 MB / 입력 응답 ≤0.25 s / 실행·콘텐츠 로드 ≤10 s / 버튼 반응 ≤2 s / 대비 ≥4.5:1 / 최소 폰트 12sp(실제 ≥18px) / 텍스트 한 줄 ≤80자·3줄 이하 / 아이콘 48dp 그리드 / 앱 제목 ≤30자

---

## 부록 A. 문서 URL 맵 (https://document.pleos.ai + 아래 경로)

| 카테고리 | 페이지 | 경로 |
|---|---|---|
| 시작 | 앱 개발 시작하기 | /docs/pleos-only/getting-started |
| 시작 | 프로젝트 생성 및 SDK 사용 신청 | /docs/pleos-only/create-my-project |
| 환경 | Emulator 설치·실행 / 설정 / Virtual Console 셋업 | /docs/connect/guide/getting-started/application-development/{setup-connect-sdk-emulator, configure-pleos-connect-emulator, use-vehicle-emulator} |
| 설계 원칙 | 설계 가이드 / 주행 안전 / 지역별 규제 / 카테고리 / 구조·동작 / 하드웨어 접근 / SDK API 정책 / 호환성 / 보안·개인정보 / 성능 | /docs/pleos-only/vehicle-app-planning-guide/{intro, driver-distraction-guideline, driving-restiction-policy, app-category, app-structure-screen-model, platform-api-architecture, pleos-connect-sdk-api, api-compatibility-availability, security-privacy, performance-resource-mgmt} |
| 디자인 | UI Design Guide 인트로 | /docs/connect/guide/docs-design/Introduction/intro |
| 디자인 | Display Layout / Window Policy | /docs/connect/guide/docs-design/getting-started/{layout, window} |
| 디자인 | Foundations | /docs/connect/guide/docs-design/foundations/{colors, typography, animation, shadow, contents-creation/iconography, contents-creation/app-icon} |
| 디자인 | Components | /docs/connect/guide/docs-design/components/{status-bar, gnb, app-library, actions/basic-button, actions/toggle-button, actions/text-button, actions/icon-button, actions/keypad, bar, controllers, indicator, dropdown, fields/text-field, fields/number-field, page-navigation, popups/system-popup, popups/toast-popup, segmented-menu, selections/checkbox, selections/radio, selections/switches, spinner, notification/system-notification, toast, widgets} |
| 개발 | 개발 가이드 인트로 / 오디오 포커스 / Suspend / 주행 규제 / CCP / 내비 / 음악 / TTS 묵음 / IME / FAQ 2종 | /docs/pleos-only/vehicle-app-developer-guide/{intro, audio-focus-development-guide/audio-focus-development, audio-focus-development-guide/suspend-mode-audio-focus, driving-restriction-development-guide, ccp-rotary-controller-development, navigation-app-development-guide, music-audio-app-developement-guide, voice-guidance-preroll-silence-guide, ime-window-resize-handling-guide, vehicle-app-faq, adb-command-reference-guide} |
| SDK | Overview | /docs/pleos-only/connect-sdk-pleos/ |
| SDK | Vehicle | /docs/pleos-only/connect-sdk-pleos/Vehicle/{intro, project-setup, vehicle-permission, obtain-domain-object, check-capability, get-property, set-property, callback, vehicle-error-code, entry(API Ref)} |
| SDK | NaviHelper | /docs/pleos-only/connect-sdk-pleos/NaviHelper/{intro, importing-sdk, declaring-permissions, initialization-and-release, event-listeners, destinations, routes, waypoint-settings, route-with-waypoints, error-handling, entry} |
| SDK | TextToSpeech | /docs/pleos-only/connect-sdk-pleos/TextToSpeech/{intro, importing-sdk, declaring-permissions, initialization-and-release, event-listeners, set-locale, app-registration, start-and-stop, error-code, entry} |
| SDK | SpeechToText | /docs/pleos-only/connect-sdk-pleos/SpeechToText/{intro, importing-sdk, declaring-permissions, initialization-and-release, result-listeners, app-registration, set-locale, start-and-stop, sending-audio-data, error-code, entry} |
| SDK | FusedLocation | /docs/pleos-only/connect-sdk-pleos/FusedLocation/{intro, project-setup, declaring-permissions, initialization-and-release, location-data-access, error-handling, entry} |
| Gleo | Interface / App Schema / Callback Schema / 예제 / 스키마 전달 / 라디오 Use case | /docs/pleos-only/app-integration-guide/guide/{core-concepts, app-capability, action_callback, examples, deliver-schema}, /docs/pleos-only/app-integration-guide/use-cases/radio-app |
| 테스트 | 폼 팩터 / Virtual Console 사용 | /docs/pleos-only/app-test-guide/{form-factor-test-guide, virtual-console-guide} |
| 등록 | 태그 / 업데이트 / 버전 / 보안 | /docs/pleos-only/app-registration-guide/{app-tagging-guide, app-update-guide, app-version-management-policy, app-security-guide} |
| 배포 | 심사 제출 / 심사 가이드라인(+5개 상세) | /docs/pleos-only/distribute/{app-submission-guide, app-review-guide/{review-content-safety, review-functionality-performance-stability, review-driver-distraction-elements, review-app-registration-info, review-user-privacy-security}} |
| 튜토리얼 | Vehicle SDK Sample App | /docs/pleos-only/tutorial/vehicle-sdk-sample-app |
| 릴리스 | Emulator / SDK / Playground | /docs/pleos-only/release-note/{emulator-release-note, sdk-release-note, playground-release-note} |

## 부록 B. 문서에서 확인되지 않거나 주의할 점
- 디자인 가이드의 색상 값, 타이포 크기, 컴포넌트 치수, 오디오 Interaction Matrix 표 자체는 **이미지/Figma로만 제공** → 원문 이미지 또는 design assets 다운로드 필요.
- 원문 예제 간 불일치: Getting Started는 `Vehicle:2.3.1`, SDK 문서·릴리스 노트는 `2.3.2`(최신 사용 권장). NaviHelper Java 예제의 `RouteInfo` 인자 순서가 data class 선언(위도 먼저)과 다름 → Kotlin named argument 사용. Gleo 파라미터 전달 방식(개별 putExtra vs `ACTION_PARAM` JSON) 설명이 혼재.
- 내비 탑재 여부 확인 방법·UX 규제 Figma 사양은 비공개(partnership@pleos.ai 문의).
- 실차 전용: `Build.DEVICE` 플랫폼 구분, CCS 연결 명령. 에뮬레이터 코덱 지원 ≠ 실차.

