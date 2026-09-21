/*
 * Decompiled with CFR 0.152.
 */
package com.heroclock.api;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HeroClockAPI {
    private static final String ROOT = "HeroClockTimers";
    private static final Map<Class<?>, Access> ACCESS = new ConcurrentHashMap();

    private HeroClockAPI() {
    }

    public static long now(Object object) {
        if (object == null) {
            return 0L;
        }
        try {
            return HeroClockAPI.access(object).gameTime(object);
        }
        catch (Throwable throwable) {
            return 0L;
        }
    }

    public static long set(Object object, String string, long l) {
        long l2 = HeroClockAPI.now(object) + Math.max(0L, l);
        HeroClockAPI.putDeadline(object, string, l2);
        return l2;
    }

    public static long setSeconds(Object object, String string, double d) {
        return HeroClockAPI.set(object, string, Math.max(0L, Math.round(d * 20.0)));
    }

    public static long setDeadline(Object object, String string, long l) {
        HeroClockAPI.putDeadline(object, string, Math.max(0L, l));
        return Math.max(0L, l);
    }

    public static long add(Object object, String string, long l) {
        long l2 = Math.max(HeroClockAPI.now(object), HeroClockAPI.deadline(object, string));
        long l3 = l2 + l;
        if (l3 <= HeroClockAPI.now(object)) {
            HeroClockAPI.clear(object, string);
            return 0L;
        }
        HeroClockAPI.putDeadline(object, string, l3);
        return l3;
    }

    public static long remaining(Object object, String string) {
        long l = HeroClockAPI.deadline(object, string);
        if (l <= 0L) {
            return 0L;
        }
        return Math.max(0L, l - HeroClockAPI.now(object));
    }

    public static double remainingSeconds(Object object, String string) {
        return (double)HeroClockAPI.remaining(object, string) / 20.0;
    }

    public static boolean active(Object object, String string) {
        return HeroClockAPI.remaining(object, string) > 0L;
    }

    public static boolean expired(Object object, String string) {
        return !HeroClockAPI.active(object, string);
    }

    public static long deadline(Object object, String string) {
        if (!HeroClockAPI.valid(object, string)) {
            return 0L;
        }
        try {
            return HeroClockAPI.access(object).getLong(object, HeroClockAPI.safeKey(string));
        }
        catch (Throwable throwable) {
            return 0L;
        }
    }

    public static void clear(Object object, String string) {
        if (!HeroClockAPI.valid(object, string)) {
            return;
        }
        try {
            HeroClockAPI.access(object).remove(object, HeroClockAPI.safeKey(string));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public static void clearAll(Object object) {
        if (object == null) {
            return;
        }
        try {
            HeroClockAPI.access(object).clearAll(object);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static void putDeadline(Object object, String string, long l) {
        if (!HeroClockAPI.valid(object, string)) {
            return;
        }
        try {
            HeroClockAPI.access(object).putLong(object, HeroClockAPI.safeKey(string), l);
        }
        catch (Throwable throwable) {
            throw new IllegalStateException("HeroClock could not access player persistent data", throwable);
        }
    }

    private static boolean valid(Object object, String string) {
        return object != null && string != null && !string.isBlank();
    }

    private static String safeKey(String string) {
        String string2 = string.trim().replaceAll("[^A-Za-z0-9_.:-]", "_");
        return string2.length() > 96 ? string2.substring(0, 96) : string2;
    }

    private static Access access(Object object) {
        return ACCESS.computeIfAbsent(object.getClass(), Access::new);
    }

    private static final class Access {
        final Method getPersistentData;
        final Method level;
        final Method getGameTime;

        Access(Class<?> clazz) {
            try {
                this.getPersistentData = clazz.getMethod("getPersistentData", new Class[0]);
                this.level = Access.find(clazz, "level", "getLevel");
                this.getGameTime = this.level.getReturnType().getMethod("getGameTime", new Class[0]);
            }
            catch (Exception exception) {
                throw new IllegalStateException("Unsupported player object: " + clazz.getName(), exception);
            }
        }

        long gameTime(Object object) throws Exception {
            return ((Number)this.getGameTime.invoke(this.level.invoke(object, new Object[0]), new Object[0])).longValue();
        }

        Object root(Object object, boolean bl) throws Exception {
            Object object2 = this.getPersistentData.invoke(object, new Object[0]);
            Method method = object2.getClass().getMethod("contains", String.class, Integer.TYPE);
            boolean bl2 = (Boolean)method.invoke(object2, HeroClockAPI.ROOT, 10);
            if (!bl2 && !bl) {
                return null;
            }
            if (!bl2) {
                Class<?> clazz = Class.forName("net.minecraft.nbt.CompoundTag");
                Object obj = clazz.getConstructor(new Class[0]).newInstance(new Object[0]);
                object2.getClass().getMethod("put", String.class, Class.forName("net.minecraft.nbt.Tag")).invoke(object2, HeroClockAPI.ROOT, obj);
                return obj;
            }
            return object2.getClass().getMethod("getCompound", String.class).invoke(object2, HeroClockAPI.ROOT);
        }

        long getLong(Object object, String string) throws Exception {
            Object object2 = this.root(object, false);
            return object2 == null ? 0L : ((Number)object2.getClass().getMethod("getLong", String.class).invoke(object2, string)).longValue();
        }

        void putLong(Object object, String string, long l) throws Exception {
            Object object2 = this.root(object, true);
            object2.getClass().getMethod("putLong", String.class, Long.TYPE).invoke(object2, string, l);
        }

        void remove(Object object, String string) throws Exception {
            Object object2 = this.root(object, false);
            if (object2 != null) {
                object2.getClass().getMethod("remove", String.class).invoke(object2, string);
            }
        }

        void clearAll(Object object) throws Exception {
            Object object2 = this.getPersistentData.invoke(object, new Object[0]);
            object2.getClass().getMethod("remove", String.class).invoke(object2, HeroClockAPI.ROOT);
        }

        static Method find(Class<?> clazz, String ... stringArray) throws NoSuchMethodException {
            for (String string : stringArray) {
                try {
                    return clazz.getMethod(string, new Class[0]);
                }
                catch (NoSuchMethodException noSuchMethodException) {
                }
            }
            throw new NoSuchMethodException();
        }
    }
}

