package net.minecraft.network.protocol.game;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class ClientboundSoundEntityPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSoundEntityPacket> STREAM_CODEC = Packet.codec(
        ClientboundSoundEntityPacket::write, ClientboundSoundEntityPacket::new
    );
    private final Holder<SoundEvent> sound;
    private final SoundSource source;
    private final int id;
    private final float volume;
    private final float pitch;
    private final long seed;

    public ClientboundSoundEntityPacket(Holder<SoundEvent> pSound, SoundSource pSource, Entity pEntity, float pVolume, float pPitch, long pSeed) {
        this.sound = pSound;
        this.source = pSource;
        this.id = pEntity.getId();
        this.volume = pVolume;
        this.pitch = pPitch;
        this.seed = pSeed;
    }

    private ClientboundSoundEntityPacket(RegistryFriendlyByteBuf p_319844_) {
        this.sound = SoundEvent.STREAM_CODEC.decode(p_319844_);
        this.source = p_319844_.readEnum(SoundSource.class);
        this.id = p_319844_.readVarInt();
        this.volume = p_319844_.readFloat();
        this.pitch = p_319844_.readFloat();
        this.seed = p_319844_.readLong();
    }

    private void write(RegistryFriendlyByteBuf p_320141_) {
        SoundEvent.STREAM_CODEC.encode(p_320141_, this.sound);
        p_320141_.writeEnum(this.source);
        p_320141_.writeVarInt(this.id);
        p_320141_.writeFloat(this.volume);
        p_320141_.writeFloat(this.pitch);
        p_320141_.writeLong(this.seed);
    }

    @Override
    public PacketType<ClientboundSoundEntityPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SOUND_ENTITY;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSoundEntityEvent(this);
    }

    public Holder<SoundEvent> getSound() {
        return this.sound;
    }

    public SoundSource getSource() {
        return this.source;
    }

    public int getId() {
        return this.id;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    public long getSeed() {
        return this.seed;
    }
}
