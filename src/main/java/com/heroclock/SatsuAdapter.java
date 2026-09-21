package com.heroclock;

import com.heroclock.api.HeroClockAPI;
import com.heroclock.runtime.ServerRuntime;
import com.heroclock.runtime.TickMath;
import com.heroclock.runtime.WorkQueue;
import net.minecraft.commands.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class SatsuAdapter {
    private static final String KILL_TAG = "sentinel_kill";
    private static final String WORK_KEY = "satsu.sentinel_kill";
    private static final ResourceLocation TICK_FUNCTION = new ResourceLocation("satsu_iron_man_addon", "tick");
    private static CommandFunction checkedFunction;
    private static boolean compatible;

    private SatsuAdapter() {}

    public static boolean handles(String tag) { return KILL_TAG.equals(tag); }

    public static boolean matches(CommandFunction function) {
        if (function == null || !TICK_FUNCTION.equals(function.getId())) return false;
        if (function != checkedFunction) {
            var entries = function.getEntries();
            compatible = entries.length == 1 && entries[0] instanceof CommandFunction.CommandEntry && entries[0].toString().equals("kill @e[tag=sentinel_kill]");
            checkedFunction = function;
        }
        return compatible;
    }

    private static boolean active(MinecraftServer server) {
        return server.getFunctions().get(TICK_FUNCTION).map(SatsuAdapter::matches).orElse(false);
    }

    public static long check(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity.isRemoved() || !entity.getTags().contains(KILL_TAG)) {
            return Long.MAX_VALUE;
        }
        MinecraftServer server = level.getServer();
        if (!server.isSameThread() || !active(server)) return TickMath.add(level.getGameTime(), 20);
        long now = HeroClockAPI.now(entity);
        WorkQueue queue = ServerRuntime.queue(server);
        if (!queue.contains(entity.getUUID(), WORK_KEY)) {
            boolean accepted = queue.submit(entity.getUUID(), WORK_KEY, TickMath.add(now, 1), () -> {
                if (active(server) && !entity.isRemoved() && entity.getTags().contains(KILL_TAG)) entity.kill();
                return true;
            });
            if (!accepted) return TickMath.add(level.getGameTime(), 1);
        }
        return TickMath.add(level.getGameTime(), 2);
    }

    public static boolean suppressStockTick(CommandFunction function, MinecraftServer server) {
        return server.isSameThread() && matches(function) && ServerRuntime.queue(server).size() < 4000;
    }

    public static void clear() {
        checkedFunction = null;
        compatible = false;
    }
}
