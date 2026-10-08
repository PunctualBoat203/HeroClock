package com.heroclock.mixin;

import com.heroclock.resources.OmniPowerResources;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.resource.ResourcePackLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ResourcePackLoader.class, remap = false)
abstract class ModResourcesMixin {
    @Inject(method = "createPackForMod(Lnet/minecraftforge/forgespi/language/IModFileInfo;)Lnet/minecraftforge/resource/PathPackResources;",
            at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void heroclock$resolveCompanionPowers(IModFileInfo modFile,
                                                       CallbackInfoReturnable<PathPackResources> result) {
        PathPackResources original = result.getReturnValue();
        if (original == null) return;
        PathPackResources resolved = OmniPowerResources.resolve(modFile, original);
        if (resolved != original) result.setReturnValue(resolved);
    }
}
