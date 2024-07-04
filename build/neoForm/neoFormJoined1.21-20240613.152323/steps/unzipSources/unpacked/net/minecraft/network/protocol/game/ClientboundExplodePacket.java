package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

public class ClientboundExplodePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundExplodePacket> STREAM_CODEC = Packet.codec(
        ClientboundExplodePacket::write, ClientboundExplodePacket::new
    );
    private final double x;
    private final double y;
    private final double z;
    private final float power;
    private final List<BlockPos> toBlow;
    private final float knockbackX;
    private final float knockbackY;
    private final float knockbackZ;
    private final ParticleOptions smallExplosionParticles;
    private final ParticleOptions largeExplosionParticles;
    private final Explosion.BlockInteraction blockInteraction;
    private final Holder<SoundEvent> explosionSound;

    public ClientboundExplodePacket(
        double pX,
        double pY,
        double pZ,
        float pPower,
        List<BlockPos> pToBlow,
        @Nullable Vec3 pKnockback,
        Explosion.BlockInteraction pBlockInteraction,
        ParticleOptions pSmallExplosionParticles,
        ParticleOptions pLargeExplosionParticles,
        Holder<SoundEvent> pExplosionSound
    ) {
        this.x = pX;
        this.y = pY;
        this.z = pZ;
        this.power = pPower;
        this.toBlow = Lists.newArrayList(pToBlow);
        this.explosionSound = pExplosionSound;
        if (pKnockback != null) {
            this.knockbackX = (float)pKnockback.x;
            this.knockbackY = (float)pKnockback.y;
            this.knockbackZ = (float)pKnockback.z;
        } else {
            this.knockbackX = 0.0F;
            this.knockbackY = 0.0F;
            this.knockbackZ = 0.0F;
        }

        this.blockInteraction = pBlockInteraction;
        this.smallExplosionParticles = pSmallExplosionParticles;
        this.largeExplosionParticles = pLargeExplosionParticles;
    }

    private ClientboundExplodePacket(RegistryFriendlyByteBuf p_320217_) {
        this.x = p_320217_.readDouble();
        this.y = p_320217_.readDouble();
        this.z = p_320217_.readDouble();
        this.power = p_320217_.readFloat();
        int i = Mth.floor(this.x);
        int j = Mth.floor(this.y);
        int k = Mth.floor(this.z);
        this.toBlow = p_320217_.readList(p_178850_ -> {
            int l = p_178850_.readByte() + i;
            int i1 = p_178850_.readByte() + j;
            int j1 = p_178850_.readByte() + k;
            return new BlockPos(l, i1, j1);
        });
        this.knockbackX = p_320217_.readFloat();
        this.knockbackY = p_320217_.readFloat();
        this.knockbackZ = p_320217_.readFloat();
        this.blockInteraction = p_320217_.readEnum(Explosion.BlockInteraction.class);
        this.smallExplosionParticles = ParticleTypes.STREAM_CODEC.decode(p_320217_);
        this.largeExplosionParticles = ParticleTypes.STREAM_CODEC.decode(p_320217_);
        this.explosionSound = SoundEvent.STREAM_CODEC.decode(p_320217_);
    }

    private void write(RegistryFriendlyByteBuf p_319914_) {
        p_319914_.writeDouble(this.x);
        p_319914_.writeDouble(this.y);
        p_319914_.writeDouble(this.z);
        p_319914_.writeFloat(this.power);
        int i = Mth.floor(this.x);
        int j = Mth.floor(this.y);
        int k = Mth.floor(this.z);
        p_319914_.writeCollection(this.toBlow, (p_293728_, p_293729_) -> {
            int l = p_293729_.getX() - i;
            int i1 = p_293729_.getY() - j;
            int j1 = p_293729_.getZ() - k;
            p_293728_.writeByte(l);
            p_293728_.writeByte(i1);
            p_293728_.writeByte(j1);
        });
        p_319914_.writeFloat(this.knockbackX);
        p_319914_.writeFloat(this.knockbackY);
        p_319914_.writeFloat(this.knockbackZ);
        p_319914_.writeEnum(this.blockInteraction);
        ParticleTypes.STREAM_CODEC.encode(p_319914_, this.smallExplosionParticles);
        ParticleTypes.STREAM_CODEC.encode(p_319914_, this.largeExplosionParticles);
        SoundEvent.STREAM_CODEC.encode(p_319914_, this.explosionSound);
    }

    @Override
    public PacketType<ClientboundExplodePacket> type() {
        return GamePacketTypes.CLIENTBOUND_EXPLODE;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleExplosion(this);
    }

    public float getKnockbackX() {
        return this.knockbackX;
    }

    public float getKnockbackY() {
        return this.knockbackY;
    }

    public float getKnockbackZ() {
        return this.knockbackZ;
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

    public float getPower() {
        return this.power;
    }

    public List<BlockPos> getToBlow() {
        return this.toBlow;
    }

    public Explosion.BlockInteraction getBlockInteraction() {
        return this.blockInteraction;
    }

    public ParticleOptions getSmallExplosionParticles() {
        return this.smallExplosionParticles;
    }

    public ParticleOptions getLargeExplosionParticles() {
        return this.largeExplosionParticles;
    }

    public Holder<SoundEvent> getExplosionSound() {
        return this.explosionSound;
    }
}
