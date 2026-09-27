package com.heroclock.mixin;

import com.heroclock.runtime.ScalarChanges;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.threetag.palladium.util.property.EntityPropertyHandler", remap = false)
abstract class EntityPropertyHandlerMixin {
    @Inject(method = "onChanged(Lnet/threetag/palladium/util/property/PalladiumProperty;Ljava/lang/Object;Ljava/lang/Object;)V",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void heroclock$skipUnchangedScalar(@Coerce Object property, Object before, Object after, CallbackInfo ci) {
        if (ScalarChanges.unchanged(before, after)) ci.cancel();
    }
}
