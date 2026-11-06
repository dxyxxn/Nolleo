# Pleos Connect SDK — API Reference (자동 추출 요약)

> 출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/ 의 Dokka API Reference 782페이지 (수집 2026-11-05).
> 각 클래스의 시그니처·설명과 각 메서드 페이지의 상세 설명/퍼미션(protection level)을 합쳐 정리. `[perm: X]` = `pleos.car.permission.X` 필요. 원문은 영어이므로 그대로 둠.
> 사용법: Claude Code에서 클래스/메서드명으로 grep 해서 시그니처를 확인할 것. 개념·정책은 `PLEOS_CONNECT_DEV_GUIDE.md` 참고.


> 표기 규칙: `- **name** — 설명 [perm: …] (protection: …)` 다음 줄에 Kotlin 시그니처. `상세:`는 메서드 개별 페이지의 추가 설명(값 범위·주의사항 등).
> protection level `signature|privileged` 인 API(예: Safety Telltales, Window.setSmartGlassState)는 일반 3rd party 앱에서 사용 불가로 간주.
> 대응 문서 버전: Vehicle 2.3.2 / NaviHelper 3.0.5 / TextToSpeech 3.3.1 / SpeechToText 3.3.1 / FusedLocation 1.1.3 (2026-10-19 릴리스)

## 목차

- **Vehicle SDK**
  - `ai.pleos.playground.vehicle`: Vehicle
  - `ai.pleos.playground.vehicle.api`: Brake, CarInfo, Display, Door, DrivingMode, EvBattery, Fuel, HVAC, Light, Odometer, Safety, Seat, SideMirror, SteeringWheel, Tire, Toll, TurnSignal, Window, Wiper
  - `ai.pleos.playground.vehicle.api.model.area`: Area, DoorArea, MirrorArea, SeatArea, TurnSignalArea, WheelArea, WindowArea
  - `ai.pleos.playground.vehicle.api.model.capability`: AccessType, AreaBounds, CapabilityConfig, HvacTemperatureConfigArray, SmartGlassConfig
  - `ai.pleos.playground.vehicle.api.model.common`: TripStatistics
  - `ai.pleos.playground.vehicle.api.model.telltale`: TelltaleData, Telltales
  - `ai.pleos.playground.vehicle.callback.exception`: VehicleException
  - `ai.pleos.playground.vehicle.constant`: ErrorCode, PleosPermission
  - `ai.pleos.playground.vehicle.constant.area`: AreaValue, DoorValue, MirrorValue, SeatValue, TurnSignalValue, WheelValue, WindowValue
  - `ai.pleos.playground.vehicle.constant.control`: CruiseControl, DrivingModeType, EvChargeState, EvConnectorType, EvRegenerativeBrakingState, FanDirectionType, FuelType, GsrComplianceType, LightState, LightSwitch, MoveControl, OilLevelState, PortLocationType, SmartGlassZoneCommand, SteeringControl, TripStatisticType, WiperState, WiperSwitch
  - `ai.pleos.playground.vehicle.constant.telltale`: TelltaleColor, TelltaleState, TelltaleType
  - `ai.pleos.playground.vehicle.constant.unit`: DistanceDisplayUnit, EvBatteryDisplayUnit, SpeedDisplayUnit, TemperatureUnit
  - `ai.pleos.playground.vehicle.listener`: AmbientColorAutoByDriveModeEnabledListener, AmbientLightBrightnessColorListener, AmbientLightBrightnessListener, BrakePedalStatusListener, CruiseControlStateListener, CruiseControlTargetSpeedListener, DistanceDrivenListener, DoorLockStateListener, DoorPositionListener, DrivingModeStateListener, DrivingTimeListener, EtcsVoiceGuidanceVolumeListener, EvBatteryEfficiencyListener, EvBatteryStateOfChargeListener, FuelEconomyListener, FuelPercentListener, GloveBoxLockStateListener, HvacAcStateListener, HvacAirCleanerStateListener, HvacAutoStateListener, HvacDefrosterStateListener, HvacDualStateListener, HvacFanSpeedListener, HvacMaxAcStateListener, HvacMaxDefrostStateListener, HvacPowerStateListener, HvacRecirculationStateListener, HvacSeatTemperatureListener, HvacSeatVentilationListener, HvacSideMirrorHeatStateListener, HvacSteeringWheelHeatStateListener, HvacTemperatureListener, HvacTemperatureValueListener, LightStateListener, LowWasherFluidWaringListener, OilLevelStateListener, PowerDownWarningIndicatorListener, SeatAirbagDeployedListener, SeatAirbagEnabledListener, SeatBackrestAnglePositionListener, SeatBeltBuckledStateListener, SeatBeltHeightPositionListener, SeatBeltNotBuckledWarningListener, SeatDepthPositionListener, SeatForeAftPositionListener, SeatHeadrestAnglePositionListener, SeatHeadrestForeAftPositionListener, SeatHeadrestHeightAftPositionListener, SeatHeightPositionListener, SeatLumbarForeAftPositionListener, SeatLumbarSideSupportPositionListener, SeatOccupancyListener, SeatTiltPositionListener, SmartGlassLevelListener, SmartGlassStateListener, SteeringWheelAngleListener, TelltaleInfoListener, TemperatureDisplayUnitListener, TripDrivingDistanceListener, TripDrivingTimeListener, TripEvEfficiencyListener, TripFuelEconomyListener, TurnSignalStateListener, VehicleListener, WindowLockStateListener, WindowPositionListener, WiperStateListener
- **NaviHelper SDK**
  - `ai.pleos.playground.navi.constants`: BookmarkType, CameraType, CautionPointType, NaviErrorCode, NavigationStatus, NaviMode, RouteOption, RoutePointPoiType, RoutePointSettingType, RoutePointType, TrafficCongestionType, TrafficEventType, TurnType, WaypointCmdType, WaypointIndex
  - `ai.pleos.playground.navi.data`: BookmarkInfo, CameraAlert, CautionPoint, ChargerOperator, ChargingPlan, CurrentLocationInfo, DestinationArrivedInfo, DestinationInfo, DrivingInfo, DrivingState, NavigationScreenRequest, RecentDestinationInfo, RequestWaypointInfo, RouteInfo, RouteOverviewShowInfo, RoutePointInfo, RouteStartInfo, RouteStateInfo, RouteWithWaypointsRequest, TBTInfo, TrafficCongestionInfo, TrafficCongestionSummary, TrafficEventInfo, TrafficInfo, TrafficStatus, TripInfo, TripRequest, TripSummary, WaypointArrivedInfo, WaypointChangedInfo, WaypointInfo
  - `ai.pleos.playground.navi.helper`: NaviHelper
  - `ai.pleos.playground.navi.helper.listener`: NaviHelperEventListener
- **TextToSpeech SDK**
  - `ai.pleos.playground.tts`: TextToSpeech
  - `ai.pleos.playground.tts.constant`: ErrorType, Mode
  - `ai.pleos.playground.tts.listener`: EventListener, OnInitializeListener, OnServerConnectionListener
- **SpeechToText SDK**
  - `ai.pleos.playground.stt`: SpeechToText
  - `ai.pleos.playground.stt.constant`: ErrorType, Mode
  - `ai.pleos.playground.stt.data`: InferenceSource
  - `ai.pleos.playground.stt.listener`: OnInitializeListener, OnServerConnectionListener, ResultListener
- **FusedLocation SDK**
  - `ai.pleos.playground.location.fused`: FusedLocation
  - `ai.pleos.playground.location.fused.listener`: FusedLocationListener, ProximityAlertListener
  - `ai.pleos.playground.location.fused.model`: LocationData, ProximityAlertEvent, ProximityAlertRequest, ProximityTransition

---

## Vehicle SDK


### package `ai.pleos.playground.vehicle`

- `Vehicle` — The entry point of the Vehicle SDK.

#### `ai.pleos.playground.vehicle.Vehicle`
```kotlin
class Vehicle(context: Context)
```
The entry point of the Vehicle SDK. Create an instance with Vehicle(context), then call initialize to connect to the vehicle service. Use factory methods such as getDoor and getDisplay to obtain domain API objects (Door, Display, and other interfaces in ai.pleos.playground.vehicle.api), then call property, capability, or callback APIs on those objects. Call release when you no longer need the SDK. Typical flow: Vehicle(context) → initialize getXxx() → domain API (for example Door) Capability / get / set / callback on that domain API release

**Parameters**
- **context** — Android android.content.Context used to bind to the vehicle service.KotlinGet Property API example// Create Vehicle instanceval vehicle = Vehicle(context)// Start Vehiclevehicle.initialize()val door = vehicle.getDoor()door.getCurrentDoorPosition( area = DoorArea(DoorValue.ROW_1_LEFT), onComplete = { pos: Int? -> // do something }, onFailed = { e: Exception -> // do something })val display = vehicl …

**Constructors**
- **Vehicle**
  - `constructor(context: Context)`

**Functions**
- **getBrake** — Get the object of Brake to use Brake APIs.
  - `fun getBrake(): Brake`
- **getCarInfo** — Get the object of CarInfo to use CarInfo APIs.
  - `fun getCarInfo(): CarInfo`
- **getDisplay** — Get the object of Display to use Display APIs.
  - `fun getDisplay(): Display`
- **getDoor** — Get the object of Door to use Door APIs.
  - `fun getDoor(): Door`
- **getDrivingMode** — Get the object of DrivingMode to use DrivingMode APIs.
  - `fun getDrivingMode(): DrivingMode`
- **getEvBattery** — Get the object of EvBattery to use EvBattery APIs.
  - `fun getEvBattery(): EvBattery`
- **getFuel** — Get the object of Fuel to use Fuel APIs.
  - `fun getFuel(): Fuel`
- **getHVAC** — Get the object of HVAC to use HVAC APIs.
  - `fun getHVAC(): HVAC`
- **getLight** — Get the object of Light to use Light APIs.
  - `fun getLight(): Light`
- **getOdometer** — Get the object of Odometer to use Odometer APIs.
  - `fun getOdometer(): Odometer`
- **getSafety** — Get the object of Safety to use Safety APIs.
  - `fun getSafety(): Safety`
- **getSeat** — Get the object of Seat to use Seat APIs.
  - `fun getSeat(): Seat`
- **getSideMirror** — Get the object of SideMirror to use SideMirror APIs.
  - `fun getSideMirror(): SideMirror`
- **getSteeringWheel** — Get the object of SteeringWheel to use SteeringWheel APIs.
  - `fun getSteeringWheel(): SteeringWheel`
- **getTire** — Get the object of Tire to use Tire APIs.
  - `fun getTire(): Tire`
- **getToll** — Get the object of Toll to use Toll APIs.
  - `fun getToll(): Toll`
- **getTurnSignal** — Get the object of TurnSignal to use TurnSignal APIs.
  - `fun getTurnSignal(): TurnSignal`
- **getWindow** — Get the object of Window to use Window APIs.
  - `fun getWindow(): Window`
- **getWiper** — Get the object of Wiper to use Wiper APIs.
  - `fun getWiper(): Wiper`
- **initialize** — Initialize sdk. It is called while the application is created.
  - `fun initialize()`
- **release** — Release sdk. It is called while the application is destroyed.
  - `fun release()`

### package `ai.pleos.playground.vehicle.api`

- `Brake` — Provides APIs for brake pedal status.
- `CarInfo` — Provides APIs for vehicle identification information and capability info.
- `Display` — Provides APIs for temperature display unit.
- `Door` — Provides APIs for door lock, door position, and glove box.
- `DrivingMode` — Provides APIs for driving mode and cruise control.
- `EvBattery` — Provides APIs for EV battery efficiency.
- `Fuel` — Provides APIs for fuel economy.
- `HVAC` — Provides APIs for climate control (HVAC).
- `Light` — Provides APIs for exterior, cabin, and ambient lights.
- `Odometer` — Provides APIs for odometer and trip data.
- `Safety` — Provides APIs for airbags, warnings, fluid levels, and telltales.
- `Seat` — Provides APIs for seat position, occupancy, seat belt, and seat climate.
- `SideMirror` — Provides APIs for side mirror heat.
- `SteeringWheel` — Provides APIs for the front-wheel steering angle and steering wheel heating.
- `Tire` — Provides APIs for tire pressure.
- `Toll` — Provides APIs for ETCS voice guidance volume.
- `TurnSignal` — Provides APIs for turn signal state.
- `Window` — Provides APIs for window position, lock, blinds, and smart glass.
- `Wiper` — Provides APIs for wiper state and switch.

#### `ai.pleos.playground.vehicle.api.Brake`
```kotlin
interface Brake
```
Provides APIs for brake pedal status. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getBrake.

**Functions**
- **checkBrakePedalStatusCapabilities** — Gets the brake pedal status capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkBrakePedalStatusCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentBrakePedalStatus** — Gets the brake pedal status. If true, the brake pedal is being pressed. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getCurrentBrakePedalStatus(onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerBrakePedalStatus** — Registers a listener for brake pedal status property updates. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerBrakePedalStatus(listener: BrakePedalStatusListener)`
- **unregisterBrakePedalStatus** — Unregisters the listener so brake pedal status property updates stop. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterBrakePedalStatus(listener: BrakePedalStatusListener)`

#### `ai.pleos.playground.vehicle.api.CarInfo`
```kotlin
interface CarInfo
```
Provides APIs for vehicle identification information and capability info. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getCarInfo.

**Functions**
- **checkCRNInfoCapabilities** — Gets CRN info capabilities. `[perm: CAR_INFO]`
  - `fun checkCRNInfoCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkVINInfoCapabilities** — Gets VIN info capabilities. `[perm: CAR_INFO]` (protection: normal)
  - `fun checkVINInfoCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **getCRNInfo** — Gets the Connected Resource Name (CRN) Info. `[perm: CAR_INFO]`
  - `fun getCRNInfo(onComplete: (String?) -> Unit, onFailed: (Exception) -> Unit)`
- **getVINInfo** — Gets the Vehicle Identification Number (VIN). `[perm: CAR_INFO]` (protection: normal)
  - `fun getVINInfo(onComplete: (String?) -> Unit, onFailed: (Exception) -> Unit)`

#### `ai.pleos.playground.vehicle.api.Display`
```kotlin
interface Display
```
Provides APIs for temperature display unit. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getDisplay.

**Functions**
- **checkTemperatureDisplayUnitCapabilities** — Gets temperature display unit capabilities. `[perm: CAR_INFO]` (protection: normal)
  - `fun checkTemperatureDisplayUnitCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentTemperatureDisplayUnit** — Gets current temperature units for display. `[perm: CAR_INFO]` (protection: normal)
  - `fun getCurrentTemperatureDisplayUnit(onComplete: (TemperatureUnit?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerTemperatureDisplayUnit** — Registers TemperatureDisplayUnit to get property updates. `[perm: CAR_INFO]` (protection: normal)
  - `fun registerTemperatureDisplayUnit(listener: TemperatureDisplayUnitListener)`
- **unregisterTemperatureDisplayUnit** — Stops getting property updates for the given TemperatureDisplayUnit. `[perm: CAR_INFO]` (protection: normal)
  - `fun unregisterTemperatureDisplayUnit(listener: TemperatureDisplayUnitListener)`

#### `ai.pleos.playground.vehicle.api.Door`
```kotlin
interface Door
```
Provides APIs for door lock, door position, and glove box. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getDoor.

**Functions**
- **checkDoorLockCapabilities** — Gets DoorLock capabilities. `[perm: CAR_DOORS]` (protection: normal)
  - `fun checkDoorLockCapabilities(onComplete: (List<DoorArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkDoorLockCapabilities(onComplete: (CapabilityConfig, List<DoorArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkDoorPositionCapabilities** — Gets DoorPosition capabilities. `[perm: CAR_DOORS]` (protection: normal)
  - `fun checkDoorPositionCapabilities(onComplete: (Map<DoorArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkDoorPositionCapabilities(onComplete: (CapabilityConfig, Map<DoorArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets DoorPosition capabilities. The callback receives CapabilityConfig (access/support) and a Map of DoorArea to AreaBounds with per-area AreaBounds.minValue/AreaBounds.maxValue.
- **checkGloveBoxLockCapabilities** — Gets the glove box lock capabilities. `[perm: CAR_DOORS]` (protection: normal)
  - `fun checkGloveBoxLockCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentDoorPosition** — Gets the current door position state. `[perm: CAR_DOORS]` (protection: normal)
  - `fun getCurrentDoorPosition(area: DoorArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentGloveBoxLockState** — Gets the current glove lock state. `[perm: CAR_DOORS]` (protection: normal)
  - `fun getCurrentGloveBoxLockState(onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getDoorLockState** — Gets current door lock state. `[perm: CAR_DOORS]` (protection: normal)
  - `fun getDoorLockState(area: DoorArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerDoorLockState** — Registers the door lock state to get property updates. `[perm: CAR_DOORS]` (protection: normal)
  - `fun registerDoorLockState(area: DoorArea, listener: DoorLockStateListener)`
- **registerDoorPosition** — Registers the door area to get property updates. `[perm: CAR_DOORS]` (protection: normal)
  - `fun registerDoorPosition(area: DoorArea, listener: DoorPositionListener)`
- **registerGloveBoxLockState** — Registers the glove box lock state to get property updates. `[perm: CAR_DOORS]` (protection: normal)
  - `fun registerGloveBoxLockState(listener: GloveBoxLockStateListener)`
- **setDoorLock** — Sets the current door lock state. `[perm: CONTROL_CAR_DOORS]` (protection: normal)
  - `fun setDoorLock(area: DoorArea, value: Boolean, onComplete: (DoorArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setDoorPosition** — Sets the current door position state. The maximum (fully open) value is vehicle-dependent and is not fixed. Call checkDoorPositionCapabilities first. That API's callback returns CapabilityConfig plus a Map<DoorArea, AreaBounds<Int>>; read each area's AreaBounds.minValue/AreaBounds.maxValue from that map (not from CapabilityConfig). Min is typically 0 for fully closed. `[perm: CONTROL_CAR_DOORS]` (protection: normal)
  - `fun setDoorPosition(area: DoorArea, value: Int, onComplete: (DoorArea, Int?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setGloveBoxLock** — Sets the current glove lock state. `[perm: CONTROL_CAR_DOORS]` (protection: normal)
  - `fun setGloveBoxLock(value: Boolean, onComplete: (Boolean?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **unregisterDoorLockState** — Stops getting property updates for the given door lock state. `[perm: CAR_DOORS]` (protection: normal)
  - `fun unregisterDoorLockState(area: DoorArea, listener: DoorLockStateListener)`
- **unregisterDoorPosition** — Stops getting property updates for the given door area. `[perm: CAR_DOORS]` (protection: normal)
  - `fun unregisterDoorPosition(area: DoorArea, listener: DoorPositionListener)`
- **unregisterGloveBoxLockState** — Stops getting property updates for the given glove box lock state. `[perm: CAR_DOORS]` (protection: normal)
  - `fun unregisterGloveBoxLockState(listener: GloveBoxLockStateListener)`

#### `ai.pleos.playground.vehicle.api.DrivingMode`
```kotlin
interface DrivingMode
```
Provides APIs for driving mode and cruise control. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getDrivingMode.

**Functions**
- **checkCruiseControlStateCapabilities** — Gets CruiseControlState capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkCruiseControlStateCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkCruiseControlTargetSpeedCapabilities** — Gets CruiseControlTargetSpeed capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkCruiseControlTargetSpeedCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkDrivingModeStateCapabilities** — Gets DrivingModeState capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkDrivingModeStateCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **getCruiseControlState** — Gets the current cruise control state. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getCruiseControlState(onComplete: (CruiseControl?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCruiseControlTargetSpeed** — Gets the current cruise control target speed in meters per second (m/s). `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getCruiseControlTargetSpeed(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentDrivingModeState** — Gets current driving mode state. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getCurrentDrivingModeState(onComplete: (DrivingModeType?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerCruiseControlState** — Registers CruiseControlState to get property updates. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerCruiseControlState(listener: CruiseControlStateListener)`
- **registerCruiseControlTargetSpeed** — Registers a listener for cruise control target speed updates in meters per second (m/s). `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerCruiseControlTargetSpeed(listener: CruiseControlTargetSpeedListener)`
- **registerDrivingModeState** — Registers DrivingModeState to get property updates. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerDrivingModeState(listener: DrivingModeStateListener)`
- **unregisterCruiseControlState** — Stops getting property updates for the given CruiseControlState. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterCruiseControlState(listener: CruiseControlStateListener)`
- **unregisterCruiseControlTargetSpeed** — Stops getting property updates for the given CruiseControlTargetSpeed. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterCruiseControlTargetSpeed(listener: CruiseControlTargetSpeedListener)`
- **unregisterDrivingModeState** — Stops getting property updates for the given DrivingModeState. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterDrivingModeState(listener: DrivingModeStateListener)`

#### `ai.pleos.playground.vehicle.api.EvBattery`
```kotlin
interface EvBattery
```
Provides APIs for EV battery efficiency. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getEvBattery.

**Functions**
- **checkCumulativeEvEfficiencyCapabilities** — Gets Cumulative EV Efficiency capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkCumulativeEvEfficiencyCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkCurrentEvEfficiencyCapabilities** — Gets Current EV Efficiency capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkCurrentEvEfficiencyCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkEvBatteryStateOfChargeCapabilities** — Gets EV Battery State Of Charge capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkEvBatteryStateOfChargeCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkEvChargeCurrentDrawLimitCapabilities** — Gets the supported EV charge current draw limits in amperes (A). / Gets capabilities for EV charge current draw limits in amperes (A). `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkEvChargeCurrentDrawLimitCapabilities(onComplete: (List<Float>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkEvChargeCurrentDrawLimitCapabilities(onComplete: (CapabilityConfig, List<Float>) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets capabilities for EV charge current draw limits in amperes (A).
- **checkEvChargePercentLimitCapabilities** — Gets capabilities for EV charge limits, each expressed as a percentage (%). `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkEvChargePercentLimitCapabilities(onComplete: (CapabilityConfig, List<Float>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkEvChargePortOpenCapabilities** — Gets EvChargePortOpen capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkEvChargePortOpenCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkEvChargeSwitchCapabilities** — Gets EvChargeSwitch capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkEvChargeSwitchCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkInstantaneousEvEfficiencyCapabilities** — Gets Instantaneous EV Efficiency capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkInstantaneousEvEfficiencyCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSinceChargeEvEfficiencyCapabilities** — Gets EV Efficiency Since Charge capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkSinceChargeEvEfficiencyCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **getCumulativeEvEfficiency** — Gets cumulative EV efficiency. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getCumulativeEvEfficiency(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets cumulative EV efficiency. Cumulative EV efficiency. The cumulative EV efficiency of the vehicle calculated over its operating period. For instantaneous EV efficiency, see the INSTANTANEOUS_EV_EFFICIENCY vehicle property. Units follow km/kWh, the same as INSTANTANEOUS_EV_EFFICIENCY.
- **getCurrentEvEfficiency** — Gets current EV efficiency. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getCurrentEvEfficiency(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets current EV efficiency. Current EV efficiency in km/kWh. The EV efficiency of the vehicle during the current driving session.
- **getEvBatteryStateOfCharge** — Gets EV battery state of charge as a percentage (%). `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getEvBatteryStateOfCharge(onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets EV battery state of charge as a percentage (%). Current EV battery percentage. The EV battery state of charge of the vehicle is reported as a percentage (%).
- **getEvChargeCurrentDrawLimit** — Gets the EV charge current draw limit in amperes (A). `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getEvChargeCurrentDrawLimit(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
- **getEvChargeSwitchState** — Gets the current EV battery charging state. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getEvChargeSwitchState(onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getInstantaneousEvEfficiency** — Gets instantaneous EV efficiency. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getInstantaneousEvEfficiency(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets instantaneous EV efficiency. Instantaneous EV efficiency in km/kWh. The instantaneous EV battery efficiency of the vehicle is reported in units of km/kWh. This API will be deprecated in the future, as the latest AAOS provides INSTANTANEOUS_EV_EFFICIENCY as a standard vehicle property.
- **getSinceChargeEvEfficiency** — Gets EV efficiency since charge. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getSinceChargeEvEfficiency(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets EV efficiency since charge. EV efficiency after recharge. The EV efficiency of the vehicle measured since the last recharge. For instantaneous EV efficiency, see the INSTANTANEOUS_EV_EFFICIENCY vehicle property. Units follow km/kWh, the same as INSTANTANEOUS_EV_EFFICIENCY.
- **registerCumulativeEvEfficiency** — Registers Cumulative EV Efficiency listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun registerCumulativeEvEfficiency(listener: EvBatteryEfficiencyListener)`
- **registerCurrentEvEfficiency** — Registers Current EV Efficiency listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun registerCurrentEvEfficiency(listener: EvBatteryEfficiencyListener)`
- **registerEvBatteryStateOfCharge** — Registers a listener for EV battery state of charge updates expressed as a percentage (%). `[perm: CAR_ENERGY]` (protection: normal)
  - `fun registerEvBatteryStateOfCharge(listener: EvBatteryStateOfChargeListener)`
- **registerInstantaneousEvEfficiency** — Registers Instantaneous EV Efficiency listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun registerInstantaneousEvEfficiency(listener: EvBatteryEfficiencyListener)`
- **registerSinceChargeEvEfficiency** — Registers EV Efficiency Since Charge listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun registerSinceChargeEvEfficiency(listener: EvBatteryEfficiencyListener)`
- **setEvChargeCurrentDrawLimit** — Sets the EV charge current draw limit in amperes (A). `[perm: CONTROL_CAR_ENERGY]` (protection: normal)
  - `fun setEvChargeCurrentDrawLimit(value: Float, onComplete: (Float?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **setEvChargePercentLimit** — Sets the battery state-of-charge limit for EV charging, expressed as a percentage (%). `[perm: CONTROL_CAR_ENERGY]` (protection: normal)
  - `fun setEvChargePercentLimit(value: Float, onComplete: (Float?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
  - 상세: Sets the battery state-of-charge limit for EV charging, expressed as a percentage (%). Use checkEvChargePercentLimitCapabilities to retrieve the values supported by the vehicle.
- **setEvChargePortOpen** — Sets the EV charge port open state to On or Off. `[perm: CONTROL_CAR_ENERGY]` (protection: normal)
  - `fun setEvChargePortOpen(value: Boolean, onComplete: (Boolean?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **setEvChargeSwitch** — Sets whether to start or stop EV battery charging. `[perm: CONTROL_CAR_ENERGY]` (protection: normal)
  - `fun setEvChargeSwitch(value: Boolean, onComplete: (Boolean?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **unregisterCumulativeEvEfficiency** — Unregisters Cumulative EV Efficiency listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun unregisterCumulativeEvEfficiency(listener: EvBatteryEfficiencyListener)`
- **unregisterCurrentEvEfficiency** — Unregisters Current EV Efficiency listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun unregisterCurrentEvEfficiency(listener: EvBatteryEfficiencyListener)`
- **unregisterEvBatteryStateOfCharge** — Unregisters EV Battery State Of Charge listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun unregisterEvBatteryStateOfCharge(listener: EvBatteryStateOfChargeListener)`
- **unregisterInstantaneousEvEfficiency** — Unregisters Instantaneous EV Efficiency listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun unregisterInstantaneousEvEfficiency(listener: EvBatteryEfficiencyListener)`
- **unregisterSinceChargeEvEfficiency** — Unregisters EV Efficiency Since Charge listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun unregisterSinceChargeEvEfficiency(listener: EvBatteryEfficiencyListener)`

#### `ai.pleos.playground.vehicle.api.Fuel`
```kotlin
interface Fuel
```
Provides APIs for fuel economy. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getFuel.

**Functions**
- **checkCumulativeFuelEconomyCapabilities** — Gets Cumulative Fuel Economy capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkCumulativeFuelEconomyCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkCurrentFuelEconomyCapabilities** — Gets Current Fuel Economy capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkCurrentFuelEconomyCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkFuelPercentCapabilities** — Gets Fuel Percent capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkFuelPercentCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkInstantaneousFuelEconomyCapabilities** — Gets Instantaneous Fuel Economy capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkInstantaneousFuelEconomyCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSinceChargeFuelEconomyCapabilities** — Gets Fuel Economy Since Refuel capabilities. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun checkSinceChargeFuelEconomyCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **getCumulativeFuelEconomy** — Gets cumulative fuel economy. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getCumulativeFuelEconomy(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets cumulative fuel economy. Cumulative fuel economy. The cumulative fuel economy of the vehicle calculated over its operating period. For instantaneous fuel economy, see the INSTANTANEOUS_FUEL_ECONOMY vehicle property. Units follow L/100 km, the same as INSTANTANEOUS_FUEL_ECONOMY.
- **getCurrentFuelEconomy** — Gets current fuel economy. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getCurrentFuelEconomy(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets current fuel economy. Current fuel economy in L/100 km. The fuel economy of the vehicle during the current driving session.
- **getFuelPercent** — Gets the fuel level as a percentage (%). `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getFuelPercent(onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets the fuel level as a percentage (%). Fuel percentage. The current fuel level of the vehicle is reported as a percentage (%).
- **getInstantaneousFuelEconomy** — Gets instantaneous fuel economy. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getInstantaneousFuelEconomy(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets instantaneous fuel economy. Instantaneous Fuel Economy in L/100 km. The instantaneous fuel economy of the vehicle is reported in units of L/100 km. This API will be deprecated in the future, as the latest AAOS provides INSTANTANEOUS_FUEL_ECONOMY as a standard vehicle property.
- **getSinceChargeFuelEconomy** — Gets fuel economy since refuel. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun getSinceChargeFuelEconomy(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets fuel economy since refuel. Fuel economy after refuel. The fuel economy of the vehicle measured since the last refuel. For instantaneous fuel economy, see the INSTANTANEOUS_FUEL_ECONOMY vehicle property. Units follow L/100 km, the same as INSTANTANEOUS_FUEL_ECONOMY.
- **registerCumulativeFuelEconomy** — Registers Cumulative Fuel Economy listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun registerCumulativeFuelEconomy(listener: FuelEconomyListener)`
- **registerCurrentFuelEconomy** — Registers Current Fuel Economy listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun registerCurrentFuelEconomy(listener: FuelEconomyListener)`
- **registerFuelPercent** — Registers a listener for fuel level updates expressed as a percentage (%). `[perm: CAR_ENERGY]` (protection: normal)
  - `fun registerFuelPercent(listener: FuelPercentListener)`
- **registerInstantaneousFuelEconomy** — Registers Instantaneous Fuel Economy listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun registerInstantaneousFuelEconomy(listener: FuelEconomyListener)`
- **registerSinceChargeFuelEconomy** — Registers Fuel Economy Since Refuel listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun registerSinceChargeFuelEconomy(listener: FuelEconomyListener)`
- **unregisterCumulativeFuelEconomy** — Unregisters Cumulative Fuel Economy listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun unregisterCumulativeFuelEconomy(listener: FuelEconomyListener)`
- **unregisterCurrentFuelEconomy** — Unregisters Current Fuel Economy listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun unregisterCurrentFuelEconomy(listener: FuelEconomyListener)`
- **unregisterFuelPercent** — Unregisters Fuel Percent listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun unregisterFuelPercent(listener: FuelPercentListener)`
- **unregisterInstantaneousFuelEconomy** — Unregisters Instantaneous Fuel Economy listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun unregisterInstantaneousFuelEconomy(listener: FuelEconomyListener)`
- **unregisterSinceChargeFuelEconomy** — Unregisters Fuel Economy Since Refuel listener. `[perm: CAR_ENERGY]` (protection: normal)
  - `fun unregisterSinceChargeFuelEconomy(listener: FuelEconomyListener)`

#### `ai.pleos.playground.vehicle.api.HVAC`
```kotlin
interface HVAC
```
Provides APIs for climate control (HVAC). Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getHVAC.

**Functions**
- **checkHvacAcCapabilities** — Gets HvacAC capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacAcCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacAcCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHvacAirCleanerCapabilities** — Gets HVAC air cleaner capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacAirCleanerCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacAirCleanerCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHvacAutoCapabilities** — Gets HvacAuto capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacAutoCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacAutoCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHvacAutoRecirculationCapabilities** — Gets HVAC auto recirculation capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacAutoRecirculationCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacAutoRecirculationCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHvacDefrosterCapabilities** — Gets HVAC defroster capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacDefrosterCapabilities(onComplete: (List<WindowArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacDefrosterCapabilities(onComplete: (CapabilityConfig, List<WindowArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHvacDualCapabilities** — Gets HVAC dual capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacDualCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacDualCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHvacFanDirectionCapabilities** — Gets HVAC fan direction capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacFanDirectionCapabilities(onComplete: (Map<SeatArea, List<FanDirectionType>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacFanDirectionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, List<FanDirectionType>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHvacFanSpeedCapabilities** — Gets HVAC fan speed capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacFanSpeedCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacFanSpeedCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets HVAC fan speed capabilities. The callback receives CapabilityConfig (access/support) and a Map of SeatArea to AreaBounds with per-area AreaBounds.minValue/AreaBounds.maxValue.
- **checkHvacMaxAcCapabilities** — Gets HVAC max AC capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacMaxAcCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacMaxAcCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHvacMaxDefrostCapabilities** — Gets HVAC max defrost capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacMaxDefrostCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacMaxDefrostCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHvacPowerCapabilities** — Gets HVAC power capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacPowerCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacPowerCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHvacRecirculationCapabilities** — Gets HVAC recirculation capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacRecirculationCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacRecirculationCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHvacTemperatureCapabilities** — Gets the supported target temperature ranges in degrees Celsius (°C). / Gets HVAC temperature range capabilities as AreaBounds in degrees Celsius (°C). / Gets HVAC temperature capabilities including supported temperature ranges in degrees Celsius (°C) and the vehicle-specific temperature conversion config array. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacTemperatureCapabilities(onComplete: (Map<SeatArea, AreaBounds<Float>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacTemperatureCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Float>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacTemperatureCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Float>>, HvacTemperatureConfigArray?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets HVAC temperature capabilities including supported temperature ranges in degrees Celsius (°C) and the vehicle-specific temperature conversion config array. The returned HvacTemperatureConfigArray is derived from the vehicle's temperature setting configuration array and is used to convert temperature values between Celsius and Fahrenheit using the vehicle-specific rule (step/increment based mapping), instead of the generic conversion formula. HvacTemperatureConfigArray is the parsed form of the vehicle's temperature conversion config array and maps to the original indexes as follows: [0] ->HvacTemperatureConfigArray.minCelsius [1] ->HvacTemperatureConfigArray.maxCelsius [2] ->HvacTemperatureConfigArray.incrementCelsius [3] ->HvacTemperatureConfigArray.minFahrenheit [4] ->HvacTemperatureConfigArray.maxFahrenheit [5] ->HvacTemperatureConfigArray.incrementFahrenheit Each value is normalized to Float (raw / 10f). The original integer VHAL values are preserved in HvacTemperatureConfigArray.raw. Typical usage: CapabilityConfig reports access/support only; AreaBounds comes from the second callback argument. Use each area's AreaBounds.minValue/AreaBounds.maxValue for the supported Celsi
- **checkHvacTemperatureCurrentCapabilities** — Gets HVAC temperature current capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacTemperatureCurrentCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacAcState** — Gets the current AC state. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacAcState(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacAirCleanerState** — Gets the current air cleaner state. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacAirCleanerState(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacAutoRecirculationState** — Gets the current automatic recirculation. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacAutoRecirculationState(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacAutoState** — Gets the current automatic mode. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacAutoState(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacDefrosterState** — Gets the current defroster state. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacDefrosterState(area: WindowArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacDualState** — Gets the current temperature coupling state between areas. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacDualState(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacFanDirection** — Gets the current fan direction. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacFanDirection(area: SeatArea, onComplete: (FanDirectionType?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacFanSpeed** — Gets the current fan speed. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacFanSpeed(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacMaxAcState** — Gets the current max AC state. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacMaxAcState(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacMaxDefrostState** — Gets the current max defrost state. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacMaxDefrostState(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacPowerState** — Gets the current global power state. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacPowerState(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacRecirculationState** — Gets the current recirculation state. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacRecirculationState(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacTemperature** — Gets the target temperature in the specified unit. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacTemperature(area: SeatArea, unit: TemperatureUnit, onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacTemperatureCurrent** — Gets the current internal temperature of the vehicle in degrees Celsius (°C). / Gets the current internal temperature of the vehicle in the requested unit. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun getHvacTemperatureCurrent(area: SeatArea, onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun getHvacTemperatureCurrent(area: SeatArea, unit: TemperatureUnit, onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets the current internal temperature of the vehicle in degrees Celsius (°C).
- **registerHvacAcState** — Registers the HvacAC state to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacAcState(area: SeatArea, listener: HvacAcStateListener)`
- **registerHvacAirCleanerState** — Registers the HvacAirCleaner state to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacAirCleanerState(area: SeatArea, listener: HvacAirCleanerStateListener)`
- **registerHvacAutoRecirculationState** — Registers the HvacAutoRecirculation state to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacAutoRecirculationState(area: SeatArea, listener: HvacRecirculationStateListener)`
- **registerHvacAutoState** — Registers the HvacAuto state to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacAutoState(area: SeatArea, listener: HvacAutoStateListener)`
- **registerHvacDefrosterState** — Registers the HvacDefroster state to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacDefrosterState(area: WindowArea, listener: HvacDefrosterStateListener)`
- **registerHvacDualState** — Registers the HvacDual state to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacDualState(area: SeatArea, listener: HvacDualStateListener)`
- **registerHvacFanSpeed** — Registers the HvacFanSpeed to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacFanSpeed(area: SeatArea, listener: HvacFanSpeedListener)`
- **registerHvacMaxAcState** — Registers the HvacMaxAc state to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacMaxAcState(area: SeatArea, listener: HvacMaxAcStateListener)`
- **registerHvacMaxDefrostState** — Registers the HvacMaxDefrost state to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacMaxDefrostState(area: SeatArea, listener: HvacMaxDefrostStateListener)`
- **registerHvacPowerState** — Registers the HvacPower state to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacPowerState(area: SeatArea, listener: HvacPowerStateListener)`
- **registerHvacRecirculationState** — Registers the HvacRecirculation to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacRecirculationState(area: SeatArea, listener: HvacRecirculationStateListener)`
- **registerHvacTemperature** — Registers a listener for target temperature updates in degrees Celsius (°C) only. / Registers the HvacTemperature to get property updates in degrees Celsius (°C) and degrees Fahrenheit (°F). `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacTemperature(area: SeatArea, listener: HvacTemperatureListener)`
  - `fun registerHvacTemperature(area: SeatArea, listener: HvacTemperatureValueListener)`
  - 상세: Registers a listener for target temperature updates in degrees Celsius (°C) only.
- **registerHvacTemperatureCurrent** — Registers a listener for current interior temperature updates in degrees Celsius (°C). / Registers HvacTemperatureCurrent updates in degrees Celsius (°C) and degrees Fahrenheit (°F). `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacTemperatureCurrent(area: SeatArea, listener: HvacTemperatureListener)`
  - `fun registerHvacTemperatureCurrent(area: SeatArea, listener: HvacTemperatureValueListener)`
  - 상세: Registers a listener for current interior temperature updates in degrees Celsius (°C).
- **setHvacAc** — Sets AC to On/Off for the designated areaId. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacAc(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacAirCleaner** — Sets the air cleaner to On/Off. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacAirCleaner(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacAuto** — Sets automatic mode to On/Off. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacAuto(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacAutoRecirculation** — Sets the automatic recirculation to On/Off. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacAutoRecirculation(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacDefroster** — Sets the defroster to On/Off. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacDefroster(area: WindowArea, value: Boolean, onComplete: (WindowArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacDual** — Enable temperature coupling between areas. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacDual(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacFanDirection** — Sets fan direction. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacFanDirection(area: SeatArea, value: FanDirectionType, onComplete: (SeatArea, FanDirectionType?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacFanSpeed** — Sets the fan speed. The supported fan speed range is vehicle-dependent and is not fixed. Call checkHvacFanSpeedCapabilities first. That API's callback returns CapabilityConfig plus a Map<SeatArea, AreaBounds<Int>>; read each area's AreaBounds.minValue/AreaBounds.maxValue from that map (not from CapabilityConfig). `[perm: CONTROL_CAR_CLIMATE]` (protection: normal)
  - `fun setHvacFanSpeed(area: SeatArea, value: Int, onComplete: (SeatArea, Int?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacMaxAc** — Sets the max AC to On/Off. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacMaxAc(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacMaxDefrost** — Sets the max defrost to On/Off. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacMaxDefrost(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacPower** — Sets the global power state to On/Off. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacPower(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacRecirculation** — Sets recirculation to On/Off. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacRecirculation(area: SeatArea, value: Boolean, onComplete: (SeatArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacTemperature** — Sets the target temperature in the specified unit. The supported temperature range is vehicle-dependent and is not fixed. Call checkHvacTemperatureCapabilities first. That API's callback returns CapabilityConfig, a Map<SeatArea, AreaBounds<Float>>, and optional HvacTemperatureConfigArray; read each area's AreaBounds.minValue/AreaBounds.maxValue from that map (not from CapabilityConfig). AreaBounds … `[perm: CONTROL_CAR_CLIMATE]` (protection: normal)
  - `fun setHvacTemperature(area: SeatArea, value: Float, unit: TemperatureUnit, onComplete: (SeatArea, Float?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **unregisterHvacAcState** — Stops getting property updates for the given HvacAC state. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacAcState(area: SeatArea, listener: HvacAcStateListener)`
- **unregisterHvacAirCleanerState** — Stops getting property updates for the given HvacAirCleaner state. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacAirCleanerState(area: SeatArea, listener: HvacAirCleanerStateListener)`
- **unregisterHvacAutoRecirculationState** — Stops getting property updates for the given HvacAutoRecirculation state. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacAutoRecirculationState(area: SeatArea, listener: HvacRecirculationStateListener)`
- **unregisterHvacAutoState** — Stops getting property updates for the given HvacAuto state. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacAutoState(area: SeatArea, listener: HvacAutoStateListener)`
- **unregisterHvacDefrosterState** — Stops getting property updates for the given HvacDefroster state. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacDefrosterState(area: WindowArea, listener: HvacDefrosterStateListener)`
- **unregisterHvacDualState** — Stops getting property updates for the given HvacDual state. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacDualState(area: SeatArea, listener: HvacDualStateListener)`
- **unregisterHvacFanSpeed** — Stops getting property updates for the given HvacFanSpeed. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacFanSpeed(area: SeatArea, listener: HvacFanSpeedListener)`
- **unregisterHvacMaxAcState** — Stop getting property updates for the given HvacMaxAc state. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacMaxAcState(area: SeatArea, listener: HvacMaxAcStateListener)`
- **unregisterHvacMaxDefrostState** — Stops getting property updates for the given HvacMaxDefrost state. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacMaxDefrostState(area: SeatArea, listener: HvacMaxDefrostStateListener)`
- **unregisterHvacPowerState** — Stops getting property updates for the given HvacPower state. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacPowerState(area: SeatArea, listener: HvacPowerStateListener)`
- **unregisterHvacRecirculationState** — Stops getting property updates for the given HvacRecirculation. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacRecirculationState(area: SeatArea, listener: HvacRecirculationStateListener)`
- **unregisterHvacTemperature** — Stops getting property updates for the given HvacTemperature. / Stops getting HVAC temperature updates for the given listener that receives both Celsius and Fahrenheit values. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacTemperature(area: SeatArea, listener: HvacTemperatureListener)`
  - `fun unregisterHvacTemperature(area: SeatArea, listener: HvacTemperatureValueListener)`
  - 상세: Stops getting property updates for the given HvacTemperature.
- **unregisterHvacTemperatureCurrent** — Stops getting property updates for the given HvacTemperatureCurrent. / Stops getting HvacTemperatureCurrent updates for the listener receiving both Celsius and Fahrenheit values. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacTemperatureCurrent(area: SeatArea, listener: HvacTemperatureListener)`
  - `fun unregisterHvacTemperatureCurrent(area: SeatArea, listener: HvacTemperatureValueListener)`
  - 상세: Stops getting property updates for the given HvacTemperatureCurrent.

#### `ai.pleos.playground.vehicle.api.Light`
```kotlin
interface Light
```
Provides APIs for exterior, cabin, and ambient lights. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getLight.

**Functions**
- **checkAmbientColorAutoByDriveModeEnabledCapabilities** — Gets Ambient Color Auto By Drive Mode Enabled capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkAmbientColorAutoByDriveModeEnabledCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkAmbientLightBrightnessCapabilities** — Gets ambient light brightness capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkAmbientLightBrightnessCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkAmbientLightBrightnessColorCapabilities** — Gets ambient light brightness color capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkAmbientLightBrightnessColorCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkAmbientLightStateCapabilities** — Gets ambient light state capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkAmbientLightStateCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, List<LightState>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkAmbientLightSwitchCapabilities** — Gets ambient light switch capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkAmbientLightSwitchCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, List<LightSwitch>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkCabinLightStateCapabilities** — Gets cabin light state capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkCabinLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkCabinLightSwitchCapabilities** — Gets cabin light switch capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkCabinLightSwitchCapabilities(onComplete: (List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkCabinLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkFrontFogLightStateCapabilities** — Gets front fog light state capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkFrontFogLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkFrontFogLightSwitchCapabilities** — Gets front fog light switch capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkFrontFogLightSwitchCapabilities(onComplete: (List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkFrontFogLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHighBeamLightStateCapabilities** — Gets high beam light state capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkHighBeamLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkHighBeamLightSwitchCapabilities** — Gets high beam light switch capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkHighBeamLightSwitchCapabilities(onComplete: (List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHighBeamLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkLowBeamLightStateCapabilities** — Gets low beam light state capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkLowBeamLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkLowBeamLightSwitchCapabilities** — Gets low beam light switch capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkLowBeamLightSwitchCapabilities(onComplete: (List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkLowBeamLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkReadingLightStateCapabilities** — Gets reading light state capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkReadingLightStateCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, List<LightState>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkReadingLightSwitchCapabilities** — Gets reading light switch capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkReadingLightSwitchCapabilities(onComplete: (Map<SeatArea, List<LightSwitch>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkReadingLightSwitchCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, List<LightSwitch>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkRearFogLightStateCapabilities** — Gets rear fog light state capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkRearFogLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkRearFogLightSwitchCapabilities** — Gets rear fog light switch capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkRearFogLightSwitchCapabilities(onComplete: (List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkRearFogLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTailLampLightStateCapabilities** — Gets tail lamp light state capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkTailLampLightStateCapabilities(onComplete: (CapabilityConfig, List<LightState>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTailLampLightSwitchCapabilities** — Gets tail lamp light switch capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkTailLampLightSwitchCapabilities(onComplete: (List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkTailLampLightSwitchCapabilities(onComplete: (CapabilityConfig, List<LightSwitch>) -> Unit, onFailed: (Exception) -> Unit)`
- **getAmbientColorAutoByDriveModeEnabled** — Gets whether Ambient Color Auto By Drive Mode is enabled. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getAmbientColorAutoByDriveModeEnabled(onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getAmbientLightBrightness** — Gets the current Ambient Light Brightness for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getAmbientLightBrightness(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getAmbientLightBrightnessColor** — Gets the current Ambient Light Brightness Color (32-bit ARGB) for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getAmbientLightBrightnessColor(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets the current Ambient Light Brightness Color (32-bit ARGB) for the specified seat area. Alpha is fixed to 0xFF (fully opaque).
- **getAmbientLightState** — Gets the current Ambient Light State for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getAmbientLightState(area: SeatArea, onComplete: (LightState?) -> Unit, onFailed: (Exception) -> Unit)`
- **getAmbientLightSwitch** — Gets the current Ambient Light Switch for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getAmbientLightSwitch(area: SeatArea, onComplete: (LightSwitch?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCabinLightSwitch** — Gets the current cabin light switch. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getCabinLightSwitch(onComplete: (LightSwitch?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentCabinLightState** — Gets the current cabin light state. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getCurrentCabinLightState(onComplete: (LightState?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentFrontFogLightState** — Gets the current front fog light state. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getCurrentFrontFogLightState(onComplete: (LightState?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentHighBeamLightState** — Gets the current high beam light state. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getCurrentHighBeamLightState(onComplete: (LightState?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentLowBeamLightState** — Gets the current low beam light state. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getCurrentLowBeamLightState(onComplete: (LightState?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentReadingLightState** — Gets the current reading light state. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getCurrentReadingLightState(area: SeatArea, onComplete: (LightState?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentRearFogLightState** — Gets the current rear fog light state. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getCurrentRearFogLightState(onComplete: (LightState?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentTailLampLightState** — Gets the current tail lamp light state. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getCurrentTailLampLightState(onComplete: (LightState?) -> Unit, onFailed: (Exception) -> Unit)`
- **getFrontFogLightSwitch** — Gets the current front fog light switch. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getFrontFogLightSwitch(onComplete: (LightSwitch?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHighBeamLightSwitch** — Gets the current high beam light switch. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getHighBeamLightSwitch(onComplete: (LightSwitch?) -> Unit, onFailed: (Exception) -> Unit)`
- **getLowBeamLightSwitch** — Gets the current low beam light switch. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getLowBeamLightSwitch(onComplete: (LightSwitch?) -> Unit, onFailed: (Exception) -> Unit)`
- **getReadingLightSwitch** — Gets the current reading light switch. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getReadingLightSwitch(area: SeatArea, onComplete: (LightSwitch?) -> Unit, onFailed: (Exception) -> Unit)`
- **getRearFogLightSwitch** — Gets the current rear fog light switch. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getRearFogLightSwitch(onComplete: (LightSwitch?) -> Unit, onFailed: (Exception) -> Unit)`
- **getTailLampLightSwitch** — Gets the current tail lamp light switch. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getTailLampLightSwitch(onComplete: (LightSwitch?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerAmbientColorAutoByDriveModeEnabled** — Registers a listener to receive Ambient Color Auto By Drive Mode Enabled updates. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerAmbientColorAutoByDriveModeEnabled(listener: AmbientColorAutoByDriveModeEnabledListener)`
- **registerAmbientLightBrightnessColorListener** — Registers a listener to receive Ambient Light Brightness Color updates for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerAmbientLightBrightnessColorListener(area: SeatArea, listener: AmbientLightBrightnessColorListener)`
- **registerAmbientLightBrightnessListener** — Registers a listener to receive Ambient Light Brightness updates for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerAmbientLightBrightnessListener(area: SeatArea, listener: AmbientLightBrightnessListener)`
- **registerAmbientLightStateListener** — Registers a listener to receive Ambient Light State updates for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerAmbientLightStateListener(area: SeatArea, listener: LightStateListener)`
- **registerCabinLightState** — Registers the CabinLightState to get property updates. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerCabinLightState(listener: LightStateListener)`
- **registerFrontFogLightState** — Registers the FrontFogLightState to get property updates. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerFrontFogLightState(listener: LightStateListener)`
- **registerHighBeamLightState** — Registers the HighBeamLightState to get property updates. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerHighBeamLightState(listener: LightStateListener)`
- **registerLowBeamLightState** — Registers the LowBeamLightState to get property updates. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerLowBeamLightState(listener: LightStateListener)`
- **registerReadingLightState** — Registers the ReadingLightState to get property updates. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerReadingLightState(area: SeatArea, listener: LightStateListener)`
- **registerRearFogLightState** — Registers the RearFogLightState to get property updates. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerRearFogLightState(listener: LightStateListener)`
- **registerTailLampLightState** — Registers the TailLampLightState to get property updates. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerTailLampLightState(listener: LightStateListener)`
- **setAmbientColorAutoByDriveModeEnabled** — Sets whether Ambient Color Auto By Drive Mode is enabled. `[perm: CONTROL_CAR_LIGHTS]` (protection: normal)
  - `fun setAmbientColorAutoByDriveModeEnabled(value: Boolean, onComplete: (Boolean?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **setAmbientLightBrightness** — Sets the Ambient Light Brightness for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun setAmbientLightBrightness(area: SeatArea, value: Int, onComplete: (SeatArea, Int) -> Unit, onFailed: (Exception) -> Unit)`
- **setAmbientLightBrightnessColor** — Sets the Ambient Light Brightness Color for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun setAmbientLightBrightnessColor(area: SeatArea, value: Int, onComplete: (SeatArea, Int) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Sets the Ambient Light Brightness Color for the specified seat area. The value is a 32-bit ARGB color. Alpha is fixed to 0xFF (fully opaque).
- **setAmbientLightSwitch** — Sets the Ambient Light Switch for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun setAmbientLightSwitch(area: SeatArea, value: LightSwitch, onComplete: (SeatArea, LightSwitch?) -> Unit, onFailed: (Exception) -> Unit)`
- **setCabinLightSwitch** — Sets the cabin light switch. `[perm: CONTROL_CAR_LIGHTS]` (protection: normal)
  - `fun setCabinLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **setFrontFogLightSwitch** — Sets the front fog light switch. `[perm: CONTROL_CAR_LIGHTS]` (protection: normal)
  - `fun setFrontFogLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **setHighBeamLightSwitch** — Sets the high beam light switch. `[perm: CONTROL_CAR_LIGHTS]` (protection: normal)
  - `fun setHighBeamLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **setLowBeamLightSwitch** — Sets the low beam light switch. `[perm: CONTROL_CAR_LIGHTS]` (protection: normal)
  - `fun setLowBeamLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **setReadingLightSwitch** — Sets the reading light switch. `[perm: CONTROL_CAR_LIGHTS]` (protection: normal)
  - `fun setReadingLightSwitch(area: SeatArea, value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **setRearFogLightSwitch** — Sets the rear fog light switch. `[perm: CONTROL_CAR_LIGHTS]` (protection: normal)
  - `fun setRearFogLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **setTailLampLightSwitch** — Sets the tail lamp light switch. `[perm: CONTROL_CAR_LIGHTS]` (protection: normal)
  - `fun setTailLampLightSwitch(value: LightSwitch, onComplete: (LightSwitch?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **unregisterAmbientColorAutoByDriveModeEnabled** — Unregisters a previously registered Ambient Color Auto By Drive Mode Enabled listener. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterAmbientColorAutoByDriveModeEnabled(listener: AmbientColorAutoByDriveModeEnabledListener)`
- **unregisterAmbientLightBrightnessColorListener** — Unregisters a previously registered Ambient Light Brightness Color listener for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterAmbientLightBrightnessColorListener(area: SeatArea, listener: AmbientLightBrightnessColorListener)`
- **unregisterAmbientLightBrightnessListener** — Unregisters a previously registered Ambient Light Brightness listener for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterAmbientLightBrightnessListener(area: SeatArea, listener: AmbientLightBrightnessListener)`
- **unregisterAmbientLightStateListener** — Unregisters a previously registered Ambient Light State listener for the specified seat area. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterAmbientLightStateListener(area: SeatArea, listener: LightStateListener)`
- **unregisterCabinLightState** — Stops getting property updates for the given CabinLightState. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterCabinLightState(listener: LightStateListener)`
- **unregisterFrontFogLightState** — Stops getting property updates for the given FrontFogLightState. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterFrontFogLightState(listener: LightStateListener)`
- **unregisterHighBeamLightState** — Stops getting property updates for the given HighBeamLightState. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterHighBeamLightState(listener: LightStateListener)`
- **unregisterLowBeamLightState** — Stops getting property updates for the given LowBeamLightState. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterLowBeamLightState(listener: LightStateListener)`
- **unregisterReadingLightState** — Stops getting property updates for the given ReadingLightState. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterReadingLightState(area: SeatArea, listener: LightStateListener)`
- **unregisterRearFogLightState** — Stops getting property updates for the given RearFogLightState. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterRearFogLightState(listener: LightStateListener)`
- **unregisterTailLampLightState** — Stops getting property updates for the given TailLampLightState. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterTailLampLightState(listener: LightStateListener)`

#### `ai.pleos.playground.vehicle.api.Odometer`
```kotlin
interface Odometer
```
Provides APIs for odometer and trip data. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getOdometer.

**Functions**
- **checkCurrentDistanceDrivenCapabilities** — Gets current distance driven capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkCurrentDistanceDrivenCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTripCumulativeDrivingDistanceCapabilities** — Gets Trip Cumulative Driving Distance capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkTripCumulativeDrivingDistanceCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTripCumulativeDrivingTimeCapabilities** — Gets Trip Cumulative Driving Time capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkTripCumulativeDrivingTimeCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTripCurrentDrivingDistanceCapabilities** — Gets Trip Current Driving Distance capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkTripCurrentDrivingDistanceCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTripCurrentDrivingTimeCapabilities** — Gets Trip Current Driving Time capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkTripCurrentDrivingTimeCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTripDrivingDistanceCapabilities** — Gets Trip Driving Distance capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkTripDrivingDistanceCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTripDrivingTimeCapabilities** — Gets Trip Driving Time capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkTripDrivingTimeCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTripInstantaneousDrivingDistanceCapabilities** — Gets Trip Instantaneous Driving Distance capabilities. `[perm: CAR_DRIVE]`
  - `fun checkTripInstantaneousDrivingDistanceCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTripSinceRefillDrivingDistanceCapabilities** — Gets Trip Since Refuel Driving Distance capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkTripSinceRefillDrivingDistanceCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTripSinceRefillDrivingTimeCapabilities** — Gets Trip Since Refuel Driving Time capabilities. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkTripSinceRefillDrivingTimeCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentDistanceDriven** — Current odometer value of the vehicle in kilometers (km). `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getCurrentDistanceDriven(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
- **getTripCumulativeDrivingDistance** — Gets trip cumulative driving distance in meters (m). `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getTripCumulativeDrivingDistance(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets trip cumulative driving distance in meters (m). Trip cumulative driving distance in meters (m). The cumulative driving distance of the vehicle calculated over its operating period.
- **getTripCumulativeDrivingTime** — Gets trip cumulative driving time. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getTripCumulativeDrivingTime(onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets trip cumulative driving time. Trip cumulative driving time in minutes. The cumulative driving time of the vehicle calculated over its operating period.
- **getTripCurrentDrivingDistance** — Gets trip current driving distance in meters (m). `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getTripCurrentDrivingDistance(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets trip current driving distance in meters (m). Trip current driving distance in meters (m). The driving distance of the vehicle during the current driving session.
- **getTripCurrentDrivingTime** — Gets trip current driving time. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getTripCurrentDrivingTime(onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets trip current driving time. Trip current driving time in minutes. The driving time of the vehicle during the current driving session.
- **getTripInstantaneousDrivingDistance** — Gets trip instantaneous driving distance in meters (m). `[perm: CAR_DRIVE]`
  - `fun getTripInstantaneousDrivingDistance(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
- **getTripSinceRefillDrivingDistance** — Gets trip since refuel driving distance in meters (m). `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getTripSinceRefillDrivingDistance(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets trip since refuel driving distance in meters (m). Trip driving distance since refuel in meters (m). The driving distance of the vehicle measured since the last refuel.
- **getTripSinceRefillDrivingTime** — Gets trip since refuel driving time. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getTripSinceRefillDrivingTime(onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets trip since refuel driving time. Trip driving time since refuel in minutes. The driving time of the vehicle measured since the last refuel.
- **registerDistanceDriven** — Registers a listener for odometer updates in kilometers (km). `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerDistanceDriven(listener: DistanceDrivenListener)`
- **registerTripCumulativeDrivingDistance** — Registers a listener for trip cumulative driving distance updates in meters (m). `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerTripCumulativeDrivingDistance(listener: DistanceDrivenListener)`
- **registerTripCumulativeDrivingTime** — Registers Trip Cumulative Driving Time listener. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerTripCumulativeDrivingTime(listener: DrivingTimeListener)`
- **registerTripCurrentDrivingDistance** — Registers a listener for trip current driving distance updates in meters (m). `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerTripCurrentDrivingDistance(listener: DistanceDrivenListener)`
- **registerTripCurrentDrivingTime** — Registers Trip Current Driving Time listener. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerTripCurrentDrivingTime(listener: DrivingTimeListener)`
- **registerTripInstantaneousDrivingDistance** — Registers a listener for trip instantaneous driving distance updates in meters (m). `[perm: CAR_DRIVE]`
  - `fun registerTripInstantaneousDrivingDistance(listener: DistanceDrivenListener)`
- **registerTripSinceRefillDrivingDistance** — Registers a listener for trip since refuel driving distance updates in meters (m). `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerTripSinceRefillDrivingDistance(listener: DistanceDrivenListener)`
- **registerTripSinceRefillDrivingTime** — Registers Trip Since Refuel Driving Time listener. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerTripSinceRefillDrivingTime(listener: DrivingTimeListener)`
- **setRangeRemainingDistance** — Sets the estimated remaining range in meters (m). `[perm: CONTROL_CAR_DRIVE]`
  - `fun setRangeRemainingDistance(value: Float, onComplete: (Float?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **unregisterDistanceDriven** — Stops getting property updates for the given DistanceDriven. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterDistanceDriven(listener: DistanceDrivenListener)`
- **unregisterTripCumulativeDrivingDistance** — Unregisters Trip Cumulative Driving Distance listener. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterTripCumulativeDrivingDistance(listener: DistanceDrivenListener)`
- **unregisterTripCumulativeDrivingTime** — Unregisters Trip Cumulative Driving Time listener. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterTripCumulativeDrivingTime(listener: DrivingTimeListener)`
- **unregisterTripCurrentDrivingDistance** — Unregisters Trip Current Driving Distance listener. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterTripCurrentDrivingDistance(listener: DistanceDrivenListener)`
- **unregisterTripCurrentDrivingTime** — Unregisters Trip Current Driving Time listener. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterTripCurrentDrivingTime(listener: DrivingTimeListener)`
- **unregisterTripInstantaneousDrivingDistance** — Unregisters Trip Instantaneous Driving Distance listener. `[perm: CAR_DRIVE]`
  - `fun unregisterTripInstantaneousDrivingDistance(listener: DistanceDrivenListener)`
- **unregisterTripSinceRefillDrivingDistance** — Unregisters Trip Since Refuel Driving Distance listener. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterTripSinceRefillDrivingDistance(listener: DistanceDrivenListener)`
- **unregisterTripSinceRefillDrivingTime** — Unregisters Trip Since Refuel Driving Time listener. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterTripSinceRefillDrivingTime(listener: DrivingTimeListener)`

#### `ai.pleos.playground.vehicle.api.Safety`
```kotlin
interface Safety
```
Provides APIs for airbags, warnings, fluid levels, and telltales. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getSafety.

**Functions**
- **checkAirbagCapabilities** — Gets airbag capabilities. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun checkAirbagCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkAirbagCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkAirbagDeployedCapabilities** — Gets airbag deployed capabilities. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun checkAirbagDeployedCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkBrakeFluidLevelStateCapabilities** — Gets brake fluid level state capabilities. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun checkBrakeFluidLevelStateCapabilities(onComplete: (CapabilityConfig, List<OilLevelState>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkEngineOilLevelStateCapabilities** — Gets engine oil level state capabilities. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun checkEngineOilLevelStateCapabilities(onComplete: (CapabilityConfig, List<OilLevelState>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkLowWasherFluidWarningStateCapabilities** — Gets low washer fluid warning state capabilities. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun checkLowWasherFluidWarningStateCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkPowerDownWarningIndicatorCapabilities** — Gets power down warning indicator capabilities. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun checkPowerDownWarningIndicatorCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatBuckledCapabilities** — Gets seat buckled capabilities. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun checkSeatBuckledCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkSeatBuckledCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatNotBuckledWarningCapabilities** — Gets seat not buckled warning capabilities. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun checkSeatNotBuckledWarningCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTelltalesCapabilities** — Gets telltales capabilities. `[perm: PRIVATE_CAR_SAFETY]` (protection: signature|privileged)
  - `fun checkTelltalesCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkTransmissionOilLevelStateCapabilities** — Gets transmission oil level state capabilities. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun checkTransmissionOilLevelStateCapabilities(onComplete: (CapabilityConfig, List<OilLevelState>) -> Unit, onFailed: (Exception) -> Unit)`
- **getBrakeFluidLevelState** — Gets the current brake fluid level. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun getBrakeFluidLevelState(onComplete: (OilLevelState?) -> Unit, onFailed: (Exception) -> Unit)`
- **getEngineOilLevelState** — Gets the current engine oil level. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun getEngineOilLevelState(onComplete: (OilLevelState?) -> Unit, onFailed: (Exception) -> Unit)`
- **getTelltalesInfo** — Gets telltales info. `[perm: CAR_SAFETY]` (protection: signature|privileged)
  - `fun getTelltalesInfo(onComplete: (telltales: Telltales) -> Unit, onFailed: (Exception) -> Unit)`
- **getTransmissionOilLevelState** — Gets the current transmission oil level. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun getTransmissionOilLevelState(onComplete: (OilLevelState?) -> Unit, onFailed: (Exception) -> Unit)`
- **hasLowWasherFluidWarning** — Checks the current washer fluid low warning status. If the status is true, there is an issue with the washer fluid `[perm: CAR_SAFETY]` (protection: normal)
  - `fun hasLowWasherFluidWarning(onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **hasPowerDownWarningIndicator** — Gets the current power down warning indicator state. If the state is true, there is a problem with the engine. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun hasPowerDownWarningIndicator(onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **hasSeatAirbagBeenDeployed** — Gets the current airbag deployment status. If the state is true, the airbag is deployed. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun hasSeatAirbagBeenDeployed(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **hasSeatAirbagEnabled** — Gets the seat airbag enabled state. If the state is true, there is a problem with the seat airbag in the area. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun hasSeatAirbagEnabled(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **hasSeatBeltNotBuckledWarning** — Gets the seat belt buckled warning state. If the state is true, the seat belt in the area is not buckled. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun hasSeatBeltNotBuckledWarning(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerBrakeFluidLevelState** — Registers the BrakeFluidLevelState to get property updates. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun registerBrakeFluidLevelState(listener: OilLevelStateListener)`
- **registerEngineOilLevelState** — Registers the EngineOilLevelState to get property updates. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun registerEngineOilLevelState(listener: OilLevelStateListener)`
- **registerLowWasherFluidWaringState** — Registers the LowWasherFluidWaringState to get property updates. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun registerLowWasherFluidWaringState(listener: LowWasherFluidWaringListener)`
- **registerPowerDownWarningIndicator** — Registers the PowerDownWarningIndicator to get property updates. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun registerPowerDownWarningIndicator(listener: PowerDownWarningIndicatorListener)`
- **registerSeatAirbagDeployed** — Registers the SeatAirbagDeployed to get property updates. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun registerSeatAirbagDeployed(area: SeatArea, listener: SeatAirbagDeployedListener)`
- **registerSeatAirbagEnabled** — Registers the SeatAirbagEnabled to get property updates. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun registerSeatAirbagEnabled(area: SeatArea, listener: SeatAirbagEnabledListener)`
- **registerSeatBeltNotBuckledWarning** — Registers the SeatBeltNotBuckledWarning to get property updates. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun registerSeatBeltNotBuckledWarning(area: SeatArea, listener: SeatBeltNotBuckledWarningListener)`
- **registerTelltales** — Registers a Telltales listener to receive telltale information updates. `[perm: PRIVATE_CAR_SAFETY]` (protection: signature|privileged)
  - `fun registerTelltales(listener: TelltaleInfoListener)`
- **registerTransmissionOilLevelState** — Registers the TransmissionOilLevel to get property updates. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun registerTransmissionOilLevelState(listener: OilLevelStateListener)`
- **unregisterBrakeFluidLevelState** — Stops getting property updates for the given BrakeFluidLevelState. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun unregisterBrakeFluidLevelState(listener: OilLevelStateListener)`
- **unregisterEngineOilLevelState** — Stops getting property updates for the given EngineOilLevelState. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun unregisterEngineOilLevelState(listener: OilLevelStateListener)`
- **unregisterLowWasherFluidWaringState** — Stops getting property updates for the given LowWasherFluidWaringState. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun unregisterLowWasherFluidWaringState(listener: LowWasherFluidWaringListener)`
- **unregisterPowerDownWarningIndicator** — Stops getting property updates for the given PowerDownWarningIndicator. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun unregisterPowerDownWarningIndicator(listener: PowerDownWarningIndicatorListener)`
- **unregisterSeatAirbagDeployed** — Stops getting property updates for the given SeatAirbagDeployed. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun unregisterSeatAirbagDeployed(area: SeatArea, listener: SeatAirbagDeployedListener)`
- **unregisterSeatAirbagEnabled** — Stops getting property updates for the given SeatAirbagEnabled. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun unregisterSeatAirbagEnabled(area: SeatArea, listener: SeatAirbagEnabledListener)`
- **unregisterSeatBeltNotBuckledWarning** — Stops getting property updates for the given SeatBeltNotBuckledWarning. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun unregisterSeatBeltNotBuckledWarning(area: SeatArea, listener: SeatBeltNotBuckledWarningListener)`
- **unregisterTelltales** — Unregisters a previously registered Telltales listener. `[perm: PRIVATE_CAR_SAFETY]` (protection: signature|privileged)
  - `fun unregisterTelltales(listener: TelltaleInfoListener)`
- **unregisterTransmissionOilLevelState** — Stops getting property updates for the given TransmissionOilLevel. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun unregisterTransmissionOilLevelState(listener: OilLevelStateListener)`

#### `ai.pleos.playground.vehicle.api.Seat`
```kotlin
interface Seat
```
Provides APIs for seat position, occupancy, seat belt, and seat climate. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getSeat.

**Functions**
- **checkMoveSeatBackrestAngleCapabilities** — Gets move seat BackrestAngle capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkMoveSeatBackrestAngleCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkMoveSeatBeltHeightCapabilities** — Gets move seat belt height capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkMoveSeatBeltHeightCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkMoveSeatDepthCapabilities** — Gets move seat depth capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkMoveSeatDepthCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkMoveSeatForeAftCapabilities** — Gets move seat ForeAft capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkMoveSeatForeAftCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkMoveSeatHeadrestAngleCapabilities** — Gets move seat headrest angle capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkMoveSeatHeadrestAngleCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkMoveSeatHeadrestForeAftCapabilities** — Gets move seat HeadrestForeAft capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkMoveSeatHeadrestForeAftCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkMoveSeatHeadrestHeightCapabilities** — Gets move seat headrest height capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkMoveSeatHeadrestHeightCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkMoveSeatHeightCapabilities** — Gets move seat height capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkMoveSeatHeightCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkMoveSeatLumbarForeAftCapabilities** — Gets move seat lumbar ForeAft capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkMoveSeatLumbarForeAftCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkMoveSeatLumbarSideSupportCapabilities** — Gets move seat lumbar side support capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkMoveSeatLumbarSideSupportCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkMoveSeatTiltCapabilities** — Gets move seat tilt capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkMoveSeatTiltCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatBackrestAngleCapabilities** `[perm: CAR_SEATS]`
  - `fun checkSeatBackrestAngleCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatBackrestAnglePositionCapabilities** — Gets seat BackrestAnglePosition capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatBackrestAnglePositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatBeltBuckledCapabilities** — Gets seat belt buckled capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatBeltBuckledCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkSeatBeltBuckledCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatBeltHeightCapabilities** `[perm: CAR_SEATS]`
  - `fun checkSeatBeltHeightCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatBeltHeightPositionCapabilities** — Gets seat belt height position capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatBeltHeightPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatDepthCapabilities** `[perm: CAR_SEATS]`
  - `fun checkSeatDepthCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatDepthPositionCapabilities** — Gets seat depth position capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatDepthPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatForeAftCapabilities** `[perm: CAR_SEATS]`
  - `fun checkSeatForeAftCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatForeAftPositionCapabilities** — Gets seat ForeAft position capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatForeAftPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatHeadrestAngleCapabilities** `[perm: CAR_SEATS]`
  - `fun checkSeatHeadrestAngleCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatHeadrestAnglePositionCapabilities** — Gets seat HeadrestAnglePosition capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatHeadrestAnglePositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatHeadrestForeAftCapabilities** `[perm: CAR_SEATS]`
  - `fun checkSeatHeadrestForeAftCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatHeadrestForeAftPositionCapabilities** — Gets seat HeadrestForeAftPosition capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatHeadrestForeAftPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatHeadrestHeightCapabilities** `[perm: CAR_SEATS]`
  - `fun checkSeatHeadrestHeightCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatHeadrestHeightPositionCapabilities** — Gets seat HeadrestHeightPosition capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatHeadrestHeightPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatHeightCapabilities** `[perm: CAR_SEATS]`
  - `fun checkSeatHeightCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatHeightPositionCapabilities** — Gets seat height position capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatHeightPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatLumbarForeAftCapabilities** `[perm: CAR_SEATS]`
  - `fun checkSeatLumbarForeAftCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatLumbarForeAftPositionCapabilities** — Gets seat LumbarForeAftPosition capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatLumbarForeAftPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatLumbarSideSupportCapabilities** `[perm: CAR_SEATS]`
  - `fun checkSeatLumbarSideSupportCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatLumbarSideSupportPositionCapabilities** — Gets seat LumbarSideSupportPosition capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatLumbarSideSupportPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatMemoryCapabilities** — Gets seat memory capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatMemoryCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatOccupancyCapabilities** — Gets seat occupancy capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatOccupancyCapabilities(onComplete: (List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkSeatOccupancyCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatTemperatureCapabilities** — Gets seat temperature capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatTemperatureCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkSeatTemperatureCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatTiltCapabilities** `[perm: CAR_SEATS]`
  - `fun checkSeatTiltCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatTiltPositionCapabilities** — Gets seat tilt position capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatTiltPositionCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSeatVentilationCapabilities** — Gets seat ventilation capabilities. `[perm: CAR_SEATS]` (protection: normal)
  - `fun checkSeatVentilationCapabilities(onComplete: (Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkSeatVentilationCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatBackrestAnglePosition** — Gets the current seat backrest angle position. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatBackrestAnglePosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatBeltBuckledState** — Gets the current seat belt buckled state. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatBeltBuckledState(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatBeltHeightPosition** — Gets the current seat belt height position. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatBeltHeightPosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatDepthPosition** — Gets the current seat depth position. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatDepthPosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatForeAftPosition** — Gets the current seat fore/aft position. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatForeAftPosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatHeadrestAnglePosition** — Gets the current seat headrest angle position. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatHeadrestAnglePosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatHeadrestForeAftPosition** — Gets current seat headrest fore/aft position. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatHeadrestForeAftPosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatHeadrestHeightAftPosition** — Gets the current seat headrest height aft position. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatHeadrestHeightAftPosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatHeightPosition** — Gets the current seat height position. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatHeightPosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatLumbarForeAftPosition** — Gets the current seat lumbar fore/aft position. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatLumbarForeAftPosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatLumbarSideSupportPosition** — Gets the seat lumbar side support position. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatLumbarSideSupportPosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatOccupancy** — Gets current seat occupancy. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatOccupancy(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSeatTiltPosition** — Gets the current seat tilt position. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getCurrentSeatTiltPosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacSeatTemperature** — Gets the current seat heating/cooling temperature. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getHvacSeatTemperature(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacSeatVentilationState** — Gets the current seat ventilation state. `[perm: CAR_SEATS]` (protection: normal)
  - `fun getHvacSeatVentilationState(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **moveSeatBackrestAngle** — Move seat backrestAngle. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun moveSeatBackrestAngle(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **moveSeatBeltHeight** — Moves the seat belt height. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun moveSeatBeltHeight(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **moveSeatDepth** — Moves the seat depth. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun moveSeatDepth(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **moveSeatForeAft** — Moves the seat fore/aft. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun moveSeatForeAft(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **moveSeatHeadrestAngle** — Moves the seat headrest angle. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun moveSeatHeadrestAngle(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **moveSeatHeadrestForeAft** — Moves seat headrest fore/aft. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun moveSeatHeadrestForeAft(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **moveSeatHeadrestHeight** — Moves seat headrest height. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun moveSeatHeadrestHeight(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **moveSeatHeight** — Moves the seat height. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun moveSeatHeight(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **moveSeatLumbarForeAft** — Moves seat lumbar fore/aft. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun moveSeatLumbarForeAft(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **moveSeatLumbarSideSupport** — Moves the seat lumbar side support. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun moveSeatLumbarSideSupport(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **moveSeatTilt** — Moves seat tilt. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun moveSeatTilt(area: SeatArea, value: MoveControl, onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **registerHvacSeatTemperature** — Registers the HvacSeatTemperature to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerHvacSeatTemperature(area: SeatArea, listener: HvacSeatTemperatureListener)`
- **registerHvacSeatVentilation** — Registers the HvacSeatVentilation to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerHvacSeatVentilation(area: SeatArea, listener: HvacSeatVentilationListener)`
- **registerSeatBackrestAnglePosition** — Registers the SeatBackrestAnglePosition to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatBackrestAnglePosition(area: SeatArea, listener: SeatBackrestAnglePositionListener)`
- **registerSeatBeltBuckledState** — Registers the SeatBeltBuckledState to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatBeltBuckledState(area: SeatArea, listener: SeatBeltBuckledStateListener)`
- **registerSeatBeltHeightPosition** — Registers the SeatBeltHeightPosition to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatBeltHeightPosition(area: SeatArea, listener: SeatBeltHeightPositionListener)`
- **registerSeatDepthPosition** — Registers the SeatDepthPosition to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatDepthPosition(area: SeatArea, listener: SeatDepthPositionListener)`
- **registerSeatForeAftPosition** — Registers the SeatForeAftPosition to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatForeAftPosition(area: SeatArea, listener: SeatForeAftPositionListener)`
- **registerSeatHeadrestAnglePosition** — Registers the SeatHeadrestAnglePosition to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatHeadrestAnglePosition(area: SeatArea, listener: SeatHeadrestAnglePositionListener)`
- **registerSeatHeadrestForeAftPosition** — Registers the SeatHeadrestForeAftPosition to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatHeadrestForeAftPosition(area: SeatArea, listener: SeatHeadrestForeAftPositionListener)`
- **registerSeatHeadrestHeightAftPosition** — Registers the SeatHeadrestHeightAftPosition to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatHeadrestHeightAftPosition(area: SeatArea, listener: SeatHeadrestHeightAftPositionListener)`
- **registerSeatHeightPosition** — Registers the SeatHeightPosition to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatHeightPosition(area: SeatArea, listener: SeatHeightPositionListener)`
- **registerSeatLumbarForeAftPosition** — Registers the SeatLumbarForeAftPosition to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatLumbarForeAftPosition(area: SeatArea, listener: SeatLumbarForeAftPositionListener)`
- **registerSeatLumbarSideSupportPosition** — Registers the SeatLumbarSideSupportPosition to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatLumbarSideSupportPosition(area: SeatArea, listener: SeatLumbarSideSupportPositionListener)`
- **registerSeatOccupancy** — Registers the SeatOccupancy to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatOccupancy(area: SeatArea, listener: SeatOccupancyListener)`
- **registerSeatTiltPosition** — Registers the SeatTiltPosition to get property updates. `[perm: CAR_SEATS]` (protection: normal)
  - `fun registerSeatTiltPosition(area: SeatArea, listener: SeatTiltPositionListener)`
- **selectSeatMemory** — Selects seat memory. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun selectSeatMemory(area: SeatArea, value: Int, onFailed: (Exception) -> Unit = {})`
- **setHvacSeatTemperature** — Sets the seat heating/cooling temperature. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun setHvacSeatTemperature(area: SeatArea, value: Int, onComplete: (SeatArea, Int?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setHvacSeatVentilation** — Sets the seat ventilation. `[perm: CONTROL_CAR_SEATS]` (protection: normal)
  - `fun setHvacSeatVentilation(area: SeatArea, value: Int, onComplete: (SeatArea, Int?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **unregisterHvacSeatTemperature** — Stops getting property updates for the given HvacSeatTemperature. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterHvacSeatTemperature(area: SeatArea, listener: HvacSeatTemperatureListener)`
- **unregisterHvacSeatVentilation** — Stops getting property updates for the given HvacSeatVentilation. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterHvacSeatVentilation(area: SeatArea, listener: HvacSeatVentilationListener)`
- **unregisterSeatBackrestAnglePosition** — Stops getting property updates for the given SeatBackrestAnglePosition. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatBackrestAnglePosition(area: SeatArea, listener: SeatBackrestAnglePositionListener)`
- **unregisterSeatBeltBuckledState** — Stops getting property updates for the given SeatBeltBuckledState. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatBeltBuckledState(area: SeatArea, listener: SeatBeltBuckledStateListener)`
- **unregisterSeatBeltHeightPosition** — Stops getting property updates for the given SeatBeltHeightPosition. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatBeltHeightPosition(area: SeatArea, listener: SeatBeltHeightPositionListener)`
- **unregisterSeatDepthPosition** — Stops getting property updates for the given SeatDepthPosition. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatDepthPosition(area: SeatArea, listener: SeatDepthPositionListener)`
- **unregisterSeatForeAftPosition** — Stops getting property updates for the given SeatForeAftPosition. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatForeAftPosition(area: SeatArea, listener: SeatForeAftPositionListener)`
- **unregisterSeatHeadrestAnglePosition** — Stops getting property updates for the given SeatHeadrestAnglePosition. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatHeadrestAnglePosition(area: SeatArea, listener: SeatHeadrestAnglePositionListener)`
- **unregisterSeatHeadrestForeAftPosition** — Stops getting property updates for the given SeatHeadrestForeAftPosition. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatHeadrestForeAftPosition(area: SeatArea, listener: SeatHeadrestForeAftPositionListener)`
- **unregisterSeatHeadrestHeightAftPosition** — Stops getting property updates for the given SeatHeadrestHeightAftPosition. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatHeadrestHeightAftPosition(area: SeatArea, listener: SeatHeadrestHeightAftPositionListener)`
- **unregisterSeatHeightPosition** — Stops getting property updates for the given SeatHeightPosition. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatHeightPosition(area: SeatArea, listener: SeatHeightPositionListener)`
- **unregisterSeatLumbarForeAftPosition** — Stops getting property updates for the given SeatLumbarForeAftPosition. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatLumbarForeAftPosition(area: SeatArea, listener: SeatLumbarForeAftPositionListener)`
- **unregisterSeatLumbarSideSupportPosition** — Stops getting property updates for the given SeatLumbarSideSupportPosition. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatLumbarSideSupportPosition(area: SeatArea, listener: SeatLumbarSideSupportPositionListener)`
- **unregisterSeatOccupancy** — Stops getting property updates for the given SeatOccupancy. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatOccupancy(area: SeatArea, listener: SeatOccupancyListener)`
- **unregisterSeatTiltPosition** — Stops getting property updates for the given SeatTiltPosition. `[perm: CAR_SEATS]` (protection: normal)
  - `fun unregisterSeatTiltPosition(area: SeatArea, listener: SeatTiltPositionListener)`

#### `ai.pleos.playground.vehicle.api.SideMirror`
```kotlin
interface SideMirror
```
Provides APIs for side mirror heat. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getSideMirror.

**Functions**
- **checkHvacSideMirrorHeatCapabilities** — Gets hvac side mirror heat capabilities. `[perm: CAR_MIRRORS]` (protection: normal)
  - `fun checkHvacSideMirrorHeatCapabilities(onComplete: (Map<MirrorArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacSideMirrorHeatCapabilities(onComplete: (CapabilityConfig, Map<MirrorArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **getHvacSideMirrorHeatState** — Gets the current side mirror heat state. `[perm: CAR_MIRRORS]` (protection: normal)
  - `fun getHvacSideMirrorHeatState(area: MirrorArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerHvacSideMirrorHeatState** — Registers HvacSideMirrorHeatState state to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacSideMirrorHeatState(area: MirrorArea, listener: HvacSideMirrorHeatStateListener)`
- **setHvacSideMirrorHeat** — Sets the side mirror heat state. `[perm: CONTROL_CAR_MIRRORS]` (protection: normal)
  - `fun setHvacSideMirrorHeat(area: MirrorArea, value: Int, onComplete: (MirrorArea, Int?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **unregisterHvacSideMirrorHeatState** — Stops getting property updates for the given HvacSideMirrorHeatState state. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacSideMirrorHeatState(area: MirrorArea, listener: HvacSideMirrorHeatStateListener)`

#### `ai.pleos.playground.vehicle.api.SteeringWheel`
```kotlin
interface SteeringWheel
```
Provides APIs for the front-wheel steering angle and steering wheel heating. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getSteeringWheel.

**Functions**
- **checkHvacSteeringWheelHeatCapabilities** — Gets hvac steering wheel heat capabilities. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun checkHvacSteeringWheelHeatCapabilities(onComplete: (AreaBounds<Int>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkHvacSteeringWheelHeatCapabilities(onComplete: (CapabilityConfig, AreaBounds<Int>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSteeringWheelAngleCapabilities** — Gets the capabilities for the front-wheel steering angle. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun checkSteeringWheelAngleCapabilities(onComplete: (CapabilityConfig, AreaBounds<Float>) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSteeringWheelAngle** — Gets the current front-wheel steering angle in degrees. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun getCurrentSteeringWheelAngle(onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets the current front-wheel steering angle in degrees. Leftward steering angles are negative. This value does not represent the rotation angle of the steering wheel.
- **getHvacSteeringWheelHeatState** — Gets the current SteeringWheel heating/cooling state. Permission: PleosPermission.CAR_CLIMATE (protection level: normal) `[perm: CAR_CLIMATE]`
  - `fun getHvacSteeringWheelHeatState(onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerHvacSteeringWheelHeatState** — Registers Hvac SteeringWheel heat state to get property updates. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun registerHvacSteeringWheelHeatState(listener: HvacSteeringWheelHeatStateListener)`
- **registerSteeringWheelAngle** — Registers a listener to receive front-wheel steering angle updates. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun registerSteeringWheelAngle(listener: SteeringWheelAngleListener)`
  - 상세: Registers a listener to receive front-wheel steering angle updates. Angles are reported in degrees, and leftward steering angles are negative. The reported value does not represent the rotation angle of the steering wheel.
- **setHvacSteeringWheelHeat** — Sets SteeringWheel heating/cooling temperature. Permission: PleosPermission.CONTROL_CAR_CLIMATE (protection level: normal) `[perm: CONTROL_CAR_CLIMATE]`
  - `fun setHvacSteeringWheelHeat(value: Int, onComplete: (Int?) -> Unit = {}, onFailed: (Exception) -> Unit = {})`
- **unregisterHvacSteeringWheelHeatState** — Stops getting property updates for the given Hvac SteeringWheel heat state. `[perm: CAR_CLIMATE]` (protection: normal)
  - `fun unregisterHvacSteeringWheelHeatState(listener: HvacSteeringWheelHeatStateListener)`
- **unregisterSteeringWheelAngle** — Unregisters the given listener from front-wheel steering angle updates. `[perm: CAR_DRIVE]` (protection: normal)
  - `fun unregisterSteeringWheelAngle(listener: SteeringWheelAngleListener)`

#### `ai.pleos.playground.vehicle.api.Tire`
```kotlin
interface Tire
```
Provides APIs for tire pressure. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getTire.

**Functions**
- **checkLowTirePressureCapabilities** — Gets low tire pressure capabilities. `[perm: CAR_SAFETY]` (protection: normal)
  - `fun checkLowTirePressureCapabilities(onComplete: (CapabilityConfig, List<WheelArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkWheelAreaCapabilities** `[perm: CAR_SAFETY]`
  - `fun checkWheelAreaCapabilities(onComplete: (List<WheelArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **getLowTirePressure** — Gets the critically low tire pressure threshold in kilopascals (kPa). `[perm: CAR_SAFETY]` (protection: normal)
  - `fun getLowTirePressure(area: WheelArea, onComplete: (Float?) -> Unit, onFailed: (Exception) -> Unit)`

#### `ai.pleos.playground.vehicle.api.Toll`
```kotlin
interface Toll
```
Provides APIs for ETCS voice guidance volume. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getToll.

**Functions**
- **checkEtcsVoiceGuidanceVolumeCapabilities** — Gets ETCS voice guidance volume capabilities. `[perm: CAR_TOLL]` (protection: normal)
  - `fun checkEtcsVoiceGuidanceVolumeCapabilities(onComplete: (CapabilityConfig, AreaBounds<Int>) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentEtcsVoiceGuidanceVolume** — Gets current ETCS voice guidance volume. `[perm: CAR_TOLL]` (protection: normal)
  - `fun getCurrentEtcsVoiceGuidanceVolume(onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerEtcsVoiceGuidanceVolume** — Registers ETCS voice guidance volume updates. `[perm: CAR_TOLL]` (protection: normal)
  - `fun registerEtcsVoiceGuidanceVolume(listener: EtcsVoiceGuidanceVolumeListener)`
- **setEtcsVoiceGuidanceVolume** — Sets ETCS voice guidance volume. `[perm: CONTROL_CAR_TOLL]` (protection: normal)
  - `fun setEtcsVoiceGuidanceVolume(value: Int, onComplete: () -> Unit, onFailed: (Exception) -> Unit)`
- **unregisterEtcsVoiceGuidanceVolume** — Stops ETCS voice guidance volume updates. `[perm: CAR_TOLL]` (protection: normal)
  - `fun unregisterEtcsVoiceGuidanceVolume(listener: EtcsVoiceGuidanceVolumeListener)`

#### `ai.pleos.playground.vehicle.api.TurnSignal`
```kotlin
interface TurnSignal
```
Provides APIs for turn signal state. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getTurnSignal.

**Functions**
- **checkTurnSignalStateCapabilities** — Gets turn signal state capabilities. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun checkTurnSignalStateCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentTurnSignalState** — Gets the current turn signal state. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun getCurrentTurnSignalState(onComplete: (TurnSignalValue?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerTurnSignalState** — Registers the turn signal state to get property updates. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun registerTurnSignalState(listener: TurnSignalStateListener)`
- **unregisterTurnSignalState** — Stops getting property updates for the given turn signal state. `[perm: CAR_LIGHTS]` (protection: normal)
  - `fun unregisterTurnSignalState(listener: TurnSignalStateListener)`

#### `ai.pleos.playground.vehicle.api.Window`
```kotlin
interface Window
```
Provides APIs for window position, lock, blinds, and smart glass. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getWindow.

**Functions**
- **checkMoveWindowBlindCapabilities** — Gets move window blind capabilities. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun checkMoveWindowBlindCapabilities(onComplete: (CapabilityConfig, List<WindowArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSmartGlassLevelCapabilities** — Gets smart glass level capabilities. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun checkSmartGlassLevelCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSmartGlassStateCapabilities** — Gets smart glass state capabilities. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun checkSmartGlassStateCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)`
- **checkSmartGlassZoneStateCapabilities** — Gets smart glass zone state capabilities. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun checkSmartGlassZoneStateCapabilities(onComplete: (CapabilityConfig, List<WindowArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets smart glass zone state capabilities. Smart glass zone state support does not imply smart glass level support; check checkSmartGlassLevelCapabilities separately.
- **checkWindowBlindPositionCapabilities** — Gets window blind position capabilities. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun checkWindowBlindPositionCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkWindowLockCapabilities** — Gets window lock capabilities. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun checkWindowLockCapabilities(onComplete: (List<WindowArea>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkWindowLockCapabilities(onComplete: (CapabilityConfig, List<WindowArea>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkWindowPositionCapabilities** — Gets window position capabilities. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun checkWindowPositionCapabilities(onComplete: (Map<WindowArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkWindowPositionCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSmartGlassLevel** — Gets the current Smart Glass Level for the specified window area. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun getCurrentSmartGlassLevel(area: WindowArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentSmartGlassState** — Gets the current Smart Glass state. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun getCurrentSmartGlassState(onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentWindowBlindPosition** — Gets the current Window Blind Position for the specified window area. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun getCurrentWindowBlindPosition(area: WindowArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentWindowLockState** — Gets the current window lock state. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun getCurrentWindowLockState(area: WindowArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentWindowPosition** — Gets the current window position. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun getCurrentWindowPosition(area: WindowArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)`
- **getSmartGlassConfig** — Gets the Smart Glass configuration for the specified window area. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun getSmartGlassConfig(area: WindowArea, onComplete: (SmartGlassConfig?) -> Unit, onFailed: (Exception) -> Unit)`
  - 상세: Gets the Smart Glass configuration for the specified window area. The configuration is decoded from the vehicle's [zoneCount, levelCount] value. For example, [4, 1] is returned as SmartGlassConfig(zoneCount = 4, levelCount = 1). Use SmartGlassConfig.zoneCount to size the command list for setSmartGlassZoneState. Smart glass level support may be absent even when this configuration is available; check checkSmartGlassLevelCapabilities separately.
- **moveWindowBlind** — Moves the Window Blind for the specified window area. `[perm: CONTROL_CAR_WINDOWS]` (protection: normal)
  - `fun moveWindowBlind(area: WindowArea, value: MoveControl, onComplete: (WindowArea, MoveControl?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerSmartGlassLevel** — Registers a listener to receive Smart Glass Level updates for the specified window area. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun registerSmartGlassLevel(area: WindowArea, listener: SmartGlassLevelListener)`
- **registerSmartGlassState** — Registers Smart Glass state listener. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun registerSmartGlassState(listener: SmartGlassStateListener)`
- **registerWindowBlindPosition** — Registers a listener to receive Window Blind Position updates for the specified window area. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun registerWindowBlindPosition(area: WindowArea, listener: WindowPositionListener)`
- **registerWindowLockState** — Registers the window lock state to get property updates. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun registerWindowLockState(area: WindowArea, listener: WindowLockStateListener)`
- **registerWindowPosition** — Registers the window area to get property updates. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun registerWindowPosition(area: WindowArea, listener: WindowPositionListener)`
- **setSmartGlassLevel** — Sets the Smart Glass Level for the specified window area. `[perm: CONTROL_CAR_WINDOWS]` (protection: normal)
  - `fun setSmartGlassLevel(area: WindowArea, value: Int, onComplete: (WindowArea, Int?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setSmartGlassState** — Sets the Smart Glass state. `[perm: CONTROL_CAR_WINDOWS]` (protection: signature|privileged)
  - `fun setSmartGlassState(value: Boolean, onComplete: (Boolean?) -> Unit = { _ -> }, onFailed: (Exception) -> Unit = {})`
- **setSmartGlassZoneState** — Sets the Smart Glass zone state by submitting one command per smart glass panel for the specified window area. `[perm: CONTROL_CAR_WINDOWS]` (protection: normal)
  - `fun setSmartGlassZoneState(area: WindowArea, value: List<SmartGlassZoneCommand>, onComplete: (WindowArea, List<SmartGlassZoneCommand>?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
  - 상세: Sets the Smart Glass zone state by submitting one command per smart glass panel for the specified window area. Commands are submitted in panel order. Every position, including SmartGlassZoneCommand.SKIP, is preserved, and a list that contains only SmartGlassZoneCommand.SKIP is still submitted to the service. The SDK does not query getSmartGlassConfig or check capability support before submitting the request.
- **setWindowLock** — Sets the current window lock state. `[perm: CONTROL_CAR_WINDOWS]` (protection: normal)
  - `fun setWindowLock(area: WindowArea, value: Boolean, onComplete: (WindowArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **setWindowPosition** — Sets the current window position state. `[perm: CONTROL_CAR_WINDOWS]` (protection: normal)
  - `fun setWindowPosition(area: WindowArea, value: Int, onComplete: (WindowArea, Int?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **unregisterSmartGlassLevel** — Unregisters a previously registered Smart Glass Level listener for the specified window area. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun unregisterSmartGlassLevel(area: WindowArea, listener: SmartGlassLevelListener)`
- **unregisterSmartGlassState** — Unregisters Smart Glass state listener. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun unregisterSmartGlassState(listener: SmartGlassStateListener)`
- **unregisterWindowBlindPosition** — Unregisters a previously registered Window Blind Position listener for the specified window area. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun unregisterWindowBlindPosition(area: WindowArea, listener: WindowPositionListener)`
- **unregisterWindowLockState** — Stops getting property updates for the given window lock state. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun unregisterWindowLockState(area: WindowArea, listener: WindowLockStateListener)`
- **unregisterWindowPosition** — Stops getting property updates for the given window position. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun unregisterWindowPosition(area: WindowArea, listener: WindowPositionListener)`

#### `ai.pleos.playground.vehicle.api.Wiper`
```kotlin
interface Wiper
```
Provides APIs for wiper state and switch. Obtain an instance from ai.pleos.playground.vehicle.Vehicle.getWiper.

**Functions**
- **checkWiperStateCapabilities** — Gets wiper state capabilities. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun checkWiperStateCapabilities(onComplete: (Map<WindowArea, List<WiperState>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkWiperStateCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, List<WiperState>>) -> Unit, onFailed: (Exception) -> Unit)`
- **checkWiperSwitchCapabilities** — Gets wiper switch capabilities. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun checkWiperSwitchCapabilities(onComplete: (Map<WindowArea, List<WiperSwitch>>) -> Unit, onFailed: (Exception) -> Unit)`
  - `fun checkWiperSwitchCapabilities(onComplete: (CapabilityConfig, Map<WindowArea, List<WiperSwitch>>) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentWiperState** — Gets the current wiper mode state. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun getCurrentWiperState(area: WindowArea, onComplete: (WiperState?) -> Unit, onFailed: (Exception) -> Unit)`
- **getCurrentWiperSwitch** — Gets the current Wiper Switch state for the specified window area. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun getCurrentWiperSwitch(area: WindowArea, onComplete: (WiperSwitch?) -> Unit, onFailed: (Exception) -> Unit)`
- **registerWiperState** — Registers the wiper state to get property updates. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun registerWiperState(area: WindowArea, listener: WiperStateListener)`
- **setWiperSwitch** — Sets the current wiper switch. `[perm: CONTROL_CAR_WINDOWS]` (protection: normal)
  - `fun setWiperSwitch(area: WindowArea, value: WiperSwitch, onComplete: (WindowArea, WiperSwitch?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})`
- **unregisterWiperState** — Stops getting property updates for the given wiper state. `[perm: CAR_WINDOWS]` (protection: normal)
  - `fun unregisterWiperState(area: WindowArea, listener: WiperStateListener)`

### package `ai.pleos.playground.vehicle.api.model.area`

- `Area` — Base type for vehicle areas.
- `DoorArea` — Represents one or more door areas.
- `MirrorArea` — Represents one or more mirror areas.
- `SeatArea` — Represents one or more seat areas.
- `TurnSignalArea` — Represents one or more turn signal areas.
- `WheelArea` — Represents one or more wheel areas.
- `WindowArea` — Represents one or more window areas.

#### `ai.pleos.playground.vehicle.api.model.area.Area`
```kotlin
sealed class Area
```
Base type for vehicle areas.

**Inheritors**
- **DoorArea**
- **MirrorArea**
- **SeatArea**
- **TurnSignalArea**
- **WheelArea**
- **WindowArea**

#### `ai.pleos.playground.vehicle.api.model.area.DoorArea`
```kotlin
class DoorArea(val values: DoorValue) : Area
```
Represents one or more door areas.

**Constructors**
- **DoorArea**
  - `constructor(vararg values: DoorValue)`

**Properties**
- **values** — The selected door area values.
  - `val values: Array<out DoorValue>`

**Functions**
- **equals**
  - `operator override fun equals(other: Any?): Boolean`
- **hashCode**
  - `override fun hashCode(): Int`

#### `ai.pleos.playground.vehicle.api.model.area.MirrorArea`
```kotlin
class MirrorArea(val values: MirrorValue) : Area
```
Represents one or more mirror areas.

**Constructors**
- **MirrorArea**
  - `constructor(vararg values: MirrorValue)`

**Properties**
- **values** — The selected mirror area values.
  - `val values: Array<out MirrorValue>`

**Functions**
- **equals**
  - `operator override fun equals(other: Any?): Boolean`
- **hashCode**
  - `override fun hashCode(): Int`

#### `ai.pleos.playground.vehicle.api.model.area.SeatArea`
```kotlin
class SeatArea(val values: SeatValue) : Area
```
Represents one or more seat areas.

**Constructors**
- **SeatArea**
  - `constructor(vararg values: SeatValue)`

**Properties**
- **values** — The selected seat area values.
  - `val values: Array<out SeatValue>`

**Functions**
- **equals**
  - `operator override fun equals(other: Any?): Boolean`
- **hashCode**
  - `override fun hashCode(): Int`

#### `ai.pleos.playground.vehicle.api.model.area.TurnSignalArea`
```kotlin
class TurnSignalArea(val values: TurnSignalValue) : Area
```
Represents one or more turn signal areas.

**Constructors**
- **TurnSignalArea**
  - `constructor(vararg values: TurnSignalValue)`

**Properties**
- **values** — The selected turn signal area values.
  - `val values: Array<out TurnSignalValue>`

**Functions**
- **equals**
  - `operator override fun equals(other: Any?): Boolean`
- **hashCode**
  - `override fun hashCode(): Int`

#### `ai.pleos.playground.vehicle.api.model.area.WheelArea`
```kotlin
class WheelArea(val values: WheelValue) : Area
```
Represents one or more wheel areas.

**Constructors**
- **WheelArea**
  - `constructor(vararg values: WheelValue)`

**Properties**
- **values** — The selected wheel area values.
  - `val values: Array<out WheelValue>`

**Functions**
- **equals**
  - `operator override fun equals(other: Any?): Boolean`
- **hashCode**
  - `override fun hashCode(): Int`

#### `ai.pleos.playground.vehicle.api.model.area.WindowArea`
```kotlin
class WindowArea(val values: WindowValue) : Area
```
Represents one or more window areas.

**Constructors**
- **WindowArea**
  - `constructor(vararg values: WindowValue)`

**Properties**
- **values** — The selected window area values.
  - `val values: Array<out WindowValue>`

**Functions**
- **equals**
  - `operator override fun equals(other: Any?): Boolean`
- **hashCode**
  - `override fun hashCode(): Int`

### package `ai.pleos.playground.vehicle.api.model.capability`

- `AccessType` — Represents the access level for a capability.
- `AreaBounds` — Represents the available value range for an area.
- `CapabilityConfig` — Represents the configuration for a specific capability.
- `HvacTemperatureConfigArray` — HVAC temperature conversion table derived from the vehicle temperature setting config array.
- `SmartGlassConfig` — Describes the smart glass zones and levels for a window area.

#### `ai.pleos.playground.vehicle.api.model.capability.AccessType`
```kotlin
enum AccessType : Enum<AccessType>
```
Represents the access level for a capability.

**Entries**
- **NONE**
- **READ**
- **WRITE**
- **READ_WRITE**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<AccessType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): AccessType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<AccessType>`

#### `ai.pleos.playground.vehicle.api.model.capability.AreaBounds`
```kotlin
data class AreaBounds<T>(val minValue: T, val maxValue: T)
```
Represents the available value range for an area.

**Constructors**
- **AreaBounds**
  - `constructor(minValue: T, maxValue: T)`

**Properties**
- **maxValue** — The maximum value.
  - `val maxValue: T`
- **minValue** — The minimum value.
  - `val minValue: T`

#### `ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig`
```kotlin
data class CapabilityConfig(val accessType: AccessType = AccessType.NONE)
```
Represents the configuration for a specific capability.

**Constructors**
- **CapabilityConfig**
  - `constructor(accessType: AccessType = AccessType.NONE)`

**Properties**
- **accessType** — The level of access granted. Defaults to AccessType.NONE.
  - `val accessType: AccessType`

#### `ai.pleos.playground.vehicle.api.model.capability.HvacTemperatureConfigArray`
```kotlin
data class HvacTemperatureConfigArray(val minCelsius: Float, val maxCelsius: Float, val incrementCelsius: Float, val minFahrenheit: Float, val maxFahrenheit: Float, val incrementFahrenheit: Float, val raw: List<Int>)
```
HVAC temperature conversion table derived from the vehicle temperature setting config array. The VHAL configuration array stores temperatures and increments as integers multiplied by 10. The temperature and increment properties below are Float values obtained by dividing those integers by 10.

**Constructors**
- **HvacTemperatureConfigArray**
  - `constructor(minCelsius: Float, maxCelsius: Float, incrementCelsius: Float, minFahrenheit: Float, maxFahrenheit: Float, incrementFahrenheit: Float, raw: List<Int>)`

**Properties**
- **incrementCelsius** — Supported temperature increment in degrees Celsius (°C).
  - `val incrementCelsius: Float`
- **incrementFahrenheit** — Supported temperature increment in degrees Fahrenheit (°F).
  - `val incrementFahrenheit: Float`
- **maxCelsius** — Maximum supported temperature in degrees Celsius (°C).
  - `val maxCelsius: Float`
- **maxFahrenheit** — Maximum supported temperature in degrees Fahrenheit (°F).
  - `val maxFahrenheit: Float`
- **minCelsius** — Minimum supported temperature in degrees Celsius (°C).
  - `val minCelsius: Float`
- **minFahrenheit** — Minimum supported temperature in degrees Fahrenheit (°F).
  - `val minFahrenheit: Float`
- **raw** — Original six integer values from the VHAL configuration array, without conversion. Each value represents a temperature or increment multiplied by 10.
  - `val raw: List<Int>`

#### `ai.pleos.playground.vehicle.api.model.capability.SmartGlassConfig`
```kotlin
data class SmartGlassConfig(val zoneCount: Int, val levelCount: Int)
```
Describes the smart glass zones and levels for a window area.

**Constructors**
- **SmartGlassConfig**
  - `constructor(zoneCount: Int, levelCount: Int)`

**Properties**
- **levelCount** — Number of smart glass levels reported by the vehicle. This value does not indicate smart glass level support; check ai.pleos.playground.vehicle.api.Window.checkSmartGlassLevelCapabilities separately.
  - `val levelCount: Int`
- **zoneCount** — Number of smart glass zones (panels) in the window area. Pass this many commands to ai.pleos.playground.vehicle.api.Window.setSmartGlassZoneState.
  - `val zoneCount: Int`

### package `ai.pleos.playground.vehicle.api.model.common`

- `TripStatistics` — Represents trip statistics grouped by type.

#### `ai.pleos.playground.vehicle.api.model.common.TripStatistics`
```kotlin
data class TripStatistics<T>(val current: T? = null, val instantaneous: T? = null, val cumulative: T? = null, val sinceRefill: T? = null, val raw: List<T?> = emptyList())
```
Represents trip statistics grouped by type.

**Constructors**
- **TripStatistics**
  - `constructor(current: T? = null, instantaneous: T? = null, cumulative: T? = null, sinceRefill: T? = null, raw: List<T?> = emptyList())`

**Properties**
- **cumulative** — The cumulative value.
  - `val cumulative: T? = null`
- **current** — The current value.
  - `val current: T? = null`
- **instantaneous** — The instantaneous value.
  - `val instantaneous: T? = null`
- **raw** — The full list of available values.
  - `val raw: List<T?>`
- **sinceRefill** — The value since refill.
  - `val sinceRefill: T? = null`

**Functions**
- **get**
  - `fun get(type: TripStatisticType): T?`

### package `ai.pleos.playground.vehicle.api.model.telltale`

- `TelltaleData` — Represents a single telltale item.
- `Telltales` — Represents a collection of telltales.

#### `ai.pleos.playground.vehicle.api.model.telltale.TelltaleData`
```kotlin
data class TelltaleData(val type: TelltaleType, val state: TelltaleState, val color: Int, val priority: Int, val warningCount: Int)
```
Represents a single telltale item.

**Constructors**
- **TelltaleData**
  - `constructor(type: TelltaleType, state: TelltaleState, color: Int, priority: Int, warningCount: Int)`

**Properties**
- **color** — The telltale color.
  - `val color: Int`
- **priority** — The telltale priority.
  - `val priority: Int`
- **state** — The telltale state.
  - `val state: TelltaleState`
- **type** — The telltale type.
  - `val type: TelltaleType`
- **warningCount** — The warning count.
  - `val warningCount: Int`

#### `ai.pleos.playground.vehicle.api.model.telltale.Telltales`
```kotlin
data class Telltales(val telltales: List<TelltaleData> = arrayListOf())
```
Represents a collection of telltales.

**Constructors**
- **Telltales**
  - `constructor(telltales: List<TelltaleData> = arrayListOf())`

**Properties**
- **telltales** — The telltale items.
  - `val telltales: List<TelltaleData>`

### package `ai.pleos.playground.vehicle.callback.exception`

- `VehicleException` — Exception type used by the vehicle SDK.

#### `ai.pleos.playground.vehicle.callback.exception.VehicleException`
```kotlin
class VehicleException(error: String? = null) : Exception
```
Exception type used by the vehicle SDK.

**Parameters**
- **error** — The error value used to create the exception.

**Constructors**
- **VehicleException**
  - `constructor(error: String? = null)`

**Properties**
- **errorCode**
  - `val errorCode: ErrorCode`
- **message**
  - `override val message: String`

### package `ai.pleos.playground.vehicle.constant`

- `ErrorCode`
- `PleosPermission`

#### `ai.pleos.playground.vehicle.constant.ErrorCode`
```kotlin
enum ErrorCode : Enum<ErrorCode>
```

**Entries**
- **ERROR_INTERNAL_SERVICE**
- **ERROR_SERVICE_CONNECTION_FAILURE**
- **ERROR_DRIVING_RESTRICTED**
- **ERROR_NOT_SUPPORTED_PROPERTY**
- **ERROR_NOT_SUPPORTED_VALUE_TYPE**
- **ERROR_NOT_SUPPORTED_AREA**
- **ERROR_EMPTY_RESULT**
- **ERROR_SECURITY_PERMISSION**
- **ERROR_UNDEFINED**

**Types**
- **Companion**
  - `object Companion`

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<ErrorCode>`
- **id**
  - `val id: Int`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): ErrorCode`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<ErrorCode>`

#### `ai.pleos.playground.vehicle.constant.PleosPermission`
```kotlin
object PleosPermission
```

**Properties**
- **CAR_CLIMATE**
  - `const val CAR_CLIMATE: String`
- **CAR_DOORS**
  - `const val CAR_DOORS: String`
- **CAR_DRIVE**
  - `const val CAR_DRIVE: String`
- **CAR_ENERGY**
  - `const val CAR_ENERGY: String`
- **CAR_INFO**
  - `const val CAR_INFO: String`
- **CAR_LIGHTS**
  - `const val CAR_LIGHTS: String`
- **CAR_MIRRORS**
  - `const val CAR_MIRRORS: String`
- **CAR_SAFETY**
  - `const val CAR_SAFETY: String`
- **CAR_SEATS**
  - `const val CAR_SEATS: String`
- **CAR_TOLL**
  - `const val CAR_TOLL: String`
- **CAR_WINDOWS**
  - `const val CAR_WINDOWS: String`
- **CONTROL_CAR_CLIMATE**
  - `const val CONTROL_CAR_CLIMATE: String`
- **CONTROL_CAR_DOORS**
  - `const val CONTROL_CAR_DOORS: String`
- **CONTROL_CAR_DRIVE**
  - `const val CONTROL_CAR_DRIVE: String`
- **CONTROL_CAR_ENERGY**
  - `const val CONTROL_CAR_ENERGY: String`
- **CONTROL_CAR_INFO**
  - `const val CONTROL_CAR_INFO: String`
- **CONTROL_CAR_LIGHTS**
  - `const val CONTROL_CAR_LIGHTS: String`
- **CONTROL_CAR_MIRRORS**
  - `const val CONTROL_CAR_MIRRORS: String`
- **CONTROL_CAR_SAFETY**
  - `const val CONTROL_CAR_SAFETY: String`
- **CONTROL_CAR_SEATS**
  - `const val CONTROL_CAR_SEATS: String`
- **CONTROL_CAR_TOLL**
  - `const val CONTROL_CAR_TOLL: String`
- **CONTROL_CAR_WINDOWS**
  - `const val CONTROL_CAR_WINDOWS: String`
- **PRIVATE_CAR_CLIMATE**
  - `const val PRIVATE_CAR_CLIMATE: String`
- **PRIVATE_CAR_DOORS**
  - `const val PRIVATE_CAR_DOORS: String`
- **PRIVATE_CAR_DRIVE**
  - `const val PRIVATE_CAR_DRIVE: String`
- **PRIVATE_CAR_ENERGY**
  - `const val PRIVATE_CAR_ENERGY: String`
- **PRIVATE_CAR_INFO**
  - `const val PRIVATE_CAR_INFO: String`
- **PRIVATE_CAR_LIGHTS**
  - `const val PRIVATE_CAR_LIGHTS: String`
- **PRIVATE_CAR_MIRRORS**
  - `const val PRIVATE_CAR_MIRRORS: String`
- **PRIVATE_CAR_SAFETY**
  - `const val PRIVATE_CAR_SAFETY: String`
- **PRIVATE_CAR_SEATS**
  - `const val PRIVATE_CAR_SEATS: String`
- **PRIVATE_CAR_TOLL**
  - `const val PRIVATE_CAR_TOLL: String`
- **PRIVATE_CAR_WINDOWS**
  - `const val PRIVATE_CAR_WINDOWS: String`

### package `ai.pleos.playground.vehicle.constant.area`

- `AreaValue`
- `DoorValue` — Represents available door area values.
- `MirrorValue` — Represents available mirror area values.
- `SeatValue` — Represents available seat area values.
- `TurnSignalValue` — Represents available turn signal area values.
- `WheelValue` — Represents available wheel area values.
- `WindowValue` — Represents available window area values.

#### `ai.pleos.playground.vehicle.constant.area.AreaValue`
```kotlin
interface AreaValue
```

**Inheritors**
- **DoorValue**
- **MirrorValue**
- **SeatValue**
- **TurnSignalValue**
- **WheelValue**
- **WindowValue**

**Properties**
- **id**
  - `val id: Int`

#### `ai.pleos.playground.vehicle.constant.area.DoorValue`
```kotlin
enum DoorValue : Enum<DoorValue> , AreaValue
```
Represents available door area values.

**Entries**
- **ROW_1_LEFT**
- **ROW_1_RIGHT**
- **ROW_2_LEFT**
- **ROW_2_RIGHT**
- **ROW_3_LEFT**
- **ROW_3_RIGHT**
- **HOOD**
- **REAR**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<DoorValue>`
- **id**
  - `override val id: Int`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): DoorValue`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<DoorValue>`

#### `ai.pleos.playground.vehicle.constant.area.MirrorValue`
```kotlin
enum MirrorValue : Enum<MirrorValue> , AreaValue
```
Represents available mirror area values.

**Entries**
- **DRIVER_LEFT**
- **DRIVER_RIGHT**
- **DRIVER_CENTER**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<MirrorValue>`
- **id**
  - `override val id: Int`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): MirrorValue`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<MirrorValue>`

#### `ai.pleos.playground.vehicle.constant.area.SeatValue`
```kotlin
enum SeatValue : Enum<SeatValue> , AreaValue
```
Represents available seat area values.

**Entries**
- **ROW_1_LEFT**
- **ROW_1_CENTER**
- **ROW_1_RIGHT**
- **ROW_2_LEFT**
- **ROW_2_CENTER**
- **ROW_2_RIGHT**
- **ROW_3_LEFT**
- **ROW_3_CENTER**
- **ROW_3_RIGHT**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<SeatValue>`
- **id**
  - `override val id: Int`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): SeatValue`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<SeatValue>`

#### `ai.pleos.playground.vehicle.constant.area.TurnSignalValue`
```kotlin
enum TurnSignalValue : Enum<TurnSignalValue> , AreaValue
```
Represents available turn signal area values.

**Entries**
- **OFF**
- **RIGHT**
- **LEFT**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<TurnSignalValue>`
- **id**
  - `override val id: Int`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): TurnSignalValue`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<TurnSignalValue>`

#### `ai.pleos.playground.vehicle.constant.area.WheelValue`
```kotlin
enum WheelValue : Enum<WheelValue> , AreaValue
```
Represents available wheel area values.

**Entries**
- **UNKNOWN**
- **LEFT_FRONT**
- **RIGHT_FRONT**
- **LEFT_REAR**
- **RIGHT_REAR**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<WheelValue>`
- **id**
  - `override val id: Int`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): WheelValue`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<WheelValue>`

#### `ai.pleos.playground.vehicle.constant.area.WindowValue`
```kotlin
enum WindowValue : Enum<WindowValue> , AreaValue
```
Represents available window area values.

**Entries**
- **FRONT_WINDSHIELD**
- **REAR_WINDSHIELD**
- **ROW_1_LEFT**
- **ROW_1_RIGHT**
- **ROW_2_LEFT**
- **ROW_2_RIGHT**
- **ROOF_TOP_1**
- **ROOF_TOP_2**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<WindowValue>`
- **id**
  - `override val id: Int`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): WindowValue`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<WindowValue>`

### package `ai.pleos.playground.vehicle.constant.control`

- `CruiseControl` — Represents the cruise control state.
- `DrivingModeType` — Represents the driving mode.
- `EvChargeState` — Represents the EV charge state.
- `EvConnectorType` — Represents the EV charging connector type.
- `EvRegenerativeBrakingState` — Represents the EV regenerative braking state.
- `FanDirectionType` — Represents the HVAC fan direction.
- `FuelType` — Represents the fuel type.
- `GsrComplianceType` — Represents the GSR compliance type.
- `LightState` — Represents the light state.
- `LightSwitch` — Represents the light switch state.
- `MoveControl` — Represents the direction used for movement control.
- `OilLevelState` — Represents the oil level state.
- `PortLocationType` — Represents the charge port location.
- `SmartGlassZoneCommand` — Represents the command for a single smart glass panel.
- `SteeringControl` — Represents the steering control state.
- `TripStatisticType` — Represents the type of trip statistic.
- `WiperState` — Represents the wiper state.
- `WiperSwitch` — Represents the wiper switch state.

#### `ai.pleos.playground.vehicle.constant.control.CruiseControl`
```kotlin
enum CruiseControl : Enum<CruiseControl>
```
Represents the cruise control state.

**Entries**
- **OTHER**
- **ENABLED**
- **ACTIVATED**
- **USER_OVERRIDE**
- **SUSPENDED**
- **FORCED_DEACTIVATION_WARNING**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<CruiseControl>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): CruiseControl`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<CruiseControl>`

#### `ai.pleos.playground.vehicle.constant.control.DrivingModeType`
```kotlin
enum DrivingModeType : Enum<DrivingModeType>
```
Represents the driving mode.

**Entries**
- **NORMAL**
- **ECO**
- **SPORT**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<DrivingModeType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): DrivingModeType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<DrivingModeType>`

#### `ai.pleos.playground.vehicle.constant.control.EvChargeState`
```kotlin
enum EvChargeState : Enum<EvChargeState>
```
Represents the EV charge state.

**Entries**
- **UNKNOWN**
- **CHARGING**
- **FULLY_CHARGED**
- **NOT_CHARGING**
- **ERROR**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<EvChargeState>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): EvChargeState`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<EvChargeState>`

#### `ai.pleos.playground.vehicle.constant.control.EvConnectorType`
```kotlin
enum EvConnectorType : Enum<EvConnectorType>
```
Represents the EV charging connector type.

**Entries**
- **UNKNOWN**
- **IEC_TYPE_1_AC**
- **IEC_TYPE_2_AC**
- **IEC_TYPE_3_AC**
- **IEC_TYPE_4_DC**
- **IEC_TYPE_1_CCS_DC**
- **IEC_TYPE_2_CCS_DC**
- **TESLA_ROADSTER**
- **TESLA_HPWC**
- **TESLA_SUPERCHARGER**
- **GBT_AC**
- **GBT_DC**
- **OTHER**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<EvConnectorType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): EvConnectorType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<EvConnectorType>`

#### `ai.pleos.playground.vehicle.constant.control.EvRegenerativeBrakingState`
```kotlin
enum EvRegenerativeBrakingState : Enum<EvRegenerativeBrakingState>
```
Represents the EV regenerative braking state.

**Entries**
- **UNKNOWN**
- **DISABLED**
- **PARTIALLY_ENABLED**
- **FULLY_ENABLED**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<EvRegenerativeBrakingState>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): EvRegenerativeBrakingState`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<EvRegenerativeBrakingState>`

#### `ai.pleos.playground.vehicle.constant.control.FanDirectionType`
```kotlin
enum FanDirectionType : Enum<FanDirectionType>
```
Represents the HVAC fan direction.

**Entries**
- **UNKNOWN**
- **FACE**
- **FLOOR**
- **FACE_AND_FLOOR**
- **DEFROST**
- **FACE_AND_DEFROST**
- **DEFROST_AND_FLOOR**
- **FACE_AND_FLOOR_AND_DEFROST**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<FanDirectionType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): FanDirectionType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<FanDirectionType>`

#### `ai.pleos.playground.vehicle.constant.control.FuelType`
```kotlin
enum FuelType : Enum<FuelType>
```
Represents the fuel type.

**Entries**
- **BIODIESEL**
- **CNG**
- **DIESEL_1**
- **DIESEL_2**
- **E85**
- **ELECTRIC**
- **HYDROGEN**
- **LEADED**
- **LNG**
- **LPG**
- **OTHER**
- **UNKNOWN**
- **UNLEADED**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<FuelType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): FuelType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<FuelType>`

#### `ai.pleos.playground.vehicle.constant.control.GsrComplianceType`
```kotlin
enum GsrComplianceType : Enum<GsrComplianceType>
```
Represents the GSR compliance type.

**Entries**
- **GSR_COMPLIANCE_TYPE_NOT_REQUIRED**
- **GSR_COMPLIANCE_TYPE_REQUIRED_V1**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<GsrComplianceType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): GsrComplianceType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<GsrComplianceType>`

#### `ai.pleos.playground.vehicle.constant.control.LightState`
```kotlin
enum LightState : Enum<LightState>
```
Represents the light state.

**Entries**
- **OFF**
- **ON**
- **DAYTIME_RUNNING**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<LightState>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): LightState`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<LightState>`

#### `ai.pleos.playground.vehicle.constant.control.LightSwitch`
```kotlin
enum LightSwitch : Enum<LightSwitch>
```
Represents the light switch state.

**Entries**
- **OFF**
- **ON**
- **DAYTIME_RUNNING**
- **AUTOMATIC**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<LightSwitch>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): LightSwitch`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<LightSwitch>`

#### `ai.pleos.playground.vehicle.constant.control.MoveControl`
```kotlin
enum MoveControl : Enum<MoveControl>
```
Represents the direction used for movement control.

**Entries**
- **BACKWARD**
- **STOP**
- **FORWARD**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<MoveControl>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): MoveControl`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<MoveControl>`

#### `ai.pleos.playground.vehicle.constant.control.OilLevelState`
```kotlin
enum OilLevelState : Enum<OilLevelState>
```
Represents the oil level state.

**Entries**
- **LOW**
- **HIGH**
- **NORMAL**
- **ERROR**
- **CRITICALLY_LOW**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<OilLevelState>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): OilLevelState`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<OilLevelState>`

#### `ai.pleos.playground.vehicle.constant.control.PortLocationType`
```kotlin
enum PortLocationType : Enum<PortLocationType>
```
Represents the charge port location.

**Entries**
- **UNKNOWN**
- **FRONT_LEFT**
- **FRONT_RIGHT**
- **REAR_RIGHT**
- **REAR_LEFT**
- **FRONT**
- **REAR**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<PortLocationType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): PortLocationType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<PortLocationType>`

#### `ai.pleos.playground.vehicle.constant.control.SmartGlassZoneCommand`
```kotlin
enum SmartGlassZoneCommand : Enum<SmartGlassZoneCommand>
```
Represents the command for a single smart glass panel. Used by ai.pleos.playground.vehicle.api.Window.setSmartGlassZoneState with one entry per panel in panel order.

**Entries**
- **SKIP** — Requests no command for the panel and leaves it unchanged.
- **OFF** — Turns the panel off.
- **ON** — Turns the panel on.

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<SmartGlassZoneCommand>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): SmartGlassZoneCommand`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<SmartGlassZoneCommand>`

#### `ai.pleos.playground.vehicle.constant.control.SteeringControl`
```kotlin
enum SteeringControl : Enum<SteeringControl>
```
Represents the steering control state.

**Entries**
- **OFF**
- **READY**
- **ACTIVE**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<SteeringControl>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): SteeringControl`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<SteeringControl>`

#### `ai.pleos.playground.vehicle.constant.control.TripStatisticType`
```kotlin
enum TripStatisticType : Enum<TripStatisticType>
```
Represents the type of trip statistic.

**Entries**
- **CURRENT**
- **INSTANTANEOUS**
- **CUMULATIVE**
- **SINCE_REFILL**

**Types**
- **Companion**
  - `object Companion`

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<TripStatisticType>`
- **id**
  - `val id: Int`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): TripStatisticType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<TripStatisticType>`

#### `ai.pleos.playground.vehicle.constant.control.WiperState`
```kotlin
enum WiperState : Enum<WiperState>
```
Represents the wiper state.

**Entries**
- **OTHER**
- **OFF**
- **ON**
- **SERVICE**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<WiperState>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): WiperState`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<WiperState>`

#### `ai.pleos.playground.vehicle.constant.control.WiperSwitch`
```kotlin
enum WiperSwitch : Enum<WiperSwitch>
```
Represents the wiper switch state.

**Entries**
- **OTHER**
- **OFF**
- **MIST**
- **INTERMITTENT_LEVEL_1**
- **INTERMITTENT_LEVEL_2**
- **INTERMITTENT_LEVEL_3**
- **INTERMITTENT_LEVEL_4**
- **INTERMITTENT_LEVEL_5**
- **CONTINUOUS_LEVEL_1**
- **CONTINUOUS_LEVEL_2**
- **CONTINUOUS_LEVEL_3**
- **CONTINUOUS_LEVEL_4**
- **CONTINUOUS_LEVEL_5**
- **AUTO**
- **SERVICE**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<WiperSwitch>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): WiperSwitch`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<WiperSwitch>`

### package `ai.pleos.playground.vehicle.constant.telltale`

- `TelltaleColor` — Represents the telltale color.
- `TelltaleState` — Represents the telltale state.
- `TelltaleType` — Represents the telltale type.

#### `ai.pleos.playground.vehicle.constant.telltale.TelltaleColor`
```kotlin
enum TelltaleColor : Enum<TelltaleColor>
```
Represents the telltale color.

**Entries**
- **WHITE**
- **GREEN**
- **BLUE**
- **YELLOW**
- **RED**
- **GRAY**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<TelltaleColor>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): TelltaleColor`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<TelltaleColor>`

#### `ai.pleos.playground.vehicle.constant.telltale.TelltaleState`
```kotlin
enum TelltaleState : Enum<TelltaleState>
```
Represents the telltale state.

**Entries**
- **OFF**
- **ON**
- **BLINK**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<TelltaleState>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): TelltaleState`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<TelltaleState>`

#### `ai.pleos.playground.vehicle.constant.telltale.TelltaleType`
```kotlin
enum TelltaleType : Enum<TelltaleType>
```
Represents the telltale type.

**Entries**
- **PARKING_BRAKE**
- **ELECTRONIC_PARKING_BRAKE**
- **SEAT_BELT**
- **DOOR_OPEN**
- **ABS**
- **MDPS**
- **TPMS**
- **LOW_VOLT_BATTERY**
- **ESC**
- **ESC_OFF**
- **AIRBAG**
- **AFS**
- **MASTER_WARNING**
- **AUTO_HOLD**
- **AUTO_HIGH_BEAM**
- **HEADLAMP_LOW**
- **HEADLAMP_HIGH**
- **POSITION_LAMP**
- **FRONT_FOG_LAMP**
- **REAR_FOG_LAMP**
- **ENGINE_OIL_PRESSURE**
- **ENGINE_TEMPERATURE**
- **EMERGENCY_CALL**
- **EV_SYSTEM**
- **POWER_DOWN**
- **RBS**
- **IMMOBILIZER**
- **ENGINE**
- **LED_HEADLAMP**
- **PREHEAT**
- **PARTICLE_FILTER**
- **H2_SENSOR**
- **H2_LEAK**
- **PASSENGER_AIRBAG_OFF**
- **PASSENGER_AIRBAG_ON**
- **TURN_SIGNAL_LEFT**
- **TURN_SIGNAL_RIGHT**
- **ICY_ROAD**
- **OVER_SPEED**
- **WATER_SEPARATOR**
- **SCR**
- **LOW_EV_BATTERY**
- **HEV_SYSTEM**
- **FCEV_SYSTEM**
- **EREV_SYSTEM**
- **LPI**
- **EV_CHARGING_CONNECTOR**
- **AWD_LOCK**
- **HEV_MODE**
- **ISG**
- **DBC**
- **REV**
- **XEV_READY**
- **I_PEDAL**
- **LOW_FUEL**
- **LOW_FUEL_FCEV**
- **EV_BATTERY_CONDITIONING**
- **TRUNK_OPEN**
- **CHILD_PRESENCE_DETECTION_OFF**
- **DRIVE_MODE**
- **DRIVE_MODE_SPORT**
- **DRIVE_MODE_ECO**
- **DRIVE_MODE_COMFORT**
- **DRIVE_MODE_TERRAIN**
- **FORCED_ALL_WHEEL_DRIVE**
- **FOUR_WHEEL_DRIVE_MODE_2H**
- **FOUR_WHEEL_DRIVE_MODE_4A**
- **FOUR_WHEEL_DRIVE_MODE_4H**
- **FOUR_WHEEL_DRIVE_MODE_4L**
- **TOW_MODE**
- **ESC_SPORT**
- **CREEP_OFF**
- **DRIFT_MODE**
- **TRACK_MODE**
- **UTILITY_MODE**
- **GREEN_ZONE**
- **PHEV_MODE**
- **PHEV_MODE_HEV**
- **PHEV_MODE_AUTO**
- **PHEV_MODE_EV_PLUS**
- **EREV_MODE**
- **LAUNCH_MODE**
- **ACCEL_RESPONSE_DASH**
- **ACCEL_RESPONSE_ACCEL**
- **N_BATTERY_DRAG**
- **N_BATTERY_SPRINT**
- **N_BATTERY_ENDURANCE**
- **FCA**
- **LKA**
- **LFA**
- **LCA_LEFT**
- **LCA_RIGHT**
- **ELK**
- **ICC**
- **DRIVER_ATTENTION**

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<TelltaleType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): TelltaleType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<TelltaleType>`

### package `ai.pleos.playground.vehicle.constant.unit`

- `DistanceDisplayUnit` — Represents the distance display unit.
- `EvBatteryDisplayUnit` — Represents the EV battery display unit.
- `SpeedDisplayUnit` — Represents the speed display unit.
- `TemperatureUnit` — Represents the temperature unit.

#### `ai.pleos.playground.vehicle.constant.unit.DistanceDisplayUnit`
```kotlin
enum DistanceDisplayUnit : Enum<DistanceDisplayUnit>
```
Represents the distance display unit.

**Entries**
- **KILOMETER** — Kilometers (km).
- **MILE** — Miles (mi).

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<DistanceDisplayUnit>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): DistanceDisplayUnit`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<DistanceDisplayUnit>`

#### `ai.pleos.playground.vehicle.constant.unit.EvBatteryDisplayUnit`
```kotlin
enum EvBatteryDisplayUnit : Enum<EvBatteryDisplayUnit>
```
Represents the EV battery display unit.

**Entries**
- **WATT_HOUR** — Watt-hours (Wh).
- **AMPERE_HOURS** — Ampere-hours (Ah).
- **KILOWATT_HOUR** — Kilowatt-hours (kWh).

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<EvBatteryDisplayUnit>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): EvBatteryDisplayUnit`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<EvBatteryDisplayUnit>`

#### `ai.pleos.playground.vehicle.constant.unit.SpeedDisplayUnit`
```kotlin
enum SpeedDisplayUnit : Enum<SpeedDisplayUnit>
```
Represents the speed display unit.

**Entries**
- **METER_PER_SEC** — Meters per second (m/s).
- **MILES_PER_HOUR** — Miles per hour (mph).
- **KILOMETERS_PER_HOUR** — Kilometers per hour (km/h).

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<SpeedDisplayUnit>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): SpeedDisplayUnit`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<SpeedDisplayUnit>`

#### `ai.pleos.playground.vehicle.constant.unit.TemperatureUnit`
```kotlin
enum TemperatureUnit : Enum<TemperatureUnit>
```
Represents the temperature unit.

**Entries**
- **CELSIUS** — Degrees Celsius (°C).
- **FAHRENHEIT** — Degrees Fahrenheit (°F).

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<TemperatureUnit>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): TemperatureUnit`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<TemperatureUnit>`

### package `ai.pleos.playground.vehicle.listener`

- `AmbientColorAutoByDriveModeEnabledListener` — Interface definition for the AmbientColorAutoByDriveModeEnabled updates.
- `AmbientLightBrightnessColorListener` — Interface definition for the AmbientLightBrightnessColor updates
- `AmbientLightBrightnessListener` — Interface definition for the AmbientLightBrightness updates
- `BrakePedalStatusListener` — Interface definition for the BrakePedalStatus updates
- `CruiseControlStateListener` — Interface definition for the CruiseControlState updates.
- `CruiseControlTargetSpeedListener` — Listener for cruise control target speed updates.
- `DistanceDrivenListener` — Listener for distance updates.
- `DoorLockStateListener` — Interface definition for the door lock state updates.
- `DoorPositionListener` — Interface definition for the door position updates.
- `DrivingModeStateListener` — Interface definition for the DrivingModeState updates.
- `DrivingTimeListener` — Interface definition for the driving time updates.
- `EtcsVoiceGuidanceVolumeListener` — Interface definition for ETCS voice guidance volume updates.
- `EvBatteryEfficiencyListener` — Interface definition for the EV battery efficiency updates.
- `EvBatteryStateOfChargeListener` — Interface definition for the EV battery state of charge updates.
- `FuelEconomyListener` — Interface definition for the fuel economy updates.
- `FuelPercentListener` — Interface definition for the fuel percentage updates.
- `GloveBoxLockStateListener` — Interface definition for the GloveBox lock state updates.
- `HvacAcStateListener` — Interface definition for the HvacAcState updates
- `HvacAirCleanerStateListener` — Interface definition for the HvacAirCleanerState updates
- `HvacAutoStateListener` — Interface definition for the HvacAutoState updates
- `HvacDefrosterStateListener` — Interface definition for the HvacDefrosterState updates
- `HvacDualStateListener` — Interface definition for the HvacDualState updates
- `HvacFanSpeedListener` — Interface definition for the HvacFanSpeed updates
- `HvacMaxAcStateListener` — Interface definition for the HvacMaxAcState updates
- `HvacMaxDefrostStateListener` — Interface definition for the HvacMaxDefrostState updates
- `HvacPowerStateListener` — Interface definition for the HvacPowerState updates
- `HvacRecirculationStateListener` — Interface definition for the HvacAutoRecirculationState updates
- `HvacSeatTemperatureListener` — Interface definition for the HvacSeatTemperature updates
- `HvacSeatVentilationListener` — Interface definition for the HvacSeatVentilation updates
- `HvacSideMirrorHeatStateListener` — Interface definition for the HvacSideMirrorHeatState updates.
- `HvacSteeringWheelHeatStateListener` — Interface definition for the HvacSteeringWheelHeatState updates.
- `HvacTemperatureListener` — Listener for HVAC temperature updates.
- `HvacTemperatureValueListener` — Interface definition for HVAC temperature updates in degrees Celsius (°C) and degrees Fahrenheit (°F).
- `LightStateListener` — Interface definition for the HighBeamLightState updates
- `LowWasherFluidWaringListener` — Interface definition for the LowWasherFluidWaring updates.
- `OilLevelStateListener` — Interface definition for the OilLevelState updates.
- `PowerDownWarningIndicatorListener` — Interface definition for the PowerDownWarningIndicator updates.
- `SeatAirbagDeployedListener` — Interface definition for the SeatAirbagDeployed updates
- `SeatAirbagEnabledListener` — Interface definition for the SeatAirbagEnabled updates
- `SeatBackrestAnglePositionListener` — Interface definition for the SeatBackrestAnglePosition updates
- `SeatBeltBuckledStateListener` — Interface definition for the SeatBeltBuckledState updates
- `SeatBeltHeightPositionListener` — Interface definition for the SeatBeltHeightPosition updates
- `SeatBeltNotBuckledWarningListener` — Interface definition for the SeatBeltNotBuckledWarning updates
- `SeatDepthPositionListener` — Interface definition for the SeatDepthPosition updates
- `SeatForeAftPositionListener` — Interface definition for the SeatForeAftPosition updates
- `SeatHeadrestAnglePositionListener` — Interface definition for the SeatHeadrestAnglePosition updates
- `SeatHeadrestForeAftPositionListener` — Interface definition for the SeatHeadrestForeAftPosition updates
- `SeatHeadrestHeightAftPositionListener` — Interface definition for the SeatHeadrestHeightAftPosition updates
- `SeatHeightPositionListener` — Interface definition for the SeatHeightPosition updates
- `SeatLumbarForeAftPositionListener` — Interface definition for the SeatLumbarForeAftPosition updates
- `SeatLumbarSideSupportPositionListener` — Interface definition for the SeatLumbarSideSupportPosition updates
- `SeatOccupancyListener` — Interface definition for the SeatOccupancy updates
- `SeatTiltPositionListener` — Interface definition for the SeatTiltPosition updates
- `SmartGlassLevelListener` — Interface definition for the SmartGlass Level updates
- `SmartGlassStateListener` — Interface definition for the SmartGlassState updates
- `SteeringWheelAngleListener` — Listener for front-wheel steering angle updates.
- `TelltaleInfoListener` — Interface definition for the TelltaleInfo updates
- `TemperatureDisplayUnitListener` — Interface definition for the TemperatureDisplayUnit updates
- `TripDrivingDistanceListener` — Interface definition for the trip driving distance updates.
- `TripDrivingTimeListener` — Interface definition for the trip driving time updates.
- `TripEvEfficiencyListener` — Interface definition for the trip EV efficiency updates.
- `TripFuelEconomyListener` — Interface definition for the trip fuel economy updates.
- `TurnSignalStateListener` — Interface definition for the TurnSignalState updates
- `VehicleListener` — VehicleListener is an interface for constructing standard responses from the vehicle call.
- `WindowLockStateListener` — Interface definition for the WindowLockState updates
- `WindowPositionListener` — Interface definition for the WindowPosition updates
- `WiperStateListener` — Interface definition for the WiperState updates

#### `ai.pleos.playground.vehicle.listener.AmbientColorAutoByDriveModeEnabledListener`
```kotlin
interface AmbientColorAutoByDriveModeEnabledListener : VehicleListener
```
Interface definition for the AmbientColorAutoByDriveModeEnabled updates.

**Functions**
- **onAmbientColorAutoByDriveModeEnabledUpdated** — Callback invoked when AmbientColorAutoByDriveModeEnabled updates.
  - `fun onAmbientColorAutoByDriveModeEnabledUpdated(enabled: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.AmbientLightBrightnessColorListener`
```kotlin
interface AmbientLightBrightnessColorListener : VehicleListener
```
Interface definition for the AmbientLightBrightnessColor updates

**Functions**
- **onBrightnessColorUpdated** — Callback invoked when AmbientLightBrightnessColor updates.
  - `fun onBrightnessColorUpdated(argbColor: Int?)`

#### `ai.pleos.playground.vehicle.listener.AmbientLightBrightnessListener`
```kotlin
interface AmbientLightBrightnessListener : VehicleListener
```
Interface definition for the AmbientLightBrightness updates

**Functions**
- **onBrightnessUpdated** — Callback invoked when AmbientLightBrightness updates.
  - `fun onBrightnessUpdated(brightness: Int?)`

#### `ai.pleos.playground.vehicle.listener.BrakePedalStatusListener`
```kotlin
interface BrakePedalStatusListener : VehicleListener
```
Interface definition for the BrakePedalStatus updates

**Functions**
- **onBrakePedalStatusUpdated** — Callback invoked when BrakePedalStatus updates.
  - `fun onBrakePedalStatusUpdated(status: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.CruiseControlStateListener`
```kotlin
interface CruiseControlStateListener : VehicleListener
```
Interface definition for the CruiseControlState updates.

**Functions**
- **onCruiseControlStateUpdated** — Callback invoked when the CruiseControlState updates.
  - `fun onCruiseControlStateUpdated(state: CruiseControl?)`

#### `ai.pleos.playground.vehicle.listener.CruiseControlTargetSpeedListener`
```kotlin
interface CruiseControlTargetSpeedListener : VehicleListener
```
Listener for cruise control target speed updates.

**Functions**
- **onCruiseControlTargetSpeedUpdated** — Called with the updated cruise control target speed in meters per second (m/s).
  - `fun onCruiseControlTargetSpeedUpdated(speed: Float?)`

#### `ai.pleos.playground.vehicle.listener.DistanceDrivenListener`
```kotlin
interface DistanceDrivenListener : VehicleListener
```
Listener for distance updates. The unit depends on the method used to register this listener: ai.pleos.playground.vehicle.api.Odometer.registerDistanceDriven: kilometers (km). ai.pleos.playground.vehicle.api.Odometer.registerTripCurrentDrivingDistance, ai.pleos.playground.vehicle.api.Odometer.registerTripInstantaneousDrivingDistance, ai.pleos.playground.vehicle.api.Odometer.registerTripCumulativeDrivingDistance, and ai.pleos.playground.vehicle.api.Odometer.registerTripSinceRefillDrivingDistance: meters (m).

**Functions**
- **onDistanceDrivenUpdated** — Called with the updated distance in kilometers (km) or meters (m), depending on the registration API.
  - `fun onDistanceDrivenUpdated(value: Float?)`

#### `ai.pleos.playground.vehicle.listener.DoorLockStateListener`
```kotlin
interface DoorLockStateListener : VehicleListener
```
Interface definition for the door lock state updates.

**Functions**
- **onDoorLockStateUpdated** — Callback invoked when the door lock state updates.
  - `fun onDoorLockStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.DoorPositionListener`
```kotlin
interface DoorPositionListener : VehicleListener
```
Interface definition for the door position updates.

**Functions**
- **onPositionUpdated** — Callback invoked when the door position updates.
  - `fun onPositionUpdated(pos: Int?)`

#### `ai.pleos.playground.vehicle.listener.DrivingModeStateListener`
```kotlin
interface DrivingModeStateListener : VehicleListener
```
Interface definition for the DrivingModeState updates.

**Functions**
- **onDrivingModeStateUpdated** — Callback invoked when the DrivingModeState updates.
  - `fun onDrivingModeStateUpdated(state: DrivingModeType?)`

#### `ai.pleos.playground.vehicle.listener.DrivingTimeListener`
```kotlin
interface DrivingTimeListener : VehicleListener
```
Interface definition for the driving time updates.

**Functions**
- **onDrivingTimeUpdated** — Callback invoked when driving time updates.
  - `fun onDrivingTimeUpdated(value: Int?)`

#### `ai.pleos.playground.vehicle.listener.EtcsVoiceGuidanceVolumeListener`
```kotlin
interface EtcsVoiceGuidanceVolumeListener : VehicleListener
```
Interface definition for ETCS voice guidance volume updates.

**Functions**
- **onEtcsVoiceGuidanceVolumeUpdated** — Callback invoked when ETCS voice guidance volume updates.
  - `fun onEtcsVoiceGuidanceVolumeUpdated(volume: Int?)`

#### `ai.pleos.playground.vehicle.listener.EvBatteryEfficiencyListener`
```kotlin
interface EvBatteryEfficiencyListener : VehicleListener
```
Interface definition for the EV battery efficiency updates.

**Functions**
- **onEvBatteryEfficiencyUpdated** — Callback invoked when EV battery efficiency updates.
  - `fun onEvBatteryEfficiencyUpdated(efficiency: Float?)`

#### `ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener`
```kotlin
interface EvBatteryStateOfChargeListener : VehicleListener
```
Interface definition for the EV battery state of charge updates.

**Functions**
- **onEvBatteryStateOfChargeUpdated** — Callback invoked with the updated EV battery state of charge as a percentage (%).
  - `fun onEvBatteryStateOfChargeUpdated(value: Int?)`

#### `ai.pleos.playground.vehicle.listener.FuelEconomyListener`
```kotlin
interface FuelEconomyListener : VehicleListener
```
Interface definition for the fuel economy updates.

**Functions**
- **onFuelEconomyUpdated** — Callback invoked when fuel economy updates.
  - `fun onFuelEconomyUpdated(efficiency: Float?)`

#### `ai.pleos.playground.vehicle.listener.FuelPercentListener`
```kotlin
interface FuelPercentListener : VehicleListener
```
Interface definition for the fuel percentage updates.

**Functions**
- **onFuelPercentUpdated** — Callback invoked with the updated fuel level as a percentage (%).
  - `fun onFuelPercentUpdated(value: Int?)`

#### `ai.pleos.playground.vehicle.listener.GloveBoxLockStateListener`
```kotlin
interface GloveBoxLockStateListener : VehicleListener
```
Interface definition for the GloveBox lock state updates.

**Functions**
- **onGloveBoxLockStateUpdated** — Callback invoked when the GloveBox lock state updates.
  - `fun onGloveBoxLockStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.HvacAcStateListener`
```kotlin
interface HvacAcStateListener : VehicleListener
```
Interface definition for the HvacAcState updates

**Functions**
- **onHvacAcStateUpdated** — Callback invoked when HvacAcState updates.
  - `fun onHvacAcStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.HvacAirCleanerStateListener`
```kotlin
interface HvacAirCleanerStateListener : VehicleListener
```
Interface definition for the HvacAirCleanerState updates

**Functions**
- **onHvacAirCleanerStateUpdated** — Callback invoked when HvacAirCleanerState updates.
  - `fun onHvacAirCleanerStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.HvacAutoStateListener`
```kotlin
interface HvacAutoStateListener : VehicleListener
```
Interface definition for the HvacAutoState updates

**Functions**
- **onHvacAutoStateUpdated** — Callback invoked when HvacAutoState updates.
  - `fun onHvacAutoStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.HvacDefrosterStateListener`
```kotlin
interface HvacDefrosterStateListener : VehicleListener
```
Interface definition for the HvacDefrosterState updates

**Functions**
- **onHvacDefrosterStateUpdated** — Callback invoked when HvacDefrosterState updates.
  - `fun onHvacDefrosterStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.HvacDualStateListener`
```kotlin
interface HvacDualStateListener : VehicleListener
```
Interface definition for the HvacDualState updates

**Functions**
- **onHvacDualStateUpdated** — Callback invoked when HvacDualState updates.
  - `fun onHvacDualStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.HvacFanSpeedListener`
```kotlin
interface HvacFanSpeedListener : VehicleListener
```
Interface definition for the HvacFanSpeed updates

**Functions**
- **onHvacFanSpeedUpdated** — Callback invoked when HvacFanSpeed updates.
  - `fun onHvacFanSpeedUpdated(speed: Int?)`

#### `ai.pleos.playground.vehicle.listener.HvacMaxAcStateListener`
```kotlin
interface HvacMaxAcStateListener : VehicleListener
```
Interface definition for the HvacMaxAcState updates

**Functions**
- **onHvacMaxAcStateUpdated** — Callback invoked when HvacMaxAcState updates.
  - `fun onHvacMaxAcStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.HvacMaxDefrostStateListener`
```kotlin
interface HvacMaxDefrostStateListener : VehicleListener
```
Interface definition for the HvacMaxDefrostState updates

**Functions**
- **onHvacMaxDefrostStateUpdated** — Callback invoked when HvacMaxDefrostState updates.
  - `fun onHvacMaxDefrostStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.HvacPowerStateListener`
```kotlin
interface HvacPowerStateListener : VehicleListener
```
Interface definition for the HvacPowerState updates

**Functions**
- **onHvacPowerStateUpdated** — Callback invoked when HvacPowerState updates.
  - `fun onHvacPowerStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.HvacRecirculationStateListener`
```kotlin
interface HvacRecirculationStateListener : VehicleListener
```
Interface definition for the HvacAutoRecirculationState updates

**Functions**
- **onHvacAutoRecirculationStateUpdated** — Callback invoked when HvacAutoRecirculationState updates.
  - `fun onHvacAutoRecirculationStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.HvacSeatTemperatureListener`
```kotlin
interface HvacSeatTemperatureListener : VehicleListener
```
Interface definition for the HvacSeatTemperature updates

**Functions**
- **onHvacSeatTemperatureUpdated** — Callback invoked when HvacSeatTemperature updates.
  - `fun onHvacSeatTemperatureUpdated(temperature: Int?)`

#### `ai.pleos.playground.vehicle.listener.HvacSeatVentilationListener`
```kotlin
interface HvacSeatVentilationListener : VehicleListener
```
Interface definition for the HvacSeatVentilation updates

**Functions**
- **onHvacSeatVentilationUpdated** — Callback invoked when HvacSeatVentilation updates.
  - `fun onHvacSeatVentilationUpdated(ventilation: Int?)`

#### `ai.pleos.playground.vehicle.listener.HvacSideMirrorHeatStateListener`
```kotlin
interface HvacSideMirrorHeatStateListener : VehicleListener
```
Interface definition for the HvacSideMirrorHeatState updates.

**Functions**
- **onHvacSideMirrorHeatStateUpdated** — Callback invoked when HvacSideMirrorHeatState updates.
  - `fun onHvacSideMirrorHeatStateUpdated(state: Int?)`

#### `ai.pleos.playground.vehicle.listener.HvacSteeringWheelHeatStateListener`
```kotlin
interface HvacSteeringWheelHeatStateListener : VehicleListener
```
Interface definition for the HvacSteeringWheelHeatState updates.

**Functions**
- **onHvacSteeringWheelHeatStateUpdated** — Callback invoked when the HvacSteeringWheelHeatState updates.
  - `fun onHvacSteeringWheelHeatStateUpdated(state: Int?)`

#### `ai.pleos.playground.vehicle.listener.HvacTemperatureListener`
```kotlin
interface HvacTemperatureListener : VehicleListener
```
Listener for HVAC temperature updates. Reports the target temperature when registered with ai.pleos.playground.vehicle.api.HVAC.registerHvacTemperature, or the current interior temperature when registered with ai.pleos.playground.vehicle.api.HVAC.registerHvacTemperatureCurrent. Both registration methods report values in degrees Celsius (°C).

**Functions**
- **onHvacTemperatureUpdated** — Called with the updated HVAC temperature in degrees Celsius (°C).
  - `fun onHvacTemperatureUpdated(temperature: Float?)`

#### `ai.pleos.playground.vehicle.listener.HvacTemperatureValueListener`
```kotlin
interface HvacTemperatureValueListener : VehicleListener
```
Interface definition for HVAC temperature updates in degrees Celsius (°C) and degrees Fahrenheit (°F).

**Functions**
- **onHvacTemperatureUpdated** — Callback invoked when HVAC temperature updates.
  - `fun onHvacTemperatureUpdated(celsius: Float?, fahrenheit: Float?)`

#### `ai.pleos.playground.vehicle.listener.LightStateListener`
```kotlin
interface LightStateListener : VehicleListener
```
Interface definition for the HighBeamLightState updates

**Functions**
- **onLightStateUpdated** — Callback invoked when HighBeamLightState updates.
  - `fun onLightStateUpdated(state: LightState?)`

#### `ai.pleos.playground.vehicle.listener.LowWasherFluidWaringListener`
```kotlin
interface LowWasherFluidWaringListener : VehicleListener
```
Interface definition for the LowWasherFluidWaring updates.

**Functions**
- **onLowWasherFluidWaringUpdated** — Callback invoked when the LowWasherFluidWaring updates.
  - `fun onLowWasherFluidWaringUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.OilLevelStateListener`
```kotlin
interface OilLevelStateListener : VehicleListener
```
Interface definition for the OilLevelState updates.

**Functions**
- **onOilLevelUpdated** — Callback invoked when the OilLevelState updates.
  - `fun onOilLevelUpdated(level: OilLevelState?)`

#### `ai.pleos.playground.vehicle.listener.PowerDownWarningIndicatorListener`
```kotlin
interface PowerDownWarningIndicatorListener : VehicleListener
```
Interface definition for the PowerDownWarningIndicator updates.

**Functions**
- **onPowerDownWarningIndicatorUpdated** — Callback invoked when the PowerDownWarningIndicator updates.
  - `fun onPowerDownWarningIndicatorUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.SeatAirbagDeployedListener`
```kotlin
interface SeatAirbagDeployedListener : VehicleListener
```
Interface definition for the SeatAirbagDeployed updates

**Functions**
- **onSeatAirbagDeployUpdated** — Callback invoked when SeatAirbagDeployed updates.
  - `fun onSeatAirbagDeployUpdated(deployed: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.SeatAirbagEnabledListener`
```kotlin
interface SeatAirbagEnabledListener : VehicleListener
```
Interface definition for the SeatAirbagEnabled updates

**Functions**
- **onSeatAirbagEnabledUpdated** — Callback invoked when SeatAirbagEnabled updates.
  - `fun onSeatAirbagEnabledUpdated(enabled: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.SeatBackrestAnglePositionListener`
```kotlin
interface SeatBackrestAnglePositionListener : VehicleListener
```
Interface definition for the SeatBackrestAnglePosition updates

**Functions**
- **onSeatBackrestAnglePositionUpdated** — Callback invoked when SeatBackrestAnglePosition updates.
  - `fun onSeatBackrestAnglePositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.SeatBeltBuckledStateListener`
```kotlin
interface SeatBeltBuckledStateListener : VehicleListener
```
Interface definition for the SeatBeltBuckledState updates

**Functions**
- **onSeatBeltBuckledStateUpdated** — Callback invoked when SeatBeltBuckledState updates.
  - `fun onSeatBeltBuckledStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.SeatBeltHeightPositionListener`
```kotlin
interface SeatBeltHeightPositionListener : VehicleListener
```
Interface definition for the SeatBeltHeightPosition updates

**Functions**
- **onSeatBeltHeightPositionUpdated** — Callback invoked when SeatBeltHeightPosition updates.
  - `fun onSeatBeltHeightPositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.SeatBeltNotBuckledWarningListener`
```kotlin
interface SeatBeltNotBuckledWarningListener : VehicleListener
```
Interface definition for the SeatBeltNotBuckledWarning updates

**Functions**
- **onSeatBeltNotBuckledWarningUpdated** — Callback invoked when SeatBeltNotBuckledWarning updates.
  - `fun onSeatBeltNotBuckledWarningUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.SeatDepthPositionListener`
```kotlin
interface SeatDepthPositionListener : VehicleListener
```
Interface definition for the SeatDepthPosition updates

**Functions**
- **onSeatDepthPositionUpdated** — Callback invoked when SeatDepthPosition updates.
  - `fun onSeatDepthPositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.SeatForeAftPositionListener`
```kotlin
interface SeatForeAftPositionListener : VehicleListener
```
Interface definition for the SeatForeAftPosition updates

**Functions**
- **onSeatForeAftPositionUpdated** — Callback invoked when SeatForeAftPosition updates.
  - `fun onSeatForeAftPositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.SeatHeadrestAnglePositionListener`
```kotlin
interface SeatHeadrestAnglePositionListener : VehicleListener
```
Interface definition for the SeatHeadrestAnglePosition updates

**Functions**
- **onSeatHeadrestAnglePositionUpdated** — Callback invoked when SeatHeadrestAnglePosition updates.
  - `fun onSeatHeadrestAnglePositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.SeatHeadrestForeAftPositionListener`
```kotlin
interface SeatHeadrestForeAftPositionListener : VehicleListener
```
Interface definition for the SeatHeadrestForeAftPosition updates

**Functions**
- **onSeatHeadrestForeAftPositionUpdated** — Callback invoked when SeatHeadrestForeAftPosition updates.
  - `fun onSeatHeadrestForeAftPositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.SeatHeadrestHeightAftPositionListener`
```kotlin
interface SeatHeadrestHeightAftPositionListener : VehicleListener
```
Interface definition for the SeatHeadrestHeightAftPosition updates

**Functions**
- **onSeatHeadrestHeightAftPositionUpdated** — Callback invoked when SeatHeadrestHeightAftPosition updates.
  - `fun onSeatHeadrestHeightAftPositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.SeatHeightPositionListener`
```kotlin
interface SeatHeightPositionListener : VehicleListener
```
Interface definition for the SeatHeightPosition updates

**Functions**
- **onSeatHeightPositionUpdated** — Callback invoked when SeatHeightPosition updates.
  - `fun onSeatHeightPositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.SeatLumbarForeAftPositionListener`
```kotlin
interface SeatLumbarForeAftPositionListener : VehicleListener
```
Interface definition for the SeatLumbarForeAftPosition updates

**Functions**
- **onSeatLumbarForeAftPositionUpdated** — Callback invoked when SeatLumbarForeAftPosition updates.
  - `fun onSeatLumbarForeAftPositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.SeatLumbarSideSupportPositionListener`
```kotlin
interface SeatLumbarSideSupportPositionListener : VehicleListener
```
Interface definition for the SeatLumbarSideSupportPosition updates

**Functions**
- **onSeatLumbarSideSupportPositionUpdated** — Callback invoked when SeatLumbarSideSupportPosition updates.
  - `fun onSeatLumbarSideSupportPositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.SeatOccupancyListener`
```kotlin
interface SeatOccupancyListener : VehicleListener
```
Interface definition for the SeatOccupancy updates

**Functions**
- **onSeatOccupancyUpdated** — Callback invoked when SeatOccupancy updates.
  - `fun onSeatOccupancyUpdated(occupancy: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.SeatTiltPositionListener`
```kotlin
interface SeatTiltPositionListener : VehicleListener
```
Interface definition for the SeatTiltPosition updates

**Functions**
- **onSeatTiltPositionUpdated** — Callback invoked when SeatTiltPosition updates.
  - `fun onSeatTiltPositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.SmartGlassLevelListener`
```kotlin
interface SmartGlassLevelListener : VehicleListener
```
Interface definition for the SmartGlass Level updates

**Functions**
- **onSmartGlassLevelUpdated** — Callback invoked when SmartGlass Level updates.
  - `fun onSmartGlassLevelUpdated(level: Int?)`

#### `ai.pleos.playground.vehicle.listener.SmartGlassStateListener`
```kotlin
interface SmartGlassStateListener : VehicleListener
```
Interface definition for the SmartGlassState updates

**Functions**
- **onSmartGlassStateUpdated** — Callback invoked when SmartGlassState updates.
  - `fun onSmartGlassStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener`
```kotlin
interface SteeringWheelAngleListener : VehicleListener
```
Listener for front-wheel steering angle updates.

**Functions**
- **onSteeringWheelAngleUpdated** — Called when a front-wheel steering angle update is available.
  - `fun onSteeringWheelAngleUpdated(angle: Float?)`

#### `ai.pleos.playground.vehicle.listener.TelltaleInfoListener`
```kotlin
interface TelltaleInfoListener : VehicleListener
```
Interface definition for the TelltaleInfo updates

**Functions**
- **onTelltaleInfoUpdated** — Callback invoked when TelltaleInfo updates.
  - `fun onTelltaleInfoUpdated(telltales: Telltales?)`

#### `ai.pleos.playground.vehicle.listener.TemperatureDisplayUnitListener`
```kotlin
interface TemperatureDisplayUnitListener : VehicleListener
```
Interface definition for the TemperatureDisplayUnit updates

**Functions**
- **onTemperatureDisplayUnitUpdated** — Callback invoked when TemperatureDisplayUnit updates.
  - `fun onTemperatureDisplayUnitUpdated(unit: TemperatureUnit?)`

#### `ai.pleos.playground.vehicle.listener.TripDrivingDistanceListener`
```kotlin
interface TripDrivingDistanceListener : VehicleListener
```
Interface definition for the trip driving distance updates.

**Functions**
- **onTripDrivingDistanceUpdated** — Callback invoked when trip driving distance updates.
  - `fun onTripDrivingDistanceUpdated(statistics: TripStatistics<Float>?)`

#### `ai.pleos.playground.vehicle.listener.TripDrivingTimeListener`
```kotlin
interface TripDrivingTimeListener : VehicleListener
```
Interface definition for the trip driving time updates.

**Functions**
- **onTripDrivingTimeUpdated** — Callback invoked when trip driving time updates.
  - `fun onTripDrivingTimeUpdated(statistics: TripStatistics<Int>?)`

#### `ai.pleos.playground.vehicle.listener.TripEvEfficiencyListener`
```kotlin
interface TripEvEfficiencyListener : VehicleListener
```
Interface definition for the trip EV efficiency updates.

**Functions**
- **onTripEvEfficiencyUpdated** — Callback invoked when trip EV efficiency updates.
  - `fun onTripEvEfficiencyUpdated(statistics: TripStatistics<Float>?)`

#### `ai.pleos.playground.vehicle.listener.TripFuelEconomyListener`
```kotlin
interface TripFuelEconomyListener : VehicleListener
```
Interface definition for the trip fuel economy updates.

**Functions**
- **onTripFuelEconomyUpdated** — Callback invoked when trip fuel economy updates.
  - `fun onTripFuelEconomyUpdated(statistics: TripStatistics<Float>?)`

#### `ai.pleos.playground.vehicle.listener.TurnSignalStateListener`
```kotlin
interface TurnSignalStateListener : VehicleListener
```
Interface definition for the TurnSignalState updates

**Functions**
- **onTurnSignalStateUpdated** — Callback invoked when TurnSignalStates updates.
  - `fun onTurnSignalStateUpdated(state: TurnSignalValue?)`

#### `ai.pleos.playground.vehicle.listener.VehicleListener`
```kotlin
sealed interface VehicleListener
```
VehicleListener is an interface for constructing standard responses from the vehicle call.

**Inheritors**
- **AmbientColorAutoByDriveModeEnabledListener**
- **AmbientLightBrightnessColorListener**
- **AmbientLightBrightnessListener**
- **BrakePedalStatusListener**
- **CruiseControlStateListener**
- **CruiseControlTargetSpeedListener**
- **DistanceDrivenListener**
- **DoorLockStateListener**
- **DoorPositionListener**
- **DrivingModeStateListener**
- **DrivingTimeListener**
- **EtcsVoiceGuidanceVolumeListener**
- **EvBatteryEfficiencyListener**
- **EvBatteryStateOfChargeListener**
- **FuelEconomyListener**
- **FuelPercentListener**
- **GloveBoxLockStateListener**
- **HvacAcStateListener**
- **HvacAirCleanerStateListener**
- **HvacAutoStateListener**
- **HvacDefrosterStateListener**
- **HvacDualStateListener**
- **HvacFanSpeedListener**
- **HvacMaxAcStateListener**
- **HvacMaxDefrostStateListener**
- **HvacPowerStateListener**
- **HvacRecirculationStateListener**
- **HvacSeatTemperatureListener**
- **HvacSeatVentilationListener**
- **HvacSideMirrorHeatStateListener**
- **HvacSteeringWheelHeatStateListener**
- **HvacTemperatureListener**
- **HvacTemperatureValueListener**
- **LightStateListener**
- **LowWasherFluidWaringListener**
- **OilLevelStateListener**
- **PowerDownWarningIndicatorListener**
- **SeatAirbagDeployedListener**
- **SeatAirbagEnabledListener**
- **SeatBackrestAnglePositionListener**
- **SeatBeltBuckledStateListener**
- **SeatBeltHeightPositionListener**
- **SeatBeltNotBuckledWarningListener**
- **SeatDepthPositionListener**
- **SeatForeAftPositionListener**
- **SeatHeadrestAnglePositionListener**
- **SeatHeadrestForeAftPositionListener**
- **SeatHeadrestHeightAftPositionListener**
- **SeatHeightPositionListener**
- **SeatLumbarForeAftPositionListener**
- **SeatLumbarSideSupportPositionListener**
- **SeatOccupancyListener**
- **SeatTiltPositionListener**
- **SmartGlassLevelListener**
- **SmartGlassStateListener**
- **SteeringWheelAngleListener**
- **TelltaleInfoListener**
- **TemperatureDisplayUnitListener**
- **TripDrivingDistanceListener**
- **TripDrivingTimeListener**
- **TripEvEfficiencyListener**
- **TripFuelEconomyListener**
- **TurnSignalStateListener**
- **WindowLockStateListener**
- **WindowPositionListener**
- **WiperStateListener**

**Functions**
- **onFailed** — Callback invoked when a exception occurs.
  - `fun onFailed(e: Exception)`

#### `ai.pleos.playground.vehicle.listener.WindowLockStateListener`
```kotlin
interface WindowLockStateListener : VehicleListener
```
Interface definition for the WindowLockState updates

**Functions**
- **onWindowLockStateUpdated** — Callback invoked when WindowLockState updates.
  - `fun onWindowLockStateUpdated(state: Boolean?)`

#### `ai.pleos.playground.vehicle.listener.WindowPositionListener`
```kotlin
interface WindowPositionListener : VehicleListener
```
Interface definition for the WindowPosition updates

**Functions**
- **onWindowPositionUpdated** — Callback invoked when WindowPosition updates.
  - `fun onWindowPositionUpdated(position: Int?)`

#### `ai.pleos.playground.vehicle.listener.WiperStateListener`
```kotlin
interface WiperStateListener : VehicleListener
```
Interface definition for the WiperState updates

**Functions**
- **onWiperStateUpdated** — Callback invoked when WiperState updates.
  - `fun onWiperStateUpdated(state: WiperState?)`

---

## NaviHelper SDK


### package `ai.pleos.playground.navi.constants`

- `BookmarkType`
- `CameraType`
- `CautionPointType`
- `NaviErrorCode`
- `NavigationStatus`
- `NaviMode`
- `RouteOption`
- `RoutePointPoiType`
- `RoutePointSettingType`
- `RoutePointType`
- `TrafficCongestionType`
- `TrafficEventType`
- `TurnType`
- `WaypointCmdType`
- `WaypointIndex`

#### `ai.pleos.playground.navi.constants.BookmarkType`
```kotlin
@Serializable enum BookmarkType : Enum<BookmarkType>
```

**Entries**
- **HOME** — Home
- **OFFICE** — Office
- **EV_CHARGING_STATION** — EV Charging Station
- **DEFAULT** — Default

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<BookmarkType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): BookmarkType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<BookmarkType>`

#### `ai.pleos.playground.navi.constants.CameraType`
```kotlin
@Serializable enum CameraType : Enum<CameraType>
```

**Entries**
- **SPEED_CAMERA_FIX** — Fixed speed camera
- **SPEED_CAMERA_MOV** — Mobile speed camera
- **SPEED_CAMERA_SECTION_START** — Section enforcement start camera
- **SECTION_SPEED_ALERT** — Section speed alert
- **SPEED_CAMERA_SECTION_END** — Section enforcement end camera
- **SPEED_CAMERA_TRAFFIC_LIGHT** — Red light speed camera
- **SPEED_CAMERA_REAR** — Rear-facing speed enforcement camera
- **VARIABLE_SPEED_CAMERA** — Variable speed limit enforcement camera
- **SPEED_CAMERA_SCHOOL_ZONE** — School zone speed enforcement camera
- **SPEED_CAMERA_SCHOOL_ZONE_TRAFFIC_LIGHT** — School zone red light speed enforcement camera

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<CameraType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): CameraType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<CameraType>`

#### `ai.pleos.playground.navi.constants.CautionPointType`
```kotlin
@Serializable enum CautionPointType : Enum<CautionPointType>
```

**Entries**
- **SPEED_BUMP** — Speed bump
- **SCHOOL_ZONE** — School zone
- **ACCIDENT_PRONE_AREA** — High accident area
- **RAILROAD_CROSSING** — Railroad crossing
- **FOG_AREA** — Fog area
- **FALLING_ROCK_AREA** — Falling rocks caution zone
- **DOWNHILL_CAUTION_AREA** — Steep descent caution zone
- **WILDLIFE_CROSSING** — Wild animal caution zone
- **NARROW_ROAD** — Narrowing road
- **HIGHWAY_CONSTRUCTION** — Highway construction
- **TRAFFIC_INFO_COLLECTION** — Traffic information collection
- **NO_PARKING_ZONE** — No parking or stopping caution
- **TRAFFIC_LIGHT_VIOLATION_ENFORCEMENT** — Red light enforcement caution
- **BUS_LANE_VIOLATION_ENFORCEMENT** — Bus lane violation enforcement caution
- **CUT_IN_VIOLATION_ENFORCEMENT** — Cut-in violation enforcement caution
- **BLOCK_INTERSECTION_ENFORCEMENT** — Blocking intersection violation enforcement caution
- **LANE_CHANGE_CAMERA_ALERT** — Lane change violation camera caution
- **SLIPPERY_ROAD_ALERT** — Slippery road detection caution
- **ROAD_DAMAGE_ALERT** — Road damage detection caution
- **WRONG_WAY_DRIVING_ALERT** — Wrong-way driving alert
- **BUS_LANE_UNAVAILABLE_NOTICE** — Bus lane access restriction alert
- **BUS_LANE_AVAILABLE_NOTICE** — Bus lane access allowed alert
- **SIGN_MALFUNCION** — Sign system malfunction

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<CautionPointType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): CautionPointType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<CautionPointType>`

#### `ai.pleos.playground.navi.constants.NaviErrorCode`
```kotlin
@Serializable enum NaviErrorCode : Enum<NaviErrorCode>
```

**Entries**
- **CAUSE_LOW_BATTERY** — Low battery alert
- **CAUSE_SEARCH_ROUTE_OVER_DISTANCE** — Error: route path over max distance
- **CAUSE_SEARCH_ROUTE** — Error: route server side
- **CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE** — Error: distance between current location and destination is too short
- **CAUSE_SEARCH_TRIP** — Error: trip server side

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<NaviErrorCode>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): NaviErrorCode`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<NaviErrorCode>`

#### `ai.pleos.playground.navi.constants.NavigationStatus`
```kotlin
@Serializable enum NavigationStatus : Enum<NavigationStatus>
```

**Entries**
- **INACTIVE** — The navigation is not functioning or has completely shut down.
- **INITIALIZING** — The navigation service or map UI is being prepared. This includes initialization of settings, map rendering, and GPS sensor preparation.
- **FREE_DRIVE** — A state where only the map is shown following the user's location without route guidance. Normal driving mode.
- **ROUTE_DRIVE** — A state where the user has set a destination and route guidance is activated. Real-time navigation is in operation.

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<NavigationStatus>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): NavigationStatus`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<NavigationStatus>`

#### `ai.pleos.playground.navi.constants.NaviMode`
```kotlin
@Serializable enum NaviMode : Enum<NaviMode>
```

**Entries**
- **NOT_WORKING** — Navi Initializing or downloading map, etc.
- **SAFE_GUIDING** — Safe guiding
- **ROUTE_GUIDING** — Route guiding
- **SIMULATION_GUIDING** — Simulation guiding

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<NaviMode>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): NaviMode`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<NaviMode>`

#### `ai.pleos.playground.navi.constants.RouteOption`
```kotlin
@Serializable enum RouteOption : Enum<RouteOption>
```

**Entries**
- **RECOMMENDED** — Recommended
- **SHORT_DISTANCE** — Short distance
- **SHORT_TIME** — Short time
- **FREE** — Free
- **HIGHWAY** — Highway
- **COMFORTABLE** — Comfortable
- **SCHOOL_ZONE** — School zone

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<RouteOption>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): RouteOption`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<RouteOption>`

#### `ai.pleos.playground.navi.constants.RoutePointPoiType`
```kotlin
@Serializable enum RoutePointPoiType : Enum<RoutePointPoiType>
```

**Entries**
- **NORMAL** — General location or no POI search performed.
- **EV_CHARGING** — EV charging station

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<RoutePointPoiType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): RoutePointPoiType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<RoutePointPoiType>`

#### `ai.pleos.playground.navi.constants.RoutePointSettingType`
```kotlin
@Serializable enum RoutePointSettingType : Enum<RoutePointSettingType>
```

**Entries**
- **AUTO** — Automatic
- **MANUAL** — User-defined

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<RoutePointSettingType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): RoutePointSettingType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<RoutePointSettingType>`

#### `ai.pleos.playground.navi.constants.RoutePointType`
```kotlin
@Serializable enum RoutePointType : Enum<RoutePointType>
```

**Entries**
- **ORIGIN** — Departure
- **WAYPOINT** — Waypoint
- **DESTINATION** — Destination

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<RoutePointType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): RoutePointType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<RoutePointType>`

#### `ai.pleos.playground.navi.constants.TrafficCongestionType`
```kotlin
@Serializable enum TrafficCongestionType : Enum<TrafficCongestionType>
```

**Entries**
- **UNKNOWN** — Unknown (No traffic data collected or SafeGuiding)
- **FAST** — Fast (Smooth traffic)
- **MODERATE** — Moderate (Moderate traffic)
- **SLOW** — Slow (Slow traffic)
- **HEAVY** — Heavy (Congested traffic)

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<TrafficCongestionType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): TrafficCongestionType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<TrafficCongestionType>`

#### `ai.pleos.playground.navi.constants.TrafficEventType`
```kotlin
@Serializable enum TrafficEventType : Enum<TrafficEventType>
```

**Entries**
- **ETC** — Etc. (Other)
- **ACCIDENT** — Accident
- **ROAD_DAMAGE** — Road damage
- **REPAIRING** — Road repair
- **EVENT** — Event
- **DISASTER** — Disaster

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<TrafficEventType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): TrafficEventType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<TrafficEventType>`

#### `ai.pleos.playground.navi.constants.TurnType`
```kotlin
@Serializable enum TurnType : Enum<TurnType>
```

**Entries**
- **TURN_UNKNOWN** — UNKNOWN
- **TURN_STRAIGHT** — Go straight
- **TURN_RIGHT** — Turn right
- **TURN_LEFT** — Turn left
- **TURN_KEEP_R** — Right side
- **TURN_KEEP_L** — Left side
- **TURN_SHARP_R** — Sharp right turn
- **TURN_SHARP_L** — Sharp left turn
- **TURN_UTURN** — U-turn
- **TURN_ENTER_IC_R** — Enter highway (Right)
- **TURN_ENTER_IC_L** — Enter highway (Left)
- **TURN_ENTER_IC_C** — Enter highway (Front)
- **TURN_EXIT_IC_R** — Exit highway (Right)
- **TURN_EXIT_IC_L** — Exit highway (Left)
- **TURN_EXIT_IC_C** — Exit highway (Front)
- **TURN_ENTER_UNDER_R** — Enter underpass (Right)
- **TURN_ENTER_UNDER_L** — Enter underpass (Left)
- **TURN_ENTER_UNDER_C** — Enter underpass (Front)
- **TURN_AVOID_UNDER_R** — Exit underpass (Right)
- **TURN_AVOID_UNDER_L** — Exit underpass (Left)
- **TURN_AVOID_UNDER_C** — Exit underpass (Front)
- **TURN_ENTER_OVER_R** — Enter overpass (Right)
- **TURN_ENTER_OVER_L** — Enter overpass (Left)
- **TURN_ENTER_OVER_C** — Enter overpass (Front)
- **TURN_AVOID_OVER_R** — Exit overpass (Right)
- **TURN_AVOID_OVER_L** — Exit overpass (Left)
- **TURN_AVOID_OVER_C** — Exit overpass (Front)
- **TURN_ENTER_ROUNDABOUT_02** — Enter roundabout (2 o'clock direction)
- **TURN_ENTER_ROUNDABOUT_03** — Enter roundabout (3 o'clock direction)
- **TURN_ENTER_ROUNDABOUT_04** — Enter roundabout (4 o'clock direction)
- **TURN_ENTER_ROUNDABOUT_06** — Enter roundabout (6 o'clock direction)
- **TURN_ENTER_ROUNDABOUT_08** — Enter roundabout (8 o'clock direction)
- **TURN_ENTER_ROUNDABOUT_09** — Enter roundabout (9 o'clock direction)
- **TURN_ENTER_ROUNDABOUT_10** — Enter roundabout (10 o'clock direction)
- **TURN_ENTER_ROUNDABOUT_12** — Enter roundabout (12 o'clock direction)
- **TURN_EXIT_ROUNDABOUT** — Exit roundabout
- **TURN_WAY** — Waypoint
- **TURN_ARRIVE** — Arrival information
- **TURN_TUNNEL** — Tunnel
- **TURN_DEVIDE_MERGE** — Split and merge road
- **TURN_TOLL** — Tollgate
- **TURN_ENTER_SERVICEAREA** — Enter the rest area
- **TURN_RESTAREA** — Drowsy driving rest area

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<TurnType>`
- **value**
  - `val value: Long`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): TurnType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<TurnType>`

#### `ai.pleos.playground.navi.constants.WaypointCmdType`
```kotlin
@Serializable enum WaypointCmdType : Enum<WaypointCmdType>
```

**Entries**
- **ADD** — Add
- **REMOVE** — Remove

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<WaypointCmdType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): WaypointCmdType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<WaypointCmdType>`

#### `ai.pleos.playground.navi.constants.WaypointIndex`
```kotlin
@Serializable enum WaypointIndex : Enum<WaypointIndex>
```

**Entries**
- **FIRST** — First
- **SECOND** — Second
- **ALL** — All

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<WaypointIndex>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): WaypointIndex`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<WaypointIndex>`

### package `ai.pleos.playground.navi.data`

- `BookmarkInfo` — Data class representing bookmark information for a specific location.
- `CameraAlert` — Data class representing a camera alert, typically used in navigation systems.
- `CautionPoint` — Data class representing caution point alerts.
- `ChargerOperator` — Data class representing an electric vehicle (EV) charger operator.
- `ChargingPlan` — Data class representing the specific charging plan session at a route point.
- `CurrentLocationInfo` — Data class representing the current location information in a navigation system.
- `DestinationArrivedInfo` — Data class representing information when the destination is reached in a navigation system.
- `DestinationInfo` — Data class representing destination information in a navigation system.
- `DrivingInfo` — Data class representing driving information, including the destination and waypoints.
- `DrivingState` — Data class representing the driving state in a route.
- `NavigationScreenRequest` — Sealed class representing a request to switch the navigation screen.
- `RecentDestinationInfo` — Data class representing information about a recently visited destination.
- `RequestWaypointInfo` — Data class representing a waypoint request in a navigation system.
- `RouteInfo` — Data class representing navigation route information.
- `RouteOverviewShowInfo` — Data class representing the route overview display settings.
- `RoutePointInfo` — Data class representing destination information in a navigation system.
- `RouteStartInfo` — Data class representing information when starting a route in a navigation system.
- `RouteStateInfo` — Data class representing the current state of a navigation route.
- `RouteWithWaypointsRequest` — Data class representing a route request, including the destination and waypoints.
- `TBTInfo` — Data class representing Turn-By-Turn (TBT) navigation information.
- `TrafficCongestionInfo` — Data class representing traffic congestion information.
- `TrafficCongestionSummary` — Data class representing a summary of traffic congestion.
- `TrafficEventInfo` — Data class representing traffic information.
- `TrafficInfo` — Data class representing traffic congestion information.
- `TrafficStatus` — Data class representing the overall traffic status, including congestion and traffic events.
- `TripInfo` — Data class representing trip information.
- `TripRequest` — Data class representing a trip request.
- `TripSummary` — Data class representing driving route information.
- `WaypointArrivedInfo` — Data class representing information when arriving at a waypoint during navigation.
- `WaypointChangedInfo` — Data class representing information about a waypoint change in a navigation system.
- `WaypointInfo` — Data class representing information about a waypoint in a navigation system.

#### `ai.pleos.playground.navi.data.BookmarkInfo`
```kotlin
@Serializable data class BookmarkInfo(val latitude: Double, val longitude: Double, val displayLatitude: Double? = null, val displayLongitude: Double? = null, val name: String, val poiId: String, val poiSubId: String?, val address: String, val category: BookmarkType, val poiProvider: String = "")
```
Data class representing bookmark information for a specific location.

**Constructors**
- **BookmarkInfo**
  - `constructor(latitude: Double, longitude: Double, displayLatitude: Double? = null, displayLongitude: Double? = null, name: String, poiId: String, poiSubId: String?, address: String, category: BookmarkType, poiProvider: String = "")`

**Properties**
- **address** — The Address is the complete location information of a place (ex 123 Main St, Los Angeles, CA 90001, United States)
  - `val address: String`
- **category** — The category of the bookmark. @see BookmarkType
  - `val category: BookmarkType`
- **displayLatitude** — Displays the latitude coordinate on the map.
  - `val displayLatitude: Double? = null`
- **displayLongitude** — Displays the longitude coordinate on the map.
  - `val displayLongitude: Double? = null`
- **latitude** — Latitude is the geographic coordinate that specifies the north-south position of a point on Earth. It is measured in degrees (°), ranging from -90° (South) to 90° (North).
  - `val latitude: Double`
- **longitude** — Longitude is the geographic coordinate that specifies the east-west position of a point on Earth. It is measured in degrees (°), ranging from -180° (West) to 180° (East).
  - `val longitude: Double`
- **name** — The name of the bookmarked location.
  - `val name: String`
- **poiId** — POI ID (Point of Interest ID) is a unique identifier for a specific POI on a map
  - `val poiId: String`
- **poiProvider** — POI Content Provider Name (e.g., 42dot, Google)
  - `val poiProvider: String`
- **poiSubId** — POI Sub ID is a POI sequence, in case of the same POI, it is distinguished by sequence.
  - `val poiSubId: String?`

#### `ai.pleos.playground.navi.data.CameraAlert`
```kotlin
@Serializable data class CameraAlert(val id: String = "", val type: CameraType, val status: Boolean, val speedLimit: Int, val remainDistance: Int, val sectionAvrSpeed: Int, val isOverSpeed: Boolean = false, val isOverSectionAvrSpeed: Boolean = false)
```
Data class representing a camera alert, typically used in navigation systems.

**Constructors**
- **CameraAlert**
  - `constructor(id: String = "", type: CameraType, status: Boolean, speedLimit: Int, remainDistance: Int, sectionAvrSpeed: Int, isOverSpeed: Boolean = false, isOverSectionAvrSpeed: Boolean = false)`

**Properties**
- **id** — Camera ID
  - `val id: String`
- **isOverSectionAvrSpeed** — Indicates whether there is over speed on the average speed over a section.
  - `val isOverSectionAvrSpeed: Boolean = false`
- **isOverSpeed** — Indicates whether over speed has occurred
  - `val isOverSpeed: Boolean = false`
- **remainDistance** — The remaining distance (in meters) to the camera from the current location.
  - `val remainDistance: Int`
- **sectionAvrSpeed** — The average speed (km/h), within a speed enforcement section. CameraType.SPEED_CAMERA_SECTION only
  - `val sectionAvrSpeed: Int`
- **speedLimit** — The speed limit enforced by the camera. (km/h)
  - `val speedLimit: Int`
- **status** — Indicates whether the camera alert is active (true) or inactive (false).
  - `val status: Boolean`
- **type** — The type of camera. @see CameraType
  - `val type: CameraType`

#### `ai.pleos.playground.navi.data.CautionPoint`
```kotlin
@Serializable data class CautionPoint(val id: String, val type: CautionPointType, val remainDistance: Int)
```
Data class representing caution point alerts.

**Constructors**
- **CautionPoint**
  - `constructor(id: String, type: CautionPointType, remainDistance: Int)`

**Properties**
- **id** — Caution Point ID
  - `val id: String`
- **remainDistance** — Remaining distance to caution zone (in meters)
  - `val remainDistance: Int`
- **type** — The type of caution point (e.g., school zone, fog area).
  - `val type: CautionPointType`

#### `ai.pleos.playground.navi.data.ChargerOperator`
```kotlin
@Serializable data class ChargerOperator(val id: String, val name: String)
```
Data class representing an electric vehicle (EV) charger operator.

**Constructors**
- **ChargerOperator**
  - `constructor(id: String, name: String)`

**Properties**
- **id** — The unique identifier for the charger operator.
  - `val id: String`
- **name** — The name of the charger operator or service provider.
  - `val name: String`

#### `ai.pleos.playground.navi.data.ChargingPlan`
```kotlin
@Serializable data class ChargingPlan(val duration: Int = 0, val startSoc: Int = 0, val endSoc: Int = 0)
```
Data class representing the specific charging plan session at a route point.

**Constructors**
- **ChargingPlan**
  - `constructor(duration: Int = 0, startSoc: Int = 0, endSoc: Int = 0)`

**Properties**
- **duration** — The estimated charging time required at this location (seconds).
  - `val duration: Int = 0`
- **endSoc** — The target battery level to achieve after completing the charge (%).
  - `val endSoc: Int = 0`
- **startSoc** — The estimated battery level when starting the charge (%).
  - `val startSoc: Int = 0`

#### `ai.pleos.playground.navi.data.CurrentLocationInfo`
```kotlin
@Serializable data class CurrentLocationInfo(val latitude: Double, val longitude: Double, val heading: Double = 0.0, val address: String, val routeId: String, val currentRouteIndex: Int?)
```
Data class representing the current location information in a navigation system.

**Constructors**
- **CurrentLocationInfo**
  - `constructor(latitude: Double, longitude: Double, heading: Double = 0.0, address: String, routeId: String, currentRouteIndex: Int?)`

**Properties**
- **address** — The address of the current location.
  - `val address: String`
- **currentRouteIndex** — The current index in the route sequence (nullable).
  - `val currentRouteIndex: Int?`
- **heading** — The direction of travel.
  - `val heading: Double = 0.0`
- **latitude** — Latitude is the geographic coordinate that specifies the north-south position of a point on Earth. It is measured in degrees (°), ranging from -90° (South) to 90° (North).
  - `val latitude: Double`
- **longitude** — Longitude is the geographic coordinate that specifies the east-west position of a point on Earth. It is measured in degrees (°), ranging from -180° (West) to 180° (East).
  - `val longitude: Double`
- **routeId** — The unique identifier of the route being followed.
  - `val routeId: String`

#### `ai.pleos.playground.navi.data.DestinationArrivedInfo`
```kotlin
@Serializable data class DestinationArrivedInfo(val duration: Int, val distance: Int, val name: String, val address: String)
```
Data class representing information when the destination is reached in a navigation system.

**Constructors**
- **DestinationArrivedInfo**
  - `constructor(duration: Int, distance: Int, name: String, address: String)`

**Properties**
- **address** — The address of the destination.
  - `val address: String`
- **distance** — The total distance traveled in meters to the destination.
  - `val distance: Int`
- **duration** — The total travel time in seconds to reach the destination.
  - `val duration: Int`
- **name** — The name of the destination.
  - `val name: String`

#### `ai.pleos.playground.navi.data.DestinationInfo`
```kotlin
@Serializable data class DestinationInfo(val latitude: Double, val longitude: Double, val displayLatitude: Double? = null, val displayLongitude: Double? = null, val name: String, val address: String, val remainDistance: Int, val remainTime: Int, val tollFare: Int, val poiId: String, val poiSubId: String?, val poiProvider: String = "")
```
Data class representing destination information in a navigation system.

**Constructors**
- **DestinationInfo**
  - `constructor(latitude: Double, longitude: Double, displayLatitude: Double? = null, displayLongitude: Double? = null, name: String, address: String, remainDistance: Int, remainTime: Int, tollFare: Int, poiId: String, poiSubId: String?, poiProvider: String = "")`

**Properties**
- **address** — The address of the destination.
  - `val address: String`
- **displayLatitude** — Displays the latitude coordinate on the map.
  - `val displayLatitude: Double? = null`
- **displayLongitude** — Displays the longitude coordinate on the map.
  - `val displayLongitude: Double? = null`
- **latitude** — Latitude is the geographic coordinate that specifies the north-south position of a point on Earth. It is measured in degrees (°), ranging from -90° (South) to 90° (North).
  - `val latitude: Double`
- **longitude** — Longitude is the geographic coordinate that specifies the east-west position of a point on Earth. It is measured in degrees (°), ranging from -180° (West) to 180° (East).
  - `val longitude: Double`
- **name** — The name of the destination.
  - `val name: String`
- **poiId** — The unique identifier for POI.
  - `val poiId: String`
- **poiProvider** — POI Content Provider Name (e.g., 42dot, Google)
  - `val poiProvider: String`
- **poiSubId** — POI Sub ID is a POI sequence, in case of the same POI, it is distinguished by sequence.
  - `val poiSubId: String?`
- **remainDistance** — The remaining distance from the starting point to the waypoint (in meters).
  - `val remainDistance: Int`
- **remainTime** — The estimated remaining travel time to the waypoint (in seconds).
  - `val remainTime: Int`
- **tollFare** — The total toll fare required for the route to the destination (in won).
  - `val tollFare: Int`

#### `ai.pleos.playground.navi.data.DrivingInfo`
```kotlin
@Serializable data class DrivingInfo(val destination: DrivingState, val waypoints: List<DrivingState>)
```
Data class representing driving information, including the destination and waypoints.

**Constructors**
- **DrivingInfo**
  - `constructor(destination: DrivingState, waypoints: List<DrivingState>)`

**Properties**
- **destination** — The final destination state in the driving route
  - `val destination: DrivingState`
- **waypoints** — The state of the list of waypoints in the driving route
  - `val waypoints: List<DrivingState>`

#### `ai.pleos.playground.navi.data.DrivingState`
```kotlin
@Serializable data class DrivingState(val name: String, val duration: Int, val distance: Int, val remainDestinationChargingValue: Int, val isChargingStation: Boolean = false, val isRecommendedChargingStation: Boolean = false, val maxChargingPower: Int = 0, val targetSoc: Int = 0, val chargerCount: Int = 0, val chargingPlan: ChargingPlan? = null)
```
Data class representing the driving state in a route.

**Constructors**
- **DrivingState**
  - `constructor(name: String, duration: Int, distance: Int, remainDestinationChargingValue: Int, isChargingStation: Boolean = false, isRecommendedChargingStation: Boolean = false, maxChargingPower: Int = 0, targetSoc: Int = 0, chargerCount: Int = 0, chargingPlan: ChargingPlan? = null)`

**Properties**
- **chargerCount** — The total number of chargers available at the charging station.
  - `val chargerCount: Int = 0`
- **chargingPlan** — The specific charging plan session if charging is scheduled at this point. Returns null if no charging is planned. @see ChargingPlan
  - `val chargingPlan: ChargingPlan? = null`
- **distance** — The distance in meters to this point from the start.
  - `val distance: Int`
- **duration** — The estimated travel time in seconds from the current location to this target point (waypoint or destination).
  - `val duration: Int`
- **isChargingStation** — Indicates whether this point is a charging station.
  - `val isChargingStation: Boolean = false`
- **isRecommendedChargingStation** — Indicates whether this charging station was recommended by the navigation app. This value is meaningful only when isChargingStation is true. otherwise, it should be ignored.
  - `val isRecommendedChargingStation: Boolean = false`
- **maxChargingPower** — Indicates the maximum charging power of the charging station (kW).
  - `val maxChargingPower: Int = 0`
- **name** — The name of the location.
  - `val name: String`
- **remainDestinationChargingValue** — The estimated remaining battery or fuel level upon reaching this point.
  - `val remainDestinationChargingValue: Int`
- **targetSoc** — The required minimum battery level upon arrival at this point (%).
  - `val targetSoc: Int = 0`

#### `ai.pleos.playground.navi.data.NavigationScreenRequest`
```kotlin
@Serializable sealed class NavigationScreenRequest
```
Sealed class representing a request to switch the navigation screen.

**Inheritors**
- **Foreground**
- **FavoriteMain**
- **AddHomeFavorite**
- **AddWorkFavorite**
- **NearbyChargingStations**
- **Settings**
- **MoveToCurrentLocationOnMap**
- **OverviewRoute**
- **SearchPlace**
- **POIDetailPopup**
- **RecentDestinations**
- **RouteOptionList**
- **EditRoute**

**Types**
- **AddHomeFavorite** — Add to home favorites screen
  - `@Serializable @SerialName(value = "AddHomeFavorite") data object AddHomeFavorite : NavigationScreenRequest`
- **AddWorkFavorite** — Add to work favorites screen
  - `@Serializable @SerialName(value = "AddWorkFavorite") data object AddWorkFavorite : NavigationScreenRequest`
- **EditRoute** — Route editing screen
  - `@Serializable @SerialName(value = "EditRoute") data object EditRoute : NavigationScreenRequest`
- **FavoriteMain** — Main favorites screen
  - `@Serializable @SerialName(value = "FavoriteMain") data object FavoriteMain : NavigationScreenRequest`
- **Foreground** — Navigation Foreground
  - `@Serializable @SerialName(value = "Foreground") data object Foreground : NavigationScreenRequest`
- **MoveToCurrentLocationOnMap** — Current location screen on map
  - `@Serializable @SerialName(value = "MoveToCurrentLocationOnMap") data object MoveToCurrentLocationOnMap : NavigationScreenRequest`
- **NearbyChargingStations** — Nearby charging stations search screen
  - `@Serializable @SerialName(value = "NearbyChargingStations") data object NearbyChargingStations : NavigationScreenRequest`
- **OverviewRoute** — View entire route
  - `@Serializable @SerialName(value = "OverviewRoute") data class OverviewRoute(val info: RouteOverviewShowInfo) : NavigationScreenRequest`
- **POIDetailPopup** — POI details popup screen
  - `@Serializable @SerialName(value = "POIDetailPopup") data class POIDetailPopup(val poiId: String, val poiProvider: String = "") : NavigationScreenRequest`
- **RecentDestinations** — Recent destinations screen
  - `@Serializable @SerialName(value = "RecentDestinations") data object RecentDestinations : NavigationScreenRequest`
- **RouteOptionList** — Route option list screen
  - `@Serializable @SerialName(value = "RouteOptionList") data object RouteOptionList : NavigationScreenRequest`
- **SearchPlace** — Place search screen
  - `@Serializable @SerialName(value = "SearchPlace") data class SearchPlace(val keyword: String) : NavigationScreenRequest`
- **Settings** — Navigation settings screen
  - `@Serializable @SerialName(value = "Settings") data object Settings : NavigationScreenRequest`

##### nested `NavigationScreenRequest.AddHomeFavorite`
```kotlin
@Serializable @SerialName(value = "AddHomeFavorite") data object AddHomeFavorite : NavigationScreenRequest
```
Add to home favorites screen

##### nested `NavigationScreenRequest.AddWorkFavorite`
```kotlin
@Serializable @SerialName(value = "AddWorkFavorite") data object AddWorkFavorite : NavigationScreenRequest
```
Add to work favorites screen

##### nested `NavigationScreenRequest.EditRoute`
```kotlin
@Serializable @SerialName(value = "EditRoute") data object EditRoute : NavigationScreenRequest
```
Route editing screen

##### nested `NavigationScreenRequest.FavoriteMain`
```kotlin
@Serializable @SerialName(value = "FavoriteMain") data object FavoriteMain : NavigationScreenRequest
```
Main favorites screen

##### nested `NavigationScreenRequest.Foreground`
```kotlin
@Serializable @SerialName(value = "Foreground") data object Foreground : NavigationScreenRequest
```
Navigation Foreground

##### nested `NavigationScreenRequest.MoveToCurrentLocationOnMap`
```kotlin
@Serializable @SerialName(value = "MoveToCurrentLocationOnMap") data object MoveToCurrentLocationOnMap : NavigationScreenRequest
```
Current location screen on map

##### nested `NavigationScreenRequest.NearbyChargingStations`
```kotlin
@Serializable @SerialName(value = "NearbyChargingStations") data object NearbyChargingStations : NavigationScreenRequest
```
Nearby charging stations search screen

##### nested `NavigationScreenRequest.OverviewRoute`
```kotlin
@Serializable @SerialName(value = "OverviewRoute") data class OverviewRoute(val info: RouteOverviewShowInfo) : NavigationScreenRequest
```
View entire route
**Constructors**
- **OverviewRoute**
  - `constructor(info: RouteOverviewShowInfo)`
**Properties**
- **info** — Displays settings for a route overview.
  - `val info: RouteOverviewShowInfo`

##### nested `NavigationScreenRequest.POIDetailPopup`
```kotlin
@Serializable @SerialName(value = "POIDetailPopup") data class POIDetailPopup(val poiId: String, val poiProvider: String = "") : NavigationScreenRequest
```
POI details popup screen
**Constructors**
- **POIDetailPopup**
  - `constructor(poiId: String, poiProvider: String = "")`
**Properties**
- **poiId** — POI ID is a unique identifier for a specific POI on a map
  - `val poiId: String`
- **poiProvider** — POI Content Provider Name (e.g., 42dot, Google)
  - `val poiProvider: String`

##### nested `NavigationScreenRequest.RecentDestinations`
```kotlin
@Serializable @SerialName(value = "RecentDestinations") data object RecentDestinations : NavigationScreenRequest
```
Recent destinations screen

##### nested `NavigationScreenRequest.RouteOptionList`
```kotlin
@Serializable @SerialName(value = "RouteOptionList") data object RouteOptionList : NavigationScreenRequest
```
Route option list screen

##### nested `NavigationScreenRequest.SearchPlace`
```kotlin
@Serializable @SerialName(value = "SearchPlace") data class SearchPlace(val keyword: String) : NavigationScreenRequest
```
Place search screen
**Constructors**
- **SearchPlace**
  - `constructor(keyword: String)`
**Properties**
- **keyword** — Search keyword
  - `val keyword: String`

##### nested `NavigationScreenRequest.Settings`
```kotlin
@Serializable @SerialName(value = "Settings") data object Settings : NavigationScreenRequest
```
Navigation settings screen

#### `ai.pleos.playground.navi.data.RecentDestinationInfo`
```kotlin
@Serializable data class RecentDestinationInfo(val latitude: Double, val longitude: Double, val displayLatitude: Double? = null, val displayLongitude: Double? = null, val name: String, val address: String, val poiId: String, val poiSubId: String?, val poiProvider: String = "")
```
Data class representing information about a recently visited destination.

**Constructors**
- **RecentDestinationInfo**
  - `constructor(latitude: Double, longitude: Double, displayLatitude: Double? = null, displayLongitude: Double? = null, name: String, address: String, poiId: String, poiSubId: String?, poiProvider: String = "")`

**Properties**
- **address** — The Address is the complete location information of a place. (ex 123 Main St, Los Angeles, CA 90001, United States)
  - `val address: String`
- **displayLatitude** — Displays the latitude coordinate on the map.
  - `val displayLatitude: Double? = null`
- **displayLongitude** — Displays the longitude coordinate on the map.
  - `val displayLongitude: Double? = null`
- **latitude** — Latitude is the geographic coordinate that specifies the north-south position of a point on Earth. It is measured in degrees (°), ranging from -90° (South) to 90° (North).
  - `val latitude: Double`
- **longitude** — Longitude is the geographic coordinate that specifies the east-west position of a point on Earth. It is measured in degrees (°), ranging from -180° (West) to 180° (East).
  - `val longitude: Double`
- **name** — The name of the recent destination.
  - `val name: String`
- **poiId** — POI ID is a unique identifier for a specific POI on a map.
  - `val poiId: String`
- **poiProvider** — POI Content Provider Name (e.g., 42dot, Google)
  - `val poiProvider: String`
- **poiSubId** — POI Sub ID is a POI sequence, in case of the same POI, it is distinguished by sequence.
  - `val poiSubId: String?`

#### `ai.pleos.playground.navi.data.RequestWaypointInfo`
```kotlin
@Serializable data class RequestWaypointInfo(val latitude: Double, val longitude: Double, val poiName: String?, val waypointIndex: WaypointIndex, val poiId: String, val address: String, val poiSubId: String, val poiProvider: String = "")
```
Data class representing a waypoint request in a navigation system.

**Constructors**
- **RequestWaypointInfo**
  - `constructor(latitude: Double, longitude: Double, poiName: String?, waypointIndex: WaypointIndex, poiId: String, address: String, poiSubId: String, poiProvider: String = "")`

**Properties**
- **address** — The Address is the complete location information of a place (ex 123 Main St, Los Angeles, CA 90001, United States)
  - `val address: String`
- **latitude** — Latitude is the geographic coordinate that specifies the north-south position of a point on Earth. It is measured in degrees (°), ranging from -90° (South) to 90° (North).
  - `val latitude: Double`
- **longitude** — Longitude is the geographic coordinate that specifies the east-west position of a point on Earth. It is measured in degrees (°), ranging from -180° (West) to 180° (East).
  - `val longitude: Double`
- **poiId** — POI ID is a unique identifier for a specific POI on a map
  - `val poiId: String`
- **poiName** — The name of the POI at the waypoint.
  - `val poiName: String?`
- **poiProvider** — POI Content Provider Name (e.g., 42dot, Google)
  - `val poiProvider: String`
- **poiSubId** — POI Sub ID is a POI sequence, in case of the same POI, it is distinguished by sequence.
  - `val poiSubId: String`
- **waypointIndex** — The index of the waypoint in the route sequence. @see WaypointIndex
  - `val waypointIndex: WaypointIndex`

#### `ai.pleos.playground.navi.data.RouteInfo`
```kotlin
@Serializable data class RouteInfo(val latitude: Double, val longitude: Double, val displayLatitude: Double? = null, val displayLongitude: Double? = null, val poiName: String, val poiId: String, val address: String, val poiSubId: String, val routeOption: RouteOption = RouteOption.RECOMMENDED, val poiProvider: String = "", val isMobileRequest: Boolean = false)
```
Data class representing navigation route information.

**Constructors**
- **RouteInfo**
  - `constructor(latitude: Double, longitude: Double, displayLatitude: Double? = null, displayLongitude: Double? = null, poiName: String, poiId: String, address: String, poiSubId: String, routeOption: RouteOption = RouteOption.RECOMMENDED, poiProvider: String = "", isMobileRequest: Boolean = false)`

**Properties**
- **address** — The Address is the complete location information of a place (ex 123 Main St, Los Angeles, CA 90001, United States)
  - `val address: String`
- **displayLatitude** — Displays the latitude coordinate on the map.
  - `val displayLatitude: Double? = null`
- **displayLongitude** — Displays the longitude coordinate on the map.
  - `val displayLongitude: Double? = null`
- **isMobileRequest** — Whether the request is received from mobile (default: false)
  - `val isMobileRequest: Boolean = false`
- **latitude** — Latitude is the geographic coordinate that specifies the north-south position of a point on Earth. It is measured in degrees (°), ranging from -90° (South) to 90° (North).
  - `val latitude: Double`
- **longitude** — Longitude is the geographic coordinate that specifies the east-west position of a point on Earth. It is measured in degrees (°), ranging from -180° (West) to 180° (East).
  - `val longitude: Double`
- **poiId** — POI ID is a unique identifier for a specific POI on a map
  - `val poiId: String`
- **poiName** — The name of the POI.
  - `val poiName: String`
- **poiProvider** — POI Content Provider Name (e.g., 42dot, Google)
  - `val poiProvider: String`
- **poiSubId** — POI Sub ID is a POI sequence, in case of the same POI, it is distinguished by sequence.
  - `val poiSubId: String`
- **routeOption** — The routing option to be used (default: recommended route)
  - `val routeOption: RouteOption`

#### `ai.pleos.playground.navi.data.RouteOverviewShowInfo`
```kotlin
@Serializable data class RouteOverviewShowInfo(val durationMs: Int = 5000)
```
Data class representing the route overview display settings.

**Constructors**
- **RouteOverviewShowInfo**
  - `constructor(durationMs: Int = 5000)`

**Properties**
- **durationMs** — Shows the route overview duration in milliseconds (default: 5000ms).
  - `val durationMs: Int = 5000`

#### `ai.pleos.playground.navi.data.RoutePointInfo`
```kotlin
@Serializable data class RoutePointInfo(val latitude: Double, val longitude: Double, val poiName: String, val poiId: String, val ratioOnRoute: Double, val poiType: RoutePointPoiType, val pointType: RoutePointType, val settingType: RoutePointSettingType)
```
Data class representing destination information in a navigation system.

**Constructors**
- **RoutePointInfo**
  - `constructor(latitude: Double, longitude: Double, poiName: String, poiId: String, ratioOnRoute: Double, poiType: RoutePointPoiType, pointType: RoutePointType, settingType: RoutePointSettingType)`

**Properties**
- **latitude** — Latitude is the geographic coordinate that specifies the north-south position of a point on Earth. It is measured in degrees (°), ranging from -90° (South) to 90° (North).
  - `val latitude: Double`
- **longitude** — Longitude is the geographic coordinate that specifies the east-west position of a point on Earth. It is measured in degrees (°), ranging from -180° (West) to 180° (East).
  - `val longitude: Double`
- **poiId** — The unique identifier for POI.
  - `val poiId: String`
- **poiName** — The name of the POI.
  - `val poiName: String`
- **pointType** — Type of the route point. @see RoutePointType
  - `val pointType: RoutePointType`
- **poiType** — POI type of the route point. @see RoutePointPoiType
  - `val poiType: RoutePointPoiType`
- **ratioOnRoute** — Ratio along the route (0.0–100.0).
  - `val ratioOnRoute: Double`
- **settingType** — Setting type of the route point. @see RoutePointSettingType
  - `val settingType: RoutePointSettingType`

#### `ai.pleos.playground.navi.data.RouteStartInfo`
```kotlin
@Serializable data class RouteStartInfo(val duration: Int, val distance: Int, val tollFare: Int, val name: String, val remainDestinationChargingValue: Int)
```
Data class representing information when starting a route in a navigation system.

**Constructors**
- **RouteStartInfo**
  - `constructor(duration: Int, distance: Int, tollFare: Int, name: String, remainDestinationChargingValue: Int)`

**Properties**
- **distance** — The total distance of the route in meters.
  - `val distance: Int`
- **duration** — The estimated travel time for the route in seconds.
  - `val duration: Int`
- **name** — The name of the destination or starting point.
  - `val name: String`
- **remainDestinationChargingValue** — The estimated remaining battery or fuel level upon reaching the destination.
  - `val remainDestinationChargingValue: Int`
- **tollFare** — The total toll fare required for the route.
  - `val tollFare: Int`

#### `ai.pleos.playground.navi.data.RouteStateInfo`
```kotlin
@Serializable data class RouteStateInfo(val termsAgreement: Boolean, val naviMode: NaviMode, val isRouting: Boolean, val routeOption: RouteOption, val remainDestinationChargingInt: Int, val remainWaypointChargingInt: List<Int>, val trafficStatus: TrafficStatus?)
```
Data class representing the current state of a navigation route.

**Constructors**
- **RouteStateInfo**
  - `constructor(termsAgreement: Boolean, naviMode: NaviMode, isRouting: Boolean, routeOption: RouteOption, remainDestinationChargingInt: Int, remainWaypointChargingInt: List<Int>, trafficStatus: TrafficStatus?)`

**Properties**
- **isRouting** — Indicates whether a route is currently active.
  - `val isRouting: Boolean`
- **naviMode** — The current navigation mode. @See NaviMode
  - `val naviMode: NaviMode`
- **remainDestinationChargingInt** — The estimated remaining battery or fuel level upon reaching the destination.
  - `val remainDestinationChargingInt: Int`
- **remainWaypointChargingInt** — A list of estimated remaining battery or fuel levels at each waypoint.
  - `val remainWaypointChargingInt: List<Int>`
- **routeOption** — The selected routing option for the navigation. @See RouteOption
  - `val routeOption: RouteOption`
- **termsAgreement** — Indicates whether the user has agreed to the navigation terms.
  - `val termsAgreement: Boolean`
- **trafficStatus** — Current traffic status along the route (nullable). @see TrafficStatus
  - `val trafficStatus: TrafficStatus?`

#### `ai.pleos.playground.navi.data.RouteWithWaypointsRequest`
```kotlin
@Serializable data class RouteWithWaypointsRequest(val destination: RouteInfo, val waypoints: List<RequestWaypointInfo>)
```
Data class representing a route request, including the destination and waypoints.

**Constructors**
- **RouteWithWaypointsRequest**
  - `constructor(destination: RouteInfo, waypoints: List<RequestWaypointInfo>)`

**Properties**
- **destination** — The final destination information in the requested route. @see RouteInfo
  - `val destination: RouteInfo`
- **waypoints** — The list of waypoint information in the requested route. @see RequestWaypointInfo
  - `val waypoints: List<RequestWaypointInfo>`

#### `ai.pleos.playground.navi.data.TBTInfo`
```kotlin
@Serializable data class TBTInfo(val type: TurnType, val distance: Double, val description: String)
```
Data class representing Turn-By-Turn (TBT) navigation information.

**Constructors**
- **TBTInfo**
  - `constructor(type: TurnType, distance: Double, description: String)`

**Properties**
- **description** — A text description of the turn instruction.
  - `val description: String`
- **distance** — Distance to the upcoming turn (in meters)
  - `val distance: Double`
- **type** — The type of turn instruction (e.g., left turn, right turn).
  - `val type: TurnType`

#### `ai.pleos.playground.navi.data.TrafficCongestionInfo`
```kotlin
@Serializable data class TrafficCongestionInfo(val offset: Double, val distance: Double, val trafficCongestionTime: Long, val trafficCongestionType: TrafficCongestionType, val avrSpeed: Double, val roadName: String, val startCrossName: String, val endCrossName: String)
```
Data class representing traffic congestion information.

**Constructors**
- **TrafficCongestionInfo**
  - `constructor(offset: Double, distance: Double, trafficCongestionTime: Long, trafficCongestionType: TrafficCongestionType, avrSpeed: Double, roadName: String, startCrossName: String, endCrossName: String)`

**Properties**
- **avrSpeed** — The average speed within the traffic congestion (in km/h).
  - `val avrSpeed: Double`
- **distance** — The length of the traffic congestion (in meters).
  - `val distance: Double`
- **endCrossName** — The name of the nearby intersection at the end of the congestion section.
  - `val endCrossName: String`
- **offset** — The distance from the current location to the start of the congested area (meters).
  - `val offset: Double`
- **roadName** — Represents the main congested road name.
  - `val roadName: String`
- **startCrossName** — The name of the nearby intersection at the start of the congestion section.
  - `val startCrossName: String`
- **trafficCongestionTime** — The duration of traffic congestion (in seconds).
  - `val trafficCongestionTime: Long`
- **trafficCongestionType** — The type of traffic congestion. @see TrafficCongestionType
  - `val trafficCongestionType: TrafficCongestionType`

#### `ai.pleos.playground.navi.data.TrafficCongestionSummary`
```kotlin
@Serializable data class TrafficCongestionSummary(val totalCongestionDist: Double, val totalCongestionTime: Long, val totalCongestionAvrSpeed: Double)
```
Data class representing a summary of traffic congestion.

**Constructors**
- **TrafficCongestionSummary**
  - `constructor(totalCongestionDist: Double, totalCongestionTime: Long, totalCongestionAvrSpeed: Double)`

**Properties**
- **totalCongestionAvrSpeed** — The average speed across all congested areas (in km/h).
  - `val totalCongestionAvrSpeed: Double`
- **totalCongestionDist** — The total distance of all congested areas (in meters).
  - `val totalCongestionDist: Double`
- **totalCongestionTime** — The total duration of traffic congestion (in seconds).
  - `val totalCongestionTime: Long`

#### `ai.pleos.playground.navi.data.TrafficEventInfo`
```kotlin
@Serializable data class TrafficEventInfo(val latitude: Double, val longitude: Double, val eventType: TrafficEventType, val startTime: String, val endTime: String, val description: String)
```
Data class representing traffic information.

**Constructors**
- **TrafficEventInfo**
  - `constructor(latitude: Double, longitude: Double, eventType: TrafficEventType, startTime: String, endTime: String, description: String)`

**Properties**
- **description** — A brief description of the traffic event.
  - `val description: String`
- **endTime** — The end time of the traffic event (yyyy-MM-dd hh:mm "2023-11-05T22:08:28")
  - `val endTime: String`
- **eventType** — The type of traffic event (e.g., accident, roadwork, closure).
  - `val eventType: TrafficEventType`
- **latitude** — Latitude is the geographic coordinate that specifies the north-south position of a point on Earth. It is measured in degrees (°), ranging from -90° (South) to 90° (North).
  - `val latitude: Double`
- **longitude** — Longitude is the geographic coordinate that specifies the east-west position of a point on Earth. It is measured in degrees (°), ranging from -180° (West) to 180° (East).
  - `val longitude: Double`
- **startTime** — The start time of the traffic event (yyyy-MM-dd hh:mm "2023-11-05T22:08:28")
  - `val startTime: String`

#### `ai.pleos.playground.navi.data.TrafficInfo`
```kotlin
@Serializable data class TrafficInfo(val congestionType: TrafficCongestionType, val startRatio: Int, val endRatio: Int)
```
Data class representing traffic congestion information.

**Constructors**
- **TrafficInfo**
  - `constructor(congestionType: TrafficCongestionType, startRatio: Int, endRatio: Int)`

**Properties**
- **congestionType** — The type of traffic congestion. @see TrafficCongestionType
  - `val congestionType: TrafficCongestionType`
- **endRatio** — End ratio along the route (0–100).
  - `val endRatio: Int`
- **startRatio** — Start ratio along the route (0–100).
  - `val startRatio: Int`

#### `ai.pleos.playground.navi.data.TrafficStatus`
```kotlin
@Serializable data class TrafficStatus(val summary: TrafficCongestionSummary, val congestionItemList: List<TrafficCongestionInfo>, val trafficEventList: List<TrafficEventInfo>)
```
Data class representing the overall traffic status, including congestion and traffic events.

**Constructors**
- **TrafficStatus**
  - `constructor(summary: TrafficCongestionSummary, congestionItemList: List<TrafficCongestionInfo>, trafficEventList: List<TrafficEventInfo>)`

**Properties**
- **congestionItemList** — A list of detailed traffic congestion sections. @see TrafficCongestionInfo
  - `val congestionItemList: List<TrafficCongestionInfo>`
- **summary** — A summary of the total traffic congestion details. @see TrafficCongestionSummary
  - `val summary: TrafficCongestionSummary`
- **trafficEventList** — A list of traffic events such as accidents or road closures. @see TrafficEventInfo
  - `val trafficEventList: List<TrafficEventInfo>`

#### `ai.pleos.playground.navi.data.TripInfo`
```kotlin
@Serializable data class TripInfo(val distance: Int, val eta: Int, val estimatedSocAtDest: Int, val energyConsumption: Double, val summary: TripSummary)
```
Data class representing trip information.

**Constructors**
- **TripInfo**
  - `constructor(distance: Int, eta: Int, estimatedSocAtDest: Int, energyConsumption: Double, summary: TripSummary)`

**Properties**
- **distance** — The total distance traveled in meters to the destination.
  - `val distance: Int`
- **energyConsumption** — Estimated battery consumption after route driving (Wh)
  - `val energyConsumption: Double`
- **estimatedSocAtDest** — Estimated remaining battery after route driving (%)
  - `val estimatedSocAtDest: Int`
- **eta** — Estimated Time of Arrival (ETA) in seconds.
  - `val eta: Int`
- **summary** — Driving route information. @see TripSummary
  - `val summary: TripSummary`

#### `ai.pleos.playground.navi.data.TripRequest`
```kotlin
@Serializable data class TripRequest(val latitude: Double, val longitude: Double, val poiId: String, val poiName: String, val poiSubId: String?)
```
Data class representing a trip request.

**Constructors**
- **TripRequest**
  - `constructor(latitude: Double, longitude: Double, poiId: String, poiName: String, poiSubId: String?)`

**Properties**
- **latitude** — Latitude is the geographic coordinate that specifies the north-south position of a point on Earth. It is measured in degrees (°), ranging from -90° (South) to 90° (North).
  - `val latitude: Double`
- **longitude** — Longitude is the geographic coordinate that specifies the east-west position of a point on Earth. It is measured in degrees (°), ranging from -180° (West) to 180° (East).
  - `val longitude: Double`
- **poiId** — POI ID is a unique identifier for a specific POI on a map.
  - `val poiId: String`
- **poiName** — The name of the POI.
  - `val poiName: String`
- **poiSubId** — POI Sub ID is a POI sequence, in case of the same POI, it is distinguished by sequence.
  - `val poiSubId: String?`

#### `ai.pleos.playground.navi.data.TripSummary`
```kotlin
@Serializable data class TripSummary(val trafficConditions: List<TrafficInfo>, val routePoints: List<RoutePointInfo>)
```
Data class representing driving route information.

**Constructors**
- **TripSummary**
  - `constructor(trafficConditions: List<TrafficInfo>, routePoints: List<RoutePointInfo>)`

**Properties**
- **routePoints** — List of waypoint and destination information.
  - `val routePoints: List<RoutePointInfo>`
- **trafficConditions** — List of traffic congestion information.
  - `val trafficConditions: List<TrafficInfo>`

#### `ai.pleos.playground.navi.data.WaypointArrivedInfo`
```kotlin
@Serializable data class WaypointArrivedInfo(val duration: Int, val distance: Int, val index: WaypointIndex, val name: String, val address: String)
```
Data class representing information when arriving at a waypoint during navigation.

**Constructors**
- **WaypointArrivedInfo**
  - `constructor(duration: Int, distance: Int, index: WaypointIndex, name: String, address: String)`

**Properties**
- **address** — The address of the waypoint.
  - `val address: String`
- **distance** — The total distance traveled in meters to the waypoint.
  - `val distance: Int`
- **duration** — The total travel time in seconds to reach the waypoint.
  - `val duration: Int`
- **index**
  - `val index: WaypointIndex`
- **name** — The name of the waypoint.
  - `val name: String`

#### `ai.pleos.playground.navi.data.WaypointChangedInfo`
```kotlin
@Serializable data class WaypointChangedInfo(val index: WaypointIndex, val cmdType: WaypointCmdType, val routeStartInfo: RouteStartInfo)
```
Data class representing information about a waypoint change in a navigation system.

**Constructors**
- **WaypointChangedInfo**
  - `constructor(index: WaypointIndex, cmdType: WaypointCmdType, routeStartInfo: RouteStartInfo)`

**Properties**
- **cmdType** — Type of waypoint change command. @See WaypointCmdType
  - `val cmdType: WaypointCmdType`
- **index** — Index of the waypoint in the route sequence. @See WaypointIndex
  - `val index: WaypointIndex`
- **routeStartInfo** — Updated route information after waypoint change. @See RouteStartInfo
  - `val routeStartInfo: RouteStartInfo`

#### `ai.pleos.playground.navi.data.WaypointInfo`
```kotlin
@Serializable data class WaypointInfo(val name: String, val address: String, val latitude: Double, val longitude: Double, val displayLatitude: Double? = null, val displayLongitude: Double? = null, val remainDistance: Int, val remainTime: Int, val poiId: String, val poiSubId: String?, val poiProvider: String = "")
```
Data class representing information about a waypoint in a navigation system.

**Constructors**
- **WaypointInfo**
  - `constructor(name: String, address: String, latitude: Double, longitude: Double, displayLatitude: Double? = null, displayLongitude: Double? = null, remainDistance: Int, remainTime: Int, poiId: String, poiSubId: String?, poiProvider: String = "")`

**Properties**
- **address** — The waypoint address.
  - `val address: String`
- **displayLatitude** — Displays the latitude coordinate on the map.
  - `val displayLatitude: Double? = null`
- **displayLongitude** — Displays the longitude coordinate on the map.
  - `val displayLongitude: Double? = null`
- **latitude** — Latitude is the geographic coordinate that specifies the north-south position of a point on Earth. It is measured in degrees (°), ranging from -90° (South) to 90° (North).
  - `val latitude: Double`
- **longitude** — Longitude is the geographic coordinate that specifies the east-west position of a point on Earth. It is measured in degrees (°), ranging from -180° (West) to 180° (East).
  - `val longitude: Double`
- **name** — The waypoint name.
  - `val name: String`
- **poiId** — The unique identifier for POI.
  - `val poiId: String`
- **poiProvider** — POI Content Provider Name (e.g., 42dot, Google)
  - `val poiProvider: String`
- **poiSubId** — POI Sub ID is a POI sequence, in case of the same POI, it is distinguished by sequence.
  - `val poiSubId: String?`
- **remainDistance** — The remaining distance from the starting point to the waypoint (in meters).
  - `val remainDistance: Int`
- **remainTime** — The estimated remaining travel time to the waypoint (in seconds).
  - `val remainTime: Int`

### package `ai.pleos.playground.navi.helper`

- `NaviHelper` — The entry point of the NaviHelper SDK. Create a NaviHelper instance with an Android Context to call NaviHelper APIs directly and register event-listener callbacks.

#### `ai.pleos.playground.navi.helper.NaviHelper`
```kotlin
class NaviHelper(context: Context)
```
The entry point of the NaviHelper SDK. Create a NaviHelper instance with an Android Context to call NaviHelper APIs directly and register event-listener callbacks.

**Constructors**
- **NaviHelper**
  - `constructor(context: Context)`

**Functions**
- **addListener** — Adds a listener. Registers a listener for NaviHelper callback function
  - `fun addListener(listener: NaviHelperEventListener)`
- **addWaypoint** — Adds waypoint. `[perm: NAVI_ROUTE_SEARCH]`
  - `fun addWaypoint(requestWaypointInfo: RequestWaypointInfo)`
  - 상세: Adds waypoint. Related events such as onWayPointInfo may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **cancelRoute** — Cancels route. `[perm: NAVI_ROUTE_SEARCH]`
  - `fun cancelRoute()`
  - 상세: Cancels route. Related events such as onRouteCancelled may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **changeRouteOption** — Changes route option. `[perm: NAVI_ROUTE_SEARCH]`
  - `fun changeRouteOption(routeOption: RouteOption)`
  - 상세: Changes route option. Related events such as onRouteOptionChanged may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **getBookmarkInfo** — Gets bookmark info. `[perm: NAVI_ROUTE]`
  - `fun getBookmarkInfo()`
  - 상세: Gets bookmark info. Related events such as onBookmarkInfo may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **getChargerOperatorInfo** — Gets Charger Operator info. `[perm: NAVI_ROUTE]`
  - `fun getChargerOperatorInfo()`
  - 상세: Gets Charger Operator info. Related events such as onChargerOperatorInfo may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app implemented with NaviProvider handles and responds to this request. This method is protected by permissions.
- **getCurrentLocationInfo** — Gets current location info. `[perm: NAVI_ROUTE]`
  - `fun getCurrentLocationInfo()`
  - 상세: Gets current location info. Related events such as onCurrentLocationInfo may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **getDestinationInfo** — Gets destination info. `[perm: NAVI_ROUTE]`
  - `fun getDestinationInfo()`
  - 상세: Gets destination info. Related events such as onDestinationInfo may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **getRecentDestinationInfo** — Gets recent destination info. `[perm: NAVI_ROUTE]`
  - `fun getRecentDestinationInfo()`
  - 상세: Gets recent destination info. Related events such as onRecentDestinationInfo may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **getRouteStateInfo** — Gets route state info. `[perm: NAVI_ROUTE]`
  - `fun getRouteStateInfo()`
  - 상세: Gets route state info. Related events such as onRouteStateInfo may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **getTBTInfo** — Gets TBT info. `[perm: NAVI_ROUTE]`
  - `fun getTBTInfo()`
  - 상세: Gets TBT info. Related events such as onTBTInfo may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **getWaypointInfo** — Gets waypoint info. `[perm: NAVI_ROUTE]`
  - `fun getWaypointInfo()`
  - 상세: Gets waypoint info. Related events such as onWayPointInfo may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **initialize** — Initializes NaviHelper SDK. When the app starts or is created, it should call the initialization of SDK. / Initializes NaviHelper SDK with connection callbacks. Success is called when the first service connection is established. Failure is called when the initial connection retries are exhausted.
  - `fun initialize()`
  - `fun initialize(onConnected: () -> Unit, onFailed: (Throwable) -> Unit)`
  - 상세: Initializes NaviHelper SDK. When the app starts or is created, it should call the initialization of SDK. SIG: fun initialize(onConnected: () -> Unit, onFailed: (Throwable) -> Unit) Initializes NaviHelper SDK with connection callbacks. Success is called when the first service connection is established. Failure is called when the initial connection retries are exhausted.
- **release** — Releases NaviHelper SDK. When the app terminates or is destroyed, it should call the release of SDK
  - `fun release()`
- **removeListener** — Removes a listener.
  - `fun removeListener(listener: NaviHelperEventListener)`
- **removeWaypoint** — Removes waypoint. `[perm: NAVI_ROUTE_SEARCH]`
  - `fun removeWaypoint(waypointIndex: WaypointIndex)`
  - 상세: Removes waypoint. Related events such as onWaypointChanged may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **requestNavigationScreen** — Requests navigation screen. `[perm: NAVI_CUSTOM_MAP]`
  - `fun requestNavigationScreen(requestScreen: NavigationScreenRequest)`
  - 상세: Requests navigation screen. This method is protected by permissions
- **requestReRoute** — Requests re-route. `[perm: NAVI_ROUTE_SEARCH]`
  - `fun requestReRoute()`
  - 상세: Requests re-route. Related events such as onReRouteEnded may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **requestRoute** — Requests route. `[perm: NAVI_ROUTE_SEARCH]`
  - `fun requestRoute(routeInfo: RouteInfo)`
  - 상세: Requests route. Related events such as onRouteStarted may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **requestRouteWithWaypoints** — Requests a route with waypoints. `[perm: NAVI_ROUTE_SEARCH]`
  - `fun requestRouteWithWaypoints(request: RouteWithWaypointsRequest)`
  - 상세: Requests a route with waypoints. Related events such as onRouteStarted may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app, implemented with the NaviProvider SDK, handles and responds to this request. This method is protected by permissions.
- **requestTrip** — Requests trip. `[perm: NAVI_ROUTE]`
  - `fun requestTrip(tripRequest: TripRequest)`
  - 상세: Requests trip. Related events such as onTripInfo may be delivered to the registered listener. The actual events delivered may vary depending on how the Navi app implemented with NaviProvider handles and responds to this request. This method is protected by permissions.

### package `ai.pleos.playground.navi.helper.listener`

- `NaviHelperEventListener` — Interface definition for callback data received from the Navi application.

#### `ai.pleos.playground.navi.helper.listener.NaviHelperEventListener`
```kotlin
interface NaviHelperEventListener
```
Interface definition for callback data received from the Navi application.

**Functions**
- **onBookMarkChanged** — Called when arriving at or removing the bookmark.
  - `fun onBookMarkChanged()`
- **onBookmarkInfo** — Called when the bookmark information is changed.
  - `fun onBookmarkInfo(bookmarkInfoList: List<BookmarkInfo>)`
- **onCameraAlert** — Called when camera alerts are notified. Received when speeding camera exists.
  - `fun onCameraAlert(cameraAlertList: List<CameraAlert>)`
- **onCautionPointInfo** — Called when caution point information is notified.
  - `fun onCautionPointInfo(cautionPointList: List<CautionPoint>)`
- **onChargerOperatorInfo** — Called when charger operator information is notified.
  - `fun onChargerOperatorInfo(chargerOperatorList: List<ChargerOperator>)`
- **onCurrentLocationInfo** — Called when the current location information is changed.
  - `fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo)`
- **onCurrentRoadSpeedLimit** — Called when the road speed limit is notified. Received every second after the event listener registration.
  - `fun onCurrentRoadSpeedLimit(speedLimit: Int)`
- **onDestinationArrived** — Called when arriving at the destination.
  - `fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo)`
- **onDestinationChanged** — Called when arriving at or removing the destination.
  - `fun onDestinationChanged()`
- **onDestinationInfo** — Called when the destination information is changed.
  - `fun onDestinationInfo(destinationInfo: DestinationInfo)`
- **onDrivingInfo** — Called when driving information is notified.
  - `fun onDrivingInfo(drivingInfo: DrivingInfo)`
- **onError** — Called when a navigation error occurs.
  - `fun onError(errorCode: NaviErrorCode)`
- **onNavigationStatus** — Called when the navigation status is notified. Received whenever the driving status changes
  - `fun onNavigationStatus(status: NavigationStatus)`
- **onRecentDestinationInfo** — Called when the recent destination information is changed.
  - `fun onRecentDestinationInfo(recentDestinationInfoList: List<RecentDestinationInfo>)`
- **onReRouteEnded** — Called when navigation re-route is completed.
  - `fun onReRouteEnded()`
- **onRouteCancelled** — Called when the current route is canceled.
  - `fun onRouteCancelled()`
- **onRouteOptionChanged** — Called when the route type is changed.
  - `fun onRouteOptionChanged(routeOption: RouteOption)`
- **onRouteStarted** — Called when navigation route search begins.
  - `fun onRouteStarted(routeStartInfo: RouteStartInfo)`
- **onRouteStateInfo** — Called when the route state information is changed.
  - `fun onRouteStateInfo(routeStateInfo: RouteStateInfo)`
- **onRoutingCompleted** — Called when navigation route search is completed.
  - `fun onRoutingCompleted()`
- **onTBTInfo** — Called when the TBT information is notified.
  - `fun onTBTInfo(tbtInfoList: List<TBTInfo>)`
- **onTripInfo** — Called when trip information is notified.
  - `fun onTripInfo(tripInfo: TripInfo)`
- **onWayPointArrived** — Called when arriving at the waypoint.
  - `fun onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo)`
- **onWaypointChanged** — Called when arriving at or removing the waypoint.
  - `fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo)`
- **onWayPointInfo** — Called when the waypoint information is changed.
  - `fun onWayPointInfo(waypointInfoList: List<WaypointInfo>)`

---

## TextToSpeech SDK


### package `ai.pleos.playground.tts`

- `TextToSpeech` — Provides access to the TextToSpeech service. This SDK allows access to the TextToSpeech.

#### `ai.pleos.playground.tts.TextToSpeech`
```kotlin
class TextToSpeech(context: Context, mode: Mode = Mode.HYBRID)
```
Provides access to the TextToSpeech service. This SDK allows access to the TextToSpeech. Important: the caller MUST invoke release() when it is no longer needed and the application is destroyed. Streams audio to remote servers or on-device engines to perform TextToSpeech. Please note that the application must register your application in Pleos Playground to use the server-oriented speech synthesizer.

**Parameters**
- **context** — Application context
- **mode** — The user can set the mode that they want. We support three modes. HYBRID, SERVER, and ON_DEVICE. If the user doesn't select anything, HYBRID will be set automatically.

**See also**
- **Mode**

**Constructors**
- **TextToSpeech**
  - `constructor(context: Context, mode: Mode = Mode.HYBRID)`

**Types**
- **Companion**
  - `object Companion`

**Functions**
- **addEventListener** — Adds a listener that will be notified of various events related to TextToSpeech `[perm: TTS_SERVICE]`
  - `fun addEventListener(listener: EventListener)`
- **initialize** — Initializes TextToSpeech SDK When the app starts or is created, it should call the initialization of SDK / Initializes TextToSpeech SDK with a listener for initialization result. When the app starts or is created, it should call the initialization of SDK. `[perm: TTS_SERVICE]`
  - `fun initialize()`
  - `fun initialize(listener: OnInitializeListener)`
  - 상세: Initializes TextToSpeech SDK When the app starts or is created, it should call the initialization of SDK SIG: @RequiresPermission(value = "pleos.car.permission.TTS_SERVICE") fun initialize(listener: OnInitializeListener) Initializes TextToSpeech SDK with a listener for initialization result. When the app starts or is created, it should call the initialization of SDK.
- **registerApp** — Register app `[perm: TTS_SERVICE]`
  - `fun registerApp(clientID: String, clientSecret: String, listener: OnServerConnectionListener)`
- **release** — Releases TextToSpeech SDK When the app terminates or is destroyed, it should call the release of SDK `[perm: TTS_SERVICE]`
  - `fun release()`
- **removeEventListener** — Removes a previously registered listener `[perm: TTS_SERVICE]`
  - `fun removeEventListener(listener: EventListener)`
- **setAudioAttributes** — Sets the audio attributes to be used for TextToSpeech SDK `[perm: TTS_SERVICE]`
  - `fun setAudioAttributes(audioAttributes: AudioAttributes)`
- **setAudioZoneId** — Update audio zone id that user want to set. Requires the caller to have TTS_PERMISSION. `[perm: TTS_SERVICE]`
  - `fun setAudioZoneId(zoneId: Int)`
- **setLocale** — Updates the locale that the user wants to set. Requires the caller to have TTS_PERMISSION.
  - `fun setLocale(locale: Locale)`
- **setSpeakerType** — Updates the speaker type (voice) used for speech synthesis. Requires the caller to have TTS_PERMISSION. If this method is not called, the speaker previously saved for the app is used. If no speaker has been saved for the app, the Gleo AI speaker setting is used. If neither is set, haeun is used.
  - `fun setSpeakerType(type: String)`
- **speak** — Converts Text-To-Speech and speaks the text `[perm: TTS_SERVICE]`
  - `fun speak(text: String, isFinal: Boolean = true)`
- **stop** — Interrupts the current TextToSpeech playback `[perm: TTS_SERVICE]`
  - `fun stop()`

##### nested `TextToSpeech.Companion`
```kotlin
object Companion
```
**Properties**
- **version**
  - `val version: String`

### package `ai.pleos.playground.tts.constant`

- `ErrorType` — Represents the types of errors that can occur within the system.
- `Mode`

#### `ai.pleos.playground.tts.constant.ErrorType`
```kotlin
enum ErrorType : Enum<ErrorType>
```
Represents the types of errors that can occur within the system.

**Entries**
- **AUDIO_ERROR** — Audio playback/output related error.
- **MIC_ERROR** — Microphone input/recording related error.
- **SERVER_INTERNAL_ERROR** — Server-side processing failure (5xx, gRPC internal issues).
- **NETWORK_ERROR** — Network connectivity failure.
- **INVALID_REQUEST_ERROR** — Client-side invalid request.
- **TIMEOUT** — Request/response timeout.
- **UNAUTHENTICATED** — Authentication failure (invalid/expired token).
- **PERMISSION_DENIED** — Permission denied or insufficient access rights.
- **ON_DEVICE_ENGINE_ERROR** — On-device engine (e.g. Speech SDK) failure.
- **UNKNOWN** — Unknown error type.
- **UNAVAILABLE_LANGUAGE** — Requested language is not available.
- **NOT_SUPPORTED_API** — Requested API is not supported.

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<ErrorType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): ErrorType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<ErrorType>`

#### `ai.pleos.playground.tts.constant.Mode`
```kotlin
enum Mode : Enum<Mode>
```

**Entries**
- **SERVER** — Server-oriented Streaming TextToSpeech will be running. The application gets an error event if the network connection fails.
- **ON_DEVICE** — OnDevice-oriented Streaming TextToSpeech will be running. The application doesn't need to be registered to Pleos Playground if it only uses ON_DEVICE mode.
- **HYBRID** — Server-oriented Streaming TextToSpeech will be running. In case of network failure, SDK will stream TextToSpeech audio to the on-device engine automatically.

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<Mode>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): Mode`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<Mode>`

### package `ai.pleos.playground.tts.listener`

- `EventListener` — Listener for events relating to the progress of Text-To-Speech. Adds and removes with addEventListener/removeEventListener APIs respectively.
- `OnInitializeListener` — Listener for the result of TextToSpeech SDK initialization.
- `OnServerConnectionListener` — Server connection result listener

#### `ai.pleos.playground.tts.listener.EventListener`
```kotlin
interface EventListener
```
Listener for events relating to the progress of Text-To-Speech. Adds and removes with addEventListener/removeEventListener APIs respectively.

**Functions**
- **onDone** — Called when TextToSpeech has successfully completed processing.
  - `fun onDone()`
- **onError** — Called when an error has occurred during processing.
  - `fun onError(errorType: ErrorType)`
- **onReady** — Called when TextToSpeech speak has been called.
  - `fun onReady()`
- **onStart** — Called when TextToSpeech starts as perceived by the caller.
  - `fun onStart()`
- **onStop** — Called when TextToSpeech stops while in progress.
  - `fun onStop()`
- **onUpdatedRms** — Called when RMS is updated.
  - `fun onUpdatedRms(rms: Double)`

#### `ai.pleos.playground.tts.listener.OnInitializeListener`
```kotlin
interface OnInitializeListener
```
Listener for the result of TextToSpeech SDK initialization.

**See also**
- **TextToSpeech.initialize**

**Functions**
- **onFailure** — Called when the SDK initialization has failed.
  - `fun onFailure(errorType: ErrorType)`
- **onSuccess** — Called when the SDK has been successfully initialized and bound to the TTS service.
  - `fun onSuccess()`

#### `ai.pleos.playground.tts.listener.OnServerConnectionListener`
```kotlin
interface OnServerConnectionListener
```
Server connection result listener

**Functions**
- **onConnected** — Called when connected to the server
  - `fun onConnected()`
- **onFailed** — Called when failure to connection to server
  - `fun onFailed(errorType: ErrorType)`

---

## SpeechToText SDK


### package `ai.pleos.playground.stt`

- `SpeechToText` — Provides access to the SpeechToText service. This SDK allows access to the SpeechToText.

#### `ai.pleos.playground.stt.SpeechToText`
```kotlin
class SpeechToText(context: Context, mode: Mode = Mode.HYBRID)
```
Provides access to the SpeechToText service. This SDK allows access to the SpeechToText. Important: the caller MUST invoke release() when it is no longer needed and the application is destroyed. Streams audio to remote servers or on-device engines to perform SpeechToText. Please note that the application must be registered in Pleos Playground to use the server-oriented speech recognizer.

**Parameters**
- **context** — Application context
- **mode** — The user can set the mode that they want. We support three modes. HYBRID, SERVER, and ON_DEVICE. If the user doesn't select anything, HYBRID will be set automatically.

**See also**
- **Mode**

**Constructors**
- **SpeechToText**
  - `constructor(context: Context, mode: Mode = Mode.HYBRID)`

**Types**
- **Companion**
  - `object Companion`

**Functions**
- **addListener** — Adds a listener that will be notified of various events related to SpeechToText Requires the caller to have STT_PERMISSION. `[perm: STT_SERVICE]`
  - `fun addListener(listener: ResultListener)`
- **completeAudioSend** — Completes the external audio buffer input started by sendAudio and finishes recognition. Available only in Mode.ON_DEVICE. In other modes the call is ignored. Requires the caller to have STT_PERMISSION. `[perm: STT_SERVICE]`
  - `fun completeAudioSend()`
- **initialize** — Initializes SDK to use SpeechToText whether the application is created or not. Requires the caller to have STT_PERMISSION. / Initializes SDK to use SpeechToText with a listener for initialization result. Requires the caller to have STT_PERMISSION. `[perm: STT_SERVICE]`
  - `fun initialize()`
  - `fun initialize(listener: OnInitializeListener)`
  - 상세: Initializes SDK to use SpeechToText whether the application is created or not. Requires the caller to have STT_PERMISSION. SIG: @RequiresPermission(value = "pleos.car.permission.STT_SERVICE") fun initialize(listener: OnInitializeListener) Initializes SDK to use SpeechToText with a listener for initialization result. Requires the caller to have STT_PERMISSION.
- **registerApp** — Registers the application to use the server-oriented or hybrid SpeechToText service. Requires the caller to have STT_PERMISSION. `[perm: STT_SERVICE]`
  - `fun registerApp(clientID: String, clientSecret: String, listener: OnServerConnectionListener)`
- **release** — Releases SDK whether the application is destroyed or not. Requires the caller to have STT_PERMISSION. `[perm: STT_SERVICE]`
  - `fun release()`
- **removeListener** — Removes a previously registered listener Requires the caller to have STT_PERMISSION. `[perm: STT_SERVICE]`
  - `fun removeListener(listener: ResultListener)`
- **request** — Requests for starting speech recognition. After the application invoke request(), microphone will be open, and then you can stream your audio through microphone. `[perm: STT_SERVICE]`
  - `fun request()`
- **sendAudio** — Sends an external audio buffer (PCM) for recognition. Available only in Mode.ON_DEVICE. In other modes the call is ignored and ResultListener.onError is not called. Recognition starts when the first buffer is sent; calling request is not required. In this path, ResultListener.onReady and ResultListener.onUpdatedRms are not called. Call completeAudioSend after sending the last buffer. Requires the  … `[perm: STT_SERVICE]`
  - `fun sendAudio(bytes: ByteArray)`
- **setLocale** — Updates the locale that the user wants to set. Requires the caller to have STT_PERMISSION.
  - `fun setLocale(locale: Locale)`
- **stop** — Stops speech recognition. After the application invoke stop(), the microphone will be disabled. `[perm: STT_SERVICE]`
  - `fun stop()`

##### nested `SpeechToText.Companion`
```kotlin
object Companion
```
**Properties**
- **version**
  - `val version: String`

### package `ai.pleos.playground.stt.constant`

- `ErrorType` — Represents the types of errors that can occur within the system.
- `Mode` — This class provides SpeechToText mode.

#### `ai.pleos.playground.stt.constant.ErrorType`
```kotlin
enum ErrorType : Enum<ErrorType>
```
Represents the types of errors that can occur within the system.

**Entries**
- **AUDIO_ERROR** — Audio data processing error (e.g. failed to read the audio buffer).
- **MIC_ERROR** — Microphone input/recording related error.
- **SERVER_INTERNAL_ERROR** — Server-side processing failure (5xx, gRPC internal issues).
- **NETWORK_ERROR** — Network connectivity failure.
- **INVALID_REQUEST_ERROR** — Client-side invalid request.
- **TIMEOUT** — Request/response timeout.
- **UNAUTHENTICATED** — Authentication failure (invalid/expired token).
- **PERMISSION_DENIED** — Permission denied or insufficient access rights.
- **ON_DEVICE_ENGINE_ERROR** — On-device engine (e.g. Speech SDK) failure.
- **UNKNOWN** — Unknown error type.
- **UNAVAILABLE_LANGUAGE** — Requested language is not available.
- **NOT_SUPPORTED_API** — Requested API is not supported.

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<ErrorType>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): ErrorType`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<ErrorType>`

#### `ai.pleos.playground.stt.constant.Mode`
```kotlin
enum Mode : Enum<Mode>
```
This class provides SpeechToText mode.

**Entries**
- **HYBRID** — Streams audio to remote servers to perform SpeechToText. In case of network failure, SpeechToText will stream audio to the on-device engine automatically.
- **SERVER** — Streams audio to remote servers to perform SpeechToText.
- **ON_DEVICE** — Streams audio to the on-device engine to perform SpeechToText. The application doesn't need to be registered in Pleos Playground if it only uses ON_DEVICE mode.

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<Mode>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): Mode`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<Mode>`

### package `ai.pleos.playground.stt.data`

- `InferenceSource`

#### `ai.pleos.playground.stt.data.InferenceSource`
```kotlin
enum InferenceSource : Enum<InferenceSource>
```

**Entries**
- **ON_DEVICE**
  - `@SerializedName(value = "on_device") ON_DEVICE`
- **SERVER**
  - `@SerializedName(value = "server") SERVER`

**Types**
- **Companion**
  - `object Companion`

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<InferenceSource>`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): InferenceSource`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<InferenceSource>`

### package `ai.pleos.playground.stt.listener`

- `OnInitializeListener` — Listener for the result of SpeechToText SDK initialization.
- `OnServerConnectionListener` — Receives events from SpeechToText when the server is connected successfully.
- `ResultListener` — Receives events from SpeechToText when recognition-related events occur. Adds and removes with addListener/removeListener APIs respectively.

#### `ai.pleos.playground.stt.listener.OnInitializeListener`
```kotlin
interface OnInitializeListener
```
Listener for the result of SpeechToText SDK initialization.

**See also**
- **SpeechToText.initialize**

**Functions**
- **onFailure** — Called when the SDK initialization has failed.
  - `fun onFailure(errorType: ErrorType)`
- **onSuccess** — Called when the SDK has been successfully initialized and bound to the STT service.
  - `fun onSuccess()`

#### `ai.pleos.playground.stt.listener.OnServerConnectionListener`
```kotlin
interface OnServerConnectionListener
```
Receives events from SpeechToText when the server is connected successfully.

**Functions**
- **onConnected** — Called when connected to the server
  - `fun onConnected()`
- **onFailed** — Called when failure to connection to server
  - `fun onFailed(errorType: ErrorType)`

#### `ai.pleos.playground.stt.listener.ResultListener`
```kotlin
interface ResultListener
```
Receives events from SpeechToText when recognition-related events occur. Adds and removes with addListener/removeListener APIs respectively.

**Functions**
- **onEndedRecognition** — Called when caller recognition is finished.
  - `fun onEndedRecognition()`
- **onError** — Called when caller recognition fails.
  - `fun onError(errorType: ErrorType)`
- **onReady** — Called when listener recognition is ready.
  - `fun onReady()`
- **onStartedRecognition** — Called when caller recognition is started.
  - `fun onStartedRecognition()`
- **onUpdated** — Called when the SpeechToText result is updated.
  - `fun onUpdated(stt: String, completed: Boolean)`
- **onUpdatedEpdData** — Called when the SpeechToText EPD result is updated.
  - `fun onUpdatedEpdData(on: Long, off: Long)`
- **onUpdatedRms** — Called when RMS is updated.
  - `fun onUpdatedRms(rms: Double)`

---

## FusedLocation SDK


### package `ai.pleos.playground.location.fused`

- `FusedLocation`

#### `ai.pleos.playground.location.fused.FusedLocation`
```kotlin
class FusedLocation(context: Context)
```

**Constructors**
- **FusedLocation**
  - `constructor(context: Context)`

**Functions**
- **addProximityAlert** — Registers a proximity alert for the specified area and transitions. `[perm: FUSED_LOCATION]`
  - `fun addProximityAlert(request: ProximityAlertRequest, listener: ProximityAlertListener)`
  - 상세: Registers a proximity alert for the specified area and transitions. Reports the alert ID through ProximityAlertListener.onRegistered and invalid requests through ProximityAlertListener.onError. Requires the following permission (protection level: dangerous).
- **getLastKnownLocation** — Gets the last known location. `[perm: FUSED_LOCATION]`
  - `fun getLastKnownLocation(callback: FusedLocationListener)`
  - 상세: Gets the last known location. The method is protected by the permission
- **initialize** — Initializes the SDK.
  - `fun initialize()`
  - 상세: Initializes the SDK. Call this method when the application is created.
- **isSupported** — Returns whether the Fused Location Service package is present on Pleos Connect.
  - `fun isSupported(): Boolean`
  - 상세: Returns whether the Fused Location Service package is present on Pleos Connect. This check is independent of SDK initialization and service connection state. It does not start or bind the service.
- **registerFusedLocationCallback** — Registers a callback to receive location updates. `[perm: FUSED_LOCATION]`
  - `fun registerFusedLocationCallback(listener: FusedLocationListener)`
  - 상세: Registers a callback to receive location updates. The method is protected by the permission
- **release** — Releases the SDK.
  - `fun release()`
  - 상세: Releases the SDK. Call this method when the application is destroyed.
- **removeProximityAlert** — Removes a previously registered proximity alert. `[perm: FUSED_LOCATION]`
  - `fun removeProximityAlert(alertId: String)`
  - 상세: Removes a previously registered proximity alert. Ignores unknown or already removed alert IDs after checking permission. Requires the following permission (protection level: dangerous).
- **showLocationAccessBlockedPopup** — Displays a popup message informing the user that location access is blocked due to their current privacy settings.
  - `fun showLocationAccessBlockedPopup()`
  - 상세: Displays a popup message informing the user that location access is blocked due to their current privacy settings. This popup should be shown when the SDK cannot provide location data because the user has restricted location sharing in system privacy settings.
- **unregisterFusedLocationCallback** — Stops receiving location updates through the specified listener. `[perm: FUSED_LOCATION]`
  - `fun unregisterFusedLocationCallback(listener: FusedLocationListener)`
  - 상세: Stops receiving location updates through the specified listener. The method is protected by the permission

### package `ai.pleos.playground.location.fused.listener`

- `FusedLocationListener` — Defines the listener interface for receiving location updates.
- `ProximityAlertListener` — Receives events and errors for a registered proximity alert.

#### `ai.pleos.playground.location.fused.listener.FusedLocationListener`
```kotlin
interface FusedLocationListener
```
Defines the listener interface for receiving location updates.

**Functions**
- **onChanged** — Invoked when a location update is available.
  - `fun onChanged(location: LocationData?)`
- **onError**
  - `fun onError(exception: Exception)`

#### `ai.pleos.playground.location.fused.listener.ProximityAlertListener`
```kotlin
interface ProximityAlertListener
```
Receives events and errors for a registered proximity alert. Reports the alert identifier after registration and transitions for the configured area.

**Functions**
- **onError** — Invoked when the alert cannot be registered or processed.
  - `fun onError(exception: Exception)`
- **onRegistered** — Invoked when the alert is registered successfully.
  - `fun onRegistered(alertId: String)`
- **onTransition** — Invoked when a configured proximity transition is detected.
  - `fun onTransition(event: ProximityAlertEvent)`

### package `ai.pleos.playground.location.fused.model`

- `LocationData`
- `ProximityAlertEvent` — Describes a transition detected for a registered proximity alert.
- `ProximityAlertRequest` — Defines an area and the transitions to monitor for a proximity alert.
- `ProximityTransition` — Describes a transition that can trigger a proximity alert.

#### `ai.pleos.playground.location.fused.model.LocationData`
```kotlin
data class LocationData(val latitude: Double, val longitude: Double, val altitude: Double, val heading: Float, val confidence: Float, val timestampMillis: Long)
```

**Constructors**
- **LocationData**
  - `constructor(latitude: Double, longitude: Double, altitude: Double, heading: Float, confidence: Float, timestampMillis: Long)`

**Properties**
- **altitude**
  - `val altitude: Double`
- **confidence**
  - `val confidence: Float`
- **heading**
  - `val heading: Float`
- **latitude**
  - `val latitude: Double`
- **longitude**
  - `val longitude: Double`
- **timestampMillis**
  - `val timestampMillis: Long`

#### `ai.pleos.playground.location.fused.model.ProximityAlertEvent`
```kotlin
data class ProximityAlertEvent(val alertId: String, val transition: ProximityTransition)
```
Describes a transition detected for a registered proximity alert.

**Constructors**
- **ProximityAlertEvent**
  - `constructor(alertId: String, transition: ProximityTransition)`

**Properties**
- **alertId** — String: Identifier delivered when the alert registration succeeds.
  - `val alertId: String`
- **transition** — ProximityTransition: Detected transition.
  - `val transition: ProximityTransition`

#### `ai.pleos.playground.location.fused.model.ProximityAlertRequest`
```kotlin
data class ProximityAlertRequest(val latitude: Double, val longitude: Double, val radiusMeters: Float, val transitionTypes: Set<ProximityTransition>)
```
Defines an area and the transitions to monitor for a proximity alert.

**Constructors**
- **ProximityAlertRequest**
  - `constructor(latitude: Double, longitude: Double, radiusMeters: Float, transitionTypes: Set<ProximityTransition>)`

**Properties**
- **latitude** — Double: Finite center latitude in degrees, from -90 to 90 inclusive.
  - `val latitude: Double`
- **longitude** — Double: Finite center longitude in degrees, from -180 to 180 inclusive.
  - `val longitude: Double`
- **radiusMeters** — Float: Finite radius in meters, greater than zero.
  - `val radiusMeters: Float`
- **transitionTypes** — Set of ProximityTransition: Non-empty set of transitions that trigger the alert.
  - `val transitionTypes: Set<ProximityTransition>`

#### `ai.pleos.playground.location.fused.model.ProximityTransition`
```kotlin
enum ProximityTransition : Enum<ProximityTransition>
```
Describes a transition that can trigger a proximity alert.

**Entries**
- **ENTER** — Triggered when the device moves into the configured area.
- **EXIT** — Triggered when the device moves out of the configured area.

**Properties**
- **entries** — Returns a representation of an immutable list of all enum entries, in the order they're declared.
  - `val entries: EnumEntries<ProximityTransition>`
- **name**
  - `val name: String`
- **ordinal**
  - `val ordinal: Int`

**Functions**
- **valueOf** — Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
  - `fun valueOf(value: String): ProximityTransition`
- **values** — Returns an array containing the constants of this enum type, in the order they're declared.
  - `fun values(): Array<ProximityTransition>`
