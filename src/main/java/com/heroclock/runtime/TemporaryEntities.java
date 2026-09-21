package com.heroclock.runtime;

import com.heroclock.HeroClock;
import com.heroclock.api.HeroClockAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Mod.EventBusSubscriber(modid = HeroClock.MOD_ID)
public final class TemporaryEntities {
    private record Rule(int ticks, String score, String block, int radius, int below, int above) {}
    private static final Rule SPIKE = new Rule(200, "hcTempAge", null, 0, 0, 0);
    private static final Rule HELPER = new Rule(600, "hcTempAge", null, 0, 0, 0);
    private static final Rule SHORT = new Rule(400, "hcTempAge", null, 0, 0, 0);
    private static final Map<String, Rule> RULES = Map.ofEntries(
            Map.entry("lightning", new Rule(0, null, null, 0, 0, 0)),
            Map.entry("ice_timer", new Rule(200, "iceTime", "minecraft:blue_ice", 7, 7, 7)),
            Map.entry("blue_ice_timer", new Rule(400, "iceTime", "minecraft:blue_ice", 15, 15, 15)),
            Map.entry("web_timer", new Rule(400, "hcTempAge", "minecraft:cobweb", 1, 0, 2)),
            Map.entry("IceSpikes", SPIKE), Map.entry("ice_spike", SPIKE), Map.entry("ice_spike_target", SPIKE),
            Map.entry("earth_spike", SPIKE), Map.entry("earth_spike_target", SPIKE), Map.entry("diamond_spike", SPIKE),
            Map.entry("a.iceberg", HELPER), Map.entry("a.flying_block", HELPER), Map.entry("a.slime_rain", HELPER),
            Map.entry("a.root", HELPER), Map.entry("a.branch", HELPER), Map.entry("a.tree", HELPER),
            Map.entry("slime_puddle", SHORT), Map.entry("drago.ball", SPIKE), Map.entry("sound", SHORT),
            Map.entry("powerborne.shadow_field_marker", new Rule(1160, null, "powerborne:void_block", 6, 1, 6)),
            Map.entry("a.ice", new Rule(500, "afomni.lifetime", null, 0, 0, 0)));

    private TemporaryEntities() {}
    public static boolean handles(String tag) { return RULES.containsKey(tag); }

    public static long check(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity instanceof Player || entity.isRemoved()) return Long.MAX_VALUE;
        long now = level.getGameTime();
        long next = Long.MAX_VALUE;
        for (String tag : entity.getTags()) {
            Rule rule = RULES.get(tag);
            if (rule == null || !matchesType(entity, tag)) continue;
            String key = "cleanup." + tag;
            long deadline = HeroClockAPI.deadline(entity, key);
            if (deadline == 0) {
                int age = savedAge(entity, rule, tag);
                deadline = HeroClockAPI.set(entity, key, Math.max(0, rule.ticks - age));
            }
            if (deadline <= HeroClockAPI.now(entity)) {
                if (entity instanceof AreaEffectCloud cloud && tag.equals("powerborne.shadow_field_marker")) {
                    cloud.setDuration(Math.max(cloud.getDuration(), cloud.tickCount + 200));
                }
                WorkQueue queue = ServerRuntime.queue(level.getServer());
                if (!queue.contains(entity.getUUID(), key)) {
                    queue.submit(entity.getUUID(), key, HeroClockAPI.now(entity), new Cleanup(entity, tag, rule));
                }
                next = Math.min(next, TickMath.add(now, 20));
            } else {
                next = Math.min(next, TickMath.add(now, deadline - HeroClockAPI.now(entity)));
            }
        }
        return next;
    }

    private static boolean matchesType(Entity entity, String tag) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (tag.equals("lightning")) return type.toString().equals("palladium:custom_projectile");
        if (tag.equals("powerborne.shadow_field_marker")) return type.toString().equals("minecraft:area_effect_cloud");
        return true;
    }

    private static int savedAge(Entity entity, Rule rule, String tag) {
        if (tag.equals("powerborne.shadow_field_marker")) {
            var data = new net.minecraft.nbt.CompoundTag();
            entity.saveWithoutId(data);
            return Math.max(0, data.getInt("Age"));
        }
        if (rule.score == null) return 0;
        Scoreboard scoreboard = entity.level().getScoreboard();
        Objective objective = scoreboard.getObjective(rule.score);
        if (objective == null || !scoreboard.hasPlayerScore(entity.getScoreboardName(), objective)) return 0;
        return Math.max(0, scoreboard.getOrCreatePlayerScore(entity.getScoreboardName(), objective).getScore());
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ServerRuntime.cancelOwnerIfPresent(level.getServer(), event.getEntity().getUUID());
        }
    }

    private static final class Cleanup implements BooleanSupplier {
        private final Entity entity;
        private final String tag;
        private final Rule rule;
        private final BlockPos anchor;
        private int cursor;

        private Cleanup(Entity entity, String tag, Rule rule) {
            this.entity = entity;
            this.tag = tag;
            this.rule = rule;
            this.anchor = entity.blockPosition();
        }

        @Override
        public boolean getAsBoolean() {
            if (entity.isRemoved() || !entity.getTags().contains(tag)) return true;
            if (HeroClockAPI.remaining(entity, "cleanup." + tag) > 0) return true;
            ServerLevel level = (ServerLevel) entity.level();
            if (tag.equals("a.ice")) {
                var function = level.getServer().getFunctions().get(new ResourceLocation("afomni", "iceberg/break_ice"));
                if (function.isPresent()) {
                    level.getServer().getFunctions().execute(function.get(), entity.createCommandSourceStack().withSuppressedOutput().withPermission(2));
                }
                return true;
            }
            if (rule.block != null) {
                ResourceLocation id = new ResourceLocation(rule.block);
                if (!BuiltInRegistries.BLOCK.containsKey(id)) return true;
                Block target = BuiltInRegistries.BLOCK.get(id);
                BlockPos min = anchor.offset(-rule.radius, -rule.below, -rule.radius);
                BlockPos max = anchor.offset(rule.radius, rule.above, rule.radius);
                if (!level.hasChunksAt(min, max)) return false;
                int width = rule.radius * 2 + 1;
                int total = width * width * (rule.below + rule.above + 1);
                int stop = Math.min(total, cursor + 256);
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                while (cursor < stop) {
                    int i = cursor++;
                    pos.set(min.getX() + i % width, min.getY() + i / (width * width), min.getZ() + i / width % width);
                    if (level.getBlockState(pos).is(target)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
                if (cursor < total) return false;
            }
            entity.discard();
            return true;
        }
    }
}
