package com.heroclock;

import java.util.Map;
import net.minecraftforge.fml.ModList;

final class VersionGuard {
    private static final Map<String, String> SUPPORTED = Map.of(
            "alienevo", "1.1.3", "infinity", "7.2", "omni_evo", "1.0.6",
            "satsu_iron_man_addon", "3.5.3", "celestialsapien", "1.0.6.1");

    private VersionGuard() {}

    static void scan() {
        SUPPORTED.forEach((id, supported) -> ModList.get().getModContainerById(id).ifPresent(mod -> {
            String version = mod.getModInfo().getVersion().toString();
            if (!version.equals(supported)) {
                HeroClock.LOGGER.warn("HeroClock resources target {} {}, detected {}", id, supported, version);
            }
        }));
    }
}
