package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ClientboundSetEntityMotionPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetEntityMotionPacket> STREAM_CODEC = Packet.codec(
        ClientboundSetEntityMotionPacket::write, ClientboundSetEntityMotionPacket::new
    );
    private final int id;
    private final int xa;
    private final int ya;
    private final int za;

    public ClientboundSetEntityMotionPacket(Entity pEntity) {
        this(pEntity.getId(), pEntity.getDeltaMovement());
    }

    public ClientboundSetEntityMotionPacket(int pId, Vec3 pDeltaMovement) {
        this.id = pId;
        double d0 = 3.9;
        double d1 = Mth.clamp(pDeltaMovement.x, -3.9, 3.9);
        double d2 = Mth.clamp(pDeltaMovement.y, -3.9, 3.9);
        double d3 = Mth.clamp(pDeltaMovement.z, -3.9, 3.9);
        this.xa = (int)(d1 * 8000.0);
        this.ya = (int)(d2 * 8000.0);
        this.za = (int)(d3 * 8000.0);
    }

    private ClientboundSetEntityMotionPacket(FriendlyByteBuf p_179294_) {
        this.id = p_179294_.readVarInt();
        this.xa = p_179294_.readShort();
        this.ya = p_179294_.readShort();
        this.za = p_179294_.readShort();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133194_) {
        p_133194_.writeVarInt(this.id);
        p_133194_.writeShort(this.xa);
        p_133194_.writeShort(this.ya);
        p_133194_.writeShort(this.za);
    }

    @Override
    public PacketType<ClientboundSetEntityMotionPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_ENTITY_MOTION;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetEntityMotion(this);
    }

    public int getId() {
        return this.id;
    }

    public double getXa() {
        return (double)this.xa / 8000.0;
    }

    public double getYa() {
        return (double)this.ya / 8000.0;
    }

    public double getZa() {
        return (double)this.za / 8000.0;
    }
}
