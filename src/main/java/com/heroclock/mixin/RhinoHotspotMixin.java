package com.heroclock.mixin;

import com.heroclock.runtime.ScriptHotspots;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.latvian.mods.rhino.NativeJavaObject", remap = false)
abstract class RhinoHotspotMixin {
    @Shadow protected Object javaObject;
    @Shadow protected Class<?> staticType;
    @Shadow protected Map<?, ?> fieldAndMethods;

    @Unique private String heroclock$receiverType() {
        Class<?> type = javaObject == null ? staticType : javaObject.getClass();
        return type == null ? "<null>" : type.getName();
    }

    @Inject(method = "initMembers", at = @At("RETURN"), require = 1)
    private void heroclock$countWrappers(Context context, Scriptable scope, CallbackInfo ci) {
        if (ScriptHotspots.enabled()) ScriptHotspots.record("wrapper_init", heroclock$receiverType(), "",
                fieldAndMethods == null ? 0 : fieldAndMethods.size(), 0);
    }

    @Inject(method = "get(Ldev/latvian/mods/rhino/Context;Ljava/lang/String;Ldev/latvian/mods/rhino/Scriptable;)Ljava/lang/Object;",
            at = @At("HEAD"), require = 1)
    private void heroclock$countMemberReads(Context context, String name, Scriptable start, CallbackInfoReturnable<Object> cir) {
        if (ScriptHotspots.enabled()) ScriptHotspots.record("member_read", heroclock$receiverType(), name, 1, 0);
    }
}
