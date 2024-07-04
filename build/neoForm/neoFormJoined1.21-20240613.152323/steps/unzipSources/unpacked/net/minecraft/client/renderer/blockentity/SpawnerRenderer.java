package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpawnerRenderer implements BlockEntityRenderer<SpawnerBlockEntity> {
    private final EntityRenderDispatcher entityRenderer;

    public SpawnerRenderer(BlockEntityRendererProvider.Context pContext) {
        this.entityRenderer = pContext.getEntityRenderer();
    }

    public void render(SpawnerBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        Level level = pBlockEntity.getLevel();
        if (level != null) {
            BaseSpawner basespawner = pBlockEntity.getSpawner();
            Entity entity = basespawner.getOrCreateDisplayEntity(level, pBlockEntity.getBlockPos());
            if (entity != null) {
                renderEntityInSpawner(pPartialTick, pPoseStack, pBuffer, pPackedLight, entity, this.entityRenderer, basespawner.getoSpin(), basespawner.getSpin());
            }
        }
    }

    public static void renderEntityInSpawner(
        float pPartialTick,
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        Entity pEntity,
        EntityRenderDispatcher pEntityRenderer,
        double pOSpin,
        double pSpin
    ) {
        pPoseStack.pushPose();
        pPoseStack.translate(0.5F, 0.0F, 0.5F);
        float f = 0.53125F;
        float f1 = Math.max(pEntity.getBbWidth(), pEntity.getBbHeight());
        if ((double)f1 > 1.0) {
            f /= f1;
        }

        pPoseStack.translate(0.0F, 0.4F, 0.0F);
        pPoseStack.mulPose(Axis.YP.rotationDegrees((float)Mth.lerp((double)pPartialTick, pOSpin, pSpin) * 10.0F));
        pPoseStack.translate(0.0F, -0.2F, 0.0F);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
        pPoseStack.scale(f, f, f);
        pEntityRenderer.render(pEntity, 0.0, 0.0, 0.0, 0.0F, pPartialTick, pPoseStack, pBuffer, pPackedLight);
        pPoseStack.popPose();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(SpawnerBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0, pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
    }
}
