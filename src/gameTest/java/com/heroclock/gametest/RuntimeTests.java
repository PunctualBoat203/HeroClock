package com.heroclock.gametest;

import com.heroclock.api.HeroClockAPI;
import com.heroclock.api.HeroWorkAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("heroclock")
@PrefixGameTestTemplate(false)
public final class RuntimeTests {
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void savedTimerExpires(GameTestHelper helper) {
        var entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 2));
        entity.setNoGravity(true);
        long deadline = HeroClockAPI.set(entity, "test.cooldown", 20);
        CompoundTag saved = entity.saveWithoutId(new CompoundTag());
        var restored = EntityType.ARMOR_STAND.create(helper.getLevel());
        restored.load(saved);
        helper.assertTrue(HeroClockAPI.deadline(restored, "test.cooldown") == deadline, "Saved deadline was lost");
        helper.runAfterDelay(5, () -> helper.assertTrue(HeroClockAPI.active(restored, "test.cooldown"), "Timer expired early"));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(HeroClockAPI.expired(restored, "test.cooldown"), "Timer did not expire");
            entity.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void lateTagTriggersCleanup(GameTestHelper helper) {
        var entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 2));
        entity.setNoGravity(true);
        helper.runAfterDelay(5, () -> {
            entity.addTag("IceSpikes");
            HeroClockAPI.set(entity, "cleanup.IceSpikes", 3);
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(entity.isRemoved(), "Late helper tag did not trigger cleanup");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void workRunsAcrossTicks(GameTestHelper helper) {
        AtomicInteger steps = new AtomicInteger();
        var server = helper.getLevel().getServer();
        helper.assertTrue(HeroWorkAPI.submit(server, steps, "test", () -> steps.incrementAndGet() == 3), "Work was rejected");
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(steps.get() == 3, "Work did not complete exactly three steps");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void webCleanupOnlyChangesCobwebs(GameTestHelper helper) {
        var entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 1, 2));
        entity.setNoGravity(true);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.COBWEB);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        entity.addTag("web_timer");
        HeroClockAPI.set(entity, "cleanup.web_timer", 3);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(entity.isRemoved(), "Web marker did not clean up");
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 1));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(3, 1, 3));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void timerEdgeCases(GameTestHelper helper) {
        var entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 2));
        entity.setNoGravity(true);
        helper.assertTrue(!entity.getPersistentData().contains(HeroClockAPI.ROOT), "Unexpected timer root");
        helper.assertTrue(HeroClockAPI.remaining(entity, "missing") == 0, "Missing timer is active");
        helper.assertTrue(!entity.getPersistentData().contains(HeroClockAPI.ROOT), "Read created timer data");
        HeroClockAPI.set(entity, "edge", Long.MAX_VALUE);
        helper.assertTrue(HeroClockAPI.deadline(entity, "edge") == Long.MAX_VALUE, "Deadline overflowed");
        HeroClockAPI.add(entity, "edge", Long.MIN_VALUE);
        helper.assertTrue(HeroClockAPI.expired(entity, "edge"), "Negative adjustment failed");
        boolean invalidRejected = false;
        try { HeroClockAPI.setSeconds(entity, "edge", Double.NaN); }
        catch (IllegalArgumentException expected) { invalidRejected = true; }
        helper.assertTrue(invalidRejected, "Non-finite duration accepted");
        entity.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void palladiumMixinsApplyWhenPresent(GameTestHelper helper) throws Exception {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("palladium")) {
            helper.assertTrue(!Boolean.getBoolean("heroclock.testPalladium"), "Expected Palladium was not loaded");
            helper.succeed(); return;
        }
        var entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 2));
        entity.setNoGravity(true);
        Class<?> handlerType = Class.forName("net.threetag.palladium.power.PowerHandler");
        Object handler = handlerType.getConstructor(net.minecraft.world.entity.LivingEntity.class).newInstance(entity);
        var getter = handlerType.getMethod("getPowerHolders");
        Object first = getter.invoke(handler);
        if (Boolean.getBoolean("heroclock.testChangedTarget")) {
            helper.assertTrue(first != getter.invoke(handler), "Incompatible target was still patched");
            helper.assertTrue(com.heroclock.api.HeroIntegrationAPI.compatibility().get("PowerHandlerMixin")
                    .contains("field contract changed"), "Incompatible field change was not diagnosed");
        } else helper.assertTrue(first == getter.invoke(handler), "Power holder view was not reused");
        Class<?> properties = Class.forName("net.threetag.palladium.util.property.EntityPropertyHandler");
        helper.assertTrue(java.util.Arrays.stream(properties.getDeclaredMethods()).anyMatch(method ->
                method.getName().contains("skipUnchangedScalar")), "Property sync mixin missing");

        Class<?> managerType = Class.forName("net.threetag.palladium.util.property.PropertyManager");
        Class<?> propertyType = Class.forName("net.threetag.palladium.util.property.PalladiumProperty");
        Class<?> integerPropertyType = Class.forName("net.threetag.palladium.util.property.IntegerProperty");
        Object manager = managerType.getConstructor().newInstance();
        Object cachedProperty = integerPropertyType.getConstructor(String.class).newInstance("heroclock_cached");
        var register = managerType.getMethod("register", propertyType, Object.class);
        var lookup = managerType.getMethod("getPropertyByName", String.class);
        register.invoke(manager, cachedProperty, Integer.valueOf(1));
        helper.assertTrue(lookup.invoke(manager, "heroclock_cached") == cachedProperty, "Initial property lookup failed");
        helper.assertTrue(lookup.invoke(manager, "heroclock_cached") == cachedProperty, "Cached property lookup changed result");
        var cacheField = java.util.Arrays.stream(managerType.getDeclaredFields())
                .filter(field -> field.getName().contains("propertyByName"))
                .findFirst().orElseThrow();
        cacheField.setAccessible(true);
        Object cache = cacheField.get(manager);
        helper.assertTrue(cache instanceof java.util.Map<?, ?> map && map.get("heroclock_cached") == cachedProperty,
                "Property lookup cache did not retain the positive result");
        helper.assertTrue(lookup.invoke(manager, "heroclock_late") == null, "Unexpected property found before registration");
        Object lateProperty = integerPropertyType.getConstructor(String.class).newInstance("heroclock_late");
        register.invoke(manager, lateProperty, Integer.valueOf(2));
        helper.assertTrue(lookup.invoke(manager, "heroclock_late") == lateProperty,
                "Late property registration was hidden by lookup caching");

        var exposed = (java.util.Map<?, ?>) managerType.getMethod("values").invoke(manager);
        exposed.clear();
        helper.assertTrue(lookup.invoke(manager, "heroclock_cached") == null,
                "Exposed mutable map retained a stale cached property");

        Class<?> commands = Class.forName("net.threetag.palladium.util.property.CommandFunctionProperty$CommandFunctionParsing");
        Object parsing = commands.getConstructor(java.util.List.class).newInstance(java.util.List.of());
        helper.assertTrue(commands.getMethod("getCommandFunction", net.minecraft.server.MinecraftServer.class)
                .invoke(parsing, helper.getLevel().getServer()) != null, "Command parsing failed");
        helper.assertTrue(java.util.Arrays.stream(commands.getDeclaredFields()).anyMatch(field ->
                field.getName().contains("dispatcher")), "Command cache mixin missing");
        entity.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void datapackTimerCommands(GameTestHelper helper) {
        var entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 2));
        entity.setNoGravity(true);
        var source = entity.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        var commands = helper.getLevel().getServer().getCommands();
        int result = commands.performPrefixedCommand(source, "heroclock timer set example:cooldown 30");
        helper.assertTrue(result == 30, "Timer command did not return remaining ticks");
        helper.assertTrue(HeroClockAPI.remaining(entity, "example:cooldown") == 30, "Command did not set timer");
        commands.performPrefixedCommand(source, "heroclock timer add example:cooldown -10");
        helper.assertTrue(HeroClockAPI.remaining(entity, "example:cooldown") == 20, "Command did not adjust timer");
        commands.performPrefixedCommand(source, "heroclock timer clear example:cooldown");
        helper.assertTrue(HeroClockAPI.expired(entity, "example:cooldown"), "Command did not clear timer");
        int denied = commands.performPrefixedCommand(source.withPermission(0), "heroclock timer set example:denied 20");
        helper.assertTrue(denied == 0 && HeroClockAPI.deadline(entity, "example:denied") == 0, "Command permission was bypassed");
        entity.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void deferredFunctionsCoalesce(GameTestHelper helper) {
        var entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 2));
        entity.setNoGravity(true);
        var source = entity.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        var function = new net.minecraft.resources.ResourceLocation("heroclock", "test_marker");
        helper.assertTrue(com.heroclock.api.HeroFunctionAPI.schedule(source, "example:cancelled", 2, function), "Cancellation setup rejected");
        helper.assertTrue(com.heroclock.api.HeroFunctionAPI.cancel(source, "example:cancelled"), "Function cancellation failed");
        helper.assertTrue(com.heroclock.api.HeroFunctionAPI.schedule(source, "example:marker", 3, function), "Function rejected");
        helper.assertTrue(com.heroclock.api.HeroFunctionAPI.schedule(source, "example:marker", 8, function), "Replacement rejected");
        helper.runAfterDelay(5, () -> helper.assertTrue(!entity.getTags().contains("heroclock_test_marker"), "Superseded function ran"));
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(entity.getTags().contains("heroclock_test_marker"), "Deferred function lost its executor");
            entity.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void satsuFunctionChangesRetainStockBehavior(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var source = server.createCommandSourceStack().withPermission(2);
        var id = new net.minecraft.resources.ResourceLocation("satsu_iron_man_addon", "tick");
        var original = net.minecraft.commands.CommandFunction.fromLines(id, server.getCommands().getDispatcher(), source,
                java.util.List.of("kill @e[tag=sentinel_kill]"));
        var changed = net.minecraft.commands.CommandFunction.fromLines(id, server.getCommands().getDispatcher(), source,
                java.util.List.of("kill @e[tag=sentinel_kill]", "say additional gameplay"));
        helper.assertTrue(com.heroclock.SatsuAdapter.matches(original), "Matching function rejected");
        helper.assertTrue(!com.heroclock.SatsuAdapter.matches(changed), "Changed function was suppressed");
        helper.assertTrue(com.heroclock.SatsuAdapter.matches(original), "Function reload was not rechecked");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void curiosViewTracksReplacement(GameTestHelper helper) throws Exception {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            helper.assertTrue(!Boolean.getBoolean("heroclock.testCurios"), "Expected Curios was not loaded");
            helper.succeed(); return;
        }
        var entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 2));
        entity.setNoGravity(true);
        Class<?> type = Class.forName("top.theillusivec4.curios.common.capability.CurioInventoryCapability$CurioInventoryWrapper");
        Object inventory = type.getConstructor(net.minecraft.world.entity.LivingEntity.class).newInstance(entity);
        var getter = type.getMethod("getCurios");
        Object initial = getter.invoke(inventory);
        helper.assertTrue(initial == getter.invoke(inventory), "Curios view was not reused");
        java.util.Map<String, Object> replacement = new java.util.LinkedHashMap<>();
        type.getMethod("setCurios", java.util.Map.class).invoke(inventory, replacement);
        Object changed = getter.invoke(inventory);
        helper.assertTrue(changed != initial, "Curios view still references old storage");
        replacement.put("test", null);
        helper.assertTrue(((java.util.Map<?, ?>) changed).containsKey("test"), "Curios view is not live");
        entity.discard();
        helper.succeed();
    }
}
