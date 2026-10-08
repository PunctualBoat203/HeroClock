package com.heroclock.mixin;

import java.util.Collections;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "top.theillusivec4.curios.common.capability.CurioInventoryCapability$CurioInventoryWrapper",
        remap = false)
abstract class CuriosInventoryMixin {
    @Shadow private Map<?, ?> curios;
    @Unique private Map<?, ?> heroclock$curiosView;
    @Unique private Map<?, ?> heroclock$backing;

    @Inject(method = "getCurios()Ljava/util/Map;", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void heroclock$reuseCuriosView(CallbackInfoReturnable<Map<?, ?>> cir) {
        if (heroclock$curiosView == null || heroclock$backing != curios) {
            heroclock$backing = curios;
            heroclock$curiosView = Collections.unmodifiableMap(curios);
        }
        cir.setReturnValue(heroclock$curiosView);
    }

}
