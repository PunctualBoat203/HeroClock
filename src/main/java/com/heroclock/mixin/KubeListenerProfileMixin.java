package com.heroclock.mixin;

import com.heroclock.runtime.ScriptHotspots;
import dev.latvian.mods.kubejs.event.EventHandlerContainer;
import dev.latvian.mods.kubejs.event.IEventHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.event.EventHandlerContainer", remap = false)
abstract class KubeListenerProfileMixin {
    @Redirect(method = "handle", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Ldev/latvian/mods/kubejs/event/EventHandlerContainer;handler:Ldev/latvian/mods/kubejs/event/IEventHandler;"), require = 1, allow = 1)
    private IEventHandler heroclock$attributeListener(EventHandlerContainer container) {
        IEventHandler handler = container.handler;
        if (!ScriptHotspots.enabled()) return handler;
        return event -> {
            long start = System.nanoTime();
            try { return handler.onEvent(event); }
            finally {
                ScriptHotspots.record("kubejs_listener", container.source,
                        container.line + ":" + (event == null ? "<null>" : event.getClass().getName()), 1, System.nanoTime() - start);
            }
        };
    }
}
