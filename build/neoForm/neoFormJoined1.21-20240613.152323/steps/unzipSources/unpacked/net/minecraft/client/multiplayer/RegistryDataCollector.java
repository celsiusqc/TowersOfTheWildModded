package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagNetworkSerialization;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RegistryDataCollector {
    @Nullable
    private RegistryDataCollector.ContentsCollector contentsCollector;
    @Nullable
    private TagCollector tagCollector;

    public void appendContents(ResourceKey<? extends Registry<?>> pRegistryKey, List<RegistrySynchronization.PackedRegistryEntry> pRegistryEntries) {
        if (this.contentsCollector == null) {
            this.contentsCollector = new RegistryDataCollector.ContentsCollector();
        }

        this.contentsCollector.append(pRegistryKey, pRegistryEntries);
    }

    public void appendTags(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> pTags) {
        if (this.tagCollector == null) {
            this.tagCollector = new TagCollector();
        }

        pTags.forEach(this.tagCollector::append);
    }

    public RegistryAccess.Frozen collectGameRegistries(ResourceProvider pResourceProvider, RegistryAccess pRegistryAccess, boolean pIsMemoryConnection) {
        LayeredRegistryAccess<ClientRegistryLayer> layeredregistryaccess = ClientRegistryLayer.createRegistryAccess();
        RegistryAccess registryaccess;
        if (this.contentsCollector != null) {
            RegistryAccess.Frozen registryaccess$frozen = layeredregistryaccess.getAccessForLoading(ClientRegistryLayer.REMOTE);
            RegistryAccess.Frozen registryaccess$frozen1 = this.contentsCollector.loadRegistries(pResourceProvider, registryaccess$frozen).freeze();
            registryaccess = layeredregistryaccess.replaceFrom(ClientRegistryLayer.REMOTE, registryaccess$frozen1).compositeAccess();
        } else {
            registryaccess = pRegistryAccess;
        }

        if (this.tagCollector != null) {
            this.tagCollector.updateTags(registryaccess, pIsMemoryConnection);
        }

        return registryaccess.freeze();
    }

    @OnlyIn(Dist.CLIENT)
    static class ContentsCollector {
        private final Map<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> elements = new HashMap<>();

        public void append(ResourceKey<? extends Registry<?>> pRegistryKey, List<RegistrySynchronization.PackedRegistryEntry> pEntries) {
            this.elements.computeIfAbsent(pRegistryKey, p_321745_ -> new ArrayList<>()).addAll(pEntries);
        }

        public RegistryAccess loadRegistries(ResourceProvider pResourceProvider, RegistryAccess pRegistryAccess) {
            return RegistryDataLoader.load(this.elements, pResourceProvider, pRegistryAccess, RegistryDataLoader.SYNCHRONIZED_REGISTRIES);
        }
    }
}
