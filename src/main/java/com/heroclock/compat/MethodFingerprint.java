package com.heroclock.compat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Map;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public final class MethodFingerprint {
    @FunctionalInterface
    public interface Names {
        String map(boolean method, String owner, String name, String descriptor);
    }

    public static final Names IDENTITY = (method, owner, name, descriptor) -> name;

    private MethodFingerprint() {}

    public static String hash(MethodNode method, Names names) {
        StringBuilder out = new StringBuilder();
        append(out, method.access, method.name, method.desc);
        Map<LabelNode, Integer> labels = new IdentityHashMap<>();
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LabelNode label) labels.put(label, index);
            else if (instruction.getOpcode() >= 0) index++;
        }
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() < 0) continue;
            append(out, instruction.getOpcode());
            if (instruction instanceof IntInsnNode node) append(out, node.operand);
            else if (instruction instanceof VarInsnNode node) append(out, node.var);
            else if (instruction instanceof TypeInsnNode node) append(out, node.desc);
            else if (instruction instanceof FieldInsnNode node) {
                append(out, node.owner, names.map(false, node.owner, node.name, node.desc), node.desc);
            } else if (instruction instanceof MethodInsnNode node) {
                append(out, node.owner, names.map(true, node.owner, node.name, node.desc), node.desc, node.itf);
            } else if (instruction instanceof InvokeDynamicInsnNode node) {
                append(out, node.name, node.desc);
                constant(out, node.bsm, names);
                for (Object argument : node.bsmArgs) constant(out, argument, names);
            } else if (instruction instanceof JumpInsnNode node) append(out, labels.get(node.label));
            else if (instruction instanceof LdcInsnNode node) constant(out, node.cst, names);
            else if (instruction instanceof IincInsnNode node) append(out, node.var, node.incr);
            else if (instruction instanceof TableSwitchInsnNode node) {
                append(out, node.min, node.max, labels.get(node.dflt));
                node.labels.forEach(label -> append(out, labels.get(label)));
            } else if (instruction instanceof LookupSwitchInsnNode node) {
                append(out, labels.get(node.dflt));
                for (int i = 0; i < node.keys.size(); i++) append(out, node.keys.get(i), labels.get(node.labels.get(i)));
            } else if (instruction instanceof MultiANewArrayInsnNode node) append(out, node.desc, node.dims);
        }
        for (TryCatchBlockNode handler : method.tryCatchBlocks) {
            append(out, labels.get(handler.start), labels.get(handler.end), labels.get(handler.handler), handler.type);
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(out.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static void constant(StringBuilder out, Object value, Names names) {
        if (value instanceof Handle handle) {
            boolean method = handle.getTag() >= 5;
            append(out, "handle", handle.getTag(), handle.getOwner(),
                    names.map(method, handle.getOwner(), handle.getName(), handle.getDesc()), handle.getDesc(), handle.isInterface());
        } else if (value instanceof ConstantDynamic dynamic) {
            append(out, "dynamic", dynamic.getName(), dynamic.getDescriptor());
            constant(out, dynamic.getBootstrapMethod(), names);
            for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) constant(out, dynamic.getBootstrapMethodArgument(i), names);
        } else if (value instanceof Type type) append(out, "type", type.getDescriptor());
        else append(out, value == null ? "null" : value.getClass().getName(), value);
    }

    private static void append(StringBuilder out, Object... values) {
        for (Object value : values) {
            String text = String.valueOf(value);
            out.append(text.length()).append(':').append(text).append(';');
        }
    }
}
