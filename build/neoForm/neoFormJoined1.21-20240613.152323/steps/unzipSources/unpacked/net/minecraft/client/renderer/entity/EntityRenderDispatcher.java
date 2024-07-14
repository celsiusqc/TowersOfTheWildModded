package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class EntityRenderDispatcher implements ResourceManagerReloadListener {
    private static final RenderType SHADOW_RENDER_TYPE = RenderType.entityShadow(ResourceLocation.withDefaultNamespace("textures/misc/shadow.png"));
    private static final float MAX_SHADOW_RADIUS = 32.0F;
    private static final float SHADOW_POWER_FALLOFF_Y = 0.5F;
    private Map<EntityType<?>, EntityRenderer<?>> renderers = ImmutableMap.of();
    /**
     * lists the various player skin types with their associated Renderer class instances.
     */
    private Map<PlayerSkin.Model, EntityRenderer<? extends Player>> playerRenderers = Map.of();
    public final TextureManager textureManager;
    private Level level;
    public Camera camera;
    private Quaternionf cameraOrientation;
    public Entity crosshairPickEntity;
    private final ItemRenderer itemRenderer;
    private final BlockRenderDispatcher blockRenderDispatcher;
    private final ItemInHandRenderer itemInHandRenderer;
    private final Font font;
    public final Options options;
    private final EntityModelSet entityModels;
    private boolean shouldRenderShadow = true;
    private boolean renderHitBoxes;

    public <E extends Entity> int getPackedLightCoords(E pEntity, float pPartialTicks) {
        return this.getRenderer(pEntity).getPackedLightCoords(pEntity, pPartialTicks);
    }

    public EntityRenderDispatcher(
        Minecraft pMinecraft,
        TextureManager pTextureManager,
        ItemRenderer pItemRenderer,
        BlockRenderDispatcher pBlockRenderDispatcher,
        Font pFont,
        Options pOptions,
        EntityModelSet pEntityModels
    ) {
        this.textureManager = pTextureManager;
        this.itemRenderer = pItemRenderer;
        this.itemInHandRenderer = new ItemInHandRenderer(pMinecraft, this, pItemRenderer);
        this.blockRenderDispatcher = pBlockRenderDispatcher;
        this.font = pFont;
        this.options = pOptions;
        this.entityModels = pEntityModels;
    }

    public <T extends Entity> EntityRenderer<? super T> getRenderer(T pEntity) {
        if (pEntity instanceof AbstractClientPlayer abstractclientplayer) {
            PlayerSkin.Model playerskin$model = abstractclientplayer.getSkin().model();
            EntityRenderer<? extends Player> entityrenderer = this.playerRenderers.get(playerskin$model);
            return (EntityRenderer<? super T>)(entityrenderer != null ? entityrenderer : this.playerRenderers.get(PlayerSkin.Model.WIDE));
        } else {
            return (EntityRenderer<? super T>)this.renderers.get(pEntity.getType());
        }
    }

    public void prepare(Level pLevel, Camera pActiveRenderInfo, Entity pEntity) {
        this.level = pLevel;
        this.camera = pActiveRenderInfo;
        this.cameraOrientation = pActiveRenderInfo.rotation();
        this.crosshairPickEntity = pEntity;
    }

    public void overrideCameraOrientation(Quaternionf pCameraOrientation) {
        this.cameraOrientation = pCameraOrientation;
    }

    public void setRenderShadow(boolean pRenderShadow) {
        this.shouldRenderShadow = pRenderShadow;
    }

    public void setRenderHitBoxes(boolean pDebugBoundingBox) {
        this.renderHitBoxes = pDebugBoundingBox;
    }

    public boolean shouldRenderHitBoxes() {
        return this.renderHitBoxes;
    }

    public <E extends Entity> boolean shouldRender(E pEntity, Frustum pFrustum, double pCamX, double pCamY, double pCamZ) {
        EntityRenderer<? super E> entityrenderer = this.getRenderer(pEntity);
        return entityrenderer.shouldRender(pEntity, pFrustum, pCamX, pCamY, pCamZ);
    }

    public <E extends Entity> void render(
        E pEntity,
        double pX,
        double pY,
        double pZ,
        float pRotationYaw,
        float pPartialTicks,
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight
    ) {
        EntityRenderer<? super E> entityrenderer = this.getRenderer(pEntity);

        try {
            Vec3 vec3 = entityrenderer.getRenderOffset(pEntity, pPartialTicks);
            double d2 = pX + vec3.x();
            double d3 = pY + vec3.y();
            double d0 = pZ + vec3.z();
            pPoseStack.pushPose();
            pPoseStack.translate(d2, d3, d0);
            entityrenderer.render(pEntity, pRotationYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
            if (pEntity.displayFireAnimation()) {
                this.renderFlame(pPoseStack, pBuffer, pEntity, Mth.rotationAroundAxis(Mth.Y_AXIS, this.cameraOrientation, new Quaternionf()));
            }

            pPoseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
            if (this.options.entityShadows().get() && this.shouldRenderShadow && !pEntity.isInvisible()) {
                float f = entityrenderer.getShadowRadius(pEntity);
                if (f > 0.0F) {
                    double d1 = this.distanceToSqr(pEntity.getX(), pEntity.getY(), pEntity.getZ());
                    float f1 = (float)((1.0 - d1 / 256.0) * (double)entityrenderer.shadowStrength);
                    if (f1 > 0.0F) {
                        renderShadow(pPoseStack, pBuffer, pEntity, f1, pPartialTicks, this.level, Math.min(f, 32.0F));
                    }
                }
            }

            if (this.renderHitBoxes && !pEntity.isInvisible() && !Minecraft.getInstance().showOnlyReducedInfo()) {
                renderHitbox(pPoseStack, pBuffer.getBuffer(RenderType.lines()), pEntity, pPartialTicks, 1.0F, 1.0F, 1.0F);
            }

            pPoseStack.popPose();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering entity in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being rendered");
            pEntity.fillCrashReportCategory(crashreportcategory);
            CrashReportCategory crashreportcategory1 = crashreport.addCategory("Renderer details");
            crashreportcategory1.setDetail("Assigned renderer", entityrenderer);
            crashreportcategory1.setDetail("Location", CrashReportCategory.formatLocation(this.level, pX, pY, pZ));
            crashreportcategory1.setDetail("Rotation", pRotationYaw);
            crashreportcategory1.setDetail("Delta", pPartialTicks);
            throw new ReportedException(crashreport);
        }
    }

    private static void renderServerSideHitbox(PoseStack p_353069_, Entity p_353061_, MultiBufferSource p_353074_) {
        Entity entity = getServerSideEntity(p_353061_);
        if (entity == null) {
            DebugRenderer.renderFloatingText(p_353069_, p_353074_, "Missing", p_353061_.getX(), p_353061_.getBoundingBox().maxY + 1.5, p_353061_.getZ(), -65536);
        } else {
            p_353069_.pushPose();
            p_353069_.translate(entity.getX() - p_353061_.getX(), entity.getY() - p_353061_.getY(), entity.getZ() - p_353061_.getZ());
            renderHitbox(p_353069_, p_353074_.getBuffer(RenderType.lines()), entity, 1.0F, 0.0F, 1.0F, 0.0F);
            renderVector(p_353069_, p_353074_.getBuffer(RenderType.lines()), new Vector3f(), entity.getDeltaMovement(), -256);
            p_353069_.popPose();
        }
    }

    @Nullable
    private static Entity getServerSideEntity(Entity p_353043_) {
        IntegratedServer integratedserver = Minecraft.getInstance().getSingleplayerServer();
        if (integratedserver != null) {
            ServerLevel serverlevel = integratedserver.getLevel(p_353043_.level().dimension());
            if (serverlevel != null) {
                return serverlevel.getEntity(p_353043_.getId());
            }
        }

        return null;
    }

    private static void renderHitbox(
        PoseStack pPoseStack, VertexConsumer pBuffer, Entity pEntity, float pPartialTicks, float p_353064_, float p_353059_, float p_353042_
    ) {
        AABB aabb = pEntity.getBoundingBox().move(-pEntity.getX(), -pEntity.getY(), -pEntity.getZ());
        LevelRenderer.renderLineBox(pPoseStack, pBuffer, aabb, p_353064_, p_353059_, p_353042_, 1.0F);
        if (pEntity.isMultipartEntity()) {
            double d0 = -Mth.lerp((double)pPartialTicks, pEntity.xOld, pEntity.getX());
            double d1 = -Mth.lerp((double)pPartialTicks, pEntity.yOld, pEntity.getY());
            double d2 = -Mth.lerp((double)pPartialTicks, pEntity.zOld, pEntity.getZ());

            for (net.neoforged.neoforge.entity.PartEntity<?> enderdragonpart : pEntity.getParts()) {
                pPoseStack.pushPose();
                double d3 = d0 + Mth.lerp((double)pPartialTicks, enderdragonpart.xOld, enderdragonpart.getX());
                double d4 = d1 + Mth.lerp((double)pPartialTicks, enderdragonpart.yOld, enderdragonpart.getY());
                double d5 = d2 + Mth.lerp((double)pPartialTicks, enderdragonpart.zOld, enderdragonpart.getZ());
                pPoseStack.translate(d3, d4, d5);
                LevelRenderer.renderLineBox(
                    pPoseStack,
                    pBuffer,
                    enderdragonpart.getBoundingBox().move(-enderdragonpart.getX(), -enderdragonpart.getY(), -enderdragonpart.getZ()),
                    0.25F,
                    1.0F,
                    0.0F,
                    1.0F
                );
                pPoseStack.popPose();
            }
        }

        if (pEntity instanceof LivingEntity) {
            float f1 = 0.01F;
            LevelRenderer.renderLineBox(
                pPoseStack,
                pBuffer,
                aabb.minX,
                (double)(pEntity.getEyeHeight() - 0.01F),
                aabb.minZ,
                aabb.maxX,
                (double)(pEntity.getEyeHeight() + 0.01F),
                aabb.maxZ,
                1.0F,
                0.0F,
                0.0F,
                1.0F
            );
        }

        Entity entity = pEntity.getVehicle();
        if (entity != null) {
            float f = Math.min(entity.getBbWidth(), pEntity.getBbWidth()) / 2.0F;
            float f2 = 0.0625F;
            Vec3 vec3 = entity.getPassengerRidingPosition(pEntity).subtract(pEntity.position());
            LevelRenderer.renderLineBox(
                pPoseStack,
                pBuffer,
                vec3.x - (double)f,
                vec3.y,
                vec3.z - (double)f,
                vec3.x + (double)f,
                vec3.y + 0.0625,
                vec3.z + (double)f,
                1.0F,
                1.0F,
                0.0F,
                1.0F
            );
        }

        renderVector(pPoseStack, pBuffer, new Vector3f(0.0F, pEntity.getEyeHeight(), 0.0F), pEntity.getViewVector(pPartialTicks).scale(2.0), -16776961);
    }

    private static void renderVector(PoseStack p_353057_, VertexConsumer p_353027_, Vector3f p_353068_, Vec3 p_353070_, int p_353032_) {
        PoseStack.Pose posestack$pose = p_353057_.last();
        p_353027_.addVertex(posestack$pose, p_353068_)
            .setColor(p_353032_)
            .setNormal(posestack$pose, (float)p_353070_.x, (float)p_353070_.y, (float)p_353070_.z);
        p_353027_.addVertex(
                posestack$pose,
                (float)((double)p_353068_.x() + p_353070_.x),
                (float)((double)p_353068_.y() + p_353070_.y),
                (float)((double)p_353068_.z() + p_353070_.z)
            )
            .setColor(p_353032_)
            .setNormal(posestack$pose, (float)p_353070_.x, (float)p_353070_.y, (float)p_353070_.z);
    }

    private void renderFlame(PoseStack pPoseStack, MultiBufferSource pBuffer, Entity pEntity, Quaternionf pQuaternion) {
        TextureAtlasSprite textureatlassprite = ModelBakery.FIRE_0.sprite();
        TextureAtlasSprite textureatlassprite1 = ModelBakery.FIRE_1.sprite();
        pPoseStack.pushPose();
        float f = pEntity.getBbWidth() * 1.4F;
        pPoseStack.scale(f, f, f);
        float f1 = 0.5F;
        float f2 = 0.0F;
        float f3 = pEntity.getBbHeight() / f;
        float f4 = 0.0F;
        pPoseStack.mulPose(pQuaternion);
        pPoseStack.translate(0.0F, 0.0F, 0.3F - (float)((int)f3) * 0.02F);
        float f5 = 0.0F;
        int i = 0;
        VertexConsumer vertexconsumer = pBuffer.getBuffer(Sheets.cutoutBlockSheet());

        for (PoseStack.Pose posestack$pose = pPoseStack.last(); f3 > 0.0F; i++) {
            TextureAtlasSprite textureatlassprite2 = i % 2 == 0 ? textureatlassprite : textureatlassprite1;
            float f6 = textureatlassprite2.getU0();
            float f7 = textureatlassprite2.getV0();
            float f8 = textureatlassprite2.getU1();
            float f9 = textureatlassprite2.getV1();
            if (i / 2 % 2 == 0) {
                float f10 = f8;
                f8 = f6;
                f6 = f10;
            }

            fireVertex(posestack$pose, vertexconsumer, -f1 - 0.0F, 0.0F - f4, f5, f8, f9);
            fireVertex(posestack$pose, vertexconsumer, f1 - 0.0F, 0.0F - f4, f5, f6, f9);
            fireVertex(posestack$pose, vertexconsumer, f1 - 0.0F, 1.4F - f4, f5, f6, f7);
            fireVertex(posestack$pose, vertexconsumer, -f1 - 0.0F, 1.4F - f4, f5, f8, f7);
            f3 -= 0.45F;
            f4 -= 0.45F;
            f1 *= 0.9F;
            f5 -= 0.03F;
        }

        pPoseStack.popPose();
    }

    private static void fireVertex(
        PoseStack.Pose pMatrixEntry, VertexConsumer pBuffer, float pX, float pY, float pZ, float pTexU, float pTexV
    ) {
        pBuffer.addVertex(pMatrixEntry, pX, pY, pZ)
            .setColor(-1)
            .setUv(pTexU, pTexV)
            .setUv1(0, 10)
            .setLight(240)
            .setNormal(pMatrixEntry, 0.0F, 1.0F, 0.0F);
    }

    private static void renderShadow(
        PoseStack pPoseStack, MultiBufferSource pBuffer, Entity pEntity, float pWeight, float pPartialTicks, LevelReader pLevel, float pSize
    ) {
        double d0 = Mth.lerp((double)pPartialTicks, pEntity.xOld, pEntity.getX());
        double d1 = Mth.lerp((double)pPartialTicks, pEntity.yOld, pEntity.getY());
        double d2 = Mth.lerp((double)pPartialTicks, pEntity.zOld, pEntity.getZ());
        float f = Math.min(pWeight / 0.5F, pSize);
        int i = Mth.floor(d0 - (double)pSize);
        int j = Mth.floor(d0 + (double)pSize);
        int k = Mth.floor(d1 - (double)f);
        int l = Mth.floor(d1);
        int i1 = Mth.floor(d2 - (double)pSize);
        int j1 = Mth.floor(d2 + (double)pSize);
        PoseStack.Pose posestack$pose = pPoseStack.last();
        VertexConsumer vertexconsumer = pBuffer.getBuffer(SHADOW_RENDER_TYPE);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int k1 = i1; k1 <= j1; k1++) {
            for (int l1 = i; l1 <= j; l1++) {
                blockpos$mutableblockpos.set(l1, 0, k1);
                ChunkAccess chunkaccess = pLevel.getChunk(blockpos$mutableblockpos);

                for (int i2 = k; i2 <= l; i2++) {
                    blockpos$mutableblockpos.setY(i2);
                    float f1 = pWeight - (float)(d1 - (double)blockpos$mutableblockpos.getY()) * 0.5F;
                    renderBlockShadow(posestack$pose, vertexconsumer, chunkaccess, pLevel, blockpos$mutableblockpos, d0, d1, d2, pSize, f1);
                }
            }
        }
    }

    private static void renderBlockShadow(
        PoseStack.Pose pPose,
        VertexConsumer pVertexConsumer,
        ChunkAccess pChunk,
        LevelReader pLevel,
        BlockPos pPos,
        double pX,
        double pY,
        double pZ,
        float pSize,
        float p_277496_
    ) {
        BlockPos blockpos = pPos.below();
        BlockState blockstate = pChunk.getBlockState(blockpos);
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE && pLevel.getMaxLocalRawBrightness(pPos) > 3) {
            if (blockstate.isCollisionShapeFullBlock(pChunk, blockpos)) {
                VoxelShape voxelshape = blockstate.getShape(pChunk, blockpos);
                if (!voxelshape.isEmpty()) {
                    float f = LightTexture.getBrightness(pLevel.dimensionType(), pLevel.getMaxLocalRawBrightness(pPos));
                    float f1 = p_277496_ * 0.5F * f;
                    if (f1 >= 0.0F) {
                        if (f1 > 1.0F) {
                            f1 = 1.0F;
                        }

                        int i = FastColor.ARGB32.color(Mth.floor(f1 * 255.0F), 255, 255, 255);
                        AABB aabb = voxelshape.bounds();
                        double d0 = (double)pPos.getX() + aabb.minX;
                        double d1 = (double)pPos.getX() + aabb.maxX;
                        double d2 = (double)pPos.getY() + aabb.minY;
                        double d3 = (double)pPos.getZ() + aabb.minZ;
                        double d4 = (double)pPos.getZ() + aabb.maxZ;
                        float f2 = (float)(d0 - pX);
                        float f3 = (float)(d1 - pX);
                        float f4 = (float)(d2 - pY);
                        float f5 = (float)(d3 - pZ);
                        float f6 = (float)(d4 - pZ);
                        float f7 = -f2 / 2.0F / pSize + 0.5F;
                        float f8 = -f3 / 2.0F / pSize + 0.5F;
                        float f9 = -f5 / 2.0F / pSize + 0.5F;
                        float f10 = -f6 / 2.0F / pSize + 0.5F;
                        shadowVertex(pPose, pVertexConsumer, i, f2, f4, f5, f7, f9);
                        shadowVertex(pPose, pVertexConsumer, i, f2, f4, f6, f7, f10);
                        shadowVertex(pPose, pVertexConsumer, i, f3, f4, f6, f8, f10);
                        shadowVertex(pPose, pVertexConsumer, i, f3, f4, f5, f8, f9);
                    }
                }
            }
        }
    }

    private static void shadowVertex(
        PoseStack.Pose p_114423_, VertexConsumer p_114424_, int p_350992_, float p_114425_, float p_114426_, float p_114427_, float p_114428_, float p_114429_
    ) {
        Vector3f vector3f = p_114423_.pose().transformPosition(p_114425_, p_114426_, p_114427_, new Vector3f());
        p_114424_.addVertex(vector3f.x(), vector3f.y(), vector3f.z(), p_350992_, p_114428_, p_114429_, OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 1.0F, 0.0F);
    }

    /**
     * World sets this RenderManager's worldObj to the world provided
     */
    public void setLevel(@Nullable Level pLevel) {
        this.level = pLevel;
        if (pLevel == null) {
            this.camera = null;
        }
    }

    public double distanceToSqr(Entity pEntity) {
        return this.camera.getPosition().distanceToSqr(pEntity.position());
    }

    public double distanceToSqr(double pX, double pY, double pZ) {
        return this.camera.getPosition().distanceToSqr(pX, pY, pZ);
    }

    public Quaternionf cameraOrientation() {
        return this.cameraOrientation;
    }

    public ItemInHandRenderer getItemInHandRenderer() {
        return this.itemInHandRenderer;
    }

    public Map<PlayerSkin.Model, EntityRenderer<? extends Player>> getSkinMap() {
        return java.util.Collections.unmodifiableMap(playerRenderers);
    }

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        EntityRendererProvider.Context entityrendererprovider$context = new EntityRendererProvider.Context(
            this, this.itemRenderer, this.blockRenderDispatcher, this.itemInHandRenderer, pResourceManager, this.entityModels, this.font
        );
        this.renderers = EntityRenderers.createEntityRenderers(entityrendererprovider$context);
        this.playerRenderers = EntityRenderers.createPlayerRenderers(entityrendererprovider$context);
        net.neoforged.fml.ModLoader.postEvent(new net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers(renderers, playerRenderers, entityrendererprovider$context));
    }
}
