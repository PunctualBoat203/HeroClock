package com.heroclock.gametest;

import com.heroclock.api.HeroScriptAPI;
import com.heroclock.runtime.ScriptRuntimes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;

public final class OwnedRuntimeChecks {
    private OwnedRuntimeChecks() {}

    public static void run(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var runtime = HeroScriptAPI.openRuntime(server, "owned_test");
        List<Integer> seen = new ArrayList<>();
        runtime.on("test:event", "first", args -> seen.add((Integer) args[0]));
        helper.assertTrue(runtime.emit("test:event", 7) == 1 && seen.equals(List.of(7)), "Owned dispatch failed");
        helper.assertTrue(HeroScriptAPI.openRuntime(server, "owned_test") == runtime, "Namespace handle not reused");
        helper.assertTrue(runtime.schedule("later", 2, args -> seen.add(1)), "Schedule rejected");
        runtime.schedule("later", 2, args -> seen.add(2));
        runtime.schedule("cancelled", 2, args -> { throw new AssertionError("Cancelled job ran"); });
        runtime.cancel("cancelled");
        runtime.batch("batch", List.of(3, 4, 5).iterator(), 1, args -> {
            seen.add((Integer) args[0]);
            runtime.cancel("batch");
        });
        runtime.on("test:command", "source", args -> {
            helper.assertTrue(args[0] instanceof net.minecraft.commands.CommandSourceStack, "Command execution context lost");
            seen.add(8);
        });
        int result = server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "heroclock script emit owned_test test:command");
        helper.assertTrue(result == 1, "Datapack script emission failed");
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(seen.containsAll(List.of(7, 8, 2, 3)) && !seen.contains(1) && !seen.contains(4) && !seen.contains(5), "Scheduling/coalescing/batch cancellation changed");
            helper.assertTrue(runtime.status().pending() == 0, "Completed jobs retained");
            runtime.close();
            helper.assertTrue(!runtime.status().active() && runtime.status().listeners() == 0, "Runtime did not release listeners");
            boolean staleRejected = false;
            try { runtime.emit("test:event", 9); } catch (IllegalStateException expected) { staleRejected = true; }
            helper.assertTrue(staleRejected, "Closed runtime accepted work");
            helper.succeed();
        });
    }
}
