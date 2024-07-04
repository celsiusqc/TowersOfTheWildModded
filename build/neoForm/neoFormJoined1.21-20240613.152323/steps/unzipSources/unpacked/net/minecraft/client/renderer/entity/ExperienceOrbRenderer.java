package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExperienceOrbRenderer extends EntityRenderer<ExperienceOrb> {
    private static final ResourceLocation EXPERIENCE_ORB_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/experience_orb.png");
    private static final RenderType RENDER_TYPE = RenderType.itemEntityTranslucentCull(EXPERIENCE_ORB_LOCATION);

    public ExperienceOrbRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }

    protected int getBlockLightLevel(ExperienceOrb pEntity, BlockPos pPos) {
        return Mth.clamp(super.getBlockLightLevel(pEntity, pPos) + 7, 0, 15);
    }

    public void render(ExperienceOrb pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        pPoseStack.pushPose();
        int i = pEntity.getIcon();
        float f = (float)(i % 4 * 16 + 0) / 64.0F;
        float f1 = (float)(i % 4 * 16 + 16) / 64.0F;
        float f2 = (float)(i / 4 * 16 + 0) / 64.0F;
        float f3 = (float)(i / 4 * 16 + 16) / 64.0F;
        float f4 = 1.0F;
        float f5 = 0.5F;
        float f6 = 0.25F;
        float f7 = 255.0F;
        float f8 = ((float)pEntity.tickCount + pPartialTicks) / 2.0F;
        int j = (int)((Mth.sin(f8 + 0.0F) + 1.0F) * 0.5F * 255.0F);
        int k = 255;
        int l = (int)((Mth.sin(f8 + (float) (Math.PI * 4.0 / 3.0)) + 1.0F) * 0.1F * 255.0F);
        pPoseStack.translate(0.0F, 0.1F, 0.0F);
        pPoseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float f9 = 0.3F;
        pPoseStack.scale(0.3F, 0.3F, 0.3F);
        VertexConsumer vertexconsumer = pBuffer.getBuffer(RENDER_TYPE);
        PoseStack.Pose posestack$pose = pPoseStack.last();
        vertex(vertexconsumer, posestack$pose, -0.5F, -0.25F, j, 255, l, f, f3, pPackedLight);
        vertex(vertexconsumer, posestack$pose, 0.5F, -0.25F, j, 255, l, f1, f3, pPackedLight);
        vertex(vertexconsumer, posestack$pose, 0.5F, 0.75F, j, 255, l, f1, f2, pPackedLight);
        vertex(vertexconsumer, posestack$pose, -0.5F, 0.75F, j, 255, l, f, f2, pPackedLight);
        pPoseStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    private static void vertex(
        VertexConsumer pConsumer,
        PoseStack.Pose pPose,
        float pX,
        float pY,
        int pRed,
        int pGreen,
        int pBlue,
        float pU,
        float pV,
        int pPackedLight
    ) {
        pConsumer.addVertex(pPose, pX, pY, 0.0F)
            .setColor(pRed, pGreen, pBlue, 128)
            .setUv(pU, pV)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal(pPose, 0.0F, 1.0F, 0.0F);
    }

    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getTextureLocation(ExperienceOrb pEntity) {
        return EXPERIENCE_ORB_LOCATION;
    }
}
