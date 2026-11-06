# 2차(12/3 오프라인 PT) 설계 메모

1차 제출본은 **설계만 하고 구현하지 않은** 확장 지점을 코드에 `TODO(phase2)` 로 남겨 두었다. 반쯤 만든 기능은 데모에서 사고만 내므로 1차에는 넣지 않았다.

## 1. 졸음 케어 2단계 + 졸음쉼터 안내
- **트리거** (`domain/drowsy/DrowsinessMonitor.kt` 의 TODO): 15분 안에 Stage1 2회, 또는 (느린 응답 && 최근 5개 정답률 < 50%)
- **동작**: TTS "잠깐 쉬어 가요. 가까운 졸음쉼터로 안내해 드릴까요?" → 응답 "응/네" (STT, AnswerMatcher) 또는 화면의 `음성으로 안내 시작` 버튼(현재 Stage1 카드에 비활성으로 자리만 있음)
- **졸음쉼터 탐색**: 오프라인 테이블 `assets/content/rest_areas.json`(고속도로 졸음쉼터 좌표) + FusedLocation 현재 위치로 진행 방향 앞쪽 가장 가까운 곳 선택
- **길 안내**: `NaviHelper.addWaypoint(RequestWaypointInfo(..., waypointIndex = WaypointIndex.FIRST))` — 경유지로 넣어 원래 목적지를 유지. 퍼미션 `NAVI_ROUTE_SEARCH`(이미 선언)
- **도착 감지**: `FusedLocation.addProximityAlert(ProximityAlertRequest(lat, lng, radiusMeters = 300f, setOf(ENTER)))` → 도착하면 게임을 쉬는 시간으로 전환하고 "잘 쉬셨나요?" 확인, `NaviHelperEventListener.onWayPointArrived` 와 교차 확인
- 판단 원칙 유지: "졸음운전 감지" 같은 단정 표현 금지, 반복 경고 금지(쿨다운)

## 2. 지역 스탬프 영속화
- 현재: 세션 안에서만 `regionsVisited` 를 모아 요약 화면에 칩으로 보여 준다.
- 2차: 지역 퀴즈 정답 시 `stamps(regionCode, firstAt, count)` 를 DataStore(또는 JSON 파일)에 누적 → 홈에 "스탬프 17칸" 진행도, 새 지역 첫 스탬프는 TTS 축하

## 3. 구간별 자동 일시정지 고도화
현재(P1): `onTBTInfo` 첫 항목이 분기/진출입/회전이고 500 m 이내면 PAUSED, 지나면 3초 뒤 재개, 같은 안내가 20초 넘게 그대로면 정체로 간주.
추가할 신호 (`core/navi/NaviHelperTripSource.kt` 의 TODO 콜백):
- `onRouteStateInfo.trafficStatus` — `TrafficCongestionType.HEAVY` 구간 진입 시 문제 간격을 늘리고 긴 문제를 피함(멈춤보다는 "천천히")
- `onCautionPointInfo` — 급커브·사고다발 구간 접근 시 일시정지
- `onCameraAlert` — 단속 카메라 안내 직전 일시정지(내비 음성과 겹치지 않게)
- 오디오 포커스: 내비 안내 음성(NAVIGATION 컨텍스트)과의 상호작용을 Interaction Matrix 로 검증

## 4. 이동 앨범
- 도착 요약 화면을 확장: 경로(시뮬 타임라인 또는 FusedLocation 샘플링 좌표) 위에 판·정답·지역 스탬프를 시간순으로 배치
- 지도 렌더링은 순정 내비 화면을 쓰지 않고 앱 내부 간단한 경로선 + 지역 칩으로(외부 지도 SDK·네트워크 없이)
- `NaviHelper.requestNavigationScreen(OverviewRoute)` 는 `NAVI_CUSTOM_MAP` 퍼미션이 필요하므로 사용 여부를 App Market 과 협의 후 결정

## 5. 그림 맞히기 (정차 중 화면 게임 2종째)
- 현재 화면 게임 카드에 "준비 중"으로 비활성 자리만 있다.
- 간단한 벡터 그림을 단계적으로 드러내고 4지선다 터치. SCREEN 모드에서만 열리고 출발 시 즉시 닫힘(초성 퀴즈와 같은 규칙).

## 6. Gleo Interface (`assets/schema.json`) — 파일은 아직 넣지 않음
1차에 파일을 넣으면 심사 변수가 늘어나므로 문서로만 남긴다. 2차 후보 함수(영어 description, 3개 이하):

| 함수 | 설명(영문) | 앱 동작 |
|---|---|---|
| `START_GAME` | Start a family voice game on the current trip | 홈 설정이 있으면 세션 시작, 진행 중이면 다음 판 시작 |
| `PAUSE_GAME` | Pause or resume the current voice game | `PauseReason.USER` 토글 |
| `SKIP_QUESTION` | Skip the current question | 현재 차례를 무응답 없이 넘김(졸음 지표 제외) |

- intent-filter + 7초 안에 `APP_ACTION_RESPONSE` 브로드캐스트(DEV_GUIDE §9)
- Gleo 호출 시 `AUDIOFOCUS_LOSS_TRANSIENT` 는 이미 처리(즉시 중단 → GAIN 시 1초 뒤 같은 문제)

## 7. 그 밖의 2차 작업 후보
- 운전자 프로필 확장(이름 직접 입력은 정차 중에만, 좌석 착좌·운전석 프로필 연동)
- 동승자 이름 커스터마이즈, 아이 연령대별 난이도
- 서버 TTS/STT(HYBRID) 품질 비교 — `local.properties` 에 Client ID/Secret 만 넣으면 전환
- 앱 내 버전 체크 → `market://detail?package=com.gildongmu.app` 유도(DEV_GUIDE §11.2)
