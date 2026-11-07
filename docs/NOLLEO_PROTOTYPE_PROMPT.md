# 놀레오(Nolleo) 프로토타입 구현 지시서 — Claude Code용

> 2026-11-07 앱 이름을 **놀레오(Nolleo)**, 부제를 "가는 길이 놀이가 되는 가족 게임"으로 바꾸면서 이 문서의 앱 이름·패키지(`com.nolleo.app`)·APK 파일명 표기도 함께 고쳤다. 11/8 팀명 표기를 `돈도아연`(영문 DonDoAYeon), APK 파일 이름을 `apk/돈도아연_놀레오_release.apk` 로 확정했다. 나머지 지시 내용은 처음 그대로다.

> **이 파일은 Claude Code에게 주는 작업 지시서다.** 리포에는 루트의 `CLAUDE.md`(자동 로드)와 `docs/pleos/PLEOS_CONNECT_DEV_GUIDE.md`, `docs/pleos/PLEOS_CONNECT_API_REFERENCE.md` 세 파일이 있다는 전제 위에서 쓴다. 경로가 다르면 실제 리포 기준으로 찾아라. 공모전 규칙(일정·심사 기준·제출물)은 별도 파일이 없으므로 **이 문서 §1.1과 §12가 유일한 출처**다.
>
> 목표: **현대자동차그룹 차량용 앱 개발 공모전 1차 접수(11/15)** 에 낼 **동작하는 프로토타입 + 시연 영상 + GitHub 저장소(소스·APK)** 를 만든다. 가산점은 "실제로 돌아가는 것"에서 나온다. 기능 폭보다 **에뮬레이터에서 크래시 없이 끝까지 시연되는 것**이 우선이다.

---

## 작업 전 채워 넣을 변수

아래 값을 이 문서 안에서 치환한 뒤 작업을 시작하라. (치환이 안 되어 있으면 작업 시작 시 사용자에게 한 번만 물어보고, 답이 없으면 기본값으로 진행하고 보고서에 명시.)

| 변수 | 의미 | 기본값 |
|---|---|---|
| `{{TEAM_NAME}}` | 팀명 (README에 사용) | `돈도아연` |
| `{{PACKAGE}}` | applicationId / 루트 패키지 | `com.nolleo.app` |
| `{{APP_NAME}}` | 앱 표시명 | `놀레오` |
| `{{PLEOS_CLIENT_ID}}`, `{{PLEOS_CLIENT_SECRET}}` | Pleos Playground 프로젝트의 TTS/STT 서버용 자격증명. **코드에 넣지 말고 `local.properties`로만** | 없음 → `Mode.ON_DEVICE` 사용 |

---

## 0. 코드를 쓰기 전에 반드시 할 일

1. `CLAUDE.md` 전체를 읽는다(자동 로드됨). 공모전 배경·심사 기준·저장소 규칙은 `CLAUDE.md`의 "Project goal" 절과 이 문서 §1.1·§12에 있다. 이 문서와 `CLAUDE.md`가 충돌하면 빌드·툴체인은 `CLAUDE.md`, 기능 범위는 이 문서를 따른다.
2. `docs/pleos/PLEOS_CONNECT_DEV_GUIDE.md`는 **전체를 읽지 말고** 다음 절만 연다: §0(TL;DR), §5.4(멀티 윈도우), §7.1(주행 규제), §7.2(오디오 포커스), §7.3(Suspend), §7.4(CCP Back), §7.7(TTS 선행 묵음), §8.0~8.5(SDK 5종), §10.2(Virtual Console), §14(체크리스트).
3. `docs/pleos/PLEOS_CONNECT_API_REFERENCE.md`는 **grep으로만** 본다. 이 문서에 적힌 시그니처가 의심스러우면 레퍼런스가 이긴다.
4. 샘플 앱의 핵심 파일 3개를 읽는다 (`CLAUDE.md`의 "Key files to read" 목록): `VehicleClient.kt`, `NaviHelperViewModel.kt`, `FusedLocationClient.kt`. 각 SDK의 초기화·리스너·해제 패턴을 그대로 따른다. 샘플의 **퍼미션 목록은 복사하지 않는다.**
5. 루트 프로젝트 `./gradlew assembleDebug`가 지금 통과하는지 확인한다. 실패하면 기능 작업 전에 먼저 고친다.
6. 아래 §11 마일스톤 M0가 끝나면 **파일 트리와 진행 계획을 짧게 출력**하고 바로 M1로 진행한다. 승인을 기다리느라 멈추지 마라. 결정이 필요한 애매한 사항은 §13의 원칙으로 스스로 정하고 보고서에 적는다.

---

## 1. 서비스 개요 (기획서 요약 — 이것이 "무엇을 만드는가"다)

**놀레오 — 가는 길이 놀이가 되는 가족 게임**
- 대상: 가족과 장거리 이동하는 운전자와 동승자 (어린 자녀가 있는 가족, 명절·휴가철·주말 나들이).
- 문제: 장거리 이동에서 운전자는 함께 놀 수 없고 졸음 위험이 있으며, 동승자는 지루해져 스마트폰에 의존한다. 기존 게임·영상은 주행 중인지, 도착까지 얼마나 남았는지, 어디를 지나는지 모르고, 운전자가 화면을 보면 안전 문제가 된다.
- 해결: **차가 이미 알고 있는 주행 상태, 남은 시간, 현재 위치를 게임에 연결**한다.
  - 주행 중에는 **음성으로**, 정차 중에는 **화면으로** 즐기는 가족용 차량 게임.
  - 남은 이동 시간과 지나는 지역에 맞춰 구성이 달라지는 콘텐츠.
  - 게임 응답에서 **운전자의 졸음 징후**를 살피고 휴식·길 안내로 연결.

**주요 기능 (기획서 원문 6개)**
1. 주행 상태별 모드 전환 — 주행 중: 끝말잇기·퀴즈 등 음성 전용 게임, 화면 조작 잠금 / 정차 중: 휴게소 등에서 차가 멈추면 화면 게임 개방.
2. 도착 시간 맞춤 진행 — 남은 이동 시간에 맞춰 한 판의 길이를 조절하고 도착 직전에 결과 정리.
3. 지역 퀴즈와 지역 스탬프 — 지나는 지역 관련 문제(예: 충주를 지날 때 충주 특산물), 맞힌 지역은 스탬프로 축적.
4. 구간별 자동 일시정지 — 길 안내 직전이나 정체처럼 운전에 집중해야 하는 순간 게임이 자동으로 멈추고 여유로워지면 이어서 진행.
5. 참여형 졸음 케어 — 운전자의 응답 속도와 정답률을 평소 기록과 비교. 반응이 눈에 띄게 느려지면 가볍게 말을 걸고, 계속되면 휴식 권유. **게임 결과만으로 졸음을 단정하지 않는 보조 기능.**
6. 졸음쉼터 안내 — 휴식을 권할 때 가까운 졸음쉼터를 찾아 길 안내로 연결.
- 부가: 오늘의 이동 앨범 (도착 후 경로 위에 게임 결과·가족 점수 정리).

**사용 시나리오 (서울→부산 약 4시간)**: 목적지 설정 → 예상 소요 시간에 맞춰 오늘의 게임 구성 → 고속도로 주행 중 화면 잠김, 운전자·동승자가 번갈아 음성으로 지역 퀴즈 → 분기점 안내 구간에서 자동 일시정지 후 재개 → 운전자 응답이 느려지면 말 걸기, 계속되면 졸음쉼터 안내 → 휴게소 정차 시 화면 게임 → 도착 직전 마지막 판 종료, 이동 앨범으로 정리.

**기획서의 SDK 활용 계획 (코드와 README가 이것과 일치해야 한다)**

| SDK | 활용 |
|---|---|
| Vehicle (+ AAOS Car API) | 주행·정차 상태를 읽어 음성/화면 모드 전환, 운전 집중이 필요한 순간 게임 일시정지 |
| NaviHelper | 남은 시간·경로로 게임 길이 조절, 안내 시점에 맞춰 일시정지, 졸음쉼터 안내를 길 안내와 연동 |
| FusedLocation | 현재 위치로 지역 퀴즈 선택, 지역 스탬프 기록, 가까운 졸음쉼터 탐색 |
| SpeechToText | 운전자·동승자의 답변 인식, 응답에 걸린 시간 측정 |
| TextToSpeech | 문제 출제, 결과 안내, 휴식 권유 |

**졸음 케어 판단 방식**: 평소 응답 시간과 정답률을 운전자별 기준값으로 저장 → 1단계: 응답 시간이 기준보다 길어지면 가볍게 말을 걸고 난이도를 낮춤 → 2단계: 느린 응답과 오답이 반복되면 휴식을 권하고 가까운 졸음쉼터 안내.

**기획서에 적힌 구현 범위**: Android API 28 이상, Kotlin, Pleos Connect 에뮬레이터 구동 검증. **1차 제출: 음성 퀴즈와 끝말잇기, 주행 상태별 모드 전환, 졸음 케어 1단계.** 2차 PT 전: 졸음 케어 2단계, 졸음쉼터 안내, 지역 퀴즈·스탬프, 구간별 일시정지를 더해 실제 동작하는 앱으로 완성. 이동 앨범은 여유가 있을 때.

### 1.1 공모전 규칙 요약 (현대자동차그룹 차량용 앱 개발 공모전 2026 — 참가 가이드 기준)

리포에 별도 파일이 없으므로 여기에 적는다. 구현 우선순위와 제출물 형식은 전부 이 절에서 나온다.

- **일정(KST)**: 11/15(일) 1차 접수 마감(기획서 + 시연 영상 + GitHub 링크) → 11/23 서류 결과 → **12/3~4 2차 오프라인 PT(이때까지 실제 동작하는 앱 완성 필수)** → 12/18 최종 10팀 발표 → 2027/2 그룹 행사 전시·시연(최종 구동용 APK 제출). 이번 작업은 **11/15 제출분**이다.
- **심사 기준 4항목(배점 비공개) → 설계 우선순위**: ① 차량특화성(폰 앱이 못 하는 가치, 주행/정차 맥락·차량 기능과의 결합) ② 창의성(Pain point와 해결 방식의 명확함) ③ 기술 활용성(**Pleos SDK가 핵심 기능의 중심**이어야 함, 장식 금지) ④ 사업성(기획서 담당). 코드로 보여줄 것은 ①과 ③이다: 차량 상태에 반응하는 기능을 핵심으로, SDK 호출이 기능의 중심에 있도록.
- **가산점**: 시연 영상(목업·프로토타입·화면 녹화 인정) + GitHub 저장소(소스 + APK). 이 작업의 존재 이유다.
- **개발 환경 요구**: Android API 28+(minSdk 28), Gradle 8.0+, Java/Kotlin, Pleos Connect Emulator 구동 검증. 사용 가능 SDK: Vehicle / NaviHelper / TextToSpeech / SpeechToText / FusedLocation (TTS·STT·FusedLocation은 Playground 신청 상태에 따라 미승인일 수 있음 → §5의 폴백이 필요한 이유).
- **저장소 규칙**: 소스 코드 + **APK는 `apk/` 폴더**에(용량 초과 시 GitHub Releases + Release URL), 파일명 **`돈도아연_놀레오_release.apk`**, **실행 가능한 release 빌드**. README 필수 항목은 §12. Private 저장소면 심사용 계정을 Collaborator로 초대(사용자가 함). **심사자가 README만 보고 빌드·실행할 수 있어야 한다.**
- **접수양식 5번(개발 계획)에 Pleos Connect 구현 방식·개발 범위·SDK 사용 계획을 적는다** → 코드 구조·README의 SDK 활용 표가 기획서 §"기술 활용성" 표와 일치해야 한다. 임의로 SDK 역할을 바꾸지 말고, 바꿔야 하면 보고서에 적어 기획서를 고칠 수 있게 한다.
- 최종 순위는 2차 PT 시연 결과 기준 → 지금 만드는 구조가 2차에서 그대로 확장 가능해야 한다(§2.3의 확장 지점).

---

## 2. 이번 작업의 구현 범위

### 2.1 P0 — 반드시 구현 (1차 제출용 프로토타입)

| ID | 기능 | 완료 기준 |
|---|---|---|
| A | **여정 설정 & 트립 컨텍스트** | 프리셋 목적지 선택 → `NaviHelper.requestRoute` → `onDestinationInfo`의 `remainTime`/`remainDistance`를 받아 **세션 플랜**(판 수·판 길이)을 만든다. NaviHelper 이벤트가 안 오면 시뮬레이션 트립으로 자동 대체. |
| B | **주행 상태별 모드 전환** | `PARKED`/`STOPPED`/`DRIVING` + `CarUxRestrictions`를 조합해 `VOICE`/`SCREEN`/`PAUSED` 모드를 결정. Virtual Console에서 기어 P↔D를 바꾸거나 `driving_regulation on/off`를 치면 **2초 내 화면이 바뀐다.** |
| C | **음성 게임 2종** | (1) 지역 퀴즈: 현재 위치의 시/도 기준 문제 → TTS 출제 → STT 응답 → 판정 → TTS 결과. 위치 불가 시 일반 가족 퀴즈. (2) 끝말잇기: 앱↔운전자↔동승자 순서, 사전 검증, 두음법칙. |
| D | **졸음 케어 1단계** | 운전자 차례의 응답 지연(TTS 끝 → STT 첫 인식)을 측정·저장, 기준값 대비 느려지면 가볍게 말 걸고 난이도 하향. 디버그 패널에서 수치 확인 가능. |
| E | **데모/시뮬레이션 모드** | SDK가 하나라도 불가한 환경(일반 에뮬레이터, 내비 미탑재, STT 미승인)에서도 **같은 UI 흐름이 끝까지 시연**되도록 각 입력 소스에 Simulated 구현을 둔다. 실제 SDK 경로가 1순위, 시뮬레이션은 빈 곳만 메운다. |
| F | **제출물** | `README.md`(공모전 필수 항목), `apk/돈도아연_놀레오_release.apk`, `docs/DEMO_SCENARIO.md`(3분 시연 대본). |

### 2.2 P1 — P0가 안정된 뒤 시간이 남으면 (우선순위 순)

1. **TBT 기반 자동 일시정지(간단 버전)**: `onTBTInfo`의 첫 항목이 분기/진출입/회전이고 `distance` ≤ 500 m이면 `PAUSED`, 지나면 재개. (기획서상 2차 기능이지만 NaviHelper 이벤트만 연결하면 되므로 싸다.)
2. **정차 중 화면 게임 1종**: 초성 퀴즈(터치로 선택지 고르기). `SCREEN` 모드에서만 열린다.
3. **도착 직전 결과 정리 화면**: 남은 시간 ≤ 5분이면 진행 중인 판을 마무리하고 요약 화면(점수·정답 수·지나온 지역 목록)으로 전환. 이동 앨범의 자리이기도 하다.

### 2.3 2차(12/3 PT) 범위 — **설계만 하고 구현하지 않는다**

졸음 케어 2단계 + 졸음쉼터 안내(`NaviHelper.addWaypoint`, `FusedLocation.addProximityAlert`), 지역 스탬프 영속화, 구간별 일시정지 고도화(정체 `TrafficCongestionType.HEAVY`, `onCautionPointInfo`, `onCameraAlert`), 이동 앨범, 그림 맞히기, Gleo Interface(`assets/schema.json`). 코드에는 인터페이스·확장 지점(`TODO(phase2)`)만 남긴다. 반쯤 만든 2차 기능은 데모에서 사고만 낸다.

---

## 3. 핵심 도메인 모델과 상태 머신

이 앱의 "차량특화성"은 전부 이 상태 머신에서 나온다. 가장 먼저, 가장 꼼꼼하게 만들고 단위 테스트를 붙여라.

```
// 차량 입력에서 파생되는 상태 (core/car)
enum class DriveState { PARKED, STOPPED, DRIVING, UNKNOWN }
//  PARKED  : gear == PARK  (또는 parkingBrake == true && speed ≈ 0)
//  STOPPED : gear != PARK && speed < 0.5 m/s 가 3초 이상 지속  (신호대기·정체)
//  DRIVING : speed ≥ 0.5 m/s
//  UNKNOWN : Car API 연결 실패 → 안전 쪽(=VOICE 모드, 화면 잠금)으로 처리

data class UxState(val requiresDO: Boolean, val activeRestrictions: Int)   // CarUxRestrictionsManager

// 앱이 결정하는 모드 (domain/engine/ModeArbiter)
enum class GameMode { IDLE, VOICE, SCREEN, PAUSED }
//  IDLE   : 여정 없음 / 세션 종료
//  VOICE  : DRIVING || STOPPED || requiresDO  → 음성만, 화면은 glanceable 상태판, 터치는 '일시정지/계속' 1개
//  SCREEN : PARKED && !requiresDO            → 화면 게임 허용
//  PAUSED : VOICE 중 주의가 필요할 때 → 길 안내 임박(P1), 오디오 포커스 상실(통화·Gleo), 사용자 일시정지

규칙
 1. requiresDO == true 면 어떤 경우에도 SCREEN 금지. (플랫폼 규제가 기어 신호보다 우선)
 2. DriveState 전환은 1.5초 디바운스. STOPPED→DRIVING 처럼 짧게 흔들리는 신호로 모드가 깜빡이면 안 된다.
 3. PAUSED 진입 시 진행 중인 TTS/STT를 즉시 멈추고(현재 문제는 보존), 해제되면 같은 문제를 다시 출제한다.
 4. SCREEN→VOICE 전환(출발)은 화면 게임을 즉시 닫고 음성 세션을 이어 간다. VOICE→SCREEN(정차)은 음성 판을 끝낸 뒤 전환한다.
 5. 모든 전환은 로그(`ModeArbiter: DRIVING+DO → VOICE (reason=...)`)로 남긴다. 데모 영상에서 logcat을 같이 보여줄 수 있다.
```

```
// 트립 컨텍스트 (core/navi)
data class TripContext(
  val destinationName: String,
  val remainTimeSec: Int?, val remainDistanceM: Int?,   // null = 아직 모름
  val navigationStatus: NavigationStatus?,              // ROUTE_DRIVE / FREE_DRIVE ...
  val nextTurn: TurnHint?,                              // TBTInfo 첫 항목 요약 (P1)
  val regionCode: String?,                              // 현재 시/도 코드 (FusedLocation → RegionResolver)
  val source: TripSourceKind                            // REAL_NAVI / SIMULATED
)

// 세션 플랜 (domain/engine/SessionPlanner)
data class SessionPlan(val rounds: List<RoundPlan>)
data class RoundPlan(val index: Int, val game: GameKind, val targetDurationSec: Int, val questionCount: Int)
//  규칙: 판 길이 6~10분, 판 사이 2분 휴지(말 안 거는 시간), 마지막 판은 도착 5분 전에 끝나야 함.
//  remainTime이 ±20% 이상 바뀌면 남은 판만 다시 계산(진행 중인 판은 유지).
//  remainTime < 12분이면 "짧은 판" 1개만.
```

```
// 플레이어 & 턴
data class Player(val id: Int, val name: String, val isDriver: Boolean)
//  운전자 1 + 동승자 0~3. 턴은 라운드 로빈. 퀴즈는 "이번엔 ○○ 차례!"로 TTS가 지목한다.
//  STT는 화자를 구분하지 못하므로 '지목된 사람이 답했다'고 가정한다. 졸음 지표는 운전자 차례의 응답만 쓴다.
```

---

## 4. 아키텍처와 패키지 구조

- 단일 모듈 `:app`, Kotlin, MVVM + 단방향 상태(`StateFlow`), 코루틴. 모든 SDK 콜백은 `callbackFlow`/`suspendCancellableCoroutine`으로 감싸서 도메인 계층은 Flow만 본다.
- **DI는 수동**(`AppContainer` 하나). Hilt/kapt는 넣지 않는다(루트 툴체인이 AGP 9 built-in Kotlin이라 kapt가 걸림돌이 된다). Room도 쓰지 않는다 — 영속화는 `DataStore(Preferences)` + JSON 파일이면 충분하다.
- JSON: `kotlinx.serialization` 플러그인이 현재 툴체인에서 깔끔히 적용되면 사용, 아니면 `org.json`으로 수동 파싱. 둘 중 하나로 통일.
- **UI 툴킷**: Jetpack Compose + Material3를 1순위로 시도한다(샘플 앱과 같다). 단, 루트 프로젝트 툴체인(AGP 9.5 / compileSdk 37 / built-in Kotlin)에서 Compose 컴파일러 설정이 **1시간 안에 빌드까지 안 되면** Views(ViewBinding + Navigation Component + Material)로 간다. 어느 쪽이든 §8의 디자인 규칙은 같다. 결정을 보고서에 적는다.
- AAOS Car API: `android { useLibrary("android.car") }`. compileSdk 37 플랫폼에 `optional/android.car.jar`가 없으면 compileSdk 35(샘플과 동일)로 내린다 — `CLAUDE.md`의 "build config는 리포가 이긴다"는 규칙의 유일한 예외이며, 바꾸면 보고한다.
- SDK 의존성은 `gradle/libs.versions.toml`에 추가하고 `libs.*`로 참조 (`CLAUDE.md` "Adding a Pleos SDK" 절차 그대로): `Vehicle:2.3.2`, `NaviHelper:3.0.5`, `TextToSpeech:3.3.1`, `SpeechToText:3.3.1`, `FusedLocation:1.1.3`.

```
{{PACKAGE}}
├─ app/            NolleoApp(Application: SDK 싱글톤 소유·release), AppContainer(DI), MainActivity(단일 액티비티)
├─ core/
│  ├─ car/        CarStateSource(interface) · AaosCarStateSource(CarPropertyManager+CarUxRestrictionsManager)
│  │              · VehicleSdkClient(Pleos Vehicle: Brake, Seat occupancy) · SimulatedCarStateSource
│  ├─ navi/       TripSource(interface) · NaviHelperTripSource · SimulatedTripSource · TripUnits(단위 변환 한 곳)
│  ├─ location/   LocationSource(interface) · FusedLocationSource · SimulatedLocationSource · RegionResolver
│  └─ speech/     SpeechEngine(interface) · PleosSpeechEngine(TTS/STT SDK) · AndroidSpeechEngine(플랫폼 폴백)
│                 · ScriptedSpeechEngine(데모·테스트) · AudioFocusHelper
├─ domain/
│  ├─ model/      DriveState, UxState, GameMode, TripContext, SessionPlan, Player, Question, Answer, Turn
│  ├─ engine/     ModeArbiter · SessionPlanner · GameSession(게임 루프) · AnswerMatcher
│  ├─ games/      Game(interface) · RegionQuizGame · WordChainGame · ChosungQuizGame(P1)
│  └─ drowsy/     ResponseSample · DriverBaselineRepo · DrowsinessMonitor(1단계)
├─ data/          ContentRepository(assets JSON) · WordDictionary · SettingsRepo(DataStore) · ProfileRepo
└─ ui/            theme/ · home/(TripSetup) · voice/(VoiceGameScreen) · screen/(ScreenGameScreen)
                  · pause/(PausedCard, RestSuggestionCard) · summary/ · debug/(DebugPanel) · common/
```

입력 소스 3종(`CarStateSource`, `TripSource`, `LocationSource`)과 `SpeechEngine`은 **런타임에 Real → Fallback → Simulated 순으로 자동 선택**되고, 디버그 패널에서 강제 전환할 수 있다. 어떤 구현이 선택되었는지 항상 UI(디버그 패널)와 로그에 보인다.

---

## 5. SDK 통합 상세

공통: `CLAUDE.md`의 "Pleos SDK integration pattern"을 그대로 지킨다 — `applicationContext`로 1회 생성 → `initialize()` → 리스너 등록 → 호출 → 리스너 해제(같은 인스턴스) → `release()`. 콜백이 끝나기 전에 `release()` 금지. 초기화 실패는 **예외가 아니라 상태**다(`SdkStatus.UNAVAILABLE(reason)`) — 앱은 절대 죽지 않고 폴백으로 간다.

### 5.1 주행 상태 — AAOS Car API(표준 데이터) + Pleos Vehicle SDK(보조)

DEV_GUIDE §5.6/§8.1 주석대로 **속도·기어는 AAOS `CarPropertyManager`**, Pleos Vehicle SDK는 OEM 특화 보조 신호에 쓴다.

- `Car.createCar(context)` → `getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager`
  - `VehiclePropertyIds.GEAR_SELECTION` (Int, `VehicleGear.GEAR_PARK` 비교) — 콜백 등록(`registerCallback`, rate ONCHANGE)
  - `VehiclePropertyIds.PERF_VEHICLE_SPEED` (Float m/s) — rate 1 Hz 이하. **폴링 금지**, 콜백만.
  - `VehiclePropertyIds.PARKING_BRAKE_ON` (Boolean) — 있으면 PARKED 보조 신호
  - 퍼미션: `android.car.permission.CAR_POWERTRAIN`(기어·주차브레이크), `android.car.permission.CAR_SPEED`(속도, **dangerous → 런타임 요청**). 정확한 문자열은 `pleos-example/pleos-sample-vehicle-app/app/src/main/AndroidManifest.xml`에서 grep 해서 확인하고, 없으면 AAOS 공식 상수명을 쓴다. 거부되면 속도 없이 기어만으로 동작(STOPPED 판정 불가 → DRIVING으로 간주).
- `Car.CAR_UX_RESTRICTION_SERVICE` → `CarUxRestrictionsManager.registerListener { r -> UxState(r.isRequiresDistractionOptimization, r.activeRestrictions) }` (퍼미션 불필요). `UX_RESTRICTIONS_NO_KEYBOARD` 비트가 켜지면 디버그 패널의 텍스트 입력을 막고, `LIMIT_STRING_LENGTH`면 화면의 문제 텍스트를 1줄 요약으로 줄인다. 지역을 하드코딩하지 말고 받은 비트만 반영한다.
- Pleos Vehicle SDK (`ai.pleos.playground:Vehicle:2.3.2`):
  - `vehicle.getBrake()` → `checkBrakePedalStatusCapabilities` → 지원 시 `registerBrakePedalStatus(BrakePedalStatusListener)` : STOPPED 판정의 보조 신호(속도 0 + 브레이크 on). 퍼미션 `pleos.car.permission.CAR_DRIVE`.
  - `vehicle.getSeat()` → `checkSeatOccupancyCapabilities` → 지원 영역의 `getCurrentSeatOccupancy(SeatArea, ...)` : 조수석·2열 착좌 수로 **플레이어 수를 자동 제안**("동승자 2명이 감지됐어요"). 퍼미션 `pleos.car.permission.CAR_SEATS`. 미지원이면 조용히 수동 선택만.
  - 매니페스트에는 위 두 퍼미션만 선언한다. 다른 `CAR_*`/`CONTROL_CAR_*`는 넣지 않는다.
- 에뮬레이터 검증: Virtual Console(`adb forward tcp:8088 tcp:8088` → http://127.0.0.1:8088/) Motion > Gear Selection P/D, Brake Pedal; Convenience > Seat Occupancy. 주행 규제: `adb shell cmd hmg_car_service --service drivingstate driving_regulation on|off`. AVD는 **cold boot**.

### 5.2 NaviHelper — 남은 시간·경로·안내 시점

- 초기화: `NaviHelper(applicationContext).initialize(onConnected = { addListener(listener) ; getRouteStateInfo(); getDestinationInfo() }, onFailed = { ... UNAVAILABLE })`.
- 리스너(`NaviHelperEventListener`, 필요한 것만 override):
  - `onRouteStarted(RouteStartInfo)` — `duration`, `distance`, `name` → 플랜 초기값
  - `onRoutingCompleted()` — 경로 탐색 완료. `requestRoute` 성공 반환 ≠ 탐색 완료.
  - `onDestinationInfo(DestinationInfo)` — `remainTime`, `remainDistance`, `name` → `TripContext` 갱신, 플랜 재계산 트리거
  - `onDrivingInfo(DrivingInfo)` — `destination.duration/distance` (보조)
  - `onNavigationStatus(NavigationStatus)` — `ROUTE_DRIVE`일 때만 "여정 진행 중"
  - `onTBTInfo(List<TBTInfo>)` — 첫 항목 `type: TurnType`, `distance: Double`, `description` → `nextTurn` (P1 일시정지)
  - `onCurrentLocationInfo(CurrentLocationInfo)` — `address` 문자열에서 시/도 파싱 → FusedLocation 폴백용 지역 추정
  - `onDestinationArrived`, `onRouteCancelled`, `onError(NaviErrorCode)`
  - 2차용(지금은 override만 비워 두고 TODO): `onCautionPointInfo`, `onRouteStateInfo(trafficStatus)`, `onCameraAlert`
- 호출: `requestRoute(RouteInfo(latitude=, longitude=, poiName=, poiId=, poiSubId=, address=))` — **Kotlin named argument 필수**(좌표 순서 변경 이력 있음). 프리셋 목적지의 `poiId`는 `pleos-example/pleos-sample-navi-app/.../data/sample/`에 있는 실제 샘플 값을 재사용한다. 임의 문자열 poiId는 `onError`가 날 수 있다.
- 퍼미션: `pleos.car.permission.NAVI_ROUTE`, `pleos.car.permission.NAVI_ROUTE_SEARCH`. `NAVI_CUSTOM_MAP`은 쓰지 않는다.
- **단위 주의**: `remainTime`/`duration`이 초인지 분인지 문서에 없다. `TripUnits` 한 곳에서 변환하고, 에뮬레이터에서 실제 값을 로그로 관측해 결정한 뒤 주석으로 근거를 남긴다. 추측해서 흩뿌리지 마라.
- 에뮬레이터에 순정 내비가 없거나 이벤트가 안 오면 `TripSource`는 60초 안에 `SimulatedTripSource`로 전환하고 UI에 "시뮬레이션 여정" 배지를 띄운다. 이때도 `NaviHelperTripSource` 코드는 그대로 남아 있어야 한다(심사 대상).

### 5.3 FusedLocation — 지역 퀴즈 선택

- `FusedLocation(context)` → `isSupported()`가 true일 때만 `initialize()` → `registerFusedLocationCallback(FusedLocationListener)` (`onChanged(LocationData?)`의 null은 무시, `onError` 구현 필수). 종료 시 `unregister` → `release()` (둘은 서로 대체 불가).
- 퍼미션 `pleos.car.permission.FUSED_LOCATION`은 **dangerous → 런타임 요청 흐름** 구현. 거부되면 위치 기능만 끈다(일반 퀴즈로).
- `RegionResolver`: 위경도 → 시/도 코드. 정밀 폴리곤은 필요 없다. **17개 시/도 바운딩 박스 + 경계 겹침 시 중심점 거리**로 판정하는 오프라인 테이블(`assets/content/regions.json`). 폴백 순서: FusedLocation → NaviHelper `onCurrentLocationInfo.address` 문자열 → `SimulatedLocationSource` → 지역 없음(일반 퀴즈).
- `adb emu geo fix <lon> <lat>`는 Android 위치이지 FusedLocation SDK 값이 아닐 수 있다. 관측해서 안 바뀌면 시뮬레이션 위치로 시연한다.

### 5.4 TextToSpeech / SpeechToText — 게임의 입출력

- TTS: `TextToSpeech(applicationContext, mode)` → `initialize(OnInitializeListener)` → `addEventListener(EventListener)` → `setLocale(Locale.KOREA)` → (자격증명 있을 때만) `registerApp(clientId, clientSecret, OnServerConnectionListener)` → `speak(text, isFinal = true)` → `onDone`. `mode`는 `BuildConfig`에 Client ID/Secret이 있으면 `Mode.HYBRID`, 없으면 `Mode.ON_DEVICE`. 발화 전 **150~200 ms 선행 묵음**(§7.7) — SDK가 알아서 하는지 확인하고, 아니면 첫 발화 앞에 짧은 지연을 둔다.
- STT: `SpeechToText(applicationContext, mode)` → `initialize(OnInitializeListener)` → **`addListener(ResultListener)`를 `request()` 전에** → `setLocale(Locale.KOREA)` → `request()` → `onUpdated(text, completed)` → 최종 결과 또는 타임아웃 시 `stop()`. 에뮬레이터는 Extended Controls > Microphone에서 입력 허용 필요(README에 적는다).
- **응답 시간 측정 정의** (졸음 케어의 원재료):
  - `t0` = 문제 TTS의 `onDone` 시각
  - `tFirst` = 첫 `onUpdated(text != blank)` 또는 `onUpdatedEpdData(on)` 중 먼저 온 것
  - `tFinal` = `onUpdated(completed = true)`
  - `latencyMs = tFirst - t0`, 타임아웃 8초 → `latencyMs = 8000, answer = null`(느린 응답으로 집계)
- 퍼미션 `pleos.car.permission.TTS_SERVICE`, `pleos.car.permission.STT_SERVICE`. `ErrorType` `when`에는 반드시 `else` 분기.
- 오디오 포커스(§7.2): 게임 음성은 `USAGE_MEDIA`/`CONTENT_TYPE_SPEECH`로 **세션 시작 시 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` 1회 요청**, 세션 종료·PAUSED 진입·Suspend 시 abandon. 포커스 콜백: `LOSS` → 세션 종료(IDLE로, 상태 저장) / `LOSS_TRANSIENT`(통화·Gleo) → 즉시 TTS/STT 중단 + PAUSED / `LOSS_TRANSIENT_CAN_DUCK` → 계속(게임은 duck 대상이 아니라 발화자이므로 무시 가능) / `GAIN` → PAUSED였다면 1초 뒤 현재 문제를 다시 출제. TTS SDK가 Speech Service 경유로 자체 포커스를 잡는지 로그로 확인하고, 이중 요청이 충돌하면 앱 측 요청을 세션 단위 1회로 유지한 채 보고한다.
- `SpeechEngine` 인터페이스와 구현 3종:
  ```kotlin
  interface SpeechEngine {
    val kind: SpeechEngineKind                       // PLEOS / ANDROID / SCRIPTED
    suspend fun speak(text: String)                  // onDone 또는 onError에서 리턴, 취소 가능
    fun listen(timeoutMs: Long): Flow<SttEvent>      // Partial(text, tMs) / Final(text, tMs) / Timeout / Error
    fun stop()
  }
  ```
  - `PleosSpeechEngine`: 위 SDK. 초기화 실패(`PERMISSION_DENIED`, `NOT_SUPPORTED_API`, `UNAUTHENTICATED` 등)는 kind 전환 사유로 보고.
  - `AndroidSpeechEngine`: `android.speech.tts.TextToSpeech` + `android.speech.SpeechRecognizer`. 에뮬레이터에 엔진이 없을 수 있으니 `isRecognitionAvailable()`·엔진 목록으로 감지해 없으면 건너뛴다.
  - `ScriptedSpeechEngine`: TTS는 화면 자막 + 짧은 비프음(또는 무음)으로, STT는 **대본(`assets/demo/answers_normal.json`, `answers_sleepy.json`)의 답을 지정된 지연 후 돌려준다.** 디버그 패널에서 직접 타이핑도 가능하되 `requiresDO`면 입력창을 비활성화. 데모 영상 녹화와 단위/통합 테스트용.
  - 시작 시 Pleos → Android → Scripted 순서로 시도. 선택 결과를 홈 화면 하단 작은 배지와 로그에 표시.

### 5.5 Gleo Interface — 이번엔 하지 않는다

`assets/schema.json`은 2차 확장 포인트(`START_GAME`, `PAUSE_GAME`, `SKIP_QUESTION`)로 `docs/PHASE2_NOTES.md`에만 적어 둔다. 파일을 실제로 넣으면 심사 변수가 늘어난다.

---

## 6. 게임 콘텐츠

- `assets/content/quiz_regions.json` — 17개 시/도(서울·경기·인천·강원·충북·충남·대전·세종·경북·대구·경남·울산·부산·전북·전남·광주·제주) × **각 8문제 이상**, `difficulty 1~3`, `question`, `answers`(허용 표기 여러 개), `hint`, `category`(특산물/지명/문화/음식). 사실에 근거한 문제만, 정답은 **STT가 잘 받는 짧은 명사**(예: 충주 → 사과, 횡성 → 한우, 경주 → 불국사, 제주 → 한라봉). 전연령·가족용. 지역 논쟁 소지(정치·역사 해석)는 넣지 않는다.
- `assets/content/quiz_general.json` — 지역 무관 가족 퀴즈 40문제 이상(동물·과학·동요·속담 빈칸 등), 같은 스키마.
- `assets/content/words_ko.txt` — 끝말잇기 사전: **2음절 이상 일반 명사 3,000개 이상**, 한 줄에 하나, 아이에게 부적절한 단어 없음, 중복 없음. 공개 어휘 목록에서 정제하거나 직접 생성하되 출처·생성 방식을 `docs/CONTENT_SOURCES.md`에 적는다.
- `AnswerMatcher`: 정규화(공백·문장부호 제거, 끝의 "요/이요/입니다/예요/이에요" 제거) → 허용 표기 중 하나와 일치 또는 포함 → 3글자 이상이면 편집거리 ≤ 1 허용. 단위 테스트 필수.
- `WordChainGame`: 마지막 음절로 시작하는 단어 검증 + **두음법칙**(녀→여, 뇨→요, 뉴→유, 니→이, 랴→야, 려→여, 례→예, 료→요, 류→유, 리→이, 라→나, 래→내, 로→노, 뢰→뇌, 루→누, 르→느 등) + 사용 단어 중복 금지 + 2음절 이상. 사전에 없는 단어는 한 번 "다시 한번 말해 줄래요?" 후 **관대하게 인정**(가족 게임이지 국어 시험이 아니다; 로그에는 남긴다). 앱 차례에는 다음 사람이 이을 수 있는 단어를 고른다(막다른 음절 회피). 앱이 이을 단어가 없으면 "제가 졌어요!"로 판을 끝낸다.
- `RegionQuizGame`: 현재 `regionCode`의 미출제 문제 → 없으면 인접 지역 → 없으면 일반 퀴즈. 난이도는 `DrowsinessMonitor`가 내린 상한 이하에서 고른다. 지역이 바뀌면 "이제 충청북도예요!" 한 줄 안내 후 그 지역 문제로.
- 모든 TTS 문장은 **한 문장 ≤ 80자, 문제는 2문장 이내**. 아이도 알아듣는 말투("~해 볼까요?"). 존댓말 통일.

---

## 7. 졸음 케어 1단계 알고리즘

```
ResponseSample(latencyMs: Long, correct: Boolean?, difficulty: Int, atEpochMs: Long, driveState: DriveState)
  — 운전자 차례의 응답만. PAUSED 직후 첫 응답, 판 첫 문제(워밍업)는 제외.

DriverBaseline (DataStore, 운전자 프로필별)
  — latencyMs의 지수이동평균(EMA, α=0.1) + 정답률 EMA. 과거 여정 누적.
  — 기준값이 없는 첫 여정: 첫 5개 응답으로 캘리브레이션(이 동안 알림 없음, "기준을 만드는 중" 로그).

Rolling window: 최근 5개 샘플
slow(sample)  = latencyMs > max(baseline * 1.5, baseline + 1500)
Stage1 trigger = window 안에 slow가 3개 이상 (타임아웃 포함)
Stage1 action  = (1) TTS로 가볍게 말 걸기 — 졸음을 단정하지 않는 문장
                     예: "○○님, 운전 오래 하셨죠? 다음 문제는 조금 쉬운 걸로 가 볼게요."
                 (2) 난이도 상한 -1 (최소 1)
                 (3) 쿨다운 5분 (그 안엔 다시 트리거하지 않음)
                 (4) 화면에는 "반응이 조금 느려졌어요" 카드 + (2차 자리) "잠깐 쉬어 갈까요?" 문구만. 졸음쉼터 거리는 2차.
Stage2 (2차, 구현 안 함) = 15분 내 Stage1 2회 또는 (slow && window 정답률 < 50%) → 휴식 권유 + 졸음쉼터

디버그 패널에 표시: baseline, 최근 5개 latency, slow 수, 난이도 상한, 마지막 트리거 시각.
```

원칙: **보조 기능**이다. UI 문구 어디에도 "졸음운전 감지"처럼 단정하는 표현을 쓰지 않는다. 운전자를 재촉하거나 반복 경고하지 않는다(쿨다운).

---

## 8. 화면 구성

멀티 윈도우(DEV_GUIDE §5.4, §6.3): **portrait(1 window)와 landscape(2 window) 레이아웃 둘 다**, 레터박스·호환 모드 금지, 백그라운드 복귀 시 상태 복원. `screenOrientation`은 지정하지 않는다(기본 portrait). 반응형으로 두 비율에서 요소 잘림·겹침이 없어야 한다.

| 화면 | 모드 | 내용 |
|---|---|---|
| **Home / 여정 설정** | IDLE | 프리셋 목적지 카드(서울→부산, 서울→강릉, 서울→전주, 짧은 시연용 "근처 10분"), 운전자 프로필 선택(프리셋 이름 버튼 — 키보드 금지), 동승자 수(착좌 감지 시 자동 제안), 게임 선택(지역 퀴즈/끝말잇기/번갈아), `출발` 버튼 → 플랜 미리보기("4시간 10분 → 5판") |
| **주행 중(VOICE)** | VOICE | 기획서 화면안 그대로: 상단 "음성 모드 · 도착까지 1시간 12분", 가운데 현재 문제 **≤ 3줄**, 하단 마이크 상태 아이콘(정적 상태 3종: 대기/듣는 중/말하는 중 — **주행 중 애니메이션 금지**) + "화면 조작 잠금" 캡션, 터치 가능한 것은 `일시정지/계속` 한 개 |
| **일시정지(PAUSED)** | PAUSED | "길 안내 후 이어서 할게요" / "통화가 끝나면 이어서 할게요" + 사유 |
| **반응 저하 감지 카드** | VOICE 위 오버레이 | 기획서 화면안: "잠깐 쉬어 갈까요?" + 1차에선 "다음 문제는 쉬운 걸로 갈게요" + `음성으로 안내 시작` 버튼(2차 자리) |
| **정차 중(SCREEN)** | SCREEN | 기획서 화면안: 카드 2개(초성 퀴즈 / 그림 맞히기 — 후자는 "준비 중" 비활성), 하단 "정차 중에만 열려요" |
| **도착 / 요약** | IDLE | 오늘의 점수·정답 수·플레이어별 점수·지나온 지역 목록. 이동 앨범 자리. |
| **디버그 패널** | 어디서나 | 제목 롱프레스로 열기. 선택된 소스/엔진 상태, 시뮬레이션 토글(기어·속도·DO·지역·남은 시간 슬라이더), 대본 선택(normal/sleepy), 졸음 지표, 최근 로그 20줄. release 빌드에서도 접근 가능하되 상단에 `DEV` 표시 |

디자인 규칙 (DEV_GUIDE §6, `CLAUDE.md` UI): 대비 ≥ 4.5:1, 최소 12sp(실제 ≥ 18px; 주행 화면 본문은 28sp 이상), 터치 타깃 ≥ 76dp, 아이콘 48dp 그리드, 다크 테마 기본, 주행 중 애니메이션·자동 스크롤·영상 금지, 한 줄 ≤ 80자. 폰트는 시스템 기본(Asta Sans가 있으면 자동 적용됨). 광고·헤드업 알림 없음.

---

## 9. 플랫폼 규칙 체크리스트 (심사 반려 사유 — 전부 적용)

- [ ] **CCP Back**: 최상단 화면에서 Back → `moveTaskToBack(true)`. `finish()`/`finishAffinity()` 금지. 하위 화면·팝업은 일반 Back.
- [ ] **CarUxRestrictions** 리스너 등록, `requiresDO`면 SCREEN 금지·키보드 금지·텍스트 축약. 지역 하드코딩 없음.
- [ ] **오디오 포커스** 요청·콜백 처리(§5.4). Gleo/통화(`LOSS_TRANSIENT`) 즉시 중단. Suspend 진입 시 중단 → abandon → 상태 저장 → 리소스 해제, Resume 시 자동 재개 금지(사용자 `계속` 필요).
- [ ] **상태 복원**: `SavedStateHandle` + DataStore로 진행 중 세션(플랜·현재 판·점수·사용 단어·졸음 윈도우) 저장, 프로세스 재생성·백그라운드 복귀 시 같은 문제부터.
- [ ] **성능**: 이벤트 기반만(폴링·타이머 루프 금지, 플랜 타이머는 `delay` 코루틴 1개), 주행 중 CPU < 5%, RAM ≤ 400 MB. 백그라운드 전환 시 화면 갱신·STT 중단.
- [ ] **매니페스트**: `minSdk 28`, `android:allowBackup="false"`, 모든 컴포넌트 `exported="false"`(런처 액티비티만 true), `<meta-data android:name="com.hmg.rotary.ROTARY_SUPPORTED" android:value="false"/>`(터치만으로 모든 조작 가능해야 함), 퍼미션은 §5에 적힌 것만: `CAR_DRIVE`, `CAR_SEATS`, `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `TTS_SERVICE`, `STT_SERVICE`, `FUSED_LOCATION`, (AAOS) `CAR_POWERTRAIN`, `CAR_SPEED`, `RECORD_AUDIO`(Android 폴백 엔진용, Pleos STT가 요구하지 않으면 폴백 경로에서만 요청).
- [ ] **보안**: 비밀값은 `local.properties` → `BuildConfig`, 리포에 커밋 금지(`.gitignore` 확인). 로그에 PII 금지 — **인식된 발화 텍스트와 위치는 debug 빌드에서만** 로그. release는 `debuggable=false`, 난독화 켜도 SDK 클래스는 keep(샘플의 keep 규칙 참고).
- [ ] **UI**: 두 orientation, 레터박스 없음, 주행 중 애니메이션·자동 스크롤 없음, 대비·글자 크기 규칙.
- [ ] 버전: `versionCode`/`versionName` 설정(1.0.0 / 1), 제출마다 증가.

---

## 10. 데모/시뮬레이션 모드 — 시연 영상이 반드시 찍혀야 한다

- 설정값 `demoMode`(DataStore, 기본 false)와 소스별 강제 선택(`car=auto|real|sim`, `trip=…`, `location=…`, `speech=…`). 디버그 패널에서 바꾼다.
- `SimulatedTripSource`: 서울→부산 **타임라인 압축**(기본 1 실제분 = 30 여정분, 슬라이더로 조절). 이벤트: 출발 → 경기(+20분) → 충북(+70분) → 대전(+100분) → 경북(+150분) → 부산 진입(+230분) → 도착(+250분). 중간에 "분기점 안내 300 m" TBT 이벤트 2회(P1 일시정지 시연용). `remainTime`은 매 5초 갱신.
- `SimulatedCarStateSource`: 기어·속도 토글. 실제 `CarUxRestrictionsManager`가 살아 있으면 그 값은 그대로 쓴다(둘을 섞어도 됨 — DO는 실제, 속도는 시뮬).
- `ScriptedSpeechEngine` 대본: `answers_normal.json`(정답 위주, 1.0~2.5초 지연), `answers_sleepy.json`(지연이 2초→6초→타임아웃으로 늘어나 Stage1이 걸리게). 대본은 현재 문제의 `answers[0]`을 돌려주는 `"{correct}"`, 틀린 답 `"{wrong}"` 토큰을 지원해 콘텐츠와 분리.
- `docs/DEMO_SCENARIO.md`에 **3분 시연 대본**을 쓴다: ① Home에서 목적지·플레이어 설정 → 플랜 미리보기 ② Virtual Console 기어 D → VOICE 전환, 음성 퀴즈 2문제(실제 TTS/STT가 되면 실제로, 아니면 대본) ③ 끝말잇기 3턴 ④ sleepy 대본 → "반응이 조금 느려졌어요" 카드 + 난이도 하향 TTS ⑤ (P1) TBT 일시정지 → 재개 ⑥ 기어 P → SCREEN 전환, 초성 퀴즈 ⑦ 남은 시간 5분 → 요약 화면. 각 단계에 사용하는 adb/Virtual Console 조작을 적는다.
- README의 "테스트 환경" 절에 **무엇이 실제 SDK로 동작했고 무엇이 시뮬레이션인지 정직하게** 표로 적는다. 심사위원은 코드를 본다.

---

## 11. 작업 순서 (마일스톤) — 각 단계 끝에 빌드·에뮬레이터 실행·커밋

커밋 메시지는 `feat(M1): ...` 형식. 각 마일스톤 보고에는 "검증한 것 / 검증 못 한 것(이유)"를 적는다.

- **M0 환경** — 루트 빌드 통과 확인 → `libs.versions.toml`에 SDK 5종 + Compose(또는 Views) + DataStore + 코루틴 → 매니페스트(퍼미션·`allowBackup`·`ROTARY_SUPPORTED`·`exported`) → `applicationId={{PACKAGE}}`, 앱명 `{{APP_NAME}}` → `local.properties` 키 로딩(`PLEOS_CLIENT_ID`, `PLEOS_CLIENT_SECRET`, 없으면 빈 문자열) → 빈 MainActivity가 Pleos AVD(cold boot)에서 뜨는지 확인. 파일 트리·계획 출력.
- **M1 주행 상태·모드** — `AaosCarStateSource` + `VehicleSdkClient` + `SimulatedCarStateSource` + `ModeArbiter` + Home/Voice/Screen 화면 뼈대(모드별 전환만). **검증**: Virtual Console 기어 P↔D, `driving_regulation on/off`로 화면이 바뀜. `ModeArbiter` 단위 테스트(디바운스·DO 우선 규칙 포함).
- **M2 여정** — `NaviHelperTripSource` + `SimulatedTripSource` + `SessionPlanner` + Home의 목적지 프리셋·플랜 미리보기. **검증**: `requestRoute` 후 이벤트 로그(없으면 시뮬 전환 확인). `SessionPlanner` 단위 테스트(4시간→5판, 12분 미만→1판, 재계산).
- **M3 음성** — `SpeechEngine` 3종 + `AudioFocusHelper` + 응답 시간 측정. **검증**: "안녕하세요, 놀레오예요" 발화 → STT 1회 인식 → latency 로그. 어느 엔진이 잡혔는지 보고.
- **M4 게임** — 콘텐츠 JSON·사전 + `ContentRepository` + `AnswerMatcher` + `RegionQuizGame` + `WordChainGame` + `GameSession` 루프(출제→듣기→판정→결과→다음 턴) + `RegionResolver` + `FusedLocationSource`. **검증**: 한 판 끝까지 진행. 단위 테스트: 매처·두음법칙·턴 순서·지역 판정.
- **M5 졸음 케어 1단계** — `DrowsinessMonitor` + 기준값 저장 + Stage1 액션 + 카드 UI + 디버그 지표. **검증**: sleepy 대본으로 Stage1이 정확히 1회(쿨다운) 걸림. 단위 테스트(합성 샘플).
- **M6 P1** — TBT 일시정지(간단) → 초성 퀴즈 → 요약 화면. P0 회귀 없을 때만.
- **M7 마감** — portrait/landscape 점검, CCP Back, 상태 복원(홈 버튼으로 내렸다 복귀), `lintDebug`, `testDebugUnitTest` 전부 통과, release 서명 빌드(서명 키는 리포 밖, 경로는 `local.properties`; README에 생성 방법) → `apk/돈도아연_놀레오_release.apk` → `README.md` → `docs/DEMO_SCENARIO.md` → `docs/PHASE2_NOTES.md` → 최종 보고.

시간이 부족하면 **M6을 버리고 M7을 지킨다.** 반쯤 된 P1보다 깨끗한 P0가 낫다.

---

## 12. README.md 필수 항목 (공모전 참가 가이드의 GitHub 제출 요건 — `CLAUDE.md` "Repo conventions for submission"과 동일)

1. 앱 이름 `놀레오`와 한 줄 소개 ("가는 길이 놀이가 되는 가족 게임").
2. 실행 방법: Pleos Connect Emulator 2.1.0 설치·AVD(cold boot, RAM 4 GB) → `./gradlew installDebug` 또는 APK 설치 → Virtual Console 포트 포워딩 → 마이크 허용 → 앱 실행.
3. 테스트 환경 및 필요한 설정: AVD 이름, `local.properties` 키(선택), 런타임 퍼미션, **실제 SDK vs 시뮬레이션 표**, 알려진 제약(내비 미탑재 시 시뮬 여정 등).
4. APK 위치: `apk/돈도아연_놀레오_release.apk` (용량 초과 시 Release 링크).
5. 기획서 §"기술 활용성" 표와 같은 **SDK 활용 표** + 어느 파일에서 호출하는지 경로.
6. 아키텍처 그림(텍스트 트리)과 상태 머신 요약, 시연 영상 링크 자리.

---

## 13. 결정 원칙과 금지 사항

- 막히면 **안전·안정 쪽**으로: 차량 상태를 모르면 VOICE(화면 잠금), SDK가 실패하면 폴백, 예외는 삼키지 말고 상태로 바꿔 UI와 로그에 드러낸다. **어떤 경로에서도 크래시 금지.**
- SDK 시그니처·퍼미션명·단위는 **추측 금지** — 레퍼런스 grep, 샘플 코드, 에뮬레이터 로그로 확인하고 근거를 주석에 남긴다. 확인 불가면 `// TODO(verify): ...` + 보고서.
- `CLAUDE.md`의 Platform rules·Adding a Pleos SDK·integration pattern을 위반하는 코드는 쓰지 않는다. 특히 샘플의 전체 퍼미션 복사, 차량 모델명 분기, Telltales/SmartGlass 같은 privileged API.
- 2차 기능(§2.3)을 "조금만" 만들지 않는다. 인터페이스·TODO·문서까지만.
- 네트워크 호출 없음(콘텐츠는 전부 assets). 외부 API 키 없음. 비밀값 커밋 없음.
- 영상·애니메이션·광고·헤드업 알림 없음. 주행 중 텍스트 입력 없음.
- 콘텐츠는 사실에 근거하고 전연령. 지역 비하·정치·사행성 요소 없음.
- 라이브러리는 최소한으로. 새 의존성을 넣을 때마다 "왜 필요한지" 한 줄 보고.
- 작업 중간에 사용자에게 묻기보다는 §13 원칙으로 결정하고 **최종 보고서에 결정 목록**을 적는다. 단, 되돌리기 어려운 변경(compileSdk 하향, UI 툴킷 변경, 퍼미션 추가)은 보고서 맨 위에 굵게.

---

## 14. 완료 조건 (Definition of Done)과 최종 보고 형식

**DoD**
- `./gradlew lintDebug testDebugUnitTest assembleRelease` 통과.
- Pleos AVD(cold boot)에서: 앱 실행 ≤ 10초, 기어 P↔D·DO on/off에 모드 전환, 여정 설정→음성 퀴즈 한 판→끝말잇기 한 판→Stage1 카드→요약까지 **크래시·멈춤 없이** 한 번에 진행(실제 SDK 또는 시뮬 어느 조합으로든).
- portrait/landscape 둘 다 요소 잘림 없음. 홈으로 내렸다 복귀 시 같은 문제가 유지됨. Back 키로 앱이 종료되지 않음.
- `apk/` 에 release APK, `README.md`·`docs/DEMO_SCENARIO.md`·`docs/PHASE2_NOTES.md`·`docs/CONTENT_SOURCES.md` 존재.

**최종 보고 (마지막 메시지)**
1. 되돌리기 어려운 결정 (굵게)
2. 실제 SDK로 검증된 것 / 시뮬레이션으로만 확인한 것 / 검증 못 한 것 — 표
3. 선택된 UI 툴킷·엔진·소스와 이유
4. 알려진 이슈와 2차 전 해야 할 일 (우선순위 순)
5. 데모 녹화 전에 사람이 직접 확인해야 할 체크리스트 (마이크 허용, 포트 포워딩, 대본 선택 등)