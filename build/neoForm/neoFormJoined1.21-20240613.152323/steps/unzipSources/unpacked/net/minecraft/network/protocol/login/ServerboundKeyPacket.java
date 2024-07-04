package net.minecraft.network.protocol.login;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.SecretKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;

public class ServerboundKeyPacket implements Packet<ServerLoginPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundKeyPacket> STREAM_CODEC = Packet.codec(ServerboundKeyPacket::write, ServerboundKeyPacket::new);
    private final byte[] keybytes;
    private final byte[] encryptedChallenge;

    public ServerboundKeyPacket(SecretKey pSecretKey, PublicKey pPublicKey, byte[] pChallenge) throws CryptException {
        this.keybytes = Crypt.encryptUsingKey(pPublicKey, pSecretKey.getEncoded());
        this.encryptedChallenge = Crypt.encryptUsingKey(pPublicKey, pChallenge);
    }

    private ServerboundKeyPacket(FriendlyByteBuf p_179829_) {
        this.keybytes = p_179829_.readByteArray();
        this.encryptedChallenge = p_179829_.readByteArray();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134870_) {
        p_134870_.writeByteArray(this.keybytes);
        p_134870_.writeByteArray(this.encryptedChallenge);
    }

    @Override
    public PacketType<ServerboundKeyPacket> type() {
        return LoginPacketTypes.SERVERBOUND_KEY;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerLoginPacketListener pHandler) {
        pHandler.handleKey(this);
    }

    public SecretKey getSecretKey(PrivateKey pKey) throws CryptException {
        return Crypt.decryptByteToSecretKey(pKey, this.keybytes);
    }

    public boolean isChallengeValid(byte[] pExpected, PrivateKey pKey) {
        try {
            return Arrays.equals(pExpected, Crypt.decryptUsingKey(pKey, this.encryptedChallenge));
        } catch (CryptException cryptexception) {
            return false;
        }
    }
}
