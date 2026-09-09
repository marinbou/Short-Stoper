# Data Model: Short-Form Content Blocker

The app is intentionally data-minimal. It persists no user content and performs no
network I/O. The "entities" below are the in-memory/state concepts the system manages.

## TargetPlatform

- Represents one supported app whose short-form feed must be exited.
- **Fields**: `packageName` (stable identifier, e.g. `com.google.android.youtube`),
  display name, and a reference to its dedicated detector.
- **Instances**: YouTube (Shorts), Instagram (Reels), Snapchat (Spotlight).
- **Validation**: The package name MUST be one of the three configured packages.
- **Relationship**: Each platform maps to exactly one `ShortFormDetector`.

## ShortFormDetector (behavioral contract)

- One per `TargetPlatform`. Exposes `isShortFormScreen(root): Boolean`.
- MUST be stateless with respect to other detectors (no shared mutable state).

## CooldownState

- Tracks the last Back-action timestamp per package to enforce the 800-1000ms debounce.
- **Fields**: `packageName -> lastActionElapsedRealtime (Long)`.
- **State transition**:
  - `idle` -> `acted` when Back fires for a package.
  - `acted` -> `idle` after the cooldown window elapses.
- **Validation**: A Back action MUST NOT fire for a package whose elapsed time since
  `lastActionElapsedRealtime` is below the cooldown floor.

## MonitoringState

- Derived (not persisted) at dashboard resume time from the OS enabled-services list.
- **Values**: `enabled` / `disabled`.
- **Relationship**: Drives the dashboard toggle and status text.

## BatteryPromptFlag

- Persisted in `SharedPreferences`; a single boolean `battery_prompt_shown`.
- **Transition**: `false` -> `true` once the user has been shown (and answered) the
  battery-optimization prompt. Never resets.