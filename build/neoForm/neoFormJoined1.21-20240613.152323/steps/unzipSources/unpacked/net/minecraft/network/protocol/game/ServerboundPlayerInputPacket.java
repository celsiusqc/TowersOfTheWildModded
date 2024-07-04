package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPlayerInputPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundPlayerInputPacket> STREAM_CODEC = Packet.codec(
        ServerboundPlayerInputPacket::write, ServerboundPlayerInputPacket::new
    );
    private static final int FLAG_JUMPING = 1;
    private static final int FLAG_SHIFT_KEY_DOWN = 2;
    /**
     * Positive for left strafe, negative for right
     */
    private final float xxa;
    private final float zza;
    private final boolean isJumping;
    private final boolean isShiftKeyDown;

    public ServerboundPlayerInputPacket(float pXxa, float pZza, boolean pIsJumping, boolean pIsShiftKeyDown) {
        this.xxa = pXxa;
        this.zza = pZza;
        this.isJumping = pIsJumping;
        this.isShiftKeyDown = pIsShiftKeyDown;
    }

    private ServerboundPlayerInputPacket(FriendlyByteBuf p_179720_) {
        this.xxa = p_179720_.readFloat();
        this.zza = p_179720_.readFloat();
        byte b0 = p_179720_.readByte();
        this.isJumping = (b0 & 1) > 0;
        this.isShiftKeyDown = (b0 & 2) > 0;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134357_) {
        p_134357_.writeFloat(this.xxa);
        p_134357_.writeFloat(this.zza);
        byte b0 = 0;
        if (this.isJumping) {
            b0 = (byte)(b0 | 1);
        }

        if (this.isShiftKeyDown) {
            b0 = (byte)(b0 | 2);
        }

        p_134357_.writeByte(b0);
    }

    @Override
    public PacketType<ServerboundPlayerInputPacket> type() {
        return GamePacketTypes.SERVERBOUND_PLAYER_INPUT;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handlePlayerInput(this);
    }

    public float getXxa() {
        return this.xxa;
    }

    public float getZza() {
        return this.zza;
    }

    public boolean isJumping() {
        return this.isJumping;
    }

    public boolean isShiftKeyDown() {
        return this.isShiftKeyDown;
    }
}
