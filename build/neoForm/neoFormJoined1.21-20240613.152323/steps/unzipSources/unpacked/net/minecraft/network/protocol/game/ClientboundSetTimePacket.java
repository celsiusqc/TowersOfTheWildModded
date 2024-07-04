package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetTimePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetTimePacket> STREAM_CODEC = Packet.codec(
        ClientboundSetTimePacket::write, ClientboundSetTimePacket::new
    );
    private final long gameTime;
    private final long dayTime;

    public ClientboundSetTimePacket(long pGameTime, long pDayTime, boolean pDaylightCycleEnabled) {
        this.gameTime = pGameTime;
        long i = pDayTime;
        if (!pDaylightCycleEnabled) {
            i = -pDayTime;
            if (i == 0L) {
                i = -1L;
            }
        }

        this.dayTime = i;
    }

    private ClientboundSetTimePacket(FriendlyByteBuf p_179387_) {
        this.gameTime = p_179387_.readLong();
        this.dayTime = p_179387_.readLong();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133360_) {
        p_133360_.writeLong(this.gameTime);
        p_133360_.writeLong(this.dayTime);
    }

    @Override
    public PacketType<ClientboundSetTimePacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_TIME;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetTime(this);
    }

    public long getGameTime() {
        return this.gameTime;
    }

    public long getDayTime() {
        return this.dayTime;
    }
}
