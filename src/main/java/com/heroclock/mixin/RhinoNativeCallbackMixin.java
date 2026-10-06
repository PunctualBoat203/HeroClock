package com.heroclock.mixin;

import com.heroclock.scripting.NativeCallbackHandler;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.InterfaceAdapter;
import dev.latvian.mods.rhino.Scriptable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.latvian.mods.rhino.VMBridge", remap = false)
abstract class RhinoNativeCallbackMixin {
    @Redirect(method = "newInterfaceProxy", at = @At(value = "INVOKE",
            target = "Ljava/lang/reflect/Constructor;newInstance([Ljava/lang/Object;)Ljava/lang/Object;"), require = 1, allow = 1)
    private static Object heroclock$nativeCallback(Constructor<?> constructor, Object[] arguments,
            Object helper, InterfaceAdapter adapter, Object target, Scriptable scope, Context context)
            throws InvocationTargetException, InstantiationException, IllegalAccessException {
        if (adapter.getClass() == InterfaceAdapter.class && target instanceof Scriptable scriptable)
            arguments[0] = new NativeCallbackHandler(context, scope, scriptable);
        return constructor.newInstance(arguments);
    }
}
