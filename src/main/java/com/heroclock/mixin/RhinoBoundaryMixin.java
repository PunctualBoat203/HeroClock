package com.heroclock.mixin;

import com.heroclock.runtime.ScriptTelemetry;
import dev.latvian.mods.rhino.Callable;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.latvian.mods.rhino.Context", remap = false)
abstract class RhinoBoundaryMixin {
    @Redirect(method = "callSync", at = @At(value = "INVOKE", target = "Ldev/latvian/mods/rhino/Callable;call(Ldev/latvian/mods/rhino/Context;Ldev/latvian/mods/rhino/Scriptable;Ldev/latvian/mods/rhino/Scriptable;[Ljava/lang/Object;)Ljava/lang/Object;"), require = 1)
    private Object heroclock$measureCall(Callable callable, Context context, Scriptable scope, Scriptable self, Object[] args) {
        boolean measured = ScriptTelemetry.enter(ScriptTelemetry.RHINO);
        try { return callable.call(context, scope, self, args); }
        finally { if (measured) ScriptTelemetry.exit(ScriptTelemetry.RHINO); }
    }
}
