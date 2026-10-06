package com.heroclock.runtime;

import com.heroclock.api.HeroScriptAPI;
import java.util.concurrent.atomic.LongAdder;

public final class ScriptTakeover {
    private static final LongAdder PROXIES = new LongAdder();
    private static final LongAdder LISTENERS = new LongAdder();
    private ScriptTakeover() {}
    public static void proxyCreated() { PROXIES.increment(); }
    public static void listenerCreated() { LISTENERS.increment(); }
    public static HeroScriptAPI.TakeoverStats snapshot() {
        return new HeroScriptAPI.TakeoverStats(PROXIES.sum(), LISTENERS.sum());
    }
}
