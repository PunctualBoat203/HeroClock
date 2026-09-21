package com.heroclock.mixin;

import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.threetag.palladium.util.property.PropertyManager", remap = false)
abstract class PropertyManagerMixin {
    @Unique private final Map<String, Object> heroclock$propertyByName = new HashMap<>();

    @Inject(
            method = "getPropertyByName(Ljava/lang/String;)Lnet/threetag/palladium/util/property/PalladiumProperty;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void heroclock$reusePropertyLookup(String name, CallbackInfoReturnable<Object> cir) {
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
        if (property != null) {
            heroclock$propertyByName.putIfAbsent(name, property);
        }
    }
}
