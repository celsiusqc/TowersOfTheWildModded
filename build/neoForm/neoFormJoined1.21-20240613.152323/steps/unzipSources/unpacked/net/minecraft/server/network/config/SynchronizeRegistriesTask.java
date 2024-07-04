package net.minecraft.server.network.config;

import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.tags.TagNetworkSerialization;

public class SynchronizeRegistriesTask implements ConfigurationTask {
    public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("synchronize_registries");
    private final List<KnownPack> requestedPacks;
    private final LayeredRegistryAccess<RegistryLayer> registries;

    public SynchronizeRegistriesTask(List<KnownPack> pRequestedPacks, LayeredRegistryAccess<RegistryLayer> pRegistries) {
        this.requestedPacks = pRequestedPacks;
        this.registries = pRegistries;
    }

    @Override
    public void start(Consumer<Packet<?>> pTask) {
        pTask.accept(new ClientboundSelectKnownPacks(this.requestedPacks));
    }

    private void sendRegistries(Consumer<Packet<?>> pPacketSender, Set<KnownPack> pPacks) {
        DynamicOps<Tag> dynamicops = this.registries.compositeAccess().createSerializationContext(NbtOps.INSTANCE);
        RegistrySynchronization.packRegistries(
            dynamicops,
            this.registries.getAccessFrom(RegistryLayer.WORLDGEN),
            pPacks,
            (p_326010_, p_326361_) -> pPacketSender.accept(new ClientboundRegistryDataPacket(p_326010_, p_326361_))
        );
        pPacketSender.accept(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries)));
    }

    public void handleResponse(List<KnownPack> pPacks, Consumer<Packet<?>> pPacketSender) {
        // Neo: instead of using either all available KnownPacks or none, allow partial fallback to normal syncing
        Set<KnownPack> requested = new java.util.HashSet<>(this.requestedPacks);
        requested.retainAll(pPacks);
        this.sendRegistries(pPacketSender, requested);
    }

    @Override
    public ConfigurationTask.Type type() {
        return TYPE;
    }
}
