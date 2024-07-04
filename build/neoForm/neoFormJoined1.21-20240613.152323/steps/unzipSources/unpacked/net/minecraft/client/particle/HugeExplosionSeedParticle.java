package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HugeExplosionSeedParticle extends NoRenderParticle {
    HugeExplosionSeedParticle(ClientLevel pLevel, double pX, double pY, double pZ) {
        super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
        this.lifetime = 8;
    }

    @Override
    public void tick() {
        for (int i = 0; i < 6; i++) {
            double d0 = this.x + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
            double d1 = this.y + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
            double d2 = this.z + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
            this.level.addParticle(ParticleTypes.EXPLOSION, d0, d1, d2, (double)((float)this.age / (float)this.lifetime), 0.0, 0.0);
        }

        this.age++;
        if (this.age == this.lifetime) {
            this.remove();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            return new HugeExplosionSeedParticle(pLevel, pX, pY, pZ);
        }
    }
}
