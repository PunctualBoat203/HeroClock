package com.heroclock.runtime;

import com.heroclock.HeroClock;
import com.heroclock.api.HeroClockAPI;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = HeroClock.MOD_ID)
public final class ServerRuntime {
    private static MinecraftServer current;
    private static WorkQueue work;

    private ServerRuntime() {}

    public static WorkQueue queue(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Use HeroWork on the server thread");
        if (current != server) {
            current = server;
            work = new WorkQueue(4096, failure -> HeroClock.LOGGER.error("HeroClock task failed", failure));
        }
        return work;
    }

    public static void cancelOwnerIfPresent(MinecraftServer server, Object owner) {
        if (current == server && work != null) work.cancelOwner(owner);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && work != null && current == server && work.size() != 0) {
            work.drain(HeroClockAPI.now(server), 128, 1_000_000);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        com.heroclock.SatsuAdapter.clear();
        if (current == event.getServer()) {
            work.clear();
            work = null;
            current = null;
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        var original = event.getOriginal().getPersistentData();
        if (original.contains(HeroClockAPI.ROOT, Tag.TAG_COMPOUND)) {
            event.getEntity().getPersistentData().put(HeroClockAPI.ROOT, original.getCompound(HeroClockAPI.ROOT).copy());
        }
    }
}
