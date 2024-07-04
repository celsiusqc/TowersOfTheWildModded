package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;

public class ClientboundSetEntityLinkPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetEntityLinkPacket> STREAM_CODEC = Packet.codec(
        ClientboundSetEntityLinkPacket::write, ClientboundSetEntityLinkPacket::new
    );
    private final int sourceId;
    /**
     * The entity that is holding the leash, or -1 to clear the holder.
     */
    private final int destId;

    /**
     * @param pDestination The entity to link to or {@code null} to break any existing
     *                     link.
     */
    public ClientboundSetEntityLinkPacket(Entity pSource, @Nullable Entity pDestination) {
        this.sourceId = pSource.getId();
        this.destId = pDestination != null ? pDestination.getId() : 0;
    }

    private ClientboundSetEntityLinkPacket(FriendlyByteBuf p_179292_) {
        this.sourceId = p_179292_.readInt();
        this.destId = p_179292_.readInt();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133174_) {
        p_133174_.writeInt(this.sourceId);
        p_133174_.writeInt(this.destId);
    }

    @Override
    public PacketType<ClientboundSetEntityLinkPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_ENTITY_LINK;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleEntityLinkPacket(this);
    }

    public int getSourceId() {
        return this.sourceId;
    }

    public int getDestId() {
        return this.destId;
    }
}
