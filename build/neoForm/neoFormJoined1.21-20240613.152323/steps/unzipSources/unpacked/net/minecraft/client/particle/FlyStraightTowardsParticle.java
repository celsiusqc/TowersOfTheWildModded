package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlyStraightTowardsParticle extends TextureSheetParticle {
    private final double xStart;
    private final double yStart;
    private final double zStart;
    private final int startColor;
    private final int endColor;

    FlyStraightTowardsParticle(
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed,
        int pStartColor,
        int pEndColor
    ) {
        super(pLevel, pX, pY, pZ);
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
        this.hasPhysics = false;
        this.lifetime = (int)(Math.random() * 5.0) + 25;
        this.startColor = pStartColor;
        this.endColor = pEndColor;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void move(double pX, double pY, double pZ) {
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return 240;
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
            float f1 = 1.0F - f;
            this.x = this.xStart + this.xd * (double)f1;
            this.y = this.yStart + this.yd * (double)f1;
            this.z = this.zStart + this.zd * (double)f1;
            int i = FastColor.ARGB32.lerp(f, this.startColor, this.endColor);
            this.setColor((float)FastColor.ARGB32.red(i) / 255.0F, (float)FastColor.ARGB32.green(i) / 255.0F, (float)FastColor.ARGB32.blue(i) / 255.0F);
            this.setAlpha((float)FastColor.ARGB32.alpha(i) / 255.0F);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class OminousSpawnProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public OminousSpawnProvider(SpriteSet pSprite) {
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
            FlyStraightTowardsParticle flystraighttowardsparticle = new FlyStraightTowardsParticle(
                pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, -12210434, -1
            );
            flystraighttowardsparticle.scale(Mth.randomBetween(pLevel.getRandom(), 3.0F, 5.0F));
            flystraighttowardsparticle.pickSprite(this.sprite);
            return flystraighttowardsparticle;
        }
    }
}
