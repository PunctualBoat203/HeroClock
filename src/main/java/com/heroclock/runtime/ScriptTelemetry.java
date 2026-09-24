package com.heroclock.runtime;

import com.heroclock.api.HeroScriptAPI.BoundaryStats;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public final class ScriptTelemetry {
    public static final int RHINO = 0;
    public static final int KUBEJS = 1;
    private static final LongAdder[] CALLS = {new LongAdder(), new LongAdder()};
    private static final LongAdder[] NANOS = {new LongAdder(), new LongAdder()};
    private static final ThreadLocal<State> LOCAL = ThreadLocal.withInitial(State::new);
    private static volatile boolean enabled;
    private ScriptTelemetry() {}

    private static final class State {
        final int[] depth = new int[2];
        final long[] start = new long[2];
    }

    public static boolean enabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }

    public static boolean enter(int boundary) {
        if (!enabled) return false;
        State state = LOCAL.get();
        CALLS[boundary].increment();
        if (state.depth[boundary]++ == 0) state.start[boundary] = System.nanoTime();
        return true;
    }

    public static void exit(int boundary) {
        State state = LOCAL.get();
        if (--state.depth[boundary] == 0) NANOS[boundary].add(System.nanoTime() - state.start[boundary]);
    }

    public static Map<String, BoundaryStats> snapshot() {
        return Map.of("rhino_calls", stats(RHINO), "kubejs_handlers", stats(KUBEJS));
    }

    private static BoundaryStats stats(int boundary) {
        return new BoundaryStats(CALLS[boundary].sum(), NANOS[boundary].sum());
    }
}
