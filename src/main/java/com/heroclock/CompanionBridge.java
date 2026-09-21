package com.heroclock;

final class CompanionBridge {
    private CompanionBridge() {}

    static void handshake() {
        try {
            Class<?> api = Class.forName("com.openai.omnioptimizer.api.OmniOptimizerAPI");
            api.getMethod("registerCompanion", String.class, String.class, String[].class)
                    .invoke(null, HeroClock.MOD_ID, HeroClock.VERSION,
                            new String[]{"deadlines", "bounded_work", "palladium_optimizations"});
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException | LinkageError failure) {
            HeroClock.LOGGER.warn("OmniOptimizer companion handshake unavailable", failure);
        }
    }
}
