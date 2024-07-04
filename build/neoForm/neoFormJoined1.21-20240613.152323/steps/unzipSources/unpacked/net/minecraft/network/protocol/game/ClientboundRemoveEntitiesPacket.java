package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundRemoveEntitiesPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundRemoveEntitiesPacket> STREAM_CODEC = Packet.codec(
        ClientboundRemoveEntitiesPacket::write, ClientboundRemoveEntitiesPacket::new
    );
    private final IntList entityIds;

    public ClientboundRemoveEntitiesPacket(IntList pEntityIds) {
        this.entityIds = new IntArrayList(pEntityIds);
    }

    public ClientboundRemoveEntitiesPacket(int... pEntityIds) {
        this.entityIds = new IntArrayList(pEntityIds);
    }

    private ClientboundRemoveEntitiesPacket(FriendlyByteBuf p_182721_) {
        this.entityIds = p_182721_.readIntIdList();
    }

    private void write(FriendlyByteBuf p_182725_) {
        p_182725_.writeIntIdList(this.entityIds);
    }

    @Override
    public PacketType<ClientboundRemoveEntitiesPacket> type() {
        return GamePacketTypes.CLIENTBOUND_REMOVE_ENTITIES;
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleRemoveEntities(this);
    }

    public IntList getEntityIds() {
        return this.entityIds;
    }
}
