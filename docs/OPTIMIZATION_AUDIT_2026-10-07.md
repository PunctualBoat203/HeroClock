# October 7 profile and optimization audit

Author: PunctualBoat. Development checkpoint: HeroClock 2.2.23.

## Capture and measurement limits

Input: `CYEh85ALbT.sparkprofile`, 28,149,019 bytes, SHA-256 `c8ae7f2949f1bcae1ed9e642d668d44dc6fc72fef6bb9d489d877c31534c8be3`.

The capture spans 930.774 seconds and records 5,528 ticks on Minecraft 1.20.1 / Forge 47.4.23, Spark 1.10.53. The recorded five-minute TPS is 5.970; five-minute tick median is 146.551 ms and p95 is 196.896 ms. These rolling statistics are not whole-capture averages. RAM is excluded from this analysis.

There are 66 entries in the mod/library metadata. Neither HeroClock nor Mantis is listed, and no HeroClock frames were found. This capture identifies pack costs; it does not establish whether HeroClock's optimizations worked or measure a before/after improvement. Only server-thread execution is captured, so it provides no FPS evidence.

The server thread contains 120,810 call-tree nodes and 25 roots, totaling **898.744 sampled seconds**. Root totals and the sum of exclusive node weights both equal that denominator; no negative exclusive weights were found. Percentages below use this full denominator. For inclusive categories, matching descendants of matching ancestors are excluded so recursion does not double count within a row. Different rows still overlap and must not be added. Sampled time is an attribution estimate, not a promise that removing a call removes all its children or yields the same wall-clock saving.

The file was decoded using the field definitions in Spark's [sampler protobuf](https://github.com/lucko/spark/blob/master/spark-common/src/main/proto/spark/spark_sampler.proto). Source input and exact method identifiers are recorded below so the analysis can be checked in another reader. The raw profile is not committed.

## Current priorities

| Priority | Sampled path | Inclusive time / share | Next safe action |
| --- | --- | --- | --- |
| P0 | `ServerFunctionManager.m_136112_` function execution | 384.372 s / 42.77% | Identify effective function bodies and their callers; preserve immediate side effects, execution order, source and return counts. |
| P0 | `EntitySelector.m_121160_` | 285.936 s / 31.82% | Find broad selectors and repeated `execute` chains in current resources. Narrow them only where the result set and order remain equivalent. |
| P0 | NBT selector predicate `EntitySelectorOptions.m_175173_` | 144.912 s / 16.12% | Replace repeated full-entity NBT queries at their owning scripts/functions with supported direct checks or correctly maintained state. Do not cache arbitrary entity NBT globally. |
| P1 | `SuitSetPowerProvider.providePowers` | 94.772 s / 10.55% | Audit suit registration, shared equipment, subclasses, supplier behavior and reload before building a complete candidate index. |
| P1 | `PropertyManager.toNBT` | 73.564 s / 8.19% | Reduce the caller's unnecessary serialization first; retain every custom property serializer and persistent field. |
| P2 | Rhino-attributed classes, exclusive work | 79.212 s / 8.81% | Attribute scripts and wrapper receivers. Keep the existing narrow allocation patches; engine replacement belongs to Mantis. |

Command execution contains much of the selector work. NBT predicates serialize entities and their mod data: `Entity.m_20240_` accounts for 213.360 s / 23.74% inclusively across callers. At least 42.096 s of `PropertyManager.toNBT` lies directly under the two largest NBT-selector caller groups. Fixing those queries can reduce apparent Palladium serialization cost without changing save behavior.

Name selectors also matter: the two largest `Component.getString` caller groups contribute 48.304 s beneath `EntitySelectorOptions.m_175206_`. This supports reviewing selectors against display names, not globally caching mutable/translatable component text.

Suit enumeration calls `SuitSet.isWearing` repeatedly (37.452 s inclusive). The supplied implementation invokes equipment suppliers repeatedly. The public item-to-suit map is one-to-one and can overwrite shared-item associations; it is not a complete substitute for the original scan. An equipment-result cache also needs invalidation for mutable suppliers, custom suit subclasses, equipment and resource reload.

Rhino wrapper construction remains visible (`WrapFactory.wrapAsJavaObject`: 24.284 s inclusive), but inclusive scripting stacks contain called Minecraft/Palladium work and can recur. Summing `Context.callSync` frames overstates engine cost. The union of Rhino-class subtrees is 132.692 s / 14.76%, including descendants; the exclusive class attribution above is the better engine-work estimate. Source attribution comes from Spark's class-to-mod map; Rhino-package classes including an unattributed generated lambda total 79.288 s exclusively. KubeJS-attributed exclusive work is 1.180 s / 0.13%; this does not include all the gameplay its callbacks invoke.

The old phasing priority does not transfer to this capture: `AbilityUtil.getEnabledInstances` is 19.280 s inclusive and `preventCollisionWhenPhasing` is 8.308 s (under 1%). `getPropertyByName` is 2.912 s inclusive / 0.708 s exclusive. These are different workloads from September, not evidence that a HeroClock patch produced those reductions.

## Retained optimization review

The review covers the current Java clock/work infrastructure, optional optimization mixins, their audited dependency implementations and existing regression coverage. It is not a certification of every historical addon resource or every possible third-party transformation.

| Area | Review result / boundary |
| --- | --- |
| Satsu sentinel redirect | **Confirmed correctness issue; removed in 2.2.23.** The HEAD cancellation intercepted every matching function execution, returned zero, and deferred kills. Explicit/manual calls therefore lost synchronous effects and executed-command counts. Direct changes to the exposed tag set could also escape lifecycle scheduling. Function-text matching and queue-pressure fallback did not fix these semantics. Stock execution is restored and the shape-only test is replaced with actual execution checks. |
| Palladium power-holder and Curios views | Cached objects remain unmodifiable live views, not snapshots. Palladium's backing field is guarded as final; Curios checks backing-map identity before reusing a view. Existing runtime coverage checks Curios replacement and incompatible Palladium fields. |
| Palladium property lookup | Registration and deserialization invalidate positive results; exposing `values()` permanently disables caching for that manager. Missing results are not cached. The supplied base property's key is final, but subclass overrides of `getKey()` and direct package-level mutation are not covered by this base-class review; do not claim universal addon safety from method fingerprints alone. |
| Scalar property sync | Equal immutable scalar values are filtered; mutable values are not deduplicated. The supplied `onChanged` target sends sync packets rather than running ability logic. Custom serializers and other mixins still need pack validation. |
| Parsed command cache | Dispatcher identity invalidates cached parsing/error state. Command execution frequency is unchanged. This reduces parsing work, not the new profile's large execution/selector cost. |
| KubeJS listener append | Tail reuse only shortens registration traversal; native dispatch, listener order and cancellation remain upstream. Existing scripting tests exercise registration, clearing, errors and ordering. |
| Rhino allocations | Lazy overload lists remain per method instance, published with CAS. Map-ID conversion preserves key conversion and resized iteration; member maps/wrappers remain fresh. Existing tests cover overload behavior, isolation, concurrency and changed-code fallback. No script-result cache or global mutable wrapper cache is introduced. |
| Profiling hooks | Detailed hooks require startup opt-in. Boundary timing wraps the original call with `finally`; exceptions continue through the upstream path. Profiling results include descendants and must not be added as disjoint costs. |
| Clock and bounded work | Saved overworld deadlines, overflow saturation, server-thread mutation, coalescing, cancellation and admission limits remain. The time budget cannot preempt a running callback. Existing unit/runtime checks cover these contracts; no measured zero-lag guarantee is implied. |
| Legacy temporary-entity cleanup/resources | Still pack-specific. Tag names, cleanup regions, block rules and resource overrides are not made safe for arbitrary addon updates by Java bytecode guards. Exact current packs and gameplay/reload tests are required before calling this portion fully validated. |
| Compatibility gates | Changed or missing audited targets disable their patch independently; relabeled unchanged code remains eligible. This checks selected input code, not every subclass, later transformer or datapack. Previous CI covers relabeled and deliberately changed targets, but a target-pack startup log is still needed. |

The Satsu regression test executes the original one-command function against a tagged entity, checks immediate death and the returned command count, then executes a changed two-command function and checks its followup and source. It inserts the tag through the exposed set, so correctness cannot depend on intercepting `addTag`. Removing both cancellation and deferred kills avoids running two competing implementations.

## Inputs needed for the next patch

Already available: Palladium **4.5.9**, KubeJS **2001.6.5-build.26**, Rhino **2001.2.3-build.10**, and audited Curios **5.14.1+1.20.1**. Their version labels match this capture. No need to resend those unless the installed binaries differ from the audited inputs in the compatibility/scripting docs.

Please supply:

1. **OmniOptimizer 1.8.2 JAR**, the version in this capture. Earlier 1.8.0 inspection does not establish its current mixin/resource behavior.
2. The active **datapacks, Palladium addonpacks, `kubejs/server_scripts` and `kubejs/startup_scripts`**, including locally changed resources. These are necessary to identify actual function IDs, selector strings, tick tags and script callers; Spark's Java stacks do not identify them reliably here. Include relevant addon JARs when they are the only container for those resources.
3. A current **`/heroclock status` output and startup compatibility lines** once HeroClock is installed, followed by a comparable capture with the same scene/actions. This confirms which guards enabled before attempting a performance comparison.

Further addon-specific JAR requests should follow resource attribution, rather than asking for every installed mod. In particular, do not replace an entire entity query, power scan or serialization path based only on a large inclusive percentage.

## Validation

The preceding 2.2.22 checkpoint passed unit/build/API checks and twelve required GameTests in each of seven environments; see [VALIDATION.md](VALIDATION.md). The 2.2.23 Satsu correction and stronger execution regression passed [CI run 37681532074](https://github.com/PunctualBoat203/HeroClock/actions/runs/37681532074) at `a43b5276b454c157b52ad781e5e16af3581c09b3`: unit/build/API checks and all twelve required GameTests in each of seven environments. Optional-mod checks skip where dependencies are absent. [Runtime and embedded developer API download](https://github.com/PunctualBoat203/HeroClock/actions/runs/37681532074/artifacts/11509496450). Subsequent audit-documentation edits do not change tested code. Full-pack gameplay/resource validation and comparable performance measurement remain outstanding.
