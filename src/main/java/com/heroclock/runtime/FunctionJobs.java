package com.heroclock.runtime;

import com.heroclock.api.HeroWorkAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;

public final class FunctionJobs {
    private FunctionJobs() {}

    public static boolean schedule(CommandSourceStack source, String key, long delay, ResourceLocation function) {
        if (!source.getServer().isSameThread()) throw new IllegalStateException("Use functions on the server thread");
        if (source.getServer().getFunctions().get(function).isEmpty()) return false;
        String task = taskKey(key);
        return HeroWorkAPI.schedule(source.getServer(), owner(source), task, delay, () -> {
            if (source.getEntity() != null && source.getEntity().isRemoved()) return true;
            source.getServer().getFunctions().get(function).ifPresent(current ->
                    source.getServer().getFunctions().execute(current, source));
            return true;
        });
    }

    public static boolean cancel(CommandSourceStack source, String key) {
        return HeroWorkAPI.cancel(source.getServer(), owner(source), taskKey(key));
    }

    private static Object owner(CommandSourceStack source) {
        return source.getEntity() == null ? source.getLevel().dimension() : source.getEntity().getUUID();
    }

    private static String taskKey(String key) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null || key.length() > 96) throw new IllegalArgumentException("Function work keys must be resource locations up to 96 characters");
        return "heroclock:function/" + id;
    }
}
