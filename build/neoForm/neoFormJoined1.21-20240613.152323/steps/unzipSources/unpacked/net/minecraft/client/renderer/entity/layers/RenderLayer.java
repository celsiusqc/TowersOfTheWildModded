package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RenderLayer<T extends Entity, M extends EntityModel<T>> {
    private final RenderLayerParent<T, M> renderer;

    public RenderLayer(RenderLayerParent<T, M> pRenderer) {
        this.renderer = pRenderer;
    }

    protected static <T extends LivingEntity> void coloredCutoutModelCopyLayerRender(
        EntityModel<T> p_117360_,
        EntityModel<T> p_117361_,
        ResourceLocation p_117362_,
        PoseStack p_117363_,
        MultiBufferSource p_117364_,
        int p_117365_,
        T p_117366_,
        float p_117367_,
        float p_117368_,
        float p_117369_,
        float p_117370_,
        float p_117371_,
        float p_117372_,
        int p_350559_
    ) {
        if (!p_117366_.isInvisible()) {
            p_117360_.copyPropertiesTo(p_117361_);
            p_117361_.prepareMobModel(p_117366_, p_117367_, p_117368_, p_117372_);
            p_117361_.setupAnim(p_117366_, p_117367_, p_117368_, p_117369_, p_117370_, p_117371_);
            renderColoredCutoutModel(p_117361_, p_117362_, p_117363_, p_117364_, p_117365_, p_117366_, p_350559_);
        }
    }

    protected static <T extends LivingEntity> void renderColoredCutoutModel(
        EntityModel<T> p_117377_, ResourceLocation p_117378_, PoseStack p_117379_, MultiBufferSource p_117380_, int p_117381_, T p_117382_, int p_350384_
    ) {
        VertexConsumer vertexconsumer = p_117380_.getBuffer(RenderType.entityCutoutNoCull(p_117378_));
        p_117377_.renderToBuffer(p_117379_, vertexconsumer, p_117381_, LivingEntityRenderer.getOverlayCoords(p_117382_, 0.0F), p_350384_);
    }

    public M getParentModel() {
        return this.renderer.getModel();
    }

    protected ResourceLocation getTextureLocation(T pEntity) {
        return this.renderer.getTextureLocation(pEntity);
    }

    public abstract void render(
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        T pLivingEntity,
        float pLimbSwing,
        float pLimbSwingAmount,
        float pPartialTick,
        float pAgeInTicks,
        float pNetHeadYaw,
        float pHeadPitch
    );
}
