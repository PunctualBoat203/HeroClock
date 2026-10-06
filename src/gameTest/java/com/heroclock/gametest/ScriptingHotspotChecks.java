package com.heroclock.gametest;

import com.heroclock.api.HeroIntegrationAPI;
import com.heroclock.api.HeroScriptAPI;
import dev.latvian.mods.kubejs.event.EventHandlerContainer;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.event.EventExit;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;

public final class ScriptingHotspotChecks {
    private ScriptingHotspotChecks() {}

    public static void run(GameTestHelper helper, boolean changed) throws Exception {
        for (String patch : List.of("RhinoHotspotMixin")) {
            helper.assertTrue(HeroIntegrationAPI.compatibility().getOrDefault(patch, "missing")
                    .startsWith(changed ? "disabled:" : "enabled:"), "Wrong detailed profiling gate: " + patch);
        }
        boolean direct = HeroIntegrationAPI.compatibility().getOrDefault("KubeNativeDispatchMixin", "missing").startsWith("enabled:");
        helper.assertTrue(HeroIntegrationAPI.compatibility().getOrDefault("KubeListenerProfileMixin", "missing")
                .startsWith(changed || direct ? "disabled:" : "enabled:"), "Listener profiling route was not selected exclusively");
        HeroScriptAPI.resetHotspots();
        HeroScriptAPI.setHotspotProfilingEnabled(true);
        try {
            var context = Context.enter();
            var scope = context.initStandardObjects();
            var receiver = new RhinoHotspotChecks.Receiver(11);
            var wrapper = new NativeJavaObject(scope, receiver, receiver.getClass(), context);
            wrapper.get(context, "value", wrapper);
            List<Integer> order = new ArrayList<>();
            RuntimeException failure = new RuntimeException("listener-test");
            var root = new EventHandlerContainer(null, event -> { order.add(1); throw failure; }, "first.js", 7);
            root.add(null, event -> order.add(2), "second.js", 9);
            try {
                root.handle(new EventJS(), (event, container, throwable) -> {
                    helper.assertTrue(throwable == failure, "Profiling changed the exception");
                    return null;
                });
            } catch (EventExit unexpected) { throw new AssertionError(unexpected); }
            helper.assertTrue(order.equals(List.of(1, 2)), "Profiling changed listener continuation/order");
            EventExit exit = EventResult.Type.ERROR.exit(failure);
            var stopped = new EventHandlerContainer(null, event -> { throw exit; }, "exit.js", 3);
            stopped.add(null, event -> { throw new AssertionError("Cancelled listener executed"); }, "unreachable.js", 4);
            try { stopped.handle(new EventJS(), null); throw new AssertionError("Exit swallowed"); }
            catch (EventExit actual) { helper.assertTrue(actual == exit, "Exit identity changed"); }
            var snapshot = HeroScriptAPI.hotspots();
            if (changed) helper.assertTrue(snapshot.entries().isEmpty(), "Rejected hooks still collected data");
            else {
                var wrap = snapshot.entries().entrySet().stream().filter(e -> e.getKey().kind().equals("wrapper_init")
                        && e.getKey().owner().equals(receiver.getClass().getName())).findFirst().orElseThrow();
                helper.assertTrue(wrap.getValue().calls() == 1 && wrap.getValue().units() == 2, "Wrapper/collision attribution wrong");
                helper.assertTrue(snapshot.entries().keySet().stream().anyMatch(k -> k.kind().equals("member_read") && k.detail().equals("value")), "Member attribution missing");
                for (String source : List.of("first.js", "second.js", "exit.js")) {
                    var listener = snapshot.entries().entrySet().stream().filter(e -> e.getKey().kind().equals("kubejs_listener")
                            && e.getKey().owner().equals(source)).findFirst().orElseThrow().getValue();
                    helper.assertTrue(listener.calls() == 1 && listener.inclusiveNanos() > 0, "Listener attribution missing");
                }
            }
        } finally { HeroScriptAPI.setHotspotProfilingEnabled(false); }
        var before = HeroScriptAPI.hotspots();
        var quiet = new EventHandlerContainer(null, event -> null, "disabled.js", 1);
        try { quiet.handle(new EventJS(), null); }
        catch (EventExit unexpected) { throw new AssertionError(unexpected); }
        helper.assertTrue(before.equals(HeroScriptAPI.hotspots()), "Disabled profiling collected data");
        HeroScriptAPI.resetHotspots();
    }
}
