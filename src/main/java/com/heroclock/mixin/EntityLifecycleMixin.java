package com.heroclock.mixin;

import com.heroclock.runtime.TemporaryEntities;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class EntityLifecycleMixin {
    @Unique private long heroclock$checkAt = Long.MIN_VALUE;

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void heroclock$checkTemporaryEntity(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!entity.level().isClientSide && entity.level().getGameTime() >= heroclock$checkAt) {
            heroclock$checkAt = TemporaryEntities.check(entity);
        }
    }

    @Inject(method = {"addTag", "removeTag"}, at = @At("RETURN"))
    private void heroclock$tagsChanged(String tag, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && TemporaryEntities.handles(tag)) heroclock$checkAt = Long.MIN_VALUE;
    }
}
