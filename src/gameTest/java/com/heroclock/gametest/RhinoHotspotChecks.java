package com.heroclock.gametest;

import com.heroclock.api.HeroIntegrationAPI;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaMethod;
import dev.latvian.mods.rhino.NativeJavaObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.gametest.framework.GameTestHelper;

public final class RhinoHotspotChecks {
    private RhinoHotspotChecks() {}

    public static final class Receiver {
        public int value;
        public int other;
        public Receiver(int value) { this.value = value; this.other = value * 2; }
        public int value() { return value; }
        public int value(int added) { return value + added; }
        public int other() { return other; }
    }

    public static void run(GameTestHelper helper, boolean changed) throws Exception {
        for (String patch : List.of("RhinoLazyOverloadMixin", "RhinoMemberMapMixin")) {
            helper.assertTrue(HeroIntegrationAPI.compatibility().getOrDefault(patch, "missing")
                    .startsWith(changed ? "disabled:" : "enabled:"), "Wrong hotspot gate decision: " + patch);
        }
        var context = Context.enter();
        var scope = context.initStandardObjects();
        var firstReceiver = new Receiver(7);
        var secondReceiver = new Receiver(19);
        var first = new NativeJavaObject(scope, firstReceiver, Receiver.class, context);
        var second = new NativeJavaObject(scope, secondReceiver, Receiver.class, context);
        var firstMethod = (NativeJavaMethod) first.get(context, "value", first);
        var secondMethod = (NativeJavaMethod) second.get(context, "value", second);
        var single = (NativeJavaMethod) first.get(context, "other", first);
        var cacheField = NativeJavaMethod.class.getDeclaredField(changed ? "fixture_overloadCache" : "overloadCache");
        cacheField.setAccessible(true);
        Object initial = cacheField.get(firstMethod);
        Object members = NativeJavaMethod.class.getField("methods").get(firstMethod);
        var arrayConstructor = NativeJavaMethod.class.getDeclaredConstructor(members.getClass());
        arrayConstructor.setAccessible(true);
        Object arrayMethod = arrayConstructor.newInstance(members);
        var reflectedMethod = new NativeJavaMethod(Receiver.class.getMethod("value"), "value");
        helper.assertTrue((cacheField.get(arrayMethod) == initial) == !changed, "Array constructor cache behavior changed");
        helper.assertTrue((cacheField.get(reflectedMethod) == initial) == !changed, "Reflected constructor cache behavior changed");
        helper.assertTrue((initial == cacheField.get(secondMethod)) == !changed, "Unused overload storage was not deferred/fell back incorrectly");
        helper.assertTrue(firstMethod != secondMethod, "Receiver-bound wrappers were shared");
        helper.assertTrue(((Number) firstMethod.getDefaultValue(context, Number.class)).intValue() == 7, "Field receiver changed");
        firstReceiver.value = 9;
        helper.assertTrue(((Number) firstMethod.getDefaultValue(context, Number.class)).intValue() == 9, "Live field read was cached");
        helper.assertTrue(((Number) firstMethod.call(context, scope, first, new Object[0])).intValue() == 9, "Zero-argument overload changed");
        helper.assertTrue(((Number) firstMethod.call(context, scope, first, new Object[]{3})).intValue() == 12, "Argument overload changed");
        helper.assertTrue(((Number) secondMethod.call(context, scope, second, new Object[]{3})).intValue() == 22, "Other receiver changed");
        helper.assertTrue(cacheField.get(firstMethod) != cacheField.get(secondMethod), "Active caches were shared across receivers");
        helper.assertTrue(!((CopyOnWriteArrayList<?>) cacheField.get(firstMethod)).isEmpty(), "Stock overload entries were not retained");
        Object singleCache = cacheField.get(single);
        single.call(context, scope, first, new Object[0]);
        helper.assertTrue(cacheField.get(single) == singleCache, "Single-method call unnecessarily created overload storage");
        if (!changed) {
            helper.assertTrue(singleCache == initial, "Unused cache marker was mutated or replaced");
            var third = new NativeJavaObject(scope, new Receiver(31), Receiver.class, context);
            var thirdMethod = (NativeJavaMethod) third.get(context, "value", third);
            var getter = java.util.Arrays.stream(NativeJavaMethod.class.getDeclaredMethods())
                    .filter(m -> m.getName().contains("heroclock$resolveOverloads")).findFirst().orElseThrow();
            getter.setAccessible(true);
            var pool = java.util.concurrent.Executors.newFixedThreadPool(4);
            try {
                var results = new java.util.ArrayList<java.util.concurrent.Future<Object>>();
                for (int i = 0; i < 8; i++) results.add(pool.submit(() -> getter.invoke(thirdMethod, thirdMethod)));
                Object expected = results.get(0).get(5, java.util.concurrent.TimeUnit.SECONDS);
                for (var result : results) helper.assertTrue(result.get(5, java.util.concurrent.TimeUnit.SECONDS) == expected,
                        "Concurrent first use published different caches");
                helper.assertTrue(cacheField.get(thirdMethod) == expected, "Stock field lost initialized cache");
            } finally { pool.shutdownNow(); }
        }
        var newParent = context.initStandardObjects();
        firstMethod.setParentScope(newParent);
        helper.assertTrue(firstMethod.getParentScope() == newParent, "Wrapper scope could not change");
        var newPrototype = context.newObject(scope);
        firstMethod.setPrototype(newPrototype);
        helper.assertTrue(firstMethod.getPrototype(context) == newPrototype, "Wrapper prototype could not change");
    }
}
