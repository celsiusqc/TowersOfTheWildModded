package net.minecraft.server.packs;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;

public class CompositePackResources implements PackResources {
    private final PackResources primaryPackResources;
    private final List<PackResources> packResourcesStack;

    public CompositePackResources(PackResources pPrimaryPackResources, List<PackResources> pPackResourcesStack) {
        this.primaryPackResources = pPrimaryPackResources;
        List<PackResources> list = new ArrayList<>(pPackResourcesStack.size() + 1);
        list.addAll(Lists.reverse(pPackResourcesStack));
        list.add(pPrimaryPackResources);
        this.packResourcesStack = List.copyOf(list);
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... pElements) {
        return this.primaryPackResources.getRootResource(pElements);
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType pPackType, ResourceLocation pLocation) {
        for (PackResources packresources : this.packResourcesStack) {
            IoSupplier<InputStream> iosupplier = packresources.getResource(pPackType, pLocation);
            if (iosupplier != null) {
                return iosupplier;
            }
        }

        return null;
    }

    @Override
    public void listResources(PackType pPackType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
        Map<ResourceLocation, IoSupplier<InputStream>> map = new HashMap<>();

        for (PackResources packresources : this.packResourcesStack) {
            packresources.listResources(pPackType, pNamespace, pPath, map::putIfAbsent);
        }

        map.forEach(pResourceOutput);
    }

    @Override
    public Set<String> getNamespaces(PackType pType) {
        Set<String> set = new HashSet<>();

        for (PackResources packresources : this.packResourcesStack) {
            set.addAll(packresources.getNamespaces(pType));
        }

        return set;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> pDeserializer) throws IOException {
        return this.primaryPackResources.getMetadataSection(pDeserializer);
    }

    @Override
    public PackLocationInfo location() {
        return this.primaryPackResources.location();
    }

    @Override
    public void close() {
        this.packResourcesStack.forEach(PackResources::close);
    }
}
