package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundLockDifficultyPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundLockDifficultyPacket> STREAM_CODEC = Packet.codec(
        ServerboundLockDifficultyPacket::write, ServerboundLockDifficultyPacket::new
    );
    private final boolean locked;

    public ServerboundLockDifficultyPacket(boolean pLocked) {
        this.locked = pLocked;
    }

    private ServerboundLockDifficultyPacket(FriendlyByteBuf p_179673_) {
        this.locked = p_179673_.readBoolean();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134117_) {
        p_134117_.writeBoolean(this.locked);
    }

    @Override
    public PacketType<ServerboundLockDifficultyPacket> type() {
        return GamePacketTypes.SERVERBOUND_LOCK_DIFFICULTY;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleLockDifficulty(this);
    }

    public boolean isLocked() {
        return this.locked;
    }
}
