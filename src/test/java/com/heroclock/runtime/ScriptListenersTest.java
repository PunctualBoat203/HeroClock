package com.heroclock.runtime;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScriptListenersTest {
    private int emit(ScriptListeners bus, Object... args) {
        return bus.emit("test:event", args, () -> true, (callback, arguments) -> callback.call(arguments));
    }

    @Test void replacementsKeepOrderAndChangesDuringDispatchTakeEffectSafely() {
        var bus = new ScriptListeners();
        var seen = new ArrayList<Integer>();
        bus.on("test:event", "first", args -> {
            seen.add(1);
            bus.off("test:event", "second");
            bus.on("test:event", "late", later -> seen.add(3));
        });
        bus.on("test:event", "second", args -> seen.add(2));
        assertEquals(1, emit(bus));
        assertEquals(List.of(1), seen);
        bus.on("test:event", "first", args -> seen.add(4));
        assertEquals(2, emit(bus));
        assertEquals(List.of(1, 4, 3), seen);
    }

    @Test void errorsPropagateOnceAndArgumentsAreIsolated() {
        var bus = new ScriptListeners();
        var seen = new ArrayList<Object>();
        bus.on("test:event", "first", args -> args[0] = "changed");
        bus.on("test:event", "second", args -> seen.add(args[0]));
        Object[] args = {"original"};
        emit(bus, args);
        assertEquals("original", args[0]);
        assertEquals(List.of("original"), seen);
        var failure = new IllegalStateException("expected");
        bus.on("test:event", "first", values -> { throw failure; });
        assertSame(failure, assertThrows(IllegalStateException.class, () -> emit(bus, args)));
        assertEquals(1, seen.size());
    }

    @Test void limitsBacklogAndRecursionAndClearsInFlightListeners() {
        var bus = new ScriptListeners();
        for (int i = 0; i < 1024; i++) assertTrue(bus.on("test:event", "key" + i, args -> {}));
        assertFalse(bus.on("new:event", "overflow", args -> {}));
        assertTrue(bus.on("test:event", "key0", args -> {}));
        bus.clear();
        bus.on("test:event", "recursive", args -> emit(bus));
        assertThrows(IllegalStateException.class, () -> emit(bus));
        bus.clear();
        bus.on("test:event", "clear", args -> bus.clear());
        bus.on("test:event", "unreachable", args -> fail("Cleared listener ran"));
        assertEquals(1, emit(bus));
        assertEquals(0, bus.size());
    }
}
