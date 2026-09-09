<!--
Sync Impact Report
==================
Version change: unresolved template -> 1.0.0
Bump rationale: Initial constitution. The prior file contained only unresolved
  template placeholders with no project-specific governance content.
Modified principles: none (all seven principles newly defined)
Added sections: Core Principles (7 principles), Technology Stack & Environment,
  Development Workflow
Removed sections: none
Follow-up TODOs: none
-->

# Short-Form Content Blocker (Android) Constitution

## Core Principles

### I. Ultra-Minimalism

The project MUST NOT use Jetpack Compose. UI MUST be defined with plain XML
layouts. Dependencies MUST be kept to an absolute minimum: a dependency is added
only when a feature cannot reasonably be implemented without it.

Rationale: a minimal dependency graph keeps the APK small, the build simple, and
the codebase buildable from the command line without Android Studio.

### II. Single-Activity, Single-Service Architecture

The app MUST contain exactly one MainActivity and exactly one AccessibilityService
(named BlockerService). All detection, cooldown, and navigation logic MUST live in
or be reachable from these two components. Additional UI screens MUST NOT introduce
new activities.

Rationale: an accessibility-based blocker must keep its control flow small and
auditable.

### III. Modular Detection

Each target app (YouTube, Instagram, Snapchat) MUST have its own isolated detector
class implementing a common detector interface. A detector MUST be updatable or
replaceable without modifying any other detector. Detectors MUST NOT share mutable
state with one another.

Rationale: target apps change frequently and independently; isolation confines the
blast radius of any change.

### IV. Performance & Safety

Before firing GLOBAL_ACTION_BACK, the service MUST enforce a debounce/cooldown of
800-1000ms per package to prevent action loops. AccessibilityNodeInfo objects MUST
be recycled after use (try/finally or an equivalent guaranteed path). The cooldown
MUST be tracked per target package, not globally.

Rationale: back-action loops can trap the user; unrecycled node info leaks memory.

### V. Privacy First

The app MUST have zero network access: it MUST NOT declare the INTERNET permission
and MUST NOT open any sockets. No analytics and no logging of content the user
views. Nothing the user watches may leave the device.

Rationale: a content blocker must not become a surveillance surface.

### VI. Resilience

Detection logic MUST fail gracefully and MUST be null-safe, because target apps
change their UI frequently (Snapchat most of all). A detector failure MUST degrade
to a no-op and MUST NOT crash the accessibility service.

Rationale: the service runs continuously in the background; a crash from a bad
assumption disables the whole app until the user restarts it.

### VII. Battery

On first launch, the app MUST prompt the user once to exclude it from battery
optimization (ACTION_IGNORE_BATTERY_OPTIMIZATIONS). The prompt MUST NOT be repeated
after the user has made a choice.

Rationale: aggressive OEM battery managers (Huawei EMUI in particular) kill
background AccessibilityServices, which would silently break the blocker.

## Technology Stack & Environment

- Language: Kotlin.
- Build system: Gradle Kotlin DSL (`build.gradle.kts`).
- Environment: built via VS Code and command-line Gradle. Android Studio MUST NOT be
  a required dependency for building.
- Min API 24 (to support older EMUI-based Huawei devices); Target API 34.
- Target devices: standard Android phones and Huawei phones running EMUI/HarmonyOS
  with Android app support. HarmonyOS NEXT (which drops Android app support) is NOT
  a target.

## Development Workflow

- Every generated file MUST compile successfully via `./gradlew assembleDebug` from
  the command line before it is considered complete.
- No commit that breaks the debug build is acceptable. If the build fails, the change
  is incomplete regardless of code review status.

## Governance

This constitution supersedes all other project practices and conventions. All
changes MUST comply with the principles above; any added complexity beyond them MUST
be explicitly justified in the feature spec or plan.

- Amendments MUST be documented with a Sync Impact Report and a semantic version bump.
- Versioning policy: MAJOR for principle removals or redefinitions; MINOR for new
  principles or sections; PATCH for clarifications and wording fixes.
- Compliance review: each feature and pull request MUST be checked against the seven
  Core Principles before it is accepted.

**Version**: 1.0.0 | **Ratified**: 2026-09-06 | **Last Amended**: 2026-09-06