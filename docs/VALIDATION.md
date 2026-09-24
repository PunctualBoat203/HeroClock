# Validation

Local pure-Java regression tests cover deadline arithmetic, legacy key normalization, unchanged immutable values versus mutable values, queue ordering, capacity, coalescing, cooperative time budgets, exceptions, cancellation, continuations and thread confinement.

A separate GameTest source set exercises actual timers, NBT save/load, expiry, deferred steps, late helper tags, targeted block cleanup, datapack command permissions, cancelled/coalesced functions, Satsu function changes, Palladium caches and Curios backing-map replacement in headless Forge worlds. CI requires the explicit twelve-test completion message because Forge can return exit code zero after a startup failure.

CI runs seven environments:

- HeroClock without optional mods.
- Hash-pinned Palladium 4.5.9 and Curios 5.14.1.
- The same Palladium code with only its version metadata changed to a synthetic `99.0.0`; matching optimizations must remain enabled. This is not a claim about an actual future release.
- Palladium with the private PowerHandler backing field renamed throughout its class; that optimization must stay disabled while other matching patches still work and the server completes its tests.

Three additional scripting environments exercise the exact supplied KubeJS/Rhino stack, unchanged code under synthetic new version labels and deliberately incompatible scripting targets. They check listener ordering/registration, live map IDs, original locking and exceptions, optional boundary measurements, JavaScript access to the embedded API and bounded callbacks across ticks. See [SCRIPTING.md](SCRIPTING.md) for the profile interpretation and supported developer hooks.

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
