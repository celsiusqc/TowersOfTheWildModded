package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ClientboundEntityEventPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundEntityEventPacket> STREAM_CODEC = Packet.codec(
        ClientboundEntityEventPacket::write, ClientboundEntityEventPacket::new
    );
    private final int entityId;
    private final byte eventId;

    public ClientboundEntityEventPacket(Entity pEntity, byte pEventId) {
        this.entityId = pEntity.getId();
        this.eventId = pEventId;
    }

    private ClientboundEntityEventPacket(FriendlyByteBuf p_178843_) {
        this.entityId = p_178843_.readInt();
        this.eventId = p_178843_.readByte();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_132104_) {
        p_132104_.writeInt(this.entityId);
        p_132104_.writeByte(this.eventId);
    }

    @Override
    public PacketType<ClientboundEntityEventPacket> type() {
        return GamePacketTypes.CLIENTBOUND_ENTITY_EVENT;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleEntityEvent(this);
    }

    @Nullable
    public Entity getEntity(Level pLevel) {
        return pLevel.getEntity(this.entityId);
    }

    public byte getEventId() {
        return this.eventId;
    }
}
