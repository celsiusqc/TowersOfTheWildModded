package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ClientboundRotateHeadPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundRotateHeadPacket> STREAM_CODEC = Packet.codec(
        ClientboundRotateHeadPacket::write, ClientboundRotateHeadPacket::new
    );
    private final int entityId;
    private final byte yHeadRot;

    public ClientboundRotateHeadPacket(Entity pEntity, byte pYHeadRot) {
        this.entityId = pEntity.getId();
        this.yHeadRot = pYHeadRot;
    }

    private ClientboundRotateHeadPacket(FriendlyByteBuf p_179193_) {
        this.entityId = p_179193_.readVarInt();
        this.yHeadRot = p_179193_.readByte();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_132979_) {
        p_132979_.writeVarInt(this.entityId);
        p_132979_.writeByte(this.yHeadRot);
    }

    @Override
    public PacketType<ClientboundRotateHeadPacket> type() {
        return GamePacketTypes.CLIENTBOUND_ROTATE_HEAD;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleRotateMob(this);
    }

    public Entity getEntity(Level pLevel) {
        return pLevel.getEntity(this.entityId);
    }

    public byte getYHeadRot() {
        return this.yHeadRot;
    }
}
