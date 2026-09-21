package com.heroclock.runtime;

import com.heroclock.HeroClock;
import com.heroclock.api.HeroClockAPI;
import com.heroclock.api.HeroFunctionAPI;
import com.heroclock.api.HeroIntegrationAPI;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod.EventBusSubscriber(modid = HeroClock.MOD_ID)
public final class HeroCommands {
    private static final SimpleCommandExceptionType REJECTED = new SimpleCommandExceptionType(Component.literal("Function is missing or HeroClock's work queue is full"));
    private static final SimpleCommandExceptionType KEY = new SimpleCommandExceptionType(Component.literal("Use a resource-location key of at most 96 characters"));
    private HeroCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("heroclock").requires(source -> source.hasPermission(2))
                .then(literal("timer")
                    .then(literal("set").then(argument("key", StringArgumentType.word())
                        .then(argument("ticks", LongArgumentType.longArg(0)).executes(context -> {
                            var entity = context.getSource().getEntityOrException();
                            HeroClockAPI.set(entity, key(context), LongArgumentType.getLong(context, "ticks"));
                            return result(HeroClockAPI.remaining(entity, key(context)));
                        }))))
                    .then(literal("add").then(argument("key", StringArgumentType.word())
                        .then(argument("ticks", LongArgumentType.longArg()).executes(context -> {
                            var entity = context.getSource().getEntityOrException();
                            HeroClockAPI.add(entity, key(context), LongArgumentType.getLong(context, "ticks"));
                            return result(HeroClockAPI.remaining(entity, key(context)));
                        }))))
                    .then(literal("remaining").then(argument("key", StringArgumentType.word())
                        .executes(context -> result(HeroClockAPI.remaining(context.getSource().getEntityOrException(), key(context))))))
                    .then(literal("clear").then(argument("key", StringArgumentType.word()).executes(context -> {
                        HeroClockAPI.clear(context.getSource().getEntityOrException(), key(context));
                        return 1;
                    }))))
                .then(literal("work")
                    .then(literal("schedule").then(argument("key", StringArgumentType.word())
                        .then(argument("ticks", LongArgumentType.longArg(0))
                        .then(argument("function", ResourceLocationArgument.id()).executes(context -> {
                            boolean accepted = HeroFunctionAPI.schedule(context.getSource(), key(context),
                                    LongArgumentType.getLong(context, "ticks"), ResourceLocationArgument.getId(context, "function"));
                            if (!accepted) throw REJECTED.create();
                            return 1;
                        })))))
                    .then(literal("cancel").then(argument("key", StringArgumentType.word())
                        .executes(context -> HeroFunctionAPI.cancel(context.getSource(), key(context)) ? 1 : 0))))
                .then(literal("status").executes(context -> {
                    var source = context.getSource();
                    var status = HeroIntegrationAPI.workStatus(source.getServer());
                    source.sendSuccess(() -> Component.literal("HeroClock " + HeroIntegrationAPI.version() + " | API "
                            + HeroIntegrationAPI.apiVersion() + " | pending " + status.pending() + " | rejected " + status.rejected()), false);
                    HeroIntegrationAPI.compatibility().forEach((patch, decision) ->
                            source.sendSuccess(() -> Component.literal(patch + ": " + decision), false));
                    return 1;
                })));
    }

    private static String key(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "key");
        if (key.length() > 96 || ResourceLocation.tryParse(key) == null || !TickMath.key(key).equals(key)) throw KEY.create();
        return key;
    }

    private static int result(long value) { return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value)); }
}
