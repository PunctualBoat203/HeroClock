/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.fml.common.Mod
 */
package com.heroclock;

import com.heroclock.CompanionBridge;
import com.heroclock.PatchManager;
import com.heroclock.VersionGuard;
import net.minecraftforge.fml.common.Mod;

@Mod(value="heroclock")
public final class HeroClock {
    public static final String MOD_ID = "heroclock";

    public HeroClock() {
        System.out.println("[HeroClock] v2.2.8 loading timer API + guarded compatibility layer");
        VersionGuard.scan();
        PatchManager.apply();
        CompanionBridge.handshake();
    }
}

