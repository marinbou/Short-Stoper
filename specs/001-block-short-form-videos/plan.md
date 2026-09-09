# Implementation Plan: Short-Form Content Blocker

**Branch**: `001-block-short-form-videos` | **Date**: 2026-09-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-block-short-form-videos/spec.md`

## Summary

Build a single-activity Android app that silently monitors YouTube, Instagram, and
Snapchat via one AccessibilityService, automatically fires the system Back action to
exit Shorts/Reels/Spotlight screens, and provides a minimal dashboard to enable
monitoring and reflect its live status. Detection is isolated per platform behind a
common `ShortFormDetector` interface so new platforms can be added without touching
existing ones. The app is fully offline (no INTERNET permission), uses plain XML
layouts (no Compose), and ships as a sideloaded debug APK.

## Technical Context

**Language/Version**: Kotlin (JVM target for Android)

**Primary Dependencies**: Kotlin stdlib; AndroidX `appcompat` and `core-ktx` only.
No Compose, no networking, no analytics libraries.

**Storage**: `SharedPreferences` for a single "battery prompt shown" flag. No other
persistence; no user-content storage.

**Testing**: Build verification via `./gradlew assembleDebug`; manual on-device
verification for detection behavior. No automated UI test framework in scope.

**Target Platform**: Android, min API 24, target API 34. Standard Android and Huawei
EMUI/HarmonyOS (with Android app support); not HarmonyOS NEXT.

**Project Type**: mobile-app (Android)

**Performance Goals**: Detect and exit a short-form screen within 2 seconds of it
appearing; debounce/cooldown of 800-1000ms per package between Back actions.

**Constraints**: Zero network access (no INTERNET permission); exactly one
`MainActivity` and one `BlockerService`; plain XML layouts only; detectors must be
null-safe and recycle all `AccessibilityNodeInfo` objects.

**Scale/Scope**: 3 target platforms initially (YouTube, Instagram, Snapchat); ~6
Kotlin source files; one activity + one accessibility service.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Ultra-Minimalism | PASS | XML layouts only; no Compose; minimal AndroidX deps. |
| II. Single-Activity, Single-Service | PASS | Exactly `MainActivity` + `BlockerService`. |
| III. Modular Detection | PASS | `ShortFormDetector` interface + one detector per platform. |
| IV. Performance & Safety | PASS | 800-1000ms per-package debounce; node recycling; try/finally. |
| V. Privacy First | PASS | No INTERNET permission; no logging/analytics. |
| VI. Resilience | PASS | Null-safe detectors; failure degrades to no-op. |
| VII. Battery | PASS | One-time `ACTION_IGNORE_BATTERY_OPTIMIZATIONS` prompt. |

## Project Structure

### Documentation (this feature)

```text
specs/001-block-short-form-videos/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/wrapper/                     # Gradle wrapper (gradlew, gradlew.bat, jar, properties)
app/
├── build.gradle.kts
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/shortstoper/app/
    │   ├── MainActivity.kt
    │   ├── BlockerService.kt
    │   └── detection/
    │       ├── ShortFormDetector.kt
    │       ├── YouTubeShortsDetector.kt
    │       ├── InstagramReelsDetector.kt
    │       └── SnapchatSpotlightDetector.kt
    └── res/
        ├── layout/activity_main.xml
        ├── values/strings.xml
        ├── values/colors.xml
        ├── values/themes.xml
        ├── xml/accessibility_service_config.xml
        ├── drawable/ic_logo.xml
        └── mipmap-*/ic_launcher.png (adaptive icon)
```

**Structure Decision**: Single Android module (`app/`) using the standard Gradle
Android project layout. Detection classes live in a `detection/` subpackage to keep
the service thin and each platform isolated.

## Complexity Tracking

> No Constitution Check violations; this section intentionally left empty.