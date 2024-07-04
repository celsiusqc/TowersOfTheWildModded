package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundTagQueryPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundTagQueryPacket> STREAM_CODEC = Packet.codec(
        ClientboundTagQueryPacket::write, ClientboundTagQueryPacket::new
    );
    private final int transactionId;
    @Nullable
    private final CompoundTag tag;

    public ClientboundTagQueryPacket(int pTransactionId, @Nullable CompoundTag pTag) {
        this.transactionId = pTransactionId;
        this.tag = pTag;
    }

    private ClientboundTagQueryPacket(FriendlyByteBuf p_179433_) {
        this.transactionId = p_179433_.readVarInt();
        this.tag = p_179433_.readNbt();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133508_) {
        p_133508_.writeVarInt(this.transactionId);
        p_133508_.writeNbt(this.tag);
    }

    @Override
    public PacketType<ClientboundTagQueryPacket> type() {
        return GamePacketTypes.CLIENTBOUND_TAG_QUERY;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleTagQueryPacket(this);
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    @Nullable
    public CompoundTag getTag() {
        return this.tag;
    }

    @Override
    public boolean isSkippable() {
        return true;
    }
}
