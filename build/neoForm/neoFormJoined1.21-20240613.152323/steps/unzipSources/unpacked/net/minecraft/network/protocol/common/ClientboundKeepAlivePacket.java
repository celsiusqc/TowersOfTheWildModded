package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundKeepAlivePacket implements Packet<ClientCommonPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundKeepAlivePacket> STREAM_CODEC = Packet.codec(
        ClientboundKeepAlivePacket::write, ClientboundKeepAlivePacket::new
    );
    private final long id;

    public ClientboundKeepAlivePacket(long pId) {
        this.id = pId;
    }

    private ClientboundKeepAlivePacket(FriendlyByteBuf p_296088_) {
        this.id = p_296088_.readLong();
    }

    private void write(FriendlyByteBuf p_295294_) {
        p_295294_.writeLong(this.id);
    }

    @Override
    public PacketType<ClientboundKeepAlivePacket> type() {
        return CommonPacketTypes.CLIENTBOUND_KEEP_ALIVE;
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientCommonPacketListener pHandler) {
        pHandler.handleKeepAlive(this);
    }

    public long getId() {
        return this.id;
    }
}
