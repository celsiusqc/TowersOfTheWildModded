package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AgeableHierarchicalModel<E extends Entity> extends HierarchicalModel<E> {
    private final float youngScaleFactor;
    private final float bodyYOffset;

    public AgeableHierarchicalModel(float pYoungScaleFactor, float pBodyYOffset) {
        this(pYoungScaleFactor, pBodyYOffset, RenderType::entityCutoutNoCull);
    }

    public AgeableHierarchicalModel(float pYoungScaleFactor, float pBodyYOffset, Function<ResourceLocation, RenderType> pRenderType) {
        super(pRenderType);
        this.bodyYOffset = pBodyYOffset;
        this.youngScaleFactor = pYoungScaleFactor;
    }

    @Override
    public void renderToBuffer(PoseStack p_273029_, VertexConsumer p_272763_, int p_273665_, int p_272602_, int p_350346_) {
        if (this.young) {
            p_273029_.pushPose();
            p_273029_.scale(this.youngScaleFactor, this.youngScaleFactor, this.youngScaleFactor);
            p_273029_.translate(0.0F, this.bodyYOffset / 16.0F, 0.0F);
            this.root().render(p_273029_, p_272763_, p_273665_, p_272602_, p_350346_);
            p_273029_.popPose();
        } else {
            this.root().render(p_273029_, p_272763_, p_273665_, p_272602_, p_350346_);
        }
    }
}
