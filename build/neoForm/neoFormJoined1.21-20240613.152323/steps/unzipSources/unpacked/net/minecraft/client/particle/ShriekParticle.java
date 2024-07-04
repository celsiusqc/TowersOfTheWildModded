package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class ShriekParticle extends TextureSheetParticle {
    private static final float MAGICAL_X_ROT = 1.0472F;
    private int delay;

    ShriekParticle(ClientLevel pLevel, double pX, double pY, double pZ, int pDelay) {
        super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
        this.quadSize = 0.85F;
        this.delay = pDelay;
        this.lifetime = 30;
        this.gravity = 0.0F;
        this.xd = 0.0;
        this.yd = 0.1;
        this.zd = 0.0;
    }

    @Override
    public float getQuadSize(float pScaleFactor) {
        return this.quadSize * Mth.clamp(((float)this.age + pScaleFactor) / (float)this.lifetime * 0.75F, 0.0F, 1.0F);
    }

    @Override
    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        if (this.delay <= 0) {
            this.alpha = 1.0F - Mth.clamp(((float)this.age + pPartialTicks) / (float)this.lifetime, 0.0F, 1.0F);
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotationX(-1.0472F);
            this.renderRotatedQuad(pBuffer, pRenderInfo, quaternionf, pPartialTicks);
            quaternionf.rotationYXZ((float) -Math.PI, 1.0472F, 0.0F);
            this.renderRotatedQuad(pBuffer, pRenderInfo, quaternionf, pPartialTicks);
        }
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return 240;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        if (this.delay > 0) {
            this.delay--;
        } else {
            super.tick();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<ShriekParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet pSprite) {
            this.sprite = pSprite;
        }

        public Particle createParticle(
            ShriekParticleOption pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            ShriekParticle shriekparticle = new ShriekParticle(pLevel, pX, pY, pZ, pType.getDelay());
            shriekparticle.pickSprite(this.sprite);
            shriekparticle.setAlpha(1.0F);
            return shriekparticle;
        }
    }
}
