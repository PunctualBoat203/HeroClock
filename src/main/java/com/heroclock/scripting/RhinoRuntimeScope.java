package com.heroclock.scripting;

import com.heroclock.runtime.ScriptRuntimes;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class RhinoRuntimeScope {
    private final Context context;
    private final Scriptable scope;
    private final Map<ScriptRuntimes.Session, RhinoRuntimeBinding> sessions = new HashMap<>();
    private volatile boolean active = true;

    public RhinoRuntimeScope(Context context, Scriptable scope) { this.context = context; this.scope = scope; }

    public synchronized RhinoRuntimeBinding forServer(MinecraftServer server, String namespace) {
        if (!active) throw new IllegalStateException("HeroClock script scope has been unloaded");
        var session = ScriptRuntimes.open(server, namespace, this, this::prune);
        return sessions.computeIfAbsent(session, value -> new RhinoRuntimeBinding(context, scope, value));
    }

    public synchronized boolean onServer(String namespace, Object function) {
        if (!active) throw new IllegalStateException("HeroClock script scope has been unloaded");
        var callback = RhinoRuntimeBinding.adapt(context, scope, function);
        return ScriptRuntimes.prepare(this, namespace, server -> {
            if (active) callback.call(new Object[]{forServer(server, namespace), server});
        });
    }

    private synchronized void prune() { sessions.keySet().removeIf(session -> !session.isActive()); }

    @dev.latvian.mods.rhino.util.HideFromJS
    public void invalidate() {
        ScriptRuntimes.Session[] previous;
        synchronized (this) {
            active = false;
            previous = sessions.keySet().toArray(ScriptRuntimes.Session[]::new);
            sessions.clear();
            ScriptRuntimes.cancelSetups(this);
        }
        for (var session : previous) session.close();
    }
}
