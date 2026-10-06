# HeroClock scripting development handoff

Author: PunctualBoat. Development version: **2.2.20** on `improve-clock-runtime`.

## Current checkpoint

Step one of the HeroClock-owned scripting integration is implemented. HeroClock owns namespaced event dispatch, direct Rhino callback adapters, scheduling/batches and lifecycle cleanup. Rhino remains the JavaScript engine; KubeJS remains the script loader. Existing native KubeJS callbacks and timers are not migrated.

- Matching KubeJS server scopes receive `HeroRuntime.onServer/forServer`. Direct callbacks use the original context lock, wrap factory and top-level scope without Java functional-interface proxies.
- Listener snapshots rebuild only on registration changes. Namespaces without a tick listener skip tick dispatch. Registrations and shared queued work have explicit capacity limits.
- Load/unload closes old runtimes, cancels pending setup/work and releases listeners. Failed setup closes its partial namespace without replaying callbacks. A callback waiting on Rhino's lock rechecks validity before entering JavaScript; already executing code is not preempted.
- The embedded `HeroScriptRuntime` and `HeroScriptAPI.openRuntime/emit` APIs expose owned systems to mod developers without Rhino/KubeJS types. Java registration also works without those mods installed. Datapack functions can emit registered events with `/heroclock script emit` and retain their command source.
- The binding requires matching ScriptManager and Rhino context/wrapping contracts. Unchanged code with different version metadata remains enabled; incompatible code omits the adapter and retains native execution. The Java API remains available.

Read [OWNED_SCRIPTING.md](OWNED_SCRIPTING.md) for examples, limits, ordering, exception behavior, ownership and fallback. The embedded API is a supported developer surface, not a copy-protection mechanism. It remains an inert resource, byte-identical to the standalone compile-only artifact, and excludes implementation classes.

## Validation and prior work

The initial 2.2.20 checkpoint at `97a55d40` passed CI run `36291086554`. The final cleanup checkpoint at `147e26c37` passed CI run `37526895955` on 2026-10-06: build/unit/API checks and fourteen runtime tests in each of seven environments. It closes partial failed setups, rejects invalid setup namespaces immediately and prevents unloaded callbacks waiting for the context lock. Subsequent commits are documentation-only. See [VALIDATION.md](VALIDATION.md) for evidence and downloadable artifacts.

The matrix covers base Forge, supplied Palladium/Curios, relabeled and incompatible Palladium, and supplied/relabeled/incompatible KubeJS/Rhino. Owned-runtime checks cover event order, argument isolation, capacity/recursion limits, coalesced/cancelled jobs, datapack source forwarding, direct Rhino calls, server tick events and unload/reload. Cleanup regression checks cover failed setup, pending setup cancellation and invalidation while waiting for the Rhino lock. Optional-mod checks skip where their dependencies are absent.

The 2.2.18 lazy overload-cache allocation and member-map sizing fixes remain active behind their independent guards. The 2.2.19 bounded hotspot attribution remains startup-opt-in and retains labels/counters rather than world objects. Those checkpoints and their CI evidence are retained in [SCRIPTING.md](SCRIPTING.md) and [VALIDATION.md](VALIDATION.md). Earlier release/real-pack findings remain in [the archived handoff](archive/2.2.16-HANDOFF.md).

## Next performance work

Use [the supplied Spark analysis](SCRIPTING_PROFILE_2026-09-24.md) and [Astra priorities](ASTRA_OPTIMIZATION_TARGETS.md) to select a concrete addon workload. Obtain a source-attributed capture, move that workload explicitly onto the owned integration, then compare matched gameplay against the 2.2.16 baseline and stock behavior. No target-pack TPS/FPS gain is established by this checkpoint.

Preserve Java coercion/wrappers, mutable state, event order, errors, reload and client/server boundaries. Do not add global receiver/property caches, automatic native scheduler migration, blanket throttling or callback failure retries. The 1 ms budget limits admission of additional queued steps; it cannot bound arbitrary callbacks. Client scripting and an independent JavaScript engine remain outside this step.

Continue committing and pushing checkpoints to `improve-clock-runtime`. Keep the draft PR unmerged until the target-pack gameplay/reload and matched performance comparison have been reviewed.
