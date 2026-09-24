package com.heroclock.gametest;

import com.heroclock.api.HeroIntegrationAPI;
import com.heroclock.api.HeroScriptAPI;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandlerContainer;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;

public final class ScriptingChecks {
    private ScriptingChecks() {}

    public static void run(GameTestHelper helper) throws Exception {
        boolean changed = Boolean.getBoolean("heroclock.testChangedScripts");
        RhinoHotspotChecks.run(helper, changed);
        var decisions = HeroIntegrationAPI.compatibility();
        for (String patch : List.of("KubeEventContainerMixin", "RhinoMapIdsMixin", "KubeBoundaryMixin")) {
            helper.assertTrue(decisions.getOrDefault(patch, "missing").startsWith(changed ? "disabled:" : "enabled:"),
                    "Unexpected scripting contract result: " + patch + " " + decisions);
        }
        helper.assertTrue(decisions.getOrDefault("RhinoBoundaryMixin", "missing").startsWith("enabled:"),
                "Matching Rhino boundary was not retained");

        List<Integer> seen = new ArrayList<>();
        var root = new EventHandlerContainer(null, event -> seen.add(0), "test", 0);
        for (int i = 1; i <= 1000; i++) {
            int item = i;
            root.add(null, event -> seen.add(item), "test", i);
        }
        var child = EventHandlerContainer.class.getDeclaredField(changed ? "fixture_child" : "child");
        child.setAccessible(true);
        ((EventHandlerContainer) child.get(root)).add(null, event -> seen.add(1001), "test", 1001);
        root.add(null, event -> seen.add(1002), "test", 1002);
        try { root.handle(new EventJS(), null); }
        catch (dev.latvian.mods.kubejs.event.EventExit unexpected) { throw new AssertionError(unexpected); }
        helper.assertTrue(seen.equals(java.util.stream.IntStream.rangeClosed(0, 1002).boxed().toList()),
                "Listener append changed order or lost descendant appends");
        if (!changed) {
            var tail = Arrays.stream(root.getClass().getDeclaredFields()).filter(f -> f.getName().contains("heroclock$tail"))
                    .findFirst().orElseThrow();
            tail.setAccessible(true);
            helper.assertTrue(tail.get(root) != null, "Listener append cache was not populated");
        }

        Context context = Context.enter();
        var scope = context.initStandardObjects();
        LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
        map.put("first", 1); map.put(4, 2); map.put(null, 3);
        NativeJavaMap wrapped = new NativeJavaMap(context, scope, map, map);
        Object[] ids = wrapped.getIds(context);
        helper.assertTrue(Arrays.equals(ids, new Object[]{"first", 4, "null"}), "Rhino key conversion or order changed");
        ids[0] = "corrupt";
        map.remove(4); map.put("new", 4);
        helper.assertTrue(Arrays.equals(wrapped.getIds(context), new Object[]{"first", "null", "new"}),
                "Enumeration retained stale map keys or shared mutable results");
        map.clear();
        helper.assertTrue(wrapped.getIds(context).length == 0, "Empty map enumeration changed");

        var handler = EventGroup.of("HeroClockTest").server("boundary", () -> EventJS.class);
        handler.listenJava(ScriptType.SERVER, null, event -> seen.add(-1));
        long before = HeroScriptAPI.profile().get("rhino_calls").calls();
        long kubeBefore = HeroScriptAPI.profile().get("kubejs_handlers").calls();
        HeroScriptAPI.setProfilingEnabled(true);
        try {
            context.callSync((cx, s, self, args) -> {
                helper.assertTrue(Thread.holdsLock(cx.lock), "Rhino synchronization was bypassed");
                return cx.callSync((nested, ns, nself, nargs) -> 42, s, self, args);
            }, scope, scope, new Object[0]);
            RuntimeException expected = new RuntimeException("test");
            try {
                context.callSync((cx, s, self, args) -> { throw expected; }, scope, scope, new Object[0]);
                throw new AssertionError("Rhino exception swallowed");
            } catch (RuntimeException actual) { helper.assertTrue(actual == expected, "Rhino exception changed"); }
            handler.post(ScriptType.SERVER, new EventJS());
        } finally { HeroScriptAPI.setProfilingEnabled(false); }
        helper.assertTrue(HeroScriptAPI.profile().get("rhino_calls").calls() == before + 3, "Rhino nested-call count changed");
        helper.assertTrue(HeroScriptAPI.profile().get("rhino_calls").outermostNanos() > 0, "Rhino timing missing");
        helper.assertTrue(HeroScriptAPI.profile().get("kubejs_handlers").calls() == kubeBefore + (changed ? 0 : 1),
                "KubeJS boundary instrumentation/fallback failed");
        helper.assertTrue(seen.get(seen.size() - 1) == -1, "Handler dispatch changed");
        handler.clear(ScriptType.SERVER);
        handler.post(ScriptType.SERVER, new EventJS());
        helper.assertTrue(!handler.hasListeners(), "Listener reload retained old registration");
        context.addToScope(scope, "HeroScript", HeroScriptAPI.class);
        Object visible = context.evaluateString(scope, "HeroScript.profilingEnabled() === false", "heroclock-api-test", 1, null);
        helper.assertTrue(Boolean.TRUE.equals(visible), "Embedded API is not callable from Rhino");
        context.addToScope(scope, "server", helper.getLevel().getServer());
        context.addToScope(scope, "items", new ArrayList<>(List.of(1, 2, 3)).iterator());
        Object accepted = context.evaluateString(scope,
                "var receivedTotal = 0; HeroScript.batch(server, 'rhino_test', 'callback', items, 1, value => { receivedTotal += Number(value); })",
                "heroclock-batch-test", 1, null);
        helper.assertTrue(Boolean.TRUE.equals(accepted), "Rhino callback batch rejected");
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(Boolean.TRUE.equals(context.evaluateString(scope, "receivedTotal === 6",
                    "heroclock-batch-result", 1, null)), "Rhino callback lost items or failed Java adaptation");
            helper.succeed();
        });
    }
}
