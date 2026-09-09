# Quickstart: Short-Form Content Blocker

Validation guide. Build and verify the app end-to-end without Android Studio.

## Prerequisites

- JDK 17+ (the Gradle wrapper downloads the correct Gradle version automatically).
- Android SDK with platform 34 and build tools installed; `ANDROID_HOME` or
  `sdk.dir` in `local.properties` pointing at it.
- A device/emulator running Android 7.0 (API 24) or later (Huawei EMUI/HarmonyOS with
  Android app support works).

## Build

```bash
./gradlew assembleDebug
```

Expected output APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Verify the dashboard and enable flow

1. Open the app. Expect: the logo, a status line, and a toggle in the OFF position.
2. Tap the toggle. Expect: the device's Accessibility settings open.
3. Find this app's service and enable it.
4. Return to the app. Expect: the toggle now shows ON.

## Verify auto-escape

1. Open YouTube and tap into Shorts. Expect: within ~2 seconds you are returned to the
   normal YouTube feed, exactly once (no repeated backs).
2. Open Instagram and tap into Reels. Expect: returned to the normal feed once.
3. Open Snapchat and tap into Spotlight. Expect: returned to the normal feed once.
4. Browse any other app. Expect: nothing is exited or blocked.

## Verify privacy and resilience

- Confirm the installed APK requests no INTERNET permission (e.g., via
  `adb shell dumpsys package <package>` or the device's app-info Permissions screen).
- In a target app, navigate to a screen that is not a short-form feed. Expect: no Back
  is triggered.