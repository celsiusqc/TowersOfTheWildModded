package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.damagesource.CombatTracker;

public class ClientboundPlayerCombatEndPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundPlayerCombatEndPacket> STREAM_CODEC = Packet.codec(
        ClientboundPlayerCombatEndPacket::write, ClientboundPlayerCombatEndPacket::new
    );
    private final int duration;

    public ClientboundPlayerCombatEndPacket(CombatTracker pCombatTracker) {
        this(pCombatTracker.getCombatDuration());
    }

    public ClientboundPlayerCombatEndPacket(int pDuration) {
        this.duration = pDuration;
    }

    private ClientboundPlayerCombatEndPacket(FriendlyByteBuf p_179042_) {
        this.duration = p_179042_.readVarInt();
    }

    private void write(FriendlyByteBuf p_179044_) {
        p_179044_.writeVarInt(this.duration);
    }

    @Override
    public PacketType<ClientboundPlayerCombatEndPacket> type() {
        return GamePacketTypes.CLIENTBOUND_PLAYER_COMBAT_END;
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handlePlayerCombatEnd(this);
    }
}
