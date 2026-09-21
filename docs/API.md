# API and runtime behavior

## Deadlines

```java
HeroClockAPI.set(entity, "myaddon.cooldown", 200);
HeroClockAPI.remaining(entity, "myaddon.cooldown");
HeroClockAPI.add(entity, "myaddon.cooldown", 40);
HeroClockAPI.clear(entity, "myaddon.cooldown");
```

Existing API method signatures are retained. Timer storage accepts Minecraft entities and block entities. `now` also accepts a level or server. Use the server thread for gameplay timers. Unsupported objects, off-thread server access, client writes, missing levels, and non-finite seconds fail explicitly rather than pretending the time is zero.

Timers measure game ticks, not wall-clock time or day time. They pause while the server is stopped and advance while the server is running, including while their entity's chunk is unloaded. Use namespaced keys of up to 96 ASCII letters, digits, `_`, `.`, `:`, and `-`. Historical sanitization is retained to read existing saved keys; distinct invalid keys can still normalize to the same key, so integrations should supply valid unique names. Reads do not add NBT data or update scoreboards. Expired entries remain available through `deadline` until cleared. Timers are not automatically synced to client HUDs.

## Deferred and incremental work

```java
boolean accepted = HeroWorkAPI.schedule(server, player.getUUID(), "refresh", 20, () -> {
    refreshSmallBatch();
    return finished();
});
```

`true` from a work step means complete; `false` schedules its continuation for the next tick. Submitting the same owner/key replaces the pending job, coalescing repeated requests. Submissions return `false` when capacity is exhausted. Use stable owner keys such as UUIDs, and handle rejection. Cancellation removes the pending callback immediately. Owner cancellation and `clear` during a callback also prevent its continuation. Exceptions remove that job and are logged; other jobs continue.

Callbacks are transient and discarded on server stop. They are not serialized. Persist domain state separately when recovery matters, as the helper cleanup integration does with NBT deadlines. No callbacks run asynchronously against Minecraft world state.

## Temporary helper cleanup

Recognized helper tags are the ones used by the original 2.2.10 persistence guard. Players are excluded; the lightning and shadow-field rules additionally check entity type. Existing scoreboard age is read once when migrating a marker, then elapsed-time checks use its saved deadline. Tags are discovered at the first base tick and on `addTag`/`removeTag` calls. Mods directly mutating the returned tag set after that first tick bypass those notifications.

Block cleanup starts at the original timeout and finishes over multiple ticks. It changes only the original rule's target block within its original volume. A needed unloaded neighboring chunk defers cleanup. The `a.ice` rule still calls the addon's own `afomni:iceberg/break_ice` function; that function is not preemptible. Other original addon commands are not automatically made cheap by this API.

Palladium patches can be disabled with `-Dheroclock.disablePalladiumOptimizations=true`. Unsupported Palladium versions keep their own behavior. The optional OmniOptimizer bridge retains its existing external class name for binary compatibility; HeroClock's authorship is PunctualBoat.
