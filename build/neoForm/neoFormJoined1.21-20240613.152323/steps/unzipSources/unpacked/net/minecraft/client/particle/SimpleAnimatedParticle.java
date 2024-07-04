package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleAnimatedParticle extends TextureSheetParticle {
    protected final SpriteSet sprites;
    private float fadeR;
    private float fadeG;
    private float fadeB;
    private boolean hasFade;

    protected SimpleAnimatedParticle(ClientLevel pLevel, double pX, double pY, double pZ, SpriteSet pSprites, float pGravity) {
        super(pLevel, pX, pY, pZ);
        this.friction = 0.91F;
        this.gravity = pGravity;
        this.sprites = pSprites;
    }

    public void setColor(int pColor) {
        float f = (float)((pColor & 0xFF0000) >> 16) / 255.0F;
        float f1 = (float)((pColor & 0xFF00) >> 8) / 255.0F;
        float f2 = (float)((pColor & 0xFF) >> 0) / 255.0F;
        float f3 = 1.0F;
        this.setColor(f * 1.0F, f1 * 1.0F, f2 * 1.0F);
    }

    /**
     * Sets a color for the particle to drift toward (20% closer each tick, never actually getting very close)
     */
    public void setFadeColor(int pRgb) {
        this.fadeR = (float)((pRgb & 0xFF0000) >> 16) / 255.0F;
        this.fadeG = (float)((pRgb & 0xFF00) >> 8) / 255.0F;
        this.fadeB = (float)((pRgb & 0xFF) >> 0) / 255.0F;
        this.hasFade = true;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        if (this.age > this.lifetime / 2) {
            this.setAlpha(1.0F - ((float)this.age - (float)(this.lifetime / 2)) / (float)this.lifetime);
            if (this.hasFade) {
                this.rCol = this.rCol + (this.fadeR - this.rCol) * 0.2F;
                this.gCol = this.gCol + (this.fadeG - this.gCol) * 0.2F;
                this.bCol = this.bCol + (this.fadeB - this.bCol) * 0.2F;
            }
        }
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return 15728880;
    }
}
