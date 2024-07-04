package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPlayerActionPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundPlayerActionPacket> STREAM_CODEC = Packet.codec(
        ServerboundPlayerActionPacket::write, ServerboundPlayerActionPacket::new
    );
    private final BlockPos pos;
    private final Direction direction;
    /**
     * Status of the digging (started, ongoing, broken).
     */
    private final ServerboundPlayerActionPacket.Action action;
    private final int sequence;

    public ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action pAction, BlockPos pPos, Direction pDirection, int pSequence) {
        this.action = pAction;
        this.pos = pPos.immutable();
        this.direction = pDirection;
        this.sequence = pSequence;
    }

    public ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action pAction, BlockPos pPos, Direction pDirection) {
        this(pAction, pPos, pDirection, 0);
    }

    private ServerboundPlayerActionPacket(FriendlyByteBuf p_179711_) {
        this.action = p_179711_.readEnum(ServerboundPlayerActionPacket.Action.class);
        this.pos = p_179711_.readBlockPos();
        this.direction = Direction.from3DDataValue(p_179711_.readUnsignedByte());
        this.sequence = p_179711_.readVarInt();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134283_) {
        p_134283_.writeEnum(this.action);
        p_134283_.writeBlockPos(this.pos);
        p_134283_.writeByte(this.direction.get3DDataValue());
        p_134283_.writeVarInt(this.sequence);
    }

    @Override
    public PacketType<ServerboundPlayerActionPacket> type() {
        return GamePacketTypes.SERVERBOUND_PLAYER_ACTION;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handlePlayerAction(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public ServerboundPlayerActionPacket.Action getAction() {
        return this.action;
    }

    public int getSequence() {
        return this.sequence;
    }

    public static enum Action {
        START_DESTROY_BLOCK,
        ABORT_DESTROY_BLOCK,
        STOP_DESTROY_BLOCK,
        DROP_ALL_ITEMS,
        DROP_ITEM,
        RELEASE_USE_ITEM,
        SWAP_ITEM_WITH_OFFHAND;
    }
}
