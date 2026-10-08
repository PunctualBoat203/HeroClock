package com.heroclock.gametest;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.resource.ResourcePackLoader;

final class CommandResourceChecks {
    private CommandResourceChecks() {}

    static void verify(GameTestHelper helper) throws Exception {
        String command;
        try (var pack = ResourcePackLoader.createPackForMod(ModList.get().getModFileById("heroclock"));
             var input = pack.getRootResource("data/alienevo_aliens/palladium/powers/necrofriggian.json").get()) {
            command = JsonParser.parseReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("abilities").getAsJsonObject("necrofriggian_loop")
                    .getAsJsonArray("commands").get(0).getAsString()
                    .replace("afomni:iceberg/projectile_tick", "heroclock:test_marker");
        }
        String original = "execute if entity @e[tag=a.iceberg,distance=..128,limit=1] as "
                + "@e[tag=a.iceberg,distance=..128] at @s run function heroclock:test_marker";
        var commands = helper.getLevel().getServer().getCommands();
        List<Entity> entities = new ArrayList<>();
        try {
            var executor = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
            executor.setNoGravity(true);
            entities.add(executor);
            var source = executor.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            var outside = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(4, 2, 1));
            entities.add(outside);
            outside.setPos(executor.getX() + 129, executor.getY(), executor.getZ());
            outside.setNoGravity(true);
            outside.addTag("a.iceberg");
            for (int count = 0; count <= 2; count++) {
                if (count > 0) {
                    var target = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(count + 1, 2, 1));
                    target.setNoGravity(true);
                    target.addTag("a.iceberg");
                    entities.add(target);
                }
                int before = commands.performPrefixedCommand(source, original);
                var selected = entities.stream().filter(entity -> entity.getTags().contains("heroclock_test_marker")).toList();
                helper.assertTrue(selected.size() == count && !selected.contains(outside) && !selected.contains(executor),
                        "Original selector fixture did not select the expected entities");
                entities.forEach(entity -> entity.removeTag("heroclock_test_marker"));
                int after = commands.performPrefixedCommand(source, command);
                helper.assertTrue(before == after, "Removing the pre-scan changed command results");
                helper.assertTrue(selected.equals(entities.stream()
                        .filter(entity -> entity.getTags().contains("heroclock_test_marker")).toList()),
                        "Removing the pre-scan changed function executors or distance scope");
                entities.forEach(entity -> entity.removeTag("heroclock_test_marker"));
            }
            helper.assertTrue(commands.performPrefixedCommand(source.withPermission(0), command) == 0
                    && entities.stream().noneMatch(entity -> entity.getTags().contains("heroclock_test_marker")),
                    "Optimized command bypassed permissions");
        } finally {
            entities.forEach(Entity::discard);
        }
    }
}
