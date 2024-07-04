package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPaddleBoatPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundPaddleBoatPacket> STREAM_CODEC = Packet.codec(
        ServerboundPaddleBoatPacket::write, ServerboundPaddleBoatPacket::new
    );
    private final boolean left;
    private final boolean right;

    public ServerboundPaddleBoatPacket(boolean pLeft, boolean pRight) {
        this.left = pLeft;
        this.right = pRight;
    }

    private ServerboundPaddleBoatPacket(FriendlyByteBuf p_179702_) {
        this.left = p_179702_.readBoolean();
        this.right = p_179702_.readBoolean();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134220_) {
        p_134220_.writeBoolean(this.left);
        p_134220_.writeBoolean(this.right);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handlePaddleBoat(this);
    }

    @Override
    public PacketType<ServerboundPaddleBoatPacket> type() {
        return GamePacketTypes.SERVERBOUND_PADDLE_BOAT;
    }

    public boolean getLeft() {
        return this.left;
    }

    public boolean getRight() {
        return this.right;
    }
}
