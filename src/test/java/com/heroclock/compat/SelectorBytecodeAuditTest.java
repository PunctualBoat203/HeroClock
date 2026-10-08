package com.heroclock.compat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

final class SelectorBytecodeAuditTest {
    @Test
    void inspectSupportedNameSelector() throws Exception {
        if (!Boolean.getBoolean("heroclock.auditSelectorCode")) return;
        for (String owner : Set.of(
                "net/minecraft/commands/arguments/selector/options/EntitySelectorOptions",
                "net/minecraft/network/chat/Component", "net/minecraft/network/chat/FormattedText",
                "net/minecraft/network/chat/MutableComponent", "net/minecraft/network/chat/contents/LiteralContents")) {
            ClassNode type = new ClassNode();
            try (var input = getClass().getResourceAsStream("/" + owner + ".class")) {
                if (input == null) throw new IllegalStateException("Missing " + owner);
                new ClassReader(input).accept(type, 0);
            }
            System.out.println("AUDIT CLASS " + owner + " extends " + type.superName + " " + type.interfaces);
            for (MethodNode method : type.methods) {
                boolean selected = method.name.contains("getString") || method.name.equals("visit")
                        || method.name.equals("getContents") || method.name.equals("getSiblings");
                if (owner.endsWith("EntitySelectorOptions")) {
                    for (var instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode call && call.name.equals("getString")) selected = true;
                    }
                }
                if (!selected) continue;
                System.out.println("AUDIT METHOD " + method.name + method.desc + " access=" + method.access);
                for (var instruction : method.instructions) {
                    if (instruction.getOpcode() < 0) continue;
                    String detail = "";
                    if (instruction instanceof MethodInsnNode call) detail = call.owner + "." + call.name + call.desc;
                    else if (instruction instanceof FieldInsnNode field) detail = field.owner + "." + field.name + " " + field.desc;
                    else if (instruction instanceof VarInsnNode variable) detail = "local=" + variable.var;
                    else if (instruction instanceof TypeInsnNode typeInsn) detail = typeInsn.desc;
                    else if (instruction instanceof LdcInsnNode constant) detail = String.valueOf(constant.cst);
                    else if (instruction instanceof IntInsnNode integer) detail = "operand=" + integer.operand;
                    else if (instruction instanceof InvokeDynamicInsnNode dynamic) detail = dynamic.name + dynamic.desc + java.util.Arrays.toString(dynamic.bsmArgs);
                    else if (instruction instanceof JumpInsnNode jump) detail = "target=" + method.instructions.indexOf(jump.label);
                    System.out.println("AUDIT " + method.instructions.indexOf(instruction) + " " + instruction.getOpcode() + " " + detail);
                }
            }
        }
    }
}
