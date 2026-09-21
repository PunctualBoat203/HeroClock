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
        if (!net.minecraftforge.fml.ModList.get().isLoaded("palladium")) { helper.succeed(); return; }
        var entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 2));
        entity.setNoGravity(true);
        Class<?> handlerType = Class.forName("net.threetag.palladium.power.PowerHandler");
        Object handler = handlerType.getConstructor(net.minecraft.world.entity.LivingEntity.class).newInstance(entity);
        var getter = handlerType.getMethod("getPowerHolders");
        Object first = getter.invoke(handler);
        helper.assertTrue(first == getter.invoke(handler), "Power holder view was not reused");
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

        Class<?> commands = Class.forName("net.threetag.palladium.util.property.CommandFunctionProperty$CommandFunctionParsing");
        Object parsing = commands.getConstructor(java.util.List.class).newInstance(java.util.List.of());
        helper.assertTrue(commands.getMethod("getCommandFunction", net.minecraft.server.MinecraftServer.class)
                .invoke(parsing, helper.getLevel().getServer()) != null, "Command parsing failed");
        helper.assertTrue(java.util.Arrays.stream(commands.getDeclaredFields()).anyMatch(field ->
                field.getName().contains("dispatcher")), "Command cache mixin missing");
        entity.discard();
        helper.succeed();
    }
}
