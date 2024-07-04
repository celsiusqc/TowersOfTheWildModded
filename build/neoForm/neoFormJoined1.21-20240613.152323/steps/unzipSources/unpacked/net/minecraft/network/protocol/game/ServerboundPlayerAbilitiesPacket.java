package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.player.Abilities;

public class ServerboundPlayerAbilitiesPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundPlayerAbilitiesPacket> STREAM_CODEC = Packet.codec(
        ServerboundPlayerAbilitiesPacket::write, ServerboundPlayerAbilitiesPacket::new
    );
    private static final int FLAG_FLYING = 2;
    private final boolean isFlying;

    public ServerboundPlayerAbilitiesPacket(Abilities pAbilities) {
        this.isFlying = pAbilities.flying;
    }

    private ServerboundPlayerAbilitiesPacket(FriendlyByteBuf p_179709_) {
        byte b0 = p_179709_.readByte();
        this.isFlying = (b0 & 2) != 0;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134266_) {
        byte b0 = 0;
        if (this.isFlying) {
            b0 = (byte)(b0 | 2);
        }

        p_134266_.writeByte(b0);
    }

    @Override
    public PacketType<ServerboundPlayerAbilitiesPacket> type() {
        return GamePacketTypes.SERVERBOUND_PLAYER_ABILITIES;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handlePlayerAbilities(this);
    }

    public boolean isFlying() {
        return this.isFlying;
    }
}
