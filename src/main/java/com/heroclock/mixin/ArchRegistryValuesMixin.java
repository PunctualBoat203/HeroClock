package com.heroclock.mixin;

import java.util.ArrayList;
import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.threetag.palladiumcore.compat.architectury.ArchRegistryWrapper", remap = false)
abstract class ArchRegistryValuesMixin {
    @Unique private volatile int heroclock$previousValueCount;

    @Redirect(method = "getValues", at = @At(value = "NEW", target = "java/util/ArrayList"), require = 1, allow = 1)
    private ArrayList<Object> heroclock$sizeFreshValues() {
        int capacity = heroclock$previousValueCount;
        return capacity == 0 ? new ArrayList<>() : new ArrayList<>(capacity);
    }

    @Inject(method = "getValues", at = @At("RETURN"), require = 1)
    private void heroclock$rememberValueCount(CallbackInfoReturnable<Collection<?>> result) {
        heroclock$previousValueCount = Math.min(result.getReturnValue().size(), 65_536);
    }
}
