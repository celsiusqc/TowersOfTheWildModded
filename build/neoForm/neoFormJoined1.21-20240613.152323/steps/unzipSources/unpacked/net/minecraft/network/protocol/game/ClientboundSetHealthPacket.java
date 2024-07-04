package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetHealthPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetHealthPacket> STREAM_CODEC = Packet.codec(
        ClientboundSetHealthPacket::write, ClientboundSetHealthPacket::new
    );
    private final float health;
    private final int food;
    private final float saturation;

    public ClientboundSetHealthPacket(float pHealth, int pFood, float pSaturation) {
        this.health = pHealth;
        this.food = pFood;
        this.saturation = pSaturation;
    }

    private ClientboundSetHealthPacket(FriendlyByteBuf p_179301_) {
        this.health = p_179301_.readFloat();
        this.food = p_179301_.readVarInt();
        this.saturation = p_179301_.readFloat();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133249_) {
        p_133249_.writeFloat(this.health);
        p_133249_.writeVarInt(this.food);
        p_133249_.writeFloat(this.saturation);
    }

    @Override
    public PacketType<ClientboundSetHealthPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_HEALTH;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetHealth(this);
    }

    public float getHealth() {
        return this.health;
    }

    public int getFood() {
        return this.food;
    }

    public float getSaturation() {
        return this.saturation;
    }
}
