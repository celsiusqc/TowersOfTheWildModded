package net.minecraft.network.protocol.login;

import java.security.PublicKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;

public class ClientboundHelloPacket implements Packet<ClientLoginPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundHelloPacket> STREAM_CODEC = Packet.codec(
        ClientboundHelloPacket::write, ClientboundHelloPacket::new
    );
    private final String serverId;
    private final byte[] publicKey;
    private final byte[] challenge;
    private final boolean shouldAuthenticate;

    public ClientboundHelloPacket(String pServerId, byte[] pPublicKey, byte[] pChallenge, boolean pShouldAuthenticate) {
        this.serverId = pServerId;
        this.publicKey = pPublicKey;
        this.challenge = pChallenge;
        this.shouldAuthenticate = pShouldAuthenticate;
    }

    private ClientboundHelloPacket(FriendlyByteBuf p_179816_) {
        this.serverId = p_179816_.readUtf(20);
        this.publicKey = p_179816_.readByteArray();
        this.challenge = p_179816_.readByteArray();
        this.shouldAuthenticate = p_179816_.readBoolean();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134793_) {
        p_134793_.writeUtf(this.serverId);
        p_134793_.writeByteArray(this.publicKey);
        p_134793_.writeByteArray(this.challenge);
        p_134793_.writeBoolean(this.shouldAuthenticate);
    }

    @Override
    public PacketType<ClientboundHelloPacket> type() {
        return LoginPacketTypes.CLIENTBOUND_HELLO;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientLoginPacketListener pHandler) {
        pHandler.handleHello(this);
    }

    public String getServerId() {
        return this.serverId;
    }

    public PublicKey getPublicKey() throws CryptException {
        return Crypt.byteToPublicKey(this.publicKey);
    }

    public byte[] getChallenge() {
        return this.challenge;
    }

    public boolean shouldAuthenticate() {
        return this.shouldAuthenticate;
    }
}
