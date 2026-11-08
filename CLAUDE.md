# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project goal: HMG in-vehicle app contest entry

This app is an entry to the **현대자동차그룹 차량용 앱 개발 공모전** ("Code your App into Pleos App Market").

- **Brief**: build a Pleos Connect app that uses vehicle data/functions and the driving context (location, route, driving state, vehicle state) to give drivers and passengers a new in-car experience. The topic is free. Moments to target include commuting, travel, charging, parking, family trips, travelling with pets, resting in the car and working from the car.
- **Example areas** (not limits):
    - Mobility & life convenience: ordering, pickup and parking that work seamlessly while driving.
    - In-car entertainment: media and sound tied to the driving context.
    - Smart driving & safety: early detection of vehicle anomalies, real-time safe-driving feedback.
    - Family & passenger care: making long trips better for kids and passengers.
- **Format**:
    - Round 1 is a written plan. Section 5 of the plan describes how the app is built on Pleos Connect: scope and SDK/API usage.
    - Round 2 is an offline pitch with a working implementation of the core features.
    - Finalists demo a release APK live at an HMG event.
- **Judging criteria** (weights undisclosed). Treat them as design priorities:
    1. **Vehicle-specificity**: value that a phone app can't offer; tightly coupled to driving/parked context and vehicle functions.
    2. **Creativity**: a clearly defined driver/passenger pain point and a clear solution.
    3. **Technical use**: Pleos SDKs/APIs drive the *core* features, not decoration.
    4. **Business viability**: target users, expected MAU, retention, expansion plan.
    - Bonus points: a demo video and a GitHub repo with source code and APK.
- **What this means for code**:
    - Prefer features that react to vehicle and driving state.
    - Keep SDK usage central and consistent with the written plan and the README.
    - Prioritise crash-free, stable demos on the Pleos emulator over breadth.
- **Repo conventions for submission**:
    - Put the runnable **release** APK at `apk/돈도아연_놀레오_release.apk` (`<팀명>_<앱이름>_release.apk`; final team decision 2026-11-08). Team name: 돈도아연 (DonDoAYeon when written in English).
    - `README.md` must include: the app name and a one-line intro, how to run it, the test environment and required settings, and the APK location or release link.
    - Never commit secrets.

## Keep the md docs in sync (always apply)

The docs are part of the contest submission: judges and teammates read them, and the user expects them to match the code without being asked. **At the end of every task that changes behaviour, UI text, data/assets, build/toolchain, or records a user decision, update every affected md file in the same turn.** Pure Q&A with no change needs no doc update.

Where things go (the docs are in Korean; write in the same plain style):

| File | Update when… |
|---|---|
| `README.md` | app intro, how to run, test environment/settings, APK location, SDK usage table (§4), architecture tree (§5), test count (§6), content licences, docs list (§7) change |
| `docs/FEATURE_GUIDE.md` | a feature's behaviour changes: what it does, emulator steps, expected screen/voice/log output, log tags (§12), what can't be verified (§13), troubleshooting (§14). Keep the table of contents and anchors valid |
| `docs/DEMO_SCENARIO.md` | the 3-minute demo flow, on-screen/TTS wording, timing, or pre-recording checklist changes |
| `docs/WORK_STATUS.md` | always, for any change: a milestone row (commit hash), a numbered section with what changed and why, decisions (§1.4), doc-vs-reality findings (§1.5), emulator verification results and DoD numbers (lint errors/warnings, test count), and remaining work (§2). It is **one file updated in place** — never create per-date files. The user edits it concurrently, so re-read it right before editing |
| `docs/PHASE2_NOTES.md` | a follow-up idea comes up, or a phase-2 item is implemented — then **remove** it (the file lists only unimplemented work; first make sure FEATURE_GUIDE/WORK_STATUS cover what was built) |
| `docs/CONTENT_SOURCES.md` | any asset/data file is added, edited or removed: source, licence, transformation steps, counts, data quirks |
| `docs/README.md` | a doc is added, removed, renamed, or its scope changes |
| `CLAUDE.md` | build commands, toolchain, conventions or these rules change |

How:
1. Before finishing, grep the docs for stale wording: removed classes/files, old UI strings, old numbers (counts, timings, test totals), log tags.
2. Write only what was actually verified; mark anything not verified explicitly (e.g. "확인 못 함") with the reason.
3. Cross-check numbers and names across files (README vs FEATURE_GUIDE vs WORK_STATUS vs CONTENT_SOURCES) so they agree.
4. Commit only when the user asks. Doc updates go in the same commit as the code. After committing, fill the commit hash into the WORK_STATUS milestone row with a small follow-up `docs:` commit.

## Reference docs (read on demand — do NOT `@`-import)

These are large. Importing them with `@` would load them into every session, so open or grep only the section you need.

- `docs/pleos/PLEOS_CONNECT_DEV_GUIDE.md` (~110 KB, Korean) covers platform policies and how-tos. Section map:
    - §0 TL;DR rules · §2 getting started · §4 emulator / Virtual Console
    - §5 design principles: driving restrictions, multi-window, performance limits
    - §6 UI design guide
    - §7 dev guides: CarUxRestrictions, audio focus, suspend, CCP, Maps/Nav, Music/Audio, TTS pre-roll, IME, FAQ, ADB
    - §8 SDKs: Vehicle / NaviHelper / TextToSpeech / SpeechToText / FusedLocation
    - §9 Gleo voice interface (`assets/schema.json`)
    - §10 testing · §11 registration, security and App Market review guidelines · §12 samples
    - §13 release notes and breaking changes · §14 implementation checklist
- `docs/pleos/PLEOS_CONNECT_API_REFERENCE.md` (~320 KB) lists every SDK class and method signature with its required permission and protection level. Grep it by class or method name (e.g. `grep -n "setDoorPosition"`); never read it whole.
- **Precedence**:
    - For build and toolchain config, this file and the repo win. The docs reflect the samples' toolchain (compileSdk 35, AGP 8.8).
    - For SDK behaviour and platform policy, follow the docs.
    - Verify signatures in the API reference or the sample code; don't guess.
- TextToSpeech / SpeechToText (`ai.pleos.playground:TextToSpeech:3.3.1`, `SpeechToText:3.3.1`) have no local sample. See guide §8.3–8.4. Server/hybrid mode needs the Client ID and Secret from Pleos Playground.

## What this workspace is

An Android workspace for building an app on **Pleos** (an Android-based in-vehicle platform) using the Pleos Playground SDKs. It contains two unrelated kinds of Gradle builds:

- **Root project (`./`, module `:app`)** — the app under development: **놀레오 (Nolleo)** — subtitle "가는 길이 놀이가 되는 가족 게임" — package `com.nolleo.app`, Jetpack Compose UI, all five Pleos SDKs wired in. The app was renamed on 2026-11-07; the old name must not reappear anywhere (code, strings, docs, file names). See `README.md` (architecture, SDK usage table), `docs/FEATURE_GUIDE.md` (every feature and how to verify it on the emulator — keep it in sync when behaviour changes) and `docs/WORK_STATUS.md` (latest work status and TODOs — a single file that is updated in place, not one file per date).
- **`pleos-example/`** — three self-contained reference apps, each its own git repo with its own Gradle wrapper. They are **not** included in the root build; build them from their own directories. Treat them as reference material for how to call each SDK.
    - `pleos-sample-vehicle-app` — Vehicle SDK (`ai.pleos.playground:Vehicle:2.3.2`): vehicle properties (door, HVAC, seat, window, …). Most detailed README; the typed Door Position flow is the canonical integration reference.
    - `pleos-sample-navi-app` — NaviHelper SDK (`ai.pleos.playground:NaviHelper:3.0.5`): route requests and navigation events, sent via NaviService to the default navigation app. Uses Hilt + kapt.
    - `pleos-sample-fusedlocation-app` — FusedLocation SDK (`ai.pleos.playground:FusedLocation:1.1.3`): last-known/continuous location and proximity alerts.

## Build commands

On Windows use `gradlew.bat` (PowerShell) or `./gradlew` (Git Bash).

Root app:
```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew testDebugUnitTest                                   # JVM unit tests
./gradlew testDebugUnitTest --tests "com.nolleo.app.domain.engine.ModeArbiterTest"   # single test class
./gradlew connectedDebugAndroidTest                           # instrumented, needs device/emulator
./gradlew lintDebug
```

Sample apps (run from inside each sample directory):
```bash
./gradlew assembleDebug
./gradlew lintDebug testDebugUnitTest assembleDebug   # vehicle sample's full local check; needs JDK 17
```
The vehicle sample's README also describes unit/Compose UI tests (and a known-failing `PublicApiPolicyTest` that still expects 441 methods instead of 442), but `src/test` and `src/androidTest` are not present in this checkout.

## Toolchain differences (important)

The root project and the samples use very different toolchains; don't copy build config between them blindly.

| | Root `:app` | Samples |
|---|---|---|
| Gradle | 9.6.0 (daemon JVM toolchain 25 via foojay) | 8.10.2 |
| AGP | 9.5.0 (built-in Kotlin; no `kotlin-android` plugin) | 8.8.x + Kotlin 1.9/2.0 plugins |
| compileSdk / minSdk | 37 / 28 | 35 / 23–33 |
| Java target | 11 | 17 (vehicle, fusedlocation), 11 (navi) |
| UI | Jetpack Compose + Material3 (compose plugin = KGP 2.2.10 bundled with AGP 9.5) | Jetpack Compose + Material3 |
| Shrinking | AGP 9 `optimization {}` block + `src/main/keepRules/*.keep` | `proguardFiles(...)` |

Configuration cache is enabled in the root `gradle.properties`.

Keep root `minSdk` ≥ 28. The platform requires it for signing-key rotation and lineage; the samples' lower minSdk doesn't apply to the product app.

## Adding a Pleos SDK to the root app

1. `settings.gradle.kts` uses `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, so the Pleos repo must go in `dependencyResolutionManagement.repositories`:
   `maven("https://nexus-playground.pleos.ai/repository/maven-releases/")` (no credentials needed).
2. Add the artifact to `gradle/libs.versions.toml` and reference it via `libs.*` in `app/build.gradle.kts`.
3. In `AndroidManifest.xml`, declare **only** the `pleos.car.permission.*` that the code actually calls.
    - Take the exact name from `@RequiresPermission` in the API reference or the sample.
    - Do **not** copy a sample's full permission list. Vehicle permissions must be pre-approved by Pleos App Market; unneeded or unapproved permissions fail review.
    - `CAR_*` = read, `CONTROL_CAR_*` = control.
    - Navi: `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `NAVI_CUSTOM_MAP`. Voice: `TTS_SERVICE`, `STT_SERVICE`.
    - Most Vehicle permissions are `protectionLevel="normal"` (no runtime dialog).
    - Telltales APIs and `Window.setSmartGlassState` are `signature|privileged`: unusable by this app, so don't build features on them.
    - `FUSED_LOCATION` is `dangerous`, so request it at runtime.

## Pleos SDK integration pattern (common to all samples)

All three SDKs share the same lifecycle shape, and the samples wrap each SDK in a small client class owned by a ViewModel:

- Construct with `applicationContext` (`Vehicle(ctx)`, `NaviHelper(ctx)`, `FusedLocation(ctx)`), call `initialize()` before any API, and `release()` when the owner is destroyed (e.g. `ViewModel.onCleared`). Guard with an `isInitialized` flag; the samples throw `IllegalStateException` if an API is used before init.
- Where available, check support first (`FusedLocation.isSupported()`; Vehicle `check*Capabilities`) and only proceed on success. Vehicle capability responses give per-area access type and value bounds — use them, never assume a range.
- APIs are **asynchronous callback-based** (`onComplete` / `onFailed`, or listener interfaces). Chain dependent steps from the previous success callback, not sequentially.
- Listeners: keep the same instance for register and unregister, and unregister before `release()` — a listener must not outlive its session.
- Vehicle control (`set*`) calls change real vehicle state: validate against capability bounds and require an explicit user action. A successful SET ack does not mean a subsequent read reflects the new value.

Key files to read when implementing a feature:
- Vehicle: `pleos-example/pleos-sample-vehicle-app/app/src/main/java/ai/pleos/playground/sample/vehicle/vehicle/VehicleClient.kt` (typed Door calls) and the README's "How to use the typed Door API". `explorer/VehicleApiExplorer.kt` uses reflection to catalog public SDK methods — useful for discovering API names, but production code should call the typed API directly.
- Navi: `pleos-example/pleos-sample-navi-app/app/src/main/java/ai/pleos/playground/navidevapp/screen/vm/NaviHelperViewModel.kt` (all `NaviHelper` calls + `NaviHelperEventListener`), sample payloads in `data/sample/`.
- FusedLocation: `pleos-example/pleos-sample-fusedlocation-app/app/src/main/java/ai/pleos/playground/location/fused/sample/location/FusedLocationClient.kt`.

## Platform rules (App Market review — always apply)

Details live in the dev guide; the section is noted for each rule.

- **Driving restrictions (§7.1)**:
    - Listen to `CarUxRestrictionsManager` and honour the active bits (`NO_VIDEO`, `NO_KEYBOARD`, `NO_SETUP`, `LIMIT_STRING_LENGTH`, …).
    - Without Distraction Optimization, the app is unusable while driving.
    - Never hardcode regions; the platform sends region-specific bits.
- **Audio (§7.2–7.3)**:
    - Request audio focus before any playback and handle every focus change.
    - Navigation prompts use `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.
    - On Gleo / `LOSS_TRANSIENT`, stop at once.
    - On suspend, abandon focus. Never auto-resume or auto-play.
- **Windows (§5.4, §6.3)**:
    - Support both portrait (1 window) and landscape (2 windows).
    - No letterboxing and no compat mode.
    - Hidden ≠ killed: restore state when the app returns to the foreground.
- **CCP Back (§7.4)**: at the top-level screen call `moveTaskToBack(true)`, never `finish()`.
- **Performance (§5.9, §11.6)**:
    - CPU while driving: < 5% (navigation apps ≤ 15%). CPU while parked: ≤ 25%.
    - RAM ≤ 400 MB.
    - Input response ≤ 0.25 s. Launch and content load ≤ 10 s. Button reaction ≤ 2 s, otherwise show progress.
- **Data sources (§5.6, §7.9)**:
    - Read standard data such as speed or gear through AAOS `CarPropertyManager` (e.g. `PERF_VEHICLE_SPEED`).
    - Use Pleos SDKs for OEM-specific data.
    - Never branch on the vehicle model name.
- **UI (§6)**:
    - Glanceable: contrast ≥ 4.5:1, ≤ 3 lines of text, minimum 12sp.
    - No video, animation or auto-scroll while driving.
    - No ads in notifications.

## App design system (always apply to UI work)

Redesigned 2026-11-08 for readability (the team picked the "yellow" palette); summarised in `README.md` §5 "화면 디자인". Keep new UI inside it:

- Use only the tokens in `ui/theme/Theme.kt`: colours via `Nl.colors.*` (never hard-coded `Color(...)` in screens), spacing `Sp`, shapes `Radius`. Day = light grey ground, white cards, near-black ink; night (system night mode) = charcoal ground, white ink. Yellow (`action`) only for the primary button, selection marks and must-see prompt cards; text on yellow is always ink. Every text/ground pair stays ≥ 4.5:1 in both modes.
- Use the components in `ui/common/Components.kt`: `PrimaryButton` (exactly one filled per view), `SecondaryButton`, `SegmentedControl` + `SettingRow` (pick one of a few options in a single row — prefer this over scattered buttons), `ChoiceRow` (radio list row), `NlCard`, `BandCard` (only for must-see prompts such as drowsiness care), `Chip`, `Avatar`, `Hairline`, `Logo`, `MicIndicator` (idle/listening/speaking pill), `AnswerCountdown` (word-chain seconds left — digits change once a second, no animation), `WordChainRow` (word-chain words, one line, drops leading words to fit). Every tappable surface uses `Modifier.pressable` (instant pressed overlay, no Material ripple — the ripple looked slow). Don't add a second button, card or radius style. Switch wide/narrow layouts with `isWideLayout()` (one threshold for every screen) and indent under a `SettingRow` with `SettingLabelWidth`.
- Group, don't scatter: a screen is a few cards; each screen has one header row with at most one button on each side.
- One accent colour; success/danger only for screen-game answer states and the voice screen's live/listening indicators. Never violet or lavender (also check Material defaults leaking into components).
- Type: platform font (Asta Sans) at weights 400 and 600 (700 only for the `Nolleo` wordmark) — on Pleos, 500 maps to Regular and 700 to ExtraBold. Smallest text 21sp (driving-screen body ≥ 33sp; raised twice on 2026-11-08 — the emulator is 160dpi, so 1sp = 1px on the 2560px screen), and the home list/setting rows use one step larger styles; touch targets ≥ 76dp.
- The title bar is the logo + English wordmark `Nolleo` only (no subtitle, no source/engine text — that lives in the debug panel). Copy: headings are nouns ("목적지", "오늘의 게임"), no exclamation marks in UI labels (spoken TTS lines may keep them).
- **Security (§11.4)**:
    - No secrets in source; inject them via `local.properties` / `BuildConfig`. Use Keystore + AES-GCM.
    - HTTPS with TLS 1.2+.
    - No PII in logs.
    - Components not exported unless needed.
    - Release build is not debuggable. Disable `allowBackup`.

## Running on a device

Pleos services exist only on a Pleos emulator/device (e.g. the `Pleos_Connect_v2.1` AVD, Android 14, 2560×1440); on a stock Android image the SDKs report unsupported/fail. The AVD must use **cold boot**; with quick boot, VHAL is unavailable.

Change vehicle state with the Pleos Connect Virtual Console: run `adb forward tcp:8088 tcp:8088`, then open http://127.0.0.1:8088/. See guide §4.4 and §10.2.

Useful emulator commands (from the sample READMEs and the dev guide):
```bash
adb emu geo fix <lon> <lat>     # longitude first; emulator only
adb shell appops set --user current <applicationId> android:mock_location allow
adb shell cmd hmg_car_service --service drivingstate driving_regulation on   # simulate driving restrictions (off to clear)
```