package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundOpenSignEditorPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundOpenSignEditorPacket> STREAM_CODEC = Packet.codec(
        ClientboundOpenSignEditorPacket::write, ClientboundOpenSignEditorPacket::new
    );
    private final BlockPos pos;
    private final boolean isFrontText;

    public ClientboundOpenSignEditorPacket(BlockPos pPos, boolean pIsFrontText) {
        this.pos = pPos;
        this.isFrontText = pIsFrontText;
    }

    private ClientboundOpenSignEditorPacket(FriendlyByteBuf p_179013_) {
        this.pos = p_179013_.readBlockPos();
        this.isFrontText = p_179013_.readBoolean();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_132642_) {
        p_132642_.writeBlockPos(this.pos);
        p_132642_.writeBoolean(this.isFrontText);
    }

    @Override
    public PacketType<ClientboundOpenSignEditorPacket> type() {
        return GamePacketTypes.CLIENTBOUND_OPEN_SIGN_EDITOR;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleOpenSignEditor(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public boolean isFrontText() {
        return this.isFrontText;
    }
}
