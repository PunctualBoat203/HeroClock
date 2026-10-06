package com.heroclock.scripting;

import com.heroclock.runtime.ScriptTakeover;
import dev.latvian.mods.rhino.Callable;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public final class NativeCallbackHandler implements InvocationHandler {
    private record Signature(Method method, String name, Class<?> result) {}
    private final Context context;
    private final Scriptable scope;
    private final Scriptable target;
    private final Callable function;
    private volatile Signature primary;

    public NativeCallbackHandler(Context context, Scriptable scope, Scriptable target) {
        this.context = context; this.scope = scope; this.target = target;
        function = target instanceof Callable callable ? callable : null;
        ScriptTakeover.proxyCreated();
    }

    @Override public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
        Signature signature = primary;
        if (signature != null && signature.method == method)
            return call(proxy, signature.name, signature.result, arguments);
        if (method.getDeclaringClass() == Object.class) {
            switch (method.getName()) {
                case "equals": return proxy == arguments[0];
                case "hashCode": return target.hashCode();
                case "toString": return "Proxy[" + target + "]";
                default: break;
            }
        }
        if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, arguments);
        String name = method.getName();
        Class<?> result = method.getReturnType();
        if (signature == null) primary = new Signature(method, name, result);
        return call(proxy, name, result, arguments);
    }

    public Object call(Object proxy, String name, Class<?> resultType, Object[] arguments) {
        Callable callback = function;
        if (callback == null) {
            Object member = ScriptableObject.getProperty(target, name, context);
            if (member == Scriptable.NOT_FOUND) {
                Context.reportWarning(ScriptRuntime.getMessage1("msg.undefined.function.interface", name), context);
                return resultType == void.class ? null : Context.jsToJava(context, null, resultType);
            }
            if (!(member instanceof Callable callable))
                throw Context.reportRuntimeError1("msg.not.function.interface", name, context);
            callback = callable;
        }
        var wrappers = context.getWrapFactory();
        Object[] values = arguments == null ? ScriptRuntime.EMPTY_OBJECTS : arguments;
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (!(value instanceof String || value instanceof Number || value instanceof Boolean))
                values[i] = wrappers.wrap(context, scope, value, null);
        }
        Scriptable receiver = wrappers.wrapAsJavaObject(context, scope, proxy, null);
        Object result = context.callSync(callback, scope, receiver, values);
        return resultType == void.class ? null : Context.jsToJava(context, result, resultType);
    }
}
