package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public abstract class ServerboundMovePlayerPacket implements Packet<ServerGamePacketListener> {
    protected final double x;
    protected final double y;
    protected final double z;
    protected final float yRot;
    protected final float xRot;
    protected final boolean onGround;
    protected final boolean hasPos;
    protected final boolean hasRot;

    protected ServerboundMovePlayerPacket(
        double pX, double pY, double pZ, float pYRot, float pXRot, boolean pOnGround, boolean pHasPos, boolean pHasRot
    ) {
        this.x = pX;
        this.y = pY;
        this.z = pZ;
        this.yRot = pYRot;
        this.xRot = pXRot;
        this.onGround = pOnGround;
        this.hasPos = pHasPos;
        this.hasRot = pHasRot;
    }

    @Override
    public abstract PacketType<? extends ServerboundMovePlayerPacket> type();

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleMovePlayer(this);
    }

    public double getX(double pDefaultValue) {
        return this.hasPos ? this.x : pDefaultValue;
    }

    public double getY(double pDefaultValue) {
        return this.hasPos ? this.y : pDefaultValue;
    }

    public double getZ(double pDefaultValue) {
        return this.hasPos ? this.z : pDefaultValue;
    }

    public float getYRot(float pDefaultValue) {
        return this.hasRot ? this.yRot : pDefaultValue;
    }

    public float getXRot(float pDefaultValue) {
        return this.hasRot ? this.xRot : pDefaultValue;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean hasPosition() {
        return this.hasPos;
    }

    public boolean hasRotation() {
        return this.hasRot;
    }

    public static class Pos extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.Pos> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.Pos::write, ServerboundMovePlayerPacket.Pos::read
        );

        public Pos(double pX, double pY, double pZ, boolean pOnGround) {
            super(pX, pY, pZ, 0.0F, 0.0F, pOnGround, true, false);
        }

        private static ServerboundMovePlayerPacket.Pos read(FriendlyByteBuf p_179686_) {
            double d0 = p_179686_.readDouble();
            double d1 = p_179686_.readDouble();
            double d2 = p_179686_.readDouble();
            boolean flag = p_179686_.readUnsignedByte() != 0;
            return new ServerboundMovePlayerPacket.Pos(d0, d1, d2, flag);
        }

        /**
         * Writes the raw packet data to the data stream.
         */
        private void write(FriendlyByteBuf p_134159_) {
            p_134159_.writeDouble(this.x);
            p_134159_.writeDouble(this.y);
            p_134159_.writeDouble(this.z);
            p_134159_.writeByte(this.onGround ? 1 : 0);
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.Pos> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS;
        }
    }

    public static class PosRot extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.PosRot> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.PosRot::write, ServerboundMovePlayerPacket.PosRot::read
        );

        public PosRot(double pX, double pY, double pZ, float pYRot, float pXRot, boolean pOnGround) {
            super(pX, pY, pZ, pYRot, pXRot, pOnGround, true, true);
        }

        private static ServerboundMovePlayerPacket.PosRot read(FriendlyByteBuf p_179688_) {
            double d0 = p_179688_.readDouble();
            double d1 = p_179688_.readDouble();
            double d2 = p_179688_.readDouble();
            float f = p_179688_.readFloat();
            float f1 = p_179688_.readFloat();
            boolean flag = p_179688_.readUnsignedByte() != 0;
            return new ServerboundMovePlayerPacket.PosRot(d0, d1, d2, f, f1, flag);
        }

        /**
         * Writes the raw packet data to the data stream.
         */
        private void write(FriendlyByteBuf p_134173_) {
            p_134173_.writeDouble(this.x);
            p_134173_.writeDouble(this.y);
            p_134173_.writeDouble(this.z);
            p_134173_.writeFloat(this.yRot);
            p_134173_.writeFloat(this.xRot);
            p_134173_.writeByte(this.onGround ? 1 : 0);
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.PosRot> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS_ROT;
        }
    }

    public static class Rot extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.Rot> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.Rot::write, ServerboundMovePlayerPacket.Rot::read
        );

        public Rot(float pYRot, float pXRot, boolean pOnGround) {
            super(0.0, 0.0, 0.0, pYRot, pXRot, pOnGround, false, true);
        }

        private static ServerboundMovePlayerPacket.Rot read(FriendlyByteBuf p_179690_) {
            float f = p_179690_.readFloat();
            float f1 = p_179690_.readFloat();
            boolean flag = p_179690_.readUnsignedByte() != 0;
            return new ServerboundMovePlayerPacket.Rot(f, f1, flag);
        }

        /**
         * Writes the raw packet data to the data stream.
         */
        private void write(FriendlyByteBuf p_134184_) {
            p_134184_.writeFloat(this.yRot);
            p_134184_.writeFloat(this.xRot);
            p_134184_.writeByte(this.onGround ? 1 : 0);
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.Rot> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_ROT;
        }
    }

    public static class StatusOnly extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.StatusOnly> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.StatusOnly::write, ServerboundMovePlayerPacket.StatusOnly::read
        );

        public StatusOnly(boolean pOnGround) {
            super(0.0, 0.0, 0.0, 0.0F, 0.0F, pOnGround, false, false);
        }

        private static ServerboundMovePlayerPacket.StatusOnly read(FriendlyByteBuf p_179698_) {
            boolean flag = p_179698_.readUnsignedByte() != 0;
            return new ServerboundMovePlayerPacket.StatusOnly(flag);
        }

        private void write(FriendlyByteBuf p_179694_) {
            p_179694_.writeByte(this.onGround ? 1 : 0);
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.StatusOnly> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_STATUS_ONLY;
        }
    }
}
