package com.heroclock.runtime;

import com.heroclock.HeroClock;
import com.heroclock.api.HeroScriptRuntime;
import com.heroclock.api.HeroWorkAPI;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

public final class ScriptRuntimes {
    private static final String TICK_EVENT = "heroclock:server_tick";
    private static final Object JAVA_OWNER = new Object();
    private static final Map<MinecraftServer, ServerState> SERVERS = new IdentityHashMap<>();
    private record SetupKey(Object owner, String namespace) {}
    private static final Map<SetupKey, Consumer<MinecraftServer>> SETUPS = new LinkedHashMap<>();
    private static final class ServerState {
        final Map<String, Session> sessions = new LinkedHashMap<>();
        List<Session> snapshot = List.of();
        void changed() { snapshot = List.copyOf(sessions.values()); }
    }
    private ScriptRuntimes() {}

    public static synchronized boolean prepare(Object owner, String namespace, Consumer<MinecraftServer> action) {
        namespace(namespace); Objects.requireNonNull(action, "action");
        SetupKey key = new SetupKey(Objects.requireNonNull(owner, "owner"), namespace);
        if (SETUPS.size() == 128 && !SETUPS.containsKey(key)) return false;
        SETUPS.put(key, action);
        return true;
    }

    public static synchronized void cancelSetups(Object owner) { SETUPS.keySet().removeIf(key -> key.owner == owner); }

    private static synchronized Map<SetupKey, Consumer<MinecraftServer>> takeSetups() {
        if (SETUPS.isEmpty()) return Map.of();
        var result = new LinkedHashMap<>(SETUPS); SETUPS.clear(); return result;
    }

    public static HeroScriptRuntime open(MinecraftServer server, String namespace) {
        return open(server, namespace, JAVA_OWNER, () -> {});
    }

    public static Session open(MinecraftServer server, String namespace, Object owner, Runnable closed) {
        thread(server); namespace(namespace);
        Objects.requireNonNull(owner, "owner"); Objects.requireNonNull(closed, "closed");
        ServerState state = SERVERS.computeIfAbsent(server, ignored -> new ServerState());
        Session existing = state.sessions.get(namespace);
        if (existing != null) {
            if (!existing.active.get()) existing.release();
            else if (existing.owner == owner) return existing;
            else throw new IllegalStateException("Script namespace already belongs to another owner: " + namespace);
        }
        if (state.sessions.size() == 128) throw new IllegalStateException("HeroClock runtime limit reached");
        Session result = new Session(server, namespace, owner, closed);
        state.sessions.put(namespace, result); state.changed();
        return result;
    }

    public static int emit(MinecraftServer server, String namespace, String event, Object... args) {
        thread(server);
        ServerState state = SERVERS.get(server);
        Session session = state == null ? null : state.sessions.get(namespace);
        return session == null || !session.active.get() ? 0 : session.emit(event, args);
    }

    public static void tick(MinecraftServer server) {
        takeSetups().forEach((key, setup) -> {
            try { setup.accept(server); }
            catch (RuntimeException failure) { HeroClock.LOGGER.error("HeroClock script setup failed in {}", key.namespace, failure); }
        });
        ServerState state = SERVERS.get(server);
        if (state == null) return;
        for (Session session : state.snapshot) {
            if (!session.active.get() || !session.listeners.has(TICK_EVENT)) continue;
            try { session.emit(TICK_EVENT, server); }
            catch (RuntimeException failure) { HeroClock.LOGGER.error("HeroClock script tick failed in {}", session.namespace, failure); }
        }
    }

    public static void stop(MinecraftServer server) {
        thread(server);
        ServerState state = SERVERS.get(server);
        if (state != null) for (Session session : state.snapshot) { session.close(); session.release(); }
        SERVERS.remove(server);
        synchronized (ScriptRuntimes.class) { SETUPS.clear(); }
    }

    private static void thread(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (!server.isSameThread()) throw new IllegalStateException("Use HeroClock script runtimes on the server thread");
    }

    private static void namespace(String namespace) {
        ScriptListeners.name(namespace);
        if (namespace.indexOf(':') >= 0 || namespace.indexOf('/') >= 0) throw new IllegalArgumentException("Use an addon namespace");
    }

    public static final class Session implements HeroScriptRuntime {
        private final MinecraftServer server;
        private final String namespace;
        private Object owner;
        private Runnable closed;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private final ScriptListeners listeners = new ScriptListeners();
        private final Map<String, Object> jobs = new HashMap<>();
        private long calls;
        private long failures;
        private interface Step { boolean run(BooleanSupplier current); }
        private Session(MinecraftServer server, String namespace, Object owner, Runnable closed) {
            this.server = server; this.namespace = namespace; this.owner = owner; this.closed = closed;
        }

        private void check() {
            thread(server);
            if (!active.get()) throw new IllegalStateException("HeroClock script runtime has been closed or reloaded");
        }
        public boolean isActive() { return active.get(); }
        private void invoke(Callback callback, Object[] args) {
            calls++;
            try { callback.call(args); }
            catch (RuntimeException | Error failure) { failures++; throw failure; }
        }
        @Override public boolean on(String event, String key, Callback callback) { check(); return listeners.on(event, key, callback); }
        @Override public boolean off(String event, String key) { check(); return listeners.off(event, key); }
        @Override public int emit(String event, Object... args) {
            check(); return listeners.emit(event, args, active::get, this::invoke);
        }
        @Override public boolean schedule(String key, long delayTicks, Callback callback, Object... args) {
            check(); Objects.requireNonNull(callback, "callback");
            Object[] captured = Objects.requireNonNull(args, "arguments").clone();
            return submit(key, delayTicks, current -> { invoke(callback, captured); return true; });
        }
        @Override public boolean batch(String key, Iterator<?> items, int itemsPerTick, Callback callback) {
            check(); Objects.requireNonNull(items, "items"); Objects.requireNonNull(callback, "callback");
            if (itemsPerTick < 1 || itemsPerTick > 256) throw new IllegalArgumentException("Use 1 to 256 items per tick");
            return submit(key, 0, current -> {
                for (int i = 0; i < itemsPerTick && current.getAsBoolean() && items.hasNext(); i++) invoke(callback, new Object[]{items.next()});
                return !current.getAsBoolean() || !items.hasNext();
            });
        }
        private boolean submit(String key, long delay, Step step) {
            ScriptListeners.name(key);
            if (delay < 0) throw new IllegalArgumentException("Delay must not be negative");
            Object token = new Object();
            BooleanSupplier current = () -> active.get() && jobs.get(key) == token;
            boolean accepted = HeroWorkAPI.schedule(server, this, key, delay, () -> {
                boolean finished = true;
                try { return finished = !current.getAsBoolean() || step.run(current); }
                finally { if (finished) jobs.remove(key, token); }
            });
            if (accepted) jobs.put(key, token);
            return accepted;
        }
        @Override public boolean cancel(String key) {
            check(); ScriptListeners.name(key); jobs.remove(key);
            return HeroWorkAPI.cancel(server, this, key);
        }
        @Override public Status status() {
            thread(server); return new Status(namespace, active.get(), listeners.size(), jobs.size(), calls, failures);
        }
        @Override public void close() {
            if (!active.compareAndSet(true, false)) return;
            if (server.isSameThread()) release(); else server.execute(this::release);
        }
        private void release() {
            thread(server);
            ServerRuntime.cancelOwnerIfPresent(server, this);
            listeners.clear(); jobs.clear(); owner = null;
            ServerState state = SERVERS.get(server);
            if (state != null && state.sessions.remove(namespace, this)) state.changed();
            Runnable action = closed; closed = null;
            if (action != null) action.run();
        }
    }
}
