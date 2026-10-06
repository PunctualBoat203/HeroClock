package com.heroclock.scripting;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;

public final class ScriptJavaClasses {
    private final Context context;
    private final Scriptable scope;

    public ScriptJavaClasses(Context context, Scriptable scope) {
        this.context = context;
        this.scope = scope;
    }

    public Scriptable loadClass(String name) throws ClassNotFoundException {
        Class<?> type = Class.forName(name, false, ScriptJavaClasses.class.getClassLoader());
        return context.getWrapFactory().wrapJavaClass(context, scope, type);
    }
}
