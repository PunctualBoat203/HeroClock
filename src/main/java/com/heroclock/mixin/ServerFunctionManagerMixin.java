package com.heroclock.mixin;

import com.heroclock.SatsuAdapter;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.ServerFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerFunctionManager.class)
abstract class ServerFunctionManagerMixin {
    @Inject(
            method = "execute(Lnet/minecraft/commands/CommandFunction;Lnet/minecraft/commands/CommandSourceStack;)I",
            at = @At("HEAD"),
            cancellable = true)
    private void heroclock$redirectSatsuSentinelTick(
            CommandFunction function,
            CommandSourceStack source,
            CallbackInfoReturnable<Integer> cir) {
        if (SatsuAdapter.suppressStockTick(function, source.getServer())) {
            cir.setReturnValue(0);
        }
    }
}
