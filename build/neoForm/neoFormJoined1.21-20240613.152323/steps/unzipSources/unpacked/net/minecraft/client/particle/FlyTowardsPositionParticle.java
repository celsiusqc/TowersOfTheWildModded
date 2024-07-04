package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlyTowardsPositionParticle extends TextureSheetParticle {
    private final double xStart;
    private final double yStart;
    private final double zStart;
    private final boolean isGlowing;
    private final Particle.LifetimeAlpha lifetimeAlpha;

    FlyTowardsPositionParticle(
        ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
    ) {
        this(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, false, Particle.LifetimeAlpha.ALWAYS_OPAQUE);
    }

    FlyTowardsPositionParticle(
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed,
        boolean pIsGlowing,
        Particle.LifetimeAlpha pLifetimeAlpha
    ) {
        super(pLevel, pX, pY, pZ);
        this.isGlowing = pIsGlowing;
        this.lifetimeAlpha = pLifetimeAlpha;
        this.setAlpha(pLifetimeAlpha.startAlpha());
        this.xd = pXSpeed;
        this.yd = pYSpeed;
        this.zd = pZSpeed;
        this.xStart = pX;
        this.yStart = pY;
        this.zStart = pZ;
        this.xo = pX + pXSpeed;
        this.yo = pY + pYSpeed;
        this.zo = pZ + pZSpeed;
        this.x = this.xo;
        this.y = this.yo;
        this.z = this.zo;
        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.2F);
        float f = this.random.nextFloat() * 0.6F + 0.4F;
        this.rCol = 0.9F * f;
        this.gCol = 0.9F * f;
        this.bCol = f;
        this.hasPhysics = false;
        this.lifetime = (int)(Math.random() * 10.0) + 30;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return this.lifetimeAlpha.isOpaque() ? ParticleRenderType.PARTICLE_SHEET_OPAQUE : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void move(double pX, double pY, double pZ) {
        this.setBoundingBox(this.getBoundingBox().move(pX, pY, pZ));
        this.setLocationFromBoundingbox();
    }

    @Override
    public int getLightColor(float pPartialTick) {
        if (this.isGlowing) {
            return 240;
        } else {
            int i = super.getLightColor(pPartialTick);
            float f = (float)this.age / (float)this.lifetime;
            f *= f;
            f *= f;
            int j = i & 0xFF;
            int k = i >> 16 & 0xFF;
            k += (int)(f * 15.0F * 16.0F);
            if (k > 240) {
                k = 240;
            }

            return j | k << 16;
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            float f = (float)this.age / (float)this.lifetime;
            f = 1.0F - f;
            float f1 = 1.0F - f;
            f1 *= f1;
            f1 *= f1;
            this.x = this.xStart + this.xd * (double)f;
            this.y = this.yStart + this.yd * (double)f - (double)(f1 * 1.2F);
            this.z = this.zStart + this.zd * (double)f;
        }
    }

    @Override
    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        this.setAlpha(this.lifetimeAlpha.currentAlphaForAge(this.age, this.lifetime, pPartialTicks));
        super.render(pBuffer, pRenderInfo, pPartialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    public static class EnchantProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public EnchantProvider(SpriteSet pSprite) {
            this.sprite = pSprite;
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
            FlyTowardsPositionParticle flytowardspositionparticle = new FlyTowardsPositionParticle(
                pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed
            );
            flytowardspositionparticle.pickSprite(this.sprite);
            return flytowardspositionparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class NautilusProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public NautilusProvider(SpriteSet pSprite) {
            this.sprite = pSprite;
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
            FlyTowardsPositionParticle flytowardspositionparticle = new FlyTowardsPositionParticle(
                pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed
            );
            flytowardspositionparticle.pickSprite(this.sprite);
            return flytowardspositionparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class VaultConnectionProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public VaultConnectionProvider(SpriteSet pSprite) {
            this.sprite = pSprite;
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
            FlyTowardsPositionParticle flytowardspositionparticle = new FlyTowardsPositionParticle(
                pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, true, new Particle.LifetimeAlpha(0.0F, 0.6F, 0.25F, 1.0F)
            );
            flytowardspositionparticle.scale(1.5F);
            flytowardspositionparticle.pickSprite(this.sprite);
            return flytowardspositionparticle;
        }
    }
}
