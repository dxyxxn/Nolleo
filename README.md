# 길동무 (Gildongmu)

**가는 길이 놀이가 되는 차량용 가족 레크리에이션 서비스**

차가 이미 알고 있는 **주행 상태 · 남은 시간 · 현재 위치**를 게임에 연결합니다. 주행 중에는 음성으로, 정차 중에는 화면으로 즐기고, 게임 응답에서 운전자의 반응 저하를 살펴 가볍게 말을 걸어 줍니다. 반응이 계속 느려지면 내비가 알려 준 **경로 위 휴게소·졸음쉼터** 중 가까운 곳을 수면실·수유실·가족사랑 화장실 정보와 함께 권하고, 도착하면 쉬었다 가도록 게임을 멈춥니다.

기능별 설명과 에뮬레이터에서 하나씩 확인하는 방법은 [`docs/FEATURE_GUIDE.md`](docs/FEATURE_GUIDE.md)(사용설명서)에 있습니다.

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
   - 길동무를 켜 둔 채 **내비 앱에서 길 안내를 시작**해도 홈에 "내비 목적지: ○○ · 남은 시간 …" 카드가 바로 생기고 자동으로 선택됩니다(디버그 패널 여정이 **시뮬**이면 카드만 보이고 자동 선택하지 않음 — 시연용 시뮬 여정 보호). 안내를 끝내면 카드가 사라지고 프리셋으로 돌아갑니다. 목적지·경유지·경로 옵션을 바꾸거나 재탐색하면 남은 시간도 바로 다시 받습니다.
   - **3분할**(왼쪽 Driving view + 앱 2개): 화면 오른쪽 끝 창 막대(⋮)를 왼쪽으로 끌면 내비와 나란히 놓입니다. 길동무가 운전석 쪽 창에 들어가면 스스로 **맨 오른쪽(조수석 쪽)** 으로 옮겨 Driving view | 내비 | 길동무 가 됩니다. 사용자가 창 막대를 탭해 직접 바꾼 위치는 그대로 둡니다.
3. Virtual Console 에서 기어 **D** → 음성 게임 시작 / 기어 **P** → 판을 마무리하고 화면 게임(초성 퀴즈)
4. 판은 여정 전체에 나눠 둡니다. 홈의 **"얼마나 자주 할까요?"** 에서 간격을 고릅니다 — **여유 있게**(기본, 약 45분마다: 4시간 10분 → 5판, 판 사이 약 39분) · **자주**(약 20분마다: 12판, 판 사이 약 10분) · **쉬지 않고**(판 사이 2분: 20판). 고른 간격은 다음 실행 때도 유지되고, 미리보기에 "판 사이 약 N분 쉬어요"가 나옵니다. 판 사이 **쉬는 시간**에는 말을 걸지 않고 "다음 판은 약 N분 뒤에 시작해요"를 보여 줍니다. 바로 하려면 **지금 시작**.
   - 길동무가 화면에서 사라지면(내비가 앱 영역 전체를 덮는 등) 게임은 **일시정지**합니다. 다시 보이면 "계속"으로 같은 문제부터 이어 갑니다(DEV_GUIDE §5.4 백그라운드 정책에 맞춘 결정).
5. **졸음 케어**(보조 기능 — 졸음을 단정하지 않음): 운전자 답이 평소보다 느려지면 **1단계** "운전 오래 하셨죠? 다음 문제는 조금 쉬운 걸로…"(난이도 −1). 15분 안에 1단계가 다시 걸리거나 느린 답과 정답률 50% 미만이 겹치면 **2단계** — 내비 길 안내 목록(TBT)에 있는 **경로 위 휴게소·졸음쉼터** 중 가장 가까운 곳(휴게소가 5 km 안쪽으로만 더 멀면 휴게소)을 골라 "아빠, 반응이 계속 느려졌어요. 가는 길에 있는 기흥휴게소가 6킬로미터 앞이에요. 수유실과 가족사랑 화장실이 있어요. 거기서 잠깐 쉬어 갈까요?"라고 묻습니다. "네"(음성) 또는 화면 **쉬어 가기**를 누르면 도착할 때까지 게임은 쉽니다(경로 위라 경유지를 넣지 않음 — 내비가 이미 지나가는 곳). 근처에서 차를 세우거나 주차하면 도착 — "도착했어요. 차를 세우고 충분히 쉬었다 가요." 뒤 **쉼터에서 쉬는 중**으로 멈춰 있다가, 다시 출발하면 "잘 쉬셨나요? 다시 출발해요!"와 함께 이어 갑니다. 길 안내가 없을 때만 공공데이터 졸음쉼터 253곳 중 진행 방향 앞쪽 가장 가까운 곳을 고릅니다.
6. **게임 끝내기**: 음성 화면에서는 일시정지·쉬는 시간에, 정차 중 화면 게임에서는 상단에 있습니다(문제를 내고 듣는 중에는 운전 중 조작을 줄이려고 **일시정지** 하나만 보임). 맞힌 문제나 마친 판이 있으면 "오늘의 길동무 · 여기까지 했어요" 요약 → **처음으로**, 없으면 바로 홈으로 갑니다. 내비 길 안내는 취소하지 않습니다.
7. 제목 **"길동무"를 길게 누르면 디버그 패널**(DEV): 입력 소스 강제 선택, 시뮬레이션 기어·속도·지역·남은 시간, 대본, 졸음 지표·경로 위 휴게소·졸음쉼터와 "지금 걸리면 권할 곳" 미리보기·2단계 시험, 최근 로그 — 사용법은 [`docs/FEATURE_GUIDE.md`](docs/FEATURE_GUIDE.md) §11

3분 시연 대본은 [`docs/DEMO_SCENARIO.md`](docs/DEMO_SCENARIO.md), 기능별 확인 방법은 [`docs/FEATURE_GUIDE.md`](docs/FEATURE_GUIDE.md) 를 보세요.

---

## 3. 테스트 환경 및 필요한 설정

| 항목 | 값 |
|---|---|
| 에뮬레이터 | Pleos Connect Emulator v2.1.0 (`Pleos_Connect_v2.1.0`, Android 14 / API 34 Automotive, 2560×1440, cold boot) |
| 빌드 | Gradle 9.6.0 · AGP 9.5.0 (built-in Kotlin 2.2.10) · compileSdk 37 · Java 11 target |
| UI | Jetpack Compose (BOM 2025.09.00) + Material3, 다크 테마 |
| 런타임 권한 | `android.car.permission.CAR_SPEED`(dangerous), `pleos.car.permission.FUSED_LOCATION`(dangerous, `FusedLocation.isSupported()` 가 true 일 때만 요청), `RECORD_AUDIO`(Android 폴백 인식기가 선택될 때만 요청) |
| `local.properties` (선택) | `PLEOS_CLIENT_ID`, `PLEOS_CLIENT_SECRET` — 있으면 TTS/STT 가 `Mode.HYBRID`, 없으면 `Mode.ON_DEVICE` (이 제출본은 **ON_DEVICE**) |
| release 서명 (선택) | `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` — 없으면 debug 키로 서명 |

### 3.1 실제 SDK vs 시뮬레이션 (정직한 표)

> **TBT (Turn-By-Turn)**: 내비가 다음 안내(좌회전·IC 진입·톨게이트·휴게소 진입·졸음쉼터 …)를 순서대로 알려 주는 목록. NaviHelper `onTBTInfo` 로 받으며, 항목의 `distance` 는 바로 앞 항목부터의 거리다. 길동무는 회전 직전 자동 일시정지와 경로 위 휴게소·졸음쉼터 찾기에 쓴다. 용어 설명은 [`docs/FEATURE_GUIDE.md`](docs/FEATURE_GUIDE.md) §0.7.

| 기능 | Pleos Connect Emulator 에서 | 비고 |
|---|---|---|
| 기어·속도·주차브레이크 (AAOS `CarPropertyManager`) | ✅ **실제** | Virtual Console 기어 P/D·브레이크·속도 변화가 콜백으로 수신됨 |
| 주행 규제 (`CarUxRestrictionsManager`) | ✅ **실제** | 기어 D 에서 `requiresDO=true, active=80(NO_VIDEO+NO_SETUP)` 수신 |
| 브레이크 페달 · 좌석 착좌 (Pleos Vehicle SDK) | ✅ **실제** | `checkXxxCapabilities` → `register`/`getCurrent` |
| 경로 요청·남은 시간·TBT (NaviHelper) | ✅ **실제** (요청·이벤트) / ⚠️ 진행은 시뮬 | `requestRoute` → `onRouteStarted(14820s, 378km)`·`onDestinationInfo`·`onTBTInfo` 수신. 내비 앱에서 직접 시작·종료한 길 안내도 `onRouteStarted`·`onNavigationStatus`·`onRouteCancelled` 로 즉시 반영(→ `getDestinationInfo`, 길 안내 중 30초마다 재조회). 에뮬레이터 차량이 움직이지 않아 남은 시간이 줄지 않으므로 **시연은 시뮬레이션 여정(시간 압축 ×30)** 사용 |
| 경로 위 휴게소·졸음쉼터 (NaviHelper `onTBTInfo` `TURN_ENTER_SERVICEAREA`·`TURN_RESTAREA`) | ✅ **실제** | 에뮬레이터 내비는 고속도로 경로 TBT 에 휴게소·졸음쉼터를 경로 순서대로 넣어 준다(부산역 경로 22곳: 용인졸음쉼터 18.1 km · 용인휴게소 28.5 km · 덕평휴게소 41.2 km …). `distance` 는 **앞 항목부터의 구간 거리**라 누적해서 쓴다(11/7 확인 — 그대로 쓰면 용인졸음쉼터를 3.8 km 로 잘못 말함). 이후 남은 거리가 준 만큼 앞당긴다 |
| 쉼터 도착 감지 (FusedLocation `addProximityAlert`) | ✅ **실제** | 졸음쉼터는 이름으로 공공데이터 표와 맞춰 좌표를 얻고 반경 300 m ENTER 등록 → `adb emu geo fix <경도> <위도>`(디버그 패널 "쉬어 가기 상태" 줄에 나옴)로 옮기면 도착(11/7 확인). 옆을 지나가기만 하면 도착이 아니고 **근처에서 서거나 주차**해야 도착. 좌표가 없는 휴게소는 주차하면 도착 |
| 좌표 경유지 (NaviHelper `addWaypoint`) | ⛔ **쓰지 않음** | 11/7 시험: 공공데이터에는 POI ID 가 없어 좌표로 넣으면 내비가 고속도로 옆 일반 도로에 붙여 **오산IC 로 나갔다가 시내를 돌아 다시 들어오는 우회 경로**를 만들었다(쉼터가 이미 경로 위인데도). 경로 위 쉼 장소만 권하므로 경유지가 필요 없다 |
| 현재 위치 (FusedLocation) | ✅ **실제** | 에뮬레이터 기본 좌표로는 국내 지역이 안 나와 NaviHelper 주소("수정구 시흥동" → 경기) → 시뮬 경로 순으로 폴백. `adb emu geo fix 126.9780 37.5665`(경도 먼저)로 국내 좌표를 넣으면 FusedLocation 으로 판정(확인: `region=seoul via FusedLocation`). 시뮬 여정 중에는 시뮬 경로의 지역이 우선 |
| 음성 출력 (Pleos TTS) | ✅ **실제** (`ON_DEVICE`) | 발화 완료(onDone) 확인, 오디오 포커스 충돌 없음 |
| 음성 인식 (Pleos STT) | ✅ **실제** (호스트 마이크 필요) | 에뮬레이터 Microphone 을 호스트 입력으로 켜고 응답 입력 "마이크"로 실제 인식 확인. 마이크를 켜지 않으면 8초 무응답 처리 → 마이크 없이 시연할 때는 **대본 응답**(ScriptedSpeechEngine) 사용 |
| `driving_regulation on/off` 셸 명령 | ❌ 없음 | 이 사용자 빌드 에뮬레이터에는 `hmg_car_service` 가 없음 → 규제는 기어 D 로 검증 |

### 3.2 알려진 제약
- 내비 경로가 있어도 에뮬레이터 차량이 움직이지 않으면 남은 시간·TBT 가 그대로입니다. 같은 TBT 안내가 20초 넘게 그대로면 정체/정차로 보고 일시정지를 풉니다.
- 내비에 이미 길 안내가 있는데 다른 목적지로 출발하면, 내비가 이전 경로를 먼저 취소하며 남은 시간 0/0 을 보냅니다. 이 값은 "도착"으로 쓰지 않습니다(11/7 수정 — 전에는 출발하자마자 요약 화면으로 끝날 수 있었음).
- 에뮬레이터 내비의 차 위치는 성남 수정구 부근이라 **근처 10분** 프리셋은 실제 내비로 요청하면 남은 1분으로 바로 도착 처리됩니다(시뮬 여정에서는 10분).
- 경로 요청(`requestRoute`) 시 순정 내비 앱이 같은 창의 앞으로 나옵니다. 길동무로 돌아오면 이어서 진행됩니다(발화가 끊겼다면 "계속").
- 앱은 창을 직접 나누거나 옆 창을 지정할 수 없습니다(Pleos 창 정책 §6.3, `FLAG_ACTIVITY_LAUNCH_ADJACENT` 무시 확인). 3분할은 사용자가 창 막대로 만들고, 길동무는 그 상태에서 **기본 지도 앱(`CATEGORY_APP_MAPS`)을 앞으로 불러** 자리를 바꿉니다(새로 앞에 나온 앱이 운전석 쪽 primary 창으로 가는 규칙). NaviHelper `requestNavigationScreen(Foreground)` 는 내비가 이미 보이면 다시 실행되지 않아 자리가 바뀌지 않으므로 쓰지 않습니다. 좌핸들 배치(Driving view 왼쪽)에서만 동작합니다.
- Vehicle SDK 2.3.2 AAR 은 `VehicleException` 클래스가 난독화되어 있어 문서의 `errorCode` 분기 대신 메시지 기반 재시도를 씁니다.
- 쉬어 갈 곳은 내비 TBT 목록에서 고르므로 **길 안내 중일 때** 정확합니다. 에뮬레이터 내비의 TBT 목록은 50여 항목까지라 아주 긴 경로는 뒷부분이 빠질 수 있습니다(가까운 곳만 쓰므로 문제없음). 실차에서 주행 중 TBT 를 얼마나 자주 보내는지는 미확인 — 그 사이는 남은 거리 변화로 앞당겨 계산합니다.
- 길 안내가 없을 때 쓰는 졸음쉼터 표(공공데이터)는 **어느 도로를 달리는지 모른 채** 위치·진행 방향만으로 고르는 근사라, 노선 이름("경부선 부산 방향")을 함께 말해 운전자가 거절할 수 있게 했습니다. 원자료의 "기점 + 종점" 방향 표기는 기점 → 종점 차로로 해석했습니다(공식 정의를 찾지 못함).
- 휴게소 편의시설은 좌표가 없는 자료라 TBT 의 휴게소 이름과 **정확히 같은 이름**일 때만 붙입니다(민자 고속도로 휴게소 등 일부는 자료에 없음). 상·하행은 진행 방향으로 고르고, 모르면 양쪽에 공통인 시설만 말합니다.
- 실제 내비에서 권한 곳을 지나쳐도(서지 않고) 게임은 계속 쉬고 있습니다 — "계속"·"안내 그만두기"로 이어 갑니다.

---

## 4. Pleos SDK 활용 (기획서 "기술 활용성" 표와 동일)

| SDK | 활용 | 호출 위치 |
|---|---|---|
| **Vehicle** (+ AAOS Car API) | 주행·정차 상태로 음성/화면 모드 전환, 운전 집중이 필요한 순간 게임 일시정지 | `core/car/AaosCarStateSource.kt` (GEAR_SELECTION·PERF_VEHICLE_SPEED·PARKING_BRAKE_ON, CarUxRestrictions) · `core/car/VehicleSdkClient.kt` (Brake·Seat) · `domain/engine/ModeArbiter.kt` |
| **NaviHelper** | 남은 시간·경로로 게임 길이 조절, 내비에서 시작한 길 안내를 홈에 실시간 반영, 안내 시점(TBT)에 맞춰 일시정지, 졸음 케어 2단계: **TBT 목록의 경로 위 휴게소·졸음쉼터**(`TURN_ENTER_SERVICEAREA`·`TURN_RESTAREA`, 구간 거리 누적)에서 쉬어 갈 곳 고르기 · 경로가 바뀌면 다시 조회(`getTBTInfo`) | `core/navi/NaviHelperTripSource.kt` · `core/navi/TripRepository.kt` · `domain/engine/SessionPlanner.kt` · `app/GameCoordinator.kt#observeTbt` · `domain/rest/RestStop.kt` |
| **FusedLocation** | 현재 위치로 지역 퀴즈 선택, 지나온 지역 기록, 졸음 케어 2단계: 진행 방향으로 상·하행 가르기(휴게소 편의시설·졸음쉼터 좌표), 쉼터 **도착 감지**(`addProximityAlert` 반경 300 m ENTER + 서거나 주차), 길 안내가 없을 때 앞쪽 졸음쉼터 탐색 | `core/location/FusedLocationSource.kt` · `core/location/RegionResolver.kt` · `domain/rest/RestStop.kt` · `app/RestCareController.kt` |
| **SpeechToText** | 운전자·동승자의 답변 인식, 응답에 걸린 시간 측정, 쉬어 가기 질문의 "네/아니요" | `core/speech/PleosSpeechEngine.kt#listen` · `domain/engine/AnswerCollector.kt` · `domain/engine/YesNo.kt` |
| **TextToSpeech** | 문제 출제, 결과 안내, 반응 저하 시 말 걸기, 휴게소·졸음쉼터 권유(편의시설 포함)·도착·다시 출발 안내 | `core/speech/PleosSpeechEngine.kt#speak` · `domain/engine/GameSession.kt` |

매니페스트 퍼미션은 실제로 호출하는 API 의 것만 선언했습니다: `pleos.car.permission.CAR_DRIVE, CAR_SEATS, NAVI_ROUTE, NAVI_ROUTE_SEARCH, TTS_SERVICE, STT_SERVICE, FUSED_LOCATION`, `android.car.permission.CAR_POWERTRAIN, CAR_SPEED`, `android.permission.RECORD_AUDIO`(폴백). 졸음 케어 2단계의 TBT 목록(`getTBTInfo`)은 `NAVI_ROUTE`, 근접 알림(`addProximityAlert`)은 `FUSED_LOCATION` 으로 동작해 퍼미션을 새로 늘리지 않았습니다. `INTERNET`·`*_BINDING` 은 Pleos SDK AAR 이 병합하는 것으로, 앱 자체의 네트워크 호출은 없습니다(퀴즈·사전·졸음쉼터·휴게소 자료 모두 assets).

---

## 5. 아키텍처

```
com.gildongmu.app
├─ app/        GildongmuApp · AppContainer(수동 DI) · MainActivity(단일 액티비티) · GameCoordinator · RestCareController(쉬어 가기) · WindowPlacement(3분할 창 위치)
├─ core/
│  ├─ car/       CarStateSource · AaosCarStateSource · VehicleSdkClient · SimulatedCarStateSource · CarStateRepository
│  ├─ navi/      TripSource · NaviHelperTripSource · SimulatedTripSource · TripRepository · TripUnits · DestinationPreset
│  ├─ location/  FusedLocationSource · RegionResolver · LocationRepository(Fused → 내비 주소 → 시뮬)
│  ├─ speech/    SpeechEngine · PleosSpeechEngine · AndroidSpeechEngine · ScriptedSpeechEngine · SpeechRouter · AudioFocusHelper
│  └─ log/       AppLog(PII 마스킹, 디버그 패널 링버퍼)
├─ domain/
│  ├─ model/     DriveState · UxState · GameMode · PauseReason · TripContext · SessionPlan · Player · Question · GeoMath …
│  ├─ engine/    DriveStateDeriver · ModeArbiter · ModeController · SessionPlanner · GameSession · AnswerMatcher · AnswerCollector · YesNo
│  ├─ games/     VoiceGame · RegionQuizGame · WordChainGame · ChosungQuizGame · WordDictionary · Hangul
│  ├─ drowsy/    DrowsinessMonitor (1단계 말 걸기 · 2단계 쉬어 가기)
│  └─ rest/      RouteRestStops(TBT → 경로 위 쉼 장소) · RestStopFinder(고르기·상하행 맞추기) · RestArea · ServiceAreaInfo · MotionHeading
├─ data/       ContentRepository(assets) · ContentParser · AppPreferences(DataStore) · SnapshotCodec
tools/         convert_rest_areas.js (졸음쉼터 CSV → rest_areas.json) · convert_service_areas.js (휴게소 편의시설 → service_areas.json)
└─ ui/         theme · home · voice · screen · summary · debug · common
```
입력 소스(차량·여정·위치)와 음성 엔진은 **실제 SDK → 폴백 → 시뮬레이션** 순으로 자동 선택되고, 선택 결과는 상단 배지·디버그 패널·logcat 에 보입니다.

### 상태 머신 요약
```
DriveState  PARKED(기어 P) · STOPPED(저속 3초 또는 속도0+브레이크) · DRIVING · UNKNOWN(→ 안전하게 VOICE)
GameMode    IDLE(세션 없음) · VOICE(주행/정차/DO) · SCREEN(주차 && !requiresDO) · PAUSED(길 안내·통화·사용자·백그라운드)
규칙  1) requiresDO 면 SCREEN 금지(즉시)  2) 주행 상태 1.5초 디바운스  3) PAUSED 진입 시 TTS/STT 즉시 중단, 재개 시 같은 문제
      4) SCREEN→VOICE 즉시, VOICE→SCREEN 은 음성 판 마무리 후  5) 전환 로그 "ModeArbiter: DRIVING+DO → VOICE (reason=…)"
세션  남은 시간 + 판 간격(여유 있게 45분 · 자주 20분 · 쉬지 않고 휴지 2분) → 판 수(판 6~10분, 휴지 ≥2분, 도착 5분 전 종료,
      12분 미만 짧은 판), ±20% 변하면 같은 간격으로 남은 판 재계산
졸음  운전자 응답 지연(TTS 끝→첫 인식) 기준값 EMA, 최근 5개 중 3개 느림 → 1단계: 말 걸기 + 난이도 -1 + 쿨다운 5분 (보조 기능)
      15분 안에 1단계 두 번 또는 (느린 답 && 최근 5개 정답률 < 50%) → 2단계: 쉬어 가기 질문(네/아니요, 화면 버튼) + 난이도 -1,
      쿨다운 20분 (데모 모드는 1단계 20초 · 2단계 60초)
쉼터  경로 정보(내비 TBT·시뮬 경로)가 있으면 경로 위에서만: 1~80 km 앞 가장 가까운 곳, 휴게소가 5 km 안쪽으로만 더 멀면 휴게소
      (TBT distance = 구간 거리 → 누적). 휴게소는 편의시설(수면실·수유실·가족사랑 화장실·대표 음식), 졸음쉼터는 표와 맞춰 좌표
      경로 정보가 없으면 표에서: 도로 방향 ±45°, 앞쪽 ±45°, 옆 거리 ≤ max(2 km, 25%), 0.3~80 km
      진행 방향 = 시뮬 경로 / 움직인 방향 → SDK heading → 목적지 쪽 (상·하행 가르기)
      수락 → PAUSED(REST_STOP) + (좌표 있으면) 근접 알림 300 m → 근처에서 서거나 주차 / 주차 / 시뮬이 그 지점에 닿음 = 도착
      → 안내 후 PAUSED(REST_AREA) → 주차했다가 다시 주행 → "잘 쉬셨나요?" + 응답 창·난이도 초기화
```

---

## 6. 품질 확인
```bash
./gradlew lintDebug testDebugUnitTest assembleRelease
```
- 단위 테스트 152개 (ModeArbiter·DriveStateDeriver·SessionPlanner(판 간격 3종 포함)·AnswerMatcher·두음법칙·끝말잇기·지역 판정·콘텐츠 자산 검증(졸음쉼터·휴게소 자료 포함)·졸음 모니터(1·2단계)·TBT 누적 거리·쉼 장소 고르기(휴게소 우선·상하행)·시뮬 경로 쉼 장소·네/아니요 판정·저장본 코덱·GameSession 통합(쉬어 가기 질문·도착 후 대기·다시 출발 인사)·내비 재조회 조건·내비 0/0 값 무시·홈 내비 목적지 선택·3분할 창 위치)
- 주행 중 CPU 0~0.5%, 메모리 PSS 약 50 MB (에뮬레이터 `top`/`dumpsys meminfo` 측정)

## 7. 문서
- [`docs/FEATURE_GUIDE.md`](docs/FEATURE_GUIDE.md) — 기능 사용설명서 (기능별로 에뮬레이터에서 확인하는 방법)
- [`docs/DEMO_SCENARIO.md`](docs/DEMO_SCENARIO.md) — 3분 시연 대본
- [`docs/PHASE2_NOTES.md`](docs/PHASE2_NOTES.md) — 2차(12/3 PT) 설계 메모
- [`docs/CONTENT_SOURCES.md`](docs/CONTENT_SOURCES.md) — 퀴즈·사전·졸음쉼터·휴게소 자료 출처와 생성 방식

## 8. release 서명 키 만들기 (직접 빌드할 때)
```bash
keytool -genkeypair -v -keystore <리포 밖 경로>/gildongmu-release.jks -storetype PKCS12 \
  -alias gildongmu -keyalg RSA -keysize 2048 -validity 10000
```
`local.properties` 에 `RELEASE_STORE_FILE`(슬래시 경로), `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` 를 적고 `./gradlew assembleRelease`. 키 파일과 비밀번호는 절대 커밋하지 마세요.
