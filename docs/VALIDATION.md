# Validation

Local pure-Java regression tests cover deadline arithmetic, legacy key normalization, unchanged immutable values versus mutable values, queue ordering, capacity, coalescing, cooperative time budgets, exceptions, cancellation, continuations and thread confinement.

A separate GameTest source set exercises the actual timer API, entity NBT save/load, expiry, deferred steps, late helper tags and targeted block cleanup inside a headless Forge world. CI runs it both without Palladium and with the exact public Palladium 4.5.9 artifact matching the project's test dependency pin. The latter also verifies the optional mixins are applied. Test classes and structures are excluded from the production JAR. The test setup extracts Palladium’s bundled libraries so ForgeGradle remaps each one for development; this does not alter the production Palladium installation. CI requires the explicit six-test completion message because Forge can return exit code zero after a startup failure.

## Supplied stock-mod baselines

This development pass is intentionally scoped only to JARs actually supplied for inspection. Do not assume unprovided addon versions have been verified. Exact hashes and embedded Forge metadata are recorded in [HANDOFF.md](HANDOFF.md).

The supplied set currently covers HeroClock 2.2.14, AlienEvo, Infinity, Infintrix, Satsu Iron Man Addon, Omni Evo, IntoTheOmniverse, CelestialSapien/MyPowers, Powerborne Heroes, Saiyan, PantheonSent, and OmniOptimizer 1.8.0.

The compatibility goal is **stock addon JAR + HeroClock**. Historical patched addon JARs are server-specific references, not a required deployment model. Generic optimizations must preserve gameplay semantics; version-specific adapters should activate only when their known target is present and otherwise leave the addon untouched.

## Astra 6 / target-pack verification still required

A production release still needs the real target pack to verify:

1. Launch on Forge 47.x both with and without Palladium 4.5.9; check all mixins apply and no duplicate addon registrations occur.
2. Test the supplied stock addon JARs without server-patched copies and confirm HeroClock's generic/runtime benefits remain active.
3. Save/reload, chunk unload/reload, player death/respawn and dimension travel preserve expected remaining timer ticks.
4. Known temporary helpers clean up in every dimension, including partial block cleanup across unload/reload and saturated work queues.
5. Property changes reach clients; repeated equal scalar values do not send redundant packets; mutable properties continue syncing.
6. Datapack reload and world reconnect rebuild command caches correctly.
7. Exercise AlienEvo, Infinity, Infintrix, Satsu, Omni Evo, IntoTheOmniverse, CelestialSapien/MyPowers, Powerborne Heroes, Saiyan and PantheonSent gameplay paths that are actually present in Astra 6. Watch for changed cadence, missing helper entities, stale effects, duplicate ability registration, command-function errors, and persistence differences.
8. Run the same representative workload before/after HeroClock and compare server tick-time percentiles, packet counts, helper-entity counts, and client frame-time behavior.
9. Verify OmniOptimizer 1.8.0 coexistence: overlapping work should not be duplicated, companion registration should succeed, and HeroClock's deadlines/bounded-work/Palladium optimizations should remain available.
10. Only after the runtime pass, promote any newly discovered mod-specific redirect from experimental/version-gated to supported.

No live Astra 6 benchmark or in-game validation is claimed by the repository tests. The 1 ms work budget limits starting additional steps; it cannot bound an individual callback's execution time.
