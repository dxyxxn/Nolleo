# 길동무 작업 현황 — 2026-11-06

- 브랜치: `feat/gildongmu-prototype` (main 에서 분기, 아직 main 에 병합·push 하지 않음)
- 기준 지시서: `docs/GILDONGMU_PROTOTYPE_PROMPT.md` (사용자가 `ff8783f` 에서 `.gitignore` 변경과 함께 커밋)
- 변수: 팀명 `Team-DonDoAYeon` · 패키지 `com.gildongmu.app` · 앱 이름 `길동무` · Pleos Client ID/Secret 없음 → TTS/STT `Mode.ON_DEVICE`
- 검증 환경: Pleos Connect Emulator v2.1.0 AVD(cold boot, 2560×1440), Gradle 9.6.0 / AGP 9.5.0 / built-in Kotlin 2.2.10 / compileSdk 37

---

## 1. 작업한 내용

### 1.1 마일스톤별 결과

| 마일스톤 | 커밋 | 내용 | 검증 |
|---|---|---|---|
| M0 환경 | `aa6a894` | Pleos Maven, SDK 5종 + Compose + DataStore + 코루틴, 매니페스트(필요 퍼미션만·allowBackup=false·ROTARY_SUPPORTED=false·distractionOptimized), applicationId/앱 이름, local.properties → BuildConfig | `assembleDebug` 통과, 빈 화면 AVD 실행 |
| M1 주행 상태·모드 | `67cd35b` | AaosCarStateSource(기어·속도·주차브레이크·UX 규제), VehicleSdkClient(Brake·Seat), SimulatedCarStateSource, DriveStateDeriver, ModeArbiter(DO 즉시·1.5초 디바운스·판 마무리 후 SCREEN), ModeController | Virtual Console 기어 P↔D·속도·브레이크 → 2초 안 모드 전환, 테스트 25개 |
| M2 여정 | `1f1209c` | NaviHelperTripSource, SimulatedTripSource(×30 압축·TBT 2회), TripRepository(오류·60초 무응답 폴백), TripUnits, SessionPlanner | `requestRoute` → `onRouteStarted(14820s, 378km)` 수신, 단위=초 확정, 테스트 13개 |
| M3 음성 | `07fda00` | Pleos/Android/대본 엔진, SpeechRouter, AudioFocusHelper, AnswerCollector(지연 측정) | Pleos TTS/STT ON_DEVICE 초기화·발화 완료·STT 타임아웃 측정, 포커스 충돌 없음 |
| M4~M6 게임·졸음·P1·UI | `ea6942f` | 콘텐츠(170+48+40, 사전 3,582), 지역 퀴즈·끝말잇기·초성 퀴즈, GameSession, FusedLocation·지역 판정, DrowsinessMonitor(1단계), TBT 일시정지, 요약 화면, Compose 화면 전부, 디버그 패널, 상태 저장/복원 | 기기에서 전체 흐름 확인(아래 1.2), 테스트 총 101개 |
| M7 마감 | `9f0b89c` | README, DEMO_SCENARIO, PHASE2_NOTES, CONTENT_SOURCES, release 서명 APK, CLAUDE.md 현행화, 이 문서 | `./gradlew lintDebug testDebugUnitTest assembleRelease` 통과(lint 0 errors) |
| 피드백 반영 ① | `f5e6be0` | 게임 끝내기 버튼, 중간 종료 요약 제목, 쉬는 시간 안내를 "약 N분 뒤"로 (아래 1.6) | 에뮬레이터 release 빌드로 확인, DoD 통과, 테스트 102개 |
| 피드백 반영 ② | (이 커밋) | 내비에서 시작한 길 안내를 홈에 실시간 반영, 3분할에서 길동무를 맨 오른쪽 창으로 (아래 1.7) | 에뮬레이터 release 빌드로 확인, DoD 통과, 테스트 115개 |

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
| FusedLocation | 실제 SDK로 수신 (에뮬레이터 기본 GPS가 미국 → 지역 판정은 내비 주소/시뮬 폴백) |
| Pleos TTS | 실제 SDK로 검증 (onDone, 포커스 스택 확인) |
| Pleos STT | 초기화·청취는 실제, 음성 인식 결과는 미검증(호스트 마이크 미사용) → 대본 응답으로 시연 |
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
- **작업 브랜치 `feat/gildongmu-prototype`** 에 커밋 (main 직접 커밋·push 안 함).
- JSON 은 `org.json` 으로 통일(테스트 클래스패스에만 `org.json:json` 추가), DI 는 수동(AppContainer), 영속화는 DataStore(Preferences).
- 오디오 포커스: 음성 판 시작 시 요청, 판 종료·일시정지·백그라운드 시 반납(긴 쉬는 시간 동안 transient 포커스를 붙잡지 않기 위함). 통화 대기(LOSS_TRANSIENT) 중에는 GAIN 을 받으려고 유지.
- Pleos TTS 발화 중 들어온 포커스 손실은 Speech Service 자체 포커스로 보고 무시(관측상 충돌은 없었음).
- 정차(VOICE→SCREEN): "판이 끝날 때까지" 대신 **진행 중인 차례만 끝내고 판을 마무리** — 데모·실사용 모두 10분을 기다리지 않게.
- 판 진행은 문제 수 기준(문제당 약 30초 환산), 판 시작 시점은 "남은 시간이 기준값 이하"로 판단. 시뮬 여정은 주차 중 시간이 흐르지 않음.
- TBT 일시정지: 같은 안내가 20초 넘게 그대로면 정체/정차로 보고 재개(에뮬레이터의 고정 TBT "TURN_LEFT 80m" 대응).
- 졸음 기준값: 캘리브레이션 5개는 중앙값, 무응답(8초)은 캘리브레이션에서 제외, EMA 는 느리지 않은 응답으로만 갱신.
- 데모 모드: 판당 6문제, 운전자 기준값 없으면 1.5초 시드. sleepy 대본은 운전자 차례에만 적용(driverOnly).
- PARKED 판정: 기어를 알면 기어만 본다(에뮬레이터는 D 상태에서도 주차브레이크 텔테일이 켜질 수 있음).

### 1.5 문서·자료와 실제가 달랐던 점 (기획서/문서 수정 참고)
- API 레퍼런스의 `VehicleException.errorCode` → 배포된 Vehicle 2.3.2 AAR 에서는 클래스가 난독화(`callback.exception.a`)되고 `ErrorCode` 가 runtime 스코프라 컴파일 시 사용 불가 → 메시지 기반 재시도로 대체.
- DEV_GUIDE §7.1 의 `adb shell cmd hmg_car_service … driving_regulation` → Emulator 2.1.0(user 빌드)에 서비스 없음. `cmd car_service inject-vhal-event` 도 user 빌드라 막힘. Virtual Console 은 HTTP API(`PUT /vehicle/gear?gear=Drive`, `PUT /vehicle/controls?speed=…`, 브라우저 세션 하트비트 필요)로 스크립트 조작 가능.
- `requestRoute` 를 호출하면 순정 내비가 같은 창 앞으로 나온다. 장거리 프리셋은 샘플에 POI ID 가 없어 **빈 poiId + 좌표·주소**로 요청했고 정상 수락됨.
- 경로가 없을 때도 `onDestinationInfo(remainTime=0, remainDistance=0)` 가 온다 → 무효 값으로 처리.
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

---

## 2. 추후 작업할 내용 (우선순위 순)

### 2.1 11/15 1차 접수 전 (사람이 해야 할 일)
1. **시연 영상 녹화** — `docs/DEMO_SCENARIO.md` 대본(3분). 녹화 전 체크리스트(권한·포트 포워딩·데모 모드·대본 선택·볼륨) 확인. 녹화 후 README 상단 "시연 영상" 링크 채우기.
2. **브랜치 병합·push** — `feat/gildongmu-prototype` → main 병합 후 GitHub push. Private 저장소면 심사용 계정을 Collaborator 로 초대. `docs/GILDONGMU_PROTOTYPE_PROMPT.md` 는 `ff8783f` 에서 커밋됨 — 공개 저장소에 그대로 둘지 push 전에 확인.
3. **서명 키 백업** — `C:\Users\rlaeh\.android\gildongmu-release.jks` 와 `local.properties` 의 비밀번호를 팀 비밀 저장소에 보관. 최종(2027/2) APK 도 같은 키로 서명해야 한다.
4. **콘텐츠 사람 검수** — 지역 퀴즈 170문항·일반 48문항을 팀원이 한 번 더 확인(AI 생성 + 1회 검수 상태).
5. **기획서 접수양식 5번과 README SDK 표 대조** — Vehicle 활용에 "AAOS Car API(기어·속도·주행 규제) + Pleos Vehicle(브레이크·좌석)"로 구체화했으니 기획서 문구도 맞출 것.
6. (선택) 실제 마이크 시연 — 에뮬레이터 Microphone 을 호스트 입력으로 켜고 응답 입력 "마이크"로 Pleos STT 인식 품질 확인.
7. (선택) Pleos Playground Client ID/Secret 을 받으면 `local.properties` 에 넣어 HYBRID 모드 품질 비교.
8. (결정 필요) 판 간격 — 지금은 약 45분마다 한 판(4시간 10분 여정이면 판 사이 약 39분 휴식). 아이와 함께 갈 때 길게 느껴지면 `SessionPlanner.SLOT_SEC` 를 줄이거나 "쉬지 않고 이어 하기" 옵션을 추가.
9. (결정 필요) 내비를 앱 영역 전체로 볼 때도 게임을 음성으로 이어 갈지 — 지금은 길동무가 화면에서 사라지면 일시정지한다(§5.4 백그라운드 정책에 맞춘 선택). 이어 가려면 포그라운드 서비스·알림·백그라운드 마이크 정책 검토가 필요.

### 2.2 12/3 2차 PT 전 (기능, `docs/PHASE2_NOTES.md` 상세)
1. 졸음 케어 2단계(15분 내 Stage1 2회 또는 느림+정답률<50%) → 휴식 권유 → 졸음쉼터 탐색(오프라인 테이블 + FusedLocation) → `NaviHelper.addWaypoint` 길 안내 → `FusedLocation.addProximityAlert` 도착 감지
2. 지역 스탬프 영속화(DataStore) + 홈 진행도
3. 구간별 일시정지 고도화: `onRouteStateInfo.trafficStatus`(HEAVY), `onCautionPointInfo`, `onCameraAlert` (콜백 자리 `TODO(phase2)`)
4. 이동 앨범(요약 화면 확장), 그림 맞히기(정차 중 화면 게임 2종째)
5. Gleo Interface(`assets/schema.json`: START_GAME / PAUSE_GAME / SKIP_QUESTION) — 1차에는 파일을 넣지 않음

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

---

## 3. 다시 시작할 때 참고
- 빌드/검사: `./gradlew lintDebug testDebugUnitTest assembleRelease` → `apk/` 에 복사 (`app/build/outputs/apk/release/app-release.apk` → `apk/Team-DonDoAYeon_길동무_release.apk`)
- 에뮬레이터 차량 조작 자동화: `adb forward tcp:8088 tcp:8088` 후
  `curl -X POST "http://127.0.0.1:8088/browser/session?client=cli"` (하트비트, 3초 유효) → `curl -X PUT "http://127.0.0.1:8088/vehicle/gear?gear=Drive"`
- 로그: `adb logcat -s Gildongmu/ModeArbiter:I Gildongmu/GameSession:I Gildongmu/Coordinator:I`
- 코드 지도: README §5 아키텍처, 주요 진입점 `app/GameCoordinator.kt`, `domain/engine/GameSession.kt`, `domain/engine/ModeArbiter.kt`
