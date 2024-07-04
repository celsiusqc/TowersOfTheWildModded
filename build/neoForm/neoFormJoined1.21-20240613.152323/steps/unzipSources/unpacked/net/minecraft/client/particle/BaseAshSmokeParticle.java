package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BaseAshSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected BaseAshSmokeParticle(
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        float pXSeedMultiplier,
        float pYSpeedMultiplier,
        float pZSpeedMultiplier,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed,
        float pQuadSizeMultiplier,
        SpriteSet pSprites,
        float pRColMultiplier,
        int pLifetime,
        float pGravity,
        boolean pHasPhysics
    ) {
        super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
        this.friction = 0.96F;
        this.gravity = pGravity;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = pSprites;
        this.xd *= (double)pXSeedMultiplier;
        this.yd *= (double)pYSpeedMultiplier;
        this.zd *= (double)pZSpeedMultiplier;
        this.xd += pXSpeed;
        this.yd += pYSpeed;
        this.zd += pZSpeed;
        float f = pLevel.random.nextFloat() * pRColMultiplier;
        this.rCol = f;
        this.gCol = f;
        this.bCol = f;
        this.quadSize *= 0.75F * pQuadSizeMultiplier;
        this.lifetime = (int)((double)pLifetime / ((double)pLevel.random.nextFloat() * 0.8 + 0.2) * (double)pQuadSizeMultiplier);
        this.lifetime = Math.max(this.lifetime, 1);
        this.setSpriteFromAge(pSprites);
        this.hasPhysics = pHasPhysics;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public float getQuadSize(float pScaleFactor) {
        return this.quadSize * Mth.clamp(((float)this.age + pScaleFactor) / (float)this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }
}
