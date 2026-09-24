package com.heroclock.api;

import com.heroclock.runtime.ScriptTasks;
import com.heroclock.runtime.ScriptTelemetry;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

public final class HeroScriptAPI {
    public record BoundaryStats(long calls, long outermostNanos) {}
    private HeroScriptAPI() {}

    public static boolean batch(MinecraftServer server, String namespace, String key, Iterator<?> items,
                                int itemsPerTick, Consumer<Object> action) {
        return ScriptTasks.batch(server, namespace, key, items, itemsPerTick, action);
    }
    public static void cancelNamespace(MinecraftServer server, String namespace) {
        ScriptTasks.cancelNamespace(server, namespace);
    }
    public static void setProfilingEnabled(boolean enabled) { ScriptTelemetry.setEnabled(enabled); }
    public static boolean profilingEnabled() { return ScriptTelemetry.enabled(); }
    public static Map<String, BoundaryStats> profile() { return ScriptTelemetry.snapshot(); }
}
