package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetCarriedItemPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetCarriedItemPacket> STREAM_CODEC = Packet.codec(
        ClientboundSetCarriedItemPacket::write, ClientboundSetCarriedItemPacket::new
    );
    private final int slot;

    public ClientboundSetCarriedItemPacket(int pSlot) {
        this.slot = pSlot;
    }

    private ClientboundSetCarriedItemPacket(FriendlyByteBuf p_179280_) {
        this.slot = p_179280_.readByte();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133081_) {
        p_133081_.writeByte(this.slot);
    }

    @Override
    public PacketType<ClientboundSetCarriedItemPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_CARRIED_ITEM;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetCarriedItem(this);
    }

    public int getSlot() {
        return this.slot;
    }
}
