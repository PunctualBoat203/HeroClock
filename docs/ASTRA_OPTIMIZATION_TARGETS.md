# Astra optimization targets

Author: PunctualBoat. Reference project: HeroClock / superhero stack.  
Profile checkpoint: `profile-2026-09-24_01.16.05.sparkprofile` (single-player integrated server, 2026-09-24).

This note records optimization targets for the Astra pass. It is a profiling roadmap, not a claim that every target can be safely patched.

## Profile context

The capture ran for about 31m50s in a very large pack (Spark reported 339 mod/library sources). The integrated server was genuinely overloaded, but the machine was also under severe memory pressure: roughly 99.4% physical RAM utilization and about 88% swap utilization with Minecraft launched around a 7.5 GiB max heap.

Treat paging/GC pressure as an amplifier, not as an explanation for all of the server-thread cost. Astra should prioritize repeatable CPU/MSPT waste that also benefits systems with more RAM.

HeroClock itself remained effectively negligible in the sampled server work (about 0.01% exclusive). Do not spend Astra effort micro-optimizing HeroClock's bounded queue unless later profiles change that result.

## Priority optimization targets

### P0 — Palladium enabled-ability lookup during phasing/collision

The strongest superhero-specific hotspot in this capture is Palladium repeatedly resolving enabled ability instances inside phasing/collision checks.

Observed paths include:

- `AbilityUtil.getEnabledInstances`: about 248 s inclusive (~15.6% of active sampled work);
- `preventCollisionWhenPhasing`: about 224 s inclusive (~14.1%);
- `RegularImmutableMap$Values.get`: about 109 s exclusive (~6.9%) under this area.

Goal: reduce repeated enumeration/materialization/lookups when the relevant ability state has not changed.

Safety requirements:

- cache only state with a reliable invalidation source;
- invalidate on power/ability enable-state changes, reloads and lifecycle transitions;
- preserve per-entity and per-power semantics;
- fail open to Palladium's stock path if compatibility cannot be proven;
- never reuse mutable gameplay results across entities or ticks without explicit validity rules.

### P0 — Repeated Minecraft functions, commands and selectors

Command/function execution remains one of the largest costs in the pack.

Observed paths include:

- `ServerFunctionManager` execution: about 343 s inclusive (~21.6% active);
- `CommandDispatcher.execute`: about 354 s across call sites;
- entity selector evaluation: about 148 s inclusive (~9.3%).

Astra should identify the actual function IDs / addon namespaces responsible before changing behavior.

Goal: remove redundant high-frequency command work, especially global selectors, repeated `execute` chains, repeated entity-data reads and script-triggered commands whose inputs have not changed.

Safety requirements:

- instrument function IDs and selector patterns first;
- preserve command order, side effects, permissions and execution context;
- prefer event/lifecycle-driven replacements for known deterministic polling loops;
- do not globally memoize arbitrary command results;
- do not suppress addon functions without verifying the exact effective function body.

### P1 — Rhino Java↔JavaScript boundary overhead

The latest profile still shows meaningful Rhino engine cost independent of downstream gameplay.

Observed values:

- Rhino-exclusive work: about 106 s (~6.7% active);
- `Interpreter.interpretLoop`: about 159 s inclusive;
- `NativeJavaObject.initMembers`: about 15 s exclusive;
- wrapping/member-resolution paths remain recurring hotspots.

Goal: reduce deterministic repeated wrapper/member lookup work without caching arbitrary script results.

Candidate areas:

- member metadata / reflection lookup reuse where class identity is stable;
- avoiding repeated wrapper initialization for safe immutable metadata;
- improving hot Java-object access paths used by Palladium/KubeJS integrations;
- reducing unnecessary boundary crossings rather than skipping script behavior.

Safety requirements:

- preserve Rhino locking and exception behavior;
- preserve dynamic/prototype/member changes where observable;
- no global wrapper reuse for mutable Java objects without a proven lifetime model;
- no caching of arbitrary JavaScript return values.

### P1 — High-frequency KubeJS tick callback chains

HeroClock 2.2.17 instrumentation and Spark both show material recurring script callback activity.

Prominent groups in this capture were approximately:

- level post-tick: ~18.1 s;
- server post-tick: ~15.9 s;
- player tick: ~15.1 s;
- Palladium `ScriptableAbility.tick`: ~57.6 s inclusive;
- `ScriptableCondition.active`: ~39.3 s inclusive.

Goal: reduce redundant callback setup/dispatch and help addon authors move deterministic bulk work to bounded/event-driven paths.

Safety requirements:

- do not globally throttle tick callbacks;
- do not merge callbacks that may observe different state or ordering;
- preserve listener order, cancellation and exception propagation;
- prefer instrumentation that identifies duplicate work by namespace/function before introducing any automatic suppression;
- use `HeroScriptAPI` batching only for explicitly opted-in work.

### P1 — Palladium property lookup and NBT serialization

These remain persistent secondary hotspots:

- `PropertyManager.getPropertyByName`: about 34.4 s exclusive (~2.2% active);
- `PropertyManager.toNBT`: about 27.3 s exclusive (~1.7%).

HeroClock already has guarded property-name caching, so Astra should not assume "add another cache" is the answer.

Goal: instrument hit/miss/invalidation behavior and determine which callers repeatedly serialize or miss the existing fast path.

Safety requirements:

- measure existing cache effectiveness first;
- do not cache missing/dynamic registrations unless invalidation is complete;
- avoid suppressing NBT serialization when data can mutate or sync semantics depend on it.

### P2 — Lock waits around selector/entity-data work

The capture contains roughly 75 s in shared-lock acquisition paths, with significant occurrences below command execution reading `SynchedEntityData`.

Goal: reduce the command/selector traffic that creates contention rather than attempting to bypass Minecraft synchronization.

Do not patch lock semantics directly unless a narrowly proven upstream redundancy can be removed instead.

### P2 — Pehkui and Curios call frequency

Current profile:

- Pehkui: ~14.9 s exclusive (~0.9%);
- Curios: ~8.2 s exclusive (~0.5%).

Neither is a top-level Astra target right now. Pehkui already has its own scale-data cache, and HeroClock already reuses Curios' read-only map view.

Only revisit these when profiling shows a superhero-specific repeated-call pattern that existing caches do not handle.

## Current 2.2.17 scripting work

HeroClock 2.2.17 already contains narrowly guarded KubeJS/Rhino work:

- KubeJS listener append traversal optimization;
- Rhino `NativeJavaMap.getIds` allocation reduction;
- optional KubeJS/Rhino boundary profiling;
- `HeroScriptAPI` bounded iterator batching and profiling snapshots;
- compatibility-contract gating with relabeled/incompatible fixture tests.

These are intentionally narrow. They do not justify broad callback suppression, wrapper reuse or scheduler replacement.

See `docs/SCRIPTING.md` for the current implementation contract and limitations.

## Measurement plan

Use the 2.2.16 release artifact as the clean pre-universal-KubeJS/Rhino baseline and compare one isolated Astra optimization at a time.

For each candidate:

1. reproduce a representative superhero-heavy workload;
2. capture the same Spark mode/window and HeroScript counters where relevant;
3. compare active server-thread work, MSPT median/p95 and the targeted call path;
4. confirm gameplay behavior, reload behavior, power state changes, death/dimension transitions and multiplayer-relevant semantics;
5. disable the patch and verify stock behavior returns cleanly;
6. reject an optimization if the gain depends on unsafe stale state or callback suppression.

A successful Astra optimization should reduce deterministic repeated CPU/MSPT work even on machines that are not RAM constrained. Lower allocation/GC pressure is useful, but it should not be confused with a real reduction in gameplay work.

## Non-goals

- General optimization of unrelated mods in the pack.
- Blanket throttling of scripts or abilities.
- Caching mutable gameplay outcomes without complete invalidation.
- Replacing Minecraft/Rhino/KubeJS synchronization just to make a profiler number smaller.
- Hiding broken addon behavior instead of identifying the offending namespace/function.
- Moving experimental Astra behavior into a stable HeroClock release without independent compatibility and runtime validation.
