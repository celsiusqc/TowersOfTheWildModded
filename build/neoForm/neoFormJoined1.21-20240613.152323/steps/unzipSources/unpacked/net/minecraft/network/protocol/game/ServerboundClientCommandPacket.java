package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundClientCommandPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundClientCommandPacket> STREAM_CODEC = Packet.codec(
        ServerboundClientCommandPacket::write, ServerboundClientCommandPacket::new
    );
    private final ServerboundClientCommandPacket.Action action;

    public ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action pAction) {
        this.action = pAction;
    }

    private ServerboundClientCommandPacket(FriendlyByteBuf p_179547_) {
        this.action = p_179547_.readEnum(ServerboundClientCommandPacket.Action.class);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133852_) {
        p_133852_.writeEnum(this.action);
    }

    @Override
    public PacketType<ServerboundClientCommandPacket> type() {
        return GamePacketTypes.SERVERBOUND_CLIENT_COMMAND;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleClientCommand(this);
    }

    public ServerboundClientCommandPacket.Action getAction() {
        return this.action;
    }

    public static enum Action {
        PERFORM_RESPAWN,
        REQUEST_STATS;
    }
}
