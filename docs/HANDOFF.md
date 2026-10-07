# HeroClock development handoff

Author: PunctualBoat. Development version: **2.2.24** on `improve-clock-runtime`.

## Current direction

HeroClock keeps its internal clock and performance infrastructure. **Mantis is the separate KubeJS/Rhino replacement project.** Do not continue the removed HeroClock-owned scripting runtime or build a second engine/loader here.

The 2.2.20–2.2.21 experimental stack is removed: owned event namespaces and callbacks, Rhino runtime bindings, standalone server-script loading, Java/console globals, automatic native callback takeover, their compatibility contracts, registration counters, `HeroScriptRuntime`, loader/takeover API methods and `/heroclock script` commands. Their tests, fixtures and guides are removed too. The implementation remains recoverable through Git history; no history was rewritten.

HeroClock no longer reads `config/heroclock/server_scripts`. Existing user files are not deleted or automatically moved. Scripts or addons using the experimental APIs need migration to the separate scripting project.

## Retained systems

- Persistent mapped Minecraft timers, saved deadlines and lifecycle cleanup.
- Bounded/coalesced server work and deferred datapack functions.
- Guarded Palladium/Curios optimizations and existing addon cleanup adapters.
- Narrow KubeJS listener-append and Rhino map/overload-cache/member-map optimizations. Native loaders, event dispatch, bindings and timers remain upstream.
- Optional bounded boundary/hotspot profiling; detailed hooks stay startup-opt-in.
- Embedded developer API v1: `HeroClockAPI`, `HeroWorkAPI`, `HeroFunctionAPI`, `HeroIntegrationAPI` and the existing batch/profiling-only `HeroScriptAPI`. No loader/engine types are exposed. Mantis can use these APIs and own callback/cancellation lifecycle itself.
- Executable-code compatibility checks, including the later fix that fingerprints declared exceptions. Version labels alone do not enable a patch; changed or missing targets fall back independently.

See [INTEGRATION.md](INTEGRATION.md), [SCRIPTING.md](SCRIPTING.md) and [COMPATIBILITY.md](COMPATIBILITY.md). No Mantis dependency or speculative adapter is added. Runtime/API author metadata remains PunctualBoat.

## October 7 audit

The new profile and retained-patch review are recorded in [OPTIMIZATION_AUDIT_2026-10-07.md](OPTIMIZATION_AUDIT_2026-10-07.md). The capture does not list HeroClock, so it cannot validate HeroClock performance. Commands/selectors and suit-set enumeration now take priority over the earlier phasing target. The Satsu sentinel redirect has been removed after finding observable execution differences; its old shape-only test is replaced with an actual command-execution regression.

## Supplied-mod follow-up

The 23-JAR `Needed Mods.zip` is inventoried in [MOD_AUDIT_2026-10-07.md](MOD_AUDIT_2026-10-07.md). Ninjago and FSang resources provide concrete command/selector leads. OmniOptimizer 1.8.2 has no bytecode patch overlapping the new target, but seven AlienEvo power resources differ between the mods and still require effective-pack verification.

Version 2.2.24 adds guarded capacity hints for fresh PalladiumCore Architectury registry snapshots. It retains live traversal and original gameplay behavior. No suit/selector/gameplay result cache is introduced. The new eight-environment CI matrix requires thirteen GameTests and includes relabeled/changed PalladiumCore fixtures and Palladium without Architectury. This checkpoint is awaiting CI.

## Validation

The retained runtime is based on the pre-replacement 2.2.19 checkpoint, plus declared-exception fingerprinting. The preceding 2.2.22–2.2.23 matrix required twelve GameTests in seven environments: base Forge, supplied Palladium/Curios, relabeled Palladium, changed Palladium, supplied scripting, relabeled scripting and changed scripting targets. Packaging checks also reject the removed runtime/API/mixin classes.

The 2.2.22 removal checkpoint passed [CI run 37563048110](https://github.com/PunctualBoat203/HeroClock/actions/runs/37563048110) at `629507e1da658a480bac2d4184cad9904d4c2979`: build/unit/API packaging checks and all twelve required GameTests in each of seven environments. Optional-mod tests skip where dependencies are absent. [Runtime and developer API download](https://github.com/PunctualBoat203/HeroClock/actions/runs/37563048110/artifacts/11458136177). The 2.2.23 Satsu safety correction passed [CI run 37681532074](https://github.com/PunctualBoat203/HeroClock/actions/runs/37681532074) at `a43b5276b454c157b52ad781e5e16af3581c09b3`: unit/build/API checks and all twelve required GameTests in each of seven environments. [Download 2.2.23](https://github.com/PunctualBoat203/HeroClock/actions/runs/37681532074/artifacts/11509496450). See [VALIDATION.md](VALIDATION.md) for artifact details and target-pack checks. Target-pack TPS/FPS gains have not been measured.

## Next work

Continue safe clock/optimization work in HeroClock. Keep replacement scripting work in Mantis and integrate through the existing public API once its concrete requirements exist. Use [the supplied Spark analysis](SCRIPTING_PROFILE_2026-09-24.md) and [Astra priorities](ASTRA_OPTIMIZATION_TARGETS.md) for measured hotspots. Preserve callback cadence, mutable state, synchronization and errors. Commit/push checkpoints and keep the current PR draft pending target-pack validation.
