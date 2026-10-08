package com.heroclock.resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.resource.ResourcePackLoader;

public final class CompanionResourceChecks {
    private CompanionResourceChecks() {}

    public static void verify(GameTestHelper helper) throws Exception {
        helper.assertTrue(java.util.Arrays.stream(ResourcePackLoader.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().contains("resolveCompanionPowers")), "Resource hook was not applied");
        try (var actual = ResourcePackLoader.createPackForMod(ModList.get().getModFileById("heroclock"))) {
            for (String path : OmniPowerResources.CONTRACTS.keySet()) {
                helper.assertTrue(actual.getRootResource(path) != null, "Standalone power disappeared: " + path);
            }
        }

        Path directory = Files.createTempDirectory("heroclock-resource-test-");
        try {
            Path heroRoot = directory.resolve("hero");
            Path omniRoot = directory.resolve("omni");
            Path stockRoot = directory.resolve("stock");
            Path customRoot = directory.resolve("custom");
            Map<String, String> contracts = new HashMap<>();
            String digest = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest("omni".getBytes(StandardCharsets.UTF_8)));
            for (String path : OmniPowerResources.CONTRACTS.keySet()) {
                write(heroRoot, path, "hero");
                write(omniRoot, path, "omni");
                write(stockRoot, path, "stock");
                write(customRoot, path, "custom");
                write(heroRoot, path.replaceFirst("data/", "assets/"), "asset");
                contracts.put(path, digest);
            }
            String tag = "data/minecraft/tags/functions/load.json";
            write(heroRoot, tag, "{\"values\":[\"heroclock:load\"]}");
            write(omniRoot, tag, "{\"values\":[\"omnioptimizer:load\"]}");
            write(heroRoot, "pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"test\"}}");
            write(heroRoot, "data/heroclock/functions/unrelated.mcfunction", "say unchanged");

            var original = new PathPackResources("hero", true, heroRoot);
            var filtered = OmniPowerResources.yieldVerified(original, omniRoot::resolve, contracts);
            var omni = new PathPackResources("omni", true, omniRoot);
            var stock = new PathPackResources("stock", true, stockRoot);
            var custom = new PathPackResources("custom", false, customRoot);
            helper.assertTrue(filtered != original && filtered.packId().equals(original.packId())
                    && filtered.isBuiltin() == original.isBuiltin(), "Filtered pack identity changed");
            helper.assertTrue(filtered.getNamespaces(PackType.SERVER_DATA).equals(original.getNamespaces(PackType.SERVER_DATA)),
                    "Resource namespaces changed");
            helper.assertTrue(filtered.getRootResource("pack.mcmeta") != null, "Pack metadata disappeared");
            helper.assertTrue(filtered.getResource(PackType.SERVER_DATA,
                    new ResourceLocation("heroclock", "functions/unrelated.mcfunction")) != null, "Unrelated resource disappeared");
            for (String path : contracts.keySet()) {
                ResourceLocation id = id(path);
                helper.assertTrue(filtered.getResource(PackType.SERVER_DATA, id) == null, "Audited conflict was not yielded");
                helper.assertTrue(filtered.getRootResource(path) == null && filtered.getRootResource(path.split("/")) == null,
                        "Root resource lookup bypassed filtering");
                helper.assertTrue(filtered.getResource(PackType.CLIENT_RESOURCES, id) != null, "Client resource was filtered");
            }
            List<ResourceLocation> listed = new ArrayList<>();
            filtered.listResources(PackType.SERVER_DATA, "alienevo_aliens", "palladium/powers", (id, input) -> listed.add(id));
            helper.assertTrue(listed.isEmpty(), "Resource discovery bypassed filtering");

            checkSelection(helper, List.of(stock, original), contracts, "hero");
            checkSelection(helper, List.of(stock, omni, filtered), contracts, "omni");
            checkSelection(helper, List.of(stock, filtered, omni), contracts, "omni");
            checkSelection(helper, List.of(stock, omni, filtered, custom), contracts, "custom");
            try (var manager = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(stock, omni, filtered))) {
                helper.assertTrue(manager.getResourceStack(id(tag)).size() == 2, "Mergeable load tag was filtered");
            }

            String changed = contracts.keySet().iterator().next();
            write(omniRoot, changed, "changed upstream");
            var partial = OmniPowerResources.yieldVerified(original, omniRoot::resolve, contracts);
            helper.assertTrue(partial.getResource(PackType.SERVER_DATA, id(changed)) != null,
                    "Changed companion definition displaced the fallback");
            for (String path : contracts.keySet()) {
                if (!path.equals(changed)) helper.assertTrue(partial.getResource(PackType.SERVER_DATA, id(path)) == null,
                        "One changed resource disabled unchanged conflicts");
            }
            var absent = OmniPowerResources.yieldVerified(original, directory.resolve("missing")::resolve, contracts);
            helper.assertTrue(absent == original, "Missing companion changed the fallback pack");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void checkSelection(GameTestHelper helper, List<PathPackResources> packs,
                                       Map<String, String> contracts, String expected) throws IOException {
        try (var manager = new MultiPackResourceManager(PackType.SERVER_DATA, List.copyOf(packs))) {
            var listed = manager.listResources("palladium/powers", location -> location.getPath().endsWith(".json"));
            for (String path : contracts.keySet()) {
                ResourceLocation id = id(path);
                try (var input = manager.getResource(id).orElseThrow().open()) {
                    helper.assertTrue(new String(input.readAllBytes(), StandardCharsets.UTF_8).equals(expected),
                            "Direct resource priority changed: " + id);
                }
                try (var input = listed.get(id).open()) {
                    helper.assertTrue(new String(input.readAllBytes(), StandardCharsets.UTF_8).equals(expected),
                            "Discovered resource priority changed: " + id);
                }
            }
        }
    }

    private static ResourceLocation id(String path) {
        String[] parts = path.split("/", 3);
        return new ResourceLocation(parts[1], parts[2]);
    }

    private static void write(Path root, String path, String content) throws IOException {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
