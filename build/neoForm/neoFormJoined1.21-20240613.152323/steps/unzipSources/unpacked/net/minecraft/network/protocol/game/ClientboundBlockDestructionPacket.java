package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundBlockDestructionPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundBlockDestructionPacket> STREAM_CODEC = Packet.codec(
        ClientboundBlockDestructionPacket::write, ClientboundBlockDestructionPacket::new
    );
    private final int id;
    private final BlockPos pos;
    private final int progress;

    public ClientboundBlockDestructionPacket(int pId, BlockPos pPos, int pProgress) {
        this.id = pId;
        this.pos = pPos;
        this.progress = pProgress;
    }

    private ClientboundBlockDestructionPacket(FriendlyByteBuf p_178606_) {
        this.id = p_178606_.readVarInt();
        this.pos = p_178606_.readBlockPos();
        this.progress = p_178606_.readUnsignedByte();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_131687_) {
        p_131687_.writeVarInt(this.id);
        p_131687_.writeBlockPos(this.pos);
        p_131687_.writeByte(this.progress);
    }

    @Override
    public PacketType<ClientboundBlockDestructionPacket> type() {
        return GamePacketTypes.CLIENTBOUND_BLOCK_DESTRUCTION;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleBlockDestruction(this);
    }

    public int getId() {
        return this.id;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int getProgress() {
        return this.progress;
    }
}
