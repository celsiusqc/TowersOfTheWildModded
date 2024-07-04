package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundContainerClosePacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundContainerClosePacket> STREAM_CODEC = Packet.codec(
        ServerboundContainerClosePacket::write, ServerboundContainerClosePacket::new
    );
    private final int containerId;

    public ServerboundContainerClosePacket(int pContainerId) {
        this.containerId = pContainerId;
    }

    private ServerboundContainerClosePacket(FriendlyByteBuf p_179584_) {
        this.containerId = p_179584_.readByte();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133978_) {
        p_133978_.writeByte(this.containerId);
    }

    @Override
    public PacketType<ServerboundContainerClosePacket> type() {
        return GamePacketTypes.SERVERBOUND_CONTAINER_CLOSE;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleContainerClose(this);
    }

    public int getContainerId() {
        return this.containerId;
    }
}
