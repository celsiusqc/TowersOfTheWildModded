package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.InteractionHand;

public class ServerboundSwingPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundSwingPacket> STREAM_CODEC = Packet.codec(
        ServerboundSwingPacket::write, ServerboundSwingPacket::new
    );
    private final InteractionHand hand;

    public ServerboundSwingPacket(InteractionHand pHand) {
        this.hand = pHand;
    }

    private ServerboundSwingPacket(FriendlyByteBuf p_179792_) {
        this.hand = p_179792_.readEnum(InteractionHand.class);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134676_) {
        p_134676_.writeEnum(this.hand);
    }

    @Override
    public PacketType<ServerboundSwingPacket> type() {
        return GamePacketTypes.SERVERBOUND_SWING;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleAnimate(this);
    }

    public InteractionHand getHand() {
        return this.hand;
    }
}
