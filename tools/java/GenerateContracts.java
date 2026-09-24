import com.google.gson.GsonBuilder;
import com.heroclock.compat.CompatibilityContract;
import com.heroclock.compat.MethodFingerprint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public final class GenerateContracts {
    private static CompatibilityContract contract(String jar, String mixin, String mod, String target,
                                                   Set<String> fields, Set<String> methods) throws Exception {
        ClassNode node = new ClassNode();
        try (ZipFile zip = new ZipFile(jar)) {
            new ClassReader(zip.getInputStream(zip.getEntry(target.replace('.', '/') + ".class"))).accept(node, 0);
        }
        List<CompatibilityContract.Field> fieldContracts = new ArrayList<>();
        for (var field : node.fields) if (fields.contains(field.name)) {
            fieldContracts.add(new CompatibilityContract.Field(field.name, field.desc, field.access));
        }
        if (fieldContracts.size() != fields.size()) throw new IllegalStateException("Missing fields in " + target);
        List<CompatibilityContract.Method> methodContracts = new ArrayList<>();
        Set<CompatibilityContract.Member> anchors = new LinkedHashSet<>();
        Set<String> found = new LinkedHashSet<>();
        for (var method : node.methods) if (methods.contains(method.name)) {
            found.add(method.name);
            String hash = MethodFingerprint.hash(method, (call, owner, name, descriptor) -> {
                if (owner.startsWith("net/minecraft/") && (name.startsWith("m_") || name.startsWith("f_"))) {
                    anchors.add(new CompatibilityContract.Member(call, owner, name, descriptor));
                }
                return name;
            });
            methodContracts.add(new CompatibilityContract.Method(method.name, method.desc, hash));
        }
        if (!found.equals(methods)) throw new IllegalStateException("Missing methods in " + target);
        return new CompatibilityContract(mixin, mod, target, node.superName, fieldContracts, methodContracts, List.copyOf(anchors));
    }

    public static void main(String[] args) throws Exception {
        List<CompatibilityContract> contracts = List.of(
            contract(args[0], "EntityPropertyHandlerMixin", "palladium", "net.threetag.palladium.util.property.EntityPropertyHandler", Set.of(), Set.of("onChanged")),
            contract(args[0], "PowerHandlerMixin", "palladium", "net.threetag.palladium.power.PowerHandler", Set.of("powers"), Set.of("getPowerHolders")),
            contract(args[0], "CommandFunctionMixin", "palladium", "net.threetag.palladium.util.property.CommandFunctionProperty$CommandFunctionParsing", Set.of("commandFunction", "error"), Set.of("getCommandFunction")),
            contract(args[0], "PropertyManagerMixin", "palladium", "net.threetag.palladium.util.property.PropertyManager", Set.of(), Set.of("getPropertyByName", "register", "fromNBT", "fromBuffer", "fromJSON", "values")),
            contract(args[1], "CuriosInventoryMixin", "curios", "top.theillusivec4.curios.common.capability.CurioInventoryCapability$CurioInventoryWrapper", Set.of("curios"), Set.of("getCurios")),
            contract(args[2], "KubeEventContainerMixin", "kubejs", "dev.latvian.mods.kubejs.event.EventHandlerContainer", Set.of("child"), Set.of("<init>", "add", "handle")),
            contract(args[3], "RhinoMapIdsMixin", "rhino", "dev.latvian.mods.rhino.NativeJavaMap", Set.of("map"), Set.of("<init>", "getIds"))
        );
        Files.writeString(Path.of(args[4]), new GsonBuilder().setPrettyPrinting().create().toJson(contracts) + "\n");
    }
}
