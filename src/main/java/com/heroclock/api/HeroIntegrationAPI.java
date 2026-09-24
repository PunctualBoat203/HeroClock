package com.heroclock.api;

import com.heroclock.HeroClock;
import com.heroclock.compat.CompatibilityGate;
import com.heroclock.runtime.ServerRuntime;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public final class HeroIntegrationAPI {
    public record WorkStatus(int pending, long executed, long rejected, long failed) {}
    private HeroIntegrationAPI() {}

    public static int apiVersion() { return 1; }
    public static String version() { return HeroClock.VERSION; }
    public static Set<String> capabilities() {
        return Set.of("deadlines", "bounded_work", "deferred_functions", "compatibility_status", "script_batches", "script_boundary_profiling");
    }
    public static Map<String, String> compatibility() { return CompatibilityGate.decisions(); }
    public static WorkStatus workStatus(MinecraftServer server) {
        var queue = ServerRuntime.queue(server);
        return new WorkStatus(queue.size(), queue.executed(), queue.rejected(), queue.failed());
    }
}
