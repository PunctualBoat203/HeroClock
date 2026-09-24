# KubeJS/Rhino profile cross-reference — 2026-09-24

Author: PunctualBoat. Cross-reference: [optimization roadmap](ASTRA_OPTIMIZATION_TARGETS.md).

This analysis focuses on server-thread CPU paths. RAM capacity, swapping and heap tuning are outside this sweep. No runtime optimization or before/after speedup is claimed by this document.

## Input and calculation

Input: `profile-2026-09-24_01.16.05.sparkprofile` from the supplied ZIP.

Uncompressed SHA-256: `ace646935a26fd2684ab9cf086cbe55c94cf13a220b8c60f09640777ee86a24f`.

Embedded source metadata reports HeroClock 2.2.17, KubeJS 2001.6.5-build.26, Rhino 2001.2.3-build.10 and Palladium 4.5.9. The scripting versions match the audited input binaries used for the current contracts. Version metadata alone does not prove binary identity.

Decoded using Spark's `SamplerData`, `ThreadNode` and `StackTraceNode` schema. Each node's inclusive weight is the sum of its time-window weights. Exclusive weight subtracts the inclusive weights of immediate children. All windows are included; only `Server thread` is analyzed here.

The server-thread sample weight is 1,908.720 seconds, including 323.352 seconds attributed exclusively to `Unsafe.park`. Subtracting that parked weight gives a 1,585.368-second non-parked comparison denominator, reproducing the roadmap's approximate active-work percentages. This is a sampling proxy, not hardware CPU utilization or a promise that every remaining sample is productive work. Inclusive method totals can overlap through recursion and callers and must not be added together.

## Measured scripting priorities

| Path | Exclusive sampled seconds | Inclusive sampled seconds | Exclusive share of non-parked work |
|---|---:|---:|---:|
| Rhino package, combined | 106.380 | Not additive | 6.71% |
| KubeJS package, combined | 2.868 | Not additive | 0.18% |
| `Interpreter.interpretLoop` | 19.812 | 158.820 | 1.25% |
| `NativeJavaObject.initMembers` | 15.116 | 33.020 | 0.95% |
| `ScriptableObject.getTopLevelScope` | 10.908 | 10.908 | 0.69% |
| `NativeJavaObject.get` string-property path | 8.268 | 26.944 | 0.52% |
| `EventHandler.post`, combined overloads | 0.764 | 198.540 | 0.05% |

The low exclusive KubeJS dispatcher weight does not mean callbacks are cheap: substantial work happens inside Rhino and the gameplay those callbacks invoke. Sampling attribution can also reflect JIT inlining; these numbers do not directly count allocations or cache misses.

### First target: wrapper initialization at callback boundaries

`NativeJavaObject.initMembers` spends 33.020 seconds inclusively. Its immediate child paths include:

- `JavaMembers.lookupClass`: 8.900 seconds, of which 8.092 is below `ConcurrentHashMap.get` and 0.808 below `JavaMembers.<init>`.
- `JavaMembers.getFieldAndMethodsObjects`: 9.004 seconds, including 6.392 below `FieldAndMethods.<init>` and 1.972 below `HashMap.put`.

Source inspection confirms that `Context.getClassCacheMap` already maintains a per-context concurrent class cache. `JavaMembers.lookupClass` consults it before reflecting a class. Adding another broad reflection cache is therefore not justified by this capture.

The field-and-method collision wrappers are deliberately bound to each Java receiver: `getFieldAndMethodsObjects` creates new `FieldAndMethods` instances and assigns their `javaObject`. Global reuse would risk binding a member to the wrong receiver. Their constructors also resolve function prototypes through the scope chain. Approximately 5.552 seconds of scope traversal appears under these constructors in the sampled wrapper-initialization paths.

A prominent initialization caller is Palladium's `ScriptableAbility.tick` through Rhino's `InterfaceAdapter.invoke`. One major wrapping branch accounts for 11.872 seconds under `initMembers`, with another direct wrapping branch accounting for 1.236 seconds. These are nested path weights, not additional independent engine totals. Other callers include Java method return values, property getters and KubeJS event handlers.

Next implementation gate: identify dominant wrapped receiver classes and repeated class-cache hits, then prove the validity/lifetime of any shortcut. Keep receiver-bound wrappers, custom wrapper providers, class visibility checks, context identity and mutable prototype/scope semantics intact. A sampled lookup is not evidence that its result can be reused indefinitely.

### Second target: repeated property access and scope traversal

`NativeJavaObject.get` contributes 8.268 seconds exclusively and 26.944 inclusively on its string-property path. `getTopLevelScope` contributes 10.908 seconds, including 6.196 below function-prototype lookup and 2.268 below top-level call setup.

Source inspection shows that scope lookup follows `getParentScope` on every link. Parent scopes and prototypes are mutable and getters may be implemented by arbitrary Scriptable types. A permanent cached top-level scope would therefore require complete invalidation or a narrowly proven immutable scope family; unchanged method bytecode alone does not establish that lifetime rule.

Next implementation gate: attribute repeated property/member access by receiver class and member name with bounded, opt-in diagnostics. Distinguish stable metadata lookup from live getter execution. Do not cache property values or bypass getter side effects.

### Third target: identify expensive script callbacks

The roadmap's recurring callback groups are supported by this capture:

| Entry path | Inclusive sampled seconds |
|---|---:|
| Palladium `ScriptableAbility.tick` | 57.620 |
| Palladium `ScriptableCondition.active` | 39.276 |
| KubeJS level post-tick | 18.564 |
| Server post-tick callback dispatch below the injected server hook | 15.916 |
| KubeJS player tick | 15.200 |
| Native scheduled-event processing below the server hook | 3.924 |

The complete injected server post-tick hook totals 19.904 seconds; quoting all of that as callback dispatch would incorrectly include its scheduler and other child work. The level/player values are close to the roadmap's rounded estimates. Preserve the exact entry path when comparing future captures.

These inclusive values include downstream gameplay. They do not identify the responsible JavaScript source files, addon namespaces, argument classes, exact callback counts or safe-to-skip invocations. The existing aggregate `HeroScriptAPI` counters also do not supply per-source attribution.

Next implementation gate: bounded opt-in attribution by the registered listener's source/line and event identity, plus receiver/member diagnostics at the Rhino boundary. Preserve listener order, checked exits, exceptions and synchronization. Use developer-requested `HeroScriptAPI` batching only for explicitly opted-in bulk work; native tick callbacks remain synchronous.

## Low-priority work in this capture

- `NativeJavaMap.getIds`: 0.112 seconds inclusive. Most of the mapped implementation cost appears in HeroClock's redirected enumeration handler. It is not a material next target in this capture.
- `EventHandlerContainer.add`: no sampled weight in this gameplay capture. The existing append optimization is primarily a registration/reload improvement.
- Native scheduled-event processing: 3.924 seconds inclusive. Replacing it wholesale would risk wall-clock timers, public event mutation, cancellation and ordering for a comparatively small sampled path.
- HeroClock's bounded queue is not promoted as an optimization target by these results.

## Order for the next guarded scripting pass

1. Measure wrapped receiver classes and class-cache lookup outcomes at the ability/event boundary. Start with the `ScriptableAbility.tick -> InterfaceAdapter.invoke -> wrapper initialization` path.
2. Attribute property/member access and receiver-bound collision-wrapper construction. Investigate a shortcut only after proving its invalidation and context/receiver lifetime rules.
3. Attribute expensive callback chains by event/source; distinguish dispatch overhead from command, selector, NBT and ability work called by scripts.
4. Implement one deterministic shortcut at a time, guarded by the actual target code and any required lifecycle contracts, with independent stock fallback and runtime regression fixtures.
5. Re-run the same workload and sample mode against the 2.2.17 immediate baseline; retain 2.2.16 as the historical pre-scripting baseline. Compare the target path and MSPT median/p95, not only total allocation or an inclusive parent percentage.

Palladium phasing and global command/selector redesign remain roadmap priorities outside this requested KubeJS/Rhino-first pass. Palladium's script boundary is included here because it directly drives Rhino wrapping and execution.
