package com.heroclock;

import java.util.Map;
import net.minecraftforge.fml.ModList;

final class VersionGuard {
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
            Map.entry("palladium", "4.5.9"),
            Map.entry("kubejs", "2001.6.5-build.26"),
            Map.entry("rhino", "2001.2.3-build.10"),
            Map.entry("curios", "5.14.1+1.20.1"),
            Map.entry("pehkui", "3.8.2+1.20.1-forge"),
            Map.entry("omnioptimizer", "1.8.0"));

    private VersionGuard() {}

    static void scan() {
        VERIFIED_BASELINES.forEach((id, supported) ->
                ModList.get().getModContainerById(id).ifPresent(mod -> {
                    String version = mod.getModInfo().getVersion().toString();
                    if (!version.equals(supported)) {
                        HeroClock.LOGGER.warn(
                                "HeroClock verified baseline for {} is {}, detected {}; "
                                        + "optional runtime hooks are checked against their own code contracts",
                                id, supported, version);
                    }
                }));
    }

}
