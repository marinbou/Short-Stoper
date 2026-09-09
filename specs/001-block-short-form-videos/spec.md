# Feature Specification: Short-Form Content Blocker

**Feature Branch**: `001-block-short-form-videos`

**Created**: 2026-09-06

**Status**: Draft

**Input**: User description: "App Concept: Short-Form Content Blocker. Purpose: Instantly exit short-form video feeds (YouTube Shorts, Instagram Reels, Snapchat Spotlight) to reduce compulsive scrolling. Core Features: Background Monitoring, Auto-Escape, Simple Dashboard, Per-App Extensibility. Non-Goals: No internet access, no ads, no data collection, sideloaded APK."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Instantly Exit Short-Form Feeds (Priority: P1)

A user opens YouTube, Instagram, or Snapchat and taps into the short-form feed
(Shorts, Reels, or Spotlight). Within moments, the app automatically takes them back
out to the normal feed, interrupting the compulsive scroll without any manual action.

**Why this priority**: This is the core value of the product. Everything else exists
to support or configure this behavior. Without auto-escape, there is no product.

**Independent Test**: Can be fully tested by enabling monitoring, opening a target
app, entering its short-form feed, and confirming the user is returned to the normal
feed automatically. Delivers the primary value proposition on its own.

**Acceptance Scenarios**:

1. **Given** monitoring is enabled and the user is on YouTube, **When** the user opens
   YouTube Shorts, **Then** the app automatically returns the user to the normal
   YouTube feed.
2. **Given** monitoring is enabled and the user is on Instagram, **When** the user
   opens Reels, **Then** the app automatically returns the user to the normal
   Instagram feed.
3. **Given** monitoring is enabled and the user is on Snapchat, **When** the user opens
   Spotlight, **Then** the app automatically returns the user to the normal Snapchat
   feed.
4. **Given** monitoring is enabled and the user is in any other app, **When** the user
   browses normally, **Then** nothing is blocked or exited.

---

### User Story 2 - Enable Monitoring and See Its Status (Priority: P2)

A user opens the app for the first time and sees the logo and an OFF toggle. They tap
the toggle, are taken to the device's accessibility settings, and enable the service.
Returning to the app, the toggle now shows ON, confirming monitoring is active.

**Why this priority**: Monitoring cannot function until the user grants the required
system permission. A clear, simple enable flow is the gate to the core experience and
the primary onboarding moment.

**Independent Test**: Can be fully tested by installing the app, opening it, toggling
the switch, granting the permission in system settings, and confirming the toggle
reflects ON after returning. Delivers the complete setup experience on its own.

**Acceptance Scenarios**:

1. **Given** the user opens the app for the first time, **When** the dashboard is
   displayed, **Then** the app shows the logo and a toggle in the OFF position.
2. **Given** the toggle is OFF, **When** the user taps the toggle, **Then** the device's
   accessibility settings screen opens so the user can grant permission.
3. **Given** the user has enabled monitoring in system settings, **When** the user
   returns to the app, **Then** the toggle shows ON.

---

### User Story 3 - Add a New Short-Form Platform Without Rework (Priority: P3)

A maintainer decides to support a new short-form platform. They add detection for that
platform only, without modifying or risking the existing YouTube, Instagram, and
Snapchat behavior.

**Why this priority**: It does not change the end-user experience for the initial
release, but it protects future development cost and is an explicit product
requirement for how the feature is built.

**Independent Test**: Can be fully tested by adding detection for a new platform and
verifying that (a) the new platform is detected and exited, and (b) the three existing
platforms continue to behave exactly as before.

**Acceptance Scenarios**:

1. **Given** the app supports the three original platforms, **When** detection for a
   new platform is added, **Then** the existing platforms continue to work unchanged.
2. **Given** detection for a new platform has been added, **When** a user enters that
   platform's short-form feed, **Then** the user is automatically returned to the
   normal feed.

---

### Edge Cases

- What happens when the user is inside a target app but not on a short-form feed?
  Nothing is blocked; normal browsing is unaffected.
- What happens when the user re-enters a short-form feed immediately after being
  exited? The app must not loop; it should exit once per entry and enforce a pause
  before acting again.
- What happens when a target app changes its interface (e.g., a UI redesign)?
  Detection fails gracefully with no crash and no action taken.
- What happens if the operating system kills or disables monitoring (e.g., aggressive
  battery management)? The user is prompted once to exclude the app from battery
  optimization, and the toggle reflects the current state so they can re-enable it.
- What happens when the user revokes the accessibility permission while the app is in
  use? The toggle shows OFF the next time the app is opened.
- What happens if a target app is not installed on the device? The app still works for
  the platforms that are installed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST monitor screen activity only within YouTube, Instagram, and
  Snapchat and MUST take no action in any other app.
- **FR-002**: The app MUST automatically return the user to the normal feed when a
  short-form feed (Shorts, Reels, or Spotlight) is detected.
- **FR-003**: The app MUST exit a short-form feed at most once per entry and MUST
  enforce a cooldown between consecutive auto-exits to prevent loops.
- **FR-004**: The dashboard MUST display the app logo.
- **FR-005**: The dashboard MUST display a single ON/OFF toggle that reflects whether
  monitoring is currently enabled.
- **FR-006**: Tapping the toggle MUST open the device's accessibility settings screen
  so the user can grant the required permission.
- **FR-007**: When the user returns to the app, the toggle MUST reflect the current
  enabled state.
- **FR-008**: The app MUST support adding a new short-form platform without modifying
  or breaking existing platform detection.
- **FR-009**: The app MUST operate with no internet connectivity and MUST NOT collect,
  store, or transmit any usage data.
- **FR-010**: Detection MUST fail gracefully (no crash, no action) when a target app's
  interface is unrecognized or changed.
- **FR-011**: The app MUST prompt the user once to exclude it from battery optimization
  to keep monitoring reliable.

### Key Entities *(include if feature involves data)*

- **Target Platform**: One of the supported apps (YouTube, Instagram, Snapchat), each
  with its own short-form feed to detect.
- **Monitoring State**: Whether monitoring is currently enabled for the device,
  reflected by the dashboard toggle.
- **Blocking Event**: A single detection-and-exit action, governed by a cooldown so it
  cannot repeat in a loop.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user entering a short-form feed is returned to the normal feed within
  2 seconds of the feed appearing.
- **SC-002**: The app performs exactly one auto-exit per short-form entry, with no
  repeated or looping exits while the user remains in the normal feed.
- **SC-003**: 100% of monitoring activity is confined to the three target apps; zero
  actions are taken in any other app.
- **SC-004**: Adding a new platform requires changes to detection for that platform
  only, with existing platforms' behavior verified unchanged.
- **SC-005**: A new user can complete initial setup (install, open, enable monitoring)
  in under 1 minute.
- **SC-006**: The app makes zero network transmissions and collects zero user data,
  verifiable by inspecting the app's declared permissions and behavior.

## Assumptions

- The app is distributed by sideloading the APK, not via the Play Store.
- Monitoring depends on the user granting the device's accessibility permission, which
  only the user can enable.
- Both the short-form feed and a full-screen short-form video player count as
  "short-form" screens that should be exited.
- "Normal feed" refers to each target app's primary/home feed that the user lands on
  after leaving the short-form screen.
- The device runs standard Android or Huawei EMUI/HarmonyOS with Android app support.