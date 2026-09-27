package com.heroclock.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScriptHotspotsTest {
    @AfterEach void clear() { ScriptHotspots.reset(); ScriptHotspots.setEnabled(false); }

    @Test void limitsDistinctLabelsButContinuesExistingCounters() {
        ScriptHotspots.reset();
        for (int i = 0; i < 300; i++) ScriptHotspots.record("member", "receiver", "field" + i, 1, 0);
        ScriptHotspots.record("member", "receiver", "field0", 2, 7);
        var snapshot = ScriptHotspots.snapshot();
        assertEquals(256, snapshot.entries().size());
        assertEquals(44, snapshot.dropped());
        var first = snapshot.entries().entrySet().stream().filter(e -> e.getKey().detail().equals("field0")).findFirst().orElseThrow().getValue();
        assertEquals(2, first.calls());
        assertEquals(3, first.units());
        assertEquals(7, first.inclusiveNanos());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.entries().clear());
        ScriptHotspots.reset();
        assertEquals(256, snapshot.entries().size());
        assertTrue(ScriptHotspots.snapshot().entries().isEmpty());
    }

    @Test void boundsLabelLengthAndHandlesConcurrentUpdates() throws Exception {
        ScriptHotspots.reset();
        String label = "x".repeat(10000);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            var tasks = new java.util.ArrayList<java.util.concurrent.Callable<Void>>();
            for (int i = 0; i < 4; i++) tasks.add(() -> {
                for (int j = 0; j < 1000; j++) ScriptHotspots.record("wrapper", label, null, 2, 0);
                return null;
            });
            for (var result : pool.invokeAll(tasks)) result.get();
        } finally { pool.shutdownNow(); }
        var entry = ScriptHotspots.snapshot().entries().entrySet().iterator().next();
        assertEquals(256, entry.getKey().owner().length());
        assertEquals("<null>", entry.getKey().detail());
        assertEquals(4000, entry.getValue().calls());
        assertEquals(8000, entry.getValue().units());
    }
}
