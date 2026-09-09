# Contract: Accessibility service configuration

Declared in `res/xml/accessibility_service_config.xml` and linked from the manifest
via `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`.

## Config shape

- `accessibilityEventTypes`: `typeWindowStateChanged` and
  `typeWindowContentChanged`.
- `accessibilityFeedbackType`: `feedbackGeneric`.
- `packageNames`: `com.google.android.youtube`, `com.instagram.android`,
  `com.snapchat.android`.
- `canRetrieveWindowContent`: `true`.

## Guarantees

- The OS delivers events only for the three configured packages.
- The service cannot read content from any other app.