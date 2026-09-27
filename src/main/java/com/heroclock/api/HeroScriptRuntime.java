package com.heroclock.api;

import java.util.Iterator;

public interface HeroScriptRuntime extends AutoCloseable {
    @FunctionalInterface interface Callback { void call(Object[] arguments); }
    record Status(String namespace, boolean active, int listeners, int pending, long calls, long failures) {}

    boolean on(String event, String key, Callback callback);
    boolean off(String event, String key);
    int emit(String event, Object... arguments);
    boolean schedule(String key, long delayTicks, Callback callback, Object... arguments);
    boolean batch(String key, Iterator<?> items, int itemsPerTick, Callback callback);
    boolean cancel(String key);
    Status status();
    @Override void close();
}
