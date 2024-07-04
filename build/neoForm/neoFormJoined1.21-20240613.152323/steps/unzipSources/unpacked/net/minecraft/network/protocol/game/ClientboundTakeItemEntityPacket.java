package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundTakeItemEntityPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundTakeItemEntityPacket> STREAM_CODEC = Packet.codec(
        ClientboundTakeItemEntityPacket::write, ClientboundTakeItemEntityPacket::new
    );
    private final int itemId;
    private final int playerId;
    private final int amount;

    public ClientboundTakeItemEntityPacket(int pItemId, int pPlayerId, int pAmount) {
        this.itemId = pItemId;
        this.playerId = pPlayerId;
        this.amount = pAmount;
    }

    private ClientboundTakeItemEntityPacket(FriendlyByteBuf p_179435_) {
        this.itemId = p_179435_.readVarInt();
        this.playerId = p_179435_.readVarInt();
        this.amount = p_179435_.readVarInt();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133526_) {
        p_133526_.writeVarInt(this.itemId);
        p_133526_.writeVarInt(this.playerId);
        p_133526_.writeVarInt(this.amount);
    }

    @Override
    public PacketType<ClientboundTakeItemEntityPacket> type() {
        return GamePacketTypes.CLIENTBOUND_TAKE_ITEM_ENTITY;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleTakeItemEntity(this);
    }

    public int getItemId() {
        return this.itemId;
    }

    public int getPlayerId() {
        return this.playerId;
    }

    public int getAmount() {
        return this.amount;
    }
}
