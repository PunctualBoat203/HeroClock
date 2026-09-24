package com.heroclock.api;

import com.heroclock.runtime.Timers;

public final class HeroClockAPI {
    public static final String ROOT = "HeroClockTimers";
    private HeroClockAPI() {}

    public static long now(Object target) { return Timers.now(target); }
    public static long set(Object target, String key, long ticks) { return Timers.set(target, key, ticks); }
    public static long setSeconds(Object target, String key, double seconds) { return Timers.setSeconds(target, key, seconds); }
    public static long setDeadline(Object target, String key, long deadline) { return Timers.setDeadline(target, key, deadline); }
    public static long add(Object target, String key, long ticks) { return Timers.add(target, key, ticks); }
    public static long remaining(Object target, String key) { return Timers.remaining(target, key); }
    public static double remainingSeconds(Object target, String key) { return Timers.remainingSeconds(target, key); }
    public static boolean active(Object target, String key) { return Timers.active(target, key); }
    public static boolean expired(Object target, String key) { return Timers.expired(target, key); }
    public static long deadline(Object target, String key) { return Timers.deadline(target, key); }
    public static void clear(Object target, String key) { Timers.clear(target, key); }
    public static void clearAll(Object target) { Timers.clearAll(target); }
}
