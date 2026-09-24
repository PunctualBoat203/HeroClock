# HeroClock scripting development handoff

Author: PunctualBoat. Development version: **2.2.18** on `improve-clock-runtime`.

This pass starts from the pushed 2.2.16 embedded-API baseline (`20a3411`, with runtime packaging introduced in `bd8a777`). The previous release artifact and real-pack findings remain in [the archived handoff](archive/2.2.16-HANDOFF.md). This pass changes only KubeJS/Rhino integration and its shared compatibility/API/test infrastructure.

## Implemented

- Individually guarded KubeJS listener append and Rhino map-enumeration optimizations.
- Optional, guarded measurements around Rhino calls and KubeJS handler chains; disabled by default, with original locking and exception propagation retained.
- Embedded `HeroScriptAPI` for bounded iterator batches, namespace cancellation and immutable profiling snapshots. The API remains byte-identical to the standalone compile-only artifact and inert inside the runtime JAR.
- Hash-pinned scripting fixtures from the exact supplied KubeJS/Rhino artifacts, including relabeled and incompatible-code variants.

Read [SCRIPTING.md](SCRIPTING.md) for behavior, profile evidence and limitations. The latest Astra performance priorities and safety gates are tracked in [ASTRA_OPTIMIZATION_TARGETS.md](ASTRA_OPTIMIZATION_TARGETS.md). The supplied Spark capture identifies wrapping, interpreter and callback work; it does not establish an improvement from this development build. Arbitrary callback suppression, global wrapper reuse and native-scheduler replacement remain inappropriate without preserving their observable semantics.

## Validation

CI builds the runtime and embedded API, then tests base Forge, existing Palladium/Curios cases, the supplied scripting stack, unchanged scripting code with altered version labels and intentionally incompatible scripting targets. Scripting checks cover listener order, descendant appends, clearing/re-registration, map enumeration, nested calls, original exceptions/locking, API access from Rhino and bounded batches. Confirm the final CI result before using an artifact.

Keep the 2.2.16 baseline for a matched real-pack comparison. Client gameplay, reload behavior in actual addon scripts and TPS/FPS improvement require a representative pack run; the repository tests do not replace that evidence.

The supplied 2026-09-24 capture has now been cross-referenced at [SCRIPTING_PROFILE_2026-09-24.md](SCRIPTING_PROFILE_2026-09-24.md). It prioritizes wrapper/member initialization and source-attributed callbacks for the requested scripting-first pass, with RAM tuning outside scope.

The 2.2.18 follow-up defers unused Rhino overload caches and pre-sizes receiver-bound member maps. It preserves per-receiver wrappers and stock resolution, with independent guards and stricter full-method-set checks for the private-cache transformation. See the 2.2.18 section of [SCRIPTING.md](SCRIPTING.md).

The completed 2.2.18 candidate passed CI run `35982475439` at `bcb3b713` across all seven environments. See [VALIDATION.md](VALIDATION.md) for evidence and artifacts. Subsequent checkpoint documentation does not change the tested runtime.
