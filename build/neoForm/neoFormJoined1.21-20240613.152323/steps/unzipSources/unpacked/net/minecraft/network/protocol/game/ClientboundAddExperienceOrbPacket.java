package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;

public class ClientboundAddExperienceOrbPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundAddExperienceOrbPacket> STREAM_CODEC = Packet.codec(
        ClientboundAddExperienceOrbPacket::write, ClientboundAddExperienceOrbPacket::new
    );
    private final int id;
    private final double x;
    private final double y;
    private final double z;
    private final int value;

    public ClientboundAddExperienceOrbPacket(ExperienceOrb pOrbEntity, ServerEntity p_352182_) {
        this.id = pOrbEntity.getId();
        Vec3 vec3 = p_352182_.getPositionBase();
        this.x = vec3.x();
        this.y = vec3.y();
        this.z = vec3.z();
        this.value = pOrbEntity.getValue();
    }

    private ClientboundAddExperienceOrbPacket(FriendlyByteBuf p_178564_) {
        this.id = p_178564_.readVarInt();
        this.x = p_178564_.readDouble();
        this.y = p_178564_.readDouble();
        this.z = p_178564_.readDouble();
        this.value = p_178564_.readShort();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_131526_) {
        p_131526_.writeVarInt(this.id);
        p_131526_.writeDouble(this.x);
        p_131526_.writeDouble(this.y);
        p_131526_.writeDouble(this.z);
        p_131526_.writeShort(this.value);
    }

    @Override
    public PacketType<ClientboundAddExperienceOrbPacket> type() {
        return GamePacketTypes.CLIENTBOUND_ADD_EXPERIENCE_ORB;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleAddExperienceOrb(this);
    }

    public int getId() {
        return this.id;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public int getValue() {
        return this.value;
    }
}
