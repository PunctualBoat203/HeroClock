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
}
