package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class BakedGlyph {
    private final GlyphRenderTypes renderTypes;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private final float left;
    private final float right;
    private final float up;
    private final float down;

    public BakedGlyph(
        GlyphRenderTypes pRenderTypes,
        float pU0,
        float pU1,
        float pV0,
        float pV1,
        float pLeft,
        float pRight,
        float pUp,
        float pDown
    ) {
        this.renderTypes = pRenderTypes;
        this.u0 = pU0;
        this.u1 = pU1;
        this.v0 = pV0;
        this.v1 = pV1;
        this.left = pLeft;
        this.right = pRight;
        this.up = pUp;
        this.down = pDown;
    }

    public void render(
        boolean pItalic,
        float pX,
        float pY,
        Matrix4f pMatrix,
        VertexConsumer pBuffer,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha,
        int pPackedLight
    ) {
        float f = pX + this.left;
        float f1 = pX + this.right;
        float f2 = pY + this.up;
        float f3 = pY + this.down;
        float f4 = pItalic ? 1.0F - 0.25F * this.up : 0.0F;
        float f5 = pItalic ? 1.0F - 0.25F * this.down : 0.0F;
        pBuffer.addVertex(pMatrix, f + f4, f2, 0.0F).setColor(pRed, pGreen, pBlue, pAlpha).setUv(this.u0, this.v0).setLight(pPackedLight);
        pBuffer.addVertex(pMatrix, f + f5, f3, 0.0F).setColor(pRed, pGreen, pBlue, pAlpha).setUv(this.u0, this.v1).setLight(pPackedLight);
        pBuffer.addVertex(pMatrix, f1 + f5, f3, 0.0F).setColor(pRed, pGreen, pBlue, pAlpha).setUv(this.u1, this.v1).setLight(pPackedLight);
        pBuffer.addVertex(pMatrix, f1 + f4, f2, 0.0F).setColor(pRed, pGreen, pBlue, pAlpha).setUv(this.u1, this.v0).setLight(pPackedLight);
    }

    public void renderEffect(BakedGlyph.Effect pEffect, Matrix4f pMatrix, VertexConsumer pBuffer, int pPackedLight) {
        pBuffer.addVertex(pMatrix, pEffect.x0, pEffect.y0, pEffect.depth)
            .setColor(pEffect.r, pEffect.g, pEffect.b, pEffect.a)
            .setUv(this.u0, this.v0)
            .setLight(pPackedLight);
        pBuffer.addVertex(pMatrix, pEffect.x1, pEffect.y0, pEffect.depth)
            .setColor(pEffect.r, pEffect.g, pEffect.b, pEffect.a)
            .setUv(this.u0, this.v1)
            .setLight(pPackedLight);
        pBuffer.addVertex(pMatrix, pEffect.x1, pEffect.y1, pEffect.depth)
            .setColor(pEffect.r, pEffect.g, pEffect.b, pEffect.a)
            .setUv(this.u1, this.v1)
            .setLight(pPackedLight);
        pBuffer.addVertex(pMatrix, pEffect.x0, pEffect.y1, pEffect.depth)
            .setColor(pEffect.r, pEffect.g, pEffect.b, pEffect.a)
            .setUv(this.u1, this.v0)
            .setLight(pPackedLight);
    }

    public RenderType renderType(Font.DisplayMode pDisplayMode) {
        return this.renderTypes.select(pDisplayMode);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Effect {
        protected final float x0;
        protected final float y0;
        protected final float x1;
        protected final float y1;
        protected final float depth;
        protected final float r;
        protected final float g;
        protected final float b;
        protected final float a;

        public Effect(
            float pX0, float pY0, float pX1, float pY1, float pDepth, float pR, float pG, float pB, float pA
        ) {
            this.x0 = pX0;
            this.y0 = pY0;
            this.x1 = pX1;
            this.y1 = pY1;
            this.depth = pDepth;
            this.r = pR;
            this.g = pG;
            this.b = pB;
            this.a = pA;
        }
    }
}
