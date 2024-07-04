package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundAcceptTeleportationPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundAcceptTeleportationPacket> STREAM_CODEC = Packet.codec(
        ServerboundAcceptTeleportationPacket::write, ServerboundAcceptTeleportationPacket::new
    );
    private final int id;

    public ServerboundAcceptTeleportationPacket(int pId) {
        this.id = pId;
    }

    private ServerboundAcceptTeleportationPacket(FriendlyByteBuf p_179538_) {
        this.id = p_179538_.readVarInt();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133797_) {
        p_133797_.writeVarInt(this.id);
    }

    @Override
    public PacketType<ServerboundAcceptTeleportationPacket> type() {
        return GamePacketTypes.SERVERBOUND_ACCEPT_TELEPORTATION;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleAcceptTeleportPacket(this);
    }

    public int getId() {
        return this.id;
    }
}
