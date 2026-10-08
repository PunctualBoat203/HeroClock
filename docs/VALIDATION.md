# Validation

Local pure-Java regression tests cover deadline arithmetic, legacy key normalization, unchanged immutable values versus mutable values, queue ordering, capacity, coalescing, cooperative time budgets, exceptions, cancellation, continuations and thread confinement.

A separate GameTest source set exercises actual timers, NBT save/load, expiry, deferred steps, late helper tags, targeted block cleanup, datapack command permissions, cancelled/coalesced functions, synchronous Satsu execution and changed function bodies, Palladium caches and Curios backing-map replacement in headless Forge worlds. CI now requires the explicit fifteen-test completion message because Forge can return exit code zero after a startup failure.

CI runs eight environments:

- HeroClock without optional mods.
- Hash-pinned Palladium 4.5.9 and Curios 5.14.1 with Architectury 9.2.14.
- Palladium without Architectury, preserving the separate Forge registry path.
- The same Palladium and PalladiumCore code with only their version metadata changed to a synthetic `99.0.0`; matching optimizations must remain enabled. This is not a claim about an actual future release.
- Palladium with the private PowerHandler backing field and PalladiumCore registrar field renamed throughout their classes; that optimization must stay disabled while other matching patches still work and the server completes its tests.

Three additional scripting environments exercise the exact supplied KubeJS/Rhino stack, unchanged code under synthetic new version labels and deliberately incompatible scripting targets. They check listener ordering/registration, live map IDs, original locking and exceptions, optional boundary measurements, JavaScript access to the embedded API and bounded callbacks across ticks. Rhino wrapper checks also cover all three cache-allocation constructor paths, live field reads, independent receivers/caches, zero/one-argument overloads, single-method storage, concurrent first-use publication and mutable scopes/prototypes. See [SCRIPTING.md](SCRIPTING.md) for the profile interpretation and supported developer hooks.

Test classes, structures and modified dependency fixtures are excluded from the production artifacts. Palladium's nested libraries are extracted only for separate ForgeGradle development remapping. CI also checks that the compile-only API JAR contains only public facades/value records, while the runtime contains the implementation, contracts and refmap.

## Supplied stock-mod baselines

This development pass is intentionally scoped only to JARs actually supplied for inspection. Do not assume unprovided addon versions have been verified. Historical hashes and embedded Forge metadata are recorded in [the archived handoff](archive/2.2.15-HANDOFF.md); current contract inputs are recorded in [COMPATIBILITY.md](COMPATIBILITY.md).

The original supplied set covers HeroClock 2.2.14, AlienEvo, Infinity, Infintrix, Satsu Iron Man Addon, Omni Evo, IntoTheOmniverse, CelestialSapien/MyPowers, Powerborne Heroes, Saiyan, PantheonSent, and OmniOptimizer 1.8.0. The later 23-JAR archive includes OmniOptimizer 1.8.2, Ninjago and FSang; its exact inputs are recorded in [the current manifest](inputs/needed-mods-2026-10-07.json).

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
9. Verify coexistence with the installed OmniOptimizer version (the current audit includes 1.8.2): overlapping work should not be duplicated, companion registration should succeed, and HeroClock's deadlines/bounded-work/Palladium optimizations should remain available.
10. Only after the runtime pass, promote any newly discovered mod-specific redirect from experimental/contract-gated to supported.

No live target-pack benchmark or in-game validation is claimed by the repository tests. The 1 ms work budget limits starting additional steps; it cannot bound an individual callback's execution time.

## 2.2.18 scripting hotspot checkpoint

[CI run 35982475439](https://github.com/PunctualBoat203/HeroClock/actions/runs/35982475439) passed at `bcb3b713c5864f06d194b94ab4565abdf300a8e6` on 2026-09-24: unit/build checks, byte-identical embedded API verification and all twelve required GameTests in each of seven environments. Both new Rhino patches enabled on supplied and relabeled code, then independently declined the changed-field fixtures.

An earlier candidate failed its constructor injection count; explicit audited constructor signatures corrected that failure, and runtime checks now exercise every allocation path. The scripting startup step has a diagnostic timeout so a Forge startup failure cannot hang indefinitely.

[Runtime and developer API artifacts](https://github.com/PunctualBoat203/HeroClock/actions/runs/35982475439/artifacts/10801266073) are development candidates. This establishes tested behavior in the fixtures, not target-pack TPS/FPS gains.

## 2.2.19 attribution checkpoint

[CI run 36290064908](https://github.com/PunctualBoat203/HeroClock/actions/runs/36290064908) passed at `62727337bc14065667587afe41ff7b0f500f7ff9`: build/unit/API packaging checks and all twelve required GameTests in each of seven environments. Detail hooks enabled on matching and relabeled scripting code, rejected the incompatible fixtures, and were omitted without the startup switch.

Tests cover receiver/collision counts, property labels, listener source attribution, original handled exceptions and event exits, disabled collection, immutable snapshots, bounded label cardinality/length and concurrent updates. [Runtime and API artifacts](https://github.com/PunctualBoat203/HeroClock/actions/runs/36290064908/artifacts/10921954054). This is a diagnostic checkpoint retaining the 2.2.18 optimizations, not evidence of additional performance gains.

## 2.2.22: HeroClock/Mantis split

[CI run 37563048110](https://github.com/PunctualBoat203/HeroClock/actions/runs/37563048110) passed at `629507e1da658a480bac2d4184cad9904d4c2979`: build/unit checks, byte-identical standalone/embedded API verification and all twelve required GameTests in each of seven environments. Optional-mod checks skip when their dependencies are absent. Subsequent checkpoint changes are documentation-only.

The custom owned runtime, standalone loader, automatic callback takeover and associated APIs/commands are removed. Packaging checks reject the removed scripting implementation, runtime, mixin and public runtime API classes. The retained clock/work/optimization implementation matches the pre-replacement 2.2.19 baseline, with the later declared-exception fingerprint fix and regenerated contracts. Supplied/relabeled scripting and Palladium targets remain supported; changed targets retain stock behavior, and base Forge works without optional mods.

[Download the 2.2.22 runtime and developer API](https://github.com/PunctualBoat203/HeroClock/actions/runs/37563048110/artifacts/11458136177). Install only `HeroClock-2.2.22.jar` as a mod; the API JAR is compile-only and also embedded as an inert resource. Artifact ZIP SHA-256: `29d26a9cd7fb5630fe6e54420ad4e432d820ca0c9e26ee88e4acb38b6173c72b`.

Mantis is a separate scripting replacement project, not part of this build. Existing experimental standalone script files are left untouched and no longer loaded. Target-pack behavior and performance still require the checks above.

## 2.2.23: profile audit and Satsu execution correction

[CI run 37681532074](https://github.com/PunctualBoat203/HeroClock/actions/runs/37681532074) passed at `a43b5276b454c157b52ad781e5e16af3581c09b3` on 2026-10-07: unit/build/API packaging checks and all twelve required GameTests in each of seven environments. This includes supplied, relabeled and changed scripting/Palladium targets and base Forge. Optional-mod checks skip when dependencies are absent. Subsequent checkpoint changes are documentation-only.

The former Satsu shape-only check is replaced by actual function execution: an exact matching function must kill synchronously and return one executed command; a changed function must retain both commands, source and followup. The broad cancellation and deferred sentinel scheduling are removed. See [the profile and optimization audit](OPTIMIZATION_AUDIT_2026-10-07.md) for the defect, current priorities and remaining validation limits.

[Download the 2.2.23 runtime and developer API](https://github.com/PunctualBoat203/HeroClock/actions/runs/37681532074/artifacts/11509496450). Install only `HeroClock-2.2.23.jar` as a mod. Artifact ZIP SHA-256: `d83bde266166069cdc5c4d3339111646a377a82d4acb751364a18578269dae63`. The attached profile does not list HeroClock; no before/after TPS or FPS gain is claimed.

## 2.2.24: bounded registry capacity hints

[CI run 37695224729](https://github.com/PunctualBoat203/HeroClock/actions/runs/37695224729) passed at `ac7ba57279fecf30a89eda4bf7bb01137450e817`: unit/build/API packaging checks and all thirteen required GameTests in each of eight environments. Optional-mod tests skip where their dependencies are absent. Subsequent checkpoint edits are documentation-only.

The new registry test verifies live reads, independent mutable snapshots, order, size growth/shrinkage, equal-size replacement and exception propagation. The new contract enables unchanged/relabelled PalladiumCore and declines a changed registrar field. Palladium also completes its runtime tests without Architectury. [Supplied-mod findings and scope](MOD_AUDIT_2026-10-07.md).

[Download 2.2.24 runtime and developer API](https://github.com/PunctualBoat203/HeroClock/actions/runs/37695224729/artifacts/11515826153). Install only `HeroClock-2.2.24.jar` as a mod. Artifact ZIP SHA-256: `74fa01c8ce2ecc3173527a8bb95f91db4c4d7b37e7df160961842be9662e5a3c`. This is an allocation reduction with tested semantics, not a measured target-pack TPS/FPS result.

## 2.2.25: resource conflicts and redundant selector scans

[CI run 37698770315](https://github.com/PunctualBoat203/HeroClock/actions/runs/37698770315) passed at `b3af6fe206c5e72e288bd879bbd4ee322246cb64`: unit/build/API packaging checks and all fifteen required GameTests in each of eight environments. Optional-mod checks skip where dependencies are absent. [2.2.25 runtime and developer API](https://github.com/PunctualBoat203/HeroClock/actions/runs/37698770315/artifacts/11516489394). Subsequent checkpoint edits are documentation-only. Local fingerprint tests pass, and all seven production resource hashes match the supplied OmniOptimizer 1.8.2 JAR bytes.

The new resource regression uses Minecraft's actual resource manager with synthetic pack fixtures at all seven conflict paths. It checks standalone fallback, both mod-resource orders, external datapack priority, direct reads and discovery, retained namespaces/metadata/client assets/unrelated resources, both load tags, one changed resource and absent resources. It also verifies the transformed Forge factory hook and actual bundled standalone powers. This does not launch the full supplied gameplay pack.

The command regression executes the bundled ice-projectile selector with a test function and compares it with the old guarded command for zero, one and multiple matches, distance exclusions, executor identity, return values and permission denial. The player and scored-kill variants remove only the same redundant existence precondition; their original downstream selectors and commands remain. The test does not claim full AlienEvo ability coverage.

The first final-revision CI attempt stopped before compilation when Gradle's plugin repository could not supply three dependencies. The same commit passed on retry; no test requirement was removed. Install only `HeroClock-2.2.25.jar` as a mod; the standalone API JAR is compile-only. Artifact ZIP SHA-256: `e36ba97dc0f3c898234c928b8c411a24bc769cee3b0f9ce91f9fad00d218c264`. Target-pack TPS/FPS improvements remain unmeasured.
