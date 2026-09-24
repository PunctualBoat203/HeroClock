package com.heroclock.mixin;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ScriptRuntime;
import java.util.Arrays;
import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.latvian.mods.rhino.NativeJavaMap", remap = false)
abstract class RhinoMapIdsMixin {
    @Shadow @Final private Map<?, ?> map;

    @Inject(method = "getIds", at = @At("HEAD"), cancellable = true, require = 1)
    private void heroclock$directIds(Context context, CallbackInfoReturnable<Object[]> result) {
        Object[] ids = new Object[map.size()];
        int size = 0;
        for (Object key : map.keySet()) {
            Object id = key instanceof Integer ? key : ScriptRuntime.toString(context, key);
            if (size == ids.length) ids = Arrays.copyOf(ids, Math.addExact(size, Math.max(1, size >>> 1)));
            ids[size++] = id;
        }
        result.setReturnValue(size == ids.length ? ids : Arrays.copyOf(ids, size));
    }
}
