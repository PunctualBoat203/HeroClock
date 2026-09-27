package com.heroclock.gametest;

import com.heroclock.api.HeroIntegrationAPI;
import com.heroclock.api.HeroScriptAPI;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.ScriptableObject;
import net.minecraft.gametest.framework.GameTestHelper;

public final class RhinoOwnedRuntimeChecks {
    private RhinoOwnedRuntimeChecks() {}

    private static Object evaluate(ScriptManager manager, String script) {
        return manager.context.evaluateString(manager.topLevelScope, script, "heroclock-owned-test", 1, null);
    }

    public static void run(GameTestHelper helper) {
        boolean changed = Boolean.getBoolean("heroclock.testChangedScripts");
        var server = helper.getLevel().getServer();
        var manager = new ScriptManager(ScriptType.SERVER);
        manager.load();
        helper.assertTrue(HeroIntegrationAPI.compatibility().getOrDefault("KubeRuntimeMixin", "missing")
                .startsWith(changed ? "disabled:" : "enabled:"), "Wrong owned-runtime compatibility decision");
        boolean bound = ScriptableObject.hasProperty(manager.topLevelScope, "HeroRuntime", manager.context);
        helper.assertTrue(bound != changed, "Runtime binding/fallback incorrect");
        if (changed) { manager.unload(); helper.succeed(); return; }
        evaluate(manager, """
                var ownedTotal = 0;
                var ownedEffects = 0;
                var ownedTicks = 0;
                var ownedThis = this;
                var correctThis = false;
                var ownedHandle;
                HeroRuntime.onServer('rhino_owned_test', (runtime, server) => {
                    ownedHandle = runtime;
                    runtime.on('test:add', 'add', function(value) {
                        correctThis = this === ownedThis;
                        ownedTotal += Number(value);
                    });
                    runtime.on('test:fail', 'fail', () => { ownedEffects++; throw new Error('expected'); });
                    runtime.on('heroclock:server_tick', 'tick', () => { ownedTicks++; });
                    runtime.schedule('initial', 0, value => { ownedTotal += Number(value); }, 5);
                    runtime.schedule('cancel_on_reload', 100, () => { ownedEffects += 100; });
                });
                """);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(HeroScriptAPI.emit(server, "rhino_owned_test", "test:add", 4) == 1, "Rhino direct callback not registered");
            helper.assertTrue(Boolean.TRUE.equals(evaluate(manager, "ownedTotal === 9 && correctThis && ownedTicks > 0")), "Owned Rhino argument/this/scheduling behavior failed");
            boolean failed = false;
            try { HeroScriptAPI.emit(server, "rhino_owned_test", "test:fail"); }
            catch (RuntimeException expected) { failed = true; }
            helper.assertTrue(failed && Boolean.TRUE.equals(evaluate(manager, "ownedEffects === 1")), "Callback failed to propagate or was retried");
            manager.unload();
            helper.assertTrue(HeroScriptAPI.emit(server, "rhino_owned_test", "test:add", 10) == 0, "Reload retained old callbacks");
            helper.assertTrue(Boolean.TRUE.equals(evaluate(manager, "ownedHandle.status().active() === false && ownedHandle.status().pending() === 0")), "Reload did not cancel pending jobs");
            manager.load();
            evaluate(manager, "var replacement = 0; HeroRuntime.onServer('rhino_owned_test', runtime => runtime.on('test:new', 'new', () => { replacement++; }));");
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(HeroScriptAPI.emit(server, "rhino_owned_test", "test:new") == 1, "Reload could not replace namespace");
                helper.assertTrue(Boolean.TRUE.equals(evaluate(manager, "replacement === 1")), "Replacement callback did not execute");
                manager.unload();
                helper.succeed();
            });
        });
    }
}
