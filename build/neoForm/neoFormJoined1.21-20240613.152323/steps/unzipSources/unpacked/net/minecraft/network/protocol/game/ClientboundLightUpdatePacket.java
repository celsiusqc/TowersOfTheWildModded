package net.minecraft.network.protocol.game;

import java.util.BitSet;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class ClientboundLightUpdatePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundLightUpdatePacket> STREAM_CODEC = Packet.codec(
        ClientboundLightUpdatePacket::write, ClientboundLightUpdatePacket::new
    );
    private final int x;
    private final int z;
    private final ClientboundLightUpdatePacketData lightData;

    public ClientboundLightUpdatePacket(ChunkPos pChunkPos, LevelLightEngine pLightEngine, @Nullable BitSet pSkyLight, @Nullable BitSet pBlockLight) {
        this.x = pChunkPos.x;
        this.z = pChunkPos.z;
        this.lightData = new ClientboundLightUpdatePacketData(pChunkPos, pLightEngine, pSkyLight, pBlockLight);
    }

    private ClientboundLightUpdatePacket(FriendlyByteBuf p_178918_) {
        this.x = p_178918_.readVarInt();
        this.z = p_178918_.readVarInt();
        this.lightData = new ClientboundLightUpdatePacketData(p_178918_, this.x, this.z);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_132351_) {
        p_132351_.writeVarInt(this.x);
        p_132351_.writeVarInt(this.z);
        this.lightData.write(p_132351_);
    }

    @Override
    public PacketType<ClientboundLightUpdatePacket> type() {
        return GamePacketTypes.CLIENTBOUND_LIGHT_UPDATE;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleLightUpdatePacket(this);
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public ClientboundLightUpdatePacketData getLightData() {
        return this.lightData;
    }
}
