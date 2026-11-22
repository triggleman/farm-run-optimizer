# Agent Guidelines — Farm Run Optimizer

RuneLite plugin. Java 11, source in `src/main/java/com/herbrunhelper`, tests in
`src/test/java/com/herbrunhelper`.

## Build, test and run

- Build and test: `.\gradlew.bat build --no-daemon`
- Launch the development client: `.\gradlew.bat run --no-daemon`
- Never claim an in-game feature is verified from a build alone. Tell the user exactly what to
  test and wait for their confirmation.

## Logging

- Use `log.debug()` for diagnostics.
- Never log per-frame or per-event messages at INFO. INFO is reserved for one-time
  startup/shutdown or genuinely infrequent events.

## Threading

- Never use `Thread.sleep()`.
- Never block `startUp()` or `shutDown()`; cancel scheduled work and use `shutdownNow()`.
- No blocking network or disk I/O on the client thread. Call back into the game through
  `clientThread.invoke()`.
- Use `CompletableFuture.allOf()` for batching async work, not `CountDownLatch`.

## Performance

- Do not scan the whole scene every tick or frame. Track objects/NPCs from spawn/despawn events.
- Keep per-frame overlay work small and allocation-free where practical.

## API

- Use `net.runelite.api.gameval` constants (`InterfaceID`, `ItemID`, `VarbitID`, `SpriteID`, ...)
  instead of magic numbers when a constant exists.
- Look widgets up by their component ID from gamevals, not by manually combining group/child IDs.
- Use `LinkBrowser` for URLs. Reflection, JNI/JNA, native memory and external processes are
  forbidden.
- Use OkHttp and the injected `Gson`; do not add transitive RuneLite dependencies to
  `build.gradle`.

## Config

- Keep the config group specific and never rename a group/key without a migration.
- Third-party-server features must be opt-in and carry the required warning.

## Packaging and assets

- Keep Java 11 compatibility and the example-plugin structure.
- Do not commit build artifacts.
- Keep icon PNGs small and make sure files with a `.png` extension really are PNGs.
- Keep a permissive licence (BSD-2 in this repo).

## Cleanup

- Remove unused fields, methods, config keys and imports.
- Remove comments that only restate the code or describe an outdated behaviour.
- Unregister overlays, listeners and subscriptions in `shutDown()`.
- Do not mix wholesale reformatting with feature changes.

## Plugin Hub restrictions

The plugin must follow Jagex's third-party client guidelines and RuneLite's rejected/rolled-back
features rules. In particular: no input injection, no autotyping, no combat/boss automation or
predictions, no PvP scouting/indicators, no added menu actions that send server actions, no
unhiding hidden interface components, no moving combat/inventory/equipment/spellbook click zones,
and no crowdsourcing or exposing player data.
