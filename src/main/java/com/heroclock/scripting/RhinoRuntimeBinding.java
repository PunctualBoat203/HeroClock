package com.heroclock.scripting;

import com.heroclock.api.HeroScriptRuntime;
import com.heroclock.runtime.ScriptRuntimes;
import dev.latvian.mods.rhino.Callable;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import java.util.Iterator;
import java.util.function.BooleanSupplier;

public final class RhinoRuntimeBinding {
    private final Context context;
    private final Scriptable scope;
    private final ScriptRuntimes.Session runtime;

    RhinoRuntimeBinding(Context context, Scriptable scope, ScriptRuntimes.Session runtime) {
        this.context = context; this.scope = scope; this.runtime = runtime;
    }

    private HeroScriptRuntime.Callback callback(Object function) {
        return adapt(context, scope, function, runtime::isActive);
    }

    static HeroScriptRuntime.Callback adapt(Context context, Scriptable scope, Object function, BooleanSupplier active) {
        if (!(function instanceof Callable callable)) throw new IllegalArgumentException("Expected a JavaScript function");
        return arguments -> {
            synchronized (context.lock) {
                if (!active.getAsBoolean()) return;
                var wrappers = context.getWrapFactory();
                for (int i = 0; i < arguments.length; i++) {
                    Object value = arguments[i];
                    if (!(value instanceof String || value instanceof Number || value instanceof Boolean))
                        arguments[i] = wrappers.wrap(context, scope, value, null);
                }
                context.callSync(callable, scope, scope, arguments);
            }
        };
    }

    public boolean on(String event, String key, Object function) { return runtime.on(event, key, callback(function)); }
    public boolean off(String event, String key) { return runtime.off(event, key); }
    public int emit(String event, Object... arguments) { return runtime.emit(event, arguments); }
    public boolean schedule(String key, long delayTicks, Object function, Object... arguments) {
        return runtime.schedule(key, delayTicks, callback(function), arguments);
    }
    public boolean batch(String key, Iterator<?> items, int itemsPerTick, Object function) {
        return runtime.batch(key, items, itemsPerTick, callback(function));
    }
    public boolean cancel(String key) { return runtime.cancel(key); }
    public HeroScriptRuntime.Status status() { return runtime.status(); }
    public void close() { runtime.close(); }
}
