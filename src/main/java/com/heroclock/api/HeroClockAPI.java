package com.heroclock.api;

import com.heroclock.runtime.TickMath;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class HeroClockAPI {
    public static final String ROOT = "HeroClockTimers";

    private HeroClockAPI() {}

    public static long now(Object target) {
        Level level;
        if (target instanceof Entity entity) level = entity.level();
        else if (target instanceof BlockEntity block) level = block.getLevel();
        else if (target instanceof Level world) level = world;
        else if (target instanceof MinecraftServer server) return server.overworld().getGameTime();
        else throw new IllegalArgumentException("Expected an entity, block entity, level or server");
        if (level == null) throw new IllegalStateException("Target has no level");
        MinecraftServer server = level.getServer();
        return server == null ? level.getGameTime() : server.overworld().getGameTime();
    }

    public static long set(Object target, String key, long ticks) {
        return setDeadline(target, key, TickMath.add(now(target), Math.max(0, ticks)));
    }

    public static long setSeconds(Object target, String key, double seconds) {
        if (!Double.isFinite(seconds)) throw new IllegalArgumentException("Seconds must be finite");
        return set(target, key, Math.round(Math.max(0, seconds) * 20));
    }

    public static long setDeadline(Object target, String key, long deadline) {
        String safeKey = TickMath.key(key);
        CompoundTag data = data(target, true);
        CompoundTag root = data.getCompound(ROOT);
        long value = Math.max(0, deadline);
        if (!data.contains(ROOT, Tag.TAG_COMPOUND)) data.put(ROOT, root);
        if (!root.contains(safeKey, Tag.TAG_LONG) || root.getLong(safeKey) != value) {
            root.putLong(safeKey, value);
            changed(target);
        }
        return value;
    }

    public static long add(Object target, String key, long ticks) {
        long now = now(target);
        long value = TickMath.add(Math.max(now, deadline(target, key)), ticks);
        if (value <= now) {
            clear(target, key);
            return 0;
        }
        return setDeadline(target, key, value);
    }

    public static long remaining(Object target, String key) {
        long deadline = deadline(target, key);
        return deadline <= 0 ? 0 : Math.max(0, deadline - now(target));
    }

    public static double remainingSeconds(Object target, String key) {
        return remaining(target, key) / 20.0;
    }

    public static boolean active(Object target, String key) { return remaining(target, key) > 0; }
    public static boolean expired(Object target, String key) { return !active(target, key); }

    public static long deadline(Object target, String key) {
        String safeKey = TickMath.key(key);
        CompoundTag data = data(target, false);
        return data.contains(ROOT, Tag.TAG_COMPOUND) ? data.getCompound(ROOT).getLong(safeKey) : 0;
    }

    public static void clear(Object target, String key) {
        String safeKey = TickMath.key(key);
        CompoundTag data = data(target, true);
        if (!data.contains(ROOT, Tag.TAG_COMPOUND)) return;
        CompoundTag root = data.getCompound(ROOT);
        if (!root.contains(safeKey)) return;
        root.remove(safeKey);
        if (root.isEmpty()) data.remove(ROOT);
        changed(target);
    }

    public static void clearAll(Object target) {
        CompoundTag data = data(target, true);
        if (data.contains(ROOT)) {
            data.remove(ROOT);
            changed(target);
        }
    }

    private static CompoundTag data(Object target, boolean writing) {
        Level level;
        CompoundTag data;
        if (target instanceof Entity entity) {
            level = entity.level();
            data = entity.getPersistentData();
        } else if (target instanceof BlockEntity block) {
            level = block.getLevel();
            data = block.getPersistentData();
        } else throw new IllegalArgumentException("Timers require an entity or block entity");
        if (level == null) throw new IllegalStateException("Target has no level");
        MinecraftServer server = level.getServer();
        if (server != null && !server.isSameThread()) throw new IllegalStateException("Use timers on the server thread");
        if (writing && level.isClientSide) throw new IllegalStateException("Timers are server authoritative");
        return data;
    }

    private static void changed(Object target) {
        if (target instanceof BlockEntity block) block.setChanged();
    }
}
