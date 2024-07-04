package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DustPlumeParticle extends BaseAshSmokeParticle {
    private static final int COLOR_RGB24 = 12235202;

    protected DustPlumeParticle(
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed,
        float pQuadSizeMultiplier,
        SpriteSet pSprites
    ) {
        super(pLevel, pX, pY, pZ, 0.7F, 0.6F, 0.7F, pXSpeed, pYSpeed + 0.15F, pZSpeed, pQuadSizeMultiplier, pSprites, 0.5F, 7, 0.5F, false);
        float f = (float)Math.random() * 0.2F;
        this.rCol = (float)FastColor.ARGB32.red(12235202) / 255.0F - f;
        this.gCol = (float)FastColor.ARGB32.green(12235202) / 255.0F - f;
        this.bCol = (float)FastColor.ARGB32.blue(12235202) / 255.0F - f;
    }

    @Override
    public void tick() {
        this.gravity = 0.88F * this.gravity;
        this.friction = 0.92F * this.friction;
        super.tick();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet pSprites) {
            this.sprites = pSprites;
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
            return new DustPlumeParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, 1.0F, this.sprites);
        }
    }
}
