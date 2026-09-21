package com.heroclock.mixin;

import com.heroclock.compat.CompatibilityGate;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class PalladiumPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!targetClassName.startsWith("net.threetag.palladium.")
                && !targetClassName.startsWith("top.theillusivec4.curios.")) return true;
        return CompatibilityGate.allows(mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1), targetClassName);
    }

    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
