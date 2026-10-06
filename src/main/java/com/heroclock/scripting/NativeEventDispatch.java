package com.heroclock.scripting;

import com.heroclock.runtime.ScriptHotspots;
import com.heroclock.runtime.ScriptTakeover;
import dev.latvian.mods.kubejs.event.EventExit;
import dev.latvian.mods.kubejs.event.IEventHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;

public final class NativeEventDispatch {
    private static final boolean DETAIL = Boolean.getBoolean("heroclock.enableDetailedScriptingProfiling");
    private NativeEventDispatch() {}

    public static IEventHandler prepare(IEventHandler original, String source, int line) {
        IEventHandler direct = original;
        if (original != null && Proxy.isProxyClass(original.getClass())
                && Proxy.getInvocationHandler(original) instanceof NativeCallbackHandler callback) {
            direct = event -> {
                try { return callback.call(original, "onEvent", Object.class, new Object[]{event}); }
                catch (RuntimeException | Error failure) { throw failure; }
                catch (Throwable failure) {
                    if (failure instanceof EventExit exit) throw exit;
                    throw new UndeclaredThrowableException(failure);
                }
            };
            ScriptTakeover.listenerCreated();
        }
        if (!DETAIL) return direct;
        IEventHandler dispatch = direct;
        return event -> {
            if (!ScriptHotspots.enabled()) return dispatch.onEvent(event);
            long start = System.nanoTime();
            try { return dispatch.onEvent(event); }
            finally {
                ScriptHotspots.record("kubejs_listener", source,
                        line + ":" + (event == null ? "<null>" : event.getClass().getName()), 1, System.nanoTime() - start);
            }
        };
    }
}
