package com.heroclock.runtime;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class WorkQueue {
    private record Key(Object owner, String name) {}
    private record Task(Key key, long due, BooleanSupplier work) {}

    private final TreeMap<Long, LinkedHashSet<Task>> due = new TreeMap<>();
    private final Map<Key, Task> pending = new HashMap<>();
    private final Map<Object, LinkedHashSet<Key>> owners = new HashMap<>();
    private final Thread thread = Thread.currentThread();
    private final int capacity;
    private final LongSupplier nanos;
    private final Consumer<RuntimeException> errors;
    private long executed;
    private long rejected;
    private long failed;
    private boolean draining;
    private Task running;
    private boolean runningCancelled;

    public WorkQueue(int capacity, Consumer<RuntimeException> errors) {
        this(capacity, errors, System::nanoTime);
    }

    WorkQueue(int capacity, Consumer<RuntimeException> errors, LongSupplier nanos) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
        this.errors = Objects.requireNonNull(errors);
        this.nanos = Objects.requireNonNull(nanos);
    }

    public boolean submit(Object owner, String name, long tick, BooleanSupplier work) {
        checkThread();
        Key key = new Key(Objects.requireNonNull(owner), Objects.requireNonNull(name));
        Objects.requireNonNull(work);
        Task old = pending.get(key);
        int reserved = running != null && !running.key.equals(key) && !pending.containsKey(running.key) ? 1 : 0;
        if (old == null && pending.size() + reserved >= capacity) { rejected++; return false; }
        if (old != null) remove(old);
        Task task = new Task(key, tick, work);
        pending.put(key, task);
        due.computeIfAbsent(tick, ignored -> new LinkedHashSet<>()).add(task);
        owners.computeIfAbsent(owner, ignored -> new LinkedHashSet<>()).add(key);
        return true;
    }

    public boolean contains(Object owner, String name) {
        checkThread();
        return pending.containsKey(new Key(owner, name));
    }

    public boolean cancel(Object owner, String name) {
        checkThread();
        Task task = pending.get(new Key(owner, name));
        if (running != null && running.key.equals(new Key(owner, name))) {
            runningCancelled = true;
            if (task == null) return true;
        }
        if (task == null) return false;
        remove(task);
        return true;
    }

    public void cancelOwner(Object owner) {
        checkThread();
        if (running != null && running.key.owner.equals(owner)) runningCancelled = true;
        LinkedHashSet<Key> keys = owners.get(owner);
        if (keys == null) return;
        for (Key key : keys.toArray(Key[]::new)) remove(pending.get(key));
    }

    public int drain(long tick, int maxSteps, long maxNanos) {
        checkThread();
        if (draining) throw new IllegalStateException("Work queue cannot drain recursively");
        if (maxSteps < 1 || maxNanos < 1) return 0;
        draining = true;
        long start = nanos.getAsLong();
        int steps = 0;
        try {
            while (steps < maxSteps && nanos.getAsLong() - start < maxNanos) {
                Map.Entry<Long, LinkedHashSet<Task>> first = due.firstEntry();
                if (first == null || first.getKey() > tick) break;
                Task task = first.getValue().iterator().next();
                remove(task);
                steps++;
                executed++;
                running = task;
                runningCancelled = false;
                try {
                    if (!task.work.getAsBoolean() && !runningCancelled && !pending.containsKey(task.key)) {
                        submit(task.key.owner, task.key.name, TickMath.add(tick, 1), task.work);
                    }
                } catch (RuntimeException failure) {
                    failed++;
                    errors.accept(failure);
                } finally { running = null; }
            }
            return steps;
        } finally { draining = false; }
    }

    public void clear() {
        checkThread();
        runningCancelled = true;
        due.clear();
        pending.clear();
        owners.clear();
    }

    public int size() { checkThread(); return pending.size(); }
    public long executed() { checkThread(); return executed; }
    public long rejected() { checkThread(); return rejected; }
    public long failed() { checkThread(); return failed; }

    private void remove(Task task) {
        pending.remove(task.key);
        LinkedHashSet<Task> bucket = due.get(task.due);
        bucket.remove(task);
        if (bucket.isEmpty()) due.remove(task.due);
        LinkedHashSet<Key> keys = owners.get(task.key.owner);
        keys.remove(task.key);
        if (keys.isEmpty()) owners.remove(task.key.owner);
    }

    private void checkThread() {
        if (Thread.currentThread() != thread) throw new IllegalStateException("Work queue is confined to its server thread");
    }
}
