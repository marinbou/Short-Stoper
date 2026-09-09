# Contract: ShortFormDetector interface

Every platform detector implements this contract. It is the single extension point for
adding a new short-form platform.

```kotlin
interface ShortFormDetector {
    /**
     * Return true when [root] represents the platform's short-form screen.
     * MUST be null-safe and MUST NOT throw: return false on any failure.
     * MUST recycle every AccessibilityNodeInfo obtained during traversal.
     */
    fun isShortFormScreen(root: AccessibilityNodeInfo): Boolean
}
```

## Rules for implementors

- `root` may be null; return `false`.
- No shared mutable state with other detectors.
- Recycle nodes via `try/finally`.
- **Signal precedence**: prefer specific `viewIdResourceName` markers (e.g. YouTube
  `reel`/`shorts`, Instagram `clips`, Snapchat `spotlight`) over raw text matching.
  Plain text/content-description matching causes false positives on persistent
  bottom-nav tabs ("Shorts", "Reels", "Spotlight") and home-feed shelves, which
  makes the target app exit as soon as it opens.
- Always exclude bottom-nav tabs: a nav tab is `clickable` and sits in the bottom
  strip of the window (see `AccessibilityTree.isInBottomBar`).
- Treat "any recognized signal" as a short-form screen (resilient to UI drift).
- A detector MUST NOT perform actions (no Back, no navigation); it only answers a
  boolean question.

## Conformance check

- Adding a detector for a new platform requires: one new class implementing this
  interface, plus one mapping entry in `BlockerService`. No changes to existing
  detectors or to the service's action logic.