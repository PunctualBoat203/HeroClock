package com.heroclock.gametest;

import com.heroclock.api.HeroScriptAPI;
import com.heroclock.runtime.ServerScripts;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.ModList;

public final class StandaloneScriptChecks {
    private StandaloneScriptChecks() {}

    public static void run(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        if (!ModList.get().isLoaded("rhino")) {
            helper.assertTrue(HeroScriptAPI.reloadServerScripts(server).engine().equals("rhino_missing"),
                    "Absent Rhino loaded optional scripting classes");
            helper.succeed(); return;
        }
        Path directory = Files.createTempDirectory("heroclock-scripts-test");
        Path script = directory.resolve("owned.js");
        Path broken = directory.resolve("broken.js");
        Files.writeString(script, """
                var total = 0;
                var runtime = HeroRuntime.forServer(server, 'standalone_test');
                runtime.on('test:add', 'add', value => { total += Number(value); });
                runtime.on('test:verify', 'verify', () => { if (total !== 7) throw new Error('wrong total'); });
                runtime.schedule('initial', 0, () => { total += 3; });
                runtime.schedule('stale', 100, () => { throw new Error('old file ran'); });
                """);
        Files.writeString(broken, """
                var runtime = HeroRuntime.forServer(server, 'standalone_broken');
                runtime.on('test:leak', 'leak', () => {});
                HeroRuntime.onServer('standalone_abandoned', value => value.on('test:leak', 'leak', () => {}));
                throw new Error('expected failed file');
                """);
        var status = ServerScripts.reload(server, directory);
        helper.assertTrue(status.engine().equals("rhino") && status.loaded() == 1 && status.failed() == 1,
                "Standalone load did not isolate the failed file");
        helper.runAfterDelay(5, () -> {
            try {
                helper.assertTrue(HeroScriptAPI.emit(server, "standalone_broken", "test:leak") == 0
                        && HeroScriptAPI.emit(server, "standalone_abandoned", "test:leak") == 0, "Failed file retained callbacks or setup");
                helper.assertTrue(HeroScriptAPI.emit(server, "standalone_test", "test:add", 4) == 1, "Standalone event missing");
                helper.assertTrue(HeroScriptAPI.emit(server, "standalone_test", "test:verify") == 1, "Standalone scheduled callback failed");
                Files.delete(broken);
                Files.writeString(script, """
                        var runtime = HeroRuntime.forServer(server, 'standalone_test');
                        runtime.on('test:new', 'new', () => {});
                        """);
                helper.assertTrue(ServerScripts.reload(server, directory).loaded() == 1, "Standalone reload failed");
                helper.assertTrue(HeroScriptAPI.emit(server, "standalone_test", "test:add", 4) == 0
                        && HeroScriptAPI.emit(server, "standalone_test", "test:new") == 1, "Reload retained old file listeners");
                ServerScripts.close(server);
                helper.assertTrue(HeroScriptAPI.emit(server, "standalone_test", "test:new") == 0, "Standalone close retained namespace");
                helper.succeed();
            } catch (Exception failure) { throw new AssertionError(failure); }
            finally {
                ServerScripts.close(server);
                try (var paths = Files.walk(directory)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                } catch (Exception failure) { throw new AssertionError(failure); }
            }
        });
    }
}
