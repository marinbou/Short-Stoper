# Tasks: Short-Form Content Blocker

**Input**: Design documents from `specs/001-block-short-form-videos/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Not requested in the feature specification. Verification is via `./gradlew assembleDebug` and manual device checks per quickstart.md.

**Organization**: Tasks are grouped by user story. The six high-level work items the user supplied (Project Init → Manifest/Resources → UI → Detection → Core Engine → Compilation) are preserved as phase groups and expanded into granular, path-specific checklist items.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths in every description

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and Gradle build scripts (user Task 1)

- [X] T001 Create Android project directory structure per plan.md: `app/`, `app/src/main/java/com/shortstoper/app/`, `app/src/main/java/com/shortstoper/app/detection/`, `app/src/main/res/{layout,values,xml,drawable,mipmap-*/}`.
- [X] T002 Create root Gradle Kotlin DSL files: `settings.gradle.kts` (include `:app`, pluginManagement with google/mavenCentral), root `build.gradle.kts` (plugin declarations only), and `gradle.properties`.
- [X] T003 [P] Create `local.properties` pointing `sdk.dir` at the Android SDK; add Gradle wrapper files `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, and `gradle/wrapper/gradle-wrapper.jar`.
- [X] T004 Create `app/build.gradle.kts`: Android application plugin + Kotlin plugin, `compileSdk 34`, `minSdk 24`, `targetSdk 34`, dependencies limited to `androidx.appcompat` and `androidx.core-ktx` (no Compose, no networking, no analytics).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Manifest and resources every user story depends on (user Task 2)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T005 Create `app/src/main/AndroidManifest.xml` declaring `MainActivity` (launcher) and `BlockerService` (with `android.permission.BIND_ACCESSIBILITY_SERVICE`, meta-data pointing to `@xml/accessibility_service_config`), `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission, and NO `INTERNET` permission.
- [X] T006 [P] Create `app/src/main/res/xml/accessibility_service_config.xml` with `packageNames` `com.google.android.youtube`, `com.instagram.android`, `com.snapchat.android`; `accessibilityEventTypes` `typeWindowStateChanged` + `typeWindowContentChanged`; `canRetrieveWindowContent=true`.
- [X] T007 [P] Create `app/src/main/res/values/strings.xml`, `app/src/main/res/values/colors.xml`, and `app/src/main/res/values/themes.xml` with a plain XML app theme (no Material Components required).
- [X] T008 [P] Create app logo as vector drawable `app/src/main/res/drawable/ic_logo.xml` and adaptive icon resources under `app/src/main/res/mipmap-*/` referenced from the manifest.

**Checkpoint**: Project builds a skeleton APK; foundation ready for user stories.

---

## Phase 3: User Story 1 - Instantly Exit Short-Form Feeds (Priority: P1) 🎯 MVP

**Goal**: Detect Shorts/Reels/Spotlight and auto-exit via Back, once per entry (user Tasks 4 + 5).

**Independent Test**: Enable the service, open YouTube/Instagram/Snapchat, enter the short-form feed, and confirm a single automatic return to the normal feed (per quickstart.md).

### Implementation for User Story 1

- [X] T009 [P] [US1] Create `ShortFormDetector` interface in `app/src/main/java/com/shortstoper/app/detection/ShortFormDetector.kt` with `fun isShortFormScreen(root: AccessibilityNodeInfo): Boolean` (contract in `contracts/detector-interface.md`).
- [X] T010 [P] [US1] Create `YouTubeShortsDetector.kt` in `app/src/main/java/com/shortstoper/app/detection/` implementing `ShortFormDetector` with null-safe, recycling YouTube Shorts heuristics.
- [X] T011 [P] [US1] Create `InstagramReelsDetector.kt` in `app/src/main/java/com/shortstoper/app/detection/` implementing `ShortFormDetector` with null-safe, recycling Reels heuristics.
- [X] T012 [P] [US1] Create `SnapchatSpotlightDetector.kt` in `app/src/main/java/com/shortstoper/app/detection/` implementing `ShortFormDetector` with null-safe, recycling Spotlight heuristics.
- [X] T013 [US1] Create `BlockerService.kt` in `app/src/main/java/com/shortstoper/app/`: map `packageName` → detector, apply 800-1000ms per-package debounce, call `performGlobalAction(GLOBAL_ACTION_BACK)`, and recycle nodes in `try/finally` (depends on T009-T012).

**Checkpoint**: User Story 1 fully functional and testable on a device.

---

## Phase 4: User Story 2 - Enable Monitoring and See Its Status (Priority: P2)

**Goal**: Dashboard with logo, ON/OFF toggle reflecting live service state, and access to accessibility settings (user Task 3).

**Independent Test**: Open the app, see logo + OFF toggle, tap toggle to open Accessibility settings, enable the service, return to see ON (per quickstart.md).

### Implementation for User Story 2

- [X] T014 [US2] Create `app/src/main/res/layout/activity_main.xml`: logo `ImageView`, status `TextView`, ON/OFF `Switch` (or `ToggleButton`), and a settings `Button`.
- [X] T015 [US2] Create `MainActivity.kt` in `app/src/main/java/com/shortstoper/app/`: on resume read the enabled accessibility-services list and set the toggle/status; toggle opens `Settings.ACTION_ACCESSIBILITY_SETTINGS`; prompt `ACTION_IGNORE_BATTERY_OPTIMIZATIONS` once via a `SharedPreferences` flag (depends on T014).

**Checkpoint**: User Stories 1 AND 2 both work independently.

---

## Phase 5: User Story 3 - Add a New Short-Form Platform Without Rework (Priority: P3)

**Goal**: Confirm the detection layer allows adding a platform by touching only a new detector class plus one dispatch mapping entry.

**Independent Test**: Add a stub detector for a new package, wire one mapping entry, and verify the three existing platforms still detect/exit unchanged.

### Implementation for User Story 3

- [X] T016 [US3] Add a central package→detector mapping registry in `app/src/main/java/com/shortstoper/app/BlockerService.kt` (or a small registry file) and verify by following `contracts/detector-interface.md` that a new platform requires only a new detector class + one registry entry, with no edits to `YouTubeShortsDetector.kt`, `InstagramReelsDetector.kt`, or `SnapchatSpotlightDetector.kt`.

**Checkpoint**: Extensibility contract satisfied and demonstrated.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Build verification and cleanup (user Task 6)

- [X] T017 Run `./gradlew assembleDebug` and confirm the APK at `app/build/outputs/apk/debug/app-debug.apk`; fix any compile errors until the build passes with zero errors.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories.
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion; proceed in priority order (US1 → US2 → US3) or in parallel.
- **Polish (Phase 6)**: Depends on all desired user stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: After Foundational - no dependency on other stories.
- **User Story 2 (P2)**: After Foundational - independent of US1.
- **User Story 3 (P3)**: After Foundational - depends on the detector architecture from US1 but does not modify existing detectors.

### Within Each User Story

- Interface (T009) before detectors and service (T010-T013).
- Detectors (T010-T012) before the service dispatch/back logic (T013).
- Layout (T014) before activity logic (T015).

### Parallel Opportunities

- Setup: T003 can run alongside T002/T004.
- Foundational: T006, T007, T008 are independent files and can run in parallel.
- US1: T010, T011, T012 (the three detectors) are independent files and can run in parallel.
- US1 vs US2 vs US3 can proceed in parallel once Foundational completes.

---

## Parallel Example: User Story 1

```text
# Launch all three detectors together (independent files):
Task: "Create YouTubeShortsDetector.kt ..."
Task: "Create InstagramReelsDetector.kt ..."
Task: "Create SnapchatSpotlightDetector.kt ..."
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories).
3. Complete Phase 3: User Story 1 (detectors + BlockerService).
4. **STOP and VALIDATE**: build via `./gradlew assembleDebug` and test US1 on device.
5. Demo the core auto-escape value.

### Incremental Delivery

1. Setup + Foundational → skeleton APK builds.
2. Add US1 → test auto-escape independently (MVP!).
3. Add US2 → test dashboard enable/status independently.
4. Add US3 → verify extensibility contract.
5. Phase 6 build verification gates every delivery.

---

## Notes

- [P] tasks = different files, no dependencies.
- [Story] label maps task to a user story for traceability.
- Commit after each task or logical group.
- Stop at any checkpoint to validate a story independently.
- The constitution (I-VII) governs all tasks: XML only, one activity + one service, per-package debounce 800-1000ms, node recycling, zero network, null-safety, one-time battery prompt.