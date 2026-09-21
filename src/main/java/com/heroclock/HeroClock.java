package com.heroclock;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(HeroClock.MOD_ID)
public final class HeroClock {
    public static final String MOD_ID = "heroclock";
    public static final String VERSION = "2.2.15";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HeroClock() {
        VersionGuard.scan();
        CompanionBridge.handshake();
        LOGGER.info("HeroClock {} loaded", VERSION);
    }
}
