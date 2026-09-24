package com.heroclock.api;

import com.heroclock.runtime.ServerRuntime;
import com.heroclock.runtime.TickMath;
import net.minecraft.server.MinecraftServer;
import java.util.function.BooleanSupplier;

public final class HeroWorkAPI {
    private HeroWorkAPI() {}

    public static boolean submit(MinecraftServer server, Object owner, String key, BooleanSupplier step) {
        return schedule(server, owner, key, 0, step);
    }

    public static boolean schedule(MinecraftServer server, Object owner, String key, long delay, BooleanSupplier step) {
        return ServerRuntime.queue(server).submit(owner, key,
                TickMath.add(HeroClockAPI.now(server), Math.max(0, delay)), step);
    }

    public static boolean cancel(MinecraftServer server, Object owner, String key) {
        return ServerRuntime.queue(server).cancel(owner, key);
    }

    public static void cancelOwner(MinecraftServer server, Object owner) {
        ServerRuntime.queue(server).cancelOwner(owner);
    }
}
