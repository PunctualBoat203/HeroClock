# Validation

Local pure-Java regression tests cover deadline arithmetic, legacy key normalization, unchanged immutable values versus mutable values, queue ordering, capacity, coalescing, cooperative time budgets, exceptions, cancellation, continuations and thread confinement. Owned listener tests additionally cover registration/removal during dispatch, argument isolation, listener bounds and recursion limits.

A separate GameTest source set exercises actual timers, NBT save/load, expiry, deferred steps, late helper tags, targeted block cleanup, datapack command permissions, cancelled/coalesced functions, Satsu function changes, Palladium caches and Curios backing-map replacement in headless Forge worlds. CI requires the explicit fourteen-test completion message because Forge can return exit code zero after a startup failure.

CI runs seven environments:

- HeroClock without optional mods.
- Hash-pinned Palladium 4.5.9 and Curios 5.14.1.
- The same Palladium code with only its version metadata changed to a synthetic `99.0.0`; matching optimizations must remain enabled. This is not a claim about an actual future release.
- Palladium with the private PowerHandler backing field renamed throughout its class; that optimization must stay disabled while other matching patches still work and the server completes its tests.

Three additional scripting environments exercise the exact supplied KubeJS/Rhino stack, unchanged code under synthetic new version labels and deliberately incompatible scripting targets. They check listener ordering/registration, live map IDs, original locking and exceptions, optional boundary measurements, JavaScript access to the embedded API and bounded callbacks across ticks. Rhino wrapper checks also cover all three cache-allocation constructor paths, live field reads, independent receivers/caches, zero/one-argument overloads, single-method storage, concurrent first-use publication and mutable scopes/prototypes. See [SCRIPTING.md](SCRIPTING.md) for the profile interpretation and supported developer hooks.

Owned-runtime checks also cover Java registration without scripting mods, datapack command-source forwarding, direct Rhino arguments/this, tick events, coalesced jobs, batch cancellation, setup failure cleanup and unload/reload. A separate lock-wait regression verifies that an invalidated scope cannot start a waiting JavaScript callback. Checks requiring an optional mod skip in environments without that mod.

Test classes, structures and modified dependency fixtures are excluded from the production artifacts. Palladium's nested libraries are extracted only for separate ForgeGradle development remapping. CI also checks that the compile-only API JAR contains only public facades/value records, while the runtime contains the implementation, contracts and refmap.

## Supplied stock-mod baselines

This development pass is intentionally scoped only to JARs actually supplied for inspection. Do not assume unprovided addon versions have been verified. Historical hashes and embedded Forge metadata are recorded in [the archived handoff](archive/2.2.15-HANDOFF.md); current contract inputs are recorded in [COMPATIBILITY.md](COMPATIBILITY.md).

The supplied set currently covers HeroClock 2.2.14, AlienEvo, Infinity, Infintrix, Satsu Iron Man Addon, Omni Evo, IntoTheOmniverse, CelestialSapien/MyPowers, Powerborne Heroes, Saiyan, PantheonSent, and OmniOptimizer 1.8.0.

The compatibility goal is **stock addon JAR + HeroClock**. Historical patched addon JARs are server-specific references, not a required deployment model. Generic optimizations must preserve gameplay semantics; targeted adapters should activate only when their audited prerequisites match and otherwise leave the addon untouched.

## Target-pack verification still required

A production release still needs the real target pack to verify:

1. Launch on Forge 47.x both with and without Palladium 4.5.9; check all mixins apply and no duplicate addon registrations occur.
2. Test the supplied stock addon JARs without server-patched copies and confirm HeroClock's generic/runtime benefits remain active.
3. Save/reload, chunk unload/reload, player death/respawn and dimension travel preserve expected remaining timer ticks.
4. Known temporary helpers clean up in every dimension, including partial block cleanup across unload/reload and saturated work queues.
5. Property changes reach clients; repeated equal scalar values do not send redundant packets; mutable properties continue syncing.
6. Datapack reload and world reconnect rebuild command caches correctly.
7. Exercise AlienEvo, Infinity, Infintrix, Satsu, Omni Evo, IntoTheOmniverse, CelestialSapien/MyPowers, Powerborne Heroes, Saiyan and PantheonSent gameplay paths that are actually present in target-pack. Watch for changed cadence, missing helper entities, stale effects, duplicate ability registration, command-function errors, and persistence differences.
8. Run the same representative workload before/after HeroClock and compare server tick-time percentiles, packet counts, helper-entity counts, and client frame-time behavior.
9. Verify OmniOptimizer 1.8.0 coexistence: overlapping work should not be duplicated, companion registration should succeed, and HeroClock's deadlines/bounded-work/Palladium optimizations should remain available.
10. Only after the runtime pass, promote any newly discovered mod-specific redirect from experimental/contract-gated to supported.

No live target-pack benchmark or in-game validation is claimed by the repository tests. The 1 ms work budget limits starting additional steps; it cannot bound an individual callback's execution time.

## 2.2.18 scripting hotspot checkpoint

[CI run 35982475439](https://github.com/PunctualBoat203/HeroClock/actions/runs/35982475439) passed at `bcb3b713c5864f06d194b94ab4565abdf300a8e6` on 2026-09-24: unit/build checks, byte-identical embedded API verification and all twelve required GameTests in each of seven environments. Both new Rhino patches enabled on supplied and relabeled code, then independently declined the changed-field fixtures.

An earlier candidate failed its constructor injection count; explicit audited constructor signatures corrected that failure, and runtime checks now exercise every allocation path. The scripting startup step has a diagnostic timeout so a Forge startup failure cannot hang indefinitely.

[Runtime and developer API artifacts](https://github.com/PunctualBoat203/HeroClock/actions/runs/35982475439/artifacts/10801266073) are development candidates. This establishes tested behavior in the fixtures, not target-pack TPS/FPS gains.

## 2.2.19 attribution checkpoint

[CI run 36290064908](https://github.com/PunctualBoat203/HeroClock/actions/runs/36290064908) passed at `62727337bc14065667587afe41ff7b0f500f7ff9`: build/unit/API packaging checks and all twelve required GameTests in each of seven environments. Detail hooks enabled on matching and relabeled scripting code, rejected the incompatible fixtures, and were omitted without the startup switch.

Tests cover receiver/collision counts, property labels, listener source attribution, original handled exceptions and event exits, disabled collection, immutable snapshots, bounded label cardinality/length and concurrent updates. [Runtime and API artifacts](https://github.com/PunctualBoat203/HeroClock/actions/runs/36290064908/artifacts/10921954054). This is a diagnostic checkpoint retaining the 2.2.18 optimizations, not evidence of additional performance gains.

## 2.2.20 owned-runtime checkpoint

[CI run 37526895955](https://github.com/PunctualBoat203/HeroClock/actions/runs/37526895955) passed on 2026-10-06 at `147e26c37ab59a193485cbfab224b3b73485e107`: build/unit checks, byte-identical standalone/embedded API verification and all fourteen required GameTests in each of seven environments. Subsequent commits update documentation only.

The owned Rhino binding enabled with supplied and relabeled scripting code. Its incompatible ScriptManager fixture rejected the binding and retained native KubeJS behavior. The Java runtime also passed without KubeJS/Rhino installed. Tests exercised setup failure cleanup without retries, immediate namespace validation, cancelled pending setup, callbacks invalidated while waiting for the context lock, unload/reload, direct callback arguments/this, tick events, coalesced scheduling and cancelled batches. Optional-mod tests skip when their dependencies are absent.

[Download the 2.2.20 runtime and developer API](https://github.com/PunctualBoat203/HeroClock/actions/runs/37526895955/artifacts/11443945071). Install only `HeroClock-2.2.20.jar` as a mod; the API JAR is compile-only and is also embedded as an inert resource. Artifact ZIP SHA-256: `8ebb63da780674545f90fd3f97e2a50c909f2045359752d1b5863d4849479b8d`.

This validates the integration and conservative fallback in the test fixtures. It does not establish target-pack TPS/FPS gains or compatibility with every custom loader/plugin. Use [OWNED_SCRIPTING.md](OWNED_SCRIPTING.md) for developer integration and the target-pack checks above before release.
