/*
 * Decompiled with CFR 0.152.
 */
package com.heroclock;

import java.lang.reflect.Method;

public final class CompanionBridge {
    private CompanionBridge() {
    }

    public static void handshake() {
        try {
            Class<?> clazz = Class.forName("com.openai.omnioptimizer.api.OmniOptimizerAPI");
            Method method = clazz.getMethod("version", new Class[0]);
            Method method2 = clazz.getMethod("registerCompanion", String.class, String.class, String[].class);
            method2.invoke(null, "heroclock", "2.2.3", new String[]{"deadlines", "compat_patching", "ice_decay", "omnitrix_utilities"});
            System.out.println("[HeroClock] OmniOptimizer " + String.valueOf(method.invoke(null, new Object[0])) + " companion handshake active.");
        }
        catch (ClassNotFoundException classNotFoundException) {
        }
        catch (Throwable throwable) {
            System.err.println("[HeroClock] OmniOptimizer companion handshake unavailable: " + throwable.getClass().getSimpleName());
        }
    }
}

