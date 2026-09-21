package com.heroclock;

/**
 * Public reflection bridge for optional optimizer companions.
 *
 * <p>OmniOptimizer discovers this class without a compile-time HeroClock
 * dependency and calls {@link #handshake()} via {@code Class.getMethod}, so
 * both the class and method are intentionally public. Keep this surface
 * binary-compatible unless the companion protocol is versioned.</p>
 */
public final class CompanionBridge {
    private CompanionBridge() {}

    public static void handshake() {
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
