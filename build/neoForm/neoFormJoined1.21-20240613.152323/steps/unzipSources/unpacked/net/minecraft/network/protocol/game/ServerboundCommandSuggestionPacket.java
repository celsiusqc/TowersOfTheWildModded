package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundCommandSuggestionPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundCommandSuggestionPacket> STREAM_CODEC = Packet.codec(
        ServerboundCommandSuggestionPacket::write, ServerboundCommandSuggestionPacket::new
    );
    private final int id;
    private final String command;

    public ServerboundCommandSuggestionPacket(int pId, String pCommand) {
        this.id = pId;
        this.command = pCommand;
    }

    private ServerboundCommandSuggestionPacket(FriendlyByteBuf p_179565_) {
        this.id = p_179565_.readVarInt();
        this.command = p_179565_.readUtf(32500);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133903_) {
        p_133903_.writeVarInt(this.id);
        p_133903_.writeUtf(this.command, 32500);
    }

    @Override
    public PacketType<ServerboundCommandSuggestionPacket> type() {
        return GamePacketTypes.SERVERBOUND_COMMAND_SUGGESTION;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleCustomCommandSuggestions(this);
    }

    public int getId() {
        return this.id;
    }

    public String getCommand() {
        return this.command;
    }
}
