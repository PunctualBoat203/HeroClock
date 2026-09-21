package com.heroclock.mixin;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class PalladiumPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (targetClassName.startsWith("net.threetag.palladium.")) {
            if (Boolean.getBoolean("heroclock.disablePalladiumOptimizations")) return false;
            return hasVersion("palladium", "4.5.9");
        }
        if (targetClassName.startsWith("top.theillusivec4.curios.")) {
            if (Boolean.getBoolean("heroclock.disableCuriosOptimizations")) return false;
            return hasVersion("curios", "5.14.1+1.20.1");
        }
        return true;
    }

    private static boolean hasVersion(String modId, String version) {
        var mods = LoadingModList.get();
        if (mods == null) return false;
        var file = mods.getModFileById(modId);
        return file != null && file.getMods().stream().anyMatch(mod ->
                mod.getModId().equals(modId) && mod.getVersion().toString().equals(version));
    }

    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
