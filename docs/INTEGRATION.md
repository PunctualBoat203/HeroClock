# HeroClock integration API v1

Author: PunctualBoat. Minecraft 1.20.1 / Forge 47.x.

Use `HeroClock-2.2.21-api.jar` as a compile-only dependency and install the full HeroClock mod at runtime. The standalone API artifact is also embedded inside the normal runtime JAR at `META-INF/heroclock/HeroClock-2.2.21-api.jar` so developers can extract it directly from the distributed mod. The embedded copy is an inert resource, not a Forge JarJar dependency, and is byte-for-byte identical to the separately produced API JAR. Do not install the API JAR as a mod and do not bundle/shade it into another mod.

The API artifact contains only supported API facades and value records, not the scheduler, compatibility inspectors, cleanup implementations or mixins. The implementation stays outside the supported integration contract. Existing All Rights Reserved licensing is unchanged. A small API artifact is not a copy-protection mechanism for the separately distributed runtime or public repository.

## Java addons

```java
if (HeroIntegrationAPI.apiVersion() == 1) {
    HeroClockAPI.set(entity, "myaddon:cooldown", 200);
    boolean accepted = HeroWorkAPI.submit(server, entity.getUUID(), "myaddon:refresh", () -> {
        refreshSmallBatch();
        return finished();
    });
}
```

Use `HeroClockAPI` for persistent entity/block-entity timers; `HeroWorkAPI` for keyed scheduling, coalescing, cancellation and cooperative incremental work; and `HeroFunctionAPI` to defer a registered datapack function. `HeroIntegrationAPI` exposes the API version, capability names, immutable compatibility decisions and read-only queue metrics. No mutable queues, property caches or mixin internals are exposed through these facades.

Invoke gameplay APIs on the server thread. Return `false` from a work step to continue next tick and `true` when complete. Check the submission result: a full queue rejects new work instead of growing indefinitely. Use your addon namespace in keys; use an entity UUID as owner when its work should be cancelled on unload. Each step must stay small because callbacks and Minecraft functions cannot be preempted. Jobs are transient; persistent timers and your own saved domain state are the recovery mechanism after restart.

## KubeJS / Palladium addon scripts

```javascript
const HeroClock = Java.loadClass('com.heroclock.api.HeroClockAPI');
HeroClock.set(entity, 'myaddon:cooldown', 200);
if (HeroClock.expired(entity, 'myaddon:cooldown')) {
    // Run your addon action.
}
```

KubeJS users do not need to extract or install the API JAR; the same public `com.heroclock.api.*` classes are already present in the installed runtime mod. Use event handlers rather than registering duplicate global scripts. Installing HeroClock does not automatically migrate arbitrary addon scoreboards or script loops.

## Datapacks / addonpacks

Commands require permission level 2, matching normal function execution. Timer commands operate on the executing entity, so `execute as` controls the target explicitly:

```mcfunction
execute as @a run heroclock timer set myaddon:cooldown 200
execute as @a store result score @s cooldown run heroclock timer remaining myaddon:cooldown
heroclock work schedule myaddon:refresh 20 myaddon:refresh
heroclock work cancel myaddon:refresh
execute as @a at @s run heroclock script emit myaddon myaddon:cooldown
heroclock status
```

Create the `cooldown` objective yourself if you want that explicit scoreboard export; HeroClock does not maintain a scoreboard clock. Remaining results clamp to the command system's maximum integer. Supported commands are `timer set`, `timer add`, `timer remaining`, `timer clear`, `work schedule`, `work cancel`, `script emit`, `script reload`, `script status` and `status`. Timer/work keys are resource-location names up to 96 characters using letters, digits, `_`, `.`, `:`, and `-`.

`script emit` calls listeners previously registered by a Java addon, HeroClock standalone script or compatible KubeJS server script. It passes the current command source and returns the completed listener count; an unregistered event returns zero. See [owned scripting](OWNED_SCRIPTING.md) for a matching registration example and event naming rules.

Function jobs retain command permissions, executor, position and dimension. The same executor/key replaces a pending function job. Entity work is cancelled on unload; console/level work is scoped to the dimension. At execution, the function is resolved again, so reloads use the current function body and removed functions are safely dropped. Scheduled functions are not persisted through server restarts. Splitting expensive work into separate small functions is the pack author's responsibility.

## Scripting support

The runtime and embedded API provide `HeroScriptAPI` for bounded batches and opt-in diagnostics, plus `HeroScriptRuntime` for owned events, scheduling and lifecycle cleanup. Java addons can register without KubeJS/Rhino installed. See [SCRIPTING.md](SCRIPTING.md) and [OWNED_SCRIPTING.md](OWNED_SCRIPTING.md). Automatic scripting patches remain independent of developer API calls. HeroClock also loads its own server scripts with Rhino and no KubeJS; see [automatic takeover and standalone scripts](AUTOMATIC_TAKEOVER.md). `HeroScriptAPI.reloadServerScripts/serverScripts` expose loader control/status, `executor(source)` provides mapped entity access, and `takeover()` reports cumulative adapter-registration counts. The additional `native_scripting_takeover` and `standalone_server_scripts` capabilities describe available interfaces, not a guarantee that an installed backend passed compatibility checks.
