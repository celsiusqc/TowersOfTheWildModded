package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AgeableListModel<E extends Entity> extends EntityModel<E> {
    private final boolean scaleHead;
    private final float babyYHeadOffset;
    private final float babyZHeadOffset;
    private final float babyHeadScale;
    private final float babyBodyScale;
    private final float bodyYOffset;

    protected AgeableListModel(boolean pScaleHead, float pBabyYHeadOffset, float pBabyZHeadOffset) {
        this(pScaleHead, pBabyYHeadOffset, pBabyZHeadOffset, 2.0F, 2.0F, 24.0F);
    }

    protected AgeableListModel(boolean pScaleHead, float pBabyYHeadOffset, float pBabyZHeadOffset, float pBabyHeadScale, float pBabyBodyScale, float pBodyYOffset) {
        this(RenderType::entityCutoutNoCull, pScaleHead, pBabyYHeadOffset, pBabyZHeadOffset, pBabyHeadScale, pBabyBodyScale, pBodyYOffset);
    }

    protected AgeableListModel(
        Function<ResourceLocation, RenderType> pRenderType,
        boolean pScaleHead,
        float pBabyYHeadOffset,
        float pBabyZHeadOffset,
        float pBabyHeadScale,
        float pBabyBodyScale,
        float pBodyYOffset
    ) {
        super(pRenderType);
        this.scaleHead = pScaleHead;
        this.babyYHeadOffset = pBabyYHeadOffset;
        this.babyZHeadOffset = pBabyZHeadOffset;
        this.babyHeadScale = pBabyHeadScale;
        this.babyBodyScale = pBabyBodyScale;
        this.bodyYOffset = pBodyYOffset;
    }

    protected AgeableListModel() {
        this(false, 5.0F, 2.0F);
    }

    @Override
    public void renderToBuffer(PoseStack p_102034_, VertexConsumer p_102035_, int p_102036_, int p_102037_, int p_350361_) {
        if (this.young) {
            p_102034_.pushPose();
            if (this.scaleHead) {
                float f = 1.5F / this.babyHeadScale;
                p_102034_.scale(f, f, f);
            }

            p_102034_.translate(0.0F, this.babyYHeadOffset / 16.0F, this.babyZHeadOffset / 16.0F);
            this.headParts().forEach(p_349807_ -> p_349807_.render(p_102034_, p_102035_, p_102036_, p_102037_, p_350361_));
            p_102034_.popPose();
            p_102034_.pushPose();
            float f1 = 1.0F / this.babyBodyScale;
            p_102034_.scale(f1, f1, f1);
            p_102034_.translate(0.0F, this.bodyYOffset / 16.0F, 0.0F);
            this.bodyParts().forEach(p_349825_ -> p_349825_.render(p_102034_, p_102035_, p_102036_, p_102037_, p_350361_));
            p_102034_.popPose();
        } else {
            this.headParts().forEach(p_349819_ -> p_349819_.render(p_102034_, p_102035_, p_102036_, p_102037_, p_350361_));
            this.bodyParts().forEach(p_349813_ -> p_349813_.render(p_102034_, p_102035_, p_102036_, p_102037_, p_350361_));
        }
    }

    protected abstract Iterable<ModelPart> headParts();

    protected abstract Iterable<ModelPart> bodyParts();
}
