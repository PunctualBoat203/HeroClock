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
        if (!targetClassName.startsWith("net.threetag.palladium.")) return true;
        if (Boolean.getBoolean("heroclock.disablePalladiumOptimizations")) return false;
        var mods = LoadingModList.get();
        if (mods == null) return false;
        var file = mods.getModFileById("palladium");
        return file != null && file.getMods().stream().anyMatch(mod ->
                mod.getModId().equals("palladium") && mod.getVersion().toString().equals("4.5.9"));
    }

    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
