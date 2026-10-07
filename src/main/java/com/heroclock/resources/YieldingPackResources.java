package com.heroclock.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.resource.PathPackResources;

final class YieldingPackResources extends PathPackResources {
    private final PathPackResources original;
    private final Set<String> yieldedPaths;

    YieldingPackResources(PathPackResources original, Set<String> yieldedPaths) {
        super(original.packId(), original.isBuiltin(), original.getSource());
        this.original = original;
        this.yieldedPaths = Set.copyOf(yieldedPaths);
    }

    private boolean yields(PackType type, ResourceLocation id) {
        return type == PackType.SERVER_DATA
                && yieldedPaths.contains("data/" + id.getNamespace() + "/" + id.getPath());
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        return yieldedPaths.contains(String.join("/", paths)) ? null : original.getRootResource(paths);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        return yields(type, id) ? null : original.getResource(type, id);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        original.listResources(type, namespace, path, (id, resource) -> {
            if (!yields(type, id)) output.accept(id, resource);
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return original.getNamespaces(type);
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        return original.getMetadataSection(serializer);
    }

    @Override
    public void close() {
        original.close();
    }
}
