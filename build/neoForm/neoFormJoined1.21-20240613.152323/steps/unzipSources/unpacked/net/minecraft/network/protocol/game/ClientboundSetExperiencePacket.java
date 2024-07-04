package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetExperiencePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetExperiencePacket> STREAM_CODEC = Packet.codec(
        ClientboundSetExperiencePacket::write, ClientboundSetExperiencePacket::new
    );
    private final float experienceProgress;
    private final int totalExperience;
    private final int experienceLevel;

    public ClientboundSetExperiencePacket(float pExperienceProgress, int pTotalExperience, int pExperienceLevel) {
        this.experienceProgress = pExperienceProgress;
        this.totalExperience = pTotalExperience;
        this.experienceLevel = pExperienceLevel;
    }

    private ClientboundSetExperiencePacket(FriendlyByteBuf p_179299_) {
        this.experienceProgress = p_179299_.readFloat();
        this.experienceLevel = p_179299_.readVarInt();
        this.totalExperience = p_179299_.readVarInt();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    private void write(FriendlyByteBuf p_133230_) {
        p_133230_.writeFloat(this.experienceProgress);
        p_133230_.writeVarInt(this.experienceLevel);
        p_133230_.writeVarInt(this.totalExperience);
    }

    @Override
    public PacketType<ClientboundSetExperiencePacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_EXPERIENCE;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetExperience(this);
    }

    public float getExperienceProgress() {
        return this.experienceProgress;
    }

    public int getTotalExperience() {
        return this.totalExperience;
    }

    public int getExperienceLevel() {
        return this.experienceLevel;
    }
}
