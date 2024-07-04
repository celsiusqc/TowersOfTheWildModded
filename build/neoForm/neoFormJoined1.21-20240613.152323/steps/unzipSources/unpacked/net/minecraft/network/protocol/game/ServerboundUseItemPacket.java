package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.InteractionHand;

public class ServerboundUseItemPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundUseItemPacket> STREAM_CODEC = Packet.codec(
        ServerboundUseItemPacket::write, ServerboundUseItemPacket::new
    );
    private final InteractionHand hand;
    private final int sequence;
    private final float yRot;
    private final float xRot;

    public ServerboundUseItemPacket(InteractionHand pHand, int pSequence, float p_348579_, float p_348520_) {
        this.hand = pHand;
        this.sequence = pSequence;
        this.yRot = p_348579_;
        this.xRot = p_348520_;
    }

    private ServerboundUseItemPacket(FriendlyByteBuf p_179798_) {
        this.hand = p_179798_.readEnum(InteractionHand.class);
        this.sequence = p_179798_.readVarInt();
        this.yRot = p_179798_.readFloat();
        this.xRot = p_179798_.readFloat();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134719_) {
        p_134719_.writeEnum(this.hand);
        p_134719_.writeVarInt(this.sequence);
        p_134719_.writeFloat(this.yRot);
        p_134719_.writeFloat(this.xRot);
    }

    @Override
    public PacketType<ServerboundUseItemPacket> type() {
        return GamePacketTypes.SERVERBOUND_USE_ITEM;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleUseItem(this);
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public int getSequence() {
        return this.sequence;
    }

    public float getYRot() {
        return this.yRot;
    }

    public float getXRot() {
        return this.xRot;
    }
}
