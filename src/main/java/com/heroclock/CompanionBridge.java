package com.heroclock;

import net.minecraftforge.fml.ModList;

public final class CompanionBridge {
    private CompanionBridge() {}

    public static void handshake() {
        ModList.get().getModContainerById("omnioptimizer").ifPresent(container -> {
            Object companion = container.getMod();
            if (companion == null) return;
            String namespace = companion.getClass().getPackageName();
            ClassLoader loader = companion.getClass().getClassLoader();
            while (namespace.contains(".")) {
                try {
                    Class<?> api = Class.forName(namespace + ".api.OmniOptimizerAPI", false, loader);
                    api.getMethod("registerCompanion", String.class, String.class, String[].class)
                            .invoke(null, HeroClock.MOD_ID, HeroClock.VERSION,
                                    new String[]{"deadlines", "bounded_work", "palladium_optimizations"});
                    return;
                } catch (ClassNotFoundException absent) {
                    namespace = namespace.substring(0, namespace.lastIndexOf('.'));
                } catch (ReflectiveOperationException | LinkageError failure) {
                    HeroClock.LOGGER.warn("OmniOptimizer companion handshake unavailable", failure);
                    return;
                }
            }
        });
    }
}
