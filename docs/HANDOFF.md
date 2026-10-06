# HeroClock scripting development handoff

Author: PunctualBoat. Development version: **2.2.21** on `improve-clock-runtime`.

## Current checkpoint

HeroClock can load its own server scripts with Rhino alone. When KubeJS is installed, matching existing interface callbacks and listener invocations use HeroClock execution adapters automatically, without script rewrites. The intended direction is an independent scripting stack with KubeJS as a compatibility shell and a later independent JavaScript engine. This checkpoint implements callback takeover and standalone loading, not complete KubeJS replacement.

- `RhinoNativeCallbackMixin` replaces the standard interface invocation handler at proxy construction. It retains the proxy class/identity, Object/default methods, context lock, custom wrapping, receiver identity, result conversion and dynamic object-member lookup. Primary callback metadata is cached; custom InterfaceAdapter subclasses remain upstream.
- `KubeNativeDispatchMixin` prepares a direct adapter once per listener container. The public handler stays original; the stock loop retains ordering, live chain appends, cancellation and exception handling. Checked exceptions match Proxy behavior. Addon-defined listener subinterfaces retain normal proxy dispatch; the standard interface and its declared exceptions have their own guard. Detailed profiling uses a cached wrapper and excludes the older profiler when active.
- `config/heroclock/server_scripts` loads sorted UTF-8 files on server start or `/heroclock script reload`, with isolated file variables and a shared root/context. Files receive HeroRuntime, public API facades, server, `Java.loadClass` and console. Limits: 128 files, 1 MiB per file, bounded reads, no script symlinks.
- Each file owns its runtime handles. Failed loads, reload and shutdown release owned listeners, jobs and pending setup. Old world mutations are not rolled back; callbacks are never retried on another backend. Raw HeroWork and legacy HeroScript batches retain explicit cancellation requirements.
- The embedded API adds `HeroScriptAPI.reloadServerScripts/serverScripts`, `executor(source)` and `takeover()`. No Rhino/KubeJS types enter the supported API. Java addons and datapacks retain access to owned events, scheduling, batching and diagnostics; Java registration works without scripting mods.
- Automatic routes require executable-code/field contracts and shared Rhino peers. Relabeled unchanged code remains enabled. Changed Rhino callbacks disable dependent KubeJS direct dispatch. Missing/changed standalone prerequisites produce status rather than invoking an unknown engine. `heroclock.disableNativeScriptingTakeover` restores native callback routes independently of owned scripting.

See [AUTOMATIC_TAKEOVER.md](AUTOMATIC_TAKEOVER.md) for actual ownership and standalone examples, [OWNED_SCRIPTING.md](OWNED_SCRIPTING.md) for runtime semantics, and [SCRIPTING.md](SCRIPTING.md) for retained optimizations and diagnostics. The API artifact is compile-only and embedded as an inert byte-identical resource. No dependency JARs are bundled. Author metadata remains PunctualBoat.

## Validation

The initial automatic-takeover checkpoint at `af8d081889e6024851fd57e539f555e4ca179616` passed CI run `37532010088`: build/unit/API checks and sixteen required GameTests in all ten environments. A strengthened standalone test then caught the absence of a global `java` object in this Rhino build. The follow-up provides HeroClock's own `Java.loadClass` binding and guards Java-class wrapping. The final review also protects addon listener default methods and fingerprints declared exceptions. Final checkpoint validation is in progress; see [VALIDATION.md](VALIDATION.md) for completed evidence.

The matrix includes stock-dispatch comparisons, isolated changed-Rhino fallback and Rhino without KubeJS, plus the existing supplied/relabeled/changed scripting and Forge/Palladium/Curios fixtures. Optional tests skip when dependencies are absent. Target-pack gameplay and TPS/FPS have not been measured. The previous 2.2.18 allocation fixes, 2.2.19 attribution and 2.2.20 owned-runtime checks remain in place. Historical real-pack findings remain in [the archived handoff](archive/2.2.16-HANDOFF.md).

## Next work

1. Capture a matched target-pack profile with takeover enabled and disabled, using [the supplied Spark analysis](SCRIPTING_PROFILE_2026-09-24.md) and [Astra priorities](ASTRA_OPTIMIZATION_TARGETS.md). Compare the specific boundary work and gameplay behavior, not inclusive nested totals or registration counts.
2. Audit a concrete legacy KubeJS subsystem and build its compatible implementation before taking ownership. Remaining surfaces include script-type lifecycle/loading, event registration/result APIs, recipes/registries/startup, client integrations, plugin/custom binding/wrapper APIs and native wall-clock/game-tick timers. Existing addons may depend on KubeJS classes and declared mod dependencies; replacing an ID or disabling KubeJS wholesale cannot satisfy those contracts.
3. Keep standalone APIs independent of the engine. A future Rhino replacement must establish JavaScript semantics, coercion, overload selection and context behavior before taking over existing scripts. The internal loader interface is not an externally supported engine-plugin API.

Never suppress callbacks, reuse mutable receivers, remove synchronization or retry a partially executed callback to manufacture a speedup. Extend guards and behavioral comparisons with each new ownership boundary. Continue committing/pushing checkpoints to `improve-clock-runtime`; keep the draft PR unmerged until target-pack behavior and performance have been reviewed.
