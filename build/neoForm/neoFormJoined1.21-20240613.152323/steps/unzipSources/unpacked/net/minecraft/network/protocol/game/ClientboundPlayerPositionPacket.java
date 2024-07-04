package net.minecraft.network.protocol.game;

import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.RelativeMovement;

public class ClientboundPlayerPositionPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundPlayerPositionPacket> STREAM_CODEC = Packet.codec(
        ClientboundPlayerPositionPacket::write, ClientboundPlayerPositionPacket::new
    );
    private final double x;
    private final double y;
    private final double z;
    private final float yRot;
    private final float xRot;
    private final Set<RelativeMovement> relativeArguments;
    private final int id;

    public ClientboundPlayerPositionPacket(
        double pX, double pY, double pZ, float pYRot, float pXRot, Set<RelativeMovement> pRelativeArguments, int pId
    ) {
        this.x = pX;
        this.y = pY;
        this.z = pZ;
        this.yRot = pYRot;
        this.xRot = pXRot;
        this.relativeArguments = pRelativeArguments;
        this.id = pId;
    }

    private ClientboundPlayerPositionPacket(FriendlyByteBuf p_179158_) {
        this.x = p_179158_.readDouble();
        this.y = p_179158_.readDouble();
        this.z = p_179158_.readDouble();
        this.yRot = p_179158_.readFloat();
        this.xRot = p_179158_.readFloat();
        this.relativeArguments = RelativeMovement.unpack(p_179158_.readUnsignedByte());
        this.id = p_179158_.readVarInt();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_132820_) {
        p_132820_.writeDouble(this.x);
        p_132820_.writeDouble(this.y);
        p_132820_.writeDouble(this.z);
        p_132820_.writeFloat(this.yRot);
        p_132820_.writeFloat(this.xRot);
        p_132820_.writeByte(RelativeMovement.pack(this.relativeArguments));
        p_132820_.writeVarInt(this.id);
    }

    @Override
    public PacketType<ClientboundPlayerPositionPacket> type() {
        return GamePacketTypes.CLIENTBOUND_PLAYER_POSITION;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleMovePlayer(this);
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public float getYRot() {
        return this.yRot;
    }

    public float getXRot() {
        return this.xRot;
    }

    public int getId() {
        return this.id;
    }

    public Set<RelativeMovement> getRelativeArguments() {
        return this.relativeArguments;
    }
}
