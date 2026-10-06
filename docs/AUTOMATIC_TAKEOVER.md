# Automatic takeover and standalone scripts

HeroClock 2.2.21 — PunctualBoat. Minecraft 1.20.1 / Forge 47.x.

HeroClock can load its own server scripts with Rhino installed and KubeJS absent. When KubeJS is installed, compatible existing JavaScript interface callbacks also use HeroClock's native callback bridge automatically. Compatible KubeJS listeners receive a direct execution adapter without script edits or `HeroRuntime` registration.

This is a partial replacement of the scripting stack. KubeJS still implements legacy loading, recipes, bindings, plugins, event selection and native timers. Rhino still evaluates JavaScript. Removing either dependency from an addon that declares it is not supported by this checkpoint.

## Current ownership

| Operation | Current implementation |
| --- | --- |
| Loading `config/heroclock/server_scripts` | HeroClock, using Rhino directly; KubeJS is optional |
| Owned events, scheduling, batches and reload cleanup | HeroClock |
| Matching native Rhino Java-interface callback routing | HeroClock, including existing addon registrations |
| Matching KubeJS listener callback invocation | Cached HeroClock adapter; public listener/proxy identity is retained |
| KubeJS listener selection, chain traversal, cancellation and error handling | Existing KubeJS control flow |
| Legacy KubeJS script directories, recipes, plugin APIs and custom bindings | KubeJS |
| Legacy KubeJS timers | KubeJS, with original wall-clock/game-tick semantics |
| JavaScript parsing, interpretation and Java interop services | Rhino |

The supported Java API and internal `ServerScripts.Engine` implementation boundary do not expose Rhino types. The engine boundary itself is not part of the embedded developer API. A future engine can implement the owned loader/runtime boundary independently. Replacing legacy KubeJS and Rhino completely also requires compatible implementations of the remaining rows, not just a different mod identifier.

## Automatic callback routing

`RhinoNativeCallbackMixin` selects HeroClock's invocation handler when the audited VMBridge/InterfaceAdapter and context/wrapping contracts match. Rhino still constructs the same Java proxy class. Its proxy identity, `equals/hashCode/toString`, Java default methods, original script scope, fresh `this` wrappers, custom wrap factory and return conversion remain intact. Object-style callback members are resolved on each invocation, so replacing a function or property getter remains observable. Custom InterfaceAdapter subclasses retain their original handler.

The HeroClock handler retains immutable method metadata for its primary callback instead of repeating generic reflective classification on every invocation. It does not cache script results, world state, receiver wrappers or property values. Native argument wrapping still happens in its original position, and execution still uses the original context's `callSync` lock.

`KubeNativeDispatchMixin` builds a direct adapter once per compatible listener container. Calls enter the HeroClock callback bridge without passing through Java Proxy dispatch and Rhino InterfaceAdapter dispatch each time. The container's public `handler` field remains the original listener. Java/custom handlers and proxies implementing addon-defined listener subinterfaces keep their own invocation behavior. Checked exceptions preserve the proxy's declared/undeclared exception rules before returning to KubeJS's existing error handling. Listeners appended during dispatch are still visited by the original chain.

Detailed listener profiling, when installed at startup, uses a cached wrapper rather than allocating an instrumentation lambda per dispatch. The `KubeListenerProfileMixin` decision reports that profiling is supplied by automatic dispatch when that route is active. If takeover is unavailable, the older independently guarded profiler can still be selected. Its labels and counters remain unchanged.

`HeroScriptAPI.takeover()` returns cumulative `nativeProxiesCreated()` and `directListenersCreated()` counters. They count created adapters for the process lifetime, including registrations later unloaded; they are not live-listener counts, callback execution counts or a performance measurement. Collection performs no counter update on the callback hot path. Use compatibility decisions to distinguish unavailable routes from a workload that has not registered callbacks yet.

## Standalone server scripts

Install HeroClock and the supported Forge Rhino dependency. Put UTF-8 `.js` files under `config/heroclock/server_scripts`; subdirectories are supported. HeroClock loads them on server start and through `/heroclock script reload`. This directory also works when KubeJS is present. Existing KubeJS directories retain their existing loader.

Each file has its own script scope and owned-runtime lifecycle. Files execute in sorted relative-path order with a shared standard-library root/context. Top-level file variables stay in their file scope; explicitly mutating shared standard prototypes still affects that context. Failed files release their owned listeners, jobs and pending setup, and other files continue. Reload closes the previous files before loading the current directory. World changes already performed by a script are not rolled back or retried.

For example, save `cooldown.js`:

```javascript
const runtime = HeroRuntime.forServer(server, 'myaddon');

runtime.on('myaddon:cooldown', 'start', source => {
    const entity = HeroScript.executor(source);
    if (entity != null) HeroClock.set(entity, 'myaddon:cooldown', 40);
});

console.info('Cooldown handler ready');
```

A datapack function can call it:

```mcfunction
execute as @a at @s run heroclock script emit myaddon myaddon:cooldown
```

The prebound globals are `HeroRuntime`, `HeroClock`, `HeroScript`, `HeroWork`, `HeroFunctions`, `HeroIntegration`, `server`, `Java` and `console`. The API globals refer to the corresponding `com.heroclock.api` facades. `console.info/warn/error/log` accept a message string and include the source file in the log. `Java.loadClass('your.addon.API')` loads an installed Java class through HeroClock's class loader and returns Rhino's Java-class wrapper. For example, `const ArrayList = Java.loadClass('java.util.ArrayList'); const values = new ArrayList();` constructs a Java list without KubeJS. These scripts execute trusted server code; class loading is not a sandbox. The binding does not provide KubeJS's remapper, custom wrapper registrations or other `Java` helpers. KubeJS-specific globals such as `ServerEvents` are not provided. Use the stable HeroClock facades or your addon mod's own API instead of assuming legacy KubeJS bindings exist.

Use [the owned-runtime API](OWNED_SCRIPTING.md) for events, tick callbacks, keyed scheduling and batches. `HeroRuntime.onServer` also works in these files. Owned runtime handles are closed automatically on reload/stop. Raw `HeroWork` and the older `HeroScript.batch` APIs retain their separate ownership rules; their callers must arrange cancellation on reload. No arbitrary callback can be preempted or automatically spread across ticks.

The loader accepts at most 128 script files and 1 MiB per file, with bounded reads and UTF-8 validation. Too many files reject the directory; an oversized or invalid file fails individually. Script-file symlinks and directory symlink traversal are excluded. These limits bound loading inputs, not the execution time or memory consumed by arbitrary JavaScript.

## Commands and embedded API

Commands require permission level 2:

```mcfunction
heroclock script reload
heroclock script status
heroclock script emit myaddon myaddon:cooldown
```

Reload applies only to HeroClock's standalone directory. `/reload` and KubeJS's own reload commands retain their existing scopes. The status command reports the owned loader and cumulative adapter-registration counts. `/heroclock status` reports individual compatibility decisions.

Java addons can use the embedded API without importing Rhino types:

- `HeroScriptAPI.reloadServerScripts(server)` reloads the configured directory.
- `HeroScriptAPI.serverScripts(server)` returns the last `ScriptLoadStatus(engine, loaded, failed)`.
- `HeroScriptAPI.executor(source)` returns the command's entity or null, using mapped Minecraft access in the runtime mod.
- `HeroScriptAPI.takeover()` returns immutable registration counts.

Loader states are `rhino`, `rhino_missing`, `incompatible` and `not_loaded`. Loading/status methods require the server thread. Capabilities report API availability; check loader state and compatibility decisions for actual backend availability. The standalone and embedded API JARs remain byte-identical, compile-only developer artifacts.

## Fallback and verification

`-Dheroclock.disableNativeScriptingTakeover=true` disables the automatic callback/direct-dispatch routes while retaining the owned runtime and standalone loader. The broader existing Rhino/KubeJS optimization switches still apply to their respective code contracts. Disabling Rhino integration or failing the standalone context/wrapping contracts leaves the owned loader unavailable rather than running an unknown adapter.

Each route checks executable code and required fields before installation; the direct listener route also checks the complete standard interface and its declared exceptions; version labels alone do not select a route. If Rhino's callback bridge changes while KubeJS remains unchanged, the direct KubeJS route also stays off. Unsupported operations keep their native backend. A callback that has begun executing is never retried through a fallback backend.

CI compares observable callback behavior with takeover enabled and disabled, unchanged code under different version labels, combined changed targets and an isolated changed-Rhino bridge. It also runs HeroClock with Rhino and no KubeJS, plus the existing optional-mod matrix. The fixtures verify direct/stock route selection, receivers, return conversion, default methods, object-method mutation, custom wrapping, live chain appends, errors, standalone reload/failure cleanup and datapack-driven timer changes. See [VALIDATION.md](VALIDATION.md) for the completed build evidence. Target-pack TPS/FPS gains still require a matched gameplay profile.

## Remaining replacement work

To make KubeJS a passive compatibility shell, HeroClock still needs a compatible legacy loader and script-type lifecycle, event registration/result APIs, recipe/registry/startup handling, client integrations, custom bindings/wrappers, third-party plugin integration and native timer semantics. Build and validate these separately while retaining stock behavior for unsupported surfaces. An eventual Rhino replacement additionally needs JavaScript language, coercion, overload resolution and context behavior parity. The current update does not claim those replacements exist.
