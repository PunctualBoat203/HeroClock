package com.heroclock.gametest;

import com.heroclock.api.HeroIntegrationAPI;
import com.heroclock.api.HeroScriptAPI;
import com.heroclock.scripting.NativeCallbackHandler;
import com.heroclock.scripting.NativeEventContainer;
import dev.latvian.mods.kubejs.event.EventHandlerContainer;
import dev.latvian.mods.kubejs.event.EventExit;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.event.IEventHandler;
import dev.latvian.mods.rhino.BaseFunction;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaObject;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.WrapFactory;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;

public final class NativeTakeoverChecks {
    public interface Callback {
        int apply(int number, Object value);
        default int twice(int number) { return apply(number, null) * 2; }
    }
    private NativeTakeoverChecks() {}

    private static <T> T adapt(Context context, Scriptable scope, Class<T> type, ScriptableObject function) {
        context.setTopCall(scope);
        try { return type.cast(NativeJavaObject.createInterfaceAdapter(context, type, function)); }
        finally { context.setTopCall(null); }
    }

    private static Object evaluate(Context context, Scriptable scope, String source) {
        return context.evaluateString(scope, source, "heroclock-native-takeover-test", 1, null);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException raise(Throwable failure) throws E { throw (E) failure; }

    public static void run(GameTestHelper helper) throws Exception {
        boolean active = !Boolean.getBoolean("heroclock.testChangedScripts")
                && !Boolean.getBoolean("heroclock.testChangedCallback")
                && !Boolean.getBoolean("heroclock.disableNativeScriptingTakeover");
        for (String patch : List.of("RhinoNativeCallbackMixin", "KubeNativeDispatchMixin"))
            helper.assertTrue(HeroIntegrationAPI.compatibility().getOrDefault(patch, "missing")
                    .startsWith(active ? "enabled:" : "disabled:"), "Wrong automatic takeover decision: " + patch);
        var before = HeroScriptAPI.takeover();
        Context context = Context.enter();
        var scope = context.initStandardObjects();
        ScriptableObject function = (ScriptableObject) evaluate(context, scope, """
                var receiver;
                var effects = 0;
                (function(number, value) {
                    receiver = this;
                    effects++;
                    if (number < 0) throw new Error('expected');
                    return number + 1;
                })
                """);
        Callback callback = adapt(context, scope, Callback.class, function);
        helper.assertTrue((Proxy.getInvocationHandler(callback) instanceof NativeCallbackHandler) == active,
                "Native proxy did not select the expected backend");
        helper.assertTrue(callback.apply(6, new Object()) == 7 && callback.twice(9) == 20,
                "Native conversion/default method changed");
        Object receiver = evaluate(context, scope, "receiver");
        helper.assertTrue(receiver instanceof Wrapper wrapper && wrapper.unwrap() == callback,
                "Native callback lost its original proxy receiver");
        helper.assertTrue(callback.equals(callback) && !callback.equals(new Object())
                && callback.hashCode() == function.hashCode() && callback.toString().equals("Proxy[" + function + "]"),
                "Native proxy Object methods changed");
        boolean failed = false;
        try { callback.apply(-1, null); } catch (RuntimeException expected) { failed = true; }
        helper.assertTrue(failed && Boolean.TRUE.equals(evaluate(context, scope, "effects === 3")),
                "Native callback failure was swallowed or retried");

        ScriptableObject object = (ScriptableObject) evaluate(context, scope,
                "var eventObject = {onEvent: function(event) { return 1; }}; eventObject");
        IEventHandler handler = adapt(context, scope, IEventHandler.class, object);
        helper.assertTrue(((Number) handler.onEvent(new EventJS())).intValue() == 1, "Object callback failed");
        evaluate(context, scope, "eventObject.onEvent = function(event) { return 2; }");
        helper.assertTrue(((Number) handler.onEvent(new EventJS())).intValue() == 2, "Mutable callback member was cached");
        evaluate(context, scope, "eventObject.onEvent = 3");
        failed = false;
        try { handler.onEvent(new EventJS()); } catch (RuntimeException expected) { failed = true; }
        helper.assertTrue(failed, "Non-callable member did not fail");
        evaluate(context, scope, "eventObject.onEvent = function(event) { return 4; }");
        List<Integer> order = new ArrayList<>();
        var root = new EventHandlerContainer(null, handler, "native-test.js", 1);
        helper.assertTrue(root.handler == handler, "Public listener identity changed");
        helper.assertTrue((root instanceof NativeEventContainer) == active, "KubeJS direct dispatch/fallback incorrect");
        root.add(null, event -> {
            order.add(1);
            root.add(null, later -> order.add(3), "appended.js", 3);
            return null;
        }, "append.js", 2);
        root.add(null, event -> order.add(2), "last.js", 4);
        root.handle(new EventJS(), null);
        helper.assertTrue(order.equals(List.of(1, 2, 3)), "Dispatch lost listeners appended during the event");

        List<Object> wrapping = new ArrayList<>();
        context.setWrapFactory(new WrapFactory() {
            @Override public Object wrap(Context cx, Scriptable parent, Object value, Class<?> type) {
                wrapping.add(value);
                return super.wrap(cx, parent, value, type);
            }
            @Override public Scriptable wrapAsJavaObject(Context cx, Scriptable parent, Object value, Class<?> type) {
                wrapping.add(value);
                return super.wrapAsJavaObject(cx, parent, value, type);
            }
        });
        Object payload = new Object();
        callback.apply(1, payload);
        helper.assertTrue(wrapping.equals(List.of(payload, payload, callback)), "Custom wrapping order or fresh receiver changed");

        for (Throwable failure : List.of(new IOException("checked"), EventResult.Type.PASS.exit(null))) {
            BaseFunction throwing = new BaseFunction() {
                @Override public Object call(Context cx, Scriptable parent, Scriptable self, Object[] arguments) {
                    helper.assertTrue(Thread.holdsLock(cx.lock), "Native callback bypassed context locking");
                    throw NativeTakeoverChecks.<RuntimeException>raise(failure);
                }
            };
            var throwingHandler = adapt(context, scope, IEventHandler.class, throwing);
            var container = new EventHandlerContainer(null, throwingHandler, "checked.js", 1);
            if (failure instanceof EventExit expected) {
                try { container.handle(new EventJS(), null); throw new AssertionError("Event exit swallowed"); }
                catch (EventExit actual) { helper.assertTrue(actual == expected, "Declared event exit was changed"); }
            } else {
                container.handle(new EventJS(), (event, current, caught) -> {
                    helper.assertTrue(caught instanceof UndeclaredThrowableException wrapped
                            && wrapped.getUndeclaredThrowable() == failure, "Proxy exception wrapping changed on direct dispatch");
                    return null;
                });
            }
        }
        var after = HeroScriptAPI.takeover();
        helper.assertTrue(active ? after.nativeProxiesCreated() > before.nativeProxiesCreated()
                && after.directListenersCreated() > before.directListenersCreated() : after.equals(before),
                "Automatic takeover registration counters incorrect");
        helper.succeed();
    }
}
