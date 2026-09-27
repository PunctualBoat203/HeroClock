package com.heroclock.runtime;

import com.heroclock.api.HeroScriptRuntime.Callback;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public final class ScriptListeners {
    private record Listener(String key, Callback callback) {}
    private static final class Channel {
        final Map<String, Listener> keyed = new LinkedHashMap<>();
        List<Listener> snapshot = List.of();
        void changed() { snapshot = List.copyOf(keyed.values()); }
    }
    private final Map<String, Channel> channels = new HashMap<>();
    private int size;
    private int depth;

    public boolean on(String event, String key, Callback callback) {
        name(event); name(key); Objects.requireNonNull(callback, "callback");
        Channel channel = channels.get(event);
        if (size == 1024 && (channel == null || !channel.keyed.containsKey(key))) return false;
        if (channel == null) { channel = new Channel(); channels.put(event, channel); }
        if (channel.keyed.put(key, new Listener(key, callback)) == null) size++;
        channel.changed();
        return true;
    }

    public boolean off(String event, String key) {
        name(event); name(key);
        Channel channel = channels.get(event);
        if (channel == null || channel.keyed.remove(key) == null) return false;
        size--;
        channel.changed();
        if (channel.keyed.isEmpty()) channels.remove(event);
        return true;
    }

    public int emit(String event, Object[] arguments, BooleanSupplier active, BiConsumer<Callback, Object[]> invoke) {
        name(event); Objects.requireNonNull(arguments, "arguments");
        Channel channel = channels.get(event);
        if (channel == null) return 0;
        if (depth == 32) throw new IllegalStateException("HeroClock event nesting exceeds 32 calls");
        depth++;
        int calls = 0;
        try {
            for (Listener listener : channel.snapshot) {
                if (!active.getAsBoolean()) break;
                if (channel.keyed.get(listener.key) != listener) continue;
                invoke.accept(listener.callback, arguments.clone());
                calls++;
            }
            return calls;
        } finally { depth--; }
    }

    public int size() { return size; }
    public void clear() {
        for (Channel channel : channels.values()) channel.keyed.clear();
        channels.clear(); size = 0;
    }

    public static void name(String name) {
        if (name == null || name.isEmpty() || name.length() > 96) throw new IllegalArgumentException("Use a name of 1 to 96 characters");
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || "_.:/-".indexOf(c) >= 0))
                throw new IllegalArgumentException("Use lowercase resource-style names");
        }
    }
}
