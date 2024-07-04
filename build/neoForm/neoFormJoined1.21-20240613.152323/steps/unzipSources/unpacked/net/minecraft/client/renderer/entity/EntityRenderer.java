package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public abstract class EntityRenderer<T extends Entity> {
    protected static final float NAMETAG_SCALE = 0.025F;
    public static final int LEASH_RENDER_STEPS = 24;
    protected final EntityRenderDispatcher entityRenderDispatcher;
    private final Font font;
    protected float shadowRadius;
    protected float shadowStrength = 1.0F;

    protected EntityRenderer(EntityRendererProvider.Context pContext) {
        this.entityRenderDispatcher = pContext.getEntityRenderDispatcher();
        this.font = pContext.getFont();
    }

    public final int getPackedLightCoords(T pEntity, float pPartialTicks) {
        BlockPos blockpos = BlockPos.containing(pEntity.getLightProbePosition(pPartialTicks));
        return LightTexture.pack(this.getBlockLightLevel(pEntity, blockpos), this.getSkyLightLevel(pEntity, blockpos));
    }

    protected int getSkyLightLevel(T pEntity, BlockPos pPos) {
        return pEntity.level().getBrightness(LightLayer.SKY, pPos);
    }

    protected int getBlockLightLevel(T pEntity, BlockPos pPos) {
        return pEntity.isOnFire() ? 15 : pEntity.level().getBrightness(LightLayer.BLOCK, pPos);
    }

    public boolean shouldRender(T pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        if (!pLivingEntity.shouldRender(pCamX, pCamY, pCamZ)) {
            return false;
        } else if (pLivingEntity.noCulling) {
            return true;
        } else {
            AABB aabb = pLivingEntity.getBoundingBoxForCulling().inflate(0.5);
            if (aabb.hasNaN() || aabb.getSize() == 0.0) {
                aabb = new AABB(
                    pLivingEntity.getX() - 2.0,
                    pLivingEntity.getY() - 2.0,
                    pLivingEntity.getZ() - 2.0,
                    pLivingEntity.getX() + 2.0,
                    pLivingEntity.getY() + 2.0,
                    pLivingEntity.getZ() + 2.0
                );
            }

            if (pCamera.isVisible(aabb)) {
                return true;
            } else {
                if (pLivingEntity instanceof Leashable leashable) {
                    Entity entity = leashable.getLeashHolder();
                    if (entity != null) {
                        return pCamera.isVisible(entity.getBoundingBoxForCulling());
                    }
                }

                return false;
            }
        }
    }

    public Vec3 getRenderOffset(T pEntity, float pPartialTicks) {
        return Vec3.ZERO;
    }

    public void render(T pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        if (pEntity instanceof Leashable leashable) {
            Entity entity = leashable.getLeashHolder();
            if (entity != null) {
                this.renderLeash(pEntity, pPartialTick, pPoseStack, pBuffer, entity);
            }
        }

        // Neo: Post the RenderNameTagEvent and conditionally wrap #renderNameTag based on the result.
        var event = new net.neoforged.neoforge.client.event.RenderNameTagEvent(pEntity, pEntity.getDisplayName(), this, pPoseStack, pBuffer, pPackedLight, pPartialTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        if (event.canRender().isTrue() || event.canRender().isDefault() && this.shouldShowName(pEntity)) {
            this.renderNameTag(pEntity, pEntity.getDisplayName(), pPoseStack, pBuffer, pPackedLight, pPartialTick);
        }
    }

    private <E extends Entity> void renderLeash(T p_352225_, float p_352465_, PoseStack p_352205_, MultiBufferSource p_352444_, E p_352269_) {
        p_352205_.pushPose();
        Vec3 vec3 = p_352269_.getRopeHoldPosition(p_352465_);
        double d0 = (double)(p_352225_.getPreciseBodyRotation(p_352465_) * (float) (Math.PI / 180.0)) + (Math.PI / 2);
        Vec3 vec31 = p_352225_.getLeashOffset(p_352465_);
        double d1 = Math.cos(d0) * vec31.z + Math.sin(d0) * vec31.x;
        double d2 = Math.sin(d0) * vec31.z - Math.cos(d0) * vec31.x;
        double d3 = Mth.lerp((double)p_352465_, p_352225_.xo, p_352225_.getX()) + d1;
        double d4 = Mth.lerp((double)p_352465_, p_352225_.yo, p_352225_.getY()) + vec31.y;
        double d5 = Mth.lerp((double)p_352465_, p_352225_.zo, p_352225_.getZ()) + d2;
        p_352205_.translate(d1, vec31.y, d2);
        float f = (float)(vec3.x - d3);
        float f1 = (float)(vec3.y - d4);
        float f2 = (float)(vec3.z - d5);
        float f3 = 0.025F;
        VertexConsumer vertexconsumer = p_352444_.getBuffer(RenderType.leash());
        Matrix4f matrix4f = p_352205_.last().pose();
        float f4 = Mth.invSqrt(f * f + f2 * f2) * 0.025F / 2.0F;
        float f5 = f2 * f4;
        float f6 = f * f4;
        BlockPos blockpos = BlockPos.containing(p_352225_.getEyePosition(p_352465_));
        BlockPos blockpos1 = BlockPos.containing(p_352269_.getEyePosition(p_352465_));
        int i = this.getBlockLightLevel(p_352225_, blockpos);
        int j = this.entityRenderDispatcher.getRenderer(p_352269_).getBlockLightLevel(p_352269_, blockpos1);
        int k = p_352225_.level().getBrightness(LightLayer.SKY, blockpos);
        int l = p_352225_.level().getBrightness(LightLayer.SKY, blockpos1);

        for (int i1 = 0; i1 <= 24; i1++) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.025F, f5, f6, i1, false);
        }

        for (int j1 = 24; j1 >= 0; j1--) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.0F, f5, f6, j1, true);
        }

        p_352205_.popPose();
    }

    private static void addVertexPair(
        VertexConsumer p_352095_,
        Matrix4f p_352142_,
        float p_352462_,
        float p_352226_,
        float p_352086_,
        int p_352406_,
        int p_352470_,
        int p_352371_,
        int p_352167_,
        float p_352293_,
        float p_352138_,
        float p_352315_,
        float p_352162_,
        int p_352291_,
        boolean p_352079_
    ) {
        float f = (float)p_352291_ / 24.0F;
        int i = (int)Mth.lerp(f, (float)p_352406_, (float)p_352470_);
        int j = (int)Mth.lerp(f, (float)p_352371_, (float)p_352167_);
        int k = LightTexture.pack(i, j);
        float f1 = p_352291_ % 2 == (p_352079_ ? 1 : 0) ? 0.7F : 1.0F;
        float f2 = 0.5F * f1;
        float f3 = 0.4F * f1;
        float f4 = 0.3F * f1;
        float f5 = p_352462_ * f;
        float f6 = p_352226_ > 0.0F ? p_352226_ * f * f : p_352226_ - p_352226_ * (1.0F - f) * (1.0F - f);
        float f7 = p_352086_ * f;
        p_352095_.addVertex(p_352142_, f5 - p_352315_, f6 + p_352138_, f7 + p_352162_).setColor(f2, f3, f4, 1.0F).setLight(k);
        p_352095_.addVertex(p_352142_, f5 + p_352315_, f6 + p_352293_ - p_352138_, f7 - p_352162_).setColor(f2, f3, f4, 1.0F).setLight(k);
    }

    protected boolean shouldShowName(T pEntity) {
        return pEntity.shouldShowName() || pEntity.hasCustomName() && pEntity == this.entityRenderDispatcher.crosshairPickEntity;
    }

    /**
     * Returns the location of an entity's texture.
     */
    public abstract ResourceLocation getTextureLocation(T pEntity);

    public Font getFont() {
        return this.font;
    }

    protected void renderNameTag(T pEntity, Component pDisplayName, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, float pPartialTick) {
        double d0 = this.entityRenderDispatcher.distanceToSqr(pEntity);
        if (net.neoforged.neoforge.client.ClientHooks.isNameplateInRenderDistance(pEntity, d0)) {
            Vec3 vec3 = pEntity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, pEntity.getViewYRot(pPartialTick));
            if (vec3 != null) {
                boolean flag = !pEntity.isDiscrete();
                int i = "deadmau5".equals(pDisplayName.getString()) ? -10 : 0;
                pPoseStack.pushPose();
                pPoseStack.translate(vec3.x, vec3.y + 0.5, vec3.z);
                pPoseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
                pPoseStack.scale(0.025F, -0.025F, 0.025F);
                Matrix4f matrix4f = pPoseStack.last().pose();
                float f = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
                int j = (int)(f * 255.0F) << 24;
                Font font = this.getFont();
                float f1 = (float)(-font.width(pDisplayName) / 2);
                font.drawInBatch(
                    pDisplayName, f1, (float)i, 553648127, false, matrix4f, pBuffer, flag ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, j, pPackedLight
                );
                if (flag) {
                    font.drawInBatch(pDisplayName, f1, (float)i, -1, false, matrix4f, pBuffer, Font.DisplayMode.NORMAL, 0, pPackedLight);
                }

                pPoseStack.popPose();
            }
        }
    }

    protected float getShadowRadius(T pEntity) {
        return this.shadowRadius;
    }
}
