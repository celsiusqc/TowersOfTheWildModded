package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundKeepAlivePacket implements Packet<ServerCommonPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundKeepAlivePacket> STREAM_CODEC = Packet.codec(
        ServerboundKeepAlivePacket::write, ServerboundKeepAlivePacket::new
    );
    private final long id;

    public ServerboundKeepAlivePacket(long pId) {
        this.id = pId;
    }

    private ServerboundKeepAlivePacket(FriendlyByteBuf p_294566_) {
        this.id = p_294566_.readLong();
    }

    private void write(FriendlyByteBuf p_295504_) {
        p_295504_.writeLong(this.id);
    }

    @Override
    public PacketType<ServerboundKeepAlivePacket> type() {
        return CommonPacketTypes.SERVERBOUND_KEEP_ALIVE;
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ServerCommonPacketListener pHandler) {
        pHandler.handleKeepAlive(this);
    }

    public long getId() {
        return this.id;
    }
}
