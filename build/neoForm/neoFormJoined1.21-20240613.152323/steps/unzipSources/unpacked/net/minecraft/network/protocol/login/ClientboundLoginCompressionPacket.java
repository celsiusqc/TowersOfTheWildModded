package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundLoginCompressionPacket implements Packet<ClientLoginPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundLoginCompressionPacket> STREAM_CODEC = Packet.codec(
        ClientboundLoginCompressionPacket::write, ClientboundLoginCompressionPacket::new
    );
    private final int compressionThreshold;

    public ClientboundLoginCompressionPacket(int pCompressionThreshold) {
        this.compressionThreshold = pCompressionThreshold;
    }

    private ClientboundLoginCompressionPacket(FriendlyByteBuf p_179818_) {
        this.compressionThreshold = p_179818_.readVarInt();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134808_) {
        p_134808_.writeVarInt(this.compressionThreshold);
    }

    @Override
    public PacketType<ClientboundLoginCompressionPacket> type() {
        return LoginPacketTypes.CLIENTBOUND_LOGIN_COMPRESSION;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientLoginPacketListener pHandler) {
        pHandler.handleCompression(this);
    }

    public int getCompressionThreshold() {
        return this.compressionThreshold;
    }
}
