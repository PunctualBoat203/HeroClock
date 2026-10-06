package com.heroclock.api;

import com.heroclock.runtime.ScriptTasks;
import com.heroclock.runtime.ScriptTelemetry;
import com.heroclock.runtime.ScriptHotspots;
import com.heroclock.runtime.ScriptRuntimes;
import com.heroclock.runtime.ScriptTakeover;
import com.heroclock.runtime.ServerScripts;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;

public final class HeroScriptAPI {
    public record TakeoverStats(long nativeProxiesCreated, long directListenersCreated) {}
    public record ScriptLoadStatus(String engine, int loaded, int failed) {}
    public record BoundaryStats(long calls, long outermostNanos) {}
    public record HotspotKey(String kind, String owner, String detail) {}
    public record HotspotStats(long calls, long units, long inclusiveNanos) {}
    public record HotspotSnapshot(Map<HotspotKey, HotspotStats> entries, long dropped) {}
    private HeroScriptAPI() {}

    public static TakeoverStats takeover() { return ScriptTakeover.snapshot(); }
    public static ScriptLoadStatus reloadServerScripts(MinecraftServer server) { return ServerScripts.reload(server); }
    public static ScriptLoadStatus serverScripts(MinecraftServer server) { return ServerScripts.status(server); }
    public static Entity executor(CommandSourceStack source) { return source.getEntity(); }

    public static HeroScriptRuntime openRuntime(MinecraftServer server, String namespace) { return ScriptRuntimes.open(server, namespace); }
    public static int emit(MinecraftServer server, String namespace, String event, Object... arguments) {
        return ScriptRuntimes.emit(server, namespace, event, arguments);
    }

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
    public static void setHotspotProfilingEnabled(boolean enabled) { ScriptHotspots.setEnabled(enabled); }
    public static boolean hotspotProfilingEnabled() { return ScriptHotspots.enabled(); }
    public static HotspotSnapshot hotspots() { return ScriptHotspots.snapshot(); }
    public static void resetHotspots() { ScriptHotspots.reset(); }
}
