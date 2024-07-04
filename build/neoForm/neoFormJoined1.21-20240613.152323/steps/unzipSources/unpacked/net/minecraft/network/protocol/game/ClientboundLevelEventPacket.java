package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundLevelEventPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundLevelEventPacket> STREAM_CODEC = Packet.codec(
        ClientboundLevelEventPacket::write, ClientboundLevelEventPacket::new
    );
    private final int type;
    private final BlockPos pos;
    /**
     * can be a block/item id or other depending on the soundtype
     */
    private final int data;
    /**
     * If true the sound is played across the server
     */
    private final boolean globalEvent;

    public ClientboundLevelEventPacket(int pType, BlockPos pPos, int pData, boolean pGlobalEvent) {
        this.type = pType;
        this.pos = pPos.immutable();
        this.data = pData;
        this.globalEvent = pGlobalEvent;
    }

    private ClientboundLevelEventPacket(FriendlyByteBuf p_178908_) {
        this.type = p_178908_.readInt();
        this.pos = p_178908_.readBlockPos();
        this.data = p_178908_.readInt();
        this.globalEvent = p_178908_.readBoolean();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_132276_) {
        p_132276_.writeInt(this.type);
        p_132276_.writeBlockPos(this.pos);
        p_132276_.writeInt(this.data);
        p_132276_.writeBoolean(this.globalEvent);
    }

    @Override
    public PacketType<ClientboundLevelEventPacket> type() {
        return GamePacketTypes.CLIENTBOUND_LEVEL_EVENT;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleLevelEvent(this);
    }

    public boolean isGlobalEvent() {
        return this.globalEvent;
    }

    public int getType() {
        return this.type;
    }

    public int getData() {
        return this.data;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}
