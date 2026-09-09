# Research: Short-Form Content Blocker

Phase 0 output. Resolves all technical unknowns for the plan. No open
NEEDS CLARIFICATION items remain.

## R1. Accessibility event stream and package filtering

- **Decision**: Declare `BlockerService` as an `AccessibilityService` with
  `accessibility_service_config.xml` limiting `packageNames` to
  `com.google.android.youtube`, `com.instagram.android`, and
  `com.snapchat.android`, and listening to `typeWindowStateChanged` and
  `typeWindowContentChanged` events.
- **Rationale**: The OS-level package filter guarantees the service receives events
  only for the three target apps, satisfying the "monitor only within target apps"
  requirement (FR-001) without the service having to ignore everything else.
- **Alternatives considered**: Listening to all packages and filtering in code. Rejected:
  broader surface area, more events, more battery use, and weaker privacy guarantees.

## R2. How to leave the short-form screen

- **Decision**: Call `performGlobalAction(GLOBAL_ACTION_BACK)` from the service.
- **Rationale**: The standard, non-invasive way to emulate the system Back action. It
  returns the user to the previous screen (the normal feed) without touching the target
  app's process.
- **Alternatives considered**: Simulating gesture input. Rejected: more complex and
  fragile. Intent-based deep links back to the home feed. Rejected: target apps do not
  expose stable deep links for "leave Shorts".

## R3. Debounce / loop prevention

- **Decision**: Keep a per-package last-action timestamp (monotonic clock). Before
  firing Back, require at least 800-1000ms since the last Back for that same package;
  otherwise swallow the event.
- **Rationale**: Prevents a Back → feed → re-enter loop and satisfies FR-003. Per-package
  tracking keeps one app's cooldown from suppressing another app's legit detection.
- **Alternatives considered**: Global single cooldown. Rejected: a Back in YouTube could
  delay a valid Back in Instagram, and vice versa.

## R4. Node recycling and null-safety

- **Decision**: Detectors receive the root `AccessibilityNodeInfo` and MUST call
  `recycle()` on every node they obtain (children and themselves) via `try/finally`.
  Detectors return `false` on any null node or exception; the service treats an
  exception as "not a short-form screen".
- **Rationale**: Unrecycled nodes leak native memory and eventually crash the service
  (constitution IV and VI). Null-safety is required because target apps change UI often
  (FR-010).
- **Alternatives considered**: Centralized recycling in the service after the detector
  returns. Rejected: the detector walks the tree, so it owns the references it created.

## R5. Detector heuristics

- **Decision**: Each detector checks stable, app-specific signals on the root node:
  YouTube (e.g., a "Shorts" pill/label or view-id/text patterns), Instagram (Reels
  markers), Snapchat (Spotlight markers). Signals are checked defensively (null-safe,
  sub-string/contains), and any signal is enough to declare a short-form screen.
- **Rationale**: A single shared heuristic cannot cover three different apps; per-app
  isolation is the constitution's core principle (III). "Any signal" trades a little
  precision for resilience against UI changes.
- **Alternatives considered**: Exact view-id equality. Rejected: too brittle across app
  versions. Full-screen vs feed-only detection. Rejected: both are short-form per spec
  assumptions.

## R6. Service state reflection on the dashboard

- **Decision**: `MainActivity.onResume()` reads the enabled accessibility services list
  from system settings and checks whether this app's service is present; the toggle and
  status text reflect that value.
- **Rationale**: The user can enable/disable the service in system settings at any time,
  so the dashboard must re-read state each time it becomes visible (FR-005/FR-007).
- **Alternatives considered**: Caching the state in a preference. Rejected: it can go
  stale when the user toggles the service from Settings directly.

## R7. Battery optimization prompt

- **Decision**: On first launch (tracked by a `SharedPreferences` flag), request
  `ACTION_IGNORE_BATTERY_OPTIMIZATIONS` with the
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission declared in the manifest. Never
  re-prompt after the user has answered.
- **Rationale**: EMUI/HarmonyOS battery managers aggressively kill background
  accessibility services (FR-011, constitution VII).
- **Alternatives considered**: Prompt every launch. Rejected: annoying and violates the
  "once" requirement.

## R8. Build pipeline and output

- **Decision**: Use the Gradle wrapper only (`./gradlew assembleDebug`); output at
  `app/build/outputs/apk/debug/app-debug.apk`. No Android Studio required.
- **Rationale**: Matches the constitution's command-line build rule and the target
  environment.
- **Alternatives considered**: Installed global Gradle. Rejected: the wrapper pins the
  toolchain version and needs no machine setup.