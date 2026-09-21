package com.heroclock.compat;

import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import cpw.mods.modlauncher.api.INameMappingService;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.spongepowered.asm.service.MixinService;

public final class CompatibilityGate {
    private static final Map<String, CompatibilityContract> CONTRACTS = load();
    private static final Map<String, String> DECISIONS = new ConcurrentHashMap<>();

    private CompatibilityGate() {}

    public static boolean allows(String mixin, String target) {
        CompatibilityContract contract = CONTRACTS.get(mixin);
        if (contract == null || !contract.target().equals(target)) return reject(mixin, "no audited contract");
        if (Boolean.getBoolean("heroclock.disable" + (contract.mod().equals("curios") ? "Curios" : "Palladium") + "Optimizations")) {
            return reject(mixin, "disabled by configuration");
        }
        var mods = LoadingModList.get();
        if (mods == null || mods.getModFileById(contract.mod()) == null) return reject(mixin, "target mod not loaded");
        try {
            var node = MixinService.getService().getBytecodeProvider().getClassNode(target);
            Map<String, String> aliases = new HashMap<>();
            for (var member : contract.minecraftMembers()) {
                var domain = member.method() ? INameMappingService.Domain.METHOD : INameMappingService.Domain.FIELD;
                String mapped = ObfuscationReflectionHelper.remapName(domain, member.name());
                aliases.put(key(member.method(), member.owner(), mapped, member.descriptor()), member.name());
            }
            String mismatch = contract.mismatch(node, (method, owner, name, descriptor) ->
                    aliases.getOrDefault(key(method, owner, name, descriptor), name));
            if (mismatch != null) return reject(mixin, mismatch);
            DECISIONS.put(mixin, "enabled: audited code contract matches");
            LogUtils.getLogger().info("HeroClock {} enabled: audited code contract matches", mixin);
            return true;
        } catch (Exception | LinkageError failure) {
            return reject(mixin, "inspection unavailable: " + failure.getClass().getSimpleName());
        }
    }

    public static Map<String, String> decisions() { return Map.copyOf(DECISIONS); }

    private static boolean reject(String mixin, String reason) {
        if (DECISIONS.put(mixin, "disabled: " + reason) == null) {
            LogUtils.getLogger().info("HeroClock {} keeps original behavior: {}", mixin, reason);
        }
        return false;
    }

    private static String key(boolean method, String owner, String name, String descriptor) {
        return method + ":" + owner + ":" + name + ":" + descriptor;
    }

    private static Map<String, CompatibilityContract> load() {
        try (var stream = CompatibilityGate.class.getResourceAsStream("/compatibility/contracts.json")) {
            if (stream == null) return Map.of();
            var contracts = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), CompatibilityContract[].class);
            Map<String, CompatibilityContract> result = new HashMap<>();
            for (var contract : contracts) result.put(contract.mixin(), contract);
            return Map.copyOf(result);
        } catch (RuntimeException | java.io.IOException failure) {
            LogUtils.getLogger().error("HeroClock compatibility contracts unavailable; optional patches disabled", failure);
            return Map.of();
        }
    }
}
