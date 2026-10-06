package com.heroclock.mixin;

import com.heroclock.scripting.NativeEventContainer;
import com.heroclock.scripting.NativeEventDispatch;
import dev.latvian.mods.kubejs.event.EventHandlerContainer;
import dev.latvian.mods.kubejs.event.IEventHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.event.EventHandlerContainer", remap = false)
abstract class KubeNativeDispatchMixin implements NativeEventContainer {
    @Unique private IEventHandler heroclock$dispatch;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1, allow = 1)
    private void heroclock$prepareDispatch(Object extra, IEventHandler handler, String source, int line, CallbackInfo ci) {
        heroclock$dispatch = NativeEventDispatch.prepare(handler, source, line);
    }

    @Override public IEventHandler heroclock$dispatchHandler() { return heroclock$dispatch; }

    @Redirect(method = "handle", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Ldev/latvian/mods/kubejs/event/EventHandlerContainer;handler:Ldev/latvian/mods/kubejs/event/IEventHandler;"), require = 1, allow = 1)
    private IEventHandler heroclock$dispatch(EventHandlerContainer container) {
        return ((NativeEventContainer) container).heroclock$dispatchHandler();
    }
}
