package com.heroclock.mixin;

import dev.latvian.mods.kubejs.event.EventHandlerContainer;
import dev.latvian.mods.kubejs.event.IEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.event.EventHandlerContainer", remap = false)
abstract class KubeEventContainerMixin {
    @Unique private EventHandlerContainer heroclock$tail;

    @ModifyVariable(method = "add", at = @At(value = "STORE", ordinal = 0), index = 5, require = 1)
    private EventHandlerContainer heroclock$resumeAppend(EventHandlerContainer root) {
        return heroclock$tail == null ? root : heroclock$tail;
    }

    @Redirect(method = "add", at = @At(value = "NEW", target = "dev/latvian/mods/kubejs/event/EventHandlerContainer"), require = 1)
    private EventHandlerContainer heroclock$rememberTail(Object extraId, IEventHandler handler, String source, int line) {
        return heroclock$tail = new EventHandlerContainer(extraId, handler, source, line);
    }
}
