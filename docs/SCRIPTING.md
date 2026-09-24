# KubeJS and Rhino support

HeroClock 2.2.17 — PunctualBoat. Minecraft 1.20.1 / Forge 47.x.

## Automatic optimizations

KubeJS `EventHandlerContainer.add` normally walks the listener chain from its head on every registration. HeroClock resumes that traversal from the last appended node, making repeated root appends constant-time after the first traversal. Appends made through a descendant are followed on the next root append. Dispatch, cancellation, error handling and listener order are unchanged. This primarily helps large registration/reload workloads, not the cost of executing every listener each tick. Direct reflective replacement of KubeJS's package-private chain links is outside this contract.

Rhino `NativeJavaMap.getIds` normally builds an intermediate ArrayList and copies it into an array. HeroClock fills a fresh result array directly, retaining map iteration order, integer IDs, Rhino string conversion, live map contents and independent returned arrays. A buffer grows or shrinks if a nonstandard map's iterator produces a different number of keys from its initial size. Script results and wrappers are never cached.

Each optimization is checked separately against audited executable method bodies and required fields. Unchanged code may continue working with a different version label. Missing or changed targets disable only their patch. The same checks protect the optional boundary instrumentation. Check `/heroclock status` or `HeroIntegrationAPI.compatibility()` for actual decisions.

## Embedded API

The embedded API is an integration surface for mod/addon developers; datapack developers use HeroClock commands for the systems exposed to functions. Automatic optimizations run independently of this API and require no developer calls.

`com.heroclock.api.HeroScriptAPI` is callable directly from KubeJS using `Java.loadClass`. Java addons can extract `META-INF/heroclock/HeroClock-2.2.17-api.jar` from the mod as a compile-only dependency. Players install only the full mod. The embedded API is not a Forge nested dependency.

```javascript
const HeroScript = Java.loadClass('com.heroclock.api.HeroScriptAPI');

function refreshInBatches(server, stableJavaList) {
    return HeroScript.batch(server, 'myaddon', 'refresh',
        stableJavaList.iterator(), 16, value => {
            refreshOne(value);
        });
}
```

`batch(server, namespace, key, iterator, itemsPerTick, action)` returns whether the queue accepted the job. Use 1–256 items per tick and small callbacks. It uses HeroClock's existing capacity, coalescing, cancellation and cooperative time budget. The iterator is consumed on the server thread without copying the collection; keep it valid until completion. A callback or iterator exception fails the job through the normal work-queue error handler. Submitting the same namespace/key replaces pending work.

Use `cancelNamespace(server, 'myaddon')` when unloading/reloading your scripts or abandoning their jobs. This explicit API does not silently attach to arbitrary script reload implementations. Jobs are transient and cleared on server shutdown. Script batches use namespace ownership; entity-specific automatic unload cancellation remains available through `HeroWorkAPI` with the entity UUID as owner. Submit and cancel gameplay work only on the server thread.

Native KubeJS scheduling retains its own callback ordering, wall-clock timing, repeating timers and public event lists. HeroClock does not migrate those timers automatically. Only work explicitly submitted to the batch API is spread across ticks.

## Opt-in profiling

```javascript
HeroScript.setProfilingEnabled(true);
// Run a representative workload, then capture the immutable snapshot:
const sample = HeroScript.profile();
HeroScript.setProfilingEnabled(false);
```

Snapshots contain `rhino_calls` and `kubejs_handlers`, each with `calls()` and `outermostNanos()`. Counters accumulate for the process lifetime; compare snapshots before/after a workload. Nested calls count individually, but duration is recorded only for the outermost call in each boundary on each thread. KubeJS counts handler-chain invocations, not individual listeners. Snapshots are approximate during concurrent activity and include all threads.

These timings include downstream gameplay and overlap between boundaries. They are not exclusive engine time and must not be added together. Rhino timing starts inside its existing synchronized section, excluding lock-wait time. Exceptions unwind timing in a `finally` block and propagate normally. If a target fails its compatibility check, its counters remain inactive; consult compatibility decisions rather than interpreting zeros as evidence that no scripts ran. Profiling is disabled by default and has overhead when enabled.

## Supplied-profile findings

The supplied `bCJW5elCtx(1).sparkprofile` was decoded against Spark's sampler schema. The server-thread sample weight totals 864,156; subtracting immediate child weights yields about 9.25% exclusive weight in KubeJS/Rhino classes. Prominent exclusive frames include the interpreter loop (1.47%), Java-object wrapping (1.18%), member initialization (0.73%) and top-level scope lookup (0.69%). Inclusive callback totals overlap through nesting and include gameplay called by scripts.

Map enumeration accounts for only 40 inclusive weight units in this capture; listener registration is not a sampled hot path. These two patches reduce specific redundant work, not the majority of this profile. The opt-in boundary metrics and batching API support targeted addon changes without skipping gameplay callbacks. No live TPS/FPS improvement is claimed until the same representative workload is rerun against 2.2.16 and this build.

## Audited inputs and tests

- KubeJS `2001.6.5-build.26`: SHA-256 `1769312192fbf9d72f45054ba61130523bfb471b5f480a4bff8c210ec09400bb`.
- Rhino `2001.2.3-build.10`: SHA-256 `fed2211429301bf043864183cab9ab8e92d4cc4dbb9e488ce6c75217c54584a6`.
- Architectury `9.2.14` is a hash-pinned test dependency, not an optimization target.

CI fetches the exact supplied KubeJS/Rhino binaries by hash. Runtime checks cover these inputs, synthetic changed version labels and incompatible field/method names. Modified dependency fixtures are never distributed with HeroClock. The full mod must still pass existing base/Palladium/Curios tests and verify byte-identical standalone/embedded API artifacts.

Disable scripting patches independently with `-Dheroclock.disableKubeJSOptimizations=true` or `-Dheroclock.disableRhinoOptimizations=true`. Public API support and other matching optimization contracts remain available.
