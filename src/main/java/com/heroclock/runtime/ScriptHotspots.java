package com.heroclock.runtime;

import com.heroclock.api.HeroScriptAPI.HotspotKey;
import com.heroclock.api.HeroScriptAPI.HotspotSnapshot;
import com.heroclock.api.HeroScriptAPI.HotspotStats;
import java.util.HashMap;
import java.util.Map;

public final class ScriptHotspots {
    private static final int LIMIT = 256;
    private static final Map<HotspotKey, long[]> COUNTERS = new HashMap<>();
    private static long dropped;
    private static volatile boolean enabled;

    private ScriptHotspots() {}

    public static boolean enabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }

    public static synchronized void record(String kind, String owner, String detail, long units, long nanos) {
        var key = new HotspotKey(label(kind), label(owner), label(detail));
        long[] counts = COUNTERS.get(key);
        if (counts == null) {
            if (COUNTERS.size() == LIMIT) { dropped++; return; }
            counts = new long[3];
            COUNTERS.put(key, counts);
        }
        counts[0]++;
        counts[1] += units;
        counts[2] += nanos;
    }

    public static synchronized HotspotSnapshot snapshot() {
        Map<HotspotKey, HotspotStats> result = new HashMap<>();
        COUNTERS.forEach((key, value) -> result.put(key, new HotspotStats(value[0], value[1], value[2])));
        return new HotspotSnapshot(Map.copyOf(result), dropped);
    }

    public static synchronized void reset() { COUNTERS.clear(); dropped = 0; }

    private static String label(String value) {
        if (value == null) return "<null>";
        return value.length() <= 256 ? value : value.substring(0, 256);
    }
}
