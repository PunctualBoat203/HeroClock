# HeroClock

Forge 1.20.1 timing, bounded work, and targeted Palladium compatibility by **PunctualBoat**.

## Development build: 2.2.17

This source tree was originally recovered from the supplied 2.2.10 JAR. The released **HeroClock 2.2.14** JAR has now also been supplied and hash-verified, so it is the release-behavior/regression reference for this branch even though its original source snapshot was not present in Git. See [recovery provenance](docs/RECOVERY.md) and the [development handoff](docs/HANDOFF.md).

Build with Java 17:

```sh
python tools/prepare_scripting_test.py
./gradlew test build
```

The reobfuscated mod is `build/libs/HeroClock-2.2.17.jar`. CI uploads the built JAR and test reports. Palladium and Curios are optional; each targeted mixin requires an audited code contract, independent of version labels. Palladium, KubeJS, Rhino and Architectury dependency classes/JARs are not redistributed inside HeroClock. The preparation script fetches hash-pinned compile/test dependencies matching the supplied scripting builds.

## Changes

- Direct mapped Minecraft timer access replaces reflective lookups that could silently report time zero in production. Deadlines retain the `HeroClockTimers` NBT layout, use the server overworld's saved game time across dimensions, and saturate on overflow. Player clones copy timers; block-entity writes mark storage dirty.
- `HeroWorkAPI` supports cancellable, keyed, coalesced work beyond timers. Jobs execute on the server thread with a 4,096-entry capacity, 128-step limit, and 1 ms admission budget per tick. A running callback cannot be preempted; callers must keep each step small.
- Known temporary helpers use entity lifecycle hooks and saved deadlines. The old recurring global-selector function is unscheduled. Block cleanup checks at most 256 positions per step, waits for loaded chunks, and never forces chunk loads. Queue saturation retries cleanup; entity unload cancels its queued work. Saved deadlines restore cleanup when entities load again.
- Compatible Palladium targets skip redundant scalar-property sync packets, reuses its read-only power-holder view, and invalidates parsed command caches when the dispatcher changes. Mutable property values still sync normally; ability ticks and command execution cadence are preserved.
- Duplicate Omni Evo/IntoTheOmniverse script overlays are inactive. Historical server-specific patch payloads remain reference material rather than a requirement for stock addon JARs.
- The forward compatibility direction is **stock addon JAR + HeroClock**: generic safe optimizations stay in HeroClock's runtime layer, while code-checked addon adapters may redirect known hotspots through HeroClock deadlines, bounded work, cleanup, or sync-deduplication systems without rewriting the addon JAR.
- Version checks use Forge's loaded metadata instead of reopening every installed mod JAR.

See [API and behavior](docs/API.md), [validation](docs/VALIDATION.md), and [handoff](docs/HANDOFF.md). This is a development candidate, not a claim of measured TPS/FPS gains or zero lag.

## Integration and safeguards

2.2.17 checks each targeted method body and required field contract before enabling its optional patch, and checks Satsu's effective tick function after reloads. Changed or unknown targets keep original behavior. See [compatibility contracts](docs/COMPATIBILITY.md).

A separate `HeroClock-2.2.17-api.jar` exposes supported timer, bounded-work, deferred-function and diagnostic facades for addon mods. Datapacks and addonpacks can use `/heroclock timer`, `/heroclock work` and `/heroclock status`. See [integration API v1](docs/INTEGRATION.md). The API artifact excludes implementation classes and is compile-only.

KubeJS/Rhino support and the embedded scripting API are described in [docs/SCRIPTING.md](docs/SCRIPTING.md).
