package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import java.util.Optional;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OutlineBufferSource implements MultiBufferSource {
    private final MultiBufferSource.BufferSource bufferSource;
    private final MultiBufferSource.BufferSource outlineBufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
    private int teamR = 255;
    private int teamG = 255;
    private int teamB = 255;
    private int teamA = 255;

    public OutlineBufferSource(MultiBufferSource.BufferSource pBufferSource) {
        this.bufferSource = pBufferSource;
    }

    @Override
    public VertexConsumer getBuffer(RenderType pRenderType) {
        if (pRenderType.isOutline()) {
            VertexConsumer vertexconsumer2 = this.outlineBufferSource.getBuffer(pRenderType);
            return new OutlineBufferSource.EntityOutlineGenerator(vertexconsumer2, this.teamR, this.teamG, this.teamB, this.teamA);
        } else {
            VertexConsumer vertexconsumer = this.bufferSource.getBuffer(pRenderType);
            Optional<RenderType> optional = pRenderType.outline();
            if (optional.isPresent()) {
                VertexConsumer vertexconsumer1 = this.outlineBufferSource.getBuffer(optional.get());
                OutlineBufferSource.EntityOutlineGenerator outlinebuffersource$entityoutlinegenerator = new OutlineBufferSource.EntityOutlineGenerator(
                    vertexconsumer1, this.teamR, this.teamG, this.teamB, this.teamA
                );
                return VertexMultiConsumer.create(outlinebuffersource$entityoutlinegenerator, vertexconsumer);
            } else {
                return vertexconsumer;
            }
        }
    }

    public void setColor(int pRed, int pGreen, int pBlue, int pAlpha) {
        this.teamR = pRed;
        this.teamG = pGreen;
        this.teamB = pBlue;
        this.teamA = pAlpha;
    }

    public void endOutlineBatch() {
        this.outlineBufferSource.endBatch();
    }

    @OnlyIn(Dist.CLIENT)
    static record EntityOutlineGenerator(VertexConsumer delegate, int color) implements VertexConsumer {
        public EntityOutlineGenerator(VertexConsumer p_109943_, int p_109944_, int p_109945_, int p_109946_, int p_109947_) {
            this(p_109943_, FastColor.ARGB32.color(p_109947_, p_109944_, p_109945_, p_109946_));
        }

        @Override
        public VertexConsumer addVertex(float p_350357_, float p_350369_, float p_350557_) {
            this.delegate.addVertex(p_350357_, p_350369_, p_350557_).setColor(this.color);
            return this;
        }

        @Override
        public VertexConsumer setColor(int p_350802_, int p_351011_, int p_350273_, int p_351040_) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float p_350507_, float p_350470_) {
            this.delegate.setUv(p_350507_, p_350470_);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int p_350412_, int p_350568_) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int p_350636_, int p_351006_) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float p_350484_, float p_350765_, float p_350737_) {
            return this;
        }
    }
}