package com.heroclock.mixin;

import net.minecraft.commands.CommandFunction;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.threetag.palladium.util.property.CommandFunctionProperty$CommandFunctionParsing", remap = false)
abstract class CommandFunctionMixin {
    @Shadow private CommandFunction commandFunction;
    @Shadow private boolean error;
    @Unique private Object heroclock$dispatcher;

    @Inject(method = "getCommandFunction(Lnet/minecraft/server/MinecraftServer;)Lnet/minecraft/commands/CommandFunction;",
            at = @At("HEAD"), require = 1, remap = false)
    private void heroclock$invalidateAfterReload(MinecraftServer server, CallbackInfoReturnable<CommandFunction> cir) {
        Object dispatcher = server.getCommands().getDispatcher();
        if (heroclock$dispatcher != dispatcher) {
            heroclock$dispatcher = dispatcher;
            commandFunction = null;
            error = false;
        }
    }
}
