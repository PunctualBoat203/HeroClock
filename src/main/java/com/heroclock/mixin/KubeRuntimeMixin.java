package com.heroclock.mixin;

import com.heroclock.HeroClock;
import com.heroclock.scripting.RhinoRuntimeScope;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.script.ScriptManager", remap = false)
abstract class KubeRuntimeMixin {
    @Shadow @Final public ScriptType scriptType;
    @Shadow public Context context;
    @Shadow public Scriptable topLevelScope;
    @Unique private RhinoRuntimeScope heroclock$runtimeScope;

    @Inject(method = {"load", "unload"}, at = @At("HEAD"), require = 2, allow = 2)
    private void heroclock$invalidateScope(CallbackInfo ci) {
        if (heroclock$runtimeScope != null) {
            heroclock$runtimeScope.invalidate();
            heroclock$runtimeScope = null;
        }
    }

    @Inject(method = "load", at = @At(value = "INVOKE",
            target = "Ldev/latvian/mods/kubejs/util/KubeJSPlugins;addSidedBindings(Ldev/latvian/mods/kubejs/script/BindingsEvent;)V",
            shift = At.Shift.AFTER), require = 1, allow = 1)
    private void heroclock$bindRuntime(CallbackInfo ci) {
        if (scriptType != ScriptType.SERVER) return;
        if (ScriptableObject.hasProperty(topLevelScope, "HeroRuntime", context)) {
            HeroClock.LOGGER.warn("HeroClock retains existing HeroRuntime binding; owned scripting runtime was not installed");
            return;
        }
        heroclock$runtimeScope = new RhinoRuntimeScope(context, topLevelScope);
        context.addToScope(topLevelScope, "HeroRuntime", heroclock$runtimeScope);
    }
}
