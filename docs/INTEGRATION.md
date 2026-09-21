# HeroClock integration API v1

Author: PunctualBoat. Minecraft 1.20.1 / Forge 47.x.

Use `HeroClock-2.2.16-api.jar` as a compile-only dependency and install the full HeroClock mod at runtime. Do not install or bundle the API JAR as a mod. It contains only supported API facades and value records, not the scheduler, compatibility inspectors, cleanup implementations or mixins. The implementation stays outside the supported integration contract. Existing All Rights Reserved licensing is unchanged. A small API artifact is not a copy-protection mechanism for the separately distributed runtime or public repository.

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

Use event handlers rather than registering duplicate global scripts. Installing HeroClock does not automatically migrate arbitrary addon scoreboards or script loops.

## Datapacks / addonpacks

Commands require permission level 2, matching normal function execution. Timer commands operate on the executing entity, so `execute as` controls the target explicitly:

```mcfunction
execute as @a run heroclock timer set myaddon:cooldown 200
execute as @a store result score @s cooldown run heroclock timer remaining myaddon:cooldown
heroclock work schedule myaddon:refresh 20 myaddon:refresh
heroclock work cancel myaddon:refresh
heroclock status
```

Create the `cooldown` objective yourself if you want that explicit scoreboard export; HeroClock does not maintain a scoreboard clock. Remaining results clamp to the command system's maximum integer. Supported commands are `timer set`, `timer add`, `timer remaining`, `timer clear`, `work schedule`, `work cancel` and `status`. Command keys are resource-location names up to 96 characters using letters, digits, `_`, `.`, `:`, and `-`.

Function jobs retain command permissions, executor, position and dimension. The same executor/key replaces a pending function job. Entity work is cancelled on unload; console/level work is scoped to the dimension. At execution, the function is resolved again, so reloads use the current function body and removed functions are safely dropped. Scheduled functions are not persisted through server restarts. Splitting expensive work into separate small functions is the pack author's responsibility.
