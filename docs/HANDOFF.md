# HeroClock development handoff

Author: PunctualBoat. Current development version: **2.2.16** on `improve-clock-runtime`.

This pass continues the pushed 2.2.15 pause checkpoint `d66a66d`, preserving its persistent timers, bounded work, Palladium property lookup cache, Satsu redirect, Curios view reuse and public companion handshake. Historical artifact hashes, Spark findings and 2.2.15 validation are retained in [the archived handoff](archive/2.2.15-HANDOFF.md).

## Current changes

- Optional mixins are selected separately by audited target-method fingerprints and required fields, replacing exact-version allowlists. Missing/changed targets retain base behavior. Version metadata remains context for historical resource baselines.
- Satsu's actual loaded function is validated and rechecked on reload, including before queued cleanup executes.
- Curios view reuse follows backing-map identity. Palladium lookup caching invalidates on registration/reload and yields to live lookups after the mutable values map is exposed.
- API v1 exposes timers, bounded jobs, deferred functions and immutable diagnostic snapshots. CI still produces a standalone compile-only API JAR, and the exact same API JAR is now embedded inertly at `META-INF/heroclock/HeroClock-2.2.16-api.jar` inside the runtime mod so developers can extract it from the distributed HeroClock JAR. It is not registered as a Forge JarJar dependency.
- Datapack commands provide timer and function-work access without Java integration.
- HeroClock uses PunctualBoat attribution. The companion handshake discovers its API from the installed entrypoint package instead of hardcoding a vendor namespace.

## Validation boundaries

Unit tests cover compatibility acceptance/rejection alongside clock and work-queue edge cases. CI builds a reobfuscated runtime JAR, verifies that the embedded API is byte-for-byte identical to the standalone API artifact and remains non-JarJar, then runs Forge GameTests. Target-pack save/reload, equipment, client sync and performance checks remain necessary before a release. A structural match does not prove every interaction with other mods safe. Static historical resource overrides are not automatically authorized for arbitrary addon updates by the Java compatibility guards.

See [compatibility](COMPATIBILITY.md), [integration](INTEGRATION.md) and [validation](VALIDATION.md). Keep commits incremental and retain stock behavior when a new optimization cannot prove its own prerequisites. Do not broadly throttle arbitrary script callbacks or cache mutable gameplay results.
