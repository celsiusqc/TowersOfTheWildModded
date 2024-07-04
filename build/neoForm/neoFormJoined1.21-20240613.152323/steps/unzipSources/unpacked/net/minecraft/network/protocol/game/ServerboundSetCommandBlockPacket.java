package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

public class ServerboundSetCommandBlockPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundSetCommandBlockPacket> STREAM_CODEC = Packet.codec(
        ServerboundSetCommandBlockPacket::write, ServerboundSetCommandBlockPacket::new
    );
    private static final int FLAG_TRACK_OUTPUT = 1;
    private static final int FLAG_CONDITIONAL = 2;
    private static final int FLAG_AUTOMATIC = 4;
    private final BlockPos pos;
    private final String command;
    private final boolean trackOutput;
    private final boolean conditional;
    private final boolean automatic;
    private final CommandBlockEntity.Mode mode;

    public ServerboundSetCommandBlockPacket(
        BlockPos pPos, String pCommand, CommandBlockEntity.Mode pMode, boolean pTrackOutput, boolean pConditional, boolean pAutomatic
    ) {
        this.pos = pPos;
        this.command = pCommand;
        this.trackOutput = pTrackOutput;
        this.conditional = pConditional;
        this.automatic = pAutomatic;
        this.mode = pMode;
    }

    private ServerboundSetCommandBlockPacket(FriendlyByteBuf p_179756_) {
        this.pos = p_179756_.readBlockPos();
        this.command = p_179756_.readUtf();
        this.mode = p_179756_.readEnum(CommandBlockEntity.Mode.class);
        int i = p_179756_.readByte();
        this.trackOutput = (i & 1) != 0;
        this.conditional = (i & 2) != 0;
        this.automatic = (i & 4) != 0;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134523_) {
        p_134523_.writeBlockPos(this.pos);
        p_134523_.writeUtf(this.command);
        p_134523_.writeEnum(this.mode);
        int i = 0;
        if (this.trackOutput) {
            i |= 1;
        }

        if (this.conditional) {
            i |= 2;
        }

        if (this.automatic) {
            i |= 4;
        }

        p_134523_.writeByte(i);
    }

    @Override
    public PacketType<ServerboundSetCommandBlockPacket> type() {
        return GamePacketTypes.SERVERBOUND_SET_COMMAND_BLOCK;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleSetCommandBlock(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public String getCommand() {
        return this.command;
    }

    public boolean isTrackOutput() {
        return this.trackOutput;
    }

    public boolean isConditional() {
        return this.conditional;
    }

    public boolean isAutomatic() {
        return this.automatic;
    }

    public CommandBlockEntity.Mode getMode() {
        return this.mode;
    }
}
