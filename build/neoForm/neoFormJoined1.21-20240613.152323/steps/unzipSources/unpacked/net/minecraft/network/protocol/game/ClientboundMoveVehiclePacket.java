package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;

public class ClientboundMoveVehiclePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundMoveVehiclePacket> STREAM_CODEC = Packet.codec(
        ClientboundMoveVehiclePacket::write, ClientboundMoveVehiclePacket::new
    );
    private final double x;
    private final double y;
    private final double z;
    private final float yRot;
    private final float xRot;

    public ClientboundMoveVehiclePacket(Entity pVehicle) {
        this.x = pVehicle.getX();
        this.y = pVehicle.getY();
        this.z = pVehicle.getZ();
        this.yRot = pVehicle.getYRot();
        this.xRot = pVehicle.getXRot();
    }

    private ClientboundMoveVehiclePacket(FriendlyByteBuf p_179007_) {
        this.x = p_179007_.readDouble();
        this.y = p_179007_.readDouble();
        this.z = p_179007_.readDouble();
        this.yRot = p_179007_.readFloat();
        this.xRot = p_179007_.readFloat();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_132593_) {
        p_132593_.writeDouble(this.x);
        p_132593_.writeDouble(this.y);
        p_132593_.writeDouble(this.z);
        p_132593_.writeFloat(this.yRot);
        p_132593_.writeFloat(this.xRot);
    }

    @Override
    public PacketType<ClientboundMoveVehiclePacket> type() {
        return GamePacketTypes.CLIENTBOUND_MOVE_VEHICLE;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleMoveVehicle(this);
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
}
