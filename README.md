# 길동무 (Gildongmu)

**가는 길이 놀이가 되는 차량용 가족 레크리에이션 서비스**

차가 이미 알고 있는 **주행 상태 · 남은 시간 · 현재 위치**를 게임에 연결합니다. 주행 중에는 음성으로, 정차 중에는 화면으로 즐기고, 게임 응답에서 운전자의 반응 저하를 살펴 가볍게 말을 걸어 줍니다.

- 팀: **Team-DonDoAYeon** · 현대자동차그룹 차량용 앱 개발 공모전 2026 1차 제출용 프로토타입
- 패키지 `com.gildongmu.app` · 버전 1.0.0 (versionCode 1) · minSdk 28 / targetSdk 37
- 시연 영상: _(링크 자리 — 녹화 후 추가)_

---

## 1. APK 위치

| 파일 | 설명 |
|---|---|
| [`apk/Team-DonDoAYeon_길동무_release.apk`](apk/Team-DonDoAYeon_길동무_release.apk) | 실행 가능한 **release** 빌드 (서명됨, `debuggable=false`, R8 최적화) · 약 23 MB |

---

## 2. 실행 방법

### 2.1 Pleos Connect Emulator 준비
1. Android Studio → SDK Manager에서 **Pleos Connect Emulator 2.1.0** (API 34 Automotive 이미지) 설치
2. AVD 생성: RAM **4 GB**, Boot option **Cold boot** (Quick boot면 VHAL을 쓸 수 없음)
3. AVD 실행 후 확인: `adb devices`

### 2.2 설치
```bash
# 방법 A — 저장소의 release APK
adb install apk/Team-DonDoAYeon_길동무_release.apk

# 방법 B — 소스에서 빌드 (JDK 는 Gradle 툴체인이 자동으로 받음)
./gradlew installDebug          # Windows: gradlew.bat installDebug
```

### 2.3 Virtual Console (차량 상태 조작)
```bash
adb forward tcp:8088 tcp:8088   # 브라우저로 http://127.0.0.1:8088/ 열기
```
- **Motion > Gear Selection**: P ↔ D 로 정차/주행 전환 → 길동무의 화면 게임/음성 모드가 2초 안에 바뀝니다.
- **Motion > Brake Pedal**: 신호대기(STOPPED) 보조 신호
- **Convenience > Seat Occupancy**: 동승자 수 자동 제안

### 2.4 마이크 (실제 음성 응답을 쓸 때)
에뮬레이터 **Extended Controls(⋯) > Microphone > "Virtual microphone uses host audio input"** 를 켭니다.
마이크 없이 시연할 때는 앱의 디버그 패널에서 **응답 입력 = 대본 normal / 대본 sleepy** 를 고르면 됩니다 (TTS 는 실제 Pleos 음성 그대로).

### 2.5 앱 사용 순서
1. 첫 실행 시 권한 2개 허용: **차량 속도**(`android.car.permission.CAR_SPEED`), **차량 위치**(`pleos.car.permission.FUSED_LOCATION`)
2. 홈에서 목적지(프리셋 또는 "내비 목적지") · 운전자 · 동승자 수 · 게임을 고르고 **출발**
3. Virtual Console 에서 기어 **D** → 음성 게임 시작 / 기어 **P** → 판을 마무리하고 화면 게임(초성 퀴즈)
4. 판은 여정 전체에 나눠 둡니다(4시간 10분 → 약 49분마다 10분짜리 한 판, 모두 5판). 판 사이 **쉬는 시간**에는 말을 걸지 않고 "다음 판은 약 N분 뒤에 시작해요"를 보여 줍니다. 바로 하려면 **지금 시작**.
5. **게임 끝내기**: 음성 화면에서는 일시정지·쉬는 시간에, 정차 중 화면 게임에서는 상단에 있습니다(문제를 내고 듣는 중에는 운전 중 조작을 줄이려고 **일시정지** 하나만 보임). 맞힌 문제나 마친 판이 있으면 "오늘의 길동무 · 여기까지 했어요" 요약 → **처음으로**, 없으면 바로 홈으로 갑니다. 내비 길 안내는 취소하지 않습니다.
6. 제목 **"길동무"를 길게 누르면 디버그 패널**(DEV): 입력 소스 강제 선택, 시뮬레이션 기어·속도·지역·남은 시간, 대본, 졸음 지표, 최근 로그

3분 시연 대본은 [`docs/DEMO_SCENARIO.md`](docs/DEMO_SCENARIO.md) 를 보세요.

---

## 3. 테스트 환경 및 필요한 설정

| 항목 | 값 |
|---|---|
| 에뮬레이터 | Pleos Connect Emulator v2.1.0 (`Pleos_Connect_v2.1.0`, Android 14 / API 34 Automotive, 2560×1440, cold boot) |
| 빌드 | Gradle 9.6.0 · AGP 9.5.0 (built-in Kotlin 2.2.10) · compileSdk 37 · Java 11 target |
| UI | Jetpack Compose (BOM 2025.09.00) + Material3, 다크 테마 |
| 런타임 권한 | `android.car.permission.CAR_SPEED`(dangerous), `pleos.car.permission.FUSED_LOCATION`(dangerous), `RECORD_AUDIO`(Android 폴백 인식기가 선택될 때만 요청) |
| `local.properties` (선택) | `PLEOS_CLIENT_ID`, `PLEOS_CLIENT_SECRET` — 있으면 TTS/STT 가 `Mode.HYBRID`, 없으면 `Mode.ON_DEVICE` (이 제출본은 **ON_DEVICE**) |
| release 서명 (선택) | `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` — 없으면 debug 키로 서명 |

### 3.1 실제 SDK vs 시뮬레이션 (정직한 표)

| 기능 | Pleos Connect Emulator 에서 | 비고 |
|---|---|---|
| 기어·속도·주차브레이크 (AAOS `CarPropertyManager`) | ✅ **실제** | Virtual Console 기어 P/D·브레이크·속도 변화가 콜백으로 수신됨 |
| 주행 규제 (`CarUxRestrictionsManager`) | ✅ **실제** | 기어 D 에서 `requiresDO=true, active=80(NO_VIDEO+NO_SETUP)` 수신 |
| 브레이크 페달 · 좌석 착좌 (Pleos Vehicle SDK) | ✅ **실제** | `checkXxxCapabilities` → `register`/`getCurrent` |
| 경로 요청·남은 시간·TBT (NaviHelper) | ✅ **실제** (요청·이벤트) / ⚠️ 진행은 시뮬 | `requestRoute` → `onRouteStarted(14820s, 378km)`·`onDestinationInfo`·`onTBTInfo` 수신. 에뮬레이터 차량이 움직이지 않아 남은 시간이 줄지 않으므로 **시연은 시뮬레이션 여정(시간 압축 ×30)** 사용 |
| 현재 위치 (FusedLocation) | ✅ **실제** (값은 에뮬레이터 기본 GPS) | 기본 좌표가 미국이라 국내 지역 판정 불가 → NaviHelper 주소("수정구 시흥동" → 경기) → 시뮬 경로 순으로 폴백 |
| 음성 출력 (Pleos TTS) | ✅ **실제** (`ON_DEVICE`) | 발화 완료(onDone) 확인, 오디오 포커스 충돌 없음 |
| 음성 인식 (Pleos STT) | ✅ 초기화·청취 실제 / ⚠️ 응답은 마이크 필요 | 호스트 마이크를 켜지 않으면 8초 무응답 처리 → 시연은 **대본 응답**(ScriptedSpeechEngine) 사용 |
| `driving_regulation on/off` 셸 명령 | ❌ 없음 | 이 사용자 빌드 에뮬레이터에는 `hmg_car_service` 가 없음 → 규제는 기어 D 로 검증 |

### 3.2 알려진 제약
- 내비 경로가 있어도 에뮬레이터 차량이 움직이지 않으면 남은 시간·TBT 가 그대로입니다. 같은 TBT 안내가 20초 넘게 그대로면 정체/정차로 보고 일시정지를 풉니다.
- 경로 요청(`requestRoute`) 시 순정 내비 앱이 같은 창의 앞으로 나옵니다. 길동무로 돌아오면 이어서 진행됩니다(발화가 끊겼다면 "계속").
- Vehicle SDK 2.3.2 AAR 은 `VehicleException` 클래스가 난독화되어 있어 문서의 `errorCode` 분기 대신 메시지 기반 재시도를 씁니다.

---

## 4. Pleos SDK 활용 (기획서 "기술 활용성" 표와 동일)

| SDK | 활용 | 호출 위치 |
|---|---|---|
| **Vehicle** (+ AAOS Car API) | 주행·정차 상태로 음성/화면 모드 전환, 운전 집중이 필요한 순간 게임 일시정지 | `core/car/AaosCarStateSource.kt` (GEAR_SELECTION·PERF_VEHICLE_SPEED·PARKING_BRAKE_ON, CarUxRestrictions) · `core/car/VehicleSdkClient.kt` (Brake·Seat) · `domain/engine/ModeArbiter.kt` |
| **NaviHelper** | 남은 시간·경로로 게임 길이 조절, 안내 시점(TBT)에 맞춰 일시정지 · (2차) 졸음쉼터 길 안내 | `core/navi/NaviHelperTripSource.kt` · `domain/engine/SessionPlanner.kt` · `app/GameCoordinator.kt#observeTbt` |
| **FusedLocation** | 현재 위치로 지역 퀴즈 선택, 지나온 지역 기록 · (2차) 졸음쉼터 탐색 | `core/location/FusedLocationSource.kt` · `core/location/RegionResolver.kt` |
| **SpeechToText** | 운전자·동승자의 답변 인식, 응답에 걸린 시간 측정 | `core/speech/PleosSpeechEngine.kt#listen` · `domain/engine/AnswerCollector.kt` |
| **TextToSpeech** | 문제 출제, 결과 안내, 반응 저하 시 말 걸기 | `core/speech/PleosSpeechEngine.kt#speak` · `domain/engine/GameSession.kt` |

매니페스트 퍼미션은 실제로 호출하는 API 의 것만 선언했습니다: `pleos.car.permission.CAR_DRIVE, CAR_SEATS, NAVI_ROUTE, NAVI_ROUTE_SEARCH, TTS_SERVICE, STT_SERVICE, FUSED_LOCATION`, `android.car.permission.CAR_POWERTRAIN, CAR_SPEED`, `android.permission.RECORD_AUDIO`(폴백). `INTERNET`·`*_BINDING` 은 Pleos SDK AAR 이 병합하는 것으로, 앱 자체의 네트워크 호출은 없습니다(콘텐츠는 모두 assets).

---

## 5. 아키텍처

```
com.gildongmu.app
├─ app/        GildongmuApp · AppContainer(수동 DI) · MainActivity(단일 액티비티) · GameCoordinator
├─ core/
│  ├─ car/       CarStateSource · AaosCarStateSource · VehicleSdkClient · SimulatedCarStateSource · CarStateRepository
│  ├─ navi/      TripSource · NaviHelperTripSource · SimulatedTripSource · TripRepository · TripUnits · DestinationPreset
│  ├─ location/  FusedLocationSource · RegionResolver · LocationRepository(Fused → 내비 주소 → 시뮬)
│  ├─ speech/    SpeechEngine · PleosSpeechEngine · AndroidSpeechEngine · ScriptedSpeechEngine · SpeechRouter · AudioFocusHelper
│  └─ log/       AppLog(PII 마스킹, 디버그 패널 링버퍼)
├─ domain/
│  ├─ model/     DriveState · UxState · GameMode · PauseReason · TripContext · SessionPlan · Player · Question …
│  ├─ engine/    DriveStateDeriver · ModeArbiter · ModeController · SessionPlanner · GameSession · AnswerMatcher · AnswerCollector
│  ├─ games/     VoiceGame · RegionQuizGame · WordChainGame · ChosungQuizGame · WordDictionary · Hangul
│  └─ drowsy/    DrowsinessMonitor (1단계)
├─ data/       ContentRepository(assets) · ContentParser · AppPreferences(DataStore) · SnapshotCodec
└─ ui/         theme · home · voice · screen · summary · debug · common
```
입력 소스(차량·여정·위치)와 음성 엔진은 **실제 SDK → 폴백 → 시뮬레이션** 순으로 자동 선택되고, 선택 결과는 상단 배지·디버그 패널·logcat 에 보입니다.

### 상태 머신 요약
```
DriveState  PARKED(기어 P) · STOPPED(저속 3초 또는 속도0+브레이크) · DRIVING · UNKNOWN(→ 안전하게 VOICE)
GameMode    IDLE(세션 없음) · VOICE(주행/정차/DO) · SCREEN(주차 && !requiresDO) · PAUSED(길 안내·통화·사용자·백그라운드)
규칙  1) requiresDO 면 SCREEN 금지(즉시)  2) 주행 상태 1.5초 디바운스  3) PAUSED 진입 시 TTS/STT 즉시 중단, 재개 시 같은 문제
      4) SCREEN→VOICE 즉시, VOICE→SCREEN 은 음성 판 마무리 후  5) 전환 로그 "ModeArbiter: DRIVING+DO → VOICE (reason=…)"
세션  남은 시간 → 판 수(약 45분마다 1판, 판 6~10분, 휴지 ≥2분, 도착 5분 전 종료, 12분 미만 짧은 판), ±20% 변하면 남은 판 재계산
졸음  운전자 응답 지연(TTS 끝→첫 인식) 기준값 EMA, 최근 5개 중 3개 느림 → 말 걸기 + 난이도 -1 + 쿨다운 5분 (보조 기능)
```

---

## 6. 품질 확인
```bash
./gradlew lintDebug testDebugUnitTest assembleRelease
```
- 단위 테스트 102개 (ModeArbiter·DriveStateDeriver·SessionPlanner·AnswerMatcher·두음법칙·끝말잇기·지역 판정·콘텐츠 자산 검증·졸음 모니터·저장본 코덱·GameSession 통합)
- 주행 중 CPU 0~0.5%, 메모리 PSS 약 50 MB (에뮬레이터 `top`/`dumpsys meminfo` 측정)

## 7. 문서
- [`docs/DEMO_SCENARIO.md`](docs/DEMO_SCENARIO.md) — 3분 시연 대본
- [`docs/PHASE2_NOTES.md`](docs/PHASE2_NOTES.md) — 2차(12/3 PT) 설계 메모
- [`docs/CONTENT_SOURCES.md`](docs/CONTENT_SOURCES.md) — 퀴즈·사전 콘텐츠 출처와 생성 방식

## 8. release 서명 키 만들기 (직접 빌드할 때)
```bash
keytool -genkeypair -v -keystore <리포 밖 경로>/gildongmu-release.jks -storetype PKCS12 \
  -alias gildongmu -keyalg RSA -keysize 2048 -validity 10000
```
`local.properties` 에 `RELEASE_STORE_FILE`(슬래시 경로), `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` 를 적고 `./gradlew assembleRelease`. 키 파일과 비밀번호는 절대 커밋하지 마세요.
