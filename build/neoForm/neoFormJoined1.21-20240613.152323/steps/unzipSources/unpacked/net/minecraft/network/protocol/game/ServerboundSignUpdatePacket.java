package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundSignUpdatePacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundSignUpdatePacket> STREAM_CODEC = Packet.codec(
        ServerboundSignUpdatePacket::write, ServerboundSignUpdatePacket::new
    );
    private static final int MAX_STRING_LENGTH = 384;
    private final BlockPos pos;
    private final String[] lines;
    private final boolean isFrontText;

    public ServerboundSignUpdatePacket(BlockPos pPos, boolean pIsFrontText, String pLine1, String pLine2, String pLine3, String pLine4) {
        this.pos = pPos;
        this.isFrontText = pIsFrontText;
        this.lines = new String[]{pLine1, pLine2, pLine3, pLine4};
    }

    private ServerboundSignUpdatePacket(FriendlyByteBuf p_179790_) {
        this.pos = p_179790_.readBlockPos();
        this.isFrontText = p_179790_.readBoolean();
        this.lines = new String[4];

        for (int i = 0; i < 4; i++) {
            this.lines[i] = p_179790_.readUtf(384);
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134662_) {
        p_134662_.writeBlockPos(this.pos);
        p_134662_.writeBoolean(this.isFrontText);

        for (int i = 0; i < 4; i++) {
            p_134662_.writeUtf(this.lines[i]);
        }
    }

    @Override
    public PacketType<ServerboundSignUpdatePacket> type() {
        return GamePacketTypes.SERVERBOUND_SIGN_UPDATE;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleSignUpdate(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public boolean isFrontText() {
        return this.isFrontText;
    }

    public String[] getLines() {
        return this.lines;
    }
}
