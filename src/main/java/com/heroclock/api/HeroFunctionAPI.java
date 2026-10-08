package com.heroclock.api;

import com.heroclock.runtime.FunctionJobs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;

public final class HeroFunctionAPI {
    private HeroFunctionAPI() {}

    public static boolean schedule(CommandSourceStack source, String key, long delay, ResourceLocation function) {
        return FunctionJobs.schedule(source, key, delay, function);
    }

    public static boolean cancel(CommandSourceStack source, String key) {
        return FunctionJobs.cancel(source, key);
    }
}
