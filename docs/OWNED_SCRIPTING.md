# HeroClock-owned server scripting

HeroClock 2.2.20 — PunctualBoat. Minecraft 1.20.1 / Forge 47.x.

HeroClock owns the namespaced event dispatch, lifecycle and bounded-work integration described here. Rhino still evaluates JavaScript, and KubeJS still loads scripts. Existing KubeJS events and timers keep their behavior. Only callbacks explicitly registered with this API use the new integration.

The direct Rhino adapter retains the original context lock and wrap factory, passes the manager's top-level scope as `this`, and calls `Context.callSync` without a Java functional-interface proxy for each registered JavaScript callback. Listener snapshots rebuild on registration changes, not on each event. Idle namespaces without a tick listener skip tick dispatch. These remove specific integration work; a target-pack profile is still needed to measure the net effect.

## KubeJS server scripts

On a compatible server script manager, HeroClock adds the `HeroRuntime` global before scripts load. Register setup during script loading:

```javascript
const HeroClock = Java.loadClass('com.heroclock.api.HeroClockAPI');

if (typeof HeroRuntime !== 'undefined') {
    const accepted = HeroRuntime.onServer('myaddon', (runtime, server) => {
        runtime.on('myaddon:cooldown', 'start', source => {
            const entity = source.getEntity();
            if (entity != null) HeroClock.set(entity, 'myaddon:cooldown', 40);
        });
    });
    if (!accepted) console.warn('HeroClock setup queue is full');
}
```

`onServer(namespace, setup)` queues setup for the next HeroClock end-of-server-tick phase. It can be called before a server is available. Pending setup for the same scope/namespace is replaced by the latest callback. Check its boolean result. Setup receives `(runtime, server)` and runs once on the server thread. If it throws, its namespace closes and its registered listeners/jobs are removed; completed world changes are not rolled back and the callback is never retried.

When already on the server thread, `HeroRuntime.forServer(server, namespace)` opens or reuses the same scope-owned runtime immediately. Choose one setup location per namespace. A namespace cannot be shared between different script scopes or between a scope and the Java API; emit events across that boundary instead.

Load/unload invalidates that manager's old runtime handles and cancels its pending setup, listeners and work. Closing a namespace is also explicit through `runtime.close()`. A callback already executing can finish; later listeners and batch items stop, and callbacks still waiting for the context lock recheck validity before entering JavaScript. The new scope can register the namespace again after reload. These hooks cover KubeJS `ScriptManager.load/unload`; custom loaders outside those methods need their own lifecycle integration.

## Events and work

| Method | Behavior |
| --- | --- |
| `on(event, key, callback)` | Registers or replaces a keyed listener. Returns false if a new registration exceeds the listener limit. |
| `off(event, key)` | Removes that listener; returns whether it existed. |
| `emit(event, ...arguments)` | Runs listeners synchronously in registration order and returns the completed callback count. |
| `schedule(key, delayTicks, callback, ...arguments)` | Queues a single callback using game-tick delay; returns whether accepted. |
| `batch(key, javaIterator, itemsPerTick, callback)` | Consumes an iterator across ticks, passing one item to each callback; returns whether accepted. |
| `cancel(key)` | Cancels scheduled or batch work under that key. |
| `status()` | Returns an immutable snapshot: namespace, active, listeners, pending, calls and failures. |
| `close()` | Invalidates this runtime and releases its listeners and jobs. |

Register `runtime.on('heroclock:server_tick', 'tick', server => update(server))` for each end-of-server-tick event. Use `runtime.schedule('refresh', 20, value => refresh(value), value)` for deferred work. For a stable Java collection, use `runtime.batch('refresh', values.iterator(), 16, value => refresh(value))`. These examples assume your addon supplies `update`, `refresh` and the values. Check submission results and decide whether rejected work can be dropped, retried later, or kept in your own saved state.

Events are local to the namespace. Replacing a key preserves its position. A listener removed or replaced before its turn is skipped in that dispatch; newly added listeners begin with the next dispatch. Nested emissions use the current registrations. Each listener receives its own argument array, while the objects inside remain the original live objects. JavaScript callbacks receive positional arguments and their return values are ignored. Regular functions receive the manager's top-level scope as `this`; arrow functions retain their lexical `this`.

An event exception propagates and stops that dispatch without replay. Automatic tick dispatch logs runtime exceptions per namespace and continues with other namespaces; the next game tick is a new event. Scheduled/batch runtime exceptions terminate and log that job. Status `calls`/`failures` count registered callback invocations and thrown callback failures, including nested calls; setup and iterator failures are not included in those counters.

Scheduled and batch work shares HeroClock's 4,096-job queue, 128-step limit and 1 ms admission budget per tick. A running callback cannot be interrupted. Batches allow 1–256 items per step, and a cancelled/replaced/closed batch stops before its next item. Keep callbacks and iterator operations small and keep the iterator valid until completion. Synchronous event dispatch and setup callbacks are not time-budgeted.

The work queue drains before setup and tick dispatch. A delay of zero means the next eligible queue drain, not an inline call; work submitted from owned setup/tick callbacks first becomes eligible on a later tick. Repeated submissions under the same runtime/key replace pending work. Jobs are transient and cleared at server shutdown. They do not automatically cancel on entity unload; use `HeroWorkAPI` with an entity UUID owner for that behavior.

There are at most 128 active namespaces per server, 1,024 listeners per namespace, 32 nested event dispatches per runtime, and 128 pending setup entries. Names use 1–96 lowercase ASCII letters, digits, `_`, `.`, `-`; event/listener/job names also allow `:` and `/`. Invalid names throw. Full pending-setup/listener/work queues reject additions rather than growing without bound; exhausting the active-namespace limit throws.

## Java addon API

Compile against the standalone or embedded `HeroClock-2.2.20-api.jar`; install the full mod at runtime. No KubeJS or Rhino types appear in this public API, and Java runtime registration also works when those mods are absent.

```java
HeroScriptRuntime runtime = HeroScriptAPI.openRuntime(server, "myaddon");
runtime.on("myaddon:cooldown", "start", arguments -> {
    var source = (CommandSourceStack) arguments[0];
    if (source.getEntity() != null) {
        HeroClockAPI.set(source.getEntity(), "myaddon:cooldown", 40);
    }
});
```

Call `HeroScriptAPI.emit(server, "myaddon", "myaddon:cooldown", source)` to reach a Java- or script-owned namespace without taking ownership. It returns zero when no active namespace/listener exists. Use your own addon namespace, retain the returned handle and close it when your addon unloads its content. Server shutdown closes all handles.

Gameplay methods and status reads require the server thread. `close()` also accepts an off-thread call: it invalidates immediately and queues server-thread cleanup. The older `HeroScriptAPI.batch/cancelNamespace` facade has separate ownership and does not close owned runtimes; use the handle's `cancel/close` methods for this API.

`HeroIntegrationAPI.capabilities()` includes `owned_script_runtime`. This reports the Java API capability, not the optional Rhino binding's compatibility decision. The API version remains 1 because these are additive interfaces.

## Datapacks and addonpacks

Functions can emit an event registered by a Java addon or compatible server script:

```mcfunction
execute as @a at @s run heroclock script emit myaddon myaddon:cooldown
```

The command requires permission level 2 and passes the current `CommandSourceStack` as the only argument, retaining executor, position, dimension and permissions. Its result is the completed listener count. Unregistered events return zero. The command does not evaluate JavaScript or load scripts from datapack files. Datapacks can still use HeroClock's timer and deferred-function commands without a script integration.

## Compatibility and fallback

`KubeRuntimeMixin` checks the audited ScriptManager load/unload bodies and required fields. It also requires matching `RhinoRuntimeContext` and `RhinoRuntimeWrapping` contracts for the adapter's context, lock and wrapping methods. Version-label changes alone do not disable matching code. A changed or unavailable contract omits the binding and leaves KubeJS's own behavior intact; the Java API remains available. An existing `HeroRuntime` global is retained rather than replaced.

Check `HeroIntegrationAPI.compatibility()` and `/heroclock status`. Either scripting disable switch also disables this optional adapter. Scripts that require it should guard `typeof HeroRuntime`; a pack can select its native fallback before registering any callbacks. HeroClock never retries a callback on another backend after it has started. There is no automatic conversion of existing native KubeJS callbacks or timers.
