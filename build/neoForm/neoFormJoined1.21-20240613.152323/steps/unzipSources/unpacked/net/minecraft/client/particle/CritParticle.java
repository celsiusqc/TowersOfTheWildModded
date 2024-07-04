package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CritParticle extends TextureSheetParticle {
    CritParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
        super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
        this.friction = 0.7F;
        this.gravity = 0.5F;
        this.xd *= 0.1F;
        this.yd *= 0.1F;
        this.zd *= 0.1F;
        this.xd += pXSpeed * 0.4;
        this.yd += pYSpeed * 0.4;
        this.zd += pZSpeed * 0.4;
        float f = (float)(Math.random() * 0.3F + 0.6F);
        this.rCol = f;
        this.gCol = f;
        this.bCol = f;
        this.quadSize *= 0.75F;
        this.lifetime = Math.max((int)(6.0 / (Math.random() * 0.8 + 0.6)), 1);
        this.hasPhysics = false;
        this.tick();
    }

    @Override
    public float getQuadSize(float pScaleFactor) {
        return this.quadSize * Mth.clamp(((float)this.age + pScaleFactor) / (float)this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.gCol *= 0.96F;
        this.bCol *= 0.9F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class DamageIndicatorProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DamageIndicatorProvider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

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
            CritParticle critparticle = new CritParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed + 1.0, pZSpeed);
            critparticle.setLifetime(20);
            critparticle.pickSprite(this.sprite);
            return critparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class MagicProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public MagicProvider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

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
            CritParticle critparticle = new CritParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            critparticle.rCol *= 0.3F;
            critparticle.gCol *= 0.8F;
            critparticle.pickSprite(this.sprite);
            return critparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

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
            CritParticle critparticle = new CritParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            critparticle.pickSprite(this.sprite);
            return critparticle;
        }
    }
}
