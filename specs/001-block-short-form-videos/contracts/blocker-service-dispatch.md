# Contract: BlockerService event dispatch

The accessibility service is the only actor that performs the Back action.

## Dispatch flow (per accessibility event)

1. Read `event.packageName`.
2. If `packageName` is not one of the three configured packages, ignore the event.
3. Otherwise select the detector mapped to that package.
4. Check the per-package cooldown; if within 800-1000ms of the last Back for that
   package, ignore the event.
5. Call `detector.isShortFormScreen(root)`.
   - `true` -> record the action time, then `performGlobalAction(GLOBAL_ACTION_BACK)`.
   - `false` or any exception -> do nothing.
6. Recycle the root node in `finally`.

## Guarantees

- At most one Back per package per cooldown window.
- No Back in any app outside the configured package list.
- A detector exception never crashes the service.