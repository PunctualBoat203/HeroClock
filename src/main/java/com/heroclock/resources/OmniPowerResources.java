package com.heroclock.resources;

import com.heroclock.HeroClock;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.resource.PathPackResources;

public final class OmniPowerResources {
    private static final String DIRECTORY = "data/alienevo_aliens/palladium/powers/";
    static final Map<String, String> CONTRACTS = Map.of(
            DIRECTORY + "galvanic_rod.json", "0c2cced4a76d838549ddd332fe7476cd73da2d797bc9773284f6830ab4150606",
            DIRECTORY + "methanosian.json", "b617328b9d9bbf984918a1dbca30a990a8ffb8fa48e18e7770b98b769720ca34",
            DIRECTORY + "necrofriggian.json", "4e1a67ccbe5625bbd4dcae33376bb209f57cc93fb75f1d7b83dc0d7d5330472b",
            DIRECTORY + "nucleonix.json", "c8ccbda5c1a7c9973b0436f7f05bccad592d6d9f709f9bb9f1ed18ef4df9a543",
            DIRECTORY + "sonorosian.json", "af52d15cd5a1d12f83856c2d106f17406127c8e6b845096c286c1ca58985e2a1",
            DIRECTORY + "tetramand.json", "81f7fcebd9268bb16e7e766cc3725a1d41696c3e1482cb946afc072ed9710987",
            DIRECTORY + "vaxasaurian.json", "be365d97bb158d406ff12fbd60e95bc37d57862478614590d1230c3fe96fbf77");

    private OmniPowerResources() {}

    public static PathPackResources resolve(IModFileInfo modFile, PathPackResources original) {
        if (modFile.getMods().stream().noneMatch(mod -> HeroClock.MOD_ID.equals(mod.getModId()))) return original;
        var companion = ModList.get().getModFileById("omnioptimizer");
        if (companion == null) return original;
        return yieldVerified(original, path -> companion.getFile().findResource(path), CONTRACTS);
    }

    static PathPackResources yieldVerified(PathPackResources original, Function<String, Path> companion,
                                           Map<String, String> contracts) {
        Set<String> yielded = new HashSet<>();
        contracts.forEach((path, fingerprint) -> {
            try {
                if (ResourceFingerprint.matches(companion.apply(path), fingerprint)) yielded.add(path);
                else HeroClock.LOGGER.warn("HeroClock retained {}: companion resource is missing or changed", path);
            } catch (IOException | SecurityException failure) {
                HeroClock.LOGGER.warn("HeroClock retained {}: companion resource could not be verified", path, failure);
            }
        });
        if (yielded.isEmpty()) return original;
        HeroClock.LOGGER.info("HeroClock yielded {} audited power resources to OmniOptimizer", yielded.size());
        return new YieldingPackResources(original, yielded);
    }
}
