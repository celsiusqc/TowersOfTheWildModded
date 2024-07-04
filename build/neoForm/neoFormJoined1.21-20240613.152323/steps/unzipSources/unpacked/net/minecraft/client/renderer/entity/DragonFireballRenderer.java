package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DragonFireballRenderer extends EntityRenderer<DragonFireball> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon_fireball.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE_LOCATION);

    public DragonFireballRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    protected int getBlockLightLevel(DragonFireball pEntity, BlockPos pPos) {
        return 15;
    }

    public void render(DragonFireball pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        pPoseStack.pushPose();
        pPoseStack.scale(2.0F, 2.0F, 2.0F);
        pPoseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose posestack$pose = pPoseStack.last();
        VertexConsumer vertexconsumer = pBuffer.getBuffer(RENDER_TYPE);
        vertex(vertexconsumer, posestack$pose, pPackedLight, 0.0F, 0, 0, 1);
        vertex(vertexconsumer, posestack$pose, pPackedLight, 1.0F, 0, 1, 1);
        vertex(vertexconsumer, posestack$pose, pPackedLight, 1.0F, 1, 1, 0);
        vertex(vertexconsumer, posestack$pose, pPackedLight, 0.0F, 1, 0, 0);
        pPoseStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    private static void vertex(VertexConsumer pConsumer, PoseStack.Pose pPose, int pPackedLight, float pX, int pY, int pU, int pV) {
        pConsumer.addVertex(pPose, pX - 0.5F, (float)pY - 0.25F, 0.0F)
            .setColor(-1)
            .setUv((float)pU, (float)pV)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal(pPose, 0.0F, 1.0F, 0.0F);
    }

    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getTextureLocation(DragonFireball pEntity) {
        return TEXTURE_LOCATION;
    }
}
