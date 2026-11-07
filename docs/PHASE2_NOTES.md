# 2차(12/3 오프라인 PT) 설계 메모

1차 제출본은 **설계만 하고 구현하지 않은** 확장 지점을 코드에 `TODO(phase2)` 로 남겨 두었다. 반쯤 만든 기능은 데모에서 사고만 내므로 1차에는 넣지 않았다. §1 졸음 케어 2단계(경로 위 휴게소·졸음쉼터 안내)는 11/7 에 구현했다.

## 1. 졸음 케어 2단계 + 쉬어 갈 곳 안내 — **11/7 구현 완료** (남은 것은 실차 검증)
구현 내용은 README §2.5·§5, 확인 방법은 `docs/FEATURE_GUIDE.md` §8 에 있다. 요약:
- **트리거** (`DrowsinessMonitor`): 15분 안에 1단계 두 번째, 또는 (이번 답이 느림 && 최근 5개 정답률 < 50%). 쿨다운 20분, 난이도 −1. 데모 모드는 쿨다운만 1단계 20초·2단계 60초. 디버그 패널 "2단계 시험"은 다음 운전자 답 뒤에 강제로 낸다
- **고르기** (`RouteRestStops`·`RestStopFinder`): 내비 TBT 목록의 **경로 위 휴게소·졸음쉼터**(`TURN_ENTER_SERVICEAREA`·`TURN_RESTAREA`, `distance` 는 구간 거리라 누적) 중 1~80 km 앞 가장 가까운 곳, 휴게소가 5 km 안쪽으로만 더 멀면 휴게소. 휴게소는 편의시설(수면실·수유실·가족사랑 화장실·대표 음식), 졸음쉼터는 공공데이터 표와 이름으로 맞춰 좌표. 상·하행은 진행 방향(시뮬 경로 / FusedLocation 움직인 방향 → SDK heading → 목적지 쪽)으로. 길 안내가 없을 때만 공공데이터 졸음쉼터 253곳 중 진행 방향 앞쪽
- **질문**: TTS "○○, 반응이 계속 느려졌어요. 가는 길에 있는 기흥휴게소가 6킬로미터 앞이에요. 수유실과 가족사랑 화장실이 있어요. 거기서 잠깐 쉬어 갈까요?" → STT "네/아니요"(`YesNo`) 또는 화면 **쉬어 가기 / 괜찮아요** 중 먼저 온 답. 거절·무응답이면 카드를 15초 남겨 둔다. 시뮬 여정은 묻는 동안 멈춘다(30배속이라 수락 전에 지나치지 않게)
- **경유지는 넣지 않는다**: 11/7 `addWaypoint` 시험에서 POI ID 없는 좌표 경유지가 고속도로 옆 일반 도로에 붙어 IC 로 나갔다 들어오는 우회 경로가 됐다. 경로 위 쉼 장소만 권하므로 필요 없다. 게임은 `PauseReason.REST_STOP`
- **도착 감지**: 좌표가 있으면 `FusedLocation.addProximityAlert`(300 m, ENTER)·위치가 반경 안 → **서거나 주차해야** 도착(옆을 지나가는 것과 구별). 좌표가 없는 휴게소는 주차. 시뮬 여정은 그 지점에 닿으면. 도착 안내 뒤 `PauseReason.REST_AREA` 로 쉬다가 주차 → 다시 주행이면 "잘 쉬셨나요? 다시 출발해요!" + 응답 창·난이도 초기화
- 판단 원칙 유지: "졸음운전 감지" 같은 단정 표현 금지, 반복 경고 금지(쿨다운)

남은 것:
- **실차 검증**: 주행 중 내비가 `onTBTInfo` 를 얼마나 자주 보내는지(그 사이는 남은 거리 변화로 앞당김), 근접 알림 ENTER 가 고속 주행에서 제때 오는지, 권한 곳을 지나쳤을 때 처리(지금은 게임이 계속 쉬고 "계속"으로 이어 감 → "지나쳤어요, 다음 휴게소에서 쉬어 가요" 재권유 검토)
- 휴게소 좌표: 도로공사 OpenAPI(인증키)로 빌드 때 받아 넣으면 휴게소도 근접 알림으로 도착을 알 수 있다(앱 실행 중 네트워크는 여전히 쓰지 않음)
- 원자료 방향 표기("A기점 + B종점")를 A → B 차로로 읽은 해석 확인(한국도로공사 문의 또는 실측)
- 길 안내가 없을 때의 표 근사 보완: NaviHelper `onCurrentLocationInfo` 의 `heading` 을 진행 방향 후보에 넣기

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
- 간단한 벡터 그림을 단계적으로 드러내고 4지선다 터치. SCREEN 모드에서만 열리고 출발 시 즉시 닫힘(사자성어와 같은 규칙).
- 화면 게임 1종째는 11/7 초성 퀴즈 → **사자성어**로 바꿨다(뜻을 TTS 로 읽어 주고 4지선다, 고르면 한자 훈·음 풀이 + 정답·유래 읽기 → 자동으로 다음 문제). 소리를 내는 화면 게임의 규칙(읽는 동안만 포커스, 출발·닫기·백그라운드·포커스 손실이면 즉시 멈춤, 저절로 다시 읽지 않음)은 `app/IdiomQuizController.kt` 를 따른다.
- 사자성어 후속 후보: 한자 급수(`chars[].grade`)로 난이도 고르기, 틀린 사자성어 다시 내기(오답 노트), 음성 판에 "뜻 듣고 사자성어 말하기" 넣기(STT 가 4글자 한자어를 잘 받는지 먼저 확인), 앱 안에 자료 출처·라이선스(CC BY-SA 4.0) 표기 화면. 유래 칸의 뜻풀이 조각 23개는 11/7 정리했다(`CONTENT_SOURCES.md`).

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
- **출발 전 예상 시간을 실제 내비 값으로**: 홈 프리셋의 "약 4시간 10분"은 고정값이다. `NaviHelper.requestTrip(TripRequest(lat, lng, poiId, poiName, poiSubId))` 를 부르면 길 안내를 시작하지 않고 `onTripInfo(TripInfo(distance m, eta 초, …))` 로 예상 시간을 받는다(퍼미션 `NAVI_ROUTE` — 이미 선언, 샘플 `NaviTestData.tripRequest`). 미리보기 "N시간 → M판"을 실제 교통 반영 값으로 바꿀 수 있다
- **Vehicle SDK 리스너를 화면이 보일 때만 등록**: 샘플 README("관찰하는 화면이 멈추면 unregister")·DEV_GUIDE §5.4 기준. 지금은 브레이크·좌석 리스너가 앱 수명 동안 등록돼 있다(좌석은 홈에서만 쓰임)
- 운전자 프로필 확장(이름 직접 입력은 정차 중에만, 좌석 착좌·운전석 프로필 연동)
- 동승자 이름 커스터마이즈, 아이 연령대별 난이도
- 서버 TTS/STT(HYBRID) 품질 비교 — `local.properties` 에 Client ID/Secret 만 넣으면 전환
- 앱 내 버전 체크 → `market://detail?package=com.gildongmu.app` 유도(DEV_GUIDE §11.2)
