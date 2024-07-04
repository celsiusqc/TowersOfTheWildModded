package net.minecraft.network.protocol.common;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagNetworkSerialization;

public class ClientboundUpdateTagsPacket implements Packet<ClientCommonPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundUpdateTagsPacket> STREAM_CODEC = Packet.codec(
        ClientboundUpdateTagsPacket::write, ClientboundUpdateTagsPacket::new
    );
    private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags;

    public ClientboundUpdateTagsPacket(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> pTags) {
        this.tags = pTags;
    }

    private ClientboundUpdateTagsPacket(FriendlyByteBuf p_296024_) {
        this.tags = p_296024_.readMap(FriendlyByteBuf::readRegistryKey, TagNetworkSerialization.NetworkPayload::read);
    }

    private void write(FriendlyByteBuf p_295095_) {
        p_295095_.writeMap(this.tags, FriendlyByteBuf::writeResourceKey, (p_294833_, p_294113_) -> p_294113_.write(p_294833_));
    }

    @Override
    public PacketType<ClientboundUpdateTagsPacket> type() {
        return CommonPacketTypes.CLIENTBOUND_UPDATE_TAGS;
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientCommonPacketListener pHandler) {
        pHandler.handleUpdateTags(this);
    }

    public Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> getTags() {
        return this.tags;
    }
}
