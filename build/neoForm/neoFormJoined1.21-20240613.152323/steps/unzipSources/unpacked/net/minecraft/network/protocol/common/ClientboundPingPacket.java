package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundPingPacket implements Packet<ClientCommonPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundPingPacket> STREAM_CODEC = Packet.codec(
        ClientboundPingPacket::write, ClientboundPingPacket::new
    );
    private final int id;

    public ClientboundPingPacket(int pId) {
        this.id = pId;
    }

    private ClientboundPingPacket(FriendlyByteBuf p_294565_) {
        this.id = p_294565_.readInt();
    }

    private void write(FriendlyByteBuf p_295570_) {
        p_295570_.writeInt(this.id);
    }

    @Override
    public PacketType<ClientboundPingPacket> type() {
        return CommonPacketTypes.CLIENTBOUND_PING;
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientCommonPacketListener pHandler) {
        pHandler.handlePing(this);
    }

    public int getId() {
        return this.id;
    }
}
