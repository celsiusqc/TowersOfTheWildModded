package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.debugchart.RemoteDebugSampleType;

public record ServerboundDebugSampleSubscriptionPacket(RemoteDebugSampleType sampleType) implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundDebugSampleSubscriptionPacket> STREAM_CODEC = Packet.codec(
        ServerboundDebugSampleSubscriptionPacket::write, ServerboundDebugSampleSubscriptionPacket::new
    );

    private ServerboundDebugSampleSubscriptionPacket(FriendlyByteBuf p_323947_) {
        this(p_323947_.readEnum(RemoteDebugSampleType.class));
    }

    private void write(FriendlyByteBuf p_323974_) {
        p_323974_.writeEnum(this.sampleType);
    }

    @Override
    public PacketType<ServerboundDebugSampleSubscriptionPacket> type() {
        return GamePacketTypes.SERVERBOUND_DEBUG_SAMPLE_SUBSCRIPTION;
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleDebugSampleSubscription(this);
    }
}
