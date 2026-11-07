# 길동무 작업 현황 — 2026-11-06

- 브랜치: `main` — `feat/gildongmu-prototype` 작업을 사용자가 main 으로 합쳐 `origin`(github.com/dxyxxn/HMC-Contest)에 push. 이때 히스토리가 다시 쓰여 아래 커밋 해시는 현재 main 기준이다.
- 기준 지시서: `docs/GILDONGMU_PROTOTYPE_PROMPT.md` (사용자가 `1cb7d90` 에서 `.gitignore` 변경과 함께 커밋)
- 변수: 팀명 `Team-DonDoAYeon` · 패키지 `com.gildongmu.app` · 앱 이름 `길동무` · Pleos Client ID/Secret 없음 → TTS/STT `Mode.ON_DEVICE`
- 검증 환경: Pleos Connect Emulator v2.1.0 AVD(cold boot, 2560×1440), Gradle 9.6.0 / AGP 9.5.0 / built-in Kotlin 2.2.10 / compileSdk 37

---

## 1. 작업한 내용

### 1.1 마일스톤별 결과

| 마일스톤 | 커밋 | 내용 | 검증 |
|---|---|---|---|
| M0 환경 | `9cce509` | Pleos Maven, SDK 5종 + Compose + DataStore + 코루틴, 매니페스트(필요 퍼미션만·allowBackup=false·ROTARY_SUPPORTED=false·distractionOptimized), applicationId/앱 이름, local.properties → BuildConfig | `assembleDebug` 통과, 빈 화면 AVD 실행 |
| M1 주행 상태·모드 | `120d1b4` | AaosCarStateSource(기어·속도·주차브레이크·UX 규제), VehicleSdkClient(Brake·Seat), SimulatedCarStateSource, DriveStateDeriver, ModeArbiter(DO 즉시·1.5초 디바운스·판 마무리 후 SCREEN), ModeController | Virtual Console 기어 P↔D·속도·브레이크 → 2초 안 모드 전환, 테스트 25개 |
| M2 여정 | `bb25233` | NaviHelperTripSource, SimulatedTripSource(×30 압축·TBT 2회), TripRepository(오류·60초 무응답 폴백), TripUnits, SessionPlanner | `requestRoute` → `onRouteStarted(14820s, 378km)` 수신, 단위=초 확정, 테스트 13개 |
| M3 음성 | `f0b3fac` | Pleos/Android/대본 엔진, SpeechRouter, AudioFocusHelper, AnswerCollector(지연 측정) | Pleos TTS/STT ON_DEVICE 초기화·발화 완료·STT 타임아웃 측정, 포커스 충돌 없음 |
| M4~M6 게임·졸음·P1·UI | `2fa3812` | 콘텐츠(170+48+40, 사전 3,582), 지역 퀴즈·끝말잇기·초성 퀴즈, GameSession, FusedLocation·지역 판정, DrowsinessMonitor(1단계), TBT 일시정지, 요약 화면, Compose 화면 전부, 디버그 패널, 상태 저장/복원 | 기기에서 전체 흐름 확인(아래 1.2), 테스트 총 101개 |
| M7 마감 | `d858f72` | README, DEMO_SCENARIO, PHASE2_NOTES, CONTENT_SOURCES, release 서명 APK, CLAUDE.md 현행화, 이 문서 | `./gradlew lintDebug testDebugUnitTest assembleRelease` 통과(lint 0 errors) |
| 피드백 반영 ① | `228b798` | 게임 끝내기 버튼, 중간 종료 요약 제목, 쉬는 시간 안내를 "약 N분 뒤"로 (아래 1.6) | 에뮬레이터 release 빌드로 확인, DoD 통과, 테스트 102개 |
| 피드백 반영 ② | `67930d6` | 내비에서 시작한 길 안내를 홈에 실시간 반영, 3분할에서 길동무를 맨 오른쪽 창으로 (아래 1.7) | 에뮬레이터 release 빌드로 확인, DoD 통과, 테스트 115개 |
| 샘플 대조·문서 점검 + 판 간격 선택 | `0dca7d1` | `pleos-example` 샘플과 SDK 사용법 대조 후 보완, 문서 최신화 (아래 1.8) · 홈에서 판 간격 3종 선택, 화면에서 사라지면 일시정지 유지 결정 (아래 1.9) | 에뮬레이터 release 빌드로 확인, DoD 통과, 테스트 120개 |
| 졸음 케어 2단계 + 경로 위 휴게소·졸음쉼터 안내 | (이 커밋) | 2단계 판단·쉬어 가기 질문, 도착 후 대기·재출발 인사, 내비 0/0 버그 수정, 사용설명서 `docs/FEATURE_GUIDE.md` (아래 1.10) → 같은 날 개정: 내비 TBT 의 경로 위 휴게소·졸음쉼터(구간 거리 누적)에서 고르기, 휴게소 편의시설 247곳, `addWaypoint` 제거, 서거나 주차해야 도착 (아래 1.11) | 에뮬레이터 debug·release 빌드로 확인, DoD 통과, 테스트 152개 |

M4~M6 은 GameSession·Coordinator·UI 가 서로 물려 있어 한 커밋으로 묶었다.

### 1.2 에뮬레이터에서 확인한 흐름 (DoD)
- 앱 실행: debug 2.5초 / release 0.9초 (기준 ≤ 10초)
- 기어 P → D: `requiresDO=true, active=80` 수신 → 즉시 VOICE, 1.5초 뒤 DRIVING 확정 / D → P: 진행 중 차례만 끝내고 "차가 멈췄네요…" → SCREEN
- 여정 설정 → 음성 퀴즈 한 판(대본 응답, 실제 Pleos TTS) → 끝말잇기 한 판 → sleepy 대본으로 Stage1 카드("잠깐 쉬어 갈까요?") + 난이도 상한 2 → 시뮬 TBT 300m 자동 일시정지·재개 → 정차 중 초성 퀴즈 → "남은 5분" → 도착 요약: **크래시·멈춤 없음**
- 백그라운드(다른 앱이 창을 차지) → 즉시 중단·포커스 반납·상태 저장 → 복귀 시 "잠시 멈췄어요 + 계속" → 같은 문제부터 재출제
- Back 키 → `moveTaskToBack` (액티비티 종료·SDK 해제 없음)
- 창 분할(⋮ 핸들) → 세로 1창 레이아웃, 가로 2창 레이아웃 모두 잘림 없음
- 프로세스 강제 종료 후 재실행 → 홈에 "진행 중이던 여정이 있어요 · 이어서 하기"
- 성능: 주행 중 CPU 0~0.5%, PSS 약 50 MB
- release 빌드: R8 적용 상태에서 SDK 5종 초기화·세션 진행 정상, PII 로그 `<redacted>` 마스킹

### 1.3 실제 SDK vs 시뮬레이션

| 항목 | 상태 |
|---|---|
| AAOS CarPropertyManager (GEAR_SELECTION, PERF_VEHICLE_SPEED, PARKING_BRAKE_ON) | 실제 SDK로 검증 |
| CarUxRestrictionsManager | 실제 SDK로 검증 (기어 D 에서 DO 수신) |
| Pleos Vehicle Brake·Seat | 실제 SDK로 검증 (capability → register/getCurrent) |
| NaviHelper requestRoute / onRouteStarted / onDestinationInfo / onTBTInfo / onCurrentLocationInfo | 실제 SDK로 검증 (차량이 움직이지 않아 남은 시간 감소는 시뮬로 시연) |
| NaviHelper TBT `TURN_ENTER_SERVICEAREA`·`TURN_RESTAREA` | 실제 SDK로 검증(11/7) — 고속도로 경로 TBT 목록에 휴게소·졸음쉼터가 경로 순서대로 들어 있음(부산역 경로 22곳: 용인졸음쉼터 18.1 km·용인휴게소 28.5 km…). `distance` 는 앞 항목부터의 구간 거리 |
| NaviHelper addWaypoint | 실제 SDK로 시험(11/7) 후 **쓰지 않기로** — 경유지는 들어가고 내비가 재탐색하지만, POI ID 없는 좌표라 고속도로 옆 일반 도로에 붙어 IC 로 나갔다 들어오는 우회 경로가 됨(아래 1.11) |
| FusedLocation | 실제 SDK로 검증 — 기본 좌표로는 국내 지역이 안 나와 내비 주소/시뮬로 폴백, `adb emu geo fix 126.9780 37.5665` 를 넣으면 `region=seoul via FusedLocation` (11/7 확인) |
| FusedLocation addProximityAlert | 실제 SDK로 검증(11/7) — 등록 → `alertId`, `geo fix` 로 쉼터 좌표에 넣으면 ENTER 콜백(`rest stop arrived (근접 알림 ENTER)`) |
| Pleos TTS | 실제 SDK로 검증 (onDone, 포커스 스택 확인) |
| Pleos STT | 초기화·청취 실제. 에뮬레이터 Microphone 을 호스트 입력으로 켜고 응답 입력 "마이크"로 실제 인식 확인(사용자, 11/7). 마이크 없이 시연할 때는 대본 응답 |
| Android TTS/STT 폴백 | 이 에뮬레이터에 한국어 TTS 엔진 없음 → 자동으로 건너뜀(코드 경로만 존재) |
| `driving_regulation on/off` 셸 | 이 사용자 빌드에 `hmg_car_service` 없음 → 기어 D 로 대체 검증 |
| 통화·Gleo 로 인한 `AUDIOFOCUS_LOSS_TRANSIENT` | 코드만 구현, 에뮬레이터에서 재현 못 함 |
| Suspend(IGN OFF) | 백그라운드 전환(onStop) 경로로 대체 구현·검증, 실제 Suspend 진입은 미검증 |

### 1.4 결정 사항 (되돌리기 어려운 것은 굵게)
- **UI 툴킷 = Jetpack Compose + Material3** (AGP 9.5 built-in Kotlin 2.2.10 + compose 플러그인 2.2.10 으로 첫 시도에 빌드 성공). Views/AppCompat/material 의존성은 제거.
- **compileSdk 37 유지** (platforms/android-37.0 에 `optional/android.car.jar` 존재 → 하향 불필요).
- **퍼미션**: 지시서 목록 그대로(CAR_DRIVE, CAR_SEATS, NAVI_ROUTE, NAVI_ROUTE_SEARCH, TTS_SERVICE, STT_SERVICE, FUSED_LOCATION, android.car CAR_POWERTRAIN·CAR_SPEED, RECORD_AUDIO). SDK AAR 이 `INTERNET`·`*_BINDING` 을 병합함.
- **`distractionOptimized=true` 메타데이터 추가** (지시서 체크리스트에는 없지만 주행 중 음성 화면을 띄우려면 AAOS 에서 필요. DEV_GUIDE 에 선언 방법이 없어 AAOS 표준 키 사용).
- **release 서명 키 생성**: `C:\Users\rlaeh\.android\gildongmu-release.jks` (리포 밖), 비밀번호는 `local.properties` 에만. **분실하면 같은 서명으로 업데이트할 수 없으므로 반드시 안전한 곳에 백업**.
- 작업 브랜치 `feat/gildongmu-prototype` 에서 개발 → 11/6 사용자가 main 으로 합쳐 push(이후 main 기준).
- JSON 은 `org.json` 으로 통일(테스트 클래스패스에만 `org.json:json` 추가), DI 는 수동(AppContainer), 영속화는 DataStore(Preferences).
- 오디오 포커스: 음성 판 시작 시 요청, 판 종료·일시정지·백그라운드 시 반납(긴 쉬는 시간 동안 transient 포커스를 붙잡지 않기 위함). 통화 대기(LOSS_TRANSIENT) 중에는 GAIN 을 받으려고 유지.
- Pleos TTS 발화 중 들어온 포커스 손실은 Speech Service 자체 포커스로 보고 무시(관측상 충돌은 없었음).
- 정차(VOICE→SCREEN): "판이 끝날 때까지" 대신 **진행 중인 차례만 끝내고 판을 마무리** — 데모·실사용 모두 10분을 기다리지 않게.
- 판 진행은 문제 수 기준(문제당 약 30초 환산), 판 시작 시점은 "남은 시간이 기준값 이하"로 판단. 시뮬 여정은 주차 중 시간이 흐르지 않음.
- 판 간격은 홈에서 사용자가 고른다(여유 있게 기본 · 자주 · 쉬지 않고). 길동무가 화면에서 사라지면 일시정지(백그라운드 음성 진행 없음) — 11/7 사용자 결정.
- TBT 일시정지: 같은 안내가 20초 넘게 그대로면 정체/정차로 보고 재개(에뮬레이터의 고정 TBT "TURN_LEFT 80m" 대응).
- 졸음 기준값: 캘리브레이션 5개는 중앙값, 무응답(8초)은 캘리브레이션에서 제외, EMA 는 느리지 않은 응답으로만 갱신.
- 데모 모드: 판당 6문제, 운전자 기준값 없으면 1.5초 시드. sleepy 대본은 운전자 차례에만 적용(driverOnly).
- PARKED 판정: 기어를 알면 기어만 본다(에뮬레이터는 D 상태에서도 주차브레이크 텔테일이 켜질 수 있음).
- 졸음 케어 2단계(11/7, 사용자 결정 반영): **경로 정보가 있으면 경로 위 휴게소·졸음쉼터에서만 고른다**(가장 가까운 곳, 휴게소가 5 km 안쪽으로만 더 멀면 휴게소). **경유지(`addWaypoint`)는 넣지 않는다** — 좌표 경유지가 우회 경로를 만들었고, 경로 위라 필요 없다. 길 안내가 없을 때만 공공데이터 졸음쉼터 표(근사, "A기점 + B종점" 은 A → B 차로로 해석). 도착은 근처에서 서거나 주차해야(지나가는 것과 구별). 도착 안내 뒤에는 다시 출발할 때까지 문제를 내지 않는다(`PauseReason.REST_AREA`). 질문 문구는 "졸음"을 단정하지 않고 "반응이 계속 느려졌어요".
- 디버그 패널은 release 에도 그대로 둔다(11/7 사용자 결정 — 시연·심사용). 앱 마켓 제출본에서는 빼거나 차량 칸(시뮬 전환·시뮬 DO 끔 — 주행 규제 우회 가능)을 숨길 것.
- 쉼터 이름·거리·진행 방향·좌표도 차의 위치를 짐작하게 하므로 `AppLog.sensitive`(release 에서 `<redacted>`)로만 남긴다. 에뮬레이터 시험용 `geo fix` 좌표는 디버그 패널 화면("쉬어 가기 상태")에서 본다.

### 1.5 문서·자료와 실제가 달랐던 점 (기획서/문서 수정 참고)
- API 레퍼런스의 `VehicleException.errorCode` → 배포된 Vehicle 2.3.2 AAR 에서는 클래스가 난독화(`callback.exception.a`)되고 `ErrorCode` 가 runtime 스코프라 컴파일 시 사용 불가 → 메시지 기반 재시도로 대체.
- DEV_GUIDE §7.1 의 `adb shell cmd hmg_car_service … driving_regulation` → Emulator 2.1.0(user 빌드)에 서비스 없음. `cmd car_service inject-vhal-event` 도 user 빌드라 막힘. Virtual Console 은 HTTP API(`PUT /vehicle/gear?gear=Drive`, `PUT /vehicle/controls?speed=…`, 브라우저 세션 하트비트 필요)로 스크립트 조작 가능.
- `requestRoute` 를 호출하면 순정 내비가 같은 창 앞으로 나온다. 장거리 프리셋은 샘플에 POI ID 가 없어 **빈 poiId + 좌표·주소**로 요청했고 정상 수락됨.
- 경로가 없을 때도 `onDestinationInfo(remainTime=0, remainDistance=0)` 가 온다 → 무효 값으로 처리. 새 경로를 요청해 내비가 이전 경로를 취소한 직후에는 `onDrivingInfo` 도 0/0 으로 온다 → 같은 규칙으로 무시(11/7, 아래 1.10).
- 에뮬레이터 순정 내비는 고속도로 경로의 TBT 목록에 휴게소(`TURN_ENTER_SERVICEAREA`)·졸음쉼터(`TURN_RESTAREA`)를 경로 순서대로 넣어 준다. API 레퍼런스는 `TBTInfo.distance` 를 "Distance to the upcoming turn"이라고만 적었지만 실제로는 **앞 항목부터의 구간 거리**다(14000 → 9300 → … 처럼 순서대로 늘지 않고, 더하면 실제 거리와 맞음). `TURN_ENTER_SERVICEAREA` 에는 "하이패스센터"도 있다. 목록은 50여 항목까지. 경로가 없을 때(재탐색 중 등)는 빈 목록이 온다.
- `addWaypoint` 를 부르면 내비가 경로를 취소 → 재탐색(`onRouteCancelled` → FREE_DRIVE → `onWaypointChanged` → ROUTE_DRIVE)하고 앞으로 나온다. POI ID 없이 좌표만 주면 고속도로 시설을 근처 일반 도로에 붙인다(오산졸음쉼터 좌표 → 오산IC 로 나가 시내 "경유" 지점을 찍고 다시 진입).
- 에뮬레이터 내비는 서울(판교) → 부산역을 경부선이 아니라 영동선 → 중부내륙선 → 대구 → 청도로 안내한다(378 km, 약 4시간 10분).
- 공공데이터포털 15045551 "고속도로 출입시설 위치정보(LOD)"는 설명에 휴게소·졸음쉼터가 있다고 하지만 실제 파일에는 IC·JCT 407곳뿐이다.
- Pleos TTS SDK 는 발화 텍스트를 자체 logcat(`[GAAS][SDK]OnDeviceStreamTts doSpeak text=…`, D 레벨)에 남긴다 — 앱이 막을 수 없는 SDK 동작(앱 로그는 PII 규칙을 지킴).
- AAOS 런타임 권한 대화상자에서 `CAR_SPEED` 는 "위치" 그룹으로 표시되고, `FUSED_LOCATION` 은 라벨이 없어 "알 수 없는 작업"으로 보인다(플랫폼 측 표기).
- HOME 키는 이 에뮬레이터에서 앱 창을 가리지 않는다(전용 홈 패널) → 백그라운드 검증은 다른 앱을 같은 창에 띄워서 했다.

### 1.6 사용자 피드백 반영 (11/6)
피드백: ① 일시정지하거나 멈췄을 때 홈으로 돌아가는 "닫기"가 없다 ② 쉬는 시간 화면의 "남은 시간이 3시간 N분일 때 다음 판을 시작해요"가 "3시간 뒤에 시작"으로 읽힌다(실제 뜻은 "도착까지 남은 시간이 3시간 N분이 되는 시점").

| 변경 | 내용 |
|---|---|
| 게임 끝내기 | 음성 화면 하단: 일시정지·쉬는 시간·출발 대기·모든 판 완료 때 "게임 끝내기"를 기존 버튼(계속/지금 시작/한 판 더) 옆에 표시. 문제를 내고 듣는 중에는 "일시정지" 하나만(주행 중 터치 1개 원칙). 정차 중 화면 게임: 상단에 항상 |
| 끝낸 뒤 | TTS·STT 즉시 중단, 오디오 포커스 반납, 저장된 진행 상태 삭제, 내비 경로는 유지. 맞힌 문제나 마친 판이 있으면 요약("오늘의 길동무 · 여기까지 했어요") → 처음으로, 없으면 바로 홈 (`GameCoordinator.endSession`, `SessionSummary.endedByUser`) |
| 쉬는 시간 문구 | "다음 판은 약 N분 뒤에 시작해요"(1분 이하면 "곧 다음 판을 시작해요") + "판 사이에는 말을 걸지 않아요 · 바로 하려면 '지금 시작'". 정차 화면 하단은 "다음 음성 판: 주행 약 N분 뒤"(주차 중에는 여정 시간이 흐르지 않으므로). 계산: `SessionPlanner.secondsUntilNextRound` = 남은 시간 − 판 시작 기준 |
| 기타 | 새 판을 시작할 때 이전 판의 마지막 답 줄(`lastAnswer`)을 지움 |

확인(release APK, 에뮬레이터): 일시정지 → "게임 끝내기 / 계속", 쉬는 시간 → "다음 판은 약 23분 뒤에 시작해요" + "게임 끝내기 / 지금 시작", 1문제 맞힌 뒤 끝내기 → "여기까지 했어요" 요약 → 처음으로 → 홈, 아무것도 하지 않고 끝내기 → 바로 홈. 테스트 102개(`secondsUntilNextRound` 추가).

### 1.7 사용자 피드백 반영 ② (11/6) — 내비 목적지 실시간 반영 · 3분할 배치
피드백: ① 길동무를 켠 뒤 내비에서 길 찾기를 시작하면 홈에 "내비 목적지" 버튼이 생기지 않는다 ② 길 안내 중이면 내비도 보여야 하지 않나(3분할) ③ 3분할일 때 게임을 맨 오른쪽에 배치.

| 항목 | 원인·확인 | 변경 |
|---|---|---|
| ① 내비 목적지 카드 | 내비 앱에서 직접 시작한 길 안내는 `onRouteStarted`·`onNavigationStatus(ROUTE_DRIVE)` 만 오고 `onRoutingCompleted`·`onDestinationInfo` 는 오지 않음(로그 확인). 카드는 `onDestinationInfo`(조회해야만 옴)에서만 만들어지고 있었다 | `onRouteStarted` 에서 바로 카드 생성 + `getDestinationInfo` 재조회, `ROUTE_DRIVE` 수신 시 재조회, `onRouteCancelled`·`onDestinationArrived`·FREE_DRIVE 에서 카드 제거. 앱이 다시 보일 때 즉시 재조회, 길 안내 중(앱이 보이거나 실제 내비 여정으로 게임 중)에는 30초마다 재조회(`TripRepository.shouldPollNavi`) — 실제 내비 여정의 남은 시간이 멈춰 있던 문제도 함께 해결. 홈은 새 길 안내를 자동 선택, 안내가 끝나면 프리셋으로(`HomeState.withNaviRoute`) |
| ② 내비 함께 보기 | Pleos 화면 = Driving view(고정) + 앱 창 최대 2개. 창 위치·크기는 사용자가 정한다(§5.4·§6.3). `FLAG_ACTIVITY_LAUNCH_ADJACENT` 는 무시되고 내비가 앱 영역 전체를 덮음(확인) → 앱이 3분할을 만들 수는 없다 | 3분할은 사용자가 창 막대로 만들고, 길동무는 세로 1창 레이아웃으로 그대로 동작(1차 때 확인한 그 테스트). 내비가 가려져 있을 때도 Driving view 에 TBT(회전 방향·거리)가 표시됨 |
| ③ 게임 맨 오른쪽 | 새로 앞에 나온 앱은 primary(운전석 쪽, 왼쪽) 창으로 가고 다른 앱은 오른쪽으로 밀린다(확인). NaviHelper `requestNavigationScreen(Foreground)` 는 내비가 이미 보이면 다시 실행되지 않아 효과 없음(확인) | 길동무가 운전석 쪽 창에 "새로" 들어오면(창이 줄어듦·처음 보임·다시 앞으로 나옴) 기본 지도 앱(`ACTION_MAIN` + `CATEGORY_APP_MAPS`, 패키지 하드코딩 없음)을 실행 → Driving view \| 내비 \| 길동무. 같은 크기로 자리만 바뀐 경우(사용자가 창 막대 탭)는 존중. 판단은 `WindowPlacement`(순수 함수), 실행은 `MainActivity` |

확인(release APK, 에뮬레이터): 길동무를 켠 채 내비에서 안내 시작 → 7 ms 안에 `navi route detected` + 카드 선택("내비 목적지: 부산역 · 남은 시간 4시간 2분", 플랜 "4시간 2분 → 5판"), 안내 종료 → 카드 제거·프리셋 복귀, 30초 주기 재조회 로그 확인. 창 막대를 끌어 3분할 → 길동무가 [853–1694] → [1718–2559]로 이동하는 동안 길동무 생명주기 이벤트 없음(게임 멈춤 없음), 가운데 막대 탭으로 사용자가 바꾼 위치는 유지. 퍼미션 추가 없음(`NAVI_CUSTOM_MAP` 은 시험 후 제거). 테스트 115개(+13).

### 1.8 `pleos-example` 샘플 대조 · 문서 점검 (11/7)
샘플 3종(Vehicle `VehicleClient`, Navi `NaviHelperViewModel`, FusedLocation `FusedLocationClient`)과 우리 SDK 클라이언트를 대조했다. 생명주기(생성 → initialize → 리스너 등록 → 같은 인스턴스로 해제 → release), capability 확인 후 읽기, 필요한 퍼미션만 선언은 이미 샘플과 같았다.

| 변경 | 근거(샘플) | 내용 |
|---|---|---|
| 내비 변경 이벤트 즉시 반영 | Navi 샘플이 받는 `onDestinationChanged`·`onReRouteEnded`·`onRouteOptionChanged`·`onWaypointChanged`·`onWayPointArrived` | 우리는 무시하고 있었다 → 받으면 바로 `getDestinationInfo` (30초 주기를 기다리지 않음) |
| FusedLocation 권한 요청 순서 | FusedLocation 샘플: `isSupported()` → 권한 요청 → `initialize()` | 위치 서비스가 없는 차량에서는 `FUSED_LOCATION` 권한 대화상자를 띄우지 않음 (`FusedLocationSource.isSupported`, `MainActivity`) |
| 시연 보호 | (문서 점검 중 발견) | 디버그 패널 여정=시뮬이면 내비 길 안내가 있어도 홈에서 자동 선택하지 않음 — 1.7 의 자동 선택이 시연용 시뮬 여정을 실제 내비 여정으로 바꿔 버릴 수 있었다 (`HomeState.withNaviRoute(autoSelect)`) |
| FusedLocation 실제 위치 시험 | FusedLocation 샘플 README 의 `adb emu geo fix` | 에뮬레이터에서 서울 좌표를 넣으면 FusedLocation 으로 `seoul` 판정 확인 → README·시연 대본·이 문서의 "에뮬레이터 GPS 가 미국이라 판정 불가" 설명을 바로잡음 |

제안만 하고 구현하지 않은 것(`docs/PHASE2_NOTES.md` §1·§7): `requestTrip` 으로 출발 전 프리셋 예상 시간을 실제 내비 값으로, Vehicle SDK 리스너를 화면이 보일 때만 등록, 근접 알림 시험 절차.

문서 점검: 커밋 해시(main 으로 합치며 다시 쓰임)·브랜치 상태, README 아키텍처의 `WindowPlacement`, 테스트 수, 시연 체크리스트(내비 카드와 시뮬 여정)를 고쳤다. 콘텐츠 수량(170·48·40·3,582)은 자산 파일과 일치.

확인(release APK, 에뮬레이터): 여정=시뮬에서 내비 안내 시작 → 카드("부산역 · 남은 시간 4시간 38분")는 뜨고 서울 → 부산 프리셋 선택 유지, 안내 종료 → 카드 제거. `geo fix` → `region=seoul via FusedLocation`. `./gradlew lintDebug testDebugUnitTest assembleRelease` 통과(lint 오류 0·경고 17, 테스트 116개).

### 1.9 사용자 결정 반영 (11/7) — 판 간격 선택 · 화면에서 사라지면 일시정지 유지
결정: 판 간격은 "처음 화면에서 고르는 방식", 내비가 앱 영역 전체를 덮을 때는 "지금처럼 일시정지".

| 항목 | 내용 |
|---|---|
| 판 간격 선택 | 홈에 "얼마나 자주 할까요?" — **여유 있게**(기본, 약 45분마다) · **자주**(약 20분마다) · **쉬지 않고**(판 사이 2분). 4시간 10분 기준 5판(쉬는 시간 약 39분) / 12판(약 10분) / 20판(약 2분). 미리보기에 "판 사이 약 N분 쉬어요" 추가 |
| 구현 | `RoundPacing`(판 시작 간격 목표값) → `SessionPlanner.plan(…, pacing)`. 판 길이는 간격 안에서 여유 1/4·자주 1/2·쉬지 않고 "간격 − 2분"(모두 6~10분 제한), 최소 휴지 2분·도착 5분 전 종료 규칙은 그대로. 간격은 `SessionPlan.pacing` 에 담겨 남은 시간이 바뀌어 다시 짤 때(`replanRemaining`)와 이어 하기 저장본(`SnapshotCodec`, 이전 저장본은 여유 있게로 읽음)에도 유지. 마지막으로 출발한 간격은 설정(`AppSettings.lastPacing`)에 저장 |
| 일시정지 유지 | 길동무가 화면에서 사라지면(onStop) 지금처럼 즉시 멈추고 포커스 반납·상태 저장, 다시 보이면 "계속"으로 같은 문제부터. 백그라운드 음성 진행(포그라운드 서비스)은 하지 않기로 결정 |

확인(release APK, 에뮬레이터): 홈 기본 "여유 있게" → "4시간 10분 → 5판 · 판 사이 약 39분 쉬어요", "쉬지 않고" → "20판 · 판 사이 약 2분 쉬어요"로 바뀌고 출발 시 `session start → … 20판`. 시험 뒤 간격을 "여유 있게"로 되돌려 둠. 테스트 120개(+4: 간격 3종·재계산 시 간격 유지·판 1개일 때 휴지 없음·옛 저장본 호환).

### 1.10 졸음 케어 2단계 + 졸음쉼터 안내 (11/7)
요청: "졸음 케어 2단계 전체 진행 → docs 반영 → 기능 사용설명서(에뮬레이터로 확인하는 법) 새로 작성". 기능 설명·확인 절차는 [`docs/FEATURE_GUIDE.md`](../FEATURE_GUIDE.md).

> ※ 같은 날 1.11 에서 고르는 방식이 바뀌었다(경로 위 휴게소·졸음쉼터, `addWaypoint`·"경로 위 쉼터 무시" 토글 제거, TBT 거리 누적, 서거나 주차해야 도착). 아래 표의 고르기·안내 칸과 확인 기록은 처음 구현 당시의 것이다.

| 구성 | 내용 |
|---|---|
| 판단 (`DrowsinessMonitor`) | 2단계 = 15분 안에 1단계 두 번째 또는 (이번 답 느림 && 최근 5개 정답률 < 50%). 쿨다운 20분, 난이도 −1. 데모 모드는 쿨다운만 1단계 20초·2단계 60초(`Config.DEMO`). 쉬고 다시 출발하면 `resetAfterRest`(응답 창·난이도 초기화, 기준값 유지). 디버그 "2단계 시험"은 다음 운전자 답 뒤 `forceStage2` |
| 질문 (`GameSession.offerRest`) | TTS "○○, 반응이 계속 느려졌어요. 잠깐 쉬어 갈까요? {노선} {방향} {쉼터}가 N킬로미터 앞에 있어요. 안내해 드릴까요?" → STT 네/아니요(`YesNo`) 또는 화면 **쉼터로 안내/괜찮아요** 중 먼저 온 답. 수락 "좋아요. ○○로 안내할게요. 도착할 때까지 게임은 쉴게요." / 거절 "알겠어요. 대신 다음 문제는 쉬운 걸로 갈게요." (TTS `stop()` 은 다음 말을 끊어 부르지 않음) |
| 쉼터 자료 | 공공데이터 전국졸음쉼터표준데이터 253곳 → `tools/convert_rest_areas.js` → `assets/content/rest_areas.json`(노선·from→to·좌표·지점 진행 방향). 출처·해석은 `docs/CONTENT_SOURCES.md` |
| 고르기 (`RestStopFinder`) | 내비 TBT `TURN_RESTAREA`(30 km 이내) 우선 → 없으면 표: 쉼터 도로 방향 ±45°, 앞쪽 ±45°, 옆 거리 ≤ max(2 km, 25%), 0.3~80 km, 점수 = 거리 + 3×옆 거리. 진행 방향 = 시뮬 경로 / FusedLocation 움직인 방향(30 m 이상, 5 km 넘게 튀면 무시) → SDK heading(0 은 모름) → 목적지 쪽. 국내 좌표가 아니면 고르지 않음 |
| 안내 (`RestCareController`) | 수락 → (실제 내비 여정) `addWaypoint`(WaypointIndex.FIRST, 원래 목적지 유지) + `addProximityAlert`(300 m, ENTER) + `PauseReason.REST_STOP`. 도착 = 근접 알림 / 위치가 반경 안 / `onWayPointArrived` / (좌표 없는 경로 위 쉼터는) 주차 중 먼저 온 것 → "○○에 도착했어요. 차를 세우고 충분히 쉬었다 가요." → `PauseReason.REST_AREA`(문제를 내지 않음) → 주차했다가 다시 주행하면 "○○, 잘 쉬셨나요? 다시 출발해요!". "안내 그만두기"는 게임 쉬기·도착 감지만 끄고 내비 경유지는 두며, "계속"을 누르면 언제든 바로 이어 간다 |
| 화면 | 음성 화면 카드(권유 → 가는 중·남은 거리 → 도착), 정차 화면 한 줄 배너, 요약 "졸음쉼터에서 쉬어 가기를 N번 권했어요", 디버그 패널(앞쪽 쉼터 미리보기·쉬어 가기 상태·도착 시험 좌표·2단계 시험·쉼터 도착(시뮬)·**경로 위 쉼터 무시**) |
| 시뮬 경로 | 서울 → 부산 시뮬 경유점을 경기 용인 → **경기 오산**(경부선 위)으로 바꿔 시뮬 여정에서도 경부선 쉼터가 골라지게 함 |
| 같이 고친 버그 | ① 도착 안내 직후 바로 다음 문제를 내던 것 → 다시 출발할 때까지 대기(`GameSession.holdForRest`) ② **내비에 길 안내가 있는 상태에서 다른 목적지로 출발하면 곧바로 요약 화면으로 끝나던 문제** — 이전 경로 취소 직후 `onDrivingInfo` 0/0 이 남은 시간 0(=도착)으로 들어감 → `TripUnits.isNoRoute` 로 무시(이번 작업 전부터 있던 버그) ③ 쉼터 관련 로그의 위치성 정보는 `AppLog.sensitive` 로 |

확인(release APK, 에뮬레이터):
- 시뮬 여정 + 대본 sleepy(데모 모드, 혼자): 출발 52초 뒤 1단계, 약 1분 30초 뒤 2단계 `15분 안에 1단계 두 번` → 북대전 졸음쉼터(호남지선 논산 방향) 28 km → 대본 "네" → 가는 중 카드 → `geo fix` 로 도착 → 주차·재출발 → 인사.
- 시뮬 여정 + "2단계 시험": 오산 졸음쉼터(경부선 부산 방향) 17 km → `geo fix 127.087165 37.156708` → "도착했어요" 발화 뒤 **25초 넘게 문제 없음**("쉼터에서 쉬는 중이에요") → P: 화면 게임 + "오산 졸음쉼터에서 쉬는 중" → D: "아빠, 잘 쉬셨나요? 다시 출발해요!" 끝까지 발화(onDone) → 요약 "졸음쉼터에서 쉬어 가기를 1번 권했어요".
- 실제 내비(서울 → 부산·강릉·전주): TBT 경로 위 쉼터(용인 3.8 km·대신 15 km·오산 9.3 km)가 먼저 골라짐. "경로 위 쉼터 무시"를 켜고 전주 여정: `addWaypoint` → 내비 다음 경유지 "오산 졸음쉼터 36분 · 34km" → `geo fix` → **근접 알림 ENTER** 로 도착 → 계속 → 도착 안내 → 대기 → P → D → 인사.
- 버그 ② 재현 후 수정 확인: 강릉 경로가 있는 상태에서 다른 프리셋으로 출발 → 이전 경로 취소 뒤에도 세션 유지.
- DoD: `./gradlew lintDebug testDebugUnitTest assembleRelease` 통과(lint 오류 0·경고 17), 테스트 145개(+25: 2단계 판단·데모 설정, 쉼터 고르기·문구, 네/아니요, 쉼터 자료, 질문·버튼·인사·도착 후 대기 통합, 내비 0/0).

### 1.11 경로 위 휴게소·졸음쉼터 안내로 개정 (11/7)
계기: 사용자 질문 "졸음쉼터 말고 휴게소도 추천할 수 있지 않나"(다른 AI 가 공공데이터 15045551 을 추천) → 자료를 직접 받아 확인하다가 내비 TBT 에 휴게소가 들어 있고 `distance` 가 구간 거리라는 것, 1.10 의 좌표 경유지가 우회 경로를 만든다는 것을 발견 → 사용자 결정: "전체 반영, 디버그 패널은 그대로".

| 변경 | 내용 |
|---|---|
| 버그 ① TBT 거리 | 첫 졸음쉼터의 구간 거리를 지금부터 거리로 쓰고 있었다(용인졸음쉼터를 18 km 가 아니라 3.8 km 로 말함) → `RouteRestStops.fromTbt` 가 앞 항목부터 누적. 다음 TBT 까지는 남은 거리가 준 만큼 앞당김(`advance`). 빈 TBT 목록은 "경로 정보 없음"(null) |
| 버그 ② 우회 경로 | `addWaypoint` 좌표 경유지 → IC 로 나갔다 들어오는 경로. 경유지 추가·`WaypointArrived`·"경로 위 쉼터 무시" 토글 제거. 경로 위만 권하므로 필요 없음 |
| 고르기 | 경로 정보(내비 TBT·시뮬 경로)가 있으면 경로 위 휴게소·졸음쉼터에서만: 1~80 km 앞 가장 가까운 곳, 휴게소가 5 km 안쪽으로만 더 멀면 휴게소. 경로 위에 없으면 고르지 않음. 경로 정보가 없을 때만 표(1.10 방식) |
| 휴게소 편의시설 | `assets/content/service_areas.json` 247곳 — 도로공사 LOD 휴게소별 편의시설(수유실·수면실…·대표 음식, 영리 포함 자유 이용) + 15142833 가족사랑 화장실(제한 없음), `tools/convert_service_areas.js`. TBT 이름과 정확히 같은 이름만, 상·하행은 방향 도시와 진행 방향으로(모르면 공통 시설만). 말하기: 수면실 → 수유실 → 가족사랑 화장실 중 2개, 화면에는 대표 음식까지 |
| 졸음쉼터 좌표 | 경로 위 졸음쉼터를 공공데이터 표와 이름(괄호·"졸음쉼터" 떼고)으로 맞춰 좌표 → 근접 알림. 진행 방향을 모르면 좌표 없이(반대편 차로 위험) |
| 도착 | 근처(근접 알림 ENTER·위치 반경 300 m)에서 **서거나(STOPPED) 주차** / 주차 / 시뮬 여정이 그 지점을 지남. 옆을 지나가기만 하면 도착 아님 |
| 문구 | "가는 길에 있는 기흥휴게소가 6킬로미터 앞이에요. 수유실과 가족사랑 화장실이 있어요. 거기서 잠깐 쉬어 갈까요?" / "좋아요. 기흥휴게소에서 쉬어 가요." / 버튼 **쉬어 가기**, 멈춤 사유 "쉬어 갈 곳으로 가는 중이에요", 요약 "휴게소·졸음쉼터에서 쉬어 가기를 N번 권했어요" |
| 시뮬 여정 | 부산·강릉·전주 프리셋에 내비 TBT 에서 옮긴 경로 위 목록(22·10·13곳, `DestinationPreset.restStops`, `routeKm`). 서울 → 부산 경유 지점을 내비 길(용인·여주·충주·문경·구미·대구·청도)로 변경. 쉬어 가기를 묻는 동안 시뮬 시간 멈춤(30배속이라 수락 전에 지나치지 않게). 근처 10분은 경로 목록 없음 → 표 |
| 디버그 패널 | 졸음 케어 칸: 휴게소 정보 수, "경로 위"(앞 4곳), "지금 걸리면 권할 곳"(이름·거리·편의시설), 쉬어 가기 상태 |

확인(에뮬레이터, debug 로 상세 로그 → release 로 재확인):
- 실제 내비 TBT 수집: 부산역 22곳(용인졸음쉼터 18.13 km … 덕산졸음쉼터 344.79 km), 강릉역 10곳, 전주 13곳 — 로그 `onTBTInfo 54개 · 경로 위 휴게소 9곳 · 졸음쉼터 13곳`.
- 시뮬 전주 + 2단계 시험: "가는 길에 있는 기흥휴게소가 6킬로미터 앞이에요. 수유실과 가족사랑 화장실이 있어요. 거기서 잠깐 쉬어 갈까요?" → 네 → 묻는 동안 시뮬 정지(수락 때 6.2 km 그대로) → 10초 뒤 `rest stop arrived (시뮬 여정이 쉼 장소에 닿음)` → 도착 안내 → 대기 → P → D → "잘 쉬셨나요?" (debug·release 둘 다).
- 실제 내비 부산 + 2단계 시험: 용인졸음쉼터 18.1 km(용인휴게소는 10 km 더 멀어 졸음쉼터) → 네 → 경로 취소·경유지 없음, `proximity alert registered` → `geo fix 127.138727 37.288616` → `rest stop arrived (위치가 쉼터 0m 안)` → 대기 → P → D → 인사.
- release: `content loaded … serviceAreas=247`, 쉼 장소 관련 로그 `<redacted>`.
- 확인 못 함: 차가 움직이며 근처를 **지나가기만** 할 때 도착이 아닌 것(에뮬레이터 차가 움직이지 않음 — 코드 규칙).
- DoD 통과(lint 오류 0·경고 17), 테스트 152개(+7: TBT 누적·지나친 곳 빼기, 휴게소 우선·1 km·80 km, 편의시설 방향, 졸음쉼터 좌표 방향, 경로 정보 유무, 문구, 휴게소 자료, 시뮬 프리셋).

---

## 2. 추후 작업할 내용 (우선순위 순)

### 2.1 11/15 1차 접수 전 (사람이 해야 할 일)
1. **시연 영상 녹화** — `docs/DEMO_SCENARIO.md` 대본(3분). 녹화 전 체크리스트(권한·포트 포워딩·데모 모드·대본 선택·볼륨) 확인. 녹화 후 README 상단 "시연 영상" 링크 채우기.
2. **콘텐츠 사람 검수** — 지역 퀴즈 170문항·일반 48문항을 팀원이 한 번 더 확인(AI 생성 + 1회 검수 상태).
3. **기획서 접수양식 5번과 README SDK 표 대조** — Vehicle 활용에 "AAOS Car API(기어·속도·주행 규제) + Pleos Vehicle(브레이크·좌석)"로 구체화했으니 기획서 문구도 맞출 것. 졸음 케어 2단계의 SDK 활용도 "`addWaypoint` 경유지 안내" → "NaviHelper TBT 의 경로 위 휴게소·졸음쉼터 + FusedLocation 근접 알림"으로 바뀌었다(11/7, 1.11).
4. 정차 중 초성퀴즈 손보기 -> 사자성어 맞추기도 나쁘지 않을듯? 초성퀴즈 너무 쉬움. 뜻 주고 단어 맞추기가 나을듯 냅둘거면.

### 2.2 12/3 2차 PT 전 (기능, `docs/PHASE2_NOTES.md` 상세)
1. **지역 스탬프** 영속화(DataStore) + 홈 진행도
2. 구간별 일시정지 고도화: `onRouteStateInfo.trafficStatus`(HEAVY), `onCautionPointInfo`, `onCameraAlert` (콜백 자리 `TODO(phase2)`)
3. 이동 앨범(요약 화면 확장), 그림 맞히기(정차 중 화면 게임 2종째)
4. Gleo Interface(`assets/schema.json`: START_GAME / PAUSE_GAME / SKIP_QUESTION) — 1차에는 파일을 넣지 않음
5. 샘플 대조에서 나온 제안: `requestTrip` 으로 프리셋 예상 시간을 실제 내비 값으로, Vehicle SDK 리스너를 화면이 보일 때만 등록 (`PHASE2_NOTES.md` §7)
6. 쉬어 가기 보완 (`PHASE2_NOTES.md` §1): 권한 곳을 지나쳤을 때 재권유, 휴게소 좌표(도로공사 OpenAPI 를 빌드 때) 넣어 근접 알림, 길 안내 없을 때 표 근사에 `onCurrentLocationInfo.heading` 추가

### 2.3 기술 부채·검증 못 한 것
- 통화·Gleo 호출로 인한 `AUDIOFOCUS_LOSS_TRANSIENT` → PAUSED → GAIN 1초 뒤 재출제 흐름을 실차/전화 시뮬레이션으로 검증
- 실차에서 내비가 주행 중 `onTBTInfo`·`onDrivingInfo` 를 스스로 보내는지 확인 (에뮬레이터는 차가 움직이지 않아 확인 불가). 남은 시간은 30초 재조회로 보완했지만 TBT 자동 일시정지는 push 에 의존
- 3분할 자동 배치는 좌핸들·Connect-S 배치(Driving view 왼쪽, 앱 창 1:1)에서만 확인. 우핸들·Connect-W(2:1, 조수석 App library) 에서 동작 확인 필요
- 실제 Suspend(IGN OFF) 진입 시 동작 확인(현재는 onStop 경로로 처리)
- `distractionOptimized` 선언이 Pleos App Market 심사에서 허용되는지 확인(앱 카테고리·DO 승인 절차)
- AAOS `CAR_SPEED` 를 거부했을 때(기어만으로 동작) UI 문구 점검
- 끝말잇기 사전의 비표제어 합성어 정리, STT 동음이의 오인식 사례 수집 후 AnswerMatcher 보정
- lint 경고 17건: 대부분 의존성 최신 버전 안내(Kotlin 2.2.10 호환을 위해 의도적으로 고정)·Pleos SDK 의존성 JNA 의 16KB 정렬 경고 — SDK 측 업데이트 필요
- Android TTS 폴백은 이 에뮬레이터에 한국어 엔진이 없어 실행 검증 못 함
- 계측 테스트(`connectedDebugAndroidTest`)는 작성하지 않음 — 2차 전 Compose UI 테스트 추가 고려
- 졸음 케어 2단계 실차 검증: 주행 중 `onTBTInfo` 를 얼마나 자주 보내는지(그 사이는 남은 거리로 앞당김), 고속 주행에서 근접 알림 ENTER 가 제때 오는지, 근처를 지나가기만 할 때 도착으로 치지 않는지, 권한 곳을 지나쳤을 때(지금은 게임이 계속 쉬고 "계속"으로 이어 감)
- 졸음쉼터 원자료의 방향 표기("A기점 + B종점" → A → B 차로) 해석 확인 — 한국도로공사 문의 또는 실측
- 휴게소 편의시설은 TBT 이름과 정확히 같을 때만 붙인다 — 실차 내비의 휴게소 표기가 자료(예: "하남만남의광장")와 다를 수 있어 실차 TBT 이름 수집 후 보정
- 시뮬 여정은 쉬어 가기를 묻는 동안 멈춘다(30배속 보정). 실제 내비 여정에는 해당 없음

---

## 3. 다시 시작할 때 참고
- 빌드/검사: `./gradlew lintDebug testDebugUnitTest assembleRelease` → `apk/` 에 복사 (`app/build/outputs/apk/release/app-release.apk` → `apk/Team-DonDoAYeon_길동무_release.apk`)
- 에뮬레이터 차량 조작 자동화: `adb forward tcp:8088 tcp:8088` 후
  `curl -X POST "http://127.0.0.1:8088/browser/session?client=cli"` (하트비트, 3초 유효) → `curl -X PUT "http://127.0.0.1:8088/vehicle/gear?gear=Drive"`
- 로그: `adb logcat -s Gildongmu/ModeArbiter:I Gildongmu/GameSession:I Gildongmu/Coordinator:I Gildongmu/NaviHelper:I Gildongmu/Location:I Gildongmu/Window:I Gildongmu/RestCare:I`
- 졸음 케어 2단계 시험: 시뮬 서울 → 전주 출발 → D → 디버그 패널 "2단계 시험" → 대본 응답이 "네" → 기흥휴게소(편의시설) → 시뮬이 닿으면 도착 → 기어 P → D. 실제 내비 + 근접 알림은 `geo fix 127.10 37.40` → 여정 자동 → 서울 → 부산 → 용인졸음쉼터 → `adb emu geo fix 127.138727 37.288616`.
- 자료 다시 만들기: 졸음쉼터 `node tools/convert_rest_areas.js <CSV> app/src/main/assets/content/rest_areas.json`, 휴게소 `node tools/convert_service_areas.js <serviceAreaAmenities.json> <가족사랑화장실.csv> app/src/main/assets/content/service_areas.json`. 시뮬 경로 목록을 다시 받으려면 debug 빌드로 실제 내비 출발 후 logcat `TBT.restStops=`
- 기능별 확인 절차 전체: `docs/FEATURE_GUIDE.md`
- SDK 사용법이 헷갈리면 `pleos-example/` 샘플(Vehicle `VehicleClient.kt`, Navi `NaviHelperViewModel.kt`, FusedLocation `FusedLocationClient.kt`)과 대조
- 에뮬레이터 위치: `adb emu geo fix <경도> <위도>` (예: 서울 시청 `126.9780 37.5665`) → FusedLocation 지역 판정
- 코드 지도: README §5 아키텍처, 주요 진입점 `app/GameCoordinator.kt`, `domain/engine/GameSession.kt`, `domain/engine/ModeArbiter.kt`
