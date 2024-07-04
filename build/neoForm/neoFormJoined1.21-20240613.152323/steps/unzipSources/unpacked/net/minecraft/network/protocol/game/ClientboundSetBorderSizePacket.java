package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderSizePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetBorderSizePacket> STREAM_CODEC = Packet.codec(
        ClientboundSetBorderSizePacket::write, ClientboundSetBorderSizePacket::new
    );
    private final double size;

    public ClientboundSetBorderSizePacket(WorldBorder pWorldBorder) {
        this.size = pWorldBorder.getLerpTarget();
    }

    private ClientboundSetBorderSizePacket(FriendlyByteBuf p_179245_) {
        this.size = p_179245_.readDouble();
    }

    private void write(FriendlyByteBuf p_179247_) {
        p_179247_.writeDouble(this.size);
    }

    @Override
    public PacketType<ClientboundSetBorderSizePacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_BORDER_SIZE;
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetBorderSize(this);
    }

    public double getSize() {
        return this.size;
    }
}
