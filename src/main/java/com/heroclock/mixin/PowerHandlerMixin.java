package com.heroclock.mixin;

import java.util.Collections;
import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.threetag.palladium.power.PowerHandler", remap = false)
abstract class PowerHandlerMixin {
    @Shadow @Final private Map<?, ?> powers;
    @Unique private Map<?, ?> heroclock$view;

    @Inject(method = "getPowerHolders()Ljava/util/Map;", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void heroclock$reuseView(CallbackInfoReturnable<Map<?, ?>> cir) {
        if (heroclock$view == null) heroclock$view = Collections.unmodifiableMap(powers);
        cir.setReturnValue(heroclock$view);
    }
}
