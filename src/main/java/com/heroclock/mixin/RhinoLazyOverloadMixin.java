package com.heroclock.mixin;

import dev.latvian.mods.rhino.NativeJavaMethod;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.latvian.mods.rhino.NativeJavaMethod", remap = false)
abstract class RhinoLazyOverloadMixin {
    @Unique private static final CopyOnWriteArrayList<?> heroclock$emptyOverloads = new CopyOnWriteArrayList<>();
    @Unique private static final AtomicReferenceFieldUpdater<NativeJavaMethod, CopyOnWriteArrayList> heroclock$cacheUpdater =
            AtomicReferenceFieldUpdater.newUpdater(NativeJavaMethod.class, CopyOnWriteArrayList.class, "heroclock$resolvedCache");
    @Unique private transient volatile CopyOnWriteArrayList<?> heroclock$resolvedCache;
    @Shadow @Final @Mutable private CopyOnWriteArrayList<?> overloadCache;

    @Redirect(method = {"<init>([Ldev/latvian/mods/rhino/MemberBox;)V",
            "<init>([Ldev/latvian/mods/rhino/MemberBox;Ljava/lang/String;)V",
            "<init>(Ldev/latvian/mods/rhino/MemberBox;Ljava/lang/String;)V"},
            at = @At(value = "NEW", target = "java/util/concurrent/CopyOnWriteArrayList"), require = 3, allow = 3)
    private CopyOnWriteArrayList<?> heroclock$deferOverloads() {
        return heroclock$emptyOverloads;
    }

    @Redirect(method = "findCachedFunction", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Ldev/latvian/mods/rhino/NativeJavaMethod;overloadCache:Ljava/util/concurrent/CopyOnWriteArrayList;"), require = 3, allow = 3)
    private CopyOnWriteArrayList<?> heroclock$resolveOverloads(NativeJavaMethod owner) {
        CopyOnWriteArrayList<?> cache = heroclock$resolvedCache;
        if (cache == null) {
            CopyOnWriteArrayList<?> created = new CopyOnWriteArrayList<>();
            if (heroclock$cacheUpdater.compareAndSet(owner, null, created)) {
                overloadCache = created;
                cache = created;
            } else cache = heroclock$resolvedCache;
        }
        return cache;
    }
}
