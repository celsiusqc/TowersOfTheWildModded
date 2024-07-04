package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;

public class ServerboundMoveVehiclePacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundMoveVehiclePacket> STREAM_CODEC = Packet.codec(
        ServerboundMoveVehiclePacket::write, ServerboundMoveVehiclePacket::new
    );
    private final double x;
    private final double y;
    private final double z;
    private final float yRot;
    private final float xRot;

    public ServerboundMoveVehiclePacket(Entity pVehicle) {
        this.x = pVehicle.getX();
        this.y = pVehicle.getY();
        this.z = pVehicle.getZ();
        this.yRot = pVehicle.getYRot();
        this.xRot = pVehicle.getXRot();
    }

    private ServerboundMoveVehiclePacket(FriendlyByteBuf p_179700_) {
        this.x = p_179700_.readDouble();
        this.y = p_179700_.readDouble();
        this.z = p_179700_.readDouble();
        this.yRot = p_179700_.readFloat();
        this.xRot = p_179700_.readFloat();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_134201_) {
        p_134201_.writeDouble(this.x);
        p_134201_.writeDouble(this.y);
        p_134201_.writeDouble(this.z);
        p_134201_.writeFloat(this.yRot);
        p_134201_.writeFloat(this.xRot);
    }

    @Override
    public PacketType<ServerboundMoveVehiclePacket> type() {
        return GamePacketTypes.SERVERBOUND_MOVE_VEHICLE;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
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
