package com.heroclock.mixin;

import com.heroclock.runtime.ScriptTelemetry;
import dev.latvian.mods.kubejs.event.EventExceptionHandler;
import dev.latvian.mods.kubejs.event.EventExit;
import dev.latvian.mods.kubejs.event.EventHandlerContainer;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.event.EventResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.event.EventHandler", remap = false)
abstract class KubeBoundaryMixin {
    @Redirect(method = "postToHandlers", at = @At(value = "INVOKE", target = "Ldev/latvian/mods/kubejs/event/EventHandlerContainer;handle(Ldev/latvian/mods/kubejs/event/EventJS;Ldev/latvian/mods/kubejs/event/EventExceptionHandler;)Ldev/latvian/mods/kubejs/event/EventResult;"), require = 1)
    private EventResult heroclock$measureHandlers(EventHandlerContainer handler, EventJS event, EventExceptionHandler errors) throws EventExit {
        boolean measured = ScriptTelemetry.enter(ScriptTelemetry.KUBEJS);
        try { return handler.handle(event, errors); }
        finally { if (measured) ScriptTelemetry.exit(ScriptTelemetry.KUBEJS); }
    }
}
