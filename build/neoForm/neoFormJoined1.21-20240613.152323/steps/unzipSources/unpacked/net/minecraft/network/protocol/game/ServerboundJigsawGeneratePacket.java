package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundJigsawGeneratePacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundJigsawGeneratePacket> STREAM_CODEC = Packet.codec(
        ServerboundJigsawGeneratePacket::write, ServerboundJigsawGeneratePacket::new
    );
    private final BlockPos pos;
    private final int levels;
    private final boolean keepJigsaws;

    public ServerboundJigsawGeneratePacket(BlockPos pPos, int pLevels, boolean pKeepJigsaws) {
        this.pos = pPos;
        this.levels = pLevels;
        this.keepJigsaws = pKeepJigsaws;
    }

    private ServerboundJigsawGeneratePacket(FriendlyByteBuf p_179669_) {
        this.pos = p_179669_.readBlockPos();
        this.levels = p_179669_.readVarInt();
        this.keepJigsaws = p_179669_.readBoolean();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134089_) {
        p_134089_.writeBlockPos(this.pos);
        p_134089_.writeVarInt(this.levels);
        p_134089_.writeBoolean(this.keepJigsaws);
    }

    @Override
    public PacketType<ServerboundJigsawGeneratePacket> type() {
        return GamePacketTypes.SERVERBOUND_JIGSAW_GENERATE;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleJigsawGenerate(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int levels() {
        return this.levels;
    }

    public boolean keepJigsaws() {
        return this.keepJigsaws;
    }
}
