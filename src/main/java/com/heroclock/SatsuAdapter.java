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

/**
 * Narrow runtime adapter for the verified Satsu Iron Man 3.5.3 profile.
 *
 * <p>The stock addon runs {@code kill @e[tag=sentinel_kill]} from its global
 * tick function every tick. HeroClock replaces that world scan with keyed
 * lifecycle work for entities that actually carry the tag. Unknown Satsu
 * versions keep the stock function.</p>
 */
public final class SatsuAdapter {
    private static final String MOD_ID = "satsu_iron_man_addon";
    private static final String KILL_TAG = "sentinel_kill";
    private static final String WORK_KEY = "satsu.sentinel_kill";
    private static final ResourceLocation TICK_FUNCTION =
            new ResourceLocation("satsu_iron_man_addon", "tick");

    private SatsuAdapter() {}

    public static boolean handles(String tag) {
        return KILL_TAG.equals(tag) && VersionGuard.isVerifiedProfile(MOD_ID);
    }

    public static long check(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || entity.isRemoved()
                || !handles(KILL_TAG)
                || !entity.getTags().contains(KILL_TAG)) {
            return Long.MAX_VALUE;
        }

        MinecraftServer server = level.getServer();
        if (!server.isSameThread()) {
            return TickMath.add(level.getGameTime(), 1);
        }

        long now = HeroClockAPI.now(entity);
        WorkQueue queue = ServerRuntime.queue(server);
        if (!queue.contains(entity.getUUID(), WORK_KEY)) {
            boolean accepted = queue.submit(
                    entity.getUUID(),
                    WORK_KEY,
                    TickMath.add(now, 1),
                    () -> {
                        if (!entity.isRemoved() && entity.getTags().contains(KILL_TAG)) {
                            entity.kill();
                        }
                        return true;
                    });
            if (!accepted) {
                return TickMath.add(level.getGameTime(), 1);
            }
        }
        return TickMath.add(level.getGameTime(), 2);
    }

    public static boolean suppressStockTick(CommandFunction function, MinecraftServer server) {
        if (!VersionGuard.isVerifiedProfile(MOD_ID) || !TICK_FUNCTION.equals(function.getId())) {
            return false;
        }
        if (!server.isSameThread()) {
            return false;
        }
        // Fail open if HeroClock's bounded queue is close to saturation.
        return ServerRuntime.queue(server).size() < 4000;
    }
}
