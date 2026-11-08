# 놀레오 (Nolleo)

**가는 길이 놀이가 되는 가족 게임**

차가 이미 알고 있는 **주행 상태 · 남은 시간 · 현재 위치**를 게임에 연결합니다. 주행 중에는 음성으로 퀴즈·끝말잇기를, 정차 중에는 화면으로 사자성어(한자 풀이와 유래까지)·지역 퀴즈(지금 지나는 시/도)·끝말잇기(이어질 말 고르기)를 즐기고, 게임 응답에서 운전자의 반응 저하를 살펴 가볍게 말을 걸어 줍니다. 반응이 계속 느려지면 내비가 알려 준 **경로 위 휴게소·졸음쉼터** 중 가까운 곳을 수면실·수유실·가족사랑 화장실 정보와 함께 권하고, 도착하면 쉬었다 가도록 게임을 멈춥니다.

기능별 설명과 에뮬레이터에서 하나씩 확인하는 방법은 [`docs/FEATURE_GUIDE.md`](docs/FEATURE_GUIDE.md)(사용설명서)에 있습니다.

- 팀: **돈도아연** · 현대자동차그룹 차량용 앱 개발 공모전 2026 1차 제출용 프로토타입
- 패키지 `com.nolleo.app` · 버전 1.0.0 (versionCode 1) · minSdk 28 / targetSdk 37
- 시연 영상: _(링크 자리 — 녹화 후 추가)_

---

## 1. APK 위치

| 파일 | 설명 |
|---|---|
| [`apk/돈도아연_놀레오_release.apk`](apk/돈도아연_놀레오_release.apk) | 실행 가능한 **release** 빌드 (서명됨, `debuggable=false`, R8 최적화) · 약 24 MB |

---

## 2. 실행 방법

### 2.1 Pleos Connect Emulator 준비
1. Android Studio → SDK Manager에서 **Pleos Connect Emulator 2.1.0** (API 34 Automotive 이미지) 설치
2. AVD 생성: RAM **4 GB**, Boot option **Cold boot** (Quick boot면 VHAL을 쓸 수 없음)
3. AVD 실행 후 확인: `adb devices`

### 2.2 설치
```bash
# 방법 A — 저장소의 release APK
adb install apk/돈도아연_놀레오_release.apk

# 방법 B — 소스에서 빌드 (gradlew 실행에 JDK 17 이상 필요, 빌드용 JDK 는 Gradle 이 자동으로 받음)
./gradlew installDebug          # Windows: gradlew.bat installDebug
```

### 2.3 Virtual Console (차량 상태 조작)
```bash
adb forward tcp:8088 tcp:8088   # 브라우저로 http://127.0.0.1:8088/ 열기
```
- **Motion > Gear Selection**: D 로 바꾸면 바로 음성 모드, P 로 바꾸면 진행 중인 차례를 마친 뒤 정차 중 화면 게임 목록으로 바뀝니다.
- **Motion > Brake Pedal**: 신호대기(STOPPED) 보조 신호
- **Convenience > Seat Occupancy**: 동승자 수 자동 제안용(에뮬레이터는 착좌 값이 고정돼 늘 "3명"을 제안 — §3.1).

### 2.4 마이크 (실제 음성 응답을 쓸 때)
에뮬레이터 **Extended Controls(⋯) > Microphone > "Virtual microphone uses host audio input"** 를 켭니다.
마이크 없이 시연할 때는 앱의 디버그 패널에서 **응답 입력 = 대본 normal / 대본 sleepy** 를 고르면 됩니다 (TTS 는 실제 Pleos 음성 그대로).

### 2.5 앱 사용 순서
1. 첫 실행 시 권한 2개 허용: **차량 속도**(`android.car.permission.CAR_SPEED`), **차량 위치**(`pleos.car.permission.FUSED_LOCATION`)
2. **홈**: 목적지 · 운전자 · 함께 타는 사람 · 게임 · **판 간격**을 고르고 **출발**. 목적지는 내비 길 안내가 있으면 `현재 경로`, 없으면 내비 **최근 목적지** 3곳(내비가 알려 준 예상 시간과 함께 — 출발하면 그곳으로 길 안내를 겁니다), 그리고 늘 **목적지 없이**(30분 · 1시간 · 1시간 반 · 2시간). 판 간격은 여유 있게(기본, 4시간 10분 → 5판) · 자주(12판) · 쉬지 않고(20판). — 사용설명서 §3
3. 기어 **D** → 음성 게임(지역 퀴즈 · 끝말잇기), 기어 **P** → 정차 중 화면 게임 목록(**사자성어 · 지역 퀴즈 · 끝말잇기**, 틀린 그림 찾기 · 노래방은 준비 중). 화면 게임은 주행 중 음성 판과 따로 놉니다. — §4 · §5 · §7
4. 판 사이 쉬는 시간에는 말을 걸지 않습니다(바로 하려면 **지금 시작**). 길 안내(TBT) 구간에서는 저절로 멈췄다가 같은 문제부터 다시 하고, 놀레오가 화면에서 사라지면 일시정지합니다(읽던 중이었으면 돌아와서 **계속**). — §5.4 · §6 · §10
5. **졸음 케어**(보조 기능 — 졸음을 단정하지 않음): 운전자 답이 평소보다 느려지면 1단계 말 걸기와 쉬운 문제, 계속 느리면 2단계로 내비 TBT 의 **경로 위 휴게소·졸음쉼터** 중 가까운 곳을 편의시설과 함께 권합니다("아빠, 반응이 계속 느려졌어요. 가는 길에 있는 기흥휴게소가 6킬로미터 앞이에요. 수유실과 가족사랑 화장실이 있어요. 거기서 잠깐 쉬어 갈까요?"). 도착하면 쉬게 하고, 다시 출발하면 인사합니다. — §8
6. **게임 끝내기**: 음성 화면은 일시정지·쉬는 시간에, 정차 중에는 게임 목록 머리 줄에 있습니다. 점수가 있으면 요약 화면 → **처음으로**. 내비 길 안내는 취소하지 않습니다. — §9
7. **3분할**: 화면 오른쪽 끝 창 막대(⋮)를 끌어 내비와 나란히 놓으면, 놀레오가 스스로 맨 오른쪽(조수석 쪽)으로 옮겨 `Driving view | 내비 | 놀레오` 가 됩니다. — §10
8. 왼쪽 위 로고·제목 **"Nolleo"를 길게 누르면 디버그 패널**(DEV): 입력 소스 강제 선택, 시뮬레이션 기어·속도·지역·남은 시간, 대본 응답, 졸음 지표·쉬어 갈 곳 미리보기·2단계 시험, 최근 로그. — §11

위의 "§" 는 기능 사용설명서 [`docs/FEATURE_GUIDE.md`](docs/FEATURE_GUIDE.md) 의 절입니다(무엇을 누르고 무엇이 보이는지). 3분 시연 대본은 [`docs/DEMO_SCENARIO.md`](docs/DEMO_SCENARIO.md)에 있습니다.

---

## 3. 테스트 환경 및 필요한 설정

| 항목 | 값 |
|---|---|
| 에뮬레이터 | Pleos Connect Emulator v2.1.0 (`Pleos_Connect_v2.1.0`, Android 14 / API 34 Automotive, 2560×1440, cold boot) |
| 빌드 | Gradle 9.6.0 · AGP 9.5.0 (built-in Kotlin 2.2.10) · compileSdk 37 · Java 11 target |
| UI | Jetpack Compose (BOM 2025.09.00) + Material3 위에 자체 디자인 시스템(`ui/theme`) — 주간 연회색 바탕·흰 카드·검정 글 / 야간 차콜 바탕·흰 글(시스템 야간 모드를 따름), 강조색은 노랑 하나, 글꼴은 플랫폼 기본(Asta Sans) |
| 런타임 권한 | `android.car.permission.CAR_SPEED`(dangerous), `pleos.car.permission.FUSED_LOCATION`(dangerous, `FusedLocation.isSupported()` 가 true 일 때만 요청), `RECORD_AUDIO`(Android 폴백 인식기가 선택될 때만 요청) |
| `local.properties` (선택) | `PLEOS_CLIENT_ID`, `PLEOS_CLIENT_SECRET` — 있으면 TTS/STT 가 `Mode.HYBRID`, 없으면 `Mode.ON_DEVICE` (이 제출본은 **ON_DEVICE**) |
| release 서명 (선택) | `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` — 없으면 debug 키로 서명 |

### 3.1 실제 SDK vs 시뮬레이션 (정직한 표)

> **TBT (Turn-By-Turn)**: 내비가 다음 안내(좌회전·IC 진입·휴게소 진입·졸음쉼터 …)를 순서대로 알려 주는 목록(NaviHelper `onTBTInfo`). 자세한 설명은 [`docs/FEATURE_GUIDE.md`](docs/FEATURE_GUIDE.md) §0.7.

| 기능 | Pleos Connect Emulator 에서 | 비고 |
|---|---|---|
| 기어·속도·주차브레이크 (AAOS `CarPropertyManager`) | ✅ **실제** | Virtual Console 기어 P/D·브레이크·속도 변화가 콜백으로 수신됨 |
| 주행 규제 (`CarUxRestrictionsManager`) | ✅ **실제** | 기어 D 에서 `requiresDO=true, active=80(NO_VIDEO+NO_SETUP)` 수신 |
| 브레이크 페달 · 좌석 착좌 (Pleos Vehicle SDK) | ✅ **실제** (착좌 값은 고정) | `checkXxxCapabilities` → `register`/`getCurrent`. 에뮬레이터의 SDK 는 동승석 4곳(조수석·뒷좌석 왼쪽·가운데·오른쪽)을 늘 착좌로 보내고 Virtual Console 의 Seat Occupancy 를 바꿔도 값이 그대로입니다(앱을 다시 켜도 같음). 그래서 늘 "3명"(앱이 고를 수 있는 최대)을 제안합니다 |
| 경로 요청·남은 시간·TBT (NaviHelper) | ✅ **실제** (요청·이벤트) / ⚠️ 진행은 시뮬 | 최근 목적지로 출발하면 `requestRoute` → `onRouteStarted(14820s, 378km)`·`onDestinationInfo`·`onTBTInfo` 수신. 내비 앱에서 직접 시작·종료한 길 안내도 `onRouteStarted`·`onNavigationStatus`·`onRouteCancelled` 로 즉시 반영(→ `getDestinationInfo`, 길 안내 중 30초마다 재조회). 에뮬레이터 차량이 움직이지 않아 남은 시간이 줄지 않으므로 **시연은 시뮬레이션 여정(시간 압축 ×30)** 사용 |
| 최근 목적지·예상 시간 (NaviHelper `getRecentDestinationInfo`·`requestTrip`) | ✅ **실제** | 에뮬레이터 내비가 최근 목적지(앞서 길 안내했던 부산역·소프트웨어드림센터)를 보내고, `requestTrip` 에 길 안내 없이 `onTripInfo(378452 m, 15167 s)` 로 답함(11/8 확인). 응답은 다른 앱의 요청에도 모든 연결 앱에 오고 요청 번호가 없어 한 곳씩 묻고 목적지 좌표로 맞춘다. 집·회사 즐겨찾기(`getBookmarkInfo`)는 에뮬레이터에 등록돼 있지 않아(빈 이름·0 좌표) 쓰지 않았다 |
| 경로 위 휴게소·졸음쉼터 (NaviHelper `onTBTInfo` `TURN_ENTER_SERVICEAREA`·`TURN_RESTAREA`) | ✅ **실제** | 에뮬레이터 내비는 고속도로 경로 TBT 에 휴게소·졸음쉼터를 경로 순서대로 넣어 준다(부산역 경로 22곳: 용인졸음쉼터 18.1 km · 용인휴게소 28.5 km · 덕평휴게소 41.2 km …). `distance` 는 **앞 항목부터의 구간 거리**라 누적해서 쓴다. 이후 남은 거리가 준 만큼 앞당긴다 |
| 쉼터 도착 감지 (FusedLocation `addProximityAlert`) | ✅ **실제** | 졸음쉼터는 이름으로 공공데이터 표와 맞춰 좌표를 얻고 반경 300 m ENTER 등록 → `adb emu geo fix <경도> <위도>`(디버그 패널 "쉬어 가기 상태" 줄에 나옴)로 옮기면 도착. 옆을 지나가기만 하면 도착이 아니고 **근처에서 서거나 주차**해야 도착. 좌표가 없는 휴게소는 주차하면 도착 |
| 좌표 경유지 (NaviHelper `addWaypoint`) | ⛔ **쓰지 않음** | 에뮬레이터 시험: 공공데이터에는 POI ID 가 없어 좌표로 넣으면 내비가 고속도로 옆 일반 도로에 붙여 **오산IC 로 나갔다가 시내를 돌아 다시 들어오는 우회 경로**를 만들었다(쉼터가 이미 경로 위인데도). 경로 위 쉼 장소만 권하므로 경유지가 필요 없다 |
| 현재 위치 (FusedLocation) | ✅ **실제** | 에뮬레이터 기본 좌표로는 국내 지역이 안 나와 NaviHelper 주소("수정구 시흥동" → 경기) → 시뮬 경로 순으로 폴백. `adb emu geo fix 126.9780 37.5665`(경도 먼저)로 국내 좌표를 넣으면 FusedLocation 으로 판정(확인: `region=seoul via FusedLocation`). 시뮬 여정 중에는 시뮬 경로의 지역이 우선 |
| 음성 출력 (Pleos TTS) | ✅ **실제** (`ON_DEVICE`) | 발화 완료(onDone) 확인, 오디오 포커스 충돌 없음 |
| 음성 인식 (Pleos STT) | ✅ **실제** (호스트 마이크 필요) | 에뮬레이터 Microphone 을 호스트 입력으로 켜고 응답 입력 "마이크"로 실제 인식 확인. 마이크를 켜지 않으면 8초 무응답 처리 → 마이크 없이 시연할 때는 **대본 응답**(ScriptedSpeechEngine) 사용 |
| `driving_regulation on/off` 셸 명령 | ❌ 없음 | 이 사용자 빌드 에뮬레이터에는 `hmg_car_service` 가 없음 → 규제는 기어 D 로 검증 |

### 3.2 알려진 제약
- 내비 경로가 있어도 에뮬레이터 차량이 움직이지 않으면 남은 시간·TBT 가 그대로입니다. 같은 TBT 안내가 20초 넘게 그대로면 정체/정차로 보고 일시정지를 풉니다.
- 내비에 이미 길 안내가 있는데 다른 목적지로 출발하면, 내비가 이전 경로를 먼저 취소하며 남은 시간 0/0 을 보냅니다. 이 값은 "도착"으로 쓰지 않습니다.
- 에뮬레이터 내비의 차 위치는 성남 수정구 부근이라 최근 목적지 **소프트웨어드림센터**는 예상 시간이 1분 미만이고, 고르면 "곧 도착이라 게임을 시작하지 않아요"로 출발을 막습니다.
- 최근 목적지는 내비 길 안내가 없을 때만 보입니다(길 안내 중에 고르면 운전자의 길 안내를 바꾸게 되므로). 새로 만든 에뮬레이터는 최근 목적지가 없어 순정 내비에서 한 번 길 안내를 해야 생깁니다.
- 경로 요청(`requestRoute`) 시 순정 내비 앱이 같은 창의 앞으로 나옵니다. 놀레오로 돌아오면 이어서 진행됩니다(발화가 끊겼다면 "계속").
- 앱은 창을 직접 나누거나 옆 창을 지정할 수 없습니다(Pleos 창 정책 §6.3, `FLAG_ACTIVITY_LAUNCH_ADJACENT` 무시 확인). 3분할은 사용자가 창 막대로 만들고, 놀레오는 그 상태에서 **기본 지도 앱(`CATEGORY_APP_MAPS`)을 앞으로 불러** 자리를 바꿉니다(새로 앞에 나온 앱이 운전석 쪽 primary 창으로 가는 규칙). NaviHelper `requestNavigationScreen(Foreground)` 는 내비가 이미 보이면 다시 실행되지 않아 자리가 바뀌지 않으므로 쓰지 않습니다. 좌핸들 배치(Driving view 왼쪽)에서만 동작합니다.
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
| **NaviHelper** | 남은 시간·경로로 게임 길이 조절, 내비에서 시작한 길 안내를 홈에 실시간 반영, 길 안내가 없을 때 **최근 목적지**(`getRecentDestinationInfo`)를 예상 시간(`requestTrip` — 길 안내 없이)과 함께 홈에 보이고 출발하면 그곳으로 길 안내(`requestRoute`), 안내 시점(TBT)에 맞춰 일시정지, 졸음 케어 2단계: **TBT 목록의 경로 위 휴게소·졸음쉼터**(`TURN_ENTER_SERVICEAREA`·`TURN_RESTAREA`, 구간 거리 누적)에서 쉬어 갈 곳 고르기 · 경로가 바뀌면 다시 조회(`getTBTInfo`) | `core/navi/NaviHelperTripSource.kt` · `core/navi/TripRepository.kt` · `app/HomeState.kt` · `domain/engine/SessionPlanner.kt` · `app/GameCoordinator.kt#observeTbt` · `domain/rest/RestStop.kt` |
| **FusedLocation** | 현재 위치로 지역 퀴즈 선택(주행 중 음성 판 · 정차 중 화면 지역 퀴즈), 지나온 지역 기록, 졸음 케어 2단계: 진행 방향으로 상·하행 가르기(휴게소 편의시설·졸음쉼터 좌표), 쉼터 **도착 감지**(`addProximityAlert` 반경 300 m ENTER + 서거나 주차), 길 안내가 없을 때 앞쪽 졸음쉼터 탐색 | `core/location/FusedLocationSource.kt` · `core/location/RegionResolver.kt` · `domain/rest/RestStop.kt` · `app/RestCareController.kt` |
| **SpeechToText** | 운전자·동승자의 답변 인식, 응답에 걸린 시간 측정, 쉬어 가기 질문의 "네/아니요" | `core/speech/PleosSpeechEngine.kt#listen` · `domain/engine/AnswerCollector.kt` · `domain/engine/YesNo.kt` |
| **TextToSpeech** | 문제 출제, 결과 안내, 반응 저하 시 말 걸기, 휴게소·졸음쉼터 권유(편의시설 포함)·도착·다시 출발 안내, 정차 중 화면 게임(사자성어·지역 퀴즈·끝말잇기)의 문제·결과·유래 읽기 | `core/speech/PleosSpeechEngine.kt#speak` · `domain/engine/GameSession.kt` · `app/ScreenQuizController.kt` · `domain/games/ScreenGames.kt` · `domain/games/IdiomSpeech.kt` |

매니페스트 퍼미션은 실제로 호출하는 API 의 것만 선언했습니다: `pleos.car.permission.CAR_DRIVE, CAR_SEATS, NAVI_ROUTE, NAVI_ROUTE_SEARCH, TTS_SERVICE, STT_SERVICE, FUSED_LOCATION`, `android.car.permission.CAR_POWERTRAIN, CAR_SPEED`, `android.permission.RECORD_AUDIO`(폴백). 졸음 케어 2단계의 TBT 목록(`getTBTInfo`)과 홈의 최근 목적지·예상 시간(`getRecentDestinationInfo`·`requestTrip`)은 `NAVI_ROUTE`, 근접 알림(`addProximityAlert`)은 `FUSED_LOCATION` 으로 동작해 퍼미션을 새로 늘리지 않았습니다. `INTERNET`·`*_BINDING` 은 Pleos SDK AAR 이 병합하는 것으로, 앱 자체의 네트워크 호출은 없습니다(퀴즈·사자성어·사전·졸음쉼터·휴게소 자료 모두 assets).

---

## 5. 아키텍처

```
com.nolleo.app
├─ app/        NolleoApp · AppContainer(수동 DI) · MainActivity(단일 액티비티) · GameCoordinator · HomeState(홈 목적지 고르기: 현재 경로 · 최근 목적지 · 목적지 없이) · SessionFocusPolicy(음성 세션의 오디오 포커스 대응 판단) · RestCareController(쉬어 가기) · ScreenQuizController(정차 중 화면 게임 3종의 읽기·포커스·넘김) · WindowPlacement(3분할 창 위치)
├─ core/
│  ├─ car/       CarStateSource · AaosCarStateSource · VehicleSdkClient · SimulatedCarStateSource · CarStateRepository
│  ├─ navi/      TripSource(TripTarget) · NaviHelperTripSource(최근 목적지·예상 시간 포함) · NaviPlace · TimerTripSource(목적지 없이 — 시간만) · SimulatedTripSource · TripRepository · TripUnits · DestinationPreset(시연용 시뮬 경로)
│  ├─ location/  FusedLocationSource · RegionResolver · LocationRepository(Fused → 내비 주소 → 시뮬)
│  ├─ speech/    SpeechEngine · PleosSpeechEngine · AndroidSpeechEngine · ScriptedSpeechEngine · SpeechRouter · AudioFocusHelper
│  └─ log/       AppLog(PII 마스킹, 디버그 패널 링버퍼)
├─ domain/
│  ├─ model/     DriveState · UxState · GameMode · PauseReason · TurnHint · SessionPlan · Player · Question · GeoMath(거리·방위각 — 거리 계산은 여기 하나) …
│  ├─ engine/    DriveStateDeriver · ModeArbiter · ModeController · SessionPlanner · GameSession · AnswerMatcher · AnswerCollector · YesNo
│  ├─ games/     VoiceGame · RegionQuizGame · WordChainGame · IdiomQuizGame · IdiomSpeech(읽기용 문장) · ScreenGames(정차 중 화면판: 사자성어 · 지역 퀴즈 · 끝말잇기) · WordDictionary(출제용·검증용) · WordBlocklist(금칙어) · Hangul(두음법칙)
│  ├─ drowsy/    DrowsinessMonitor (1단계 말 걸기 · 2단계 쉬어 가기 · 이어서 하기용 SavedState)
│  └─ rest/      RouteRestStops(TBT → 경로 위 쉼 장소) · RestStopFinder(고르기·상하행 맞추기) · RestArea · ServiceAreaInfo · MotionHeading
├─ data/       ContentRepository(assets) · ContentParser · AppPreferences(DataStore) · SnapshotCodec(저장본 v3 — v1·v2 도 읽음)
└─ ui/         theme(NolleoTheme · 색 Nl · 간격 Sp · 모서리 Radius) · common(주/보조 버튼 · 즉시 눌림 표시 pressable · 선택 막대 · 설정 줄 · 목록 줄 · 카드 · 안내 카드 · 칩 · 아바타 · 로고 · 넓은 창 기준 isWideLayout) · home · voice · screen · summary · debug
```
입력 소스(차량·여정·위치)와 음성 엔진은 **실제 SDK → 폴백 → 시뮬레이션** 순으로 자동 선택되고, 선택 결과는 디버그 패널 각 칸 제목과 logcat 에 보입니다(화면 제목 줄에는 두지 않음).

저장소 루트의 `tools/` 에는 자료 변환 스크립트가 있습니다: `convert_rest_areas.js`(졸음쉼터 CSV → `rest_areas.json`) · `convert_service_areas.js`(휴게소 편의시설 → `service_areas.json`).

### 화면 디자인
- **색**(팀이 고른 "옐로"): 주간은 연회색 바탕·흰 카드·거의 검정 글, 야간(시스템 야간 모드)은 차콜 바탕·흰 글. 강조색 노랑(#FFC53D, 노랑 위 글은 항상 검정)은 **주 버튼·고른 표시·꼭 봐야 하는 안내 카드**에만 씁니다. 초록·빨강 상태색은 정차 중 화면 게임의 정답·오답 표시와 음성 화면의 진행 점·"듣는 중" 표시에만, 보라색은 쓰지 않습니다.
- **덩어리를 줄였습니다**: 홈은 `목적지` 목록 카드(현재 경로 · 최근 목적지 · 목적지 없이 — 많아야 4줄, "목적지 없이"의 시간은 선택 막대)와 `오늘의 게임` 카드 두 개뿐이고, 운전·함께·게임·간격은 줄마다 **선택 막대**(한 줄로 묶인 보기)로 고르며 맨 아래 **출발** 하나만 노란 버튼입니다. 음성 화면은 문제·차례·마이크 상태를 흰 카드 하나에, 정차 중 화면 게임 3종은 머리 줄 하나(← 목록 · 제목 · 다시 듣기)에 모았습니다.
- **화면마다 채운 주 버튼은 하나**(출발·이어서 하기·일시정지·계속·다음 문제), 나머지는 테두리 버튼입니다. 정차 중 게임 카드 세 개는 대등해서 **시작하기**가 모두 테두리 버튼입니다.
- **누르면 바로 반응**: Material 물결(ripple) 대신 누르는 동안만 살짝 어두워지고 떼면 바로 돌아옵니다(주행 중 애니메이션 금지와도 맞음). 고른 목적지는 라디오 점 + 옅은 노랑 바탕, 선택 막대는 고른 칸이 떠 보여 색만으로 구분하지 않습니다.
- 졸음 케어 말 걸기·쉬어 가기는 **노란 안내 카드**(야간에는 눈부시지 않게 어두운 호박색 + 노란 테두리)로 띄우고, 쉬어 가기를 묻는 동안 아래 버튼(일시정지·계속·지금 시작)은 테두리 버튼으로 낮춥니다.
- 주행 중(주행 규제) 도착 요약은 스크롤 없이 3줄(제목·맞힌 수와 1등·안내) + **처음으로**만, 차를 세우면 전체 기록을 보여 줍니다. 화면 문구에는 느낌표를 쓰지 않습니다(발화는 그대로).
- **가족이 보이게**: 운전자·가족 점수에 이름 첫 글자 아바타(지금 차례인 사람은 채운 원). 앱 안 제목 줄은 왼쪽에 로고 + 영문 **Nolleo** 뿐. 런처 아이콘은 차콜 바탕에 노란 언덕 너머 길과 웃는 해(적응형·단색판).
- 차량 규칙: 터치 76dp 이상, 가장 작은 글자 18sp(주행 화면 본문 28sp 이상), 글 대비 4.5:1 이상(주·야간 모두), 주행 중 애니메이션 없음, 굵기는 400·600(제목 Nolleo 만 700).

### 상태 머신 요약
```
DriveState  PARKED(기어 P) · STOPPED(저속 3초 또는 속도0+브레이크) · DRIVING · UNKNOWN(→ 안전하게 VOICE)
GameMode    IDLE(세션 없음) · VOICE(주행/정차/DO) · SCREEN(주차 && !requiresDO) · PAUSED(길 안내·통화·사용자·백그라운드·쉬어 가기)
규칙  1) requiresDO 면 SCREEN 금지(즉시)  2) 주행 상태 1.5초 디바운스  3) PAUSED 진입 시 TTS/STT 즉시 중단, 재개 시 같은 문제
      4) SCREEN→VOICE 즉시, VOICE→SCREEN 은 음성 판 마무리 후  5) 전환 로그 "ModeArbiter: DRIVING+DO → VOICE (from=…, reason=…)"
여정  현재 경로(내비 길 안내 그대로) · 최근 목적지(출발 때 requestRoute, 자동 모드에서 내비가 시작 못 하면 예상 시간으로 시간만 셈)
      · 목적지 없이(시간 여정 30분~2시간, 주차 중엔 멈춤) · 시뮬 경로(디버그 패널 여정=시뮬, 시연용)
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
- 단위 테스트 214개 — 모드 판정·판 짜기(다시 계획한 판 반영)·홈 목적지 고르기(현재 경로·최근 목적지·목적지 없이)·시간 여정·게임 규칙(음성 판·정차 중 화면 게임, 목적지 없는 세션의 끝맺음)·졸음 케어(저장·복원·인식 오류 제외)·쉬어 가기 질문의 일시정지·오디오 포커스 대응·쉼 장소 고르기·콘텐츠 자산·저장본(v1/v2/v3)·내비 처리(최근 목적지 정리·예상 시간 맞추기)·창 배치 등
- 사용량(release APK, 에뮬레이터 4코어, 11/8 측정 — 기기 전체 = 100% 기준): 주행 중 음성 게임 CPU 평균 0.2%·최대 0.5%, 정차 중 화면 게임 평균 0.1%·최대 0.3%, 메모리 PSS 44~64 MB, 첫 실행 0.91초. 기준(주행 중 CPU 5% 미만 · 정차 중 25% 이하 · RAM 400 MB 이하 · 실행 10초 이하) 안이다. 음성 합성은 Pleos 플랫폼 서비스가 따로 한다. 상황별 표는 `docs/WORK_STATUS.md` 1.18

## 7. 문서
- [`docs/README.md`](docs/README.md) — docs 폴더 안내 (각 문서가 무슨 내용인지)
- [`docs/FEATURE_GUIDE.md`](docs/FEATURE_GUIDE.md) — 기능 사용설명서 (기능별로 에뮬레이터에서 확인하는 방법)
- [`docs/DEMO_SCENARIO.md`](docs/DEMO_SCENARIO.md) — 3분 시연 대본
- [`docs/PHASE2_NOTES.md`](docs/PHASE2_NOTES.md) — 2차(12/3 PT) 설계 메모(아직 구현하지 않은 것만)
- [`docs/CONTENT_SOURCES.md`](docs/CONTENT_SOURCES.md) — 퀴즈·사자성어·사전·졸음쉼터·휴게소 자료 출처와 생성 방식
- [`docs/WORK_STATUS.md`](docs/WORK_STATUS.md) — 작업 현황·결정 사항·남은 일 (한 파일을 계속 갱신)

### 콘텐츠 라이선스
`app/src/main/assets/content/quiz_idioms.json`(사자성어 1,014문항)은 앱 소스 코드와 별개로 **CC BY-SA 4.0** 으로 배포합니다. 뜻풀이·유래 출처는 국립국어원 《표준국어대사전》(CC BY-SA 2.0 KR, https://stdict.korean.go.kr)이고, k1tech-ai-kr/k1-db-idioms(CC BY-SA 4.0)를 거쳐 가공했습니다. 한자 훈·음은 한국어문회 급수 한자(rycont/hanja-grade-dataset), libhangul 한자 사전(LGPL 2.1), Wiktionary(CC BY-SA 4.0), 팀 보충에서 가져왔습니다. 변경 사항과 자세한 출처는 [`docs/CONTENT_SOURCES.md`](docs/CONTENT_SOURCES.md)에 있습니다.

끝말잇기 사전(`words_ko_valid.txt` 194,871어 · `words_ko_play.json` 7,882어)은 국립국어원 《표준국어대사전》·《한국어기초사전》(CC BY-SA 2.0 KR)에서 가져와 가공했고, 단어 목록 파일은 앱 코드와 별개로 **CC BY-SA 4.0** 으로 배포합니다.

## 8. release 서명 키 만들기 (직접 빌드할 때)
```bash
keytool -genkeypair -v -keystore <리포 밖 경로>/nolleo-release.jks -storetype PKCS12 \
  -alias nolleo -keyalg RSA -keysize 2048 -validity 10000
```
`local.properties` 에 `RELEASE_STORE_FILE`(슬래시 경로), `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` 를 적고 `./gradlew assembleRelease`. 키 파일과 비밀번호는 절대 커밋하지 마세요.
