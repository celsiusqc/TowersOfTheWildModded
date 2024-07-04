package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ClientboundPlayerLookAtPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundPlayerLookAtPacket> STREAM_CODEC = Packet.codec(
        ClientboundPlayerLookAtPacket::write, ClientboundPlayerLookAtPacket::new
    );
    private final double x;
    private final double y;
    private final double z;
    private final int entity;
    private final EntityAnchorArgument.Anchor fromAnchor;
    private final EntityAnchorArgument.Anchor toAnchor;
    private final boolean atEntity;

    public ClientboundPlayerLookAtPacket(EntityAnchorArgument.Anchor pFromAnchor, double pX, double pY, double pZ) {
        this.fromAnchor = pFromAnchor;
        this.x = pX;
        this.y = pY;
        this.z = pZ;
        this.entity = 0;
        this.atEntity = false;
        this.toAnchor = null;
    }

    public ClientboundPlayerLookAtPacket(EntityAnchorArgument.Anchor pFromAnchor, Entity pEntity, EntityAnchorArgument.Anchor pToAnchor) {
        this.fromAnchor = pFromAnchor;
        this.entity = pEntity.getId();
        this.toAnchor = pToAnchor;
        Vec3 vec3 = pToAnchor.apply(pEntity);
        this.x = vec3.x;
        this.y = vec3.y;
        this.z = vec3.z;
        this.atEntity = true;
    }

    private ClientboundPlayerLookAtPacket(FriendlyByteBuf p_179146_) {
        this.fromAnchor = p_179146_.readEnum(EntityAnchorArgument.Anchor.class);
        this.x = p_179146_.readDouble();
        this.y = p_179146_.readDouble();
        this.z = p_179146_.readDouble();
        this.atEntity = p_179146_.readBoolean();
        if (this.atEntity) {
            this.entity = p_179146_.readVarInt();
            this.toAnchor = p_179146_.readEnum(EntityAnchorArgument.Anchor.class);
        } else {
            this.entity = 0;
            this.toAnchor = null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_132795_) {
        p_132795_.writeEnum(this.fromAnchor);
        p_132795_.writeDouble(this.x);
        p_132795_.writeDouble(this.y);
        p_132795_.writeDouble(this.z);
        p_132795_.writeBoolean(this.atEntity);
        if (this.atEntity) {
            p_132795_.writeVarInt(this.entity);
            p_132795_.writeEnum(this.toAnchor);
        }
    }

    @Override
    public PacketType<ClientboundPlayerLookAtPacket> type() {
        return GamePacketTypes.CLIENTBOUND_PLAYER_LOOK_AT;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleLookAt(this);
    }

    public EntityAnchorArgument.Anchor getFromAnchor() {
        return this.fromAnchor;
    }

    @Nullable
    public Vec3 getPosition(Level pLevel) {
        if (this.atEntity) {
            Entity entity = pLevel.getEntity(this.entity);
            return entity == null ? new Vec3(this.x, this.y, this.z) : this.toAnchor.apply(entity);
        } else {
            return new Vec3(this.x, this.y, this.z);
        }
    }
}
