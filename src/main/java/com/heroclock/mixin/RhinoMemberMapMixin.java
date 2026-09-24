package com.heroclock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "dev.latvian.mods.rhino.JavaMembers", remap = false)
abstract class RhinoMemberMapMixin {
    @ModifyArg(method = "getFieldAndMethodsObjects", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;<init>(I)V"), index = 0, require = 1, allow = 1)
    private int heroclock$sizeForMembers(int entries) {
        if (entries == 0) return 0;
        return entries < 1 << 29 ? entries + (entries + 2) / 3 : Integer.MAX_VALUE;
    }
}
