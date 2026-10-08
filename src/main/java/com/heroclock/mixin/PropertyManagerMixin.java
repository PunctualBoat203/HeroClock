package com.heroclock.mixin;

import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.threetag.palladium.util.property.PropertyManager", remap = false)
abstract class PropertyManagerMixin {
    @Unique private final Map<String, Object> heroclock$propertyByName = new HashMap<>();

    @Unique private boolean heroclock$externallyMutable;

    @Inject(
            method = "getPropertyByName(Ljava/lang/String;)Lnet/threetag/palladium/util/property/PalladiumProperty;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void heroclock$reusePropertyLookup(String name, CallbackInfoReturnable<Object> cir) {
        if (heroclock$externallyMutable) return;
        Object cached = heroclock$propertyByName.get(name);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(
            method = "getPropertyByName(Ljava/lang/String;)Lnet/threetag/palladium/util/property/PalladiumProperty;",
            at = @At("RETURN"),
            require = 1,
            remap = false)
    private void heroclock$rememberPropertyLookup(String name, CallbackInfoReturnable<Object> cir) {
        Object property = cir.getReturnValue();
        if (!heroclock$externallyMutable && property != null) {
            heroclock$propertyByName.putIfAbsent(name, property);
        }
    }

    @Inject(method = "register", at = @At("HEAD"), remap = false)
    private void heroclock$registrationChanged(CallbackInfoReturnable<Object> cir) {
        heroclock$propertyByName.clear();
    }

    @Inject(method = {"fromNBT", "fromBuffer", "fromJSON"}, at = @At("HEAD"), remap = false)
    private void heroclock$contentsReloaded(CallbackInfo ci) {
        heroclock$propertyByName.clear();
    }

    @Inject(method = "values", at = @At("HEAD"), remap = false)
    private void heroclock$mutableMapExposed(CallbackInfoReturnable<Object> cir) {
        heroclock$externallyMutable = true;
        heroclock$propertyByName.clear();
    }
}
