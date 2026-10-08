package com.heroclock.runtime;

import com.heroclock.api.HeroWorkAPI;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

public final class ScriptTasks {
    private ScriptTasks() {}

    public static boolean batch(MinecraftServer server, String namespace, String key, Iterator<?> items,
                                int itemsPerTick, Consumer<Object> action) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(action, "action");
        if (itemsPerTick < 1 || itemsPerTick > 256) throw new IllegalArgumentException("Use 1 to 256 items per tick");
        return HeroWorkAPI.submit(server, owner(namespace), key, () -> {
            for (int i = 0; i < itemsPerTick && items.hasNext(); i++) action.accept(items.next());
            return !items.hasNext();
        });
    }

    public static void cancelNamespace(MinecraftServer server, String namespace) {
        HeroWorkAPI.cancelOwner(server, owner(namespace));
    }

    private static String owner(String namespace) {
        if (namespace == null || namespace.length() > 96 || namespace.indexOf(':') >= 0
                || ResourceLocation.tryParse(namespace + ":work") == null) {
            throw new IllegalArgumentException("Use a resource namespace up to 96 characters");
        }
        return "heroclock:script/" + namespace;
    }
}
