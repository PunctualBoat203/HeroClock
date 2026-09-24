package com.heroclock.compat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityContractTest {
    private ClassNode target() {
        ClassNode node = new ClassNode();
        node.name = "example/Target";
        node.superName = "java/lang/Object";
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "state", "I", null, null));
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "read", "()I", null, null);
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        node.methods.add(method);
        return node;
    }

    private CompatibilityContract contract(ClassNode node) {
        return new CompatibilityContract("TestMixin", "example", "example.Target", "java/lang/Object",
                List.of(new CompatibilityContract.Field("state", "I", Opcodes.ACC_PRIVATE)),
                List.of(new CompatibilityContract.Method("read", "()I", MethodFingerprint.hash(node.methods.get(0), MethodFingerprint.IDENTITY))), List.of());
    }

    @Test void unchangedTargetSurvivesUnrelatedChanges() {
        ClassNode node = target();
        var expected = contract(node);
        node.version = Opcodes.V17;
        node.sourceFile = "Renamed.java";
        node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "unrelated", "()V", null, null));
        assertNull(expected.mismatch(node, MethodFingerprint.IDENTITY));
    }

    @Test void executableChangesDisableOnlyTheirContract() {
        ClassNode node = target();
        var expected = contract(node);
        node.methods.get(0).instructions.set(node.methods.get(0).instructions.getFirst(), new InsnNode(Opcodes.ICONST_2));
        assertEquals("method body changed: read", expected.mismatch(node, MethodFingerprint.IDENTITY));
    }

    @Test void missingMethodsAndChangedFieldsAreRejected() {
        ClassNode node = target();
        var expected = contract(node);
        node.methods.clear();
        assertEquals("method missing: read", expected.mismatch(node, MethodFingerprint.IDENTITY));
        node.fields.get(0).desc = "J";
        assertEquals("field contract changed: state", expected.mismatch(node, MethodFingerprint.IDENTITY));
    }

    @Test void debugInformationDoesNotDisableCompatibleCode() {
        ClassNode node = target();
        var expected = contract(node);
        LabelNode label = new LabelNode();
        node.methods.get(0).instructions.insert(label);
        node.methods.get(0).instructions.insert(label, new LineNumberNode(999, label));
        assertNull(expected.mismatch(node, MethodFingerprint.IDENTITY));
    }

    @Test void minecraftDevelopmentNamesNormalizeWithoutIgnoringMemberIdentity() {
        MethodNode production = new MethodNode(Opcodes.ACC_PUBLIC, "clock", "()J", null, null);
        production.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/world/level/Level", "m_46467_", "()J", false));
        MethodNode development = new MethodNode(Opcodes.ACC_PUBLIC, "clock", "()J", null, null);
        development.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/world/level/Level", "getGameTime", "()J", false));
        String expected = MethodFingerprint.hash(production, MethodFingerprint.IDENTITY);
        assertNotEquals(expected, MethodFingerprint.hash(development, MethodFingerprint.IDENTITY));
        assertEquals(expected, MethodFingerprint.hash(development,
                (method, owner, name, descriptor) -> name.equals("getGameTime") ? "m_46467_" : name));
    }
    @Test void exhaustiveContractRejectsNewReadersAndNestmates() {
        ClassNode node = target();
        var partial = contract(node);
        var complete = new CompatibilityContract(partial.mixin(), partial.mod(), partial.target(), partial.parent(),
                partial.fields(), partial.methods(), partial.minecraftMembers(), true);
        assertNull(complete.mismatch(node, MethodFingerprint.IDENTITY));
        node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "newReader", "()V", null, null));
        assertEquals("method set changed", complete.mismatch(node, MethodFingerprint.IDENTITY));
        node.methods.remove(1);
        node.nestHostClass = "example/NewHost";
        assertEquals("nest members changed", complete.mismatch(node, MethodFingerprint.IDENTITY));
        node.nestHostClass = null;
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "heroclock$resolvedCache", "Ljava/lang/Object;", null, null));
        assertEquals("reserved field collision", complete.mismatch(node, MethodFingerprint.IDENTITY));
    }
}
