package com.heroclock.scripting;

import com.heroclock.HeroClock;
import com.heroclock.api.HeroClockAPI;
import com.heroclock.api.HeroFunctionAPI;
import com.heroclock.api.HeroIntegrationAPI;
import com.heroclock.api.HeroScriptAPI;
import com.heroclock.api.HeroWorkAPI;
import com.heroclock.runtime.ServerScripts;
import dev.latvian.mods.rhino.Context;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.MinecraftServer;

public final class RhinoServerScripts implements ServerScripts.Engine {
    private final MinecraftServer server;
    private final List<RhinoRuntimeScope> scopes = new ArrayList<>();
    public RhinoServerScripts(MinecraftServer server) { this.server = server; }

    @Override public HeroScriptAPI.ScriptLoadStatus load(Path directory) {
        List<Path> files;
        try {
            Files.createDirectories(directory);
            try (var paths = Files.walk(directory)) {
                files = paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                        && path.toString().endsWith(".js")).limit(129).sorted(Comparator.comparing(path ->
                                directory.relativize(path).toString().replace('\\', '/'))).toList();
            }
            if (files.size() > 128) throw new IOException("HeroClock supports at most 128 standalone script files");
        } catch (IOException failure) {
            HeroClock.LOGGER.error("HeroClock could not enumerate server scripts in {}", directory, failure);
            return new HeroScriptAPI.ScriptLoadStatus("rhino", 0, 1);
        }
        if (files.isEmpty()) return new HeroScriptAPI.ScriptLoadStatus("rhino", 0, 0);
        Context context = Context.enter();
        var root = context.initStandardObjects();
        context.addToScope(root, "Java", new ScriptJavaClasses(context, root));
        context.addToScope(root, "HeroClock", HeroClockAPI.class);
        context.addToScope(root, "HeroScript", HeroScriptAPI.class);
        context.addToScope(root, "HeroWork", HeroWorkAPI.class);
        context.addToScope(root, "HeroFunctions", HeroFunctionAPI.class);
        context.addToScope(root, "HeroIntegration", HeroIntegrationAPI.class);
        context.addToScope(root, "server", server);
        int loaded = 0, failed = 0;
        for (Path file : files) {
            RhinoRuntimeScope binding = null;
            try {
                String source = read(file);
                String name = "heroclock/" + directory.relativize(file).toString().replace('\\', '/');
                var scope = context.newObject(root);
                scope.setParentScope(root);
                binding = new RhinoRuntimeScope(context, scope);
                context.addToScope(scope, "HeroRuntime", binding);
                context.addToScope(scope, "console", new ScriptConsole(name));
                context.evaluateString(scope, source, name, 1, null);
                scopes.add(binding);
                loaded++;
            } catch (IOException | RuntimeException failure) {
                if (binding != null) binding.invalidate();
                failed++;
                HeroClock.LOGGER.error("HeroClock server script failed: {}", file, failure);
            } catch (Error failure) {
                if (binding != null) binding.invalidate();
                throw failure;
            }
        }
        return new HeroScriptAPI.ScriptLoadStatus("rhino", loaded, failed);
    }

    private static String read(Path file) throws IOException {
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(1_048_577);
            if (bytes.length > 1_048_576) throw new IOException("Standalone script exceeds 1 MiB");
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        }
    }

    @Override public void close() {
        for (var scope : scopes) scope.invalidate();
        scopes.clear();
    }
}
