package com.heroclock.scripting;

import dev.latvian.mods.rhino.Callable;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.gametest.framework.GameTestHelper;

public final class RhinoRuntimeChecks {
    private RhinoRuntimeChecks() {}

    public static void closedScopeSkipsWaitingCallback(GameTestHelper helper, Context context, Scriptable scope) {
        var active = new AtomicBoolean(true);
        var calls = new AtomicInteger();
        Callable function = (cx, parent, receiver, arguments) -> { calls.incrementAndGet(); return null; };
        var callback = RhinoRuntimeBinding.adapt(context, scope, function, active::get);
        var started = new CountDownLatch(1);
        CompletableFuture<Void> pending;
        try {
            synchronized (context.lock) {
                pending = CompletableFuture.runAsync(() -> {
                    started.countDown();
                    callback.call(new Object[0]);
                });
                helper.assertTrue(started.await(5, TimeUnit.SECONDS), "Callback worker did not start");
                active.set(false);
            }
            pending.get(5, TimeUnit.SECONDS);
        } catch (Exception failure) { throw new AssertionError("Callback lock test failed", failure); }
        helper.assertTrue(calls.get() == 0, "Unloaded scope ran a callback waiting for its context lock");
    }
}
