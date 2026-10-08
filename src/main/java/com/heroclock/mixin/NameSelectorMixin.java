package com.heroclock.mixin;

import com.heroclock.runtime.NameMatcher;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntitySelectorOptions.class)
abstract class NameSelectorMixin {
    @Inject(method = "lambda$bootStrap$5(Ljava/lang/String;ZLnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void heroclock$matchName(String name, boolean inverted, Entity entity,
                                          CallbackInfoReturnable<Boolean> result) {
        result.setReturnValue(NameMatcher.matches(entity, name) != inverted);
    }
}
