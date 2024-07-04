package net.minecraft.network.protocol.login;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundLoginDisconnectPacket implements Packet<ClientLoginPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundLoginDisconnectPacket> STREAM_CODEC = Packet.codec(
        ClientboundLoginDisconnectPacket::write, ClientboundLoginDisconnectPacket::new
    );
    private final Component reason;

    public ClientboundLoginDisconnectPacket(Component pReason) {
        this.reason = pReason;
    }

    private ClientboundLoginDisconnectPacket(FriendlyByteBuf p_179820_) {
        this.reason = Component.Serializer.fromJsonLenient(p_179820_.readUtf(262144), RegistryAccess.EMPTY);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134821_) {
        p_134821_.writeUtf(Component.Serializer.toJson(this.reason, RegistryAccess.EMPTY));
    }

    @Override
    public PacketType<ClientboundLoginDisconnectPacket> type() {
        return LoginPacketTypes.CLIENTBOUND_LOGIN_DISCONNECT;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientLoginPacketListener pHandler) {
        pHandler.handleDisconnect(this);
    }

    public Component getReason() {
        return this.reason;
    }
}
