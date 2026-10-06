package com.heroclock.runtime;

import com.heroclock.HeroClock;
import com.heroclock.api.HeroScriptAPI.ScriptLoadStatus;
import com.heroclock.compat.CompatibilityGate;
import com.heroclock.scripting.RhinoServerScripts;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod.EventBusSubscriber(modid = HeroClock.MOD_ID)
public final class ServerScripts {
    public interface Engine extends AutoCloseable {
        ScriptLoadStatus load(Path directory);
        @Override void close();
    }
    private record Loaded(Engine engine, ScriptLoadStatus status) {}
    private static final Map<MinecraftServer, Loaded> SERVERS = new IdentityHashMap<>();
    private ServerScripts() {}

    public static ScriptLoadStatus reload(MinecraftServer server) {
        return reload(server, FMLPaths.CONFIGDIR.get().resolve("heroclock/server_scripts"));
    }

    public static ScriptLoadStatus reload(MinecraftServer server, Path directory) {
        thread(server);
        close(server);
        if (!ModList.get().isLoaded("rhino")) return save(server, null, new ScriptLoadStatus("rhino_missing", 0, 0));
        if (!CompatibilityGate.allows("RhinoStandaloneContext", "dev.latvian.mods.rhino.Context")
                || !CompatibilityGate.allows("RhinoRuntimeWrapping", "dev.latvian.mods.rhino.WrapFactory"))
            return save(server, null, new ScriptLoadStatus("incompatible", 0, 0));
        Engine engine = new RhinoServerScripts(server);
        try { return save(server, engine, engine.load(directory)); }
        catch (RuntimeException | Error failure) { engine.close(); throw failure; }
    }

    private static ScriptLoadStatus save(MinecraftServer server, Engine engine, ScriptLoadStatus status) {
        SERVERS.put(server, new Loaded(engine, status));
        return status;
    }

    public static ScriptLoadStatus status(MinecraftServer server) {
        thread(server);
        Loaded loaded = SERVERS.get(server);
        return loaded == null ? new ScriptLoadStatus("not_loaded", 0, 0) : loaded.status;
    }

    public static void close(MinecraftServer server) {
        thread(server);
        Loaded loaded = SERVERS.remove(server);
        if (loaded != null && loaded.engine != null) loaded.engine.close();
    }

    private static void thread(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Reload HeroClock scripts on the server thread");
    }

    @SubscribeEvent public static void start(ServerStartedEvent event) { reload(event.getServer()); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { close(event.getServer()); }
}
