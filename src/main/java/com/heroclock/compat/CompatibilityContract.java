package com.heroclock.compat;

import java.util.List;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public record CompatibilityContract(String mixin, String mod, String target, String parent,
                                    List<Field> fields, List<Method> methods, List<Member> minecraftMembers, boolean exhaustiveMethods) {
    public CompatibilityContract(String mixin, String mod, String target, String parent,
                                 List<Field> fields, List<Method> methods, List<Member> minecraftMembers) {
        this(mixin, mod, target, parent, fields, methods, minecraftMembers, false);
    }
    public record Field(String name, String descriptor, int access) {}
    public record Method(String name, String descriptor, String fingerprint) {}
    public record Member(boolean method, String owner, String name, String descriptor) {}

    public String mismatch(ClassNode candidate, MethodFingerprint.Names names) {
        if (!candidate.name.equals(target.replace('.', '/'))) return "target class changed";
        if (!candidate.superName.equals(parent)) return "superclass changed";
        for (Field field : fields) {
            boolean found = candidate.fields.stream().anyMatch(current -> current.name.equals(field.name)
                    && current.desc.equals(field.descriptor) && current.access == field.access);
            if (!found) return "field contract changed: " + field.name;
        }
        if (exhaustiveMethods && candidate.methods.size() != methods.size()) return "method set changed";
        if (exhaustiveMethods && (candidate.nestHostClass != null || candidate.nestMembers != null && !candidate.nestMembers.isEmpty())) return "nest members changed";
        for (Method expected : methods) {
            MethodNode actual = candidate.methods.stream().filter(current -> current.name.equals(expected.name)
                    && current.desc.equals(expected.descriptor)).findFirst().orElse(null);
            if (actual == null) return "method missing: " + expected.name;
            if (!expected.fingerprint.equals(MethodFingerprint.hash(actual, names))) return "method body changed: " + expected.name;
        }
        return null;
    }
}
