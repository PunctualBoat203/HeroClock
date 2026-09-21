package com.heroclock;

import java.util.Map;
import net.minecraftforge.fml.ModList;

final class VersionGuard {
    /*
     * These are the embedded Forge metadata versions from the exact stock JARs
     * supplied for the 2.2.15 compatibility pass. Filenames are intentionally
     * not used: several supplied artifacts advertise a different version in
     * their filename than Forge reports at runtime.
     */
    private static final Map<String, String> VERIFIED_BASELINES = Map.ofEntries(
            Map.entry("alienevo", "1.1.2"),
            Map.entry("infinity", "7.1"),
            Map.entry("infintrix", "2.2"),
            Map.entry("satsu_iron_man_addon", "3.5.3"),
            Map.entry("omni_evo", "1.0.0"),
            Map.entry("aeo", "1.0.6"),
            Map.entry("mypowers", "1.0.6"),
            Map.entry("powerborne", "0.5.1"),
            Map.entry("saiyan", "0.1"),
            Map.entry("pantheonsent", "1.1.1"),
            Map.entry("omnioptimizer", "1.8.0"));

    private VersionGuard() {}

    static void scan() {
        VERIFIED_BASELINES.forEach((id, supported) ->
                ModList.get().getModContainerById(id).ifPresent(mod -> {
                    String version = mod.getModInfo().getVersion().toString();
                    if (!version.equals(supported)) {
                        HeroClock.LOGGER.warn(
                                "HeroClock verified baseline for {} is {}, detected {}; "
                                        + "version-specific adapters must remain disabled until revalidated",
                                id, supported, version);
                    }
                }));
    }

    static boolean isVerifiedProfile(String modId) {
        String supported = VERIFIED_BASELINES.get(modId);
        if (supported == null) {
            return false;
        }
        return ModList.get().getModContainerById(modId)
                .map(mod -> supported.equals(mod.getModInfo().getVersion().toString()))
                .orElse(false);
    }
}
