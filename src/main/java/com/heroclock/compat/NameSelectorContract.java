package com.heroclock.compat;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public final class NameSelectorContract {
    public static final String SELECTOR = "net/minecraft/commands/arguments/selector/options/EntitySelectorOptions";
    private static final String COMPONENT = "net/minecraft/network/chat/Component";
    private static final String TEXT = "net/minecraft/network/chat/FormattedText";
    private static final String ENTITY = "net/minecraft/world/entity/Entity";
    private static final String BRIDGE = "com/heroclock/runtime/NameMatcher";
    private static final String STRING = "()Ljava/lang/String;";
    private static final String NAME = "()Lnet/minecraft/network/chat/Component;";
    private static final String VISIT = "(Lnet/minecraft/network/chat/FormattedText$ContentConsumer;)Ljava/util/Optional;";
    private static final String APPEND = "(Ljava/lang/StringBuilder;Ljava/lang/String;)Ljava/util/Optional;";
    private static final String PREDICATE = "(Ljava/lang/String;ZLnet/minecraft/world/entity/Entity;)Z";
    private static final Map<String, String> EXPECTED = load();

    private NameSelectorContract() {}

    public static String mismatch(Function<String, ClassNode> classes) {
        ClassNode selector = classes.apply(SELECTOR);
        ClassNode component = classes.apply(COMPONENT);
        ClassNode text = classes.apply(TEXT);
        ClassNode bridge = classes.apply(BRIDGE);
        if (selector == null || component == null || text == null || bridge == null) return "required class missing";
        if ((component.access & Opcodes.ACC_INTERFACE) == 0 || (text.access & Opcodes.ACC_INTERFACE) == 0) {
            return "component interfaces changed";
        }

        String entityName = reference(bridge, ENTITY, NAME);
        String visit = reference(bridge, COMPONENT, VISIT);
        String getString = reference(bridge, COMPONENT, STRING);
        if (entityName == null || visit == null || getString == null) return "mapped bridge references unavailable";

        MethodNode predicate = unique(selector, PREDICATE, List.of("lambda$bootStrap$5", "m_175206_"));
        MethodNode componentString = unique(component, STRING, List.of(getString));
        MethodNode formattedString = unique(text, STRING, List.of(getString));
        if (predicate == null || componentString == null || formattedString == null) return "required method missing";

        List<Handle> appendHandles = new ArrayList<>();
        for (var instruction : formattedString.instructions) {
            if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                for (Object argument : dynamic.bsmArgs) {
                    if (argument instanceof Handle handle && handle.getOwner().equals(TEXT)
                            && handle.getDesc().equals(APPEND)) appendHandles.add(handle);
                }
            }
        }
        if (appendHandles.size() != 1) return "string visitor changed";
        String appendName = appendHandles.get(0).getName();
        MethodNode append = unique(text, APPEND, List.of(appendName));
        if (append == null) return "string visitor missing";

        MethodFingerprint.Names names = (method, owner, name, descriptor) -> {
            if (!method) return name;
            if (owner.equals(ENTITY) && name.equals(entityName) && descriptor.equals(NAME)) return "getName";
            if (owner.equals(TEXT) && name.equals(visit) && descriptor.equals(VISIT)) return "visit";
            if ((owner.equals(TEXT) || owner.equals(COMPONENT)) && name.equals(getString) && descriptor.equals(STRING)) return "getString";
            if (owner.equals(TEXT) && name.equals(appendName) && descriptor.equals(APPEND)) return "appendFragment";
            return name;
        };
        for (var check : List.of(new Check("namePredicate", "namePredicate", predicate),
                new Check("componentString", "getString", componentString),
                new Check("formattedString", "getString", formattedString),
                new Check("appendFragment", "appendFragment", append))) {
            if (!MethodFingerprint.hash(check.method(), check.name(), names).equals(EXPECTED.get(check.key()))) {
                return "method body changed: " + check.key();
            }
        }
        return null;
    }

    private static MethodNode unique(ClassNode owner, String descriptor, List<String> names) {
        List<MethodNode> matches = owner.methods.stream()
                .filter(method -> descriptor.equals(method.desc) && names.contains(method.name)).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private static String reference(ClassNode bridge, String owner, String descriptor) {
        String found = null;
        for (MethodNode method : bridge.methods) {
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.desc.equals(descriptor)) {
                    if (found != null && !found.equals(call.name)) return null;
                    found = call.name;
                }
            }
        }
        return found;
    }

    private record Check(String key, String name, MethodNode method) {}

    @SuppressWarnings("unchecked")
    private static Map<String, String> load() {
        try (var input = NameSelectorContract.class.getResourceAsStream("/compatibility/name-selector.json")) {
            if (input == null) return Map.of();
            return Map.copyOf(new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Map.class));
        } catch (Exception failure) {
            return Map.of();
        }
    }
}
